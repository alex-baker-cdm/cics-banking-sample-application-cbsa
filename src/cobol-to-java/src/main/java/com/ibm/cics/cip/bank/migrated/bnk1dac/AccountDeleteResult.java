/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from PARMS-SUBPGM delete fields in BNK1DAC.cbl.
 */
package com.ibm.cics.cip.bank.migrated.bnk1dac;

/**
 * Result of an account deletion attempt, corresponding to the
 * PARMS-SUBPGM-DEL-* fields in the original COBOL program.
 *
 * <p>Fail codes from COBOL:
 * <ul>
 *   <li>'1' - Account not found</li>
 *   <li>'2' - Datastore error</li>
 *   <li>'3' - Delete error</li>
 * </ul>
 */
public record AccountDeleteResult(
        boolean successful,
        char failCode,
        String sortCode
) {

    public static final char FAIL_NOT_FOUND = '1';
    public static final char FAIL_DATASTORE_ERROR = '2';
    public static final char FAIL_DELETE_ERROR = '3';
    public static final char NO_FAIL = ' ';

    public static AccountDeleteResult success(String sortCode) {
        return new AccountDeleteResult(true, NO_FAIL, sortCode);
    }

    public static AccountDeleteResult failure(char failCode, String sortCode) {
        return new AccountDeleteResult(false, failCode, sortCode);
    }
}
