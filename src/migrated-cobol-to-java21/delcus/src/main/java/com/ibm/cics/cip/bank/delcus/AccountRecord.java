/*
 *    Copyright IBM Corp. 2023
 *
 *    Migrated from COBOL copybook INQACCCU.cpy (account details within INQACCCU COMMAREA)
 */
package com.ibm.cics.cip.bank.delcus;

import java.math.BigDecimal;

/**
 * Represents a single account detail entry as returned by the INQACCCU
 * program. Maps the ACCOUNT-DETAILS OCCURS structure in INQACCCU.cpy.
 *
 * @param eyeCatcher      4-char eyecatcher
 * @param customerNumber  customer number (10 chars)
 * @param sortCode        sort code (6 chars)
 * @param accountNumber   account number (8 digits)
 * @param accountType     account type (8 chars)
 * @param interestRate    interest rate (9(4)V99)
 * @param opened          opened date as 8-digit int (DDMMYYYY)
 * @param overdraftLimit  overdraft limit
 * @param lastStatementDate last statement date as 8-digit int
 * @param nextStatementDate next statement date as 8-digit int
 * @param availableBalance  available balance
 * @param actualBalance     actual balance
 */
public record AccountRecord(
        String eyeCatcher,
        String customerNumber,
        String sortCode,
        int accountNumber,
        String accountType,
        BigDecimal interestRate,
        int opened,
        int overdraftLimit,
        int lastStatementDate,
        int nextStatementDate,
        BigDecimal availableBalance,
        BigDecimal actualBalance
) {
}
