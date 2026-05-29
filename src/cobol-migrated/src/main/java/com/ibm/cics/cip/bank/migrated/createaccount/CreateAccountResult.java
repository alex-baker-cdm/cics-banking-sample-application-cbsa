/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.migrated.createaccount;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateAccountResult(
        boolean success,
        String failureMessage,
        String customerNumber,
        String sortCode,
        String accountNumber,
        String accountType,
        BigDecimal interestRate,
        int overdraftLimit,
        LocalDate dateOpened,
        LocalDate lastStatementDate,
        LocalDate nextStatementDate,
        BigDecimal availableBalance,
        BigDecimal actualBalance) {

    public static CreateAccountResult failure(String message) {
        return new CreateAccountResult(
                false, message, null, null, null, null,
                null, 0, null, null, null, null, null);
    }

    public static CreateAccountResult success(
            String customerNumber,
            String sortCode,
            String accountNumber,
            String accountType,
            BigDecimal interestRate,
            int overdraftLimit,
            LocalDate dateOpened,
            LocalDate lastStatementDate,
            LocalDate nextStatementDate,
            BigDecimal availableBalance,
            BigDecimal actualBalance) {
        return new CreateAccountResult(
                true,
                null,
                customerNumber,
                sortCode,
                accountNumber,
                accountType,
                interestRate,
                overdraftLimit,
                dateOpened,
                lastStatementDate,
                nextStatementDate,
                availableBalance,
                actualBalance);
    }
}
