/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from COBOL: CREACC.cbl failure codes
 */

package com.ibm.cics.cip.bank.migrated.creacc;

/**
 * Failure codes returned by the Create Account process.
 * Mirrors the COMM-FAIL-CODE values set throughout CREACC.cbl.
 */
public final class FailCode {

    /** Customer inquiry failed or customer not found. */
    public static final String CUSTOMER_NOT_FOUND = "1";

    /** Enqueue of named counter failed. */
    public static final String ENQ_FAILED = "3";

    /** Dequeue of named counter failed. */
    public static final String DEQ_FAILED = "5";

    /** Account INSERT to DB2 failed. */
    public static final String ACCOUNT_INSERT_FAILED = "7";

    /** Customer already has maximum number of accounts (>9). */
    public static final String MAX_ACCOUNTS_EXCEEDED = "8";

    /** Error counting customer accounts. */
    public static final String ACCOUNT_COUNT_ERROR = "9";

    /** Invalid account type. */
    public static final String INVALID_ACCOUNT_TYPE = "A";

    private FailCode() {
    }
}
