/*
 *    Copyright IBM Corp. 2023
 *
 *    Equivalent of the CICS ABEND handling in the COBOL DELCUS program.
 */
package com.ibm.cics.cip.bank.delcus;

/**
 * Exception thrown when a fatal error occurs during customer deletion.
 * In the original COBOL program these conditions triggered an EXEC CICS ABEND
 * with a specific abend code (WPV6, WPV7, HWPT).
 */
public class DeleteCustomerException extends Exception {

    private final String abendCode;

    /**
     * @param abendCode  the original COBOL abend code (e.g. "WPV6", "WPV7", "HWPT")
     * @param message    descriptive error message
     */
    public DeleteCustomerException(String abendCode, String message) {
        super(message);
        this.abendCode = abendCode;
    }

    /**
     * @param abendCode  the original COBOL abend code
     * @param message    descriptive error message
     * @param cause      underlying cause
     */
    public DeleteCustomerException(String abendCode, String message, Throwable cause) {
        super(message, cause);
        this.abendCode = abendCode;
    }

    public String getAbendCode() {
        return abendCode;
    }
}
