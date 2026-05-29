/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from COBOL: BNK1UAC.cbl — ABEND-THIS-TASK SECTION
 *
 * Thrown when the original COBOL program would have issued an
 * EXEC CICS ABEND with code 'HBNK'.
 */
package com.ibm.cics.cip.bank.migrated.bnk1uac;

public class Bnk1uacException extends RuntimeException {

    private final String abendCode;
    private final String section;
    private final int responseCode;
    private final int response2Code;

    public Bnk1uacException(String section, String message,
                            int responseCode, int response2Code) {
        super(formatMessage(section, message, responseCode, response2Code));
        this.abendCode = "HBNK";
        this.section = section;
        this.responseCode = responseCode;
        this.response2Code = response2Code;
    }

    public String getAbendCode() {
        return abendCode;
    }

    public String getSection() {
        return section;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public int getResponse2Code() {
        return response2Code;
    }

    private static String formatMessage(String section, String message,
                                        int responseCode, int response2Code) {
        return "BNK1UAC - %s - %s RESP=%d RESP2=%d ABENDING TASK."
                .formatted(section, message, responseCode, response2Code);
    }
}
