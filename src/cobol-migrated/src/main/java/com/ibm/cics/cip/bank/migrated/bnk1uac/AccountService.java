/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from COBOL: BNK1UAC.cbl — INQ-ACC-DATA / UPD-ACC-DATA
 *
 * Service interface abstracting the CICS LINK to INQACC and UPDACC programs.
 */
package com.ibm.cics.cip.bank.migrated.bnk1uac;

public interface AccountService {

    AccountCommArea inquireAccount(int accountNumber);

    AccountCommArea updateAccount(AccountCommArea commArea);
}
