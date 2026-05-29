/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.migrated.createaccount;

public record ValidationResult(boolean valid, String errorMessage, String invalidField) {

    public static ValidationResult ok() {
        return new ValidationResult(true, null, null);
    }

    public static ValidationResult error(String message, String field) {
        return new ValidationResult(false, message, field);
    }
}
