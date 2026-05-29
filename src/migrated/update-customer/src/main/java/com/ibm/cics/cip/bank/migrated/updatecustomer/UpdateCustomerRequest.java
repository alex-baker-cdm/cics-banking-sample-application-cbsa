/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.migrated.updatecustomer;

/**
 * Immutable input corresponding to the DFHCOMMAREA defined by the
 * UPDCUST copybook (UPDCUST.cpy). Carries the fields supplied by
 * the presentation layer for a customer update operation.
 *
 * @param eyecatcher          PIC X(4)   - record eyecatcher
 * @param sortCode            PIC X(6)   - branch sort code
 * @param customerNumber      PIC X(10)  - customer number
 * @param customerName        PIC X(60)  - customer name (title + name)
 * @param customerAddress     PIC X(160) - customer address
 * @param dateOfBirth         PIC 9(8)   - date of birth (DDMMYYYY)
 * @param creditScore         PIC 9(3)   - credit score
 * @param creditScoreReviewDate PIC 9(8) - credit score review date (DDMMYYYY)
 */
public record UpdateCustomerRequest(
        String eyecatcher,
        String sortCode,
        String customerNumber,
        String customerName,
        String customerAddress,
        String dateOfBirth,
        int creditScore,
        String creditScoreReviewDate
) {
}
