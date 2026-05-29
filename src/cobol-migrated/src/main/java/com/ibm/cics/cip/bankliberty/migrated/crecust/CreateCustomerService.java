/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bankliberty.migrated.crecust;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Java 21 migration of CRECUST.cbl - Create Customer business logic.
 *
 * <p>This program takes customer information (name, address, DOB), gets the
 * sort code, performs a credit check using multiple credit agencies
 * asynchronously, validates the date of birth, assigns a new customer number,
 * writes the customer record and a processed transaction audit record.</p>
 *
 * <p>Original COBOL flow:</p>
 * <ol>
 *   <li>Derive date and time</li>
 *   <li>Perform async credit check across agencies</li>
 *   <li>Validate date of birth</li>
 *   <li>Enqueue the named counter for customer number serialization</li>
 *   <li>Get next customer number from customer control record</li>
 *   <li>Write customer record to the CUSTOMER data store</li>
 *   <li>Update customer control record (count and last number)</li>
 *   <li>Write processed transaction record to PROCTRAN data store</li>
 *   <li>Dequeue the named counter</li>
 *   <li>Return sort code and customer number on success</li>
 * </ol>
 *
 * <p>Fail codes preserved from COBOL:</p>
 * <ul>
 *   <li>'A' - Credit check container PUT failed</li>
 *   <li>'B' - Credit check async transaction RUN failed</li>
 *   <li>'C' - Credit check FETCH NOTFINISHED, no data retrieved</li>
 *   <li>'D' - Credit check FETCH INVREQ, no children</li>
 *   <li>'E' - Credit check GET CONTAINER failed after successful FETCH</li>
 *   <li>'F' - Credit check child ABEND</li>
 *   <li>'G' - Credit check SECERROR or general credit check error</li>
 *   <li>'H' - Credit check unknown completion status</li>
 *   <li>'O' - Date of birth year &lt; 1601 or customer age &gt; 150</li>
 *   <li>'Y' - Date of birth is in the future</li>
 *   <li>'Z' - Date of birth is invalid</li>
 *   <li>'1' - Customer VSAM write failed</li>
 *   <li>'3' - Named counter ENQ failed</li>
 *   <li>'4' - Customer control record read/rewrite failed</li>
 *   <li>'5' - Named counter DEQ failed</li>
 * </ul>
 *
 * @see CreateCustomerRequest
 * @see CreateCustomerResponse
 */
public class CreateCustomerService {

    private static final Logger logger =
            Logger.getLogger(CreateCustomerService.class.getName());

    static final int MIN_BIRTH_YEAR = 1601;
    static final int MAX_CUSTOMER_AGE = 150;
    static final int CREDIT_SCORE_REVIEW_WINDOW_DAYS = 21;
    static final String SORT_CODE_DEFAULT = "987654";

    private final CreditCheckService creditCheckService;
    private final CustomerDataStore customerDataStore;
    private final ProcessedTransactionDataStore processedTransactionDataStore;
    private final NamedCounterService namedCounterService;

    private Random random;

    public CreateCustomerService(
            CreditCheckService creditCheckService,
            CustomerDataStore customerDataStore,
            ProcessedTransactionDataStore processedTransactionDataStore,
            NamedCounterService namedCounterService) {
        this.creditCheckService = creditCheckService;
        this.customerDataStore = customerDataStore;
        this.processedTransactionDataStore = processedTransactionDataStore;
        this.namedCounterService = namedCounterService;
        this.random = new Random();
    }

    /**
     * Allows injection of a deterministic Random for testing.
     */
    void setRandom(Random random) {
        this.random = random;
    }

