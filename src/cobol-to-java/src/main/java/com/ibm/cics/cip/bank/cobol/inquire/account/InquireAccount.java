/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.cobol.inquire.account;

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
 * Java 21 migration of the COBOL program INQACC (INQACC.cbl).
 *
 * <p>This program takes an incoming account number and retrieves the associated
 * account record from the ACCOUNT DB2 table, matching on account number and
 * sort code. If the special sentinel account number 99999999 is supplied, the
 * program retrieves the last (highest-numbered) account for the sort code.</p>
 *
 * <h3>COBOL sections mapped to Java methods:</h3>
 * <ul>
 *   <li>PREMIERE / A010          &rarr; {@link #inquireAccount(InquireAccountRequest)}</li>
 *   <li>READ-ACCOUNT-DB2         &rarr; {@link #readAccountDb2(int, int)}</li>
 *   <li>FETCH-DATA               &rarr; handled inside {@link #readAccountDb2(int, int)}</li>
 *   <li>READ-ACCOUNT-LAST        &rarr; {@link #readLastAccount(int)}</li>
 *   <li>GET-LAST-ACCOUNT-DB2     &rarr; {@link #getLastAccountDb2(int)}</li>
 *   <li>CHECK-FOR-STORM-DRAIN-DB2&rarr; {@link #checkForStormDrainDb2(int)}</li>
 * </ul>
 *
 * <h3>Original COBOL SORTCODE default:</h3>
 * <p>The SORTCODE copybook defines {@code 77 SORTCODE PIC 9(6) VALUE 987654}.
 * This is used as the default sort code when the program is invoked.</p>
 */
public class InquireAccount {

    private static final Logger logger =
            Logger.getLogger(InquireAccount.class.getName());

    public static final int DEFAULT_SORT_CODE = 987654;

    static final String SQL_SELECT_BY_ACCOUNT =
            "SELECT ACCOUNT_EYECATCHER, ACCOUNT_CUSTOMER_NUMBER, "
            + "ACCOUNT_SORTCODE, ACCOUNT_NUMBER, ACCOUNT_TYPE, "
            + "ACCOUNT_INTEREST_RATE, ACCOUNT_OPENED, "
            + "ACCOUNT_OVERDRAFT_LIMIT, ACCOUNT_LAST_STATEMENT, "
            + "ACCOUNT_NEXT_STATEMENT, ACCOUNT_AVAILABLE_BALANCE, "
            + "ACCOUNT_ACTUAL_BALANCE "
            + "FROM ACCOUNT "
            + "WHERE ACCOUNT_SORTCODE = ? AND ACCOUNT_NUMBER = ?";

    static final String SQL_SELECT_LAST_ACCOUNT =
            "SELECT ACCOUNT_EYECATCHER, ACCOUNT_CUSTOMER_NUMBER, "
            + "ACCOUNT_SORTCODE, ACCOUNT_NUMBER, ACCOUNT_TYPE, "
            + "ACCOUNT_INTEREST_RATE, ACCOUNT_OPENED, "
            + "ACCOUNT_OVERDRAFT_LIMIT, ACCOUNT_LAST_STATEMENT, "
            + "ACCOUNT_NEXT_STATEMENT, ACCOUNT_AVAILABLE_BALANCE, "
            + "ACCOUNT_ACTUAL_BALANCE "
            + "FROM ACCOUNT "
            + "WHERE ACCOUNT_SORTCODE = ? "
            + "ORDER BY ACCOUNT_NUMBER DESC "
            + "FETCH FIRST 1 ROWS ONLY";

    static final int STORM_DRAIN_SQLCODE_DB2_CONNECTION_LOST = 923;

    private final Connection connection;

    public InquireAccount(Connection connection) {
        this.connection = connection;
    }

    /**
     * Main entry point - equivalent to the COBOL PREMIERE SECTION / A010.
     *
     * <p>Takes the incoming account request and returns a response containing
     * the account data (if found) or a failure indicator.</p>
     *
     * <p>Business logic preserved from COBOL:</p>
     * <ul>
     *   <li>If the account number is 99999999, retrieves the last account
     *       for the default sort code.</li>
     *   <li>If the account type is blank or null, returns a failure response
     *       (INQACC-SUCCESS = 'N').</li>
     *   <li>Otherwise, populates the response with all account fields
     *       (INQACC-SUCCESS = 'Y').</li>
     * </ul>
     *
     * @param request the account inquiry request
     * @return the inquiry response with account data or failure indicator
     * @throws InquireAccountException on unrecoverable DB2 errors (abend)
     */
    public InquireAccountResponse inquireAccount(
            InquireAccountRequest request) {

        AccountData accountData;

        if (request.isLastAccountRequest()) {
            accountData = readLastAccount(request.getSortCode());
        } else {
            accountData = readAccountDb2(
                    request.getAccountNumber(), request.getSortCode());
        }

        if (!accountData.hasValidAccountType()) {
            return InquireAccountResponse.failure();
        }

        return InquireAccountResponse.fromAccountData(accountData);
    }

    /**
     * Reads an account by account number and sort code using a DB2 cursor.
     *
     * Equivalent to COBOL sections: READ-ACCOUNT-DB2 + FETCH-DATA.
     *
     * <p>The COBOL program opens a cursor, fetches one row, and closes
     * the cursor. If SQLCODE is non-zero at any step, it abends with
     * code 'HRAC'.</p>
     *
     * @param accountNumber the account number to look up
     * @param sortCode the sort code to match
     * @return the populated AccountData, or an empty AccountData if not found
     * @throws InquireAccountException if DB2 operations fail
     */
    AccountData readAccountDb2(int accountNumber, int sortCode) {
        String sortCodeStr = String.format("%06d", sortCode);
        String accountNumberStr = String.format("%08d", accountNumber);

        try (PreparedStatement stmt =
                     connection.prepareStatement(SQL_SELECT_BY_ACCOUNT)) {
            stmt.setString(1, sortCodeStr);
            stmt.setString(2, accountNumberStr);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    AccountData emptyAccount = new AccountData();
                    emptyAccount.setSortCode(sortCode);
                    emptyAccount.setAccountNumber(accountNumber);
                    return emptyAccount;
                }

                return mapResultSetToAccountData(rs);
            }
        } catch (SQLException e) {
            checkForStormDrainDb2(e.getErrorCode());

            throw new InquireAccountException("HRAC",
                    "Failure during DB2 account inquiry for account "
                            + accountNumberStr + " sort code " + sortCodeStr,
                    e.getErrorCode(), e);
        }
    }

    /**
     * Reads the last (highest-numbered) account for the given sort code.
     *
     * Equivalent to COBOL sections: READ-ACCOUNT-LAST + GET-LAST-ACCOUNT-DB2.
     *
     * <p>Uses ORDER BY ACCOUNT_NUMBER DESC / FETCH FIRST 1 ROWS ONLY to
     * retrieve the last account, matching the COBOL cursor behavior.</p>
     *
     * @param sortCode the sort code to match
     * @return the populated AccountData for the last account
     * @throws InquireAccountException if the DB2 SELECT fails (abend 'HNCS')
     */
    AccountData readLastAccount(int sortCode) {
        return getLastAccountDb2(sortCode);
    }

    /**
     * Executes the DB2 SELECT for the last account.
     *
     * Equivalent to COBOL section: GET-LAST-ACCOUNT-DB2 (GLAD010).
     *
     * @param sortCode the sort code to match
     * @return the populated AccountData, or an empty AccountData if not found
     * @throws InquireAccountException if the query fails (abend 'HNCS')
     */
    AccountData getLastAccountDb2(int sortCode) {
        String sortCodeStr = String.format("%06d", sortCode);

        try (PreparedStatement stmt =
                     connection.prepareStatement(SQL_SELECT_LAST_ACCOUNT)) {
            stmt.setString(1, sortCodeStr);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return new AccountData();
                }

                return mapResultSetToAccountData(rs);
            }
        } catch (SQLException e) {
            throw new InquireAccountException("HNCS",
                    "Account Named Counter / last-account SELECT failed "
                            + "for sort code " + sortCodeStr
                            + ". SQLCODE=" + e.getErrorCode(),
                    e.getErrorCode(), e);
        }
    }

    /**
     * Maps a DB2 ResultSet row to an AccountData object.
     *
     * <p>Equivalent to the COBOL MOVE statements in FETCH-DATA (FD010) and
     * GET-LAST-ACCOUNT-DB2 (GLAD010) that copy host variables to OUTPUT-DATA
     * fields, including the DB2 date reformatting logic.</p>
     *
     * <p>COBOL date reformatting: DB2 stores dates as 'YYYY-MM-DD'. The COBOL
     * program extracts day, month, and year components into separate fields
     * (ACCOUNT-OPENED-DAY, ACCOUNT-OPENED-MONTH, ACCOUNT-OPENED-YEAR). In
     * Java, we use {@link LocalDate} to represent the full date.</p>
     */
    AccountData mapResultSetToAccountData(ResultSet rs) throws SQLException {
        AccountData data = new AccountData();

        data.setEyeCatcher(trimOrEmpty(rs.getString("ACCOUNT_EYECATCHER")));
        data.setCustomerNumber(
                parseLong(rs.getString("ACCOUNT_CUSTOMER_NUMBER")));
        data.setSortCode(parseInt(rs.getString("ACCOUNT_SORTCODE")));
        data.setAccountNumber(parseInt(rs.getString("ACCOUNT_NUMBER")));
        data.setAccountType(trimOrEmpty(rs.getString("ACCOUNT_TYPE")));
        data.setInterestRate(
                rs.getBigDecimal("ACCOUNT_INTEREST_RATE") != null
                        ? rs.getBigDecimal("ACCOUNT_INTEREST_RATE")
                        : BigDecimal.ZERO);

        Date opened = rs.getDate("ACCOUNT_OPENED");
        if (opened != null) {
            data.setDateOpened(opened.toLocalDate());
        }

        data.setOverdraftLimit(rs.getInt("ACCOUNT_OVERDRAFT_LIMIT"));

        Date lastStmt = rs.getDate("ACCOUNT_LAST_STATEMENT");
        if (lastStmt != null) {
            data.setLastStatementDate(lastStmt.toLocalDate());
        }

        Date nextStmt = rs.getDate("ACCOUNT_NEXT_STATEMENT");
        if (nextStmt != null) {
            data.setNextStatementDate(nextStmt.toLocalDate());
        }

        data.setAvailableBalance(
                rs.getBigDecimal("ACCOUNT_AVAILABLE_BALANCE") != null
                        ? rs.getBigDecimal("ACCOUNT_AVAILABLE_BALANCE")
                        : BigDecimal.ZERO);
        data.setActualBalance(
                rs.getBigDecimal("ACCOUNT_ACTUAL_BALANCE") != null
                        ? rs.getBigDecimal("ACCOUNT_ACTUAL_BALANCE")
                        : BigDecimal.ZERO);

        return data;
    }

    /**
     * Checks whether the SQL error code indicates a storm-drain condition.
     *
     * Equivalent to COBOL section: CHECK-FOR-STORM-DRAIN-DB2 (CFSDCD010).
     *
     * <p>SQLCODE 923 means "DB2 Connection lost" and triggers storm drain
     * logging. All other SQL codes are classified as "Not Storm Drain".</p>
     *
     * @param sqlCode the SQL error code to evaluate
     */
    void checkForStormDrainDb2(int sqlCode) {
        String stormDrainCondition;

        if (sqlCode == STORM_DRAIN_SQLCODE_DB2_CONNECTION_LOST) {
            stormDrainCondition = "DB2 Connection lost";
        } else {
            stormDrainCondition = "Not Storm Drain";
        }

        if (!"Not Storm Drain".equals(stormDrainCondition)) {
            logger.log(Level.SEVERE,
                    "INQACC: Check-For-Storm-Drain-DB2: Storm Drain "
                            + "condition ({0}) has been met ({1}).",
                    new Object[]{stormDrainCondition,
                            String.valueOf(sqlCode)});
        }
    }

    private static String trimOrEmpty(String value) {
        return value != null ? value.trim() : "";
    }

    private static int parseInt(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
