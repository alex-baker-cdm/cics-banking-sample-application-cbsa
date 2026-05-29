/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from COBOL CICS VSAM KSDS file access (ABNDFILE).
 * Abstracts the underlying data store for abend records, replacing the
 * EXEC CICS WRITE FILE('ABNDFILE') command.
 */
package com.ibm.cics.cip.bank.migrated.abndproc;

public interface AbndFileStore {

    /**
     * Writes an abend record to the store, keyed by
     * {@link AbndRecord#vsamKey()}.
     *
     * @param record the abend record to persist
     * @return a {@link WriteResult} indicating success or failure
     */
    WriteResult write(AbndRecord record);
}
