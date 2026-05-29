/*
 *
 *    Copyright IBM Corp. 2023
 *
 */

package com.ibm.cics.cip.bankliberty.migrated.inqcust;

import java.util.Optional;

/**
 * Data-access interface for customer records.
 * Abstracts the underlying storage (VSAM, Db2, etc.) so that
 * the business logic in {@link InquireCustomer} remains decoupled
 * from any specific data-store technology.
 */
public interface CustomerDao {

    /**
     * Read a customer record by its composite key.
     *
     * @param sortCode       the bank sort code
     * @param customerNumber the customer number
     * @return the customer record, or empty if not found
     * @throws CustomerDataAccessException on transient or permanent I/O errors
     */
    Optional<CustomerRecord> readCustomer(int sortCode, long customerNumber)
            throws CustomerDataAccessException;

    /**
     * Read the last (highest-numbered) customer record for a given sort code.
     *
     * @param sortCode the bank sort code
     * @return the last customer record, or empty if no customers exist
     * @throws CustomerDataAccessException on transient or permanent I/O errors
     */
    Optional<CustomerRecord> readLastCustomer(int sortCode)
            throws CustomerDataAccessException;
}
