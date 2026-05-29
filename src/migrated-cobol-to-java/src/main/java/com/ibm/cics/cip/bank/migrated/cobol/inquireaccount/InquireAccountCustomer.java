/*
 *
 *    Copyright IBM Corp. 2023
 *
 */

package com.ibm.cics.cip.bank.migrated.cobol.inquireaccount;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

/**
 * Java 21 migration of COBOL program INQACCCU (Inquire Account for Customer).
 *
 * <p>Takes a customer number, verifies the customer exists via
 * {@link CustomerInquiryService}, then queries all accounts belonging
 * to that customer from the ACCOUNT DB2 table (capped at
 * {@value AccountDetail#MAX_ACCOUNTS_PER_CUSTOMER} accounts).</p>
 *
 * <p>The original COBOL source is located at
 * {@code src/base/cobol_src/INQACCCU.cbl}.</p>
 */
public class InquireAccountCustomer {

    private static final Logger logger = Logger.getLogger(
            InquireAccountCustomer.class.getName());

    private static final String ACCOUNT_QUERY = """
            SELECT ACCOUNT_EYECATCHER,
                   ACCOUNT_CUSTOMER_NUMBER,
                   ACCOUNT_SORTCODE,
                   ACCOUNT_NUMBER,
                   ACCOUNT_TYPE,
                   ACCOUNT_INTEREST_RATE,
                   ACCOUNT_OPENED,
                   ACCOUNT_OVERDRAFT_LIMIT,
                   ACCOUNT_LAST_STATEMENT,
                   ACCOUNT_NEXT_STATEMENT,
                   ACCOUNT_AVAILABLE_BALANCE,
                   ACCOUNT_ACTUAL_BALANCE
              FROM ACCOUNT
             WHERE ACCOUNT_CUSTOMER_NUMBER = ?
               AND ACCOUNT_SORTCODE = ?
            """;

    private static final DateTimeFormatter DB2_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final int CUSTOMER_NUMBER_LENGTH = 10;
    private static final int SORT_CODE_LENGTH = 6;
    private static final int STORM_DRAIN_SQLCODE = -923;

    private final DataSource dataSource;
    private final CustomerInquiryService customerInquiryService;
    private final String sortCode;

    public InquireAccountCustomer(DataSource dataSource,
                                  CustomerInquiryService customerInquiryService,
                                  String sortCode) {
        this.dataSource = dataSource;
        this.customerInquiryService = customerInquiryService;
        this.sortCode = padSortCode(sortCode);
    }

    public InquireAccountCustomer(DataSource dataSource,
                                  CustomerInquiryService customerInquiryService) {
        this(dataSource, customerInquiryService, "987654");
    }

    /**
     * Inquires about all accounts belonging to the given customer.
     *
     * <p>Business logic preserved from COBOL INQACCCU:</p>
     * <ol>
     *   <li>Validate customer number is not zero or 9999999999</li>
     *   <li>Verify customer exists via {@link CustomerInquiryService}</li>
     *   <li>Query ACCOUNT table for matching customer/sort-code</li>
     *   <li>Return up to 20 account detail records</li>
     * </ol>
     *
     * @param request the inquiry request containing the customer number
     * @return the inquiry response with account details and status
     */
    public InquireAccountCustomerResponse inquire(
            InquireAccountCustomerRequest request) {
        long customerNumber = request.customerNumber();

        if (!isValidCustomerNumber(customerNumber)) {
            logger.log(Level.FINE, () ->
                    "Customer number invalid or sentinel: " + customerNumber);
            return InquireAccountCustomerResponse.customerNotFound();
        }

        if (!customerInquiryService.isCustomerFound(customerNumber)) {
            logger.log(Level.INFO, () ->
                    "Customer not found: " + customerNumber);
            return InquireAccountCustomerResponse.customerNotFound();
        }

        return readAccountsFromDb(customerNumber);
    }

    private boolean isValidCustomerNumber(long customerNumber) {
        return customerNumber != 0
                && customerNumber != InquireAccountCustomerRequest.INVALID_CUSTOMER_NUMBER;
    }

