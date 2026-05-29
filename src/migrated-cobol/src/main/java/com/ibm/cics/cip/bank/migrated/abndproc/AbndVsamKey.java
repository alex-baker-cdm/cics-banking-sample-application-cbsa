/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from COBOL copybook ABNDINFO.cpy - VSAM key portion.
 * Represents the composite key used for VSAM KSDS record identification.
 *
 * COBOL original:
 *   03 ABND-VSAM-KEY.
 *      05 ABND-UTIME-KEY    PIC S9(15) COMP-3.
 *      05 ABND-TASKNO-KEY   PIC 9(4).
 */
package com.ibm.cics.cip.bank.migrated.abndproc;

import java.util.Objects;

public record AbndVsamKey(long utimeKey, String tasknoKey) {

    private static final int TASKNO_LENGTH = 4;

    public AbndVsamKey {
        Objects.requireNonNull(tasknoKey, "tasknoKey must not be null");
        if (tasknoKey.length() > TASKNO_LENGTH) {
            throw new IllegalArgumentException(
                    "tasknoKey must be at most " + TASKNO_LENGTH + " characters, got " + tasknoKey.length());
        }
    }

    public String toKeyString() {
        return String.valueOf(utimeKey) + String.format("%-" + TASKNO_LENGTH + "s", tasknoKey);
    }
}
