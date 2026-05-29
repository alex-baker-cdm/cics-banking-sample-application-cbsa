/*
 *    Copyright IBM Corp. 2023
 *
 *    Migrated from COBOL copybook CUSTOMER.cpy
 */
package com.ibm.cics.cip.bank.delcus;

import java.time.LocalDate;

/**
 * Represents a customer record as defined by the CUSTOMER VSAM file layout
 * (CUSTOMER.cpy copybook). Maps the COBOL CUSTOMER-RECORD structure.
 *
 * @param eyeCatcher      4-char eyecatcher, normally "CUST"
 * @param sortCode        6-digit sort code
 * @param customerNumber  10-digit customer number
 * @param name            customer name (up to 60 chars)
 * @param address         customer address (up to 160 chars)
 * @param dateOfBirth     customer date of birth
 * @param creditScore     3-digit credit score (0–999)
 * @param csReviewDate    credit-score review date
 */
public record CustomerRecord(
        String eyeCatcher,
        int sortCode,
        long customerNumber,
        String name,
        String address,
        LocalDate dateOfBirth,
        int creditScore,
        LocalDate csReviewDate
) {
    public static final String EYECATCHER_VALUE = "CUST";
}
