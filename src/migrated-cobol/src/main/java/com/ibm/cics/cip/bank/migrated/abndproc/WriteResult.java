/*
 * Copyright IBM Corp. 2023
 *
 * Represents the outcome of writing an abend record to the file store.
 * Maps to the CICS RESP / RESP2 response codes from the original COBOL.
 */
package com.ibm.cics.cip.bank.migrated.abndproc;

public sealed interface WriteResult permits WriteResult.Success, WriteResult.Failure {

    record Success() implements WriteResult {
    }

    record Failure(int respCode, int resp2Code) implements WriteResult {
    }
}
