/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.creditagency;

/**
 * Exception thrown when the credit check agency encounters a processing error.
 * This maps to the CICS ABEND handling in the original COBOL program
 * (CRDTAGY3.cbl), where failures result in an abend with a specific code.
 */
public class CreditCheckAgencyException extends RuntimeException {

    private final String abendCode;

    /**
     * Constructs a new exception with the specified message and abend code.
     *
     * @param message   the error message
     * @param abendCode the CICS abend code (e.g., "PLOP")
     */
    public CreditCheckAgencyException(String message, String abendCode) {
        super(message);
        this.abendCode = abendCode;
    }

    /**
     * Constructs a new exception with the specified message, abend code, and
     * cause.
     *
     * @param message   the error message
     * @param abendCode the CICS abend code (e.g., "PLOP")
     * @param cause     the underlying cause
     */
    public CreditCheckAgencyException(String message, String abendCode,
                                      Throwable cause) {
        super(message, cause);
        this.abendCode = abendCode;
    }

    /**
     * Returns the CICS abend code associated with this error.
     *
     * @return the abend code
     */
    public String getAbendCode() {
        return abendCode;
    }
}
