/*
 *    Copyright IBM Corp. 2023
 *
 *    Migrated from COBOL source: src/base/cobol_src/DELCUS.cbl
 *    Original author: Jon Collett
 *
 *    This program takes an incoming customer number, retrieves the
 *    associated accounts, deletes them one at a time (writing a
 *    PROCTRAN delete-account record for each), then deletes the
 *    customer record and writes a PROCTRAN customer-delete record.
 *
 *    If a failure occurs after deletions have started, the program
 *    abends (throws DeleteCustomerException) to avoid data
 *    inconsistency. The only non-fatal failure is when an account
 *    to be deleted has already been removed by another process.
 */
package com.ibm.cics.cip.bank.delcus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Java 21 equivalent of the COBOL DELCUS program.
 * <p>
 * Orchestrates the deletion of a customer and all associated accounts,
 * recording each deletion in the PROCTRAN (Processed Transaction) DB2 table.
 * <p>
 * Dependencies are injected via constructor to decouple from CICS/VSAM/DB2
 * infrastructure and enable unit testing.
 */
public final class DeleteCustomer {

    private static final Logger logger =
            Logger.getLogger(DeleteCustomer.class.getName());

    /** Default sort code from SORTCODE.cpy (77 SORTCODE PIC 9(6) VALUE 987654). */
    static final int DEFAULT_SORT_CODE = 987654;

    /** PROCTRAN transaction type for customer deletion (ODC = Original Delete Customer). */
    static final String PROCTRAN_TYPE_DELETE_CUSTOMER = "ODC";

    /** PROCTRAN eyecatcher. */
    static final String PROCTRAN_EYECATCHER = "PRTR";

    /** Maximum number of accounts that can be returned by INQACCCU. */
    static final int MAX_ACCOUNTS = 20;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HHmmss");

    private final CustomerDataAccess customerDataAccess;
    private final AccountDataAccess accountDataAccess;
    private final ProcessedTransactionDataAccess proctranDataAccess;
    private final String applId;

    /**
     * @param customerDataAccess  data access for customer inquiry and deletion
     * @param accountDataAccess   data access for account inquiry and deletion
     * @param proctranDataAccess  data access for PROCTRAN DB2 writes
     * @param applId              application identifier (CICS APPLID equivalent)
     */
    public DeleteCustomer(
            CustomerDataAccess customerDataAccess,
            AccountDataAccess accountDataAccess,
            ProcessedTransactionDataAccess proctranDataAccess,
            String applId) {
        this.customerDataAccess = customerDataAccess;
        this.accountDataAccess = accountDataAccess;
        this.proctranDataAccess = proctranDataAccess;
        this.applId = applId;
    }

    /**
     * Executes the delete-customer process.
     * <p>
     * Workflow (mirrors COBOL PREMIERE SECTION):
     * <ol>
     *   <li>Inquire about the customer via INQCUST — if not found, return failure.</li>
     *   <li>Get all accounts for the customer via INQACCCU.</li>
     *   <li>For each account, LINK to DELACC to delete it.</li>
     *   <li>Read and delete the CUSTOMER VSAM record.</li>
     *   <li>Write the customer-deletion record to PROCTRAN DB2.</li>
     *   <li>Populate the commarea with the deleted customer's details.</li>
     * </ol>
     *
     * @param commarea the commarea containing the customer number on input;
     *                 populated with deleted customer details on output
     * @throws DeleteCustomerException if a fatal error occurs after deletions
     *         have started (equivalent to EXEC CICS ABEND)
     */
    public void execute(DeleteCustomerCommarea commarea) throws DeleteCustomerException {
        logger.entering(this.getClass().getName(), "execute");

        String customerNumber = commarea.getCustomerNumber();
        String sortCode = padLeft(String.valueOf(DEFAULT_SORT_CODE), 6, '0');

        // Step 1: Inquire about the customer (EXEC CICS LINK PROGRAM('INQCUST'))
        CustomerInquiryResult inquiryResult =
                customerDataAccess.inquireCustomer(customerNumber);

        if (!inquiryResult.success()) {
            commarea.setDeleteSuccess(false);
            commarea.setDeleteFailCode(inquiryResult.failCode());
            logger.log(Level.INFO, () ->
                    "Customer inquiry failed for customer " + customerNumber
                            + ", failCode=" + inquiryResult.failCode());
            return;
        }

        // Step 2: Get all accounts for the customer (EXEC CICS LINK PROGRAM('INQACCCU'))
        AccountInquiryResult accountResult =
                accountDataAccess.getAccountsByCustomer(customerNumber);

        // Step 3: Delete each account (EXEC CICS LINK PROGRAM('DELACC'))
        if (accountResult.numberOfAccounts() > 0) {
            deleteAccounts(accountResult);
        }

        // Step 4: Read and delete the CUSTOMER VSAM record
        Optional<CustomerRecord> deletedCustomer =
                customerDataAccess.readAndDeleteCustomer(sortCode, customerNumber);

        if (deletedCustomer.isEmpty()) {
            // Customer was already deleted by someone else — equivalent to NOTFND branch
            // in COBOL (GO TO DCV999). We still mark success per original behavior
            // since after deleting accounts we proceed to return success.
            commarea.setDeleteSuccess(true);
            commarea.setDeleteFailCode(" ");
            return;
        }

        CustomerRecord customer = deletedCustomer.get();

        // Step 5: Populate commarea with customer details
        populateCommarea(commarea, customer, sortCode);

        // Step 6: Write PROCTRAN customer-deletion record
        writeProctranCustomerDeletion(customer, sortCode);

        commarea.setDeleteSuccess(true);
        commarea.setDeleteFailCode(" ");

        logger.exiting(this.getClass().getName(), "execute");
    }

