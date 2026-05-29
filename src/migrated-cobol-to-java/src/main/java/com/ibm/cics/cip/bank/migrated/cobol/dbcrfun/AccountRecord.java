/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.migrated.cobol.dbcrfun;

import java.math.BigDecimal;

/**
 * Represents an account row from the ACCOUNT DB2 table, mapped from
 * the HOST-ACCOUNT-ROW structure in DBCRFUN.cbl and the ACCOUNT.cpy
 * copybook.
 *
 * @param eyecatcher       account eyecatcher (e.g. "ACCT")
 * @param customerNumber   customer number (10 chars)
 * @param sortCode         sort code (6 digits)
 * @param accountNumber    account number (8 digits)
 * @param accountType      account type (e.g. "MORTGAGE", "LOAN", "ISA", "SAVING", "CURRENT")
 * @param interestRate     interest rate
 * @param opened           date opened (YYYY-MM-DD format)
 * @param overdraftLimit   overdraft limit
 * @param lastStatement    last statement date
 * @param nextStatement    next statement date
 * @param availableBalance available balance
 * @param actualBalance    actual balance
 */
public record AccountRecord(
        String eyecatcher,
        String customerNumber,
        String sortCode,
        String accountNumber,
        String accountType,
        BigDecimal interestRate,
        String opened,
        int overdraftLimit,
        String lastStatement,
        String nextStatement,
        BigDecimal availableBalance,
        BigDecimal actualBalance
) {

    public AccountRecord withUpdatedBalances(BigDecimal newAvailableBalance,
            BigDecimal newActualBalance) {
        return new AccountRecord(
                eyecatcher, customerNumber, sortCode, accountNumber,
                accountType, interestRate, opened, overdraftLimit,
                lastStatement, nextStatement,
                newAvailableBalance, newActualBalance
        );
    }

    public boolean isMortgage() {
        return "MORTGAGE".equals(accountType);
    }

    public boolean isLoan() {
        return "LOAN".equals(accountType != null ? accountType.trim() : null);
    }
}
