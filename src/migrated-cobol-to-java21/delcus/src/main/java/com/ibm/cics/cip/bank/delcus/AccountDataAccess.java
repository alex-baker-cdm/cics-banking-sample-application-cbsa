/*
 *    Copyright IBM Corp. 2023
 *
 *    Abstracts INQACCCU and DELACC COBOL program LINKs.
 */
package com.ibm.cics.cip.bank.delcus;

/**
 * Interface abstracting account-related data access operations
 * originally performed via EXEC CICS LINK in the COBOL program.
 */
public interface AccountDataAccess {

    /**
     * Retrieves all accounts for a given customer number
     * (equivalent to EXEC CICS LINK PROGRAM('INQACCCU')).
     *
     * @param customerNumber the customer number
     * @return inquiry result containing the list of accounts
     */
    AccountInquiryResult getAccountsByCustomer(String customerNumber);

    /**
     * Deletes a single account
     * (equivalent to EXEC CICS LINK PROGRAM('DELACC')).
     *
     * @param accountNumber the account number to delete
     * @param applId        the application ID
     */
    void deleteAccount(int accountNumber, String applId);
}
