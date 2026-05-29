/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.migrated.cobol.dbcrfun;

import java.util.Optional;

/**
 * Abstracts access to the ACCOUNT DB2 table. Mapped from the embedded
 * SQL SELECT and UPDATE statements in the UPDATE-ACCOUNT-DB2 section
 * of DBCRFUN.cbl.
 */
public interface AccountRepository {

    /**
     * Retrieves an account by sort code and account number.
     * Equivalent to the SQL SELECT in UAD010 of DBCRFUN.cbl.
     *
     * @param sortCode      the sort code
     * @param accountNumber the account number
     * @return the account record, or empty if not found
     * @throws DataAccessException if a database error occurs (maps to
     *         non-zero SQLCODE other than +100)
     */
    Optional<AccountRecord> findBySortCodeAndAccountNumber(String sortCode,
            String accountNumber) throws DataAccessException;

    /**
     * Updates the balances on an existing account.
     * Equivalent to the SQL UPDATE in UAD010 of DBCRFUN.cbl.
     *
     * @param account the account record with updated balances
     * @throws DataAccessException if a database error occurs
     */
    void updateAccount(AccountRecord account) throws DataAccessException;
}
