/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bankliberty.migrated.transferfunds;

import java.math.BigDecimal;

/**
 * Immutable representation of a row written to the PROCTRAN DB2 table.
 * <p>
 * Migrated from the COBOL HOST-PROCTRAN-ROW structure and
 * the PROCTRAN.cpy copybook.
 */
public record ProcessedTransactionRecord(
        String eyeCatcher,
        String sortCode,
        String accountNumber,
        String date,
        String time,
        String reference,
        String type,
        String description,
        BigDecimal amount
) {

    public static final String EYECATCHER_VALUE = "PRTR";
    public static final String TYPE_TRANSFER = "TFR";
    public static final String DESCRIPTION_TRANSFER_PREFIX = "TRANSFER";
}
