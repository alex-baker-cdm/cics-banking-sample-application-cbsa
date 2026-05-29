/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.migrated.createaccount;

@FunctionalInterface
public interface AccountCreationService {

    CreateAccountResult createAccount(CreateAccountParams params);
}
