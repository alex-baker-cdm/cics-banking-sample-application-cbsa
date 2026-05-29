/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.migrated.cobol.dbcrfun;

import java.math.BigDecimal;

/**
 * Result returned by the credit/debit operation, mapped from the
 * DFHCOMMAREA (PAYDBCR.cpy) output fields in DBCRFUN.cbl.
 *
 * @param success          whether the operation succeeded
 * @param failCode         the failure code if not successful
 * @param availableBalance the updated available balance (after the operation)
 * @param actualBalance    the updated actual balance (after the operation)
 * @param sortCode         the sort code used for the operation
 */
public record CreditDebitResult(
        boolean success,
        FailCode failCode,
        BigDecimal availableBalance,
        BigDecimal actualBalance,
        String sortCode
) {

    public static CreditDebitResult failure(FailCode failCode, String sortCode) {
        return new CreditDebitResult(false, failCode, null, null, sortCode);
    }

    public static CreditDebitResult success(BigDecimal availableBalance,
            BigDecimal actualBalance, String sortCode) {
        return new CreditDebitResult(true, FailCode.NONE,
                availableBalance, actualBalance, sortCode);
    }
}
