/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from COBOL: BNK1CRA.cbl
 * Original Author: Jon Collett
 */
package com.ibm.cics.cip.bank.migrated.bnk1cra;

import java.math.BigDecimal;

/**
 * Represents a credit or debit request as received from the BMS screen input.
 * Maps to the COBOL fields: ACCNOI (account number), SIGNI (sign +/-), AMTI (amount).
 *
 * @param accountNumber the 8-character account number
 * @param sign          '+' for credit, '-' for debit
 * @param amount        the monetary amount (non-negative, applied per sign)
 */
public record CreditDebitRequest(
        String accountNumber,
        char sign,
        BigDecimal amount
) {
}
