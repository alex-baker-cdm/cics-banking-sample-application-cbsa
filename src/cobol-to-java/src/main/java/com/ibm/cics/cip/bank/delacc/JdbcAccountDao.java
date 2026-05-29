/*
 * Copyright IBM Corp. 2023
 *
 * JDBC implementation of AccountDao.
 * SQL statements are migrated directly from DELACC.cbl.
 */
package com.ibm.cics.cip.bank.delacc;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JdbcAccountDao implements AccountDao {

    private static final Logger logger =
        Logger.getLogger(JdbcAccountDao.class.getName());

    static final String SELECT_ACCOUNT_SQL =
        "SELECT ACCOUNT_EYECATCHER, ACCOUNT_CUSTOMER_NUMBER, "
        + "ACCOUNT_SORTCODE, ACCOUNT_NUMBER, ACCOUNT_TYPE, "
        + "ACCOUNT_INTEREST_RATE, ACCOUNT_OPENED, "
        + "ACCOUNT_OVERDRAFT_LIMIT, ACCOUNT_LAST_STATEMENT, "
        + "ACCOUNT_NEXT_STATEMENT, ACCOUNT_AVAILABLE_BALANCE, "
        + "ACCOUNT_ACTUAL_BALANCE "
        + "FROM ACCOUNT "
        + "WHERE ACCOUNT_NUMBER = ? AND ACCOUNT_SORTCODE = ?";

    static final String DELETE_ACCOUNT_SQL =
        "DELETE FROM ACCOUNT "
        + "WHERE ACCOUNT_SORTCODE = ? AND ACCOUNT_NUMBER = ?";

    private final Connection connection;

    public JdbcAccountDao(Connection connection) {
        this.connection = Objects.requireNonNull(connection,
            "connection must not be null");
    }

    @Override
    public Optional<AccountRecord> findByAccountNumberAndSortCode(
            String accountNumber, String sortCode) {
        try (PreparedStatement stmt = connection.prepareStatement(SELECT_ACCOUNT_SQL)) {
            stmt.setString(1, accountNumber);
            stmt.setString(2, sortCode);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAccountRecord(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE,
                "Issue with ACCOUNT row select. SQLCODE error for Account "
                    + accountNumber + " and Sortcode " + sortCode, e);
            throw new RuntimeException(
                "Failed to read account " + accountNumber, e);
        }
    }

    @Override
    public boolean deleteByAccountNumberAndSortCode(
            String accountNumber, String sortCode) {
        try (PreparedStatement stmt = connection.prepareStatement(DELETE_ACCOUNT_SQL)) {
            stmt.setString(1, sortCode);
            stmt.setString(2, accountNumber);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE,
                "Issue with ACCOUNT row delete for Account "
                    + accountNumber + " and Sortcode " + sortCode, e);
            throw new RuntimeException(
                "Failed to delete account " + accountNumber, e);
        }
    }

    private AccountRecord mapResultSetToAccountRecord(ResultSet rs)
            throws SQLException {
        String eyeCatcher = rs.getString("ACCOUNT_EYECATCHER");
        String customerNumber = rs.getString("ACCOUNT_CUSTOMER_NUMBER");
        String sortCode = rs.getString("ACCOUNT_SORTCODE");
        String accountNumber = rs.getString("ACCOUNT_NUMBER");
        String accountType = rs.getString("ACCOUNT_TYPE");
        BigDecimal interestRate = rs.getBigDecimal("ACCOUNT_INTEREST_RATE");
        Date openedDate = rs.getDate("ACCOUNT_OPENED");
        int overdraftLimit = rs.getInt("ACCOUNT_OVERDRAFT_LIMIT");
        Date lastStmtDate = rs.getDate("ACCOUNT_LAST_STATEMENT");
        Date nextStmtDate = rs.getDate("ACCOUNT_NEXT_STATEMENT");
        BigDecimal availableBalance = rs.getBigDecimal("ACCOUNT_AVAILABLE_BALANCE");
        BigDecimal actualBalance = rs.getBigDecimal("ACCOUNT_ACTUAL_BALANCE");

        return new AccountRecord(
            eyeCatcher,
            customerNumber,
            sortCode,
            accountNumber,
            accountType,
            interestRate,
            openedDate != null ? openedDate.toLocalDate() : null,
            overdraftLimit,
            lastStmtDate != null ? lastStmtDate.toLocalDate() : null,
            nextStmtDate != null ? nextStmtDate.toLocalDate() : null,
            availableBalance,
            actualBalance
        );
    }
}
