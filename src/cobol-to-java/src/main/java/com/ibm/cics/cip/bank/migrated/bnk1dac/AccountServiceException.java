/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from BNK1DAC.cbl — replaces CICS ABEND scenarios
 * that occurred when LINK to INQACC/DELACC returned a non-NORMAL response.
 */
package com.ibm.cics.cip.bank.migrated.bnk1dac;

/**
 * Thrown when an account service call (inquiry or delete) fails.
 * In the original COBOL this would trigger the ABEND-THIS-TASK section.
 */
public class AccountServiceException extends RuntimeException {

    private final int responseCode;
    private final int responseCode2;

    public AccountServiceException(String message) {
        super(message);
        this.responseCode = 0;
        this.responseCode2 = 0;
    }

    public AccountServiceException(String message, int responseCode, int responseCode2) {
        super(message);
        this.responseCode = responseCode;
        this.responseCode2 = responseCode2;
    }

    public AccountServiceException(String message, Throwable cause) {
        super(message, cause);
        this.responseCode = 0;
        this.responseCode2 = 0;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public int getResponseCode2() {
        return responseCode2;
    }
}
