/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.migrated.cobol.transferfunds;

import java.math.BigDecimal;

/**
 * Response returned by the transfer function gateway. Maps to the SUBPGM-PARMS
 * output fields used by the COBOL XFRFUN program.
 *
 * @param fromAccountNumber  8-digit FROM account (SUBPGM-FACCNO)
 * @param fromSortCode       6-digit FROM sort code (SUBPGM-FSCODE)
 * @param toAccountNumber    8-digit TO account (SUBPGM-TACCNO)
 * @param toSortCode         6-digit TO sort code (SUBPGM-TSCODE)
 * @param fromAvailableBalance FROM available balance (SUBPGM-FAVBAL)
 * @param fromActualBalance    FROM actual balance (SUBPGM-FACTBAL)
 * @param toAvailableBalance   TO available balance (SUBPGM-TAVBAL)
 * @param toActualBalance      TO actual balance (SUBPGM-TACTBAL)
 * @param failCode             failure code (SUBPGM-FAIL-CODE): '1'=FROM not
 *                             found, '2'=TO not found, '3'=unexpected error,
 *                             '4'=amount not > 0
 * @param success              success indicator (SUBPGM-SUCCESS): 'Y' or 'N'
 */
public record TransferFunctionResponse(
        String fromAccountNumber,
        String fromSortCode,
        String toAccountNumber,
        String toSortCode,
        BigDecimal fromAvailableBalance,
        BigDecimal fromActualBalance,
        BigDecimal toAvailableBalance,
        BigDecimal toActualBalance,
        char failCode,
        char success) {
}
