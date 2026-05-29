/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bankliberty.migrated.transferfunds;

/**
 * Exception representing an unrecoverable transfer failure.
 * <p>
 * Replaces the COBOL EXEC CICS ABEND pattern. The abendCode
 * maps directly to the four-character ABCODE values from the
 * original COBOL program (e.g. "SAME", "FROM", "TO  ", "HROL",
 * "WPCD", "RUF2", "RUF3").
 */
public class TransferFundsException extends RuntimeException {

    private final String abendCode;

    public TransferFundsException(String abendCode, String message) {
        super(message);
        this.abendCode = abendCode;
    }

    public TransferFundsException(String abendCode, String message,
                                   Throwable cause) {
        super(message, cause);
        this.abendCode = abendCode;
    }

    public String getAbendCode() {
        return abendCode;
    }
}
