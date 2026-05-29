/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from COBOL: BNKMENU.cbl - CICS error handling
 */
package com.ibm.cics.cip.bank.migrated.bnkmenu;

public class CicsOperationException extends Exception {

    private final int respCode;
    private final int resp2Code;

    public CicsOperationException(String message, int respCode, int resp2Code) {
        super(message);
        this.respCode = respCode;
        this.resp2Code = resp2Code;
    }

    public int getRespCode() {
        return respCode;
    }

    public int getResp2Code() {
        return resp2Code;
    }
}
