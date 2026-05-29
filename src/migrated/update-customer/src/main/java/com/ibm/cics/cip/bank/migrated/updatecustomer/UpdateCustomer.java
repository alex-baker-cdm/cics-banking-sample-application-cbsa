/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.migrated.updatecustomer;

import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Java 21 migration of the UPDCUST.cbl COBOL program.
 *
 * <p>This program updates an existing customer's name and/or address
 * in the data store. The presentation layer is responsible for field
 * validation; this class validates the title portion of the customer
 * name and ensures that at least one of name or address is supplied
 * before persisting changes.
 *
 * <p>Because only a limited set of customer fields may be changed,
 * no record is written to PROCTRAN (the processed-transactions log).
 *
 * <p>Equivalent COBOL source: {@code src/base/cobol_src/UPDCUST.cbl}
 *
 * @see CustomerDataStore
 * @see UpdateCustomerRequest
 * @see UpdateCustomerResult
 */
public final class UpdateCustomer {

    private static final Logger logger =
            Logger.getLogger(UpdateCustomer.class.getName());

    /** Default sort code from the SORTCODE copybook (PIC 9(6) VALUE 987654). */
    static final String DEFAULT_SORT_CODE = "987654";

    /** Failure code: invalid title in customer name. */
    static final String FAIL_INVALID_TITLE = "T";

    /** Failure code: customer not found. */
    static final String FAIL_NOT_FOUND = "1";

    /** Failure code: data store read error. */
    static final String FAIL_READ_ERROR = "2";

    /** Failure code: data store write (rewrite) error. */
    static final String FAIL_WRITE_ERROR = "3";

    /** Failure code: both name and address empty/blank-leading. */
    static final String FAIL_EMPTY_NAME_AND_ADDRESS = "4";

    /**
     * The set of valid titles that may appear as the first word of the
     * customer name. An empty/blank title is also accepted.
     *
     * <p>Corresponds to the EVALUATE WS-UNSTR-TITLE block in the COBOL
     * source (lines 153-189).
     */
    static final Set<String> VALID_TITLES = Set.of(
            "Professor", "Mr", "Mrs", "Miss", "Ms",
            "Dr", "Drs", "Lord", "Sir", "Lady", ""
    );

    private final CustomerDataStore dataStore;

    public UpdateCustomer(CustomerDataStore dataStore) {
        this.dataStore = dataStore;
    }

    /**
     * Executes the update-customer operation, preserving the exact
     * business logic of the original COBOL program.
     *
     * <p>Processing steps:
     * <ol>
     *   <li>Validate the title (first word of the customer name)</li>
     *   <li>Read the existing customer record from the data store</li>
     *   <li>Determine which fields to update (name, address, or both)</li>
     *   <li>Persist the updated record</li>
     *   <li>Return the updated record data on success</li>
     * </ol>
     *
     * @param request the update request (COMMAREA input)
     * @return the result of the operation
     */
    public UpdateCustomerResult execute(UpdateCustomerRequest request) {
        String sortCode = DEFAULT_SORT_CODE;

        if (!isTitleValid(request.customerName())) {
            logger.log(Level.INFO,
                    "Update rejected: invalid title in customer name");
            return new UpdateCustomerResult.Failure(FAIL_INVALID_TITLE);
        }

        return updateCustomerInDataStore(sortCode, request);
    }

    /**
     * Extracts the title (first space-delimited token) from the customer
     * name and checks it against the set of valid titles.
     *
     * <p>Mirrors the COBOL logic:
     * <pre>
     *   UNSTRING COMM-NAME DELIMITED BY SPACE INTO WS-UNSTR-TITLE
     *   EVALUATE WS-UNSTR-TITLE ...
     * </pre>
     */
    boolean isTitleValid(String customerName) {
        if (customerName == null) {
            return true;
        }
        String title = extractTitle(customerName);
        return VALID_TITLES.contains(title);
    }

    /**
     * Extracts the first space-delimited word from the customer name,
     * equivalent to {@code UNSTRING COMM-NAME DELIMITED BY SPACE INTO
     * WS-UNSTR-TITLE} in COBOL.
     */
    static String extractTitle(String customerName) {
        if (customerName == null || customerName.isEmpty()) {
            return "";
        }
        int spaceIndex = customerName.indexOf(' ');
        if (spaceIndex < 0) {
            return customerName;
        }
        return customerName.substring(0, spaceIndex);
    }

    /**
     * Reads the customer record, applies the update logic, and rewrites it.
     *
     * <p>Corresponds to the UPDATE-CUSTOMER-VSAM section in the COBOL source.
     */
    private UpdateCustomerResult updateCustomerInDataStore(
            String sortCode, UpdateCustomerRequest request) {

        String customerNumber = request.customerNumber();

        Optional<CustomerRecord> optionalRecord;
        try {
            optionalRecord = dataStore.readForUpdate(sortCode, customerNumber);
        } catch (DataStoreException e) {
            logger.log(Level.WARNING,
                    "Data store error reading customer {0}: {1}",
                    new Object[]{customerNumber, e.getMessage()});
            return new UpdateCustomerResult.Failure(FAIL_READ_ERROR);
        }

        if (optionalRecord.isEmpty()) {
            logger.log(Level.INFO, "Customer not found: {0}", customerNumber);
            return new UpdateCustomerResult.Failure(FAIL_NOT_FOUND);
        }

        CustomerRecord record = optionalRecord.get();

        String commName = request.customerName();
        String commAddr = request.customerAddress();

        boolean nameEmpty = isFieldEmptyOrLeadingSpace(commName);
        boolean addrEmpty = isFieldEmptyOrLeadingSpace(commAddr);

        if (nameEmpty && addrEmpty) {
            return new UpdateCustomerResult.Failure(
                    FAIL_EMPTY_NAME_AND_ADDRESS);
        }

        if (nameEmpty && !addrEmpty) {
            record.setCustomerAddress(commAddr);
        } else if (addrEmpty && !nameEmpty) {
            record.setCustomerName(commName);
        } else {
            record.setCustomerAddress(commAddr);
            record.setCustomerName(commName);
        }

        try {
            dataStore.rewrite(record);
        } catch (DataStoreException e) {
            logger.log(Level.WARNING,
                    "Data store error rewriting customer {0}: {1}",
                    new Object[]{customerNumber, e.getMessage()});
            return new UpdateCustomerResult.Failure(FAIL_WRITE_ERROR);
        }

        return new UpdateCustomerResult.Success(
                record.getEyecatcher(),
                record.getSortCode(),
                record.getCustomerNumber(),
                record.getCustomerName(),
                record.getCustomerAddress(),
                record.getDateOfBirth(),
                record.getCreditScore(),
                record.getCreditScoreReviewDate()
        );
    }

    /**
     * Checks whether a field is considered empty in the same way the
     * COBOL program checks:
     * <pre>
     *   IF (field = SPACES OR field(1:1) = ' ')
     * </pre>
     *
     * A field is "empty" if it is null, blank, or starts with a space.
     */
    static boolean isFieldEmptyOrLeadingSpace(String field) {
        return field == null || field.isEmpty() || field.isBlank()
                || field.charAt(0) == ' ';
    }
}
