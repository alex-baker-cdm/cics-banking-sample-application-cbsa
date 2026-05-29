/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from COBOL: BNKMENU.cbl - MAPFAIL condition
 */
package com.ibm.cics.cip.bank.migrated.bnkmenu;

public class MapFailException extends CicsOperationException {

    public MapFailException(int respCode, int resp2Code) {
        super("MAPFAIL condition received", respCode, resp2Code);
    }
}
