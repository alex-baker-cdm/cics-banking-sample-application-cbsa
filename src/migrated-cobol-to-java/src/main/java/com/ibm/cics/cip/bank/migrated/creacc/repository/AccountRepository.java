/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from COBOL: CREACC.cbl WRITE-ACCOUNT-DB2 SECTION
 * (DB2 ACCOUNT table INSERT)
 */

package com.ibm.cics.cip.bank.migrated.creacc.repository;

import java.math.BigDecimal;

/**
 * Repository for the DB2 ACCOUNT table.
 * Replaces the SQL INSERT operation in the WRITE-ACCOUNT-DB2 section
 * of CREACC.cbl.
 */
public interface AccountRepository {

    /**
     * Inserts a new account record into the ACCOUNT table.
     *
     * @param eyecatcher      record eyecatcher ('ACCT')
     * @param customerNumber  10-character customer number
     * @param sortCode        6-character sort code
     * @param accountNumber   8-character account number
     * @param accountType     account type (up to 8 chars)
     * @param interestRate    interest rate
     * @param opened          date opened (DD.MM.YYYY)
     * @param overdraftLimit  overdraft limit
     * @param lastStatement   last statement date (DD.MM.YYYY)
     * @param nextStatement   next statement date (DD.MM.YYYY)
     * @param availableBalance available balance
     * @param actualBalance   actual balance
     * @return true if the insert was successful
     */
    boolean insertAccount(
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
    );
}
