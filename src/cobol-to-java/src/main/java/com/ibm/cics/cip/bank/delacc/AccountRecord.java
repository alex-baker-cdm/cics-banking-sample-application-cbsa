/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from COBOL copybook ACCOUNT.cpy
 * Represents a bank account record in the ACCOUNT datastore.
 */
package com.ibm.cics.cip.bank.delacc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public record AccountRecord(
    String eyeCatcher,
    String customerNumber,
    String sortCode,
    String accountNumber,
    String accountType,
    BigDecimal interestRate,
    LocalDate opened,
    int overdraftLimit,
    LocalDate lastStatementDate,
    LocalDate nextStatementDate,
    BigDecimal availableBalance,
    BigDecimal actualBalance
) {

    public static final String EYECATCHER_VALUE = "ACCT";

    public AccountRecord {
        Objects.requireNonNull(customerNumber, "customerNumber must not be null");
        Objects.requireNonNull(sortCode, "sortCode must not be null");
        Objects.requireNonNull(accountNumber, "accountNumber must not be null");
    }
}
