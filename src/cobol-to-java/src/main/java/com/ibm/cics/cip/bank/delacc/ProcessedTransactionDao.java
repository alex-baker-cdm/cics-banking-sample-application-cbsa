/*
 * Copyright IBM Corp. 2023
 *
 * Data-access abstraction for the PROCTRAN table.
 * Mirrors the SQL INSERT in DELACC.cbl (WRITE-PROCTRAN-DB2).
 */
package com.ibm.cics.cip.bank.delacc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public interface ProcessedTransactionDao {

    void insertDeleteAccountTransaction(
        String sortCode,
        String accountNumber,
        LocalDate transactionDate,
        LocalTime transactionTime,
        String reference,
        String transactionType,
        String description,
        BigDecimal amount);
}
