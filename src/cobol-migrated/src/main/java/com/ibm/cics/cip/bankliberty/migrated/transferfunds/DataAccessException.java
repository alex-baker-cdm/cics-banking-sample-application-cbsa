/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bankliberty.migrated.transferfunds;

/**
 * Exception thrown when a database operation fails.
 * <p>
 * Replaces the COBOL pattern of checking SQLCODE and issuing
 * EXEC CICS ABEND for unrecoverable database errors.
 */
public class DataAccessException extends Exception {

    private final int sqlCode;

    public DataAccessException(String message) {
        super(message);
        this.sqlCode = 0;
    }

    public DataAccessException(String message, int sqlCode) {
        super(message);
        this.sqlCode = sqlCode;
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
        this.sqlCode = 0;
    }

    public DataAccessException(String message, int sqlCode, Throwable cause) {
        super(message, cause);
        this.sqlCode = sqlCode;
    }

    public int getSqlCode() {
        return sqlCode;
    }

    public boolean isConnectionLost() {
        return sqlCode == 923;
    }
}
