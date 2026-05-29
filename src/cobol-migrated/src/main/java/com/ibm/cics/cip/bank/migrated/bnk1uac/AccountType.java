/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from COBOL: BNK1UAC.cbl — VALIDATE-DATA SECTION
 */
package com.ibm.cics.cip.bank.migrated.bnk1uac;

public enum AccountType {

    CURRENT("CURRENT"),
    SAVING("SAVING"),
    LOAN("LOAN"),
    MORTGAGE("MORTGAGE"),
    ISA("ISA");

    private final String label;

    AccountType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public String paddedLabel() {
        return String.format("%-8s", label);
    }

    public static AccountType fromString(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        for (AccountType type : values()) {
            if (type.label.equalsIgnoreCase(trimmed)) {
                return type;
            }
        }
        return null;
    }

    public boolean requiresNonZeroInterest() {
        return this == LOAN || this == MORTGAGE;
    }
}
