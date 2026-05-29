/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.migrated.cobol.dbcrfun;

/**
 * Transaction types written to the PROCTRAN data store, mapped from
 * HV-PROCTRAN-TYPE in DBCRFUN.cbl.
 */
public enum TransactionType {

    DEBIT("DEB"),
    CREDIT("CRE"),
    PAYMENT_DEBIT("PDR"),
    PAYMENT_CREDIT("PCR");

    private final String code;

    TransactionType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
