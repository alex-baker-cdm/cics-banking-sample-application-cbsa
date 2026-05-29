/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.migrated.cobol.dbcrfun;

import java.math.BigDecimal;

/**
 * Input request for the credit/debit operation, mapped from the
 * DFHCOMMAREA (PAYDBCR.cpy) input fields in DBCRFUN.cbl.
 *
 * <p>A positive amount indicates a credit (deposit); a negative amount
 * indicates a debit (withdrawal).</p>
 *
 * @param accountNumber  the target account number (8 chars)
 * @param amount         the amount to credit (positive) or debit (negative)
 * @param facilityType   identifies the request source (TELLER or PAYMENT)
 * @param originDescription description of the payment origin (first 14 chars used for PROCTRAN),
 *                          only relevant when facilityType is PAYMENT
 */
public record CreditDebitRequest(
        String accountNumber,
        BigDecimal amount,
        FacilityType facilityType,
        String originDescription
) {

    public boolean isDebit() {
        return amount != null && amount.signum() < 0;
    }

    public boolean isCredit() {
        return amount != null && amount.signum() >= 0;
    }

    public boolean isFromPayment() {
        return facilityType == FacilityType.PAYMENT;
    }
}
