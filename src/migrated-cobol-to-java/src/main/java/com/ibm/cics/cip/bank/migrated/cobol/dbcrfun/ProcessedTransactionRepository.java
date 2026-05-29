/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.migrated.cobol.dbcrfun;

/**
 * Abstracts access to the PROCTRAN (Processed Transaction) DB2 table.
 * Mapped from the embedded SQL INSERT in WRITE-TO-PROCTRAN-DB2 section
 * of DBCRFUN.cbl.
 */
public interface ProcessedTransactionRepository {

    /**
     * Inserts a processed transaction record.
     * Equivalent to the SQL INSERT in WTPD010 of DBCRFUN.cbl.
     *
     * @param record the processed transaction record to insert
     * @throws DataAccessException if a database error occurs
     */
    void insertTransaction(ProcessedTransactionRecord record)
            throws DataAccessException;
}
