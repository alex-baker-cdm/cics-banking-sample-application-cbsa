/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bankliberty.migrated.transferfunds;

import java.math.BigDecimal;

/**
 * Immutable request for a fund transfer between two accounts.
 * <p>
 * Migrated from the COBOL DFHCOMMAREA (XFRFUN.cpy) input fields:
 * COMM-FACCNO, COMM-FSCODE, COMM-TACCNO, COMM-TSCODE, COMM-AMT.
 */
public record TransferFundsRequest(
        String fromAccountNumber,
        String fromSortCode,
        String toAccountNumber,
        String toSortCode,
        BigDecimal amount
) {

    public TransferFundsRequest {
        if (fromAccountNumber == null || fromAccountNumber.isBlank()) {
            throw new IllegalArgumentException("fromAccountNumber must not be blank");
        }
        if (fromSortCode == null || fromSortCode.isBlank()) {
            throw new IllegalArgumentException("fromSortCode must not be blank");
        }
        if (toAccountNumber == null || toAccountNumber.isBlank()) {
            throw new IllegalArgumentException("toAccountNumber must not be blank");
        }
        if (toSortCode == null || toSortCode.isBlank()) {
            throw new IllegalArgumentException("toSortCode must not be blank");
        }
        if (amount == null) {
            throw new IllegalArgumentException("amount must not be null");
        }
    }
}
