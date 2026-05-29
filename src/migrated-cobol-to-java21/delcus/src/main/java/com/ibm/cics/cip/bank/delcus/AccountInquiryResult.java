/*
 *    Copyright IBM Corp. 2023
 *
 *    Migrated from COBOL copybook INQACCCU.cpy
 */
package com.ibm.cics.cip.bank.delcus;

import java.util.List;

/**
 * Result of the INQACCCU program invocation.
 * Maps the INQACCCU COMMAREA output, which returns up to 20 accounts
 * for a given customer number.
 *
 * @param numberOfAccounts  count of accounts returned
 * @param accounts          list of account detail records
 */
public record AccountInquiryResult(
        int numberOfAccounts,
        List<AccountRecord> accounts
) {
}