    /**
     * Creates a new customer, performing credit checks, date validation,
     * data store writes, and returning the result.
     *
     * <p>Mirrors the PREMIERE SECTION of CRECUST.cbl.</p>
     *
     * @param request the customer creation request
     * @return the result containing the new customer number on success, or a
     *         fail code on failure
     */
    public CreateCustomerResponse createCustomer(CreateCustomerRequest request) {
        LocalDate today = LocalDate.now();

        int creditScore;
        LocalDate creditScoreReviewDate;
        CreditCheckResult creditCheckResult = performCreditCheck(request, today);
        creditScore = creditCheckResult.creditScore();
        creditScoreReviewDate = creditCheckResult.reviewDate();

        if (creditCheckResult.errorOccurred()) {
            logger.log(Level.WARNING,
                    "Credit check error, fail code: {0}",
                    creditCheckResult.failCode());
            return CreateCustomerResponse.failure(
                    creditCheckResult.failCode(),
                    creditScore,
                    creditScoreReviewDate
            );
        }

        String dobFailCode = validateDateOfBirth(request.dateOfBirth(), today);
        if (dobFailCode != null) {
            return CreateCustomerResponse.failure(dobFailCode, creditScore,
                    creditScoreReviewDate);
        }

        boolean enqueued = false;
        try {
            namedCounterService.enqueue(request.sortCode());
            enqueued = true;
        } catch (NamedCounterService.NamedCounterException e) {
            logger.log(Level.SEVERE, "Failed to enqueue named counter", e);
            return CreateCustomerResponse.failure(e.getFailCode(),
                    creditScore, creditScoreReviewDate);
        }

        try {
            long newCustomerNumber = getNextCustomerNumber(request.sortCode());

            writeCustomerRecord(request, newCustomerNumber, creditScore,
                    creditScoreReviewDate);

            updateCustomerControlAfterWrite(request.sortCode(),
                    newCustomerNumber);

            writeProcessedTransaction(request, newCustomerNumber, today);

            return CreateCustomerResponse.success(
                    request.sortCode(),
                    newCustomerNumber,
                    request.name(),
                    request.address(),
                    request.dateOfBirth(),
                    creditScore,
                    creditScoreReviewDate
            );
        } catch (CustomerDataStore.DataStoreException e) {
            logger.log(Level.SEVERE, "Customer data store operation failed", e);
            dequeueQuietly(request.sortCode());
            return CreateCustomerResponse.failure(e.getFailCode(),
                    creditScore, creditScoreReviewDate);
        } catch (ProcessedTransactionDataStore.ProcessedTransactionException e) {
            logger.log(Level.SEVERE, "Processed transaction write failed", e);
            dequeueQuietly(request.sortCode());
            throw new CreateCustomerAbendException("HWPT",
                    "Unable to write to PROCTRAN data store", e);
        } finally {
            if (enqueued) {
                dequeueQuietly(request.sortCode());
            }
        }
    }

    /**
     * Performs the credit check across multiple agencies and computes the
     * average score and review date.
     *
     * <p>Mirrors the CREDIT-CHECK SECTION of CRECUST.cbl.</p>
     */
    CreditCheckResult performCreditCheck(CreateCustomerRequest request,
                                         LocalDate today) {
        List<Integer> scores;
        try {
            scores = creditCheckService.performCreditChecks(request);
        } catch (CreditCheckService.CreditCheckException e) {
            logger.log(Level.WARNING, "Credit check exception", e);
            return new CreditCheckResult(0, today, true, e.getFailCode());
        }

        if (scores == null || scores.isEmpty()) {
            return new CreditCheckResult(0, today, true, "C");
        }

        int totalScore = 0;
        for (int score : scores) {
            totalScore += score;
        }
        int averageScore = totalScore / scores.size();

        LocalDate reviewDate = computeReviewDate(today);

        return new CreditCheckResult(averageScore, reviewDate, false, null);
    }

    /**
     * Computes a random credit score review date within the next 21 days.
     *
     * <p>Mirrors the COBOL logic:
     * <pre>
     *   COMPUTE WS-REVIEW-DATE-ADD = ((21 - 1) * FUNCTION RANDOM(WS-SEED)) + 1
     *   COMPUTE WS-NEW-REVIEW-DATE-INT = WS-TODAY-INT + WS-REVIEW-DATE-ADD
     * </pre></p>
     */
    LocalDate computeReviewDate(LocalDate today) {
        int daysToAdd = random.nextInt(CREDIT_SCORE_REVIEW_WINDOW_DAYS - 1) + 1;
        return today.plusDays(daysToAdd);
    }

    /**
     * Validates the date of birth.
     *
     * <p>Mirrors the DATE-OF-BIRTH-CHECK SECTION of CRECUST.cbl.</p>
     *
     * @param dateOfBirth the date of birth to validate
     * @param today today's date for age calculation
     * @return null if valid, or a fail code string if invalid
     */
    String validateDateOfBirth(LocalDate dateOfBirth, LocalDate today) {
        if (dateOfBirth.getYear() < MIN_BIRTH_YEAR) {
            return "O";
        }

        if (!isValidDate(dateOfBirth)) {
            return "Z";
        }

        long customerAge = ChronoUnit.YEARS.between(dateOfBirth, today);
        if (customerAge > MAX_CUSTOMER_AGE) {
            return "O";
        }

        if (dateOfBirth.isAfter(today)) {
            return "Y";
        }

        return null;
    }

