/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.migrated.cobol.creditagency;

/**
 * Exception thrown when the credit agency program encounters an abnormal
 * condition. This is the Java equivalent of a CICS ABEND in the original
 * COBOL program CRDTAGY2.cbl.
 *
 * <p>When thrown, the exception carries an {@link AbendInfo} record containing
 * the same diagnostic information that the COBOL program would have passed
 * to the ABNDPROC abend handler program.
 */
public class CreditAgencyException extends RuntimeException {

    private final AbendInfo abendInfo;

    public CreditAgencyException(String message) {
        super(message);
        this.abendInfo = null;
    }

    public CreditAgencyException(String message, Throwable cause) {
        super(message, cause);
        this.abendInfo = null;
    }

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
