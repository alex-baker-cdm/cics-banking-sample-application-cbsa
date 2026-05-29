/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from COBOL: CREACC.cbl LINK to INQCUST
 */

package com.ibm.cics.cip.bank.migrated.creacc.service;

/**
 * Service for validating customer existence.
 * Replaces the CICS LINK to program INQCUST in the original COBOL.
 */
public interface CustomerInquiryService {

    /**
     * Checks whether a customer with the given number exists.
     *
     * @param customerNumber the 10-digit customer number
     * @return true if the customer exists and was successfully retrieved
     */
    boolean customerExists(String customerNumber);
}
