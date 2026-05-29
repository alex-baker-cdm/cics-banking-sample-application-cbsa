/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bankliberty.migrated.crecust;

import java.time.LocalDate;

/**
 * Output data from the Create Customer operation.
 * Mirrors the DFHCOMMAREA output fields from CRECUST.cpy.
 *
 * <pre>
 * COBOL COMMAREA fields (output):
 *   COMM-EYECATCHER        PIC X(4)      -> "CUST"
 *   COMM-SORTCODE           PIC 9(6)
 *   COMM-NUMBER             PIC 9(10)
 *   COMM-NAME               PIC X(60)
 *   COMM-ADDRESS            PIC X(160)
 *   COMM-DATE-OF-BIRTH      PIC 9(8)
 *   COMM-CREDIT-SCORE       PIC 999
 *   COMM-CS-REVIEW-DATE     PIC 9(8)     (DDMMYYYY)
 *   COMM-SUCCESS            PIC X        ('Y' or 'N')
 *   COMM-FAIL-CODE          PIC X
 * </pre>
 */
public record CreateCustomerResponse(
        String eyecatcher,
        String sortCode,
        long customerNumber,
        String name,
        String address,
        LocalDate dateOfBirth,
        int creditScore,
        LocalDate creditScoreReviewDate,
        boolean success,
        String failCode
) {

    public static final String EYECATCHER = "CUST";

    public static CreateCustomerResponse success(String sortCode,
                                                 long customerNumber,
                                                 String name,
                                                 String address,
                                                 LocalDate dateOfBirth,
                                                 int creditScore,
                                                 LocalDate creditScoreReviewDate) {
        return new CreateCustomerResponse(
                EYECATCHER, sortCode, customerNumber, name, address,
                dateOfBirth, creditScore, creditScoreReviewDate,
                true, " "
        );
    }

    public static CreateCustomerResponse failure(String failCode,
                                                 int creditScore,
                                                 LocalDate creditScoreReviewDate) {
        return new CreateCustomerResponse(
                null, null, 0, null, null, null,
                creditScore, creditScoreReviewDate,
                false, failCode
        );
    }
}
