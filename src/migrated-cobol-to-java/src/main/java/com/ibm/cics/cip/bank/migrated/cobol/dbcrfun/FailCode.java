/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.migrated.cobol.dbcrfun;

/**
 * Fail codes returned by the credit/debit operation, mapped from the
 * COBOL COMM-FAIL-CODE field in DBCRFUN.cbl.
 */
public enum FailCode {

    NONE("0"),
    ACCOUNT_NOT_FOUND("1"),
    DB_ERROR("2"),
    INSUFFICIENT_FUNDS("3"),
    OPERATION_NOT_ALLOWED("4");

    private final String code;

    FailCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
