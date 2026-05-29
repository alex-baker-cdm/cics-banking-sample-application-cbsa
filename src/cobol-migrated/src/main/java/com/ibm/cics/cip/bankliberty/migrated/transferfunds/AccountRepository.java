/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bankliberty.migrated.transferfunds;

import java.util.Optional;

/**
 * Repository interface for account and transaction persistence operations.
 * <p>
 * Abstracts the DB2 SQL operations from the COBOL program (EXEC SQL blocks)
 * so that the business logic can be tested independently of the database.
 */
public interface AccountRepository {

    /**
     * Reads an account by sort code and account number.
     * <p>
     * Migrated from the COBOL SELECT ... FROM ACCOUNT WHERE statements
     * in UPDATE-ACCOUNT-DB2-FROM and UPDATE-ACCOUNT-DB2-TO sections.
     *
     * @param sortCode      the bank sort code
     * @param accountNumber the account number
     * @return the account record, or empty if not found
     * @throws DataAccessException if a database error occurs (COBOL fail code '3')
     */
    Optional<AccountRecord> findAccount(String sortCode, String accountNumber)
            throws DataAccessException;

    /**
     * Updates an account record in the database.
     * <p>
     * Migrated from the COBOL UPDATE ACCOUNT SET ... WHERE statements
     * in UPDATE-ACCOUNT-DB2-FROM and UPDATE-ACCOUNT-DB2-TO sections.
     *
     * @param account the account record with updated balances
     * @throws DataAccessException if a database error occurs (COBOL fail code '3')
     */
    void updateAccount(AccountRecord account) throws DataAccessException;

    /**
     * Writes a processed transaction record.
     * <p>
     * Migrated from the COBOL INSERT INTO PROCTRAN statement
     * in WRITE-TO-PROCTRAN-DB2 section.
     *
     * @param transaction the processed transaction record
     * @throws DataAccessException if the write fails
     */
    void writeProcessedTransaction(ProcessedTransactionRecord transaction)
            throws DataAccessException;

    /**
     * Rolls back the current transaction.
     * <p>
     * Migrated from the COBOL EXEC CICS SYNCPOINT ROLLBACK statements.
     *
     * @throws DataAccessException if the rollback itself fails
     */
    void rollback() throws DataAccessException;
}