    private InquireAccountCustomerResponse readAccountsFromDb(
            long customerNumber) {
        String paddedCustomerNumber = padCustomerNumber(customerNumber);
        List<AccountDetail> accounts = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(ACCOUNT_QUERY)) {

            stmt.setString(1, paddedCustomerNumber);
            stmt.setString(2, sortCode);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()
                        && accounts.size() < AccountDetail.MAX_ACCOUNTS_PER_CUSTOMER) {
                    accounts.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            String failCode = determineSqlFailCode(e);
            logSqlError(e, customerNumber, failCode);
            checkForStormDrain(e);
            return InquireAccountCustomerResponse.queryFailed(failCode);
        }

        logger.log(Level.FINE, () ->
                "Retrieved " + accounts.size()
                        + " accounts for customer " + customerNumber);
        return InquireAccountCustomerResponse.ok(accounts);
    }

    private AccountDetail mapRow(ResultSet rs) throws SQLException {
        String eyeCatcher = rs.getString("ACCOUNT_EYECATCHER");
        String custNo = rs.getString("ACCOUNT_CUSTOMER_NUMBER");
        String scode = rs.getString("ACCOUNT_SORTCODE");
        String accNo = rs.getString("ACCOUNT_NUMBER");
        String accType = rs.getString("ACCOUNT_TYPE");
        BigDecimal interestRate = rs.getBigDecimal("ACCOUNT_INTEREST_RATE");
        LocalDate opened = parseDb2Date(rs.getString("ACCOUNT_OPENED"));
        int overdraftLimit = rs.getInt("ACCOUNT_OVERDRAFT_LIMIT");
        LocalDate lastStmt = parseDb2Date(rs.getString("ACCOUNT_LAST_STATEMENT"));
        LocalDate nextStmt = parseDb2Date(rs.getString("ACCOUNT_NEXT_STATEMENT"));
        BigDecimal availBal = rs.getBigDecimal("ACCOUNT_AVAILABLE_BALANCE");
        BigDecimal actualBal = rs.getBigDecimal("ACCOUNT_ACTUAL_BALANCE");

        return new AccountDetail(
                eyeCatcher, custNo, scode, accNo, accType,
                interestRate, opened, overdraftLimit,
                lastStmt, nextStmt, availBal, actualBal);
    }

    private LocalDate parseDb2Date(String dateString) {
        if (dateString == null || dateString.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(dateString.trim(), DB2_DATE_FORMAT);
        } catch (DateTimeParseException e) {
            logger.log(Level.WARNING, () ->
                    "Unable to parse DB2 date: " + dateString);
            return null;
        }
    }

    private String determineSqlFailCode(SQLException e) {
        int errorCode = e.getErrorCode();
        String message = e.getMessage() != null ? e.getMessage() : "";

        if (message.contains("FETCH") || message.contains("fetch")) {
            return InquireAccountCustomerResponse.FAIL_CODE_FETCH;
        }
        if (message.contains("CLOSE") || message.contains("close")) {
            return InquireAccountCustomerResponse.FAIL_CODE_CURSOR_CLOSE;
        }
        if (errorCode == STORM_DRAIN_SQLCODE) {
            return InquireAccountCustomerResponse.FAIL_CODE_CURSOR_OPEN;
        }
        return InquireAccountCustomerResponse.FAIL_CODE_CURSOR_OPEN;
    }

    private void checkForStormDrain(SQLException e) {
        int errorCode = e.getErrorCode();
        if (errorCode == STORM_DRAIN_SQLCODE
                || errorCode == Math.abs(STORM_DRAIN_SQLCODE)) {
            logger.log(Level.SEVERE, () ->
                    "Storm Drain condition detected: DB2 Connection lost"
                            + " (SQLCODE=" + errorCode + ")");
        }
    }

    private void logSqlError(SQLException e, long customerNumber,
                             String failCode) {
        logger.log(Level.SEVERE, () ->
                "INQACCCU: SQL error querying accounts for customer "
                        + customerNumber + ", failCode=" + failCode
                        + ", SQLCODE=" + e.getErrorCode()
                        + ", message=" + e.getMessage());
    }

    static String padCustomerNumber(long customerNumber) {
        return String.format("%" + CUSTOMER_NUMBER_LENGTH + "d",
                customerNumber).replace(' ', '0');
    }

    static String padSortCode(String sortCode) {
        if (sortCode.length() >= SORT_CODE_LENGTH) {
            return sortCode.substring(0, SORT_CODE_LENGTH);
        }
        return "0".repeat(SORT_CODE_LENGTH - sortCode.length()) + sortCode;
    }
}
