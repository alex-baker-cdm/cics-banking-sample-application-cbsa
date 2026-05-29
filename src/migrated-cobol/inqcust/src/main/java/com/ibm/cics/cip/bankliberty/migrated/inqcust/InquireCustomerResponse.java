/*
 *
 *    Copyright IBM Corp. 2023
 *
 */

package com.ibm.cics.cip.bankliberty.migrated.inqcust;

import java.time.LocalDate;

/**
 * Output of the Inquire Customer operation.
 * Maps to the output portion of the INQCUST DFHCOMMAREA.
 *
 * @param eyecatcher       4-char record type marker (e.g. "CUST")
 * @param sortCode         6-digit sort code as a string
 * @param customerNumber   10-digit customer number
 * @param name             customer full name
 * @param address          customer address
 * @param dateOfBirth      customer date of birth
 * @param creditScore      3-digit credit score
 * @param creditScoreReviewDate  credit score review date
 * @param inquirySuccess   true if the inquiry succeeded
 * @param failureCode      failure reason code: '0' = none, '1' = not found,
 *                         '2' = storm-drain (VSAM RLS abend), '9' = data access error
 */
public record InquireCustomerResponse(
        String eyecatcher,
        String sortCode,
        long customerNumber,
        String name,
        String address,
        LocalDate dateOfBirth,
        int creditScore,
        LocalDate creditScoreReviewDate,
        boolean inquirySuccess,
        char failureCode
) {
    public static InquireCustomerResponse success(CustomerRecord record) {
        return new InquireCustomerResponse(
                record.eyecatcher(),
                String.valueOf(record.sortCode()),
                record.customerNumber(),
                record.name(),
                record.address(),
                record.dateOfBirth(),
                record.creditScore(),
                record.creditScoreReviewDate(),
                true,
                '0'
        );
    }

    public static InquireCustomerResponse notFound(long customerNumber) {
        return new InquireCustomerResponse(
                null,
                null,
                customerNumber,
                "",
                "",
                LocalDate.EPOCH,
                0,
                LocalDate.EPOCH,
                false,
                '1'
        );
    }

    public static InquireCustomerResponse stormDrain() {
        return new InquireCustomerResponse(
                null,
                null,
                0,
                "",
                "",
                LocalDate.EPOCH,
                0,
                LocalDate.EPOCH,
                false,
                '2'
        );
    }

    public static InquireCustomerResponse dataAccessError() {
        return new InquireCustomerResponse(
                null,
                null,
                0,
                "",
                "",
                LocalDate.EPOCH,
                0,
                LocalDate.EPOCH,
                false,
                '9'
        );
    }
}
