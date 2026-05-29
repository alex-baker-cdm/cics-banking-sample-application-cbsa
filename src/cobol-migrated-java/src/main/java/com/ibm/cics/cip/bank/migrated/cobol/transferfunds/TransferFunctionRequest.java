/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.migrated.cobol.transferfunds;

import java.math.BigDecimal;

/**
 * Request passed to the transfer function gateway. Maps to the SUBPGM-PARMS
 * COMMAREA fields used by the COBOL XFRFUN program.
 *
 * @param fromAccountNumber 8-digit FROM account number (SUBPGM-FACCNO)
 * @param toAccountNumber   8-digit TO account number (SUBPGM-TACCNO)
 * @param amount            transfer amount (SUBPGM-AMT)
 */
public record TransferFunctionRequest(
        String fromAccountNumber,
        String toAccountNumber,
        BigDecimal amount) {
}
