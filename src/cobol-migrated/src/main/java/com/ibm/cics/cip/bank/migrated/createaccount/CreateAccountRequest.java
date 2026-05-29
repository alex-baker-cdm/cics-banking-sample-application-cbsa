/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.migrated.createaccount;

import java.math.BigDecimal;

public record CreateAccountRequest(
        String customerNumber,
        String accountType,
        BigDecimal interestRate,
        int overdraftLimit) {
}
