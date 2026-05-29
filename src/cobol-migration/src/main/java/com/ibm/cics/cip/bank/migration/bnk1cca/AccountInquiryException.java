/*
 * Copyright IBM Corp. 2023
 *
 * Exception thrown when the account inquiry service encounters an error.
 * Replaces the CICS ABEND handling in BNK1CCA.cbl for INQACCCU link failures.
 */
package com.ibm.cics.cip.bank.migration.bnk1cca;

/**
 * Thrown when the account inquiry data access fails. Carries the original
 * CICS-equivalent response codes for diagnostic purposes.
 */
public class AccountInquiryException extends RuntimeException {

    private final int responseCode;
    private final int responseCode2;

    public AccountInquiryException(String message, int responseCode, int responseCode2) {
        super(message);
        this.responseCode = responseCode;
        this.responseCode2 = responseCode2;
    }

    public AccountInquiryException(String message, Throwable cause) {
        super(message, cause);
        this.responseCode = -1;
        this.responseCode2 = -1;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public int getResponseCode2() {
        return responseCode2;
    }
}
