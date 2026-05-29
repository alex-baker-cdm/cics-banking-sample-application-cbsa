/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from COBOL: BNK1UAC.cbl — BNK1UAM BMS map fields
 *
 * Represents a BMS screen field with both data and length,
 * mirroring the COBOL fields like ACCNOI/ACCNOL, ACTYPEI/ACTYPEL, etc.
 */
package com.ibm.cics.cip.bank.migrated.bnk1uac;

public record ScreenField(String value, int length) {

    public ScreenField(String value) {
        this(value, value == null ? 0 : value.length());
    }

    public boolean isEmpty() {
        return length == 0 || value == null || value.isBlank();
    }
}
