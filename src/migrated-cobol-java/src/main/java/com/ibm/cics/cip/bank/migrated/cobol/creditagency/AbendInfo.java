/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.migrated.cobol.creditagency;

/**
 * Immutable record representing the ABNDINFO-REC structure from the COBOL
 * copybook ABNDINFO.cpy. Captures diagnostic information when an abnormal
 * termination (abend) occurs.
 *
 * <p>Field layout matches the original COBOL structure:
 * <pre>
 * 01 ABNDINFO-REC.
 *    03 ABND-VSAM-KEY.
 *       05 ABND-UTIME-KEY      PIC S9(15) COMP-3.
 *       05 ABND-TASKNO-KEY     PIC 9(4).
 *    03 ABND-APPLID            PIC X(8).
 *    03 ABND-TRANID            PIC X(4).
 *    03 ABND-DATE              PIC X(10).
 *    03 ABND-TIME              PIC X(8).
 *    03 ABND-CODE              PIC X(4).
 *    03 ABND-PROGRAM           PIC X(8).
 *    03 ABND-RESPCODE          PIC S9(8).
 *    03 ABND-RESP2CODE         PIC S9(8).
 *    03 ABND-SQLCODE           PIC S9(8).
 *    03 ABND-FREEFORM          PIC X(600).
 * </pre>
 *
 * @param utimeKey     absolute time key
 * @param taskNumber   CICS task number
 * @param applId       CICS application identifier (max 8 chars)
 * @param tranId       CICS transaction identifier (max 4 chars)
 * @param date         formatted date string (DD.MM.YYYY)
 * @param time         formatted time string (HH:MM:SS)
 * @param abendCode    4-character abend code
 * @param program      program name that abended (max 8 chars)
 * @param respCode     CICS RESP code
 * @param resp2Code    CICS RESP2 code
 * @param sqlCode      SQL return code (0 if not applicable)
 * @param freeformText free-form diagnostic text (max 600 chars)
 */
public record AbendInfo(
        long utimeKey,
        int taskNumber,
        String applId,
        String tranId,
        String date,
        String time,
        String abendCode,
        String program,
        int respCode,
        int resp2Code,
        int sqlCode,
        String freeformText
) {

    public static final String DEFAULT_ABEND_CODE = "PLOP";
    public static final String PROGRAM_NAME = "CRDTAGY2";
    public static final int FREEFORM_MAX_LENGTH = 600;
}
