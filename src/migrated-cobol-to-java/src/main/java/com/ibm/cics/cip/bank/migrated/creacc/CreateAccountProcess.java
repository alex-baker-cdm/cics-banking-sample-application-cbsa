/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from COBOL: CREACC.cbl (Create Account - business logic)
 * Original author: Jon Collett
 *
 * This class preserves all business logic and behavior from the original
 * COBOL program. The program takes account information, validates the
 * customer, generates a new account number via the CONTROL table, inserts
 * the account record into the ACCOUNT table, and writes a processed
 * transaction record to the PROCTRAN table.
 */

package com.ibm.cics.cip.bank.migrated.creacc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.cics.cip.bank.migrated.creacc.repository.AccountControlRepository;
import com.ibm.cics.cip.bank.migrated.creacc.repository.AccountRepository;
import com.ibm.cics.cip.bank.migrated.creacc.repository.ProcessedTransactionRepository;
import com.ibm.cics.cip.bank.migrated.creacc.service.AccountCountService;
import com.ibm.cics.cip.bank.migrated.creacc.service.CustomerInquiryService;

/**
 * Creates a new bank account. This is the Java 21 equivalent of CREACC.cbl.
 * <p>
 * Flow (mirrors PREMIERE SECTION):
 * <ol>
 *   <li>Validate that the customer exists (INQCUST)</li>
 *   <li>Count existing customer accounts (INQACCCU) &mdash; max 10</li>
 *   <li>Validate the account type</li>
 *   <li>Get and increment the next account number from the CONTROL table</li>
 *   <li>Increment the account count in the CONTROL table</li>
 *   <li>Insert the account record into the ACCOUNT table</li>
 *   <li>Write a processed transaction record to PROCTRAN</li>
 *   <li>Return the new account details</li>
 * </ol>
 */
public class CreateAccountProcess {

    private static final Logger logger = Logger.getLogger(
            CreateAccountProcess.class.getName());

    private static final String ACCOUNT_EYECATCHER = "ACCT";
    private static final String PROCTRAN_EYECATCHER = "PRTR";
    private static final String PROCTRAN_TYPE_BRANCH_CREATE_ACCOUNT = "OCA";
    private static final int MAX_ACCOUNTS_PER_CUSTOMER = 9;
    private static final int ACCOUNT_NUMBER_LENGTH = 8;
    private static final int PROCTRAN_DESC_LENGTH = 40;

    static final String ACCOUNT_LAST_SUFFIX = "-ACCOUNT-LAST";
    static final String ACCOUNT_COUNT_SUFFIX = "-ACCOUNT-COUNT";

    private static final DateTimeFormatter DATE_DOTTED =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_HHMMSS =
            DateTimeFormatter.ofPattern("HHmmss");
    private static final DateTimeFormatter DATE_DDMMYYYY =
            DateTimeFormatter.ofPattern("ddMMyyyy");

    private final CustomerInquiryService customerInquiryService;
    private final AccountCountService accountCountService;
    private final AccountControlRepository accountControlRepository;
    private final AccountRepository accountRepository;
    private final ProcessedTransactionRepository processedTransactionRepository;
    private final String sortCode;

    /**
     * Constructs the process with all required dependencies.
     * The sort code defaults to "987654" matching the COBOL SORTCODE copybook.
     */
    public CreateAccountProcess(
            CustomerInquiryService customerInquiryService,
            AccountCountService accountCountService,
            AccountControlRepository accountControlRepository,
            AccountRepository accountRepository,
            ProcessedTransactionRepository processedTransactionRepository) {
        this(customerInquiryService, accountCountService,
                accountControlRepository, accountRepository,
                processedTransactionRepository, "987654");
    }

    public CreateAccountProcess(
            CustomerInquiryService customerInquiryService,
            AccountCountService accountCountService,
            AccountControlRepository accountControlRepository,
            AccountRepository accountRepository,
            ProcessedTransactionRepository processedTransactionRepository,
            String sortCode) {
        this.customerInquiryService = customerInquiryService;
        this.accountCountService = accountCountService;
        this.accountControlRepository = accountControlRepository;
        this.accountRepository = accountRepository;
        this.processedTransactionRepository = processedTransactionRepository;
        this.sortCode = sortCode;
    }

    /**
     * Executes the Create Account business process.
     * Mirrors the PREMIERE SECTION of CREACC.cbl.
     *
     * @param request the account creation request
     * @return the result containing success/failure and account details
     */
    public CreateAccountResult execute(CreateAccountRequest request) {
        return execute(request, LocalDate.now(), LocalTime.now());
    }

