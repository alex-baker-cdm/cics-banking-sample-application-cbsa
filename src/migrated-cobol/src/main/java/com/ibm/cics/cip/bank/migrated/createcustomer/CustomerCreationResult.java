/*
 *
 *    Copyright IBM Corp. 2023
 *
 */

package com.ibm.cics.cip.bank.migrated.createcustomer;

/**
 * Result returned by the CRECUST subprogram, migrated from BNK1CCS.cbl
 * CRE-CUST-DATA section — SUBPGM-PARMS after the LINK call returns.
 *
 * @param success       true if customer was created
 * @param failCode      failure reason code (null if success): 'O' = too old,
 *                      'Y' = DOB in future, 'Z' = invalid DOB
 * @param sortCode      the assigned sort code (populated on success)
 * @param customerNumber the assigned customer number (populated on success)
 * @param creditScore   the computed credit score
 * @param csReviewDate  the credit-score review date as DDMMYYYY
 * @param birthDay      day of birth (echoed back)
 * @param birthMonth    month of birth (echoed back)
 * @param birthYear     year of birth (echoed back)
 * @param address       full address (echoed back, 160 chars)
 */
public record CustomerCreationResult(
        boolean success,
        String failCode,
        String sortCode,
        String customerNumber,
        int creditScore,
        String csReviewDate,
        int birthDay,
        int birthMonth,
        int birthYear,
        String address
) {

    public static final String FAIL_TOO_OLD = "O";
    public static final String FAIL_FUTURE_DOB = "Y";
    public static final String FAIL_INVALID_DOB = "Z";
}
