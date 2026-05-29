/*
 *
 *    Copyright IBM Corp. 2023
 *
 */

package com.ibm.cics.cip.bank.migrated.createcustomer;

/**
 * Represents the BMS screen input fields for creating a customer.
 *
 * <p>Migrated from BNK1CCS.cbl — maps to the BNK1CC BMS map input fields:
 * CUSTTITI, CHRISTNI, CUSTINSI, CUSTSNI, CUSTAD1I–CUSTAD3I,
 * DOBDDI, DOBMMI, DOBYYI, and the read-only CUSTNO2I.
 *
 * @param title          customer title (e.g. Mr, Mrs, Dr)
 * @param firstName      customer first/christian name
 * @param middleInitials customer middle initials (optional, max 2 chars)
 * @param lastName       customer family/surname
 * @param addressLine1   address line 1
 * @param addressLine2   address line 2 (optional)
 * @param addressLine3   address line 3 (optional)
 * @param dobDay         date of birth day (DD)
 * @param dobMonth       date of birth month (MM)
 * @param dobYear        date of birth year (YYYY)
 * @param existingCustomerNumber if non-null, a customer number already
 *                               displayed on screen (indicates screen must
 *                               be cleared first)
 */
public record CreateCustomerInput(
        String title,
        String firstName,
        String middleInitials,
        String lastName,
        String addressLine1,
        String addressLine2,
        String addressLine3,
        String dobDay,
        String dobMonth,
        String dobYear,
        String existingCustomerNumber
) {
}
