/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.migrated.cobol.updateaccount;

import java.math.BigDecimal;

/**
 * Represents the input fields for the Update Account operation.
 * Maps to the COBOL COMMAREA input fields defined in UPDACC.cpy.
 *
 * <p>Only accountType, interestRate, and overdraftLimit are updatable.
 * The accountNumber identifies the target record.
 *
 * @param accountNumber   the 8-digit account number (COMM-ACCNO)
 * @param accountType     the account type, max 8 chars (COMM-ACC-TYPE)
 * @param interestRate    the interest rate with 2 decimal places (COMM-INT-RATE)
 * @param overdraftLimit  the overdraft limit (COMM-OVERDRAFT)
 */
public record UpdateAccountRequest(
        String accountNumber,
        String accountType,
        BigDecimal interestRate,
        int overdraftLimit
) {
}
