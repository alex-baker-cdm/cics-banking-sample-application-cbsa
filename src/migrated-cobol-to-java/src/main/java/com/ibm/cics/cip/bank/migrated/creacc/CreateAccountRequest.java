/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from COBOL: CREACC.cpy (DFHCOMMAREA input fields)
 */

package com.ibm.cics.cip.bank.migrated.creacc;

import java.math.BigDecimal;

/**
 * Input data for the Create Account process.
 * Maps to the CREACC COMMAREA input fields defined in CREACC.cpy.
 *
 * @param customerNumber  COMM-CUSTNO: 10-digit customer number
 * @param sortCode        COMM-SORTCODE: 6-digit sort code
 * @param accountType     COMM-ACC-TYPE: account type (up to 8 chars)
 * @param interestRate    COMM-INT-RT: interest rate with 2 decimal places
 * @param overdraftLimit  COMM-OVERDR-LIM: overdraft limit (8-digit integer)
 * @param availableBalance COMM-AVAIL-BAL: available balance
 * @param actualBalance   COMM-ACT-BAL: actual balance
 */
public record CreateAccountRequest(
        String customerNumber,
        String sortCode,
        String accountType,
        BigDecimal interestRate,
        int overdraftLimit,
        BigDecimal availableBalance,
        BigDecimal actualBalance
) {
}
