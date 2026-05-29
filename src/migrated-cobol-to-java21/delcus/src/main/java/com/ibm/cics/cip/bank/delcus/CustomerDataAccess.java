/*
 *    Copyright IBM Corp. 2023
 *
 *    Abstracts CICS VSAM CUSTOMER file operations and INQCUST program LINK.
 */
package com.ibm.cics.cip.bank.delcus;

import java.util.Optional;

/**
 * Interface abstracting the customer-related data access operations
 * that were originally performed via CICS commands in the COBOL program.
 */
public interface CustomerDataAccess {

    /**
     * Inquires about a customer (equivalent to EXEC CICS LINK PROGRAM('INQCUST')).
     *
     * @param customerNumber the customer number to inquire about
     * @return the inquiry result indicating success/failure
     */
    CustomerInquiryResult inquireCustomer(String customerNumber);

    /**
     * Reads and locks a customer record for update, then deletes it
     * (equivalent to EXEC CICS READ FILE('CUSTOMER') UPDATE + DELETE).
     * <p>
     * Includes retry logic for SYSIDERR (up to 100 retries with 3-second delays).
     *
     * @param sortCode       the sort code portion of the key
     * @param customerNumber the customer number portion of the key
     * @return the customer record that was deleted, or empty if not found
     *         (someone else already deleted it)
     * @throws DeleteCustomerException if the read or delete fails
     *         after retries with a non-NOTFND error
     */
    Optional<CustomerRecord> readAndDeleteCustomer(String sortCode, String customerNumber)
            throws DeleteCustomerException;
}