    /**
     * Checks if the date is a valid calendar date.
     * Mirrors the CEEDAYS validation in the COBOL program.
     */
    private boolean isValidDate(LocalDate date) {
        try {
            LocalDate.of(date.getYear(), date.getMonthValue(),
                    date.getDayOfMonth());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gets the next customer number by reading and updating the customer
     * control record.
     *
     * <p>Mirrors the GET-LAST-CUSTOMER-VSAM SECTION and UPD-NCS SECTION.</p>
     */
    private long getNextCustomerNumber(String sortCode)
            throws CustomerDataStore.DataStoreException {
        CustomerControlRecord controlRecord =
                customerDataStore.readControlRecordForUpdate("000000");

        long nextNumber = controlRecord.getLastCustomerNumber() + 1;
        controlRecord.setLastCustomerNumber(nextNumber);

        customerDataStore.rewriteControlRecord(controlRecord);

        return nextNumber;
    }

    /**
     * Writes the customer record to the CUSTOMER data store.
     *
     * <p>Mirrors the WRITE-CUSTOMER-VSAM SECTION (write portion).</p>
     */
    private void writeCustomerRecord(CreateCustomerRequest request,
                                     long customerNumber,
                                     int creditScore,
                                     LocalDate creditScoreReviewDate)
            throws CustomerDataStore.DataStoreException {
        CustomerRecord record = new CustomerRecord(
                CustomerRecord.EYECATCHER_VALUE,
                request.sortCode(),
                customerNumber,
                request.name(),
                request.address(),
                request.dateOfBirth(),
                creditScore,
                creditScoreReviewDate
        );
        customerDataStore.writeCustomer(record);
    }

    /**
     * Updates the customer control record after a successful customer write.
     *
     * <p>Mirrors the customer control update in WRITE-CUSTOMER-VSAM SECTION:
     * reads control record, increments numberOfCustomers, sets lastCustomerNumber,
     * then rewrites.</p>
     */
    private void updateCustomerControlAfterWrite(String sortCode,
                                                  long customerNumber)
            throws CustomerDataStore.DataStoreException {
        CustomerControlRecord controlRecord =
                customerDataStore.readControlRecordForUpdate("000000");
        controlRecord.setNumberOfCustomers(
                controlRecord.getNumberOfCustomers() + 1);
        controlRecord.setLastCustomerNumber(customerNumber);
        customerDataStore.rewriteControlRecord(controlRecord);
    }

    /**
     * Writes the processed transaction record.
     *
     * <p>Mirrors the WRITE-PROCTRAN-DB2 SECTION.</p>
     *
     * <p>COBOL description field layout (40 chars):
     * <pre>
     *   Positions 1-6:   Sort code
     *   Positions 7-16:  Customer number
     *   Positions 17-30: Customer name (first 14 chars)
     *   Positions 31-40: Date of birth (DD/MM/YYYY)
     * </pre></p>
     */
    private void writeProcessedTransaction(CreateCustomerRequest request,
                                           long customerNumber,
                                           LocalDate transactionDate)
            throws ProcessedTransactionDataStore.ProcessedTransactionException {
        LocalDateTime now = LocalDateTime.now();

        String custNoFormatted = String.format("%010d", customerNumber);

        String namePart = request.name().length() > 14
                ? request.name().substring(0, 14)
                : String.format("%-14s", request.name());

        String dobFormatted = String.format("%02d/%02d/%04d",
                request.dateOfBirth().getDayOfMonth(),
                request.dateOfBirth().getMonthValue(),
                request.dateOfBirth().getYear());

        String description = String.format("%-6s%-10s%-14s%-10s",
                request.sortCode(),
                custNoFormatted,
                namePart,
                dobFormatted);

        ProcessedTransactionRecord transactionRecord =
                new ProcessedTransactionRecord(
                        ProcessedTransactionRecord.EYECATCHER_VALUE,
                        request.sortCode(),
                        "00000000",
                        now.toLocalDate(),
                        now.toLocalTime(),
                        String.format("%012d",
                                Thread.currentThread().threadId()),
                        ProcessedTransactionRecord
                                .TYPE_BRANCH_CREATE_CUSTOMER,
                        description,
                        BigDecimal.ZERO
                );

        processedTransactionDataStore.writeTransaction(transactionRecord);
    }

    /**
     * Dequeues the named counter, logging but not propagating errors.
     * In COBOL, DEQ failures would set fail code '5' and exit.
     */
    private void dequeueQuietly(String sortCode) {
        try {
            namedCounterService.dequeue(sortCode);
        } catch (NamedCounterService.NamedCounterException e) {
            logger.log(Level.SEVERE,
                    "Failed to dequeue named counter for sort code: "
                            + sortCode, e);
        }
    }

    /**
     * Internal record for credit check results.
     */
    record CreditCheckResult(
            int creditScore,
            LocalDate reviewDate,
            boolean errorOccurred,
            String failCode
    ) {
    }

    /**
     * Runtime exception for unrecoverable errors that mirror COBOL ABEND.
     * In COBOL, the program would EXEC CICS ABEND ABCODE('HWPT').
     */
    public static class CreateCustomerAbendException extends RuntimeException {
        private final String abendCode;

        public CreateCustomerAbendException(String abendCode, String message,
                                            Throwable cause) {
            super(message, cause);
            this.abendCode = abendCode;
        }

        public String getAbendCode() {
            return abendCode;
        }
    }
}
