/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bankliberty.migrated.transferfunds;

import java.math.BigDecimal;

/**
 * Immutable result of a fund transfer operation.
 * <p>
 * Migrated from the COBOL DFHCOMMAREA (XFRFUN.cpy) output fields:
 * COMM-SUCCESS, COMM-FAIL-CODE, COMM-FAVBAL, COMM-FACTBAL,
 * COMM-TAVBAL, COMM-TACTBAL.
 */
public record TransferFundsResult(
        boolean success,
        FailCode failCode,
        BigDecimal fromAvailableBalance,
        BigDecimal fromActualBalance,
        BigDecimal toAvailableBalance,
        BigDecimal toActualBalance
) {

    /**
     * Fail codes corresponding to the COBOL COMM-FAIL-CODE values.
     * <ul>
     *   <li>NONE - no failure</li>
     *   <li>FROM_ACCOUNT_NOT_FOUND - '1': the source (FROM) account was not found</li>
     *   <li>TO_ACCOUNT_NOT_FOUND - '2': the target (TO) account was not found</li>
     *   <li>OTHER_SQL_ERROR - '3': a database error occurred</li>
     *   <li>INVALID_AMOUNT - '4': the transfer amount was zero or negative</li>
     * </ul>
     */
    public enum FailCode {
        NONE,
        FROM_ACCOUNT_NOT_FOUND,
        TO_ACCOUNT_NOT_FOUND,
        OTHER_SQL_ERROR,
        INVALID_AMOUNT
    }

    public static TransferFundsResult success(
            BigDecimal fromAvailableBalance,
            BigDecimal fromActualBalance,
            BigDecimal toAvailableBalance,
            BigDecimal toActualBalance) {
        return new TransferFundsResult(
                true, FailCode.NONE,
                fromAvailableBalance, fromActualBalance,
                toAvailableBalance, toActualBalance);
    }

    public static TransferFundsResult failure(FailCode failCode) {
        return new TransferFundsResult(
                false, failCode,
                BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
