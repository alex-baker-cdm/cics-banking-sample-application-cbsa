/*
 *
 *    Copyright IBM Corp. 2023
 *
 */

package com.ibm.cics.cip.bank.migrated.createcustomer;

/**
 * Data assembled for the CRECUST subprogram, migrated from BNK1CCS.cbl
 * CRE-CUST-DATA section and the SUBPGM-PARMS copybook.
 *
 * <p>In the original COBOL, this was the COMMAREA passed via
 * {@code EXEC CICS LINK PROGRAM('CRECUST')}. The eyecatcher was always 'CUST'.
 *
 * @param eyecatcher always "CUST"
 * @param name       assembled full name (title + first + initials + surname)
 * @param address    concatenated address (line1 + line2 + line3), padded to 160 chars
 * @param birthDay   DD portion of date of birth
 * @param birthMonth MM portion of date of birth
 * @param birthYear  YYYY portion of date of birth
 */
public record CustomerCreationRequest(
        String eyecatcher,
        String name,
        String address,
        int birthDay,
        int birthMonth,
        int birthYear
) {

    public static final String EYECATCHER_VALUE = "CUST";

    public static final int NAME_MAX_LENGTH = 60;
    public static final int ADDRESS_MAX_LENGTH = 160;
    public static final int ADDRESS_LINE_1_LENGTH = 60;
    public static final int ADDRESS_LINE_2_LENGTH = 60;
    public static final int ADDRESS_LINE_3_LENGTH = 40;
}
