/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from BNK1CCA.cbl DFHCOMMAREA / WS-COMM-AREA - the communication
 * area passed between pseudo-conversational CICS transactions.
 */
package com.ibm.cics.cip.bank.migration.bnk1cca;

import java.time.LocalDate;

/**
 * Communication area for the BNK1CCA transaction, preserving state across
 * pseudo-conversational invocations.
 *
 * @param eyeCatcher     4-char identifier (WS-COMM-EYE / COMM-EYE)
 * @param sortCode       6-char bank sort code (WS-COMM-SCODE / COMM-SCODE)
 * @param customerNumber 10-char customer number (WS-COMM-CUSTNO / COMM-CUSTNO)
 * @param name           60-char customer name (WS-COMM-NAME / COMM-NAME)
 * @param address        160-char customer address (WS-COMM-ADDR / COMM-ADDR)
 * @param dateOfBirth    customer date of birth (WS-COMM-DOB / COMM-DOB)
 */
public record CommArea(
        String eyeCatcher,
        String sortCode,
        String customerNumber,
        String name,
        String address,
        LocalDate dateOfBirth
) {

    public static CommArea empty() {
        return new CommArea("", "", "", "", "", null);
    }
}
