/*
 *
 *    Copyright IBM Corp. 2023
 *
 */

package com.ibm.cics.cip.bank.migrated.createcustomer;

/**
 * Represents a field-level validation error from the Create Customer screen.
 *
 * <p>Migrated from BNK1CCS.cbl EDIT-DATA section — each validation check
 * sets an error message and positions the cursor on the offending field.
 *
 * @param field   the field that failed validation
 * @param message the error message to display
 */
public record ValidationError(
        ScreenField field,
        String message
) {

    public enum ScreenField {
        TITLE,
        FIRST_NAME,
        LAST_NAME,
        ADDRESS_LINE_1,
        DOB_DAY,
        DOB_MONTH,
        DOB_YEAR,
        EXISTING_CUSTOMER_NUMBER
    }
}
