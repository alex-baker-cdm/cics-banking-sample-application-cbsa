/*
 * Copyright IBM Corp. 2023
 *
 * Abstraction for the DB2 ACCOUNT and CONTROL table operations.
 * In the original COBOL, embedded SQL was used to interact with DB2.
 * This interface allows different backing implementations (JDBC, in-memory).
 */
package com.ibm.cics.cip.bank.bankdata;

import java.sql.SQLException;

public interface AccountDataStore extends AutoCloseable {

    void deleteAccountsBySortCode(String sortCode) throws SQLException;

    void deleteControlRecord(String controlName) throws SQLException;

    void insertAccount(AccountRecord record) throws SQLException;

    void insertControlRecord(String controlName, int valueNum, String valueStr)
            throws SQLException;

    void commit() throws SQLException;

    @Override
    void close() throws SQLException;
}
