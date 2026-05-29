/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.cobol.inquire.account;

/**
 * Thrown when an unrecoverable error occurs during account inquiry.
 *
 * Mirrors the COBOL abend codes:
 *   HRAC - DB2 cursor open/close/fetch failure
 *   HNCS - Named Counter / last-account SELECT failure
 *   HROL - Syncpoint rollback failure after VSAM RLS abend
 */
public class InquireAccountException extends RuntimeException {

    private final String abendCode;
    private final int sqlCode;

    public InquireAccountException(String abendCode, String message,
                                   int sqlCode) {
        super(message);
        this.abendCode = abendCode;
        this.sqlCode = sqlCode;
    }

    public InquireAccountException(String abendCode, String message,
                                   int sqlCode, Throwable cause) {
        super(message, cause);
        this.abendCode = abendCode;
        this.sqlCode = sqlCode;
    }

    public String getAbendCode() {
        return abendCode;
    }

    public int getSqlCode() {
        return sqlCode;
    }
}
