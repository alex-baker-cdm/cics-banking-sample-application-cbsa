/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from COBOL: BNK1CRA.cbl
 * Original Author: Jon Collett
 */
package com.ibm.cics.cip.bank.migrated.bnk1cra;

/**
 * Failure codes returned by the debit/credit function (DBCRFUN equivalent).
 */
public enum FailCode {

    ACCOUNT_NOT_FOUND('1', "Account not found"),
    UNEXPECTED_ERROR('2', "Unexpected error applying amount"),
    INSUFFICIENT_FUNDS('3', "Insufficient funds available");

    private final char code;
    private final String description;

    FailCode(char code, String description) {
        this.code = code;
        this.description = description;
    }

    public char getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static FailCode fromCode(char code) {
        for (FailCode fc : values()) {
            if (fc.code == code) {
                return fc;
            }
        }
        return null;
    }
}
