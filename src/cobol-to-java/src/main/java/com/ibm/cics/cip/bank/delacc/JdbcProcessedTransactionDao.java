/*
 * Copyright IBM Corp. 2023
 *
 * JDBC implementation of ProcessedTransactionDao.
 * SQL INSERT is migrated directly from DELACC.cbl (WRITE-PROCTRAN-DB2).
 */
package com.ibm.cics.cip.bank.delacc;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JdbcProcessedTransactionDao implements ProcessedTransactionDao {

    private static final Logger logger =
        Logger.getLogger(JdbcProcessedTransactionDao.class.getName());

    static final String INSERT_PROCTRAN_SQL =
        "INSERT INTO PROCTRAN ("
        + "PROCTRAN_EYECATCHER, PROCTRAN_SORTCODE, PROCTRAN_NUMBER, "
        + "PROCTRAN_DATE, PROCTRAN_TIME, PROCTRAN_REF, "
        + "PROCTRAN_TYPE, PROCTRAN_DESC, PROCTRAN_AMOUNT"
        + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FORMATTER =
        DateTimeFormatter.ofPattern("HHmmss");

    private final Connection connection;

    public JdbcProcessedTransactionDao(Connection connection) {
        this.connection = Objects.requireNonNull(connection,
            "connection must not be null");
    }

    @Override
    public void insertDeleteAccountTransaction(
            String sortCode,
            String accountNumber,
            LocalDate transactionDate,
            LocalTime transactionTime,
            String reference,
            String transactionType,
            String description,
            BigDecimal amount) {

        try (PreparedStatement stmt = connection.prepareStatement(INSERT_PROCTRAN_SQL)) {
            stmt.setString(1, DeleteAccountService.PROCTRAN_EYECATCHER);
            stmt.setString(2, sortCode);
            stmt.setString(3, accountNumber);
            stmt.setString(4, transactionDate.format(DATE_FORMATTER));
            stmt.setString(5, transactionTime.format(TIME_FORMATTER));
            stmt.setString(6, reference);
            stmt.setString(7, transactionType);
            stmt.setString(8, description);
            stmt.setBigDecimal(9, amount);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE,
                "Unable to WRITE to PROCTRAN row datastore", e);
            throw new RuntimeException(
                "Failed to insert PROCTRAN record for account "
                    + accountNumber, e);
        }
    }
}
