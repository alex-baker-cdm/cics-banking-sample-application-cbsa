/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.migrated.createaccount;

import java.util.Arrays;
import java.util.Optional;

public enum AccountType {

    ISA("ISA"),
    CURRENT("CURRENT"),
    LOAN("LOAN"),
    SAVING("SAVING"),
    MORTGAGE("MORTGAGE");

    private final String value;

    AccountType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Optional<AccountType> fromString(String input) {
        if (input == null) {
            return Optional.empty();
        }
        String normalized = input.replace('_', ' ').trim().toUpperCase();
        return Arrays.stream(values())
                .filter(type -> type.value.equals(normalized))
                .findFirst();
    }
}
