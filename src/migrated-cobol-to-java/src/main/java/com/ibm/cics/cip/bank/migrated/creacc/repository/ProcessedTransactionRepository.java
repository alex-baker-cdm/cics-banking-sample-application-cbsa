/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from COBOL: CREACC.cbl WRITE-PROCTRAN-DB2 SECTION
 * (DB2 PROCTRAN table INSERT)
 */

package com.ibm.cics.cip.bank.migrated.creacc.repository;

import java.math.BigDecimal;

/**
 * Repository for the DB2 PROCTRAN (Processed Transaction) table.
 * Replaces the SQL INSERT operation in the WRITE-PROCTRAN-DB2 section
 * of CREACC.cbl.
 */
public interface ProcessedTransactionRepository {

    /**
     * Inserts a processed transaction record into the PROCTRAN table.
     *
     * @param eyecatcher    record eyecatcher ('PRTR')
     * @param sortCode      6-character sort code
     * @param accountNumber 8-character account number
     * @param date          transaction date (DD.MM.YYYY)
     * @param time          transaction time (HHMMSS)
     * @param reference     12-character transaction reference
     * @param type          3-character transaction type (e.g., 'OCA')
     * @param description   40-character transaction description
     * @param amount        transaction amount
     * @return true if the insert was successful
     */
    boolean insertTransaction(
            String eyecatcher,
            String sortCode,
            String accountNumber,
            String date,
            String time,
            String reference,
            String type,
            String description,
            BigDecimal amount
    );
}
