/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from COBOL copybook DELACC.cpy (DELACC-COMMAREA input fields).
 * Carries the account number to be deleted.
 */
package com.ibm.cics.cip.bank.delacc;

import java.util.Objects;

public record DeleteAccountRequest(
    String accountNumber
) {

    public DeleteAccountRequest {
        Objects.requireNonNull(accountNumber, "accountNumber must not be null");
        if (accountNumber.length() != 8) {
            throw new IllegalArgumentException(
                "accountNumber must be exactly 8 digits, got: " + accountNumber);
        }
    }
}
