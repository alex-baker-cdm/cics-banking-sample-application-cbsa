/*
 * Copyright IBM Corp. 2023
 *
 * Interface replacing the CICS LINK to INQACCCU program. Implementations
 * provide the actual data access (e.g. JDBC, REST, or in-memory for testing).
 */
package com.ibm.cics.cip.bank.migration.bnk1cca;

/**
 * Service interface for querying accounts by customer number.
 * Replaces the COBOL {@code EXEC CICS LINK PROGRAM(INQACCCU)} call.
 */
public interface AccountInquiryService {

    /**
     * Retrieves accounts belonging to the specified customer.
     *
     * @param customerNumber the 10-digit customer number to look up
     * @param maxAccounts    maximum number of accounts to return
     * @return the inquiry result containing account details and status flags
     * @throws AccountInquiryException if the underlying data access fails
     */
    AccountInquiryResult inquireAccountsByCustomer(String customerNumber, int maxAccounts);
}
