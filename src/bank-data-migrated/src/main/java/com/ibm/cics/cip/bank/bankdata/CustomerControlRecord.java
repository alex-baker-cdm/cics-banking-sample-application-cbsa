/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from COBOL copybook CUSTCTRL.cpy
 */
package com.ibm.cics.cip.bank.bankdata;

import java.util.Objects;

/**
 * Represents a customer control record, equivalent to the COBOL CUSTCTRL
 * copybook. Written to VSAM CUSTOMER file as a control/summary record.
 */
public record CustomerControlRecord(
        String eyecatcher,
        String sortCode,
        String controlNumber,
        long numberOfCustomers,
        long lastCustomerNumber) {

    public static final String EYECATCHER_VALUE = "CTRL";
    public static final String CONTROL_NUMBER = "9999999999";

    public CustomerControlRecord {
        Objects.requireNonNull(eyecatcher, "eyecatcher must not be null");
        Objects.requireNonNull(sortCode, "sortCode must not be null");
        Objects.requireNonNull(controlNumber, "controlNumber must not be null");
    }
}
