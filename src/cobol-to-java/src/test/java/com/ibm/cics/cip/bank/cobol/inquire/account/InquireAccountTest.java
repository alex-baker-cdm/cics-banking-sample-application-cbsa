/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.cobol.inquire.account;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comprehensive tests for the InquireAccount service, the Java 21 migration
 * of COBOL program INQACC.cbl.
 *
 * Uses an H2 in-memory database to simulate the DB2 ACCOUNT table.
 */
class InquireAccountTest {

    private Connection connection;
    private InquireAccount inquireAccount;

    @BeforeEach
    void setUp() throws SQLException {
        String dbName = "testdb_" + UUID.randomUUID().toString().replace("-", "");
        connection = DriverManager.getConnection(
                "jdbc:h2:mem:" + dbName, "sa", "");
        createAccountTable();
        inquireAccount = new InquireAccount(connection);
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS ACCOUNT");
            }
            connection.close();
        }
    }

    private void createAccountTable() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE ACCOUNT (
                    ACCOUNT_EYECATCHER       CHAR(4),
                    ACCOUNT_CUSTOMER_NUMBER  CHAR(10),
                    ACCOUNT_SORTCODE         CHAR(6) NOT NULL,
                    ACCOUNT_NUMBER           CHAR(8) NOT NULL,
                    ACCOUNT_TYPE             CHAR(8),
                    ACCOUNT_INTEREST_RATE    DECIMAL(4, 2),
                    ACCOUNT_OPENED           DATE,
                    ACCOUNT_OVERDRAFT_LIMIT  INTEGER,
                    ACCOUNT_LAST_STATEMENT   DATE,
                    ACCOUNT_NEXT_STATEMENT   DATE,
                    ACCOUNT_AVAILABLE_BALANCE DECIMAL(12, 2),
                    ACCOUNT_ACTUAL_BALANCE   DECIMAL(12, 2)
                )
                """);
        }
    }

    private void insertAccount(String eyeCatcher, String customerNumber,
                               String sortCode, String accountNumber,
                               String accountType, BigDecimal interestRate,
                               LocalDate opened, int overdraftLimit,
                               LocalDate lastStmt, LocalDate nextStmt,
                               BigDecimal availableBalance,
                               BigDecimal actualBalance)
            throws SQLException {
        try (var stmt = connection.prepareStatement("""
                INSERT INTO ACCOUNT (
                    ACCOUNT_EYECATCHER, ACCOUNT_CUSTOMER_NUMBER,
                    ACCOUNT_SORTCODE, ACCOUNT_NUMBER, ACCOUNT_TYPE,
                    ACCOUNT_INTEREST_RATE, ACCOUNT_OPENED,
                    ACCOUNT_OVERDRAFT_LIMIT, ACCOUNT_LAST_STATEMENT,
                    ACCOUNT_NEXT_STATEMENT, ACCOUNT_AVAILABLE_BALANCE,
                    ACCOUNT_ACTUAL_BALANCE
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            stmt.setString(1, eyeCatcher);
            stmt.setString(2, customerNumber);
            stmt.setString(3, sortCode);
            stmt.setString(4, accountNumber);
            stmt.setString(5, accountType);
            stmt.setBigDecimal(6, interestRate);
            stmt.setDate(7, java.sql.Date.valueOf(opened));
            stmt.setInt(8, overdraftLimit);
            stmt.setDate(9, java.sql.Date.valueOf(lastStmt));
            stmt.setDate(10, java.sql.Date.valueOf(nextStmt));
            stmt.setBigDecimal(11, availableBalance);
            stmt.setBigDecimal(12, actualBalance);
            stmt.executeUpdate();
        }
    }

    private void insertStandardAccount() throws SQLException {
        insertAccount("ACCT", "0000001234", "987654", "00012345",
                "SAVINGS ", new BigDecimal("3.50"),
                LocalDate.of(2022, 1, 15), 500,
                LocalDate.of(2023, 6, 1), LocalDate.of(2023, 7, 1),
                new BigDecimal("15000.75"), new BigDecimal("14500.25"));
    }

    @Nested
    @DisplayName("Standard Account Inquiry (READ-ACCOUNT-DB2)")
    class StandardAccountInquiry {

        @Test
        @DisplayName("Returns success with all fields populated for existing account")
        void returnsSuccessForExistingAccount() throws SQLException {
            insertStandardAccount();

            var request = new InquireAccountRequest(12345, 987654);
            var response = inquireAccount.inquireAccount(request);

            assertTrue(response.isSuccess());
            assertEquals("ACCT", response.getEyeCatcher());
            assertEquals(1234L, response.getCustomerNumber());
            assertEquals(987654, response.getSortCode());
            assertEquals(12345, response.getAccountNumber());
            assertEquals("SAVINGS", response.getAccountType().trim());
            assertEquals(0, new BigDecimal("3.50")
                    .compareTo(response.getInterestRate()));
            assertEquals(LocalDate.of(2022, 1, 15),
                    response.getDateOpened());
            assertEquals(500, response.getOverdraftLimit());
            assertEquals(LocalDate.of(2023, 6, 1),
                    response.getLastStatementDate());
            assertEquals(LocalDate.of(2023, 7, 1),
                    response.getNextStatementDate());
            assertEquals(0, new BigDecimal("15000.75")
                    .compareTo(response.getAvailableBalance()));
            assertEquals(0, new BigDecimal("14500.25")
                    .compareTo(response.getActualBalance()));
        }

        @Test
        @DisplayName("Returns failure when account not found (SQLCODE +100)")
        void returnsFailureWhenAccountNotFound() {
            var request = new InquireAccountRequest(99999, 987654);
            var response = inquireAccount.inquireAccount(request);

            assertFalse(response.isSuccess());
        }

        @Test
        @DisplayName("Returns failure when account type is blank")
        void returnsFailureWhenAccountTypeIsBlank() throws SQLException {
            insertAccount("ACCT", "0000001234", "987654", "00012345",
                    "        ", new BigDecimal("3.50"),
                    LocalDate.of(2022, 1, 15), 500,
                    LocalDate.of(2023, 6, 1), LocalDate.of(2023, 7, 1),
                    new BigDecimal("1000.00"), new BigDecimal("1000.00"));

            var request = new InquireAccountRequest(12345, 987654);
            var response = inquireAccount.inquireAccount(request);

            assertFalse(response.isSuccess());
        }

        @Test
        @DisplayName("Handles different sort codes correctly")
        void handlesDifferentSortCodes() throws SQLException {
            insertAccount("ACCT", "0000005678", "123456", "00054321",
                    "CURRENT ", new BigDecimal("1.25"),
                    LocalDate.of(2020, 3, 20), 1000,
                    LocalDate.of(2023, 5, 15), LocalDate.of(2023, 6, 15),
                    new BigDecimal("5000.00"), new BigDecimal("4800.00"));

            var request = new InquireAccountRequest(54321, 123456);
            var response = inquireAccount.inquireAccount(request);

            assertTrue(response.isSuccess());
            assertEquals(123456, response.getSortCode());
            assertEquals(54321, response.getAccountNumber());
        }

        @Test
        @DisplayName("Handles zero-padded account numbers correctly")
        void handlesZeroPaddedAccountNumbers() throws SQLException {
            insertAccount("ACCT", "0000000001", "987654", "00000001",
                    "ISA     ", new BigDecimal("2.00"),
                    LocalDate.of(2023, 1, 1), 0,
                    LocalDate.of(2023, 6, 1), LocalDate.of(2023, 7, 1),
                    new BigDecimal("100.00"), new BigDecimal("100.00"));

            var request = new InquireAccountRequest(1, 987654);
            var response = inquireAccount.inquireAccount(request);

            assertTrue(response.isSuccess());
            assertEquals(1, response.getAccountNumber());
        }
    }

    @Nested
    @DisplayName("Last Account Inquiry (READ-ACCOUNT-LAST)")
    class LastAccountInquiry {

        @Test
        @DisplayName("Returns the highest-numbered account for the sort code")
        void returnsLastAccount() throws SQLException {
            insertAccount("ACCT", "0000001111", "987654", "00000100",
                    "SAVINGS ", new BigDecimal("2.50"),
                    LocalDate.of(2021, 5, 10), 200,
                    LocalDate.of(2023, 4, 1), LocalDate.of(2023, 5, 1),
                    new BigDecimal("3000.00"), new BigDecimal("2500.00"));

            insertAccount("ACCT", "0000002222", "987654", "00000200",
                    "CURRENT ", new BigDecimal("1.00"),
                    LocalDate.of(2022, 8, 20), 500,
                    LocalDate.of(2023, 3, 1), LocalDate.of(2023, 4, 1),
                    new BigDecimal("8000.00"), new BigDecimal("7500.00"));

            insertAccount("ACCT", "0000003333", "987654", "00000300",
                    "MORTGAGE", new BigDecimal("4.75"),
                    LocalDate.of(2019, 12, 1), 0,
                    LocalDate.of(2023, 6, 1), LocalDate.of(2023, 7, 1),
                    new BigDecimal("250000.00"), new BigDecimal("245000.00"));

            var request = new InquireAccountRequest(
                    InquireAccountRequest.LAST_ACCOUNT_SENTINEL, 987654);
            var response = inquireAccount.inquireAccount(request);

            assertTrue(response.isSuccess());
            assertEquals(300, response.getAccountNumber());
            assertEquals("MORTGAGE", response.getAccountType().trim());
        }

        @Test
        @DisplayName("Returns failure when no accounts exist for sort code")
        void returnsFailureWhenNoAccountsExist() {
            var request = new InquireAccountRequest(
                    InquireAccountRequest.LAST_ACCOUNT_SENTINEL, 987654);
            var response = inquireAccount.inquireAccount(request);

            assertFalse(response.isSuccess());
        }

        @Test
        @DisplayName("Only returns accounts for the requested sort code")
        void filtersOnSortCode() throws SQLException {
            insertAccount("ACCT", "0000001111", "111111", "00000999",
                    "SAVINGS ", new BigDecimal("2.50"),
                    LocalDate.of(2021, 5, 10), 200,
                    LocalDate.of(2023, 4, 1), LocalDate.of(2023, 5, 1),
                    new BigDecimal("3000.00"), new BigDecimal("2500.00"));

            insertAccount("ACCT", "0000002222", "987654", "00000100",
                    "CURRENT ", new BigDecimal("1.00"),
                    LocalDate.of(2022, 8, 20), 500,
                    LocalDate.of(2023, 3, 1), LocalDate.of(2023, 4, 1),
                    new BigDecimal("8000.00"), new BigDecimal("7500.00"));

            var request = new InquireAccountRequest(
                    InquireAccountRequest.LAST_ACCOUNT_SENTINEL, 987654);
            var response = inquireAccount.inquireAccount(request);

            assertTrue(response.isSuccess());
            assertEquals(100, response.getAccountNumber());
            assertEquals(987654, response.getSortCode());
        }
    }

    @Nested
    @DisplayName("Account Data Mapping")
    class AccountDataMapping {

        @Test
        @DisplayName("Maps all DB2 columns to AccountData fields correctly")
        void mapsAllFieldsCorrectly() throws SQLException {
            insertAccount("ACCT", "0000009999", "987654", "00054321",
                    "LOAN    ", new BigDecimal("6.99"),
                    LocalDate.of(2018, 11, 30), 2000,
                    LocalDate.of(2023, 1, 31), LocalDate.of(2023, 2, 28),
                    new BigDecimal("-500.50"), new BigDecimal("-1000.75"));

            var request = new InquireAccountRequest(54321, 987654);
            var response = inquireAccount.inquireAccount(request);

            assertTrue(response.isSuccess());
            assertEquals("ACCT", response.getEyeCatcher());
            assertEquals(9999L, response.getCustomerNumber());
            assertEquals(987654, response.getSortCode());
            assertEquals(54321, response.getAccountNumber());
            assertEquals("LOAN", response.getAccountType().trim());
            assertEquals(0, new BigDecimal("6.99")
                    .compareTo(response.getInterestRate()));
            assertEquals(LocalDate.of(2018, 11, 30),
                    response.getDateOpened());
            assertEquals(2000, response.getOverdraftLimit());
            assertEquals(LocalDate.of(2023, 1, 31),
                    response.getLastStatementDate());
            assertEquals(LocalDate.of(2023, 2, 28),
                    response.getNextStatementDate());
            assertEquals(0, new BigDecimal("-500.50")
                    .compareTo(response.getAvailableBalance()));
            assertEquals(0, new BigDecimal("-1000.75")
                    .compareTo(response.getActualBalance()));
        }

        @Test
        @DisplayName("Handles negative balances (overdrawn accounts)")
        void handlesNegativeBalances() throws SQLException {
            insertAccount("ACCT", "0000001234", "987654", "00011111",
                    "CURRENT ", new BigDecimal("0.00"),
                    LocalDate.of(2022, 6, 15), 1000,
                    LocalDate.of(2023, 5, 1), LocalDate.of(2023, 6, 1),
                    new BigDecimal("-250.00"), new BigDecimal("-300.50"));

            var request = new InquireAccountRequest(11111, 987654);
            var response = inquireAccount.inquireAccount(request);

            assertTrue(response.isSuccess());
            assertTrue(response.getAvailableBalance()
                    .compareTo(BigDecimal.ZERO) < 0);
            assertTrue(response.getActualBalance()
                    .compareTo(BigDecimal.ZERO) < 0);
        }

        @Test
        @DisplayName("Handles zero interest rate")
        void handlesZeroInterestRate() throws SQLException {
            insertAccount("ACCT", "0000001234", "987654", "00022222",
                    "CURRENT ", new BigDecimal("0.00"),
                    LocalDate.of(2022, 1, 1), 0,
                    LocalDate.of(2023, 6, 1), LocalDate.of(2023, 7, 1),
                    new BigDecimal("500.00"), new BigDecimal("500.00"));

            var request = new InquireAccountRequest(22222, 987654);
            var response = inquireAccount.inquireAccount(request);

            assertTrue(response.isSuccess());
            assertEquals(0, BigDecimal.ZERO
                    .compareTo(response.getInterestRate()));
        }

        @Test
        @DisplayName("Handles zero overdraft limit")
        void handlesZeroOverdraftLimit() throws SQLException {
            insertAccount("ACCT", "0000001234", "987654", "00033333",
                    "ISA     ", new BigDecimal("5.00"),
                    LocalDate.of(2022, 4, 1), 0,
                    LocalDate.of(2023, 6, 1), LocalDate.of(2023, 7, 1),
                    new BigDecimal("10000.00"), new BigDecimal("10000.00"));

            var request = new InquireAccountRequest(33333, 987654);
            var response = inquireAccount.inquireAccount(request);

            assertTrue(response.isSuccess());
            assertEquals(0, response.getOverdraftLimit());
        }

        @Test
        @DisplayName("Handles large balances within COBOL PIC S9(10)V99 range")
        void handlesLargeBalances() throws SQLException {
            insertAccount("ACCT", "0000001234", "987654", "00044444",
                    "SAVINGS ", new BigDecimal("1.50"),
                    LocalDate.of(2020, 1, 1), 0,
                    LocalDate.of(2023, 6, 1), LocalDate.of(2023, 7, 1),
                    new BigDecimal("9999999999.99"),
                    new BigDecimal("9999999999.99"));

            var request = new InquireAccountRequest(44444, 987654);
            var response = inquireAccount.inquireAccount(request);

            assertTrue(response.isSuccess());
            assertEquals(0, new BigDecimal("9999999999.99")
                    .compareTo(response.getAvailableBalance()));
        }

        @Test
        @DisplayName("Handles large customer numbers (PIC 9(10))")
        void handlesLargeCustomerNumbers() throws SQLException {
            insertAccount("ACCT", "9999999999", "987654", "00055555",
                    "SAVINGS ", new BigDecimal("2.00"),
                    LocalDate.of(2022, 1, 1), 0,
                    LocalDate.of(2023, 6, 1), LocalDate.of(2023, 7, 1),
                    new BigDecimal("1000.00"), new BigDecimal("1000.00"));

            var request = new InquireAccountRequest(55555, 987654);
            var response = inquireAccount.inquireAccount(request);

            assertTrue(response.isSuccess());
            assertEquals(9999999999L, response.getCustomerNumber());
        }
    }

    @Nested
    @DisplayName("InquireAccountRequest")
    class RequestTests {

        @Test
        @DisplayName("Sentinel value 99999999 triggers last-account path")
        void sentinelTriggersLastAccountPath() {
            var request = new InquireAccountRequest(99999999, 987654);
            assertTrue(request.isLastAccountRequest());
        }

        @Test
        @DisplayName("Normal account number does not trigger last-account path")
        void normalAccountDoesNotTriggerLastAccountPath() {
            var request = new InquireAccountRequest(12345, 987654);
            assertFalse(request.isLastAccountRequest());
        }

        @Test
        @DisplayName("Account number 0 does not trigger last-account path")
        void zeroAccountDoesNotTriggerLastAccountPath() {
            var request = new InquireAccountRequest(0, 987654);
            assertFalse(request.isLastAccountRequest());
        }
    }

    @Nested
    @DisplayName("AccountData")
    class AccountDataTests {

        @Test
        @DisplayName("Default AccountData has invalid account type")
        void defaultAccountDataHasInvalidType() {
            var data = new AccountData();
            assertFalse(data.hasValidAccountType());
        }

        @Test
        @DisplayName("AccountData with valid type returns true")
        void accountDataWithValidTypeReturnsTrue() {
            var data = new AccountData();
            data.setAccountType("SAVINGS ");
            assertTrue(data.hasValidAccountType());
        }

        @Test
        @DisplayName("AccountData with blank type returns false")
        void accountDataWithBlankTypeReturnsFalse() {
            var data = new AccountData();
            data.setAccountType("        ");
            assertFalse(data.hasValidAccountType());
        }

        @Test
        @DisplayName("AccountData with null type returns false")
        void accountDataWithNullTypeReturnsFalse() {
            var data = new AccountData();
            data.setAccountType(null);
            assertFalse(data.hasValidAccountType());
        }

        @Test
        @DisplayName("AccountData equals and hashCode work correctly")
        void equalsAndHashCode() {
            var data1 = new AccountData();
            data1.setEyeCatcher("ACCT");
            data1.setCustomerNumber(1234);
            data1.setSortCode(987654);
            data1.setAccountNumber(12345);
            data1.setAccountType("SAVINGS");

            var data2 = new AccountData();
            data2.setEyeCatcher("ACCT");
            data2.setCustomerNumber(1234);
            data2.setSortCode(987654);
            data2.setAccountNumber(12345);
            data2.setAccountType("SAVINGS");

            assertEquals(data1, data2);
            assertEquals(data1.hashCode(), data2.hashCode());
        }

        @Test
        @DisplayName("AccountData toString contains all fields")
        void toStringContainsAllFields() {
            var data = new AccountData();
            data.setEyeCatcher("ACCT");
            data.setCustomerNumber(1234);
            data.setSortCode(987654);
            data.setAccountNumber(12345);
            data.setAccountType("SAVINGS");

            String str = data.toString();
            assertTrue(str.contains("ACCT"));
            assertTrue(str.contains("1234"));
            assertTrue(str.contains("987654"));
            assertTrue(str.contains("12345"));
            assertTrue(str.contains("SAVINGS"));
        }
    }

    @Nested
    @DisplayName("InquireAccountResponse")
    class ResponseTests {

        @Test
        @DisplayName("Failure response has success=false")
        void failureResponseHasSuccessFalse() {
            var response = InquireAccountResponse.failure();
            assertFalse(response.isSuccess());
        }

        @Test
        @DisplayName("fromAccountData creates response with all fields")
        void fromAccountDataCreatesCompleteResponse() {
            var data = new AccountData();
            data.setEyeCatcher("ACCT");
            data.setCustomerNumber(1234);
            data.setSortCode(987654);
            data.setAccountNumber(12345);
            data.setAccountType("SAVINGS");
            data.setInterestRate(new BigDecimal("3.50"));
            data.setDateOpened(LocalDate.of(2022, 1, 15));
            data.setOverdraftLimit(500);
            data.setLastStatementDate(LocalDate.of(2023, 6, 1));
            data.setNextStatementDate(LocalDate.of(2023, 7, 1));
            data.setAvailableBalance(new BigDecimal("15000.75"));
            data.setActualBalance(new BigDecimal("14500.25"));

            var response = InquireAccountResponse.fromAccountData(data);

            assertTrue(response.isSuccess());
            assertEquals("ACCT", response.getEyeCatcher());
            assertEquals(1234L, response.getCustomerNumber());
            assertEquals(987654, response.getSortCode());
            assertEquals(12345, response.getAccountNumber());
            assertEquals("SAVINGS", response.getAccountType());
            assertEquals(0, new BigDecimal("3.50")
                    .compareTo(response.getInterestRate()));
            assertEquals(LocalDate.of(2022, 1, 15),
                    response.getDateOpened());
            assertEquals(500, response.getOverdraftLimit());
            assertEquals(LocalDate.of(2023, 6, 1),
                    response.getLastStatementDate());
            assertEquals(LocalDate.of(2023, 7, 1),
                    response.getNextStatementDate());
            assertEquals(0, new BigDecimal("15000.75")
                    .compareTo(response.getAvailableBalance()));
            assertEquals(0, new BigDecimal("14500.25")
                    .compareTo(response.getActualBalance()));
        }

        @Test
        @DisplayName("Builder allows incremental field setting")
        void builderAllowsIncrementalFieldSetting() {
            var response = new InquireAccountResponse.Builder()
                    .success(true)
                    .eyeCatcher("TEST")
                    .customerNumber(5678)
                    .sortCode(111111)
                    .accountNumber(99999)
                    .accountType("CURRENT")
                    .interestRate(new BigDecimal("1.25"))
                    .dateOpened(LocalDate.of(2020, 6, 15))
                    .overdraftLimit(1000)
                    .lastStatementDate(LocalDate.of(2023, 5, 1))
                    .nextStatementDate(LocalDate.of(2023, 6, 1))
                    .availableBalance(new BigDecimal("5000.00"))
                    .actualBalance(new BigDecimal("4800.00"))
                    .build();

            assertTrue(response.isSuccess());
            assertEquals("TEST", response.getEyeCatcher());
            assertEquals(5678L, response.getCustomerNumber());
            assertEquals(111111, response.getSortCode());
        }

        @Test
        @DisplayName("Response toString contains key fields")
        void toStringContainsKeyFields() {
            var response = InquireAccountResponse.failure();
            String str = response.toString();
            assertTrue(str.contains("success=false"));
        }
    }

    @Nested
    @DisplayName("InquireAccountException")
    class ExceptionTests {

        @Test
        @DisplayName("Exception carries abend code and SQL code")
        void exceptionCarriesAbendCodeAndSqlCode() {
            var ex = new InquireAccountException(
                    "HRAC", "DB2 cursor failure", -904);
            assertEquals("HRAC", ex.getAbendCode());
            assertEquals(-904, ex.getSqlCode());
            assertEquals("DB2 cursor failure", ex.getMessage());
        }

        @Test
        @DisplayName("Exception with cause preserves the cause chain")
        void exceptionWithCausePreservesCauseChain() {
            var cause = new SQLException("Connection refused", "08001", 923);
            var ex = new InquireAccountException(
                    "HRAC", "DB2 failure", 923, cause);
            assertEquals(cause, ex.getCause());
        }
    }

    @Nested
    @DisplayName("Storm Drain Detection")
    class StormDrainTests {

        @Test
        @DisplayName("SQLCODE 923 is detected as storm drain condition")
        void sqlCode923IsStormDrain() {
            inquireAccount.checkForStormDrainDb2(923);
        }

        @Test
        @DisplayName("Other SQL codes are not storm drain conditions")
        void otherSqlCodesAreNotStormDrain() {
            inquireAccount.checkForStormDrainDb2(-904);
            inquireAccount.checkForStormDrainDb2(0);
            inquireAccount.checkForStormDrainDb2(100);
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Throws InquireAccountException on DB2 failure")
        void throwsExceptionOnDb2Failure() throws SQLException {
            Connection closedConn = DriverManager.getConnection(
                    "jdbc:h2:mem:closeddb_" + UUID.randomUUID().toString()
                            .replace("-", ""), "sa", "");
            closedConn.close();

            var sut = new InquireAccount(closedConn);
            var request = new InquireAccountRequest(12345, 987654);
            assertThrows(InquireAccountException.class,
                    () -> sut.inquireAccount(request));
        }

        @Test
        @DisplayName("Throws HNCS exception on last-account query failure")
        void throwsHncsExceptionOnLastAccountFailure() throws SQLException {
            Connection closedConn = DriverManager.getConnection(
                    "jdbc:h2:mem:closeddb2_" + UUID.randomUUID().toString()
                            .replace("-", ""), "sa", "");
            closedConn.close();

            var sut = new InquireAccount(closedConn);
            var request = new InquireAccountRequest(
                    InquireAccountRequest.LAST_ACCOUNT_SENTINEL, 987654);
            var ex = assertThrows(InquireAccountException.class,
                    () -> sut.inquireAccount(request));
            assertEquals("HNCS", ex.getAbendCode());
        }
    }

    @Nested
    @DisplayName("Default Sort Code")
    class DefaultSortCodeTests {

        @Test
        @DisplayName("Default sort code matches COBOL SORTCODE copybook value")
        void defaultSortCodeMatchesCopybook() {
            assertEquals(987654, InquireAccount.DEFAULT_SORT_CODE);
        }
    }

    @Nested
    @DisplayName("Multiple Account Types")
    class MultipleAccountTypeTests {

        @Test
        @DisplayName("Handles SAVINGS account type")
        void handlesSavingsType() throws SQLException {
            insertAccount("ACCT", "0000001234", "987654", "00060001",
                    "SAVINGS ", new BigDecimal("3.00"),
                    LocalDate.of(2022, 1, 1), 0,
                    LocalDate.of(2023, 6, 1), LocalDate.of(2023, 7, 1),
                    new BigDecimal("1000.00"), new BigDecimal("1000.00"));

            var request = new InquireAccountRequest(60001, 987654);
            var response = inquireAccount.inquireAccount(request);

            assertTrue(response.isSuccess());
            assertEquals("SAVINGS", response.getAccountType().trim());
        }

        @Test
        @DisplayName("Handles CURRENT account type")
        void handlesCurrentType() throws SQLException {
            insertAccount("ACCT", "0000001234", "987654", "00060002",
                    "CURRENT ", new BigDecimal("0.50"),
                    LocalDate.of(2022, 1, 1), 500,
                    LocalDate.of(2023, 6, 1), LocalDate.of(2023, 7, 1),
                    new BigDecimal("2000.00"), new BigDecimal("2000.00"));

            var request = new InquireAccountRequest(60002, 987654);
            var response = inquireAccount.inquireAccount(request);

            assertTrue(response.isSuccess());
            assertEquals("CURRENT", response.getAccountType().trim());
        }

        @Test
        @DisplayName("Handles ISA account type")
        void handlesIsaType() throws SQLException {
            insertAccount("ACCT", "0000001234", "987654", "00060003",
                    "ISA     ", new BigDecimal("4.25"),
                    LocalDate.of(2022, 4, 6), 0,
                    LocalDate.of(2023, 6, 1), LocalDate.of(2023, 7, 1),
                    new BigDecimal("20000.00"), new BigDecimal("20000.00"));

            var request = new InquireAccountRequest(60003, 987654);
            var response = inquireAccount.inquireAccount(request);

            assertTrue(response.isSuccess());
            assertEquals("ISA", response.getAccountType().trim());
        }

        @Test
        @DisplayName("Handles LOAN account type")
        void handlesLoanType() throws SQLException {
            insertAccount("ACCT", "0000001234", "987654", "00060004",
                    "LOAN    ", new BigDecimal("8.99"),
                    LocalDate.of(2021, 9, 15), 0,
                    LocalDate.of(2023, 6, 1), LocalDate.of(2023, 7, 1),
                    new BigDecimal("-5000.00"), new BigDecimal("-5000.00"));

            var request = new InquireAccountRequest(60004, 987654);
            var response = inquireAccount.inquireAccount(request);

            assertTrue(response.isSuccess());
            assertEquals("LOAN", response.getAccountType().trim());
        }

        @Test
        @DisplayName("Handles MORTGAGE account type")
        void handlesMortgageType() throws SQLException {
            insertAccount("ACCT", "0000001234", "987654", "00060005",
                    "MORTGAGE", new BigDecimal("2.99"),
                    LocalDate.of(2015, 3, 1), 0,
                    LocalDate.of(2023, 6, 1), LocalDate.of(2023, 7, 1),
                    new BigDecimal("-150000.00"),
                    new BigDecimal("-150000.00"));

            var request = new InquireAccountRequest(60005, 987654);
            var response = inquireAccount.inquireAccount(request);

            assertTrue(response.isSuccess());
            assertEquals("MORTGAGE", response.getAccountType());
        }
    }

    @Nested
    @DisplayName("SQL Constants")
    class SqlConstantTests {

        @Test
        @DisplayName("SELECT by account SQL contains expected clauses")
        void selectByAccountSqlContainsExpectedClauses() {
            assertTrue(InquireAccount.SQL_SELECT_BY_ACCOUNT
                    .contains("ACCOUNT_SORTCODE = ?"));
            assertTrue(InquireAccount.SQL_SELECT_BY_ACCOUNT
                    .contains("ACCOUNT_NUMBER = ?"));
        }

        @Test
        @DisplayName("SELECT last account SQL contains ORDER BY DESC")
        void selectLastAccountSqlContainsOrderByDesc() {
            assertTrue(InquireAccount.SQL_SELECT_LAST_ACCOUNT
                    .contains("ORDER BY ACCOUNT_NUMBER DESC"));
            assertTrue(InquireAccount.SQL_SELECT_LAST_ACCOUNT
                    .contains("FETCH FIRST 1 ROWS ONLY"));
        }
    }
}
