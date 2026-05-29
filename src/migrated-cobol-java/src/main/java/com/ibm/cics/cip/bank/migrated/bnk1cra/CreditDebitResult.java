/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from COBOL: BNK1CRA.cbl
 * Original Author: Jon Collett
 */
package com.ibm.cics.cip.bank.migrated.bnk1cra;

import java.math.BigDecimal;

/**
 * Result of a credit/debit operation.
 * Maps to the output fields populated by the UPD-CRED-DATA section in COBOL.
 *
 * @param success          whether the operation succeeded
 * @param message          human-readable result message
 * @param accountNumber    the account number processed
 * @param sortCode         the sort code returned by the account service
 * @param availableBalance the available balance after the operation
 * @param actualBalance    the actual balance after the operation
 */
public record CreditDebitResult(
        boolean success,
        String message,
        String accountNumber,
        String sortCode,
        BigDecimal availableBalance,
        BigDecimal actualBalance
) {

    public static CreditDebitResult failure(String message) {
        return new CreditDebitResult(false, message, null, null, null, null);
    }

    public static CreditDebitResult success(String message, String accountNumber,
                                            String sortCode, BigDecimal availableBalance,
                                            BigDecimal actualBalance) {
        return new CreditDebitResult(true, message, accountNumber, sortCode,
                availableBalance, actualBalance);
    }
}
