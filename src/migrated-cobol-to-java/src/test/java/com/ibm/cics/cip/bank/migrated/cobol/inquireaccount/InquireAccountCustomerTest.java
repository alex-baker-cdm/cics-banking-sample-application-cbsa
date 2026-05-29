/*
 *
 *    Copyright IBM Corp. 2023
 *
 */

package com.ibm.cics.cip.bank.migrated.cobol.inquireaccount;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InquireAccountCustomerTest {

    private static final String SORT_CODE = "987654";
    private static final long VALID_CUSTOMER = 1000000001L;
    private static final long UNKNOWN_CUSTOMER = 5555555555L;

    private DataSource dataSource;
    private Connection keepAlive;

    @BeforeEach
    void setUp() throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");
        dataSource = ds;

        keepAlive = dataSource.getConnection();
        try (Statement stmt = keepAlive.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS ACCOUNT (
                    ACCOUNT_EYECATCHER       VARCHAR(4),
                    ACCOUNT_CUSTOMER_NUMBER  VARCHAR(10),
                    ACCOUNT_SORTCODE         VARCHAR(6),
                    ACCOUNT_NUMBER           VARCHAR(8),
                    ACCOUNT_TYPE             VARCHAR(8),
                    ACCOUNT_INTEREST_RATE    DECIMAL(6,2),
                    ACCOUNT_OPENED           VARCHAR(10),
                    ACCOUNT_OVERDRAFT_LIMIT  INTEGER,
                    ACCOUNT_LAST_STATEMENT   VARCHAR(10),
                    ACCOUNT_NEXT_STATEMENT   VARCHAR(10),
                    ACCOUNT_AVAILABLE_BALANCE DECIMAL(12,2),
                    ACCOUNT_ACTUAL_BALANCE   DECIMAL(12,2)
                )
                """);
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        try (Statement stmt = keepAlive.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS ACCOUNT");
        }
        keepAlive.close();
    }

    private void insertAccount(String custNo, String sortCode, String accNo,
                               String type, BigDecimal rate, String opened,
                               int overdraft, String lastStmt, String nextStmt,
                               BigDecimal availBal, BigDecimal actualBal)
            throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO ACCOUNT VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            ps.setString(1, "ACCT");
            ps.setString(2, custNo);
            ps.setString(3, sortCode);
            ps.setString(4, accNo);
            ps.setString(5, type);
            ps.setBigDecimal(6, rate);
            ps.setString(7, opened);
            ps.setInt(8, overdraft);
            ps.setString(9, lastStmt);
            ps.setString(10, nextStmt);
            ps.setBigDecimal(11, availBal);
            ps.setBigDecimal(12, actualBal);
            ps.executeUpdate();
        }
    }

    private void insertSampleAccount(String accNo) throws SQLException {
        insertAccount(
                "1000000001", SORT_CODE, accNo, "SAVING",
                new BigDecimal("1.50"), "2023-01-15",
                500, "2023-06-01", "2023-12-01",
                new BigDecimal("1500.00"), new BigDecimal("1450.00"));
    }

    @Nested
    @DisplayName("Customer validation")
    class CustomerValidation {

        @Test
        @DisplayName("Returns customer-not-found when customer number is zero")
        void zeroCustomerNumber() {
            var service = new InquireAccountCustomer(
                    dataSource, cust -> true, SORT_CODE);
            var request = new InquireAccountCustomerRequest(0L);

            var response = service.inquire(request);

            assertFalse(response.success());
            assertEquals(InquireAccountCustomerResponse.FAIL_CODE_CUSTOMER_NOT_FOUND,
                    response.failCode());
            assertFalse(response.customerFound());
            assertEquals(0, response.numberOfAccounts());
        }

        @Test
        @DisplayName("Returns customer-not-found for sentinel value 9999999999")
        void sentinelCustomerNumber() {
            var service = new InquireAccountCustomer(
                    dataSource, cust -> true, SORT_CODE);
            var request = new InquireAccountCustomerRequest(9_999_999_999L);

            var response = service.inquire(request);

            assertFalse(response.success());
            assertEquals(InquireAccountCustomerResponse.FAIL_CODE_CUSTOMER_NOT_FOUND,
                    response.failCode());
            assertFalse(response.customerFound());
        }

        @Test
        @DisplayName("Returns customer-not-found when inquiry service says not found")
        void customerNotFoundByService() {
            var service = new InquireAccountCustomer(
                    dataSource, cust -> false, SORT_CODE);
            var request = new InquireAccountCustomerRequest(VALID_CUSTOMER);

            var response = service.inquire(request);

            assertFalse(response.success());
            assertEquals(InquireAccountCustomerResponse.FAIL_CODE_CUSTOMER_NOT_FOUND,
                    response.failCode());
            assertFalse(response.customerFound());
        }

        @Test
        @DisplayName("Rejects negative customer number at request creation")
        void negativeCustomerNumber() {
            assertThrows(IllegalArgumentException.class,
                    () -> new InquireAccountCustomerRequest(-1L));
        }
    }

    @Nested
    @DisplayName("Successful account retrieval")
    class SuccessfulRetrieval {

        @Test
        @DisplayName("Returns single account with all fields mapped correctly")
        void singleAccount() throws Exception {
            insertAccount(
                    "1000000001", SORT_CODE, "00000001", "CURRENT",
                    new BigDecimal("2.75"), "2022-03-10",
                    1000, "2023-05-01", "2023-11-01",
                    new BigDecimal("5000.50"), new BigDecimal("4500.25"));

            var service = new InquireAccountCustomer(
                    dataSource, cust -> true, SORT_CODE);
            var response = service.inquire(
                    new InquireAccountCustomerRequest(VALID_CUSTOMER));

            assertTrue(response.success());
            assertEquals(InquireAccountCustomerResponse.FAIL_CODE_NONE,
                    response.failCode());
            assertTrue(response.customerFound());
            assertEquals(1, response.numberOfAccounts());

            AccountDetail detail = response.accountDetails().get(0);
            assertEquals("ACCT", detail.eyeCatcher());
            assertEquals("1000000001", detail.customerNumber());
            assertEquals(SORT_CODE, detail.sortCode());
            assertEquals("00000001", detail.accountNumber());
            assertEquals("CURRENT", detail.accountType());
            assertEquals(new BigDecimal("2.75"), detail.interestRate());
            assertEquals(LocalDate.of(2022, 3, 10), detail.openedDate());
            assertEquals(1000, detail.overdraftLimit());
            assertEquals(LocalDate.of(2023, 5, 1), detail.lastStatementDate());
            assertEquals(LocalDate.of(2023, 11, 1), detail.nextStatementDate());
            assertEquals(new BigDecimal("5000.50"), detail.availableBalance());
            assertEquals(new BigDecimal("4500.25"), detail.actualBalance());
        }

        @Test
        @DisplayName("Returns multiple accounts for same customer")
        void multipleAccounts() throws Exception {
            insertSampleAccount("00000001");
            insertSampleAccount("00000002");
            insertSampleAccount("00000003");

            var service = new InquireAccountCustomer(
                    dataSource, cust -> true, SORT_CODE);
            var response = service.inquire(
                    new InquireAccountCustomerRequest(VALID_CUSTOMER));

            assertTrue(response.success());
            assertEquals(3, response.numberOfAccounts());
            assertEquals(3, response.accountDetails().size());
        }

        @Test
        @DisplayName("Returns empty list when customer has no accounts")
        void noAccounts() {
            var service = new InquireAccountCustomer(
                    dataSource, cust -> true, SORT_CODE);
            var response = service.inquire(
                    new InquireAccountCustomerRequest(VALID_CUSTOMER));

            assertTrue(response.success());
            assertEquals(0, response.numberOfAccounts());
            assertTrue(response.accountDetails().isEmpty());
        }

        @Test
        @DisplayName("Caps results at 20 accounts (MAX_ACCOUNTS_PER_CUSTOMER)")
        void maxAccountsCapped() throws Exception {
            for (int i = 1; i <= 25; i++) {
                insertSampleAccount(String.format("%08d", i));
            }

            var service = new InquireAccountCustomer(
                    dataSource, cust -> true, SORT_CODE);
            var response = service.inquire(
                    new InquireAccountCustomerRequest(VALID_CUSTOMER));

            assertTrue(response.success());
            assertEquals(AccountDetail.MAX_ACCOUNTS_PER_CUSTOMER,
                    response.numberOfAccounts());
        }
    }

    @Nested
    @DisplayName("Sort code filtering")
    class SortCodeFiltering {

        @Test
        @DisplayName("Only returns accounts matching the configured sort code")
        void filtersBySortCode() throws Exception {
            insertAccount(
                    "1000000001", SORT_CODE, "00000001", "SAVING",
                    new BigDecimal("1.00"), "2023-01-01",
                    0, "2023-06-01", "2023-12-01",
                    new BigDecimal("100.00"), new BigDecimal("100.00"));

            insertAccount(
                    "1000000001", "111111", "00000002", "SAVING",
                    new BigDecimal("1.00"), "2023-01-01",
                    0, "2023-06-01", "2023-12-01",
                    new BigDecimal("200.00"), new BigDecimal("200.00"));

            var service = new InquireAccountCustomer(
                    dataSource, cust -> true, SORT_CODE);
            var response = service.inquire(
                    new InquireAccountCustomerRequest(VALID_CUSTOMER));

            assertTrue(response.success());
            assertEquals(1, response.numberOfAccounts());
            assertEquals(SORT_CODE,
                    response.accountDetails().get(0).sortCode());
        }

        @Test
        @DisplayName("Uses default sort code 987654 when not specified")
        void defaultSortCode() throws Exception {
            insertSampleAccount("00000001");

            var service = new InquireAccountCustomer(
                    dataSource, cust -> true);
            var response = service.inquire(
                    new InquireAccountCustomerRequest(VALID_CUSTOMER));

            assertTrue(response.success());
            assertEquals(1, response.numberOfAccounts());
        }
    }

    @Nested
    @DisplayName("Database error handling")
    class DatabaseErrors {

        @Test
        @DisplayName("Returns query failure when database is unavailable")
        void databaseUnavailable() {
            JdbcDataSource badDs = new JdbcDataSource();
            badDs.setURL("jdbc:h2:mem:nonexistent;IFEXISTS=TRUE");
            badDs.setUser("sa");
            badDs.setPassword("");

            var service = new InquireAccountCustomer(
                    badDs, cust -> true, SORT_CODE);
            var response = service.inquire(
                    new InquireAccountCustomerRequest(VALID_CUSTOMER));

            assertFalse(response.success());
            assertFalse(response.customerFound());
            assertEquals(0, response.numberOfAccounts());
        }
    }

    @Nested
    @DisplayName("Date parsing")
    class DateParsing {

        @Test
        @DisplayName("Handles null date columns gracefully")
        void nullDates() throws Exception {
            insertAccount(
                    "1000000001", SORT_CODE, "00000001", "SAVING",
                    new BigDecimal("1.00"), null,
                    0, null, null,
                    new BigDecimal("0.00"), new BigDecimal("0.00"));

            var service = new InquireAccountCustomer(
                    dataSource, cust -> true, SORT_CODE);
            var response = service.inquire(
                    new InquireAccountCustomerRequest(VALID_CUSTOMER));

            assertTrue(response.success());
            assertEquals(1, response.numberOfAccounts());
            AccountDetail detail = response.accountDetails().get(0);
            assertEquals(null, detail.openedDate());
            assertEquals(null, detail.lastStatementDate());
            assertEquals(null, detail.nextStatementDate());
        }

        @Test
        @DisplayName("Parses standard DB2 date format yyyy-MM-dd")
        void standardDateFormat() throws Exception {
            insertAccount(
                    "1000000001", SORT_CODE, "00000001", "SAVING",
                    new BigDecimal("1.00"), "2024-12-25",
                    0, "2024-06-15", "2025-06-15",
                    new BigDecimal("0.00"), new BigDecimal("0.00"));

            var service = new InquireAccountCustomer(
                    dataSource, cust -> true, SORT_CODE);
            var response = service.inquire(
                    new InquireAccountCustomerRequest(VALID_CUSTOMER));

            assertTrue(response.success());
            AccountDetail detail = response.accountDetails().get(0);
            assertEquals(LocalDate.of(2024, 12, 25), detail.openedDate());
            assertEquals(LocalDate.of(2024, 6, 15), detail.lastStatementDate());
            assertEquals(LocalDate.of(2025, 6, 15), detail.nextStatementDate());
        }
    }

    @Nested
    @DisplayName("Padding utilities")
    class PaddingUtilities {

        @Test
        @DisplayName("Pads customer number to 10 digits with leading zeros")
        void padCustomerNumber() {
            assertEquals("0000000001",
                    InquireAccountCustomer.padCustomerNumber(1L));
            assertEquals("1000000001",
                    InquireAccountCustomer.padCustomerNumber(1_000_000_001L));
            assertEquals("0000000000",
                    InquireAccountCustomer.padCustomerNumber(0L));
        }

        @Test
        @DisplayName("Pads sort code to 6 digits with leading zeros")
        void padSortCode() {
            assertEquals("987654",
                    InquireAccountCustomer.padSortCode("987654"));
            assertEquals("001234",
                    InquireAccountCustomer.padSortCode("1234"));
            assertEquals("000001",
                    InquireAccountCustomer.padSortCode("1"));
        }

        @Test
        @DisplayName("Truncates sort code longer than 6 digits")
        void truncateSortCode() {
            assertEquals("123456",
                    InquireAccountCustomer.padSortCode("1234567"));
        }
    }

    @Nested
    @DisplayName("Response factory methods")
    class ResponseFactories {

        @Test
        @DisplayName("customerNotFound() returns correct fail state")
        void customerNotFoundFactory() {
            var response = InquireAccountCustomerResponse.customerNotFound();

            assertFalse(response.success());
            assertEquals("1", response.failCode());
            assertFalse(response.customerFound());
            assertEquals(0, response.numberOfAccounts());
            assertTrue(response.accountDetails().isEmpty());
        }

        @Test
        @DisplayName("queryFailed() returns correct fail state with given code")
        void queryFailedFactory() {
            var response = InquireAccountCustomerResponse.queryFailed("3");

            assertFalse(response.success());
            assertEquals("3", response.failCode());
            assertEquals(0, response.numberOfAccounts());
        }

        @Test
        @DisplayName("ok() returns success with accounts")
        void okFactory() {
            var accounts = List.of(
                    new AccountDetail("ACCT", "0000000001", "987654",
                            "00000001", "SAVING", new BigDecimal("1.50"),
                            LocalDate.of(2023, 1, 1), 500,
                            LocalDate.of(2023, 6, 1),
                            LocalDate.of(2023, 12, 1),
                            new BigDecimal("1000.00"),
                            new BigDecimal("950.00"))
            );
            var response = InquireAccountCustomerResponse.ok(accounts);

            assertTrue(response.success());
            assertEquals("0", response.failCode());
            assertTrue(response.customerFound());
            assertEquals(1, response.numberOfAccounts());
            assertNotNull(response.accountDetails());
        }

        @Test
        @DisplayName("Response account details list is unmodifiable")
        void accountDetailsUnmodifiable() {
            var response = InquireAccountCustomerResponse.ok(List.of());
            assertThrows(UnsupportedOperationException.class,
                    () -> response.accountDetails().add(null));
        }
    }

    @Nested
    @DisplayName("AccountDetail record")
    class AccountDetailTests {

        @Test
        @DisplayName("Constants are correct")
        void constants() {
            assertEquals("ACCT", AccountDetail.EYECATCHER_VALUE);
            assertEquals(20, AccountDetail.MAX_ACCOUNTS_PER_CUSTOMER);
        }
    }

    @Nested
    @DisplayName("InquireAccountCustomerRequest validation")
    class RequestValidation {

        @Test
        @DisplayName("Accepts valid customer number")
        void validCustomerNumber() {
            var request = new InquireAccountCustomerRequest(VALID_CUSTOMER);
            assertEquals(VALID_CUSTOMER, request.customerNumber());
        }

        @Test
        @DisplayName("Accepts zero customer number")
        void zeroCustomerNumber() {
            var request = new InquireAccountCustomerRequest(0L);
            assertEquals(0L, request.customerNumber());
        }

        @Test
        @DisplayName("Rejects negative customer number")
        void negativeCustomerNumber() {
            assertThrows(IllegalArgumentException.class,
                    () -> new InquireAccountCustomerRequest(-5L));
        }
    }

    @Nested
    @DisplayName("End-to-end integration")
    class Integration {

        @Test
        @DisplayName("Full flow: valid customer with multiple account types")
        void fullFlow() throws Exception {
            insertAccount(
                    "1000000001", SORT_CODE, "00000001", "CURRENT",
                    new BigDecimal("0.50"), "2020-01-15",
                    2000, "2023-06-01", "2023-12-01",
                    new BigDecimal("15000.75"), new BigDecimal("14500.25"));

            insertAccount(
                    "1000000001", SORT_CODE, "00000002", "SAVING",
                    new BigDecimal("3.25"), "2021-06-20",
                    0, "2023-06-01", "2023-12-01",
                    new BigDecimal("50000.00"), new BigDecimal("50000.00"));

            insertAccount(
                    "1000000001", SORT_CODE, "00000003", "ISA",
                    new BigDecimal("4.00"), "2022-04-06",
                    0, "2023-04-06", "2024-04-06",
                    new BigDecimal("20000.00"), new BigDecimal("20000.00"));

            var service = new InquireAccountCustomer(
                    dataSource, cust -> cust == VALID_CUSTOMER, SORT_CODE);
            var response = service.inquire(
                    new InquireAccountCustomerRequest(VALID_CUSTOMER));

            assertTrue(response.success());
            assertTrue(response.customerFound());
            assertEquals(3, response.numberOfAccounts());

            List<AccountDetail> details = response.accountDetails();
            assertEquals("CURRENT", details.get(0).accountType());
            assertEquals("SAVING", details.get(1).accountType());
            assertEquals("ISA", details.get(2).accountType());

            assertEquals(new BigDecimal("15000.75"),
                    details.get(0).availableBalance());
            assertEquals(new BigDecimal("50000.00"),
                    details.get(1).availableBalance());
            assertEquals(new BigDecimal("20000.00"),
                    details.get(2).availableBalance());
        }

        @Test
        @DisplayName("Different customers do not see each other's accounts")
        void customerIsolation() throws Exception {
            insertAccount(
                    "1000000001", SORT_CODE, "00000001", "CURRENT",
                    new BigDecimal("1.00"), "2023-01-01",
                    0, "2023-06-01", "2023-12-01",
                    new BigDecimal("100.00"), new BigDecimal("100.00"));

            insertAccount(
                    "2000000002", SORT_CODE, "00000002", "SAVING",
                    new BigDecimal("2.00"), "2023-02-01",
                    0, "2023-06-01", "2023-12-01",
                    new BigDecimal("200.00"), new BigDecimal("200.00"));

            var service = new InquireAccountCustomer(
                    dataSource, cust -> true, SORT_CODE);

            var response1 = service.inquire(
                    new InquireAccountCustomerRequest(VALID_CUSTOMER));
            assertEquals(1, response1.numberOfAccounts());
            assertEquals("00000001",
                    response1.accountDetails().get(0).accountNumber());

            var response2 = service.inquire(
                    new InquireAccountCustomerRequest(2_000_000_002L));
            assertEquals(1, response2.numberOfAccounts());
            assertEquals("00000002",
                    response2.accountDetails().get(0).accountNumber());
        }

        @Test
        @DisplayName("Handles accounts with zero balances")
        void zeroBalances() throws Exception {
            insertAccount(
                    "1000000001", SORT_CODE, "00000001", "CURRENT",
                    new BigDecimal("0.00"), "2023-01-01",
                    0, "2023-06-01", "2023-12-01",
                    new BigDecimal("0.00"), new BigDecimal("0.00"));

            var service = new InquireAccountCustomer(
                    dataSource, cust -> true, SORT_CODE);
            var response = service.inquire(
                    new InquireAccountCustomerRequest(VALID_CUSTOMER));

            assertTrue(response.success());
            assertEquals(1, response.numberOfAccounts());
            assertEquals(BigDecimal.ZERO.setScale(2),
                    response.accountDetails().get(0).availableBalance());
        }

        @Test
        @DisplayName("Handles accounts with negative balances (overdrawn)")
        void negativeBalances() throws Exception {
            insertAccount(
                    "1000000001", SORT_CODE, "00000001", "CURRENT",
                    new BigDecimal("1.00"), "2023-01-01",
                    500, "2023-06-01", "2023-12-01",
                    new BigDecimal("-250.50"), new BigDecimal("-300.75"));

            var service = new InquireAccountCustomer(
                    dataSource, cust -> true, SORT_CODE);
            var response = service.inquire(
                    new InquireAccountCustomerRequest(VALID_CUSTOMER));

            assertTrue(response.success());
            AccountDetail detail = response.accountDetails().get(0);
            assertEquals(new BigDecimal("-250.50"), detail.availableBalance());
            assertEquals(new BigDecimal("-300.75"), detail.actualBalance());
        }
    }
}
