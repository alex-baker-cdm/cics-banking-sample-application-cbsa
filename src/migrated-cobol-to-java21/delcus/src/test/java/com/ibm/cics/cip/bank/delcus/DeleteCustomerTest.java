/*
 *    Copyright IBM Corp. 2023
 *
 *    Tests for the migrated DELCUS (Delete Customer) program.
 */
package com.ibm.cics.cip.bank.delcus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DeleteCustomerTest {

    private StubCustomerDataAccess customerDataAccess;
    private StubAccountDataAccess accountDataAccess;
    private StubProctranDataAccess proctranDataAccess;
    private DeleteCustomer deleteCustomer;
    private DeleteCustomerCommarea commarea;

    private static final String TEST_APPLID = "TESTAPPL";
    private static final String TEST_CUSTOMER_NUMBER = "0000000001";
    private static final String TEST_SORT_CODE = "987654";

    @BeforeEach
    void setUp() {
        customerDataAccess = new StubCustomerDataAccess();
        accountDataAccess = new StubAccountDataAccess();
        proctranDataAccess = new StubProctranDataAccess();
        deleteCustomer = new DeleteCustomer(
                customerDataAccess,
                accountDataAccess,
                proctranDataAccess,
                TEST_APPLID
        );
        commarea = new DeleteCustomerCommarea();
        commarea.setCustomerNumber(TEST_CUSTOMER_NUMBER);
    }

    @Nested
    @DisplayName("Customer Inquiry Phase")
    class CustomerInquiryTests {

        @Test
        @DisplayName("should return failure when customer inquiry fails")
        void customerNotFound() throws DeleteCustomerException {
            customerDataAccess.inquiryResult =
                    new CustomerInquiryResult(false, "N");

            deleteCustomer.execute(commarea);

            assertFalse(commarea.isDeleteSuccess());
            assertEquals("N", commarea.getDeleteFailCode());
            assertEquals(0, accountDataAccess.getAccountsCallCount);
            assertEquals(0, customerDataAccess.deleteCallCount);
        }

        @Test
        @DisplayName("should propagate inquiry fail code to commarea")
        void propagatesFailCode() throws DeleteCustomerException {
            customerDataAccess.inquiryResult =
                    new CustomerInquiryResult(false, "X");

            deleteCustomer.execute(commarea);

            assertFalse(commarea.isDeleteSuccess());
            assertEquals("X", commarea.getDeleteFailCode());
        }
    }

    @Nested
    @DisplayName("Account Deletion Phase")
    class AccountDeletionTests {

        @BeforeEach
        void setUpSuccessfulInquiry() {
            customerDataAccess.inquiryResult =
                    new CustomerInquiryResult(true, " ");
            customerDataAccess.customerToReturn = Optional.of(buildTestCustomer());
        }

        @Test
        @DisplayName("should skip account deletion when no accounts exist")
        void noAccounts() throws DeleteCustomerException {
            accountDataAccess.accountInquiryResult =
                    new AccountInquiryResult(0, Collections.emptyList());

            deleteCustomer.execute(commarea);

            assertTrue(commarea.isDeleteSuccess());
            assertEquals(0, accountDataAccess.deleteAccountCallCount);
        }

        @Test
        @DisplayName("should delete all accounts for the customer")
        void deletesAllAccounts() throws DeleteCustomerException {
            List<AccountRecord> accounts = List.of(
                    buildAccountRecord(11111111),
                    buildAccountRecord(22222222),
                    buildAccountRecord(33333333)
            );
            accountDataAccess.accountInquiryResult =
                    new AccountInquiryResult(3, accounts);

            deleteCustomer.execute(commarea);

            assertTrue(commarea.isDeleteSuccess());
            assertEquals(3, accountDataAccess.deleteAccountCallCount);
            assertEquals(List.of(11111111, 22222222, 33333333),
                    accountDataAccess.deletedAccountNumbers);
        }

        @Test
        @DisplayName("should pass applId to DELACC when deleting accounts")
        void passesApplId() throws DeleteCustomerException {
            List<AccountRecord> accounts = List.of(
                    buildAccountRecord(44444444)
            );
            accountDataAccess.accountInquiryResult =
                    new AccountInquiryResult(1, accounts);

            deleteCustomer.execute(commarea);

            assertEquals(TEST_APPLID, accountDataAccess.lastApplId);
        }

        @Test
        @DisplayName("should handle maximum 20 accounts")
        void handlesMaxAccounts() throws DeleteCustomerException {
            List<AccountRecord> accounts = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                accounts.add(buildAccountRecord(10000000 + i));
            }
            accountDataAccess.accountInquiryResult =
                    new AccountInquiryResult(20, accounts);

            deleteCustomer.execute(commarea);

            assertTrue(commarea.isDeleteSuccess());
            assertEquals(20, accountDataAccess.deleteAccountCallCount);
        }
    }

    @Nested
    @DisplayName("Customer VSAM Deletion Phase")
    class CustomerVsamDeletionTests {

        @BeforeEach
        void setUpSuccessfulInquiry() {
            customerDataAccess.inquiryResult =
                    new CustomerInquiryResult(true, " ");
            accountDataAccess.accountInquiryResult =
                    new AccountInquiryResult(0, Collections.emptyList());
        }

        @Test
        @DisplayName("should succeed when customer record is deleted successfully")
        void successfulDeletion() throws DeleteCustomerException {
            CustomerRecord customer = buildTestCustomer();
            customerDataAccess.customerToReturn = Optional.of(customer);

            deleteCustomer.execute(commarea);

            assertTrue(commarea.isDeleteSuccess());
            assertEquals(" ", commarea.getDeleteFailCode());
            assertEquals(1, customerDataAccess.deleteCallCount);
        }

        @Test
        @DisplayName("should succeed even when customer already deleted by another process")
        void alreadyDeletedByOtherProcess() throws DeleteCustomerException {
            customerDataAccess.customerToReturn = Optional.empty();

            deleteCustomer.execute(commarea);

            assertTrue(commarea.isDeleteSuccess());
            assertEquals(" ", commarea.getDeleteFailCode());
        }

        @Test
        @DisplayName("should propagate DeleteCustomerException on VSAM failure")
        void vsamReadFailure() {
            customerDataAccess.throwOnDelete =
                    new DeleteCustomerException("WPV6",
                            "Unable to READ CUSTOMER VSAM rec");

            assertThrows(DeleteCustomerException.class,
                    () -> deleteCustomer.execute(commarea));
        }

        @Test
        @DisplayName("should use correct sort code and customer number for VSAM key")
        void correctVsamKey() throws DeleteCustomerException {
            customerDataAccess.customerToReturn = Optional.of(buildTestCustomer());

            deleteCustomer.execute(commarea);

            assertEquals(TEST_SORT_CODE, customerDataAccess.lastSortCode);
            assertEquals(TEST_CUSTOMER_NUMBER, customerDataAccess.lastCustomerNumber);
        }
    }

    @Nested
    @DisplayName("Commarea Population")
    class CommareaPopulationTests {

        @BeforeEach
        void setUpFullSuccess() {
            customerDataAccess.inquiryResult =
                    new CustomerInquiryResult(true, " ");
            accountDataAccess.accountInquiryResult =
                    new AccountInquiryResult(0, Collections.emptyList());
        }

        @Test
        @DisplayName("should populate commarea with customer details on successful deletion")
        void populatesCommareaFields() throws DeleteCustomerException {
            CustomerRecord customer = new CustomerRecord(
                    "CUST", 987654, 1L,
                    "Mr John Smith",
                    "123 Main Street, London",
                    LocalDate.of(1985, 3, 15),
                    750,
                    LocalDate.of(2024, 6, 1)
            );
            customerDataAccess.customerToReturn = Optional.of(customer);

            deleteCustomer.execute(commarea);

            assertEquals("CUST", commarea.getEyeCatcher());
            assertEquals("987654", commarea.getSortCode());
            assertEquals("0000000001", commarea.getCustomerNumber());
            assertEquals("Mr John Smith", commarea.getName());
            assertEquals("123 Main Street, London", commarea.getAddress());
            assertEquals("15", commarea.getBirthDay());
            assertEquals("03", commarea.getBirthMonth());
            assertEquals("1985", commarea.getBirthYear());
            assertEquals(750, commarea.getCreditScore());
            assertEquals("01", commarea.getCsReviewDay());
            assertEquals("06", commarea.getCsReviewMonth());
            assertEquals("2024", commarea.getCsReviewYear());
        }

        @Test
        @DisplayName("should not populate commarea when customer was already deleted")
        void noCommareaWhenAlreadyDeleted() throws DeleteCustomerException {
            customerDataAccess.customerToReturn = Optional.empty();

            deleteCustomer.execute(commarea);

            assertNull(commarea.getEyeCatcher());
            assertNull(commarea.getName());
        }
    }

    @Nested
    @DisplayName("PROCTRAN Writing")
    class ProctranTests {

        @BeforeEach
        void setUpFullSuccess() {
            customerDataAccess.inquiryResult =
                    new CustomerInquiryResult(true, " ");
            accountDataAccess.accountInquiryResult =
                    new AccountInquiryResult(0, Collections.emptyList());
        }

        @Test
        @DisplayName("should write PROCTRAN record on successful customer deletion")
        void writesProctran() throws DeleteCustomerException {
            customerDataAccess.customerToReturn = Optional.of(buildTestCustomer());

            deleteCustomer.execute(commarea);

            assertEquals(1, proctranDataAccess.writeCount);
        }

        @Test
        @DisplayName("should not write PROCTRAN when customer was already deleted")
        void noProctranWhenAlreadyDeleted() throws DeleteCustomerException {
            customerDataAccess.customerToReturn = Optional.empty();

            deleteCustomer.execute(commarea);

            assertEquals(0, proctranDataAccess.writeCount);
        }

        @Test
        @DisplayName("should use ODC transaction type for customer deletion")
        void correctTransactionType() throws DeleteCustomerException {
            customerDataAccess.customerToReturn = Optional.of(buildTestCustomer());

            deleteCustomer.execute(commarea);

            assertEquals("ODC", proctranDataAccess.lastType);
        }

        @Test
        @DisplayName("should use PRTR eyecatcher")
        void correctEyecatcher() throws DeleteCustomerException {
            customerDataAccess.customerToReturn = Optional.of(buildTestCustomer());

            deleteCustomer.execute(commarea);

            assertEquals("PRTR", proctranDataAccess.lastEyeCatcher);
        }

        @Test
        @DisplayName("should set amount to zero for customer deletion")
        void zeroAmount() throws DeleteCustomerException {
            customerDataAccess.customerToReturn = Optional.of(buildTestCustomer());

            deleteCustomer.execute(commarea);

            assertEquals(BigDecimal.ZERO, proctranDataAccess.lastAmount);
        }

        @Test
        @DisplayName("should set account number to zeros for customer deletion")
        void zeroAccountNumber() throws DeleteCustomerException {
            customerDataAccess.customerToReturn = Optional.of(buildTestCustomer());

            deleteCustomer.execute(commarea);

            assertEquals("00000000", proctranDataAccess.lastAccNumber);
        }

        @Test
        @DisplayName("should build description with sort code, customer number, name, and DOB")
        void correctDescription() throws DeleteCustomerException {
            CustomerRecord customer = new CustomerRecord(
                    "CUST", 987654, 42L,
                    "Ms Jane Doe-Smith",
                    "456 Oak Avenue",
                    LocalDate.of(1990, 12, 25),
                    800,
                    LocalDate.of(2025, 1, 15)
            );
            customerDataAccess.customerToReturn = Optional.of(customer);

            deleteCustomer.execute(commarea);

            String desc = proctranDataAccess.lastDescription;
            assertNotNull(desc);
            assertEquals(40, desc.length());
            assertTrue(desc.startsWith("987654"));
            assertTrue(desc.contains("0000000042"));
            assertTrue(desc.contains("Ms Jane Doe-Sm"));
            assertTrue(desc.contains("25/12/1990"));
        }

        @Test
        @DisplayName("should propagate DeleteCustomerException on PROCTRAN write failure")
        void proctranWriteFailure() {
            customerDataAccess.customerToReturn = Optional.of(buildTestCustomer());
            proctranDataAccess.throwOnWrite =
                    new DeleteCustomerException("HWPT",
                            "Unable to WRITE to PROCTRAN DB2 datastore");

            DeleteCustomerException ex = assertThrows(
                    DeleteCustomerException.class,
                    () -> deleteCustomer.execute(commarea));

            assertEquals("HWPT", ex.getAbendCode());
        }
    }

    @Nested
    @DisplayName("End-to-End Flow")
    class EndToEndTests {

        @Test
        @DisplayName("should execute full delete flow with accounts")
        void fullFlowWithAccounts() throws DeleteCustomerException {
            customerDataAccess.inquiryResult =
                    new CustomerInquiryResult(true, " ");

            List<AccountRecord> accounts = List.of(
                    buildAccountRecord(11111111),
                    buildAccountRecord(22222222)
            );
            accountDataAccess.accountInquiryResult =
                    new AccountInquiryResult(2, accounts);

            CustomerRecord customer = new CustomerRecord(
                    "CUST", 987654, 1L,
                    "Dr Bob Builder",
                    "789 Pine Road, Manchester",
                    LocalDate.of(1975, 7, 4),
                    650,
                    LocalDate.of(2024, 12, 31)
            );
            customerDataAccess.customerToReturn = Optional.of(customer);

            deleteCustomer.execute(commarea);

            assertTrue(commarea.isDeleteSuccess());
            assertEquals(1, customerDataAccess.inquiryCallCount);
            assertEquals(1, accountDataAccess.getAccountsCallCount);
            assertEquals(2, accountDataAccess.deleteAccountCallCount);
            assertEquals(1, customerDataAccess.deleteCallCount);
            assertEquals(1, proctranDataAccess.writeCount);
        }

        @Test
        @DisplayName("should execute full delete flow without accounts")
        void fullFlowWithoutAccounts() throws DeleteCustomerException {
            customerDataAccess.inquiryResult =
                    new CustomerInquiryResult(true, " ");
            accountDataAccess.accountInquiryResult =
                    new AccountInquiryResult(0, Collections.emptyList());

            CustomerRecord customer = buildTestCustomer();
            customerDataAccess.customerToReturn = Optional.of(customer);

            deleteCustomer.execute(commarea);

            assertTrue(commarea.isDeleteSuccess());
            assertEquals(0, accountDataAccess.deleteAccountCallCount);
            assertEquals(1, customerDataAccess.deleteCallCount);
            assertEquals(1, proctranDataAccess.writeCount);
        }

        @Test
        @DisplayName("should short-circuit on failed customer inquiry")
        void shortCircuitOnInquiryFailure() throws DeleteCustomerException {
            customerDataAccess.inquiryResult =
                    new CustomerInquiryResult(false, "F");

            deleteCustomer.execute(commarea);

            assertFalse(commarea.isDeleteSuccess());
            assertEquals(0, accountDataAccess.getAccountsCallCount);
            assertEquals(0, customerDataAccess.deleteCallCount);
            assertEquals(0, proctranDataAccess.writeCount);
        }
    }

    @Nested
    @DisplayName("Utility Methods")
    class UtilityTests {

        @Test
        @DisplayName("padLeft should pad correctly")
        void testPadLeft() {
            assertEquals("001", DeleteCustomer.padLeft("1", 3, '0'));
            assertEquals("123", DeleteCustomer.padLeft("123", 3, '0'));
            assertEquals("1234", DeleteCustomer.padLeft("1234", 3, '0'));
            assertEquals("000", DeleteCustomer.padLeft("", 3, '0'));
            assertEquals("000", DeleteCustomer.padLeft(null, 3, '0'));
        }

        @Test
        @DisplayName("padRight should pad correctly")
        void testPadRight() {
            assertEquals("1  ", DeleteCustomer.padRight("1", 3, ' '));
            assertEquals("123", DeleteCustomer.padRight("123", 3, ' '));
            assertEquals("123", DeleteCustomer.padRight("1234", 3, ' '));
            assertEquals("   ", DeleteCustomer.padRight("", 3, ' '));
            assertEquals("   ", DeleteCustomer.padRight(null, 3, ' '));
        }

        @Test
        @DisplayName("buildDescription should produce 40-char output")
        void testBuildDescription() {
            String desc = DeleteCustomer.buildDescription(
                    "987654", "0000000001", "John Smith    ", "15/03/1985");
            assertEquals(40, desc.length());
            assertEquals("987654", desc.substring(0, 6));
            assertEquals("0000000001", desc.substring(6, 16));
            assertEquals("John Smith    ", desc.substring(16, 30));
            assertEquals("15/03/1985", desc.substring(30, 40));
        }
    }

    @Nested
    @DisplayName("DeleteCustomerCommarea")
    class CommareaTests {

        @Test
        @DisplayName("should initialize with default values")
        void defaultValues() {
            DeleteCustomerCommarea c = new DeleteCustomerCommarea();
            assertFalse(c.isDeleteSuccess());
            assertEquals(" ", c.getDeleteFailCode());
        }

        @Test
        @DisplayName("should get and set all fields")
        void gettersAndSetters() {
            DeleteCustomerCommarea c = new DeleteCustomerCommarea();
            c.setEyeCatcher("CUST");
            c.setSortCode("987654");
            c.setCustomerNumber("0000000001");
            c.setName("Test Name");
            c.setAddress("Test Address");
            c.setBirthDay("15");
            c.setBirthMonth("03");
            c.setBirthYear("1985");
            c.setCreditScore(750);
            c.setCsReviewDay("01");
            c.setCsReviewMonth("06");
            c.setCsReviewYear("2024");
            c.setDeleteSuccess(true);
            c.setDeleteFailCode("A");

            assertEquals("CUST", c.getEyeCatcher());
            assertEquals("987654", c.getSortCode());
            assertEquals("0000000001", c.getCustomerNumber());
            assertEquals("Test Name", c.getName());
            assertEquals("Test Address", c.getAddress());
            assertEquals("15", c.getBirthDay());
            assertEquals("03", c.getBirthMonth());
            assertEquals("1985", c.getBirthYear());
            assertEquals(750, c.getCreditScore());
            assertEquals("01", c.getCsReviewDay());
            assertEquals("06", c.getCsReviewMonth());
            assertEquals("2024", c.getCsReviewYear());
            assertTrue(c.isDeleteSuccess());
            assertEquals("A", c.getDeleteFailCode());
        }
    }

    @Nested
    @DisplayName("DeleteCustomerException")
    class ExceptionTests {

        @Test
        @DisplayName("should carry abend code and message")
        void abendCodeAndMessage() {
            DeleteCustomerException ex =
                    new DeleteCustomerException("WPV6", "Test error");
            assertEquals("WPV6", ex.getAbendCode());
            assertEquals("Test error", ex.getMessage());
        }

        @Test
        @DisplayName("should carry cause")
        void withCause() {
            RuntimeException cause = new RuntimeException("root cause");
            DeleteCustomerException ex =
                    new DeleteCustomerException("HWPT", "DB2 error", cause);
            assertEquals("HWPT", ex.getAbendCode());
            assertEquals(cause, ex.getCause());
        }
    }

    @Nested
    @DisplayName("Record Types")
    class RecordTests {

        @Test
        @DisplayName("CustomerRecord should hold all fields")
        void customerRecord() {
            CustomerRecord cr = new CustomerRecord(
                    "CUST", 987654, 42L, "Name", "Addr",
                    LocalDate.of(2000, 1, 1), 800,
                    LocalDate.of(2025, 6, 15));
            assertEquals("CUST", cr.eyeCatcher());
            assertEquals(987654, cr.sortCode());
            assertEquals(42L, cr.customerNumber());
            assertEquals("Name", cr.name());
            assertEquals("Addr", cr.address());
            assertEquals(LocalDate.of(2000, 1, 1), cr.dateOfBirth());
            assertEquals(800, cr.creditScore());
            assertEquals(LocalDate.of(2025, 6, 15), cr.csReviewDate());
        }

        @Test
        @DisplayName("AccountRecord should hold all fields")
        void accountRecord() {
            AccountRecord ar = new AccountRecord(
                    "ACCT", "0000000001", "987654", 12345678,
                    "SAVINGS ", new BigDecimal("3.50"),
                    15032020, 1000, 1012024, 1042024,
                    new BigDecimal("5000.00"), new BigDecimal("4500.00"));
            assertEquals("ACCT", ar.eyeCatcher());
            assertEquals(12345678, ar.accountNumber());
            assertEquals("SAVINGS ", ar.accountType());
        }

        @Test
        @DisplayName("AccountInquiryResult should hold accounts list")
        void accountInquiryResult() {
            List<AccountRecord> accounts = List.of(
                    buildAccountRecord(11111111));
            AccountInquiryResult result = new AccountInquiryResult(1, accounts);
            assertEquals(1, result.numberOfAccounts());
            assertEquals(1, result.accounts().size());
        }

        @Test
        @DisplayName("CustomerInquiryResult should hold success state")
        void customerInquiryResult() {
            CustomerInquiryResult success = new CustomerInquiryResult(true, " ");
            assertTrue(success.success());
            CustomerInquiryResult failure = new CustomerInquiryResult(false, "N");
            assertFalse(failure.success());
            assertEquals("N", failure.failCode());
        }

        @Test
        @DisplayName("CustomerRecord EYECATCHER_VALUE constant should be CUST")
        void eyecatcherConstant() {
            assertEquals("CUST", CustomerRecord.EYECATCHER_VALUE);
        }
    }

    // --- Helper methods ---

    private CustomerRecord buildTestCustomer() {
        return new CustomerRecord(
                "CUST", 987654, 1L,
                "Mr Test Customer",
                "1 Test Street, Testville",
                LocalDate.of(1990, 5, 20),
                700,
                LocalDate.of(2024, 3, 15)
        );
    }

    private AccountRecord buildAccountRecord(int accountNumber) {
        return new AccountRecord(
                "ACCT", TEST_CUSTOMER_NUMBER, TEST_SORT_CODE,
                accountNumber, "SAVINGS ",
                new BigDecimal("2.50"), 20052020, 500,
                1012024, 1042024,
                new BigDecimal("1000.00"), new BigDecimal("950.00")
        );
    }

    // --- Stub implementations ---

    private static class StubCustomerDataAccess implements CustomerDataAccess {
        CustomerInquiryResult inquiryResult;
        Optional<CustomerRecord> customerToReturn = Optional.empty();
        DeleteCustomerException throwOnDelete;
        int inquiryCallCount = 0;
        int deleteCallCount = 0;
        String lastSortCode;
        String lastCustomerNumber;

        @Override
        public CustomerInquiryResult inquireCustomer(String customerNumber) {
            inquiryCallCount++;
            return inquiryResult;
        }

        @Override
        public Optional<CustomerRecord> readAndDeleteCustomer(
                String sortCode, String customerNumber)
                throws DeleteCustomerException {
            deleteCallCount++;
            lastSortCode = sortCode;
            lastCustomerNumber = customerNumber;
            if (throwOnDelete != null) {
                throw throwOnDelete;
            }
            return customerToReturn;
        }
    }

    private static class StubAccountDataAccess implements AccountDataAccess {
        AccountInquiryResult accountInquiryResult =
                new AccountInquiryResult(0, Collections.emptyList());
        int getAccountsCallCount = 0;
        int deleteAccountCallCount = 0;
        List<Integer> deletedAccountNumbers = new ArrayList<>();
        String lastApplId;

        @Override
        public AccountInquiryResult getAccountsByCustomer(String customerNumber) {
            getAccountsCallCount++;
            return accountInquiryResult;
        }

        @Override
        public void deleteAccount(int accountNumber, String applId) {
            deleteAccountCallCount++;
            deletedAccountNumbers.add(accountNumber);
            lastApplId = applId;
        }
    }

    private static class StubProctranDataAccess
            implements ProcessedTransactionDataAccess {
        int writeCount = 0;
        DeleteCustomerException throwOnWrite;
        String lastEyeCatcher;
        String lastSortCode;
        String lastAccNumber;
        String lastDate;
        String lastTime;
        String lastReference;
        String lastType;
        String lastDescription;
        BigDecimal lastAmount;

        @Override
        public void writeProcessedTransaction(
                String eyeCatcher, String sortCode, String accNumber,
                String date, String time, String reference, String type,
                String description, BigDecimal amount)
                throws DeleteCustomerException {
            if (throwOnWrite != null) {
                throw throwOnWrite;
            }
            writeCount++;
            lastEyeCatcher = eyeCatcher;
            lastSortCode = sortCode;
            lastAccNumber = accNumber;
            lastDate = date;
            lastTime = time;
            lastReference = reference;
            lastType = type;
            lastDescription = description;
            lastAmount = amount;
        }
    }
}
