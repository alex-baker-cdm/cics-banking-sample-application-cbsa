/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.creditagency;

/**
 * Java 21 record mapping the COBOL ABNDINFO copybook used for abend
 * diagnostics.
 *
 * Maps to the COBOL copybook ABNDINFO:
 * <pre>
 *   01 ABNDINFO-REC.
 *       03 ABND-VSAM-KEY.
 *          05 ABND-UTIME-KEY    PIC S9(15) COMP-3.
 *          05 ABND-TASKNO-KEY   PIC 9(4).
 *       03 ABND-APPLID          PIC X(8).
 *       03 ABND-TRANID          PIC X(4).
 *       03 ABND-DATE            PIC X(10).
 *       03 ABND-TIME            PIC X(8).
 *       03 ABND-CODE            PIC X(4).
 *       03 ABND-PROGRAM         PIC X(8).
 *       03 ABND-RESPCODE        PIC S9(8).
 *       03 ABND-RESP2CODE       PIC S9(8).
 *       03 ABND-SQLCODE         PIC S9(8).
 *       03 ABND-FREEFORM        PIC X(600).
 * </pre>
 *
 * @param utimeKey   absolute time key
 * @param taskNumber CICS task number
 * @param applId     CICS application identifier
 * @param tranId     CICS transaction identifier
 * @param date       formatted date string (DD.MM.YYYY)
 * @param time       formatted time string (HH:MM:SS)
 * @param abendCode  four-character abend code
 * @param program    program name that abended
 * @param respCode   CICS RESP value
 * @param resp2Code  CICS RESP2 value
 * @param sqlCode    SQL return code (zero when not SQL-related)
 * @param freeform   free-form diagnostic message
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
        String freeform
) {
}
