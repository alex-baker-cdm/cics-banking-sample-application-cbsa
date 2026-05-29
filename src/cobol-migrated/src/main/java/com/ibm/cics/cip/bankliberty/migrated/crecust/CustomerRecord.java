/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bankliberty.migrated.crecust;

import java.time.LocalDate;

/**
 * Represents a customer record in the CUSTOMER data store.
 * Mirrors the CUSTOMER.cpy copybook layout.
 *
 * <pre>
 * COBOL layout:
 *   CUSTOMER-EYECATCHER      PIC X(4)     "CUST"
 *   CUSTOMER-SORTCODE        PIC 9(6)
 *   CUSTOMER-NUMBER          PIC 9(10)
 *   CUSTOMER-NAME            PIC X(60)
 *   CUSTOMER-ADDRESS         PIC X(160)
 *   CUSTOMER-DATE-OF-BIRTH   PIC 9(8)     DDMMYYYY
 *   CUSTOMER-CREDIT-SCORE    PIC 999
 *   CUSTOMER-CS-REVIEW-DATE  PIC 9(8)     DDMMYYYY
 * </pre>
 */
public record CustomerRecord(
        String eyecatcher,
        String sortCode,
        long customerNumber,
        String name,
        String address,
        LocalDate dateOfBirth,
        int creditScore,
        LocalDate creditScoreReviewDate
) {
    public static final String EYECATCHER_VALUE = "CUST";
}
