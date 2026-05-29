/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.migrated.cobol.dbcrfun;

/**
 * Thrown when a database operation fails. Maps to non-zero SQLCODE
 * values in the COBOL program. The sqlCode field preserves the
 * original DB2 SQLCODE for storm drain evaluation.
 */
public class DataAccessException extends Exception {

    private final int sqlCode;

    public DataAccessException(String message, int sqlCode) {
        super(message);
        this.sqlCode = sqlCode;
    }

    public DataAccessException(String message, int sqlCode, Throwable cause) {
        super(message, cause);
        this.sqlCode = sqlCode;
    }

    public int getSqlCode() {
        return sqlCode;
    }

    public boolean isStormDrainCondition() {
        return sqlCode == 923;
    }
}
