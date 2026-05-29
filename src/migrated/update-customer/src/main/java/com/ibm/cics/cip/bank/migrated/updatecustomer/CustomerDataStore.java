/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.migrated.updatecustomer;

import java.util.Optional;

/**
 * Abstracts the VSAM CUSTOMER file operations from the original COBOL
 * program. In the COBOL source, these were CICS READ FILE('CUSTOMER')
 * UPDATE and CICS REWRITE FILE('CUSTOMER') commands.
 *
 * <p>Implementations may back this with VSAM, a relational database,
 * an in-memory store, or any other persistence mechanism.
 */
public interface CustomerDataStore {

    /**
     * Reads and locks a customer record for update, equivalent to:
     * <pre>
     *   EXEC CICS READ FILE('CUSTOMER')
     *        RIDFLD(DESIRED-CUST-KEY)
     *        INTO(WS-CUST-DATA)
     *        UPDATE
     *   END-EXEC
     * </pre>
     *
     * @param sortCode       the 6-digit sort code
     * @param customerNumber the 10-digit customer number
     * @return the customer record if found, or empty if not found
     * @throws DataStoreException if a data store error occurs (not a
     *         simple "not found")
     */
    Optional<CustomerRecord> readForUpdate(String sortCode,
                                           String customerNumber)
            throws DataStoreException;

    /**
     * Rewrites (updates) a previously locked customer record, equivalent to:
     * <pre>
     *   EXEC CICS REWRITE
     *        FILE ('CUSTOMER')
     *        FROM (WS-CUST-DATA)
     *        LENGTH(WS-CUST-REC-LEN)
     *   END-EXEC
     * </pre>
     *
     * @param record the updated customer record
     * @throws DataStoreException if the rewrite fails
     */
    void rewrite(CustomerRecord record) throws DataStoreException;
}
