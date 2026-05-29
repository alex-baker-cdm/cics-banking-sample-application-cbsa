/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bankliberty.migrated.crecust;

/**
 * Interface for the customer data store operations.
 * Abstracts CICS VSAM file operations on the CUSTOMER file.
 *
 * In COBOL, these operations used EXEC CICS READ/WRITE/REWRITE
 * on FILE('CUSTOMER') with retry logic for SYSIDERR conditions.
 */
public interface CustomerDataStore {

    /**
     * Writes a new customer record to the data store.
     * Mirrors COBOL WRITE FILE('CUSTOMER') with SYSIDERR retry.
     *
     * @param record the customer record to write
     * @throws DataStoreException if the write fails after retries
     */
    void writeCustomer(CustomerRecord record) throws DataStoreException;

    /**
     * Reads the customer control record for update.
     * The control record has sortcode=000000 and number=9999999999.
     * Mirrors COBOL READ FILE('CUSTOMER') UPDATE with SYSIDERR retry.
     *
     * @param sortCode the sort code (set to "000000" for control record)
     * @return the customer control record
     * @throws DataStoreException if the read fails after retries
     */
    CustomerControlRecord readControlRecordForUpdate(String sortCode)
            throws DataStoreException;

    /**
     * Rewrites (updates) the customer control record.
     * Mirrors COBOL REWRITE FILE('CUSTOMER') with SYSIDERR retry.
     *
     * @param controlRecord the updated control record
     * @throws DataStoreException if the rewrite fails after retries
     */
    void rewriteControlRecord(CustomerControlRecord controlRecord)
            throws DataStoreException;

    /**
     * Exception thrown when a data store operation fails.
     */
    class DataStoreException extends Exception {
        private final String failCode;

        public DataStoreException(String failCode, String message) {
            super(message);
            this.failCode = failCode;
        }

        public DataStoreException(String failCode, String message,
                                  Throwable cause) {
            super(message, cause);
            this.failCode = failCode;
        }

        public String getFailCode() {
            return failCode;
        }
    }
}
