/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from COBOL: CREACC.cbl ACCOUNT-TYPE-CHECK SECTION
 */

package com.ibm.cics.cip.bank.migrated.creacc;

/**
 * Valid account types supported by the banking system.
 * Mirrors the EVALUATE block in CREACC.cbl (ACCOUNT-TYPE-CHECK SECTION,
 * lines 1209-1228), which validates that only ISA, MORTGAGE, SAVING,
 * CURRENT, and LOAN are accepted.
 */
public enum AccountType {

    ISA("ISA", 3),
    MORTGAGE("MORTGAGE", 8),
    SAVING("SAVING", 6),
    CURRENT("CURRENT", 7),
    LOAN("LOAN", 4);

    private final String code;
    private final int matchLength;

    AccountType(String code, int matchLength) {
        this.code = code;
        this.matchLength = matchLength;
    }

    public String getCode() {
        return code;
    }

    /**
     * Validates an account type string using the same prefix-matching rules
     * as the original COBOL EVALUATE statement. Each type is checked by
     * comparing its specific prefix length against the input.
     *
     * @param value the account type string (up to 8 characters)
     * @return the matching AccountType, or null if invalid
     */
    public static AccountType fromString(String value) {
        if (value == null) {
            return null;
        }
        for (AccountType type : values()) {
            if (value.length() >= type.matchLength
                    && value.substring(0, type.matchLength).equals(type.code)) {
                return type;
            }
        }
        return null;
    }
}
