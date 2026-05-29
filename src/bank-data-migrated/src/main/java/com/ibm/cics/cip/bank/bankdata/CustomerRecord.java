/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from COBOL copybook CUSTOMER.cpy
 */
package com.ibm.cics.cip.bank.bankdata;

import java.util.Objects;

/**
 * Represents a customer record, equivalent to the COBOL CUSTOMER copybook
 * record structure used in the VSAM CUSTOMER file.
 */
public record CustomerRecord(
        String eyecatcher,
        String sortCode,
        String customerNumber,
        String name,
        String address,
        int birthDay,
        int birthMonth,
        int birthYear,
        int creditScore,
        int csReviewDay,
        int csReviewMonth,
        int csReviewYear) {

    public static final String EYECATCHER_VALUE = "CUST";

    public CustomerRecord {
        Objects.requireNonNull(eyecatcher, "eyecatcher must not be null");
        Objects.requireNonNull(sortCode, "sortCode must not be null");
        Objects.requireNonNull(customerNumber, "customerNumber must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(address, "address must not be null");
    }

    public String dateOfBirth() {
        return String.format("%02d%02d%04d", birthDay, birthMonth, birthYear);
    }

    public String csReviewDate() {
        return String.format("%02d%02d%04d", csReviewDay, csReviewMonth, csReviewYear);
    }
}
