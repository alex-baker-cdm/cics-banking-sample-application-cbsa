/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.migrated.updatecustomer;

/**
 * Signals a failure in the underlying data store, analogous to a
 * non-NORMAL CICS RESP code on a file operation.
 */
public class DataStoreException extends Exception {

    public DataStoreException(String message) {
        super(message);
    }

    public DataStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
