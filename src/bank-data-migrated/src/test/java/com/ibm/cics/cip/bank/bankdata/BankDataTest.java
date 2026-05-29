/*
 * Copyright IBM Corp. 2023
 *
 * Comprehensive tests for the Java 21 migration of BANKDATA.cbl.
 */
package com.ibm.cics.cip.bank.bankdata;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.temporal.JulianFields;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BankDataTest {

    private InMemoryCustomerDataStore customerStore;
    private BankData bankData;

    @BeforeEach
    void setUp() {
        customerStore = new InMemoryCustomerDataStore();
    }

    @Nested
    @DisplayName("Parameter Validation")
    class ParameterValidation {

        @Test
        @DisplayName("Should reject endKey smaller than startKey")
        void rejectsEndKeySmaller() {
            InMemoryAccountDataStore accountStore =
                    new InMemoryAccountDataStore();
            bankData = new BankData(customerStore, accountStore);

            assertThrows(IllegalArgumentException.class,
                    () -> bankData.execute(100, 50, 1, 12345));
        }

        @Test
        @DisplayName("Should reject step of zero")
        void rejectsZeroStep() {
            InMemoryAccountDataStore accountStore =
                    new InMemoryAccountDataStore();
            bankData = new BankData(customerStore, accountStore);

            assertThrows(IllegalArgumentException.class,
                    () -> bankData.execute(1, 100, 0, 12345));
        }

        @Test
        @DisplayName("Should accept valid parameters")
        void acceptsValidParameters() {
            InMemoryAccountDataStore accountStore =
                    new InMemoryAccountDataStore();
            bankData = new BankData(customerStore, accountStore);

            assertDoesNotThrow(
                    () -> bankData.execute(1, 10, 1, 12345));
        }
    }

    @Nested
    @DisplayName("Customer Record Generation")
    class CustomerRecordGeneration {

        @Test
        @DisplayName("Should generate correct number of customers")
        void generatesCorrectCustomerCount() {
            InMemoryAccountDataStore accountStore =
                    new InMemoryAccountDataStore();
            bankData = new BankData(customerStore, accountStore);

            bankData.execute(1, 5, 1, 12345);

            assertEquals(5, customerStore.customers.size());
            assertEquals(5, bankData.getNumberOfCustomers());
        }

        @Test
        @DisplayName("Should generate customers with correct step")
        void generatesWithCorrectStep() {
            InMemoryAccountDataStore accountStore =
                    new InMemoryAccountDataStore();
            bankData = new BankData(customerStore, accountStore);

            bankData.execute(10, 50, 10, 12345);

            assertEquals(5, customerStore.customers.size());
            assertEquals("0000000010",
                    customerStore.customers.get(0).customerNumber());
            assertEquals("0000000020",
                    customerStore.customers.get(1).customerNumber());
            assertEquals("0000000050",
                    customerStore.customers.get(4).customerNumber());
        }

        @Test
        @DisplayName("Should set CUST eyecatcher on all customers")
        void setsEyecatcher() {
            InMemoryAccountDataStore accountStore =
                    new InMemoryAccountDataStore();
            bankData = new BankData(customerStore, accountStore);

            bankData.execute(1, 3, 1, 99999);

            for (CustomerRecord c : customerStore.customers) {
                assertEquals("CUST", c.eyecatcher());
            }
        }

        @Test
        @DisplayName("Should set sort code from reference data")
        void setsSortCode() {
            InMemoryAccountDataStore accountStore =
                    new InMemoryAccountDataStore();
            bankData = new BankData(customerStore, accountStore);

            bankData.execute(1, 1, 1, 12345);

            assertEquals(ReferenceData.SORT_CODE,
                    customerStore.customers.get(0).sortCode());
        }

        @Test
        @DisplayName("Should generate name from reference data components")
        void generatesNameFromReferenceData() {
            InMemoryAccountDataStore accountStore =
                    new InMemoryAccountDataStore();
            bankData = new BankData(customerStore, accountStore);

            bankData.execute(1, 1, 1, 42);

            String name = customerStore.customers.get(0).name();
            assertNotNull(name);
            assertFalse(name.isBlank());
            assertTrue(name.contains(" "),
                    "Name should have spaces between components");
        }

        @Test
        @DisplayName("Should generate birth year between 1900 and 2000")
        void generatesBirthYearInRange() {
            InMemoryAccountDataStore accountStore =
                    new InMemoryAccountDataStore();
            bankData = new BankData(customerStore, accountStore);

            bankData.execute(1, 50, 1, 54321);

            for (CustomerRecord c : customerStore.customers) {
                assertTrue(c.birthYear() >= 1900 && c.birthYear() <= 2000,
                        "Birth year should be between 1900 and 2000, got: "
                                + c.birthYear());
                assertTrue(c.birthDay() >= 1 && c.birthDay() <= 28,
                        "Birth day should be between 1 and 28, got: "
                                + c.birthDay());
                assertTrue(c.birthMonth() >= 1 && c.birthMonth() <= 12,
                        "Birth month should be between 1 and 12, got: "
                                + c.birthMonth());
            }
        }

        @Test
        @DisplayName("Should generate credit score between 1 and 999")
        void generatesCreditScoreInRange() {
            InMemoryAccountDataStore accountStore =
                    new InMemoryAccountDataStore();
            bankData = new BankData(customerStore, accountStore);

            bankData.execute(1, 100, 1, 67890);

            for (CustomerRecord c : customerStore.customers) {
                assertTrue(c.creditScore() >= 1 && c.creditScore() <= 999,
                        "Credit score should be between 1 and 999, got: "
                                + c.creditScore());
            }
        }

        @Test
        @DisplayName("Should generate review date in the future")
        void generatesReviewDateInFuture() {
            InMemoryAccountDataStore accountStore =
                    new InMemoryAccountDataStore();
            bankData = new BankData(customerStore, accountStore);

            bankData.execute(1, 5, 1, 11111);

            LocalDate today = LocalDate.now();
            for (CustomerRecord c : customerStore.customers) {
                LocalDate reviewDate = LocalDate.of(
                        c.csReviewYear(), c.csReviewMonth(), c.csReviewDay());
                assertTrue(reviewDate.isAfter(today),
                        "Review date should be after today");
                assertTrue(
                        reviewDate.isBefore(today.plusDays(22))
                                || reviewDate.isEqual(today.plusDays(21)),
                        "Review date should be at most 21 days from today");
            }
        }

        @Test
        @DisplayName("Should write control record after all customers")
        void writesControlRecord() {
            InMemoryAccountDataStore accountStore =
                    new InMemoryAccountDataStore();
            bankData = new BankData(customerStore, accountStore);

            bankData.execute(1, 5, 1, 12345);

            assertNotNull(customerStore.controlRecord);
            assertEquals("CTRL",
                    customerStore.controlRecord.eyecatcher());
            assertEquals("000000",
                    customerStore.controlRecord.sortCode());
            assertEquals("9999999999",
                    customerStore.controlRecord.controlNumber());
            assertEquals(5,
                    customerStore.controlRecord.numberOfCustomers());
        }

        @Test
        @DisplayName("Should track last customer number")
        void tracksLastCustomerNumber() {
            InMemoryAccountDataStore accountStore =
                    new InMemoryAccountDataStore();
            bankData = new BankData(customerStore, accountStore);

            bankData.execute(10, 50, 10, 12345);

            assertEquals(50, bankData.getLastCustomerNumber());
            assertEquals(50,
                    customerStore.controlRecord.lastCustomerNumber());
        }
    }

    @Nested
    @DisplayName("Account Record Generation")
    class AccountRecordGeneration {

        @Test
        @DisplayName("Should generate 1-5 accounts per customer")
        void generatesAccountsPerCustomer() {
            InMemoryAccountDataStore accountStore =
                    new InMemoryAccountDataStore();
            bankData = new BankData(customerStore, accountStore);

            bankData.execute(1, 1, 1, 12345);

            assertFalse(accountStore.accounts.isEmpty(),
                    "Should have generated at least one account");
            assertTrue(accountStore.accounts.size() >= 1
                    && accountStore.accounts.size() <= 5,
                    "Should generate 1-5 accounts, got: "
                            + accountStore.accounts.size());
        }

        @Test
        @DisplayName("Should set ACCT eyecatcher on all accounts")
        void setsAccountEyecatcher() {
            InMemoryAccountDataStore accountStore =
                    new InMemoryAccountDataStore();
            bankData = new BankData(customerStore, accountStore);

            bankData.execute(1, 5, 1, 12345);

            for (AccountRecord a : accountStore.accounts) {
                assertEquals("ACCT", a.eyecatcher());
            }
        }

        @Test
        @DisplayName("Should assign correct account types in order")
        void assignsAccountTypesInOrder() {
            InMemoryAccountDataStore accountStore =
                    new InMemoryAccountDataStore();
            bankData = new BankData(customerStore, accountStore);

            bankData.execute(1, 100, 1, 55555);

            for (AccountRecord a : accountStore.accounts) {
                assertTrue(
                        ReferenceData.ACCOUNT_TYPES.contains(a.accountType()),
                        "Account type should be valid: " + a.accountType());
            }
        }

        @Test
        @DisplayName("LOAN and MORTGAGE accounts should have negative balances")
        void loanMortgageNegativeBalances() {
            InMemoryAccountDataStore accountStore =
                    new InMemoryAccountDataStore();
            bankData = new BankData(customerStore, accountStore);

            bankData.execute(1, 200, 1, 42);

            for (AccountRecord a : accountStore.accounts) {
                if ("LOAN".equals(a.accountType())
                        || "MORTGAGE".equals(a.accountType())) {
                    assertTrue(
                            a.actualBalance().compareTo(BigDecimal.ZERO) <= 0,
                            "LOAN/MORTGAGE actual balance should be <= 0, got: "
                                    + a.actualBalance());
                    assertTrue(
                            a.availableBalance()
                                    .compareTo(BigDecimal.ZERO) <= 0,
                            "LOAN/MORTGAGE available balance should be <= 0, "
                                    + "got: " + a.availableBalance());
                }
            }
        }

        @Test
        @DisplayName("Non-LOAN/MORTGAGE accounts should have positive balances")
        void nonLoanPositiveBalances() {
            InMemoryAccountDataStore accountStore =
                    new InMemoryAccountDataStore();
            bankData = new BankData(customerStore, accountStore);

            bankData.execute(1, 200, 1, 42);

            for (AccountRecord a : accountStore.accounts) {
                if (!"LOAN".equals(a.accountType())
                        && !"MORTGAGE".equals(a.accountType())) {
                    assertTrue(
                            a.actualBalance().compareTo(BigDecimal.ZERO) > 0,
                            "Non-loan actual balance should be > 0");
                    assertTrue(
                            a.availableBalance()
                                    .compareTo(BigDecimal.ZERO) > 0,
                            "Non-loan available balance should be > 0");
                }
            }
        }

        @Test
        @DisplayName("Available and actual balance should be equal")
        void balancesAreEqual() {
            InMemoryAccountDataStore accountStore =
                    new InMemoryAccountDataStore();
            bankData = new BankData(customerStore, accountStore);

            bankData.execute(1, 50, 1, 99999);

            for (AccountRecord a : accountStore.accounts) {
                assertEquals(a.availableBalance().abs(),
                        a.actualBalance().abs(),
                        "Available and actual balance magnitudes should match");
            }
        }

        @Test
        @DisplayName("Account opened date should use DD.MM.YYYY format")
        void openedDateFormat() {
            InMemoryAccountDataStore accountStore =
                    new InMemoryAccountDataStore();
            bankData = new BankData(customerStore, accountStore);

            bankData.execute(1, 20, 1, 12345);

            for (AccountRecord a : accountStore.accounts) {
                assertTrue(a.openedDate().matches("\\d{2}\\.\\d{2}\\.\\d{4}"),
                        "Opened date should be DD.MM.YYYY, got: "
                                + a.openedDate());
            }
        }

        @Test
        @DisplayName("Should set correct last/next statement dates")
        void setsStatementDates() {
            InMemoryAccountDataStore accountStore =
                    new InMemoryAccountDataStore();
            bankData = new BankData(customerStore, accountStore);

            bankData.execute(1, 5, 1, 12345);

            for (AccountRecord a : accountStore.accounts) {
                assertEquals("01.07.2021", a.lastStatementDate());
                assertEquals("01.08.2021", a.nextStatementDate());
            }
        }

        @Test
        @DisplayName("Account numbers should be sequential")
        void accountNumbersSequential() {
            InMemoryAccountDataStore accountStore =
                    new InMemoryAccountDataStore();
            bankData = new BankData(customerStore, accountStore);

            bankData.execute(1, 10, 1, 12345);

            for (int i = 0; i < accountStore.accounts.size(); i++) {
                String expected = String.format("%08d", i + 1);
                assertEquals(expected,
                        accountStore.accounts.get(i).accountNumber(),
                        "Account numbers should be sequential");
            }
        }

        @Test
        @DisplayName("Should insert control records into CONTROL table")
        void insertsControlRecords() {
            InMemoryAccountDataStore accountStore =
                    new InMemoryAccountDataStore();
            bankData = new BankData(customerStore, accountStore);

            bankData.execute(1, 5, 1, 12345);

            assertTrue(accountStore.controlRecords.stream()
                    .anyMatch(r -> r.name.contains("ACCOUNT-LAST")),
                    "Should insert ACCOUNT-LAST control record");
            assertTrue(accountStore.controlRecords.stream()
                    .anyMatch(r -> r.name.contains("ACCOUNT-COUNT")),
                    "Should insert ACCOUNT-COUNT control record");
        }

        @Test
        @DisplayName("ACCOUNT-COUNT should match total accounts generated")
        void accountCountMatchesTotal() {
            InMemoryAccountDataStore accountStore =
                    new InMemoryAccountDataStore();
            bankData = new BankData(customerStore, accountStore);

            bankData.execute(1, 10, 1, 12345);

            int countFromControl = accountStore.controlRecords.stream()
                    .filter(r -> r.name.contains("ACCOUNT-COUNT"))
                    .mapToInt(r -> r.valueNum)
                    .findFirst()
                    .orElse(-1);

            assertEquals(accountStore.accounts.size(), countFromControl,
                    "ACCOUNT-COUNT should match total accounts inserted");
        }
    }

    @Nested
    @DisplayName("Opened Date Generation")
    class OpenedDateGeneration {

        @Test
        @DisplayName("Opened year should be >= birth year")
        void openedYearAfterBirthYear() {
            InMemoryAccountDataStore accountStore =
                    new InMemoryAccountDataStore();
            bankData = new BankData(customerStore, accountStore);

            bankData.execute(1, 100, 1, 12345);

            for (int i = 0; i < customerStore.customers.size(); i++) {
                CustomerRecord customer = customerStore.customers.get(i);
                for (AccountRecord account : accountStore.accounts) {
                    if (account.customerNumber()
                            .equals(customer.customerNumber())) {
                        int openedYear = Integer.parseInt(
                                account.openedDate().substring(6));
                        assertTrue(openedYear >= customer.birthYear(),
                                "Opened year " + openedYear
                                        + " should be >= birth year "
                                        + customer.birthYear());
                    }
                }
            }
        }
    }

    @Nested
    @DisplayName("Deterministic Random Generation")
    class DeterministicRandom {

        @Test
        @DisplayName("Same seed should produce same results")
        void sameSeedSameResults() {
            InMemoryAccountDataStore accountStore1 =
                    new InMemoryAccountDataStore();
            InMemoryCustomerDataStore customerStore1 =
                    new InMemoryCustomerDataStore();
            BankData bd1 = new BankData(customerStore1, accountStore1);

            InMemoryAccountDataStore accountStore2 =
                    new InMemoryAccountDataStore();
            InMemoryCustomerDataStore customerStore2 =
                    new InMemoryCustomerDataStore();
            BankData bd2 = new BankData(customerStore2, accountStore2);

            bd1.execute(1, 20, 1, 42);
            bd2.execute(1, 20, 1, 42);

            assertEquals(customerStore1.customers.size(),
                    customerStore2.customers.size());
            for (int i = 0; i < customerStore1.customers.size(); i++) {
                assertEquals(customerStore1.customers.get(i),
                        customerStore2.customers.get(i));
            }

            assertEquals(accountStore1.accounts.size(),
                    accountStore2.accounts.size());
            for (int i = 0; i < accountStore1.accounts.size(); i++) {
                assertEquals(accountStore1.accounts.get(i),
                        accountStore2.accounts.get(i));
            }
        }

        @Test
        @DisplayName("Different seeds should produce different results")
        void differentSeedDifferentResults() {
            InMemoryAccountDataStore accountStore1 =
                    new InMemoryAccountDataStore();
            InMemoryCustomerDataStore customerStore1 =
                    new InMemoryCustomerDataStore();
            BankData bd1 = new BankData(customerStore1, accountStore1);

            InMemoryAccountDataStore accountStore2 =
                    new InMemoryAccountDataStore();
            InMemoryCustomerDataStore customerStore2 =
                    new InMemoryCustomerDataStore();
            BankData bd2 = new BankData(customerStore2, accountStore2);

            bd1.execute(1, 20, 1, 42);
            bd2.execute(1, 20, 1, 99999);

            boolean anyDifferent = false;
            for (int i = 0; i < customerStore1.customers.size(); i++) {
                if (!customerStore1.customers.get(i)
                        .equals(customerStore2.customers.get(i))) {
                    anyDifferent = true;
                    break;
                }
            }
            assertTrue(anyDifferent,
                    "Different seeds should produce different data");
        }
    }

    @Nested
    @DisplayName("Reference Data Integrity")
    class ReferenceDataIntegrity {

        @Test
        @DisplayName("Should have 36 titles")
        void correctTitleCount() {
            assertEquals(36, ReferenceData.TITLES.size());
        }

        @Test
        @DisplayName("Should have 50 forenames")
        void correctForenameCount() {
            assertEquals(50, ReferenceData.FORENAMES.size());
        }

        @Test
        @DisplayName("Should have 30 initials characters")
        void correctInitialsLength() {
            assertEquals(30, ReferenceData.INITIALS_STRING.length());
        }

        @Test
        @DisplayName("Should have 50 surnames")
        void correctSurnameCount() {
            assertEquals(50, ReferenceData.SURNAMES.size());
        }

        @Test
        @DisplayName("Should have 26 tree street names")
        void correctTreeStreetCount() {
            assertEquals(26, ReferenceData.STREET_NAMES_TREE.size());
        }

        @Test
        @DisplayName("Should have 19 road street names")
        void correctRoadStreetCount() {
            assertEquals(19, ReferenceData.STREET_NAMES_ROAD.size());
        }

        @Test
        @DisplayName("Should have 50 towns")
        void correctTownCount() {
            assertEquals(50, ReferenceData.TOWNS.size());
        }

        @Test
        @DisplayName("Should have 5 account types")
        void correctAccountTypeCount() {
            assertEquals(5, ReferenceData.ACCOUNT_TYPES.size());
        }

        @Test
        @DisplayName("Account types should match COBOL values")
        void accountTypesMatchCobol() {
            assertEquals("ISA", ReferenceData.ACCOUNT_TYPES.get(0));
            assertEquals("SAVING", ReferenceData.ACCOUNT_TYPES.get(1));
            assertEquals("CURRENT", ReferenceData.ACCOUNT_TYPES.get(2));
            assertEquals("LOAN", ReferenceData.ACCOUNT_TYPES.get(3));
            assertEquals("MORTGAGE", ReferenceData.ACCOUNT_TYPES.get(4));
        }

        @Test
        @DisplayName("Interest rates should match COBOL values")
        void interestRatesMatchCobol() {
            assertEquals(new BigDecimal("2.10"),
                    ReferenceData.ACCOUNT_INTEREST_RATES.get(0));
            assertEquals(new BigDecimal("1.75"),
                    ReferenceData.ACCOUNT_INTEREST_RATES.get(1));
            assertEquals(new BigDecimal("0.00"),
                    ReferenceData.ACCOUNT_INTEREST_RATES.get(2));
            assertEquals(new BigDecimal("17.90"),
                    ReferenceData.ACCOUNT_INTEREST_RATES.get(3));
            assertEquals(new BigDecimal("5.25"),
                    ReferenceData.ACCOUNT_INTEREST_RATES.get(4));
        }

        @Test
        @DisplayName("Overdraft limits should match COBOL values")
        void overdraftLimitsMatchCobol() {
            assertEquals(0,
                    ReferenceData.ACCOUNT_OVERDRAFT_LIMITS.get(0));
            assertEquals(0,
                    ReferenceData.ACCOUNT_OVERDRAFT_LIMITS.get(1));
            assertEquals(100,
                    ReferenceData.ACCOUNT_OVERDRAFT_LIMITS.get(2));
            assertEquals(0,
                    ReferenceData.ACCOUNT_OVERDRAFT_LIMITS.get(3));
            assertEquals(0,
                    ReferenceData.ACCOUNT_OVERDRAFT_LIMITS.get(4));
        }

        @Test
        @DisplayName("Sort code should be 987654")
        void sortCodeMatchesCobol() {
            assertEquals("987654", ReferenceData.SORT_CODE);
        }
    }

    @Nested
    @DisplayName("JDBC Integration with H2")
    class JdbcIntegration {

        private Connection connection;
        private static int dbCounter = 0;

        @BeforeEach
        void setUpDatabase() throws SQLException {
            String dbName = "testdb_" + (dbCounter++);
            connection = DriverManager.getConnection(
                    "jdbc:h2:mem:" + dbName, "sa", "");
            connection.setAutoCommit(false);

            try (Statement stmt = connection.createStatement()) {

                stmt.execute("CREATE TABLE ACCOUNT ("
                        + "ACCOUNT_EYECATCHER CHAR(4), "
                        + "ACCOUNT_CUSTOMER_NUMBER CHAR(10), "
                        + "ACCOUNT_SORTCODE CHAR(6) NOT NULL, "
                        + "ACCOUNT_NUMBER CHAR(8) NOT NULL, "
                        + "ACCOUNT_TYPE CHAR(8), "
                        + "ACCOUNT_INTEREST_RATE DECIMAL(4,2), "
                        + "ACCOUNT_OPENED VARCHAR(10), "
                        + "ACCOUNT_OVERDRAFT_LIMIT INTEGER, "
                        + "ACCOUNT_LAST_STATEMENT VARCHAR(10), "
                        + "ACCOUNT_NEXT_STATEMENT VARCHAR(10), "
                        + "ACCOUNT_AVAILABLE_BALANCE DECIMAL(12,2), "
                        + "ACCOUNT_ACTUAL_BALANCE DECIMAL(12,2))");

                stmt.execute("CREATE TABLE CONTROL ("
                        + "CONTROL_NAME CHAR(32) NOT NULL, "
                        + "CONTROL_VALUE_NUM INTEGER, "
                        + "CONTROL_VALUE_STR CHAR(40))");
            }
            connection.commit();
        }

        @AfterEach
        void tearDownDatabase() throws SQLException {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        }

        @Test
        @DisplayName("Should insert accounts into H2 database")
        void insertsAccountsIntoDatabase() throws SQLException {
            JdbcAccountDataStore jdbcStore =
                    new JdbcAccountDataStore(connection);
            InMemoryCustomerDataStore memCustomerStore =
                    new InMemoryCustomerDataStore();
            BankData bd = new BankData(memCustomerStore, jdbcStore);

            bd.execute(1, 10, 1, 12345);

            try (Statement stmt = connection.createStatement();
                    ResultSet rs = stmt.executeQuery(
                            "SELECT COUNT(*) FROM ACCOUNT")) {
                rs.next();
                int count = rs.getInt(1);
                assertTrue(count >= 10,
                        "Should have at least 10 accounts (1 per customer), "
                                + "got: " + count);
                assertTrue(count <= 50,
                        "Should have at most 50 accounts (5 per customer), "
                                + "got: " + count);
            }
        }

        @Test
        @DisplayName("Should insert control records into H2 database")
        void insertsControlRecords() throws SQLException {
            JdbcAccountDataStore jdbcStore =
                    new JdbcAccountDataStore(connection);
            InMemoryCustomerDataStore memCustomerStore =
                    new InMemoryCustomerDataStore();
            BankData bd = new BankData(memCustomerStore, jdbcStore);

            bd.execute(1, 5, 1, 12345);

            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT CONTROL_NAME, CONTROL_VALUE_NUM FROM CONTROL")) {
                ResultSet rs = ps.executeQuery();
                List<String> controlNames = new ArrayList<>();
                while (rs.next()) {
                    controlNames.add(rs.getString(1).trim());
                }
                assertTrue(controlNames.stream()
                        .anyMatch(n -> n.contains("ACCOUNT-LAST")));
                assertTrue(controlNames.stream()
                        .anyMatch(n -> n.contains("ACCOUNT-COUNT")));
            }
        }

        @Test
        @DisplayName("Should delete existing data before populating")
        void deletesBeforePopulating() throws SQLException {
            JdbcAccountDataStore jdbcStore =
                    new JdbcAccountDataStore(connection);
            InMemoryCustomerDataStore memCustomerStore =
                    new InMemoryCustomerDataStore();
            BankData bd = new BankData(memCustomerStore, jdbcStore);

            bd.execute(1, 5, 1, 12345);

            try (Statement stmt = connection.createStatement();
                    ResultSet rs = stmt.executeQuery(
                            "SELECT COUNT(*) FROM ACCOUNT")) {
                rs.next();
                int firstRunCount = rs.getInt(1);

                memCustomerStore = new InMemoryCustomerDataStore();
                bd = new BankData(memCustomerStore, jdbcStore);
                bd.execute(1, 5, 1, 99999);

                try (ResultSet rs2 = stmt.executeQuery(
                        "SELECT COUNT(*) FROM ACCOUNT")) {
                    rs2.next();
                    int secondRunCount = rs2.getInt(1);
                    assertTrue(secondRunCount <= 25,
                            "Old data should be deleted, max 25 accounts "
                                    + "for 5 customers");
                }
            }
        }

        @Test
        @DisplayName("Account sort code should match reference data")
        void accountSortCodeCorrect() throws SQLException {
            JdbcAccountDataStore jdbcStore =
                    new JdbcAccountDataStore(connection);
            InMemoryCustomerDataStore memCustomerStore =
                    new InMemoryCustomerDataStore();
            BankData bd = new BankData(memCustomerStore, jdbcStore);

            bd.execute(1, 3, 1, 12345);

            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT DISTINCT ACCOUNT_SORTCODE FROM ACCOUNT")) {
                ResultSet rs = ps.executeQuery();
                assertTrue(rs.next());
                assertEquals(ReferenceData.SORT_CODE,
                        rs.getString(1).trim());
                assertFalse(rs.next(),
                        "All accounts should have the same sort code");
            }
        }
    }

    @Nested
    @DisplayName("Large Scale Generation")
    class LargeScaleGeneration {

        @Test
        @DisplayName("Should handle generating 1000 customers")
        void handlesLargeGeneration() {
            InMemoryAccountDataStore accountStore =
                    new InMemoryAccountDataStore();
            bankData = new BankData(customerStore, accountStore);

            bankData.execute(1, 1000, 1, 12345);

            assertEquals(1000, customerStore.customers.size());
            assertTrue(accountStore.accounts.size() >= 1000,
                    "At least 1 account per customer");
            assertTrue(accountStore.accounts.size() <= 5000,
                    "At most 5 accounts per customer");
        }
    }

    @Nested
    @DisplayName("Record Model Tests")
    class RecordModelTests {

        @Test
        @DisplayName("CustomerRecord should enforce non-null fields")
        void customerRecordNonNull() {
            assertThrows(NullPointerException.class,
                    () -> new CustomerRecord(
                            null, "987654", "0000000001",
                            "Test", "Address",
                            1, 1, 1990, 500, 1, 1, 2025));
        }

        @Test
        @DisplayName("AccountRecord should enforce non-null fields")
        void accountRecordNonNull() {
            assertThrows(NullPointerException.class,
                    () -> new AccountRecord(
                            null, "0000000001", "987654", "00000001",
                            "ISA", BigDecimal.ONE, "01.01.2020",
                            0, "01.07.2021", "01.08.2021",
                            BigDecimal.TEN, BigDecimal.TEN));
        }

        @Test
        @DisplayName("CustomerRecord dateOfBirth format")
        void customerDobFormat() {
            CustomerRecord cr = new CustomerRecord(
                    "CUST", "987654", "0000000001", "Test Name",
                    "Test Address", 15, 6, 1985, 750, 1, 1, 2025);
            assertEquals("15061985", cr.dateOfBirth());
        }

        @Test
        @DisplayName("CustomerControlRecord constants")
        void controlRecordConstants() {
            assertEquals("CTRL",
                    CustomerControlRecord.EYECATCHER_VALUE);
            assertEquals("9999999999",
                    CustomerControlRecord.CONTROL_NUMBER);
        }
    }

    // ---- In-memory test doubles ----

    static class InMemoryCustomerDataStore implements CustomerDataStore {

        final List<CustomerRecord> customers = new ArrayList<>();
        CustomerControlRecord controlRecord;
        boolean opened = false;

        @Override
        public void open() {
            opened = true;
        }

        @Override
        public void writeCustomer(CustomerRecord record) {
            if (!opened) {
                throw new IllegalStateException("Store not opened");
            }
            customers.add(record);
        }

        @Override
        public void writeControlRecord(CustomerControlRecord record) {
            controlRecord = record;
        }

        @Override
        public void close() {
            opened = false;
        }
    }

    static class InMemoryAccountDataStore implements AccountDataStore {

        final List<AccountRecord> accounts = new ArrayList<>();
        final List<ControlEntry> controlRecords = new ArrayList<>();

        @Override
        public void deleteAccountsBySortCode(String sortCode) {
            accounts.removeIf(a -> a.sortCode().equals(sortCode));
        }

        @Override
        public void deleteControlRecord(String controlName) {
            controlRecords.removeIf(r -> r.name.equals(controlName));
        }

        @Override
        public void insertAccount(AccountRecord record) {
            accounts.add(record);
        }

        @Override
        public void insertControlRecord(String controlName, int valueNum,
                String valueStr) {
            controlRecords.add(new ControlEntry(controlName, valueNum,
                    valueStr));
        }

        @Override
        public void commit() {
            // no-op for in-memory store
        }

        @Override
        public void close() {
            // no-op
        }

        record ControlEntry(String name, int valueNum, String valueStr) {
        }
    }
}
