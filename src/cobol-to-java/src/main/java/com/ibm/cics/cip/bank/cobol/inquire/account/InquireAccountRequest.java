/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.cobol.inquire.account;

/**
 * Represents the input portion of the INQACC COMMAREA (INQACC.cpy).
 *
 * In the COBOL program, the COMMAREA carries both input and output fields.
 * This class captures only the input fields needed to drive the inquiry.
 *
 * COBOL fields mapped:
 *   03 INQACC-ACCNO   PIC 9(8)  -- the account number to look up
 *   03 INQACC-SCODE   PIC 9(6)  -- the sort code (may be overridden by default)
 *
 * Special value: account number 99999999 triggers a "last account" lookup.
 */
public class InquireAccountRequest {

    public static final int LAST_ACCOUNT_SENTINEL = 99999999;

    private final int accountNumber;
    private final int sortCode;

    public InquireAccountRequest(int accountNumber, int sortCode) {
        this.accountNumber = accountNumber;
        this.sortCode = sortCode;
    }

    public int getAccountNumber() {
        return accountNumber;
    }

    public int getSortCode() {
        return sortCode;
    }

    public boolean isLastAccountRequest() {
        return accountNumber == LAST_ACCOUNT_SENTINEL;
    }
}
