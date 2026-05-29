/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from COBOL copybook ABNDINFO.cpy (src/base/cobol_copy/ABNDINFO.cpy).
 * Represents an abend (abnormal end) record written to the centralised
 * ABNDFILE data store so that application abends can be viewed from one place.
 *
 * COBOL field mapping:
 *   ABND-VSAM-KEY  -> vsamKey  (composite: utimeKey + tasknoKey)
 *   ABND-APPLID    -> applId       PIC X(8)
 *   ABND-TRANID    -> tranId       PIC X(4)
 *   ABND-DATE      -> date         PIC X(10)
 *   ABND-TIME      -> time         PIC X(8)
 *   ABND-CODE      -> code         PIC X(4)
 *   ABND-PROGRAM   -> program      PIC X(8)
 *   ABND-RESPCODE  -> respCode     PIC S9(8) DISPLAY SIGN LEADING SEPARATE
 *   ABND-RESP2CODE -> resp2Code    PIC S9(8) DISPLAY SIGN LEADING SEPARATE
 *   ABND-SQLCODE   -> sqlCode      PIC S9(8) DISPLAY SIGN LEADING SEPARATE
 *   ABND-FREEFORM  -> freeform     PIC X(600)
 */
package com.ibm.cics.cip.bank.migrated.abndproc;

import java.util.Objects;

public record AbndRecord(
        AbndVsamKey vsamKey,
        String applId,
        String tranId,
        String date,
        String time,
        String code,
        String program,
        int respCode,
        int resp2Code,
        int sqlCode,
        String freeform) {

    private static final int APPLID_MAX_LEN = 8;
    private static final int TRANID_MAX_LEN = 4;
    private static final int DATE_MAX_LEN = 10;
    private static final int TIME_MAX_LEN = 8;
    private static final int CODE_MAX_LEN = 4;
    private static final int PROGRAM_MAX_LEN = 8;
    private static final int FREEFORM_MAX_LEN = 600;

    public AbndRecord {
        Objects.requireNonNull(vsamKey, "vsamKey must not be null");
        applId = padOrTruncate(Objects.requireNonNull(applId, "applId must not be null"), APPLID_MAX_LEN);
        tranId = padOrTruncate(Objects.requireNonNull(tranId, "tranId must not be null"), TRANID_MAX_LEN);
        date = padOrTruncate(Objects.requireNonNull(date, "date must not be null"), DATE_MAX_LEN);
        time = padOrTruncate(Objects.requireNonNull(time, "time must not be null"), TIME_MAX_LEN);
        code = padOrTruncate(Objects.requireNonNull(code, "code must not be null"), CODE_MAX_LEN);
        program = padOrTruncate(Objects.requireNonNull(program, "program must not be null"), PROGRAM_MAX_LEN);
        freeform = padOrTruncate(Objects.requireNonNull(freeform, "freeform must not be null"), FREEFORM_MAX_LEN);
    }

    private static String padOrTruncate(String value, int maxLength) {
        if (value.length() > maxLength) {
            return value.substring(0, maxLength);
        }
        return value;
    }
}
