/*
 * Copyright IBM Corp. 2023
 *
 * Runtime exception for BankData processing errors.
 * Equivalent to the COBOL RETURN-CODE 12 / GOBACK error handling.
 */
package com.ibm.cics.cip.bank.bankdata;

public class BankDataException extends RuntimeException {

    public BankDataException(String message) {
        super(message);
    }

    public BankDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
