/*
 *    Copyright IBM Corp. 2023
 *
 *    Abstracts DB2 PROCTRAN INSERT operations.
 */
package com.ibm.cics.cip.bank.delcus;

import java.math.BigDecimal;

/**
 * Interface abstracting the PROCTRAN (Processed Transaction) DB2 table
 * write operations originally performed via embedded SQL in the COBOL program.
 */
public interface ProcessedTransactionDataAccess {

    /**
     * Writes a processed transaction record to the PROCTRAN DB2 table
     * (equivalent to the EXEC SQL INSERT INTO PROCTRAN in the COBOL program).
     *
     * @param eyeCatcher  4-char eyecatcher ("PRTR")
     * @param sortCode    6-char sort code
     * @param accNumber   8-char account number (zeros for customer deletion)
     * @param date        10-char formatted date (DD.MM.YYYY)
     * @param time        6-char formatted time (HHMMSS)
     * @param reference   12-char reference (task number)
     * @param type        3-char transaction type (e.g. "ODC")
     * @param description 40-char description
     * @param amount      transaction amount
     * @throws DeleteCustomerException if the insert fails
     */
    void writeProcessedTransaction(
            String eyeCatcher,
            String sortCode,
            String accNumber,
            String date,
            String time,
            String reference,
            String type,
            String description,
            BigDecimal amount
    ) throws DeleteCustomerException;
}
