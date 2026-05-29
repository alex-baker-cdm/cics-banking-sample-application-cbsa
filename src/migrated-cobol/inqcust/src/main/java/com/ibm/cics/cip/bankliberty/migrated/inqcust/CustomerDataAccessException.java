/*
 *
 *    Copyright IBM Corp. 2023
 *
 */

package com.ibm.cics.cip.bankliberty.migrated.inqcust;

/**
 * Thrown when the customer data store encounters an unrecoverable error.
 * Equivalent to a CICS abend in the original COBOL program.
 */
public class CustomerDataAccessException extends Exception {

    private final String abendCode;

    public CustomerDataAccessException(String message, String abendCode) {
        super(message);
        this.abendCode = abendCode;
    }

    public CustomerDataAccessException(String message, String abendCode,
            Throwable cause) {
        super(message, cause);
        this.abendCode = abendCode;
    }

    public String getAbendCode() {
        return abendCode;
    }
}
