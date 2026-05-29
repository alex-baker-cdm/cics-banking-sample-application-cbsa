/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from COBOL: CREACC.cbl CUSTOMER-ACCOUNT-COUNT SECTION
 */

package com.ibm.cics.cip.bank.migrated.creacc.service;

/**
 * Service for counting a customer's existing accounts.
 * Replaces the CICS LINK to program INQACCCU in the original COBOL.
 */
public interface AccountCountService {

    /**
     * Counts the number of accounts belonging to a customer.
     *
     * @param customerNumber the 10-digit customer number
     * @return the number of accounts, or -1 if an error occurred
     */
    int countAccounts(String customerNumber);
}
