/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bankliberty.migrated.crecust;

/**
 * Interface for the processed transaction data store operations.
 * Abstracts the DB2 INSERT into PROCTRAN table.
 *
 * In COBOL, this was implemented via embedded SQL INSERT INTO PROCTRAN.
 * On failure, the COBOL program performed an ABEND with code 'HWPT'.
 */
public interface ProcessedTransactionDataStore {

    /**
     * Writes a processed transaction record.
     * Mirrors the COBOL INSERT INTO PROCTRAN SQL statement.
     *
     * @param record the processed transaction record to write
     * @throws ProcessedTransactionException if the insert fails
     */
    void writeTransaction(ProcessedTransactionRecord record)
            throws ProcessedTransactionException;

    /**
     * Exception thrown when a processed transaction write fails.
     * Maps to COBOL ABEND code 'HWPT'.
     */
    class ProcessedTransactionException extends Exception {
        private final String abendCode;

        public ProcessedTransactionException(String abendCode, String message) {
            super(message);
            this.abendCode = abendCode;
        }

        public ProcessedTransactionException(String abendCode, String message,
                                             Throwable cause) {
            super(message, cause);
            this.abendCode = abendCode;
        }

        public String getAbendCode() {
            return abendCode;
        }
    }
}
