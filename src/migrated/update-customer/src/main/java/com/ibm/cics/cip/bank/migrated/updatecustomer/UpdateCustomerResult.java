/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.migrated.updatecustomer;

/**
 * Represents the outcome of an update-customer operation, corresponding
 * to the COMM-UPD-SUCCESS and COMM-UPD-FAIL-CD fields in the original
 * COBOL COMMAREA.
 *
 * <p>Failure codes map to the original COBOL values:
 * <ul>
 *   <li>{@code T} - Invalid title in customer name</li>
 *   <li>{@code 1} - Customer not found</li>
 *   <li>{@code 2} - Data store read error</li>
 *   <li>{@code 3} - Data store write error</li>
 *   <li>{@code 4} - Both name and address are empty or start with a space</li>
 * </ul>
 */
public sealed interface UpdateCustomerResult {

    boolean isSuccess();

    /**
     * Successful update. Contains the customer record as it now exists
     * in the data store (mirrors how the COBOL program copies the updated
     * VSAM record back into the COMMAREA on success).
     */
    record Success(
            String eyecatcher,
            String sortCode,
            String customerNumber,
            String customerName,
            String customerAddress,
            String dateOfBirth,
            int creditScore,
            String creditScoreReviewDate
    ) implements UpdateCustomerResult {

        @Override
        public boolean isSuccess() {
            return true;
        }
    }

    /**
     * Failed update. Contains the failure code matching the original COBOL
     * COMM-UPD-FAIL-CD values.
     */
    record Failure(String failureCode) implements UpdateCustomerResult {

        @Override
        public boolean isSuccess() {
            return false;
        }
    }
}
