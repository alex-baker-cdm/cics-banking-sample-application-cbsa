/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.migrated.cobol.updateaccount;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateAccountTest {

    private Connection connection;

    @BeforeEach
    void setUp() throws SQLException {
        connection = DriverManager.getConnection(
                "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", "sa", "");
        createAccountTable();
    }

    @AfterEach
    void tearDown() throws SQLException {
        try (var stmt = connection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS ACCOUNT");
        }
        connection.close();
    }

    private void createAccountTable() throws SQLException {
        try (var stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE ACCOUNT (
                    ACCOUNT_EYECATCHER      VARCHAR(4),
                    ACCOUNT_CUSTOMER_NUMBER  VARCHAR(10),
                    ACCOUNT_SORTCODE         VARCHAR(6),
                    ACCOUNT_NUMBER           VARCHAR(8),
                    ACCOUNT_TYPE             VARCHAR(8),
                    ACCOUNT_INTEREST_RATE    DECIMAL(6,2),
                    ACCOUNT_OPENED           DATE,
                    ACCOUNT_OVERDRAFT_LIMIT  INTEGER,
                    ACCOUNT_LAST_STATEMENT   DATE,
                    ACCOUNT_NEXT_STATEMENT   DATE,
                    ACCOUNT_AVAILABLE_BALANCE DECIMAL(12,2),
                    ACCOUNT_ACTUAL_BALANCE    DECIMAL(12,2)
                )
                """);
        }
    }

    private void insertAccount(String eyeCatcher, String customerNumber,
            String sortCode, String accountNumber, String accountType,
            BigDecimal interestRate, LocalDate opened, int overdraftLimit,
            LocalDate lastStatement, LocalDate nextStatement,
            BigDecimal availableBalance, BigDecimal actualBalance)
            throws SQLException {
        String sql = """
                INSERT INTO ACCOUNT (
                    ACCOUNT_EYECATCHER, ACCOUNT_CUSTOMER_NUMBER,
                    ACCOUNT_SORTCODE, ACCOUNT_NUMBER,
                    ACCOUNT_TYPE, ACCOUNT_INTEREST_RATE,
                    ACCOUNT_OPENED, ACCOUNT_OVERDRAFT_LIMIT,
                    ACCOUNT_LAST_STATEMENT, ACCOUNT_NEXT_STATEMENT,
                    ACCOUNT_AVAILABLE_BALANCE, ACCOUNT_ACTUAL_BALANCE
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, eyeCatcher);
            stmt.setString(2, customerNumber);
            stmt.setString(3, sortCode);
            stmt.setString(4, accountNumber);
            stmt.setString(5, accountType);
            stmt.setBigDecimal(6, interestRate);
            stmt.setDate(7, Date.valueOf(opened));
            stmt.setInt(8, overdraftLimit);
            stmt.setDate(9, Date.valueOf(lastStatement));
            stmt.setDate(10, Date.valueOf(nextStatement));
            stmt.setBigDecimal(11, availableBalance);
            stmt.setBigDecimal(12, actualBalance);
            stmt.executeUpdate();
        }
    }

    private void insertStandardAccount() throws SQLException {
        insertAccount("ACCT", "0000000001", UpdateAccount.SORT_CODE,
                "00001234", "SAVING  ", new BigDecimal("3.50"),
                LocalDate.of(2022, 1, 15), 500,
                LocalDate.of(2023, 6, 1), LocalDate.of(2023, 7, 1),
                new BigDecimal("1500.00"), new BigDecimal("1500.00"));
    }

    @Nested
    @DisplayName("Successful update scenarios")
    class SuccessfulUpdates {

        @Test
        @DisplayName("Should update account type, interest rate, and overdraft limit")
        void updateAllThreeFields() throws SQLException {
            insertStandardAccount();
            var service = new UpdateAccount(connection);
            var request = new UpdateAccountRequest(
                    "00001234", "ISA     ", new BigDecimal("4.25"), 1000);

            UpdateAccountResponse response = service.updateAccount(request);

            assertTrue(response.isSuccess());
            assertEquals("ISA     ", response.getAccountType());
            assertEquals(new BigDecimal("4.25"), response.getInterestRate());
            assertEquals(1000, response.getOverdraftLimit());
        }

        @Test
        @DisplayName("Should return full account record on success")
        void returnsFullRecord() throws SQLException {
            insertStandardAccount();
            var service = new UpdateAccount(connection);
            var request = new UpdateAccountRequest(
                    "00001234", "CURRENT ", new BigDecimal("2.00"), 200);

            UpdateAccountResponse response = service.updateAccount(request);

            assertTrue(response.isSuccess());
            assertEquals("ACCT", response.getEyeCatcher());
            assertEquals("0000000001", response.getCustomerNumber());
            assertEquals(UpdateAccount.SORT_CODE, response.getSortCode());
            assertEquals("00001234", response.getAccountNumber());
            assertEquals(LocalDate.of(2022, 1, 15), response.getDateOpened());
            assertEquals(LocalDate.of(2023, 6, 1),
                    response.getLastStatementDate());
            assertEquals(LocalDate.of(2023, 7, 1),
                    response.getNextStatementDate());
            assertNotNull(response.getAvailableBalance());
            assertNotNull(response.getActualBalance());
        }

        @Test
        @DisplayName("Should preserve balances (cannot be amended via this method)")
        void preservesBalances() throws SQLException {
            insertStandardAccount();
            var service = new UpdateAccount(connection);
            var request = new UpdateAccountRequest(
                    "00001234", "LOAN    ", new BigDecimal("5.00"), 0);

            UpdateAccountResponse response = service.updateAccount(request);

            assertTrue(response.isSuccess());
            assertEquals(0,
                    new BigDecimal("1500.00")
                            .compareTo(response.getAvailableBalance()));
            assertEquals(0,
                    new BigDecimal("1500.00")
                            .compareTo(response.getActualBalance()));
        }

        @Test
        @DisplayName("Should update with zero interest rate")
        void updateWithZeroInterestRate() throws SQLException {
            insertStandardAccount();
            var service = new UpdateAccount(connection);
            var request = new UpdateAccountRequest(
                    "00001234", "CURRENT ", BigDecimal.ZERO, 500);

            UpdateAccountResponse response = service.updateAccount(request);

            assertTrue(response.isSuccess());
            assertEquals(0,
                    BigDecimal.ZERO.compareTo(response.getInterestRate()));
        }

        @Test
        @DisplayName("Should update with zero overdraft limit")
        void updateWithZeroOverdraftLimit() throws SQLException {
            insertStandardAccount();
            var service = new UpdateAccount(connection);
            var request = new UpdateAccountRequest(
                    "00001234", "ISA     ", new BigDecimal("3.50"), 0);

            UpdateAccountResponse response = service.updateAccount(request);

            assertTrue(response.isSuccess());
            assertEquals(0, response.getOverdraftLimit());
        }

        @Test
        @DisplayName("Should handle account type change from LOAN to ISA")
        void changeLoanToIsa() throws SQLException {
            insertAccount("ACCT", "0000000002", UpdateAccount.SORT_CODE,
                    "00005678", "LOAN    ", new BigDecimal("6.50"),
                    LocalDate.of(2021, 3, 10), 2000,
                    LocalDate.of(2023, 5, 1), LocalDate.of(2023, 6, 1),
                    new BigDecimal("5000.00"), new BigDecimal("5000.00"));
            var service = new UpdateAccount(connection);
            var request = new UpdateAccountRequest(
                    "00005678", "ISA     ", new BigDecimal("1.50"), 0);

            UpdateAccountResponse response = service.updateAccount(request);

            assertTrue(response.isSuccess());
            assertEquals("ISA     ", response.getAccountType());
            assertEquals(new BigDecimal("1.50"), response.getInterestRate());
            assertEquals(0, response.getOverdraftLimit());
        }

        @Test
        @DisplayName("Should update account type that is exactly 8 chars")
        void fullLengthAccountType() throws SQLException {
            insertStandardAccount();
            var service = new UpdateAccount(connection);
            var request = new UpdateAccountRequest(
                    "00001234", "MORTGAGE", new BigDecimal("3.99"), 0);

            UpdateAccountResponse response = service.updateAccount(request);

            assertTrue(response.isSuccess());
            assertEquals("MORTGAGE", response.getAccountType());
        }
    }

    @Nested
    @DisplayName("Account not found scenarios")
    class AccountNotFound {

        @Test
        @DisplayName("Should fail when account number does not exist")
        void nonExistentAccountNumber() throws SQLException {
            insertStandardAccount();
            var service = new UpdateAccount(connection);
            var request = new UpdateAccountRequest(
                    "99999999", "ISA     ", new BigDecimal("3.50"), 500);

            UpdateAccountResponse response = service.updateAccount(request);

            assertFalse(response.isSuccess());
            assertNotNull(response.getFailureMessage());
            assertTrue(response.getFailureMessage().contains("not found"));
        }

        @Test
        @DisplayName("Should fail when table is empty")
        void emptyTable() throws SQLException {
            var service = new UpdateAccount(connection);
            var request = new UpdateAccountRequest(
                    "00001234", "ISA     ", new BigDecimal("3.50"), 500);

            UpdateAccountResponse response = service.updateAccount(request);

            assertFalse(response.isSuccess());
            assertNotNull(response.getFailureMessage());
        }
    }

    @Nested
    @DisplayName("Invalid account type validation")
    class InvalidAccountType {

        @Test
        @DisplayName("Should reject null account type")
        void nullAccountType() throws SQLException {
            insertStandardAccount();
            var service = new UpdateAccount(connection);
            var request = new UpdateAccountRequest(
                    "00001234", null, new BigDecimal("3.50"), 500);

            UpdateAccountResponse response = service.updateAccount(request);

            assertFalse(response.isSuccess());
            assertNotNull(response.getFailureMessage());
            assertTrue(response.getFailureMessage()
                    .contains("invalid account-type"));
        }

        @Test
        @DisplayName("Should reject account type that is all spaces")
        void allSpacesAccountType() throws SQLException {
            insertStandardAccount();
            var service = new UpdateAccount(connection);
            var request = new UpdateAccountRequest(
                    "00001234", "        ", new BigDecimal("3.50"), 500);

            UpdateAccountResponse response = service.updateAccount(request);

            assertFalse(response.isSuccess());
            assertTrue(response.getFailureMessage()
                    .contains("invalid account-type"));
        }

        @Test
        @DisplayName("Should reject account type starting with a space")
        void leadingSpaceAccountType() throws SQLException {
            insertStandardAccount();
            var service = new UpdateAccount(connection);
            var request = new UpdateAccountRequest(
                    "00001234", " ISA    ", new BigDecimal("3.50"), 500);

            UpdateAccountResponse response = service.updateAccount(request);

            assertFalse(response.isSuccess());
            assertTrue(response.getFailureMessage()
                    .contains("invalid account-type"));
        }

        @Test
        @DisplayName("Should reject empty string account type")
        void emptyStringAccountType() throws SQLException {
            insertStandardAccount();
            var service = new UpdateAccount(connection);
            var request = new UpdateAccountRequest(
                    "00001234", "", new BigDecimal("3.50"), 500);

            UpdateAccountResponse response = service.updateAccount(request);

            assertFalse(response.isSuccess());
            assertTrue(response.getFailureMessage()
                    .contains("invalid account-type"));
        }
    }

    @Nested
    @DisplayName("Sort code behavior")
    class SortCodeBehavior {

        @Test
        @DisplayName("Should use the hardcoded sort code 987654")
        void usesHardcodedSortCode() throws SQLException {
            insertAccount("ACCT", "0000000001", "987654",
                    "00001234", "SAVING  ", new BigDecimal("3.50"),
                    LocalDate.of(2022, 1, 15), 500,
                    LocalDate.of(2023, 6, 1), LocalDate.of(2023, 7, 1),
                    new BigDecimal("1000.00"), new BigDecimal("1000.00"));
            var service = new UpdateAccount(connection);
            var request = new UpdateAccountRequest(
                    "00001234", "ISA     ", new BigDecimal("2.00"), 100);

            UpdateAccountResponse response = service.updateAccount(request);

            assertTrue(response.isSuccess());
            assertEquals("987654", response.getSortCode());
        }

        @Test
        @DisplayName("Should not find account with different sort code")
        void differentSortCodeNotFound() throws SQLException {
            insertAccount("ACCT", "0000000001", "111111",
                    "00001234", "SAVING  ", new BigDecimal("3.50"),
                    LocalDate.of(2022, 1, 15), 500,
                    LocalDate.of(2023, 6, 1), LocalDate.of(2023, 7, 1),
                    new BigDecimal("1000.00"), new BigDecimal("1000.00"));
            var service = new UpdateAccount(connection);
            var request = new UpdateAccountRequest(
                    "00001234", "ISA     ", new BigDecimal("2.00"), 100);

            UpdateAccountResponse response = service.updateAccount(request);

            assertFalse(response.isSuccess());
        }
    }

    @Nested
    @DisplayName("Date handling")
    class DateHandling {

        @Test
        @DisplayName("Should correctly parse and return date opened")
        void dateOpenedParsing() throws SQLException {
            insertStandardAccount();
            var service = new UpdateAccount(connection);
            var request = new UpdateAccountRequest(
                    "00001234", "ISA     ", new BigDecimal("3.50"), 500);

            UpdateAccountResponse response = service.updateAccount(request);

            assertTrue(response.isSuccess());
            assertEquals(LocalDate.of(2022, 1, 15), response.getDateOpened());
        }

        @Test
        @DisplayName("Should correctly parse last and next statement dates")
        void statementDatesParsing() throws SQLException {
            insertStandardAccount();
            var service = new UpdateAccount(connection);
            var request = new UpdateAccountRequest(
                    "00001234", "ISA     ", new BigDecimal("3.50"), 500);

            UpdateAccountResponse response = service.updateAccount(request);

            assertTrue(response.isSuccess());
            assertEquals(LocalDate.of(2023, 6, 1),
                    response.getLastStatementDate());
            assertEquals(LocalDate.of(2023, 7, 1),
                    response.getNextStatementDate());
        }
    }

    @Nested
    @DisplayName("Multiple accounts isolation")
    class MultipleAccounts {

        @Test
        @DisplayName("Should update only the targeted account")
        void updatesOnlyTargetAccount() throws SQLException {
            insertStandardAccount();
            insertAccount("ACCT", "0000000002", UpdateAccount.SORT_CODE,
                    "00009999", "CURRENT ", new BigDecimal("1.00"),
                    LocalDate.of(2023, 3, 1), 100,
                    LocalDate.of(2023, 6, 1), LocalDate.of(2023, 7, 1),
                    new BigDecimal("2000.00"), new BigDecimal("2000.00"));

            var service = new UpdateAccount(connection);
            var request = new UpdateAccountRequest(
                    "00001234", "ISA     ", new BigDecimal("5.00"), 999);

            UpdateAccountResponse response = service.updateAccount(request);

            assertTrue(response.isSuccess());
            assertEquals("00001234", response.getAccountNumber());
            assertEquals("ISA     ", response.getAccountType());

            var verifyRequest = new UpdateAccountRequest(
                    "00009999", "CURRENT ", new BigDecimal("1.00"), 100);
            UpdateAccountResponse verifyResponse =
                    service.updateAccount(verifyRequest);
            assertTrue(verifyResponse.isSuccess());
            assertEquals("CURRENT ", verifyResponse.getAccountType());
        }
    }

    @Nested
    @DisplayName("SQL error handling")
    class SqlErrorHandling {

        @Test
        @DisplayName("Should handle closed connection gracefully on SELECT")
        void closedConnectionOnSelect() throws SQLException {
            connection.close();
            Connection closedConn = DriverManager.getConnection(
                    "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", "sa", "");
            try (var stmt = closedConn.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS ACCOUNT");
            }

            var service = new UpdateAccount(closedConn);
            var request = new UpdateAccountRequest(
                    "00001234", "ISA     ", new BigDecimal("3.50"), 500);

            UpdateAccountResponse response = service.updateAccount(request);

            assertFalse(response.isSuccess());
            assertNotNull(response.getFailureMessage());
            assertTrue(response.getFailureMessage().contains("SELECT"));
            closedConn.close();
            connection = DriverManager.getConnection(
                    "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", "sa", "");
            createAccountTable();
        }
    }

    @Nested
    @DisplayName("UpdateAccountRequest record behavior")
    class RequestRecordTests {

        @Test
        @DisplayName("Request record should provide accessors")
        void requestAccessors() {
            var request = new UpdateAccountRequest(
                    "00001234", "ISA     ", new BigDecimal("3.50"), 500);

            assertEquals("00001234", request.accountNumber());
            assertEquals("ISA     ", request.accountType());
            assertEquals(new BigDecimal("3.50"), request.interestRate());
            assertEquals(500, request.overdraftLimit());
        }

        @Test
        @DisplayName("Request records with same values should be equal")
        void requestEquality() {
            var r1 = new UpdateAccountRequest(
                    "00001234", "ISA     ", new BigDecimal("3.50"), 500);
            var r2 = new UpdateAccountRequest(
                    "00001234", "ISA     ", new BigDecimal("3.50"), 500);

            assertEquals(r1, r2);
            assertEquals(r1.hashCode(), r2.hashCode());
        }
    }

    @Nested
    @DisplayName("UpdateAccountResponse behavior")
    class ResponseTests {

        @Test
        @DisplayName("Response toString should contain key fields")
        void responseToString() throws SQLException {
            insertStandardAccount();
            var service = new UpdateAccount(connection);
            var request = new UpdateAccountRequest(
                    "00001234", "ISA     ", new BigDecimal("3.50"), 500);

            UpdateAccountResponse response = service.updateAccount(request);

            String str = response.toString();
            assertTrue(str.contains("success=true"));
            assertTrue(str.contains("accountNumber=00001234"));
            assertTrue(str.contains("accountType=ISA"));
        }

        @Test
        @DisplayName("Failed response should have null failure message initially unset fields")
        void failedResponseDefaults() {
            UpdateAccountResponse response = new UpdateAccountResponse();
            assertFalse(response.isSuccess());
            assertNull(response.getEyeCatcher());
            assertNull(response.getFailureMessage());
        }
    }

    @Nested
    @DisplayName("Boundary value tests")
    class BoundaryValues {

        @Test
        @DisplayName("Should handle maximum overdraft limit")
        void maxOverdraftLimit() throws SQLException {
            insertStandardAccount();
            var service = new UpdateAccount(connection);
            var request = new UpdateAccountRequest(
                    "00001234", "CURRENT ", new BigDecimal("3.50"), 99999999);

            UpdateAccountResponse response = service.updateAccount(request);

            assertTrue(response.isSuccess());
            assertEquals(99999999, response.getOverdraftLimit());
        }

        @Test
        @DisplayName("Should handle maximum interest rate (9999.99)")
        void maxInterestRate() throws SQLException {
            insertStandardAccount();
            var service = new UpdateAccount(connection);
            var request = new UpdateAccountRequest(
                    "00001234", "CURRENT ", new BigDecimal("9999.99"), 500);

            UpdateAccountResponse response = service.updateAccount(request);

            assertTrue(response.isSuccess());
            assertEquals(0,
                    new BigDecimal("9999.99")
                            .compareTo(response.getInterestRate()));
        }

        @Test
        @DisplayName("Should handle single character account type")
        void singleCharAccountType() throws SQLException {
            insertStandardAccount();
            var service = new UpdateAccount(connection);
            var request = new UpdateAccountRequest(
                    "00001234", "X", new BigDecimal("3.50"), 500);

            UpdateAccountResponse response = service.updateAccount(request);

            assertTrue(response.isSuccess());
            assertEquals("X", response.getAccountType());
        }
    }

    @Nested
    @DisplayName("Balance precision tests")
    class BalancePrecision {

        @Test
        @DisplayName("Should preserve negative available balance")
        void negativeAvailableBalance() throws SQLException {
            insertAccount("ACCT", "0000000001", UpdateAccount.SORT_CODE,
                    "00001234", "CURRENT ", new BigDecimal("3.50"),
                    LocalDate.of(2022, 1, 15), 500,
                    LocalDate.of(2023, 6, 1), LocalDate.of(2023, 7, 1),
                    new BigDecimal("-250.50"), new BigDecimal("-250.50"));
            var service = new UpdateAccount(connection);
            var request = new UpdateAccountRequest(
                    "00001234", "CURRENT ", new BigDecimal("3.50"), 500);

            UpdateAccountResponse response = service.updateAccount(request);

            assertTrue(response.isSuccess());
            assertEquals(0,
                    new BigDecimal("-250.50")
                            .compareTo(response.getAvailableBalance()));
        }

        @Test
        @DisplayName("Should preserve large balance amounts")
        void largeBalance() throws SQLException {
            insertAccount("ACCT", "0000000001", UpdateAccount.SORT_CODE,
                    "00001234", "SAVING  ", new BigDecimal("3.50"),
                    LocalDate.of(2022, 1, 15), 500,
                    LocalDate.of(2023, 6, 1), LocalDate.of(2023, 7, 1),
                    new BigDecimal("9999999999.99"),
                    new BigDecimal("9999999999.99"));
            var service = new UpdateAccount(connection);
            var request = new UpdateAccountRequest(
                    "00001234", "ISA     ", new BigDecimal("3.50"), 500);

            UpdateAccountResponse response = service.updateAccount(request);

            assertTrue(response.isSuccess());
            assertEquals(0,
                    new BigDecimal("9999999999.99")
                            .compareTo(response.getAvailableBalance()));
        }
    }
}
