/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.migrated.cobol.updateaccount;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Migrated from COBOL program UPDACC.cbl (Update Account).
 *
 * <p>Updates an account's type, interest rate, and overdraft limit in the
 * ACCOUNT table. The balance cannot be amended through this operation and
 * no transaction record is written to PROCTRAN.
 *
 * <p>Business rules preserved from the original COBOL:
 * <ul>
 *   <li>The account is looked up by sort code + account number.</li>
 *   <li>The account type must not be blank or start with a space.</li>
 *   <li>Only account type, interest rate, and overdraft limit are updated.</li>
 *   <li>On success the full account record is returned in the response.</li>
 * </ul>
 */
public final class UpdateAccount {

    private static final Logger logger =
            Logger.getLogger(UpdateAccount.class.getName());

    static final String SORT_CODE = "987654";

    private static final String SQL_SELECT =
            "SELECT ACCOUNT_EYECATCHER, "
                    + "ACCOUNT_CUSTOMER_NUMBER, "
                    + "ACCOUNT_SORTCODE, "
                    + "ACCOUNT_NUMBER, "
                    + "ACCOUNT_TYPE, "
                    + "ACCOUNT_INTEREST_RATE, "
                    + "ACCOUNT_OPENED, "
                    + "ACCOUNT_OVERDRAFT_LIMIT, "
                    + "ACCOUNT_LAST_STATEMENT, "
                    + "ACCOUNT_NEXT_STATEMENT, "
                    + "ACCOUNT_AVAILABLE_BALANCE, "
                    + "ACCOUNT_ACTUAL_BALANCE "
                    + "FROM ACCOUNT "
                    + "WHERE ACCOUNT_SORTCODE = ? "
                    + "AND ACCOUNT_NUMBER = ?";

    private static final String SQL_UPDATE =
            "UPDATE ACCOUNT "
                    + "SET ACCOUNT_TYPE = ?, "
                    + "ACCOUNT_INTEREST_RATE = ?, "
                    + "ACCOUNT_OVERDRAFT_LIMIT = ? "
                    + "WHERE ACCOUNT_SORTCODE = ? "
                    + "AND ACCOUNT_NUMBER = ?";

    private final Connection connection;

    public UpdateAccount(Connection connection) {
        this.connection = connection;
    }

    /**
     * Updates an account record identified by the request's account number.
     *
     * @param request contains the account number and the fields to update
     * @return response with the full account record on success, or a failure
     *         indicator with a message on error
     */
    public UpdateAccountResponse updateAccount(UpdateAccountRequest request) {
        UpdateAccountResponse response = new UpdateAccountResponse();

        String sortCode = SORT_CODE;
        String accountNumber = request.accountNumber();

        if (!selectAccount(sortCode, accountNumber, response)) {
            return response;
        }

        if (!validateAccountType(request.accountType(), response)) {
            return response;
        }

        if (!performUpdate(sortCode, accountNumber, request, response)) {
            return response;
        }

        response.setSuccess(true);
        return response;
    }

    private boolean selectAccount(String sortCode, String accountNumber,
            UpdateAccountResponse response) {
        try (PreparedStatement stmt = connection.prepareStatement(SQL_SELECT)) {
            stmt.setString(1, sortCode);
            stmt.setString(2, accountNumber);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    response.setSuccess(false);
                    String message = "Account not found for sortCode="
                            + sortCode + ", accountNumber=" + accountNumber;
                    response.setFailureMessage(message);
                    logger.log(Level.WARNING, message);
                    return false;
                }
                populateResponseFromResultSet(rs, response);
                return true;
            }
        } catch (SQLException e) {
            response.setSuccess(false);
            String message = "ERROR: UPDACC returned "
                    + e.getErrorCode() + " on SELECT";
            response.setFailureMessage(message);
            logger.log(Level.SEVERE, message, e);
            return false;
        }
    }

    private boolean validateAccountType(String accountType,
            UpdateAccountResponse response) {
        if (accountType == null
                || accountType.isBlank()
                || accountType.charAt(0) == ' ') {
            response.setSuccess(false);
            String message = "ERROR: UPDACC has invalid account-type";
            response.setFailureMessage(message);
            logger.log(Level.WARNING, message);
            return false;
        }
        return true;
    }

    private boolean performUpdate(String sortCode, String accountNumber,
            UpdateAccountRequest request, UpdateAccountResponse response) {
        try (PreparedStatement stmt = connection.prepareStatement(SQL_UPDATE)) {
            stmt.setString(1, request.accountType());
            stmt.setBigDecimal(2, request.interestRate());
            stmt.setInt(3, request.overdraftLimit());
            stmt.setString(4, sortCode);
            stmt.setString(5, accountNumber);

            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated == 0) {
                response.setSuccess(false);
                String message = "ERROR: UPDACC returned 0 rows on UPDATE";
                response.setFailureMessage(message);
                logger.log(Level.WARNING, message);
                return false;
            }

            response.setAccountType(request.accountType());
            response.setInterestRate(request.interestRate());
            response.setOverdraftLimit(request.overdraftLimit());
            return true;
        } catch (SQLException e) {
            response.setSuccess(false);
            String message = "ERROR: UPDACC returned "
                    + e.getErrorCode() + " on UPDATE";
            response.setFailureMessage(message);
            logger.log(Level.SEVERE, message, e);
            return false;
        }
    }

    private void populateResponseFromResultSet(ResultSet rs,
            UpdateAccountResponse response) throws SQLException {
        response.setEyeCatcher(rs.getString("ACCOUNT_EYECATCHER"));
        response.setCustomerNumber(
                rs.getString("ACCOUNT_CUSTOMER_NUMBER"));
        response.setSortCode(rs.getString("ACCOUNT_SORTCODE"));
        response.setAccountNumber(rs.getString("ACCOUNT_NUMBER"));
        response.setAccountType(rs.getString("ACCOUNT_TYPE"));
        response.setInterestRate(
                rs.getBigDecimal("ACCOUNT_INTEREST_RATE"));

        Date opened = rs.getDate("ACCOUNT_OPENED");
        if (opened != null) {
            response.setDateOpened(opened.toLocalDate());
        }

        response.setOverdraftLimit(
                rs.getInt("ACCOUNT_OVERDRAFT_LIMIT"));

        Date lastStmt = rs.getDate("ACCOUNT_LAST_STATEMENT");
        if (lastStmt != null) {
            response.setLastStatementDate(lastStmt.toLocalDate());
        }

        Date nextStmt = rs.getDate("ACCOUNT_NEXT_STATEMENT");
        if (nextStmt != null) {
            response.setNextStatementDate(nextStmt.toLocalDate());
        }

        response.setAvailableBalance(
                rs.getBigDecimal("ACCOUNT_AVAILABLE_BALANCE"));
        response.setActualBalance(
                rs.getBigDecimal("ACCOUNT_ACTUAL_BALANCE"));
    }
}
