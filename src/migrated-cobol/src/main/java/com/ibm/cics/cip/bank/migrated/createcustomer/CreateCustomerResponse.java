/*
 *
 *    Copyright IBM Corp. 2023
 *
 */

package com.ibm.cics.cip.bank.migrated.createcustomer;

/**
 * Represents the complete screen output after processing a Create Customer
 * request. Migrated from BNK1CCS.cbl — combines the BMS output fields and
 * the result message.
 *
 * @param message       the message to display on screen (row 23)
 * @param sortCode      assigned sort code (or empty)
 * @param customerNumber assigned customer number (or empty)
 * @param creditScore   credit score (or empty)
 * @param csReviewDateDay    credit-score review date DD
 * @param csReviewDateMonth  credit-score review date MM
 * @param csReviewDateYear   credit-score review date YYYY
 * @param dobDay        date of birth DD (echoed back)
 * @param dobMonth      date of birth MM (echoed back)
 * @param dobYear       date of birth YYYY (echoed back)
 * @param addressLine1  address line 1 (echoed back)
 * @param addressLine2  address line 2 (echoed back)
 * @param addressLine3  address line 3 (echoed back)
 * @param success       true if customer was created
 */
public record CreateCustomerResponse(
        String message,
        String sortCode,
        String customerNumber,
        String creditScore,
        String csReviewDateDay,
        String csReviewDateMonth,
        String csReviewDateYear,
        String dobDay,
        String dobMonth,
        String dobYear,
        String addressLine1,
        String addressLine2,
        String addressLine3,
        boolean success
) {
}
