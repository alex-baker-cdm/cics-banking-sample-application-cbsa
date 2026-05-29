/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.migrated.cobol.dbcrfun;

import java.math.BigDecimal;

/**
 * Represents a row to be inserted into the PROCTRAN (Processed
 * Transaction) DB2 table, mapped from HOST-PROCTRAN-ROW in
 * DBCRFUN.cbl and the PROCTRAN.cpy copybook.
 *
 * @param eyecatcher  eyecatcher value (always "PRTR")
 * @param sortCode    sort code
 * @param accNumber   account number
 * @param date        transaction date (DD.MM.YYYY format)
 * @param time        transaction time (HHMMSS format)
 * @param reference   transaction reference (task number, 12 digits)
 * @param type        transaction type code (DEB, CRE, PDR, PCR)
 * @param description transaction description
 * @param amount      transaction amount
 */
public record ProcessedTransactionRecord(
        String eyecatcher,
        String sortCode,
        String accNumber,
        String date,
        String time,
        String reference,
        String type,
        String description,
        BigDecimal amount
) {

    public static final String EYECATCHER_VALUE = "PRTR";
}
