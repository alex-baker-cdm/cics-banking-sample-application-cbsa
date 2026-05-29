/*
 * Copyright IBM Corp. 2023
 *
 * Mirrors CICS ABEND behavior from DELACC.cbl.
 * Thrown when a fatal database error occurs that would have caused
 * an abend in the original COBOL program.
 */
package com.ibm.cics.cip.bank.delacc;

public class DeleteAccountAbendException extends RuntimeException {

    private final String abendCode;

    public DeleteAccountAbendException(String abendCode, String message) {
        super(message);
        this.abendCode = abendCode;
    }

    public DeleteAccountAbendException(String abendCode, String message,
                                       Throwable cause) {
        super(message, cause);
        this.abendCode = abendCode;
    }

    public String getAbendCode() {
        return abendCode;
    }
}
