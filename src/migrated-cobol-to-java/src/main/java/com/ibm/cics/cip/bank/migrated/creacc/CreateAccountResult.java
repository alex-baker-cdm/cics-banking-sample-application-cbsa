/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from COBOL: CREACC.cpy (DFHCOMMAREA output fields)
 */

package com.ibm.cics.cip.bank.migrated.creacc;

import java.math.BigDecimal;

/**
 * Result of the Create Account process.
 * Maps to the CREACC COMMAREA output fields defined in CREACC.cpy.
 *
 * @param success           COMM-SUCCESS: 'Y' if account created successfully
 * @param failCode          COMM-FAIL-CODE: failure reason code (see {@link FailCode})
 * @param eyecatcher        COMM-EYECATCHER: 'ACCT' on success
 * @param customerNumber    COMM-CUSTNO: customer number
 * @param sortCode          COMM-SORTCODE: sort code
 * @param accountNumber     COMM-NUMBER: newly assigned 8-digit account number
 * @param accountType       COMM-ACC-TYPE: account type
 * @param interestRate      COMM-INT-RT: interest rate
 * @param opened            COMM-OPENED: date opened (DDMMYYYY format)
 * @param overdraftLimit    COMM-OVERDR-LIM: overdraft limit
 * @param lastStatementDate COMM-LAST-STMT-DT: last statement date (DDMMYYYY)
 * @param nextStatementDate COMM-NEXT-STMT-DT: next statement date (DDMMYYYY)
 * @param availableBalance  COMM-AVAIL-BAL: available balance
 * @param actualBalance     COMM-ACT-BAL: actual balance
 */
public record CreateAccountResult(
        boolean success,
        String failCode,
        String eyecatcher,
        String customerNumber,
        String sortCode,
        String accountNumber,
        String accountType,
        BigDecimal interestRate,
        String opened,
        int overdraftLimit,
        String lastStatementDate,
        String nextStatementDate,
        BigDecimal availableBalance,
        BigDecimal actualBalance
) {

    public static CreateAccountResult failure(String failCode) {
        return new CreateAccountResult(
                false, failCode,
                null, null, null, null, null, null, null,
                0, null, null, null, null
        );
    }

    public static CreateAccountResult failure(String failCode,
                                              CreateAccountRequest request) {
        return new CreateAccountResult(
                false, failCode,
                null,
                request.customerNumber(),
                request.sortCode(),
                null,
                request.accountType(),
                request.interestRate(),
                null,
                request.overdraftLimit(),
                null, null,
                request.availableBalance(),
                request.actualBalance()
        );
    }
}
