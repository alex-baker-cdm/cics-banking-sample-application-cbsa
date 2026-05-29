/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from COBOL copybook ACCDB2.cpy
 */
package com.ibm.cics.cip.bank.bankdata;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Represents an account record, equivalent to the COBOL ACCDB2 copybook
 * and the DB2 ACCOUNT table structure.
 */
public record AccountRecord(
        String eyecatcher,
        String customerNumber,
        String sortCode,
        String accountNumber,
        String accountType,
        BigDecimal interestRate,
        String openedDate,
        int overdraftLimit,
        String lastStatementDate,
        String nextStatementDate,
        BigDecimal availableBalance,
        BigDecimal actualBalance) {

    public static final String EYECATCHER_VALUE = "ACCT";

    public AccountRecord {
        Objects.requireNonNull(eyecatcher, "eyecatcher must not be null");
        Objects.requireNonNull(customerNumber, "customerNumber must not be null");
        Objects.requireNonNull(sortCode, "sortCode must not be null");
        Objects.requireNonNull(accountNumber, "accountNumber must not be null");
        Objects.requireNonNull(accountType, "accountType must not be null");
        Objects.requireNonNull(interestRate, "interestRate must not be null");
        Objects.requireNonNull(openedDate, "openedDate must not be null");
        Objects.requireNonNull(lastStatementDate, "lastStatementDate must not be null");
        Objects.requireNonNull(nextStatementDate, "nextStatementDate must not be null");
        Objects.requireNonNull(availableBalance, "availableBalance must not be null");
        Objects.requireNonNull(actualBalance, "actualBalance must not be null");
    }
}