    /**
     * Executes the Create Account business process with explicit date/time
     * (useful for testing determinism).
     *
     * @param request the account creation request
     * @param today   the current date
     * @param now     the current time
     * @return the result containing success/failure and account details
     */
    public CreateAccountResult execute(CreateAccountRequest request,
                                       LocalDate today, LocalTime now) {
        logger.log(Level.INFO, "CREACC: Starting account creation for "
                + "customer {0}", request.customerNumber());

        // Step 1: Validate customer exists (LINK to INQCUST)
        if (!customerInquiryService.customerExists(
                request.customerNumber())) {
            logger.log(Level.WARNING,
                    "CREACC: Customer {0} not found or inquiry failed",
                    request.customerNumber());
            return CreateAccountResult.failure(
                    FailCode.CUSTOMER_NOT_FOUND, request);
        }

        // Step 2: Count existing accounts (LINK to INQACCCU)
        int accountCount = accountCountService.countAccounts(
                request.customerNumber());
        if (accountCount < 0) {
            logger.log(Level.WARNING,
                    "CREACC: Error counting accounts for customer {0}",
                    request.customerNumber());
            return CreateAccountResult.failure(
                    FailCode.ACCOUNT_COUNT_ERROR, request);
        }

        // Step 3: Check max accounts (> 9 in COBOL, i.e., >= 10)
        if (accountCount > MAX_ACCOUNTS_PER_CUSTOMER) {
            logger.log(Level.WARNING,
                    "CREACC: Customer {0} already has {1} accounts (max 10)",
                    new Object[]{request.customerNumber(), accountCount});
            return CreateAccountResult.failure(
                    FailCode.MAX_ACCOUNTS_EXCEEDED, request);
        }

        // Step 4: Validate account type
        AccountType accountType = AccountType.fromString(
                request.accountType());
        if (accountType == null) {
            logger.log(Level.WARNING,
                    "CREACC: Invalid account type: {0}",
                    request.accountType());
            return CreateAccountResult.failure(
                    FailCode.INVALID_ACCOUNT_TYPE, request);
        }

        // Step 5: Find next account number (FIND-NEXT-ACCOUNT)
        String accountNumber = findNextAccount();
        if (accountNumber == null) {
            logger.log(Level.SEVERE,
                    "CREACC: Failed to obtain next account number");
            return CreateAccountResult.failure(
                    FailCode.ENQ_FAILED, request);
        }

        // Step 6: Write account to DB2 (WRITE-ACCOUNT-DB2)
        return writeAccountDb2(request, accountNumber, today, now);
    }

    /**
     * Gets the next account number by reading and incrementing the
     * CONTROL table's ACCOUNT-LAST record, then incrementing the
     * ACCOUNT-COUNT record.
     * <p>
     * Mirrors FIND-NEXT-ACCOUNT SECTION (lines 429-768).
     *
     * @return the 8-digit account number, or null on failure
     */
    String findNextAccount() {
        // Read ACCOUNT-LAST control record
        String accountLastKey = sortCode + ACCOUNT_LAST_SUFFIX;
        long currentLast = accountControlRepository.getControlValue(
                accountLastKey);
        if (currentLast < 0) {
            logger.log(Level.SEVERE,
                    "CREACC: Cannot read CONTROL record {0}",
                    accountLastKey);
            return null;
        }

        // Increment and update
        long newAccountNumber = currentLast + 1;
        if (!accountControlRepository.updateControlValue(
                accountLastKey, newAccountNumber)) {
            logger.log(Level.SEVERE,
                    "CREACC: Cannot update CONTROL record {0}",
                    accountLastKey);
            return null;
        }

        // Read and increment ACCOUNT-COUNT control record
        String accountCountKey = sortCode + ACCOUNT_COUNT_SUFFIX;
        long currentCount = accountControlRepository.getControlValue(
                accountCountKey);
        if (currentCount < 0) {
            logger.log(Level.SEVERE,
                    "CREACC: Cannot read CONTROL record {0}",
                    accountCountKey);
            return null;
        }

        long newCount = currentCount + 1;
        if (!accountControlRepository.updateControlValue(
                accountCountKey, newCount)) {
            logger.log(Level.SEVERE,
                    "CREACC: Cannot update CONTROL record {0}",
                    accountCountKey);
            return null;
        }

        // Extract last 8 digits (mirrors NCS-ACC-NO-DISP(9:8))
        return formatAccountNumber(newAccountNumber);
    }

    /**
     * Formats a numeric account value as an 8-digit string,
     * taking the last 8 digits. Mirrors the COBOL:
     * <pre>
     *   MOVE NCS-ACC-NO-VALUE TO NCS-ACC-NO-DISP.
     *   MOVE NCS-ACC-NO-DISP(9:8) TO HV-ACCOUNT-ACC-NO.
     * </pre>
     * NCS-ACC-NO-DISP is PIC 9(16), so position 9 for length 8
     * gives the last 8 digits.
     */
    static String formatAccountNumber(long value) {
        String padded = String.format("%016d", value);
        return padded.substring(padded.length() - ACCOUNT_NUMBER_LENGTH);
    }

