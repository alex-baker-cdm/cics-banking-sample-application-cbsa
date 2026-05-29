/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from COBOL copybook INQACCCU.cpy - Account details structure
 * returned by the INQACCCU program for each account belonging to a customer.
 */
package com.ibm.cics.cip.bank.migration.bnk1cca;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents one account record returned by the account inquiry service.
 * Maps to the ACCOUNT-DETAILS OCCURS group in the INQACCCU copybook.
 *
 * @param eyeCatcher        4-char eye-catcher identifier (COMM-EYE)
 * @param customerNumber    10-digit customer number (COMM-CUSTNO)
 * @param sortCode          6-char sort code (COMM-SCODE)
 * @param accountNumber     8-digit account number (COMM-ACCNO)
 * @param accountType       8-char account type description (COMM-ACC-TYPE)
 * @param interestRate      interest rate with 2 decimal places (COMM-INT-RATE)
 * @param opened            date the account was opened (COMM-OPENED)
 * @param overdraft         overdraft limit (COMM-OVERDRAFT)
 * @param lastStatementDate date of last statement (COMM-LAST-STMT-DT)
 * @param nextStatementDate date of next statement (COMM-NEXT-STMT-DT)
 * @param availableBalance  available balance with 2 decimal places (COMM-AVAIL-BAL)
 * @param actualBalance     actual balance with 2 decimal places (COMM-ACTUAL-BAL)
 */
public record AccountDetail(
        String eyeCatcher,
        String customerNumber,
        String sortCode,
        int accountNumber,
        String accountType,
        BigDecimal interestRate,
        LocalDate opened,
        int overdraft,
        LocalDate lastStatementDate,
        LocalDate nextStatementDate,
        BigDecimal availableBalance,
        BigDecimal actualBalance
) {
}
