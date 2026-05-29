/*
 * Copyright IBM Corp. 2023
 *
 * JDBC-based implementation of AccountDataStore, replacing the embedded SQL
 * in the original COBOL program.
 */
package com.ibm.cics.cip.bank.bankdata;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class JdbcAccountDataStore implements AccountDataStore {

    private final Connection connection;

    public JdbcAccountDataStore(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void deleteAccountsBySortCode(String sortCode) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM ACCOUNT WHERE ACCOUNT_SORTCODE = ?")) {
            ps.setString(1, sortCode);
            ps.executeUpdate();
        }
    }

    @Override
    public void deleteControlRecord(String controlName) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM CONTROL WHERE CONTROL_NAME = ?")) {
            ps.setString(1, controlName);
            ps.executeUpdate();
        }
    }

    @Override
    public void insertAccount(AccountRecord record) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO ACCOUNT ("
                        + "ACCOUNT_EYECATCHER, ACCOUNT_CUSTOMER_NUMBER, "
                        + "ACCOUNT_SORTCODE, ACCOUNT_NUMBER, "
                        + "ACCOUNT_TYPE, ACCOUNT_INTEREST_RATE, "
                        + "ACCOUNT_OPENED, ACCOUNT_OVERDRAFT_LIMIT, "
                        + "ACCOUNT_LAST_STATEMENT, ACCOUNT_NEXT_STATEMENT, "
                        + "ACCOUNT_AVAILABLE_BALANCE, ACCOUNT_ACTUAL_BALANCE"
                        + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, record.eyecatcher());
            ps.setString(2, record.customerNumber());
            ps.setString(3, record.sortCode());
            ps.setString(4, record.accountNumber());
            ps.setString(5, record.accountType());
            ps.setBigDecimal(6, record.interestRate());
            ps.setString(7, record.openedDate());
            ps.setInt(8, record.overdraftLimit());
            ps.setString(9, record.lastStatementDate());
            ps.setString(10, record.nextStatementDate());
            ps.setBigDecimal(11, record.availableBalance());
            ps.setBigDecimal(12, record.actualBalance());
            ps.executeUpdate();
        }
    }

    @Override
    public void insertControlRecord(String controlName, int valueNum,
            String valueStr) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO CONTROL (CONTROL_NAME, CONTROL_VALUE_NUM, "
                        + "CONTROL_VALUE_STR) VALUES (?, ?, ?)")) {
            ps.setString(1, controlName);
            ps.setInt(2, valueNum);
            ps.setString(3, valueStr);
            ps.executeUpdate();
        }
    }

    @Override
    public void commit() throws SQLException {
        connection.commit();
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }
}