    /**
     * Writes the account record to DB2 and the processed transaction
     * to PROCTRAN. Mirrors WRITE-ACCOUNT-DB2 SECTION (lines 774-917).
     */
    private CreateAccountResult writeAccountDb2(
            CreateAccountRequest request,
            String accountNumber,
            LocalDate today,
            LocalTime now) {

        // Calculate dates
        String openedDotted = today.format(DATE_DOTTED);
        String lastStmtDotted = openedDotted;

        // Next statement date = today + 30 days (WRITE-ACCOUNT-DB2 lines 802-808)
        LocalDate nextStmtDate =
                NextStatementDateCalculator.calculateNextStatementDate(today);
        String nextStmtDotted = nextStmtDate.format(DATE_DOTTED);

        // Insert account record
        boolean inserted = accountRepository.insertAccount(
                ACCOUNT_EYECATCHER,
                request.customerNumber(),
                sortCode,
                accountNumber,
                request.accountType(),
                request.interestRate(),
                openedDotted,
                request.overdraftLimit(),
                lastStmtDotted,
                nextStmtDotted,
                request.availableBalance(),
                request.actualBalance()
        );

        if (!inserted) {
            logger.log(Level.WARNING,
                    "CREACC: Failed to INSERT account into DB2");
            return CreateAccountResult.failure(
                    FailCode.ACCOUNT_INSERT_FAILED, request);
        }

        // Write processed transaction (WRITE-PROCTRAN)
        writeProctran(request, accountNumber, today, now);

        // Build successful result (COMMAREA output)
        String openedDdmmyyyy = today.format(DATE_DDMMYYYY);
        String lastStmtDdmmyyyy = openedDdmmyyyy;
        String nextStmtDdmmyyyy = nextStmtDate.format(DATE_DDMMYYYY);

        logger.log(Level.INFO,
                "CREACC: Successfully created account {0} for customer {1}",
                new Object[]{accountNumber, request.customerNumber()});

        return new CreateAccountResult(
                true,
                " ",
                ACCOUNT_EYECATCHER,
                request.customerNumber(),
                sortCode,
                accountNumber,
                request.accountType(),
                request.interestRate(),
                openedDdmmyyyy,
                request.overdraftLimit(),
                lastStmtDdmmyyyy,
                nextStmtDdmmyyyy,
                request.availableBalance(),
                request.actualBalance()
        );
    }

    /**
     * Writes a processed transaction record for the account creation.
     * Mirrors WRITE-PROCTRAN-DB2 SECTION (lines 928-1065).
     * <p>
     * The description field (40 chars) is packed as:
     * <pre>
     *   Positions 1-10:  Customer number
     *   Positions 11-18: Account type
     *   Positions 19-26: Last statement date (DDMMYYYY)
     *   Positions 27-34: Next statement date (DDMMYYYY)
     *   Positions 35-40: Spaces
     * </pre>
     * Transaction type is 'OCA' (Branch Create Account).
     */
    private void writeProctran(CreateAccountRequest request,
                               String accountNumber,
                               LocalDate today,
                               LocalTime now) {

        String dateDotted = today.format(DATE_DOTTED);
        String timeStr = now.format(TIME_HHMMSS);

        // Build description (40 chars)
        String lastStmtCompact = today.format(DATE_DDMMYYYY);
        LocalDate nextStmtDate =
                NextStatementDateCalculator.calculateNextStatementDate(today);
        String nextStmtCompact = nextStmtDate.format(DATE_DDMMYYYY);

        String description = buildProctranDescription(
                request.customerNumber(),
                request.accountType(),
                lastStmtCompact,
                nextStmtCompact
        );

        // Reference is the task number padded to 12 digits.
        // In Java, use the current thread ID as reference.
        String reference = String.format("%012d",
                Thread.currentThread().threadId());

        boolean written = processedTransactionRepository.insertTransaction(
                PROCTRAN_EYECATCHER,
                sortCode,
                accountNumber,
                dateDotted,
                timeStr,
                reference,
                PROCTRAN_TYPE_BRANCH_CREATE_ACCOUNT,
                description,
                BigDecimal.ZERO
        );

        if (!written) {
            logger.log(Level.SEVERE,
                    "CREACC: Failed to write PROCTRAN record for account {0}",
                    accountNumber);
        }
    }

    /**
     * Builds the 40-character PROCTRAN description field.
     * Mirrors lines 960-964 of CREACC.cbl.
     */
    static String buildProctranDescription(String customerNumber,
                                           String accountType,
                                           String lastStmtDate,
                                           String nextStmtDate) {
        StringBuilder desc = new StringBuilder(PROCTRAN_DESC_LENGTH);

        // Positions 1-10: customer number (padded/truncated to 10)
        desc.append(padRight(customerNumber, 10));

        // Positions 11-18: account type (padded/truncated to 8)
        desc.append(padRight(accountType, 8));

        // Positions 19-26: last statement date DDMMYYYY (8 chars)
        desc.append(padRight(lastStmtDate, 8));

        // Positions 27-34: next statement date DDMMYYYY (8 chars)
        desc.append(padRight(nextStmtDate, 8));

        // Positions 35-40: spaces (6 chars)
        desc.append("      ");

        return desc.toString();
    }

    private static String padRight(String value, int length) {
        if (value == null) {
            return " ".repeat(length);
        }
        if (value.length() >= length) {
            return value.substring(0, length);
        }
        return value + " ".repeat(length - value.length());
    }
}
