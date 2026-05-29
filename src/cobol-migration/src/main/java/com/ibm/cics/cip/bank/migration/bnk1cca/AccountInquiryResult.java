/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from COBOL copybook INQACCCU.cpy - Result structure returned
 * by the INQACCCU program after querying accounts for a customer number.
 */
package com.ibm.cics.cip.bank.migration.bnk1cca;

import java.util.List;

/**
 * Encapsulates the full result from an account inquiry by customer number.
 * Maps to the INQACCCU-COMMAREA in the COBOL program.
 *
 * @param numberOfAccounts total number of accounts found (NUMBER-OF-ACCOUNTS)
 * @param customerNumber   10-digit customer number (CUSTOMER-NUMBER)
 * @param success          whether the inquiry succeeded (COMM-SUCCESS = 'Y')
 * @param failCode         failure reason code if not successful (COMM-FAIL-CODE)
 * @param customerFound    whether the customer exists (CUSTOMER-FOUND = 'Y')
 * @param accountDetails   list of account detail records
 */
public record AccountInquiryResult(
        int numberOfAccounts,
        String customerNumber,
        boolean success,
        char failCode,
        boolean customerFound,
        List<AccountDetail> accountDetails
) {
}