    /**
     * Deletes all accounts in the inquiry result.
     * Mirrors the DELETE-ACCOUNTS SECTION in the COBOL program.
     */
    private void deleteAccounts(AccountInquiryResult accountResult) {
        logger.entering(this.getClass().getName(), "deleteAccounts");

        for (int i = 0; i < accountResult.numberOfAccounts(); i++) {
            AccountRecord account = accountResult.accounts().get(i);
            accountDataAccess.deleteAccount(account.accountNumber(), applId);
        }

        logger.exiting(this.getClass().getName(), "deleteAccounts");
    }

    /**
     * Populates the commarea with the deleted customer's details.
     * Mirrors the data moves in DEL-CUST-VSAM after successful VSAM read.
     */
    private void populateCommarea(
            DeleteCustomerCommarea commarea,
            CustomerRecord customer,
            String sortCode) {

        commarea.setEyeCatcher(customer.eyeCatcher());
        commarea.setSortCode(sortCode);
        commarea.setCustomerNumber(
                padLeft(String.valueOf(customer.customerNumber()), 10, '0'));
        commarea.setName(customer.name());
        commarea.setAddress(customer.address());

        LocalDate dob = customer.dateOfBirth();
        commarea.setBirthDay(padLeft(String.valueOf(dob.getDayOfMonth()), 2, '0'));
        commarea.setBirthMonth(padLeft(String.valueOf(dob.getMonthValue()), 2, '0'));
        commarea.setBirthYear(String.valueOf(dob.getYear()));

        commarea.setCreditScore(customer.creditScore());

        LocalDate reviewDate = customer.csReviewDate();
        commarea.setCsReviewDay(
                padLeft(String.valueOf(reviewDate.getDayOfMonth()), 2, '0'));
        commarea.setCsReviewMonth(
                padLeft(String.valueOf(reviewDate.getMonthValue()), 2, '0'));
        commarea.setCsReviewYear(String.valueOf(reviewDate.getYear()));
    }

    /**
     * Writes a PROCTRAN record for the customer deletion.
     * Mirrors the WRITE-PROCTRAN-CUST-DB2 SECTION.
     * <p>
     * The description field layout (from PROCTRAN.cpy PROC-TRAN-DESC-DELCUS):
     * <pre>
     *   Positions 1-6:   sort code
     *   Positions 7-16:  customer number
     *   Positions 17-30: customer name (first 14 chars)
     *   Positions 31-40: date of birth (DD/MM/YYYY)
     * </pre>
     * The COBOL code uses the stored customer data layout:
     * <pre>
     *   HV-PROCTRAN-DESC(1:6)   = WS-STOREDC-SORTCODE
     *   HV-PROCTRAN-DESC(7:10)  = WS-STOREDC-NUMBER
     *   HV-PROCTRAN-DESC(17:14) = WS-STOREDC-NAME
     *   HV-PROCTRAN-DESC(31:10) = WS-STOREDC-DATE-OF-BIRTH
     * </pre>
     */
    private void writeProctranCustomerDeletion(
            CustomerRecord customer,
            String sortCode) throws DeleteCustomerException {

        LocalDate now = LocalDate.now();
        LocalTime timeNow = LocalTime.now();

        String formattedDate = now.format(DATE_FORMATTER);
        String formattedTime = timeNow.format(TIME_FORMATTER);

        String taskReference = padLeft("0", 12, '0');

        String custNumStr = padLeft(String.valueOf(customer.customerNumber()), 10, '0');
        String nameForDesc = customer.name() != null
                ? customer.name().length() > 14
                        ? customer.name().substring(0, 14)
                        : padRight(customer.name(), 14, ' ')
                : padRight("", 14, ' ');

        LocalDate dob = customer.dateOfBirth();
        String dobString = padLeft(String.valueOf(dob.getDayOfMonth()), 2, '0')
                + "/" + padLeft(String.valueOf(dob.getMonthValue()), 2, '0')
                + "/" + dob.getYear();

        String description = buildDescription(sortCode, custNumStr, nameForDesc, dobString);

        proctranDataAccess.writeProcessedTransaction(
                PROCTRAN_EYECATCHER,
                sortCode,
                padLeft("0", 8, '0'),
                formattedDate,
                formattedTime,
                taskReference,
                PROCTRAN_TYPE_DELETE_CUSTOMER,
                description,
                BigDecimal.ZERO
        );
    }

    /**
     * Builds the 40-character PROCTRAN description field.
     * Layout matches PROC-TRAN-DESC-DELCUS in PROCTRAN.cpy.
     */
    static String buildDescription(
            String sortCode,
            String customerNumber,
            String name,
            String dateOfBirth) {

        StringBuilder desc = new StringBuilder(40);
        desc.append(padRight(sortCode, 6, ' '));
        desc.append(padRight(customerNumber, 10, ' '));
        desc.append(padRight(name, 14, ' '));
        desc.append(padRight(dateOfBirth, 10, ' '));
        return desc.length() > 40 ? desc.substring(0, 40) : desc.toString();
    }

    static String padLeft(String value, int length, char padChar) {
        if (value == null) {
            value = "";
        }
        if (value.length() >= length) {
            return value;
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = value.length(); i < length; i++) {
            sb.append(padChar);
        }
        sb.append(value);
        return sb.toString();
    }

    static String padRight(String value, int length, char padChar) {
        if (value == null) {
            value = "";
        }
        if (value.length() >= length) {
            return value.substring(0, length);
        }
        StringBuilder sb = new StringBuilder(length);
        sb.append(value);
        for (int i = value.length(); i < length; i++) {
            sb.append(padChar);
        }
        return sb.toString();
    }
}
