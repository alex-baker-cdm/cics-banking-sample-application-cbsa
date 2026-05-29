/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.migrated.createaccount;

import java.math.BigDecimal;

public record CreateAccountParams(
        String customerNumber,
        String sortCode,
        String accountNumber,
        String accountType,
        BigDecimal interestRate,
        int overdraftLimit) {
}
