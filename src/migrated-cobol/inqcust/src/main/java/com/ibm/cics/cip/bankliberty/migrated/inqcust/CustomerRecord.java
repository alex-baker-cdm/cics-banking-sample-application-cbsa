/*
 *
 *    Copyright IBM Corp. 2023
 *
 */

package com.ibm.cics.cip.bankliberty.migrated.inqcust;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Represents a customer record as stored in the data source.
 * Equivalent to the CUSTOMER copybook (CUSTOMER.cpy) in the original COBOL.
 *
 * @param eyecatcher       4-char record type marker (e.g. "CUST")
 * @param sortCode         6-digit bank sort code
 * @param customerNumber   10-digit customer identifier
 * @param name             customer full name (up to 60 chars)
 * @param address          customer address (up to 160 chars)
 * @param dateOfBirth      customer date of birth
 * @param creditScore      3-digit credit score (0-999)
 * @param creditScoreReviewDate  date the credit score was last reviewed
 */
public record CustomerRecord(
        String eyecatcher,
        int sortCode,
        long customerNumber,
        String name,
        String address,
        LocalDate dateOfBirth,
        int creditScore,
        LocalDate creditScoreReviewDate
) {
    public CustomerRecord {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(address, "address must not be null");
        Objects.requireNonNull(dateOfBirth, "dateOfBirth must not be null");
        Objects.requireNonNull(creditScoreReviewDate,
                "creditScoreReviewDate must not be null");
    }
}
