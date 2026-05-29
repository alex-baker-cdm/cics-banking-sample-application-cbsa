/*
 *
 *    Copyright IBM Corp. 2023
 *
 */

package com.ibm.cics.cip.bank.migrated.cobol.inquireaccount;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents a single account record returned by the Inquire Account for
 * Customer operation. Corresponds to the ACCOUNT-DETAILS group in the
 * INQACCCU COMMAREA copybook.
 *
 * <p>Dates are stored as {@link LocalDate} rather than the COBOL
 * DDMMYYYY packed format to leverage Java's type-safe date handling.</p>
 */
public record AccountDetail(
        String eyeCatcher,
        String customerNumber,
        String sortCode,
        String accountNumber,
        String accountType,
        BigDecimal interestRate,
        LocalDate openedDate,
        int overdraftLimit,
        LocalDate lastStatementDate,
        LocalDate nextStatementDate,
        BigDecimal availableBalance,
        BigDecimal actualBalance
) {

    public static final String EYECATCHER_VALUE = "ACCT";

    public static final int MAX_ACCOUNTS_PER_CUSTOMER = 20;
}
