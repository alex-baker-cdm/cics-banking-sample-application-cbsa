/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.migrated.cobol.dbcrfun;

/**
 * Facility type indicator from the COMM-FACILTYPE field in PAYDBCR.cpy.
 * In CICS, FACILTYPE 496 means "NONE" (no terminal facility), which
 * indicates the request comes from a Payment link rather than a Teller.
 */
public enum FacilityType {

    TELLER(0),
    PAYMENT(496);

    private final int code;

    FacilityType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static FacilityType fromCode(int code) {
        if (code == PAYMENT.code) {
            return PAYMENT;
        }
        return TELLER;
    }
}
