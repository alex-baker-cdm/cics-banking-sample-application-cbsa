/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.creditagency;

/**
 * Exception thrown when the credit check agency encounters an unrecoverable
 * error. Carries the {@link AbendInfo} diagnostic payload that mirrors the
 * COBOL ABNDINFO record linked to the ABNDPROC abend-handler program.
 */
public class CreditAgencyException extends RuntimeException {

    private final AbendInfo abendInfo;

    public CreditAgencyException(String message, AbendInfo abendInfo) {
        super(message);
        this.abendInfo = abendInfo;
    }

    public CreditAgencyException(String message, AbendInfo abendInfo,
            Throwable cause) {
        super(message, cause);
        this.abendInfo = abendInfo;
    }

    public AbendInfo getAbendInfo() {
        return abendInfo;
    }
}
