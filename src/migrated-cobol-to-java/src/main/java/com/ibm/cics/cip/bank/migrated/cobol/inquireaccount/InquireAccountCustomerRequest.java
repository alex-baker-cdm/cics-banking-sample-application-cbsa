/*
 *
 *    Copyright IBM Corp. 2023
 *
 */

package com.ibm.cics.cip.bank.migrated.cobol.inquireaccount;

/**
 * Input for the Inquire Account for Customer operation.
 * Corresponds to the CUSTOMER-NUMBER field in the INQACCCU COMMAREA.
 *
 * @param customerNumber the 10-digit customer number to look up
 */
public record InquireAccountCustomerRequest(long customerNumber) {

    static final long INVALID_CUSTOMER_NUMBER = 9_999_999_999L;

    public InquireAccountCustomerRequest {
        if (customerNumber < 0) {
            throw new IllegalArgumentException(
                    "customerNumber must not be negative: " + customerNumber);
        }
    }
}
