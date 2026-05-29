/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.creditagency;

/**
 * Exception thrown when the credit agency encounters an error condition
 * that would have caused a CICS ABEND in the original COBOL program.
 * The abend code is preserved to maintain compatibility with the
 * original error semantics.
 */
public class CreditAgencyException extends RuntimeException {

    private final String abendCode;
    private final AbendInfo abendInfo;

    public CreditAgencyException(String message, String abendCode, AbendInfo abendInfo) {
        super(message);
        this.abendCode = abendCode;
        this.abendInfo = abendInfo;
    }

    public CreditAgencyException(String message, String abendCode) {
        super(message);
        this.abendCode = abendCode;
        this.abendInfo = null;
    }

    public String getAbendCode() {
        return abendCode;
    }

    public AbendInfo getAbendInfo() {
        return abendInfo;
    }
}
