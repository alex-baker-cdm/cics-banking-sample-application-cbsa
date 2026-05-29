/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.migrated.cobol.dbcrfun;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DbcrFunTest {

    private static final String SORT_CODE = DbcrFun.SORT_CODE;
    private static final String ACCOUNT_NUMBER = "00000001";
    private static final LocalDateTime FIXED_TIME =
            LocalDateTime.of(2024, 6, 15, 14, 30, 45);
    private static final String FIXED_REFERENCE = "000000001234";

    private StubAccountRepository accountRepository;
    private StubProctranRepository proctranRepository;
    private DbcrFun dbcrFun;

    @BeforeEach
    void setUp() {
        accountRepository = new StubAccountRepository();
        proctranRepository = new StubProctranRepository();
        dbcrFun = new DbcrFun(
                accountRepository,
                proctranRepository,
                () -> FIXED_REFERENCE,
                () -> FIXED_TIME
        );
    }

    private AccountRecord defaultAccount(BigDecimal availableBalance,
            BigDecimal actualBalance) {
        return new AccountRecord(
                "ACCT", "0000000001", SORT_CODE, ACCOUNT_NUMBER,
                "CURRENT", new BigDecimal("1.50"),
                "2023-01-15", 1000,
                "2024-01-01", "2024-02-01",
                availableBalance, actualBalance
        );
    }

    private CreditDebitRequest tellerCredit(BigDecimal amount) {
        return new CreditDebitRequest(
                ACCOUNT_NUMBER, amount, FacilityType.TELLER, null);
    }

    private CreditDebitRequest tellerDebit(BigDecimal amount) {
        return new CreditDebitRequest(
                ACCOUNT_NUMBER, amount.negate(), FacilityType.TELLER, null);
    }

    private CreditDebitRequest paymentCredit(BigDecimal amount,
            String origin) {
        return new CreditDebitRequest(
                ACCOUNT_NUMBER, amount, FacilityType.PAYMENT, origin);
    }

    private CreditDebitRequest paymentDebit(BigDecimal amount,
            String origin) {
        return new CreditDebitRequest(
                ACCOUNT_NUMBER, amount.negate(), FacilityType.PAYMENT, origin);
    }

    // ---------------------------------------------------------------
    // Credit (Deposit) Tests
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Credit (deposit) operations")
    class CreditTests {

        @Test
        @DisplayName("Teller credit adds amount to both balances")
        void tellerCreditUpdatesBalances() {
            accountRepository.setAccount(
                    defaultAccount(new BigDecimal("500.00"),
                            new BigDecimal("500.00")));

            CreditDebitResult result = dbcrFun.process(
                    tellerCredit(new BigDecimal("100.00")));

            assertTrue(result.success());
            assertEquals(FailCode.NONE, result.failCode());
            assertEquals(0, new BigDecimal("600.00")
                    .compareTo(result.availableBalance()));
            assertEquals(0, new BigDecimal("600.00")
                    .compareTo(result.actualBalance()));
        }

        @Test
        @DisplayName("Teller credit writes CRE transaction to PROCTRAN")
        void tellerCreditWritesCreditTransaction() {
            accountRepository.setAccount(
                    defaultAccount(new BigDecimal("500.00"),
                            new BigDecimal("500.00")));

            dbcrFun.process(tellerCredit(new BigDecimal("200.00")));

            assertEquals(1, proctranRepository.getInsertedRecords().size());
            ProcessedTransactionRecord record =
                    proctranRepository.getInsertedRecords().get(0);
            assertEquals("PRTR", record.eyecatcher());
            assertEquals("CRE", record.type());
            assertEquals("COUNTER RECVED", record.description());
            assertEquals(0, new BigDecimal("200.00")
                    .compareTo(record.amount()));
            assertEquals(ACCOUNT_NUMBER, record.accNumber());
            assertEquals(SORT_CODE, record.sortCode());
            assertEquals("15.06.2024", record.date());
            assertEquals("143045", record.time());
            assertEquals(FIXED_REFERENCE, record.reference());
        }

        @Test
        @DisplayName("Payment credit writes PCR transaction to PROCTRAN")
        void paymentCreditWritesPaymentCreditTransaction() {
            accountRepository.setAccount(
                    defaultAccount(new BigDecimal("100.00"),
                            new BigDecimal("100.00")));

            dbcrFun.process(paymentCredit(
                    new BigDecimal("50.00"), "PAYMENT FROM XY"));

            assertEquals(1, proctranRepository.getInsertedRecords().size());
            ProcessedTransactionRecord record =
                    proctranRepository.getInsertedRecords().get(0);
            assertEquals("PCR", record.type());
            assertEquals("PAYMENT FROM X", record.description());
        }

        @Test
        @DisplayName("Credit to zero-balance account succeeds")
        void creditToZeroBalanceAccount() {
            accountRepository.setAccount(
                    defaultAccount(BigDecimal.ZERO, BigDecimal.ZERO));

            CreditDebitResult result = dbcrFun.process(
                    tellerCredit(new BigDecimal("1000.00")));

            assertTrue(result.success());
            assertEquals(0, new BigDecimal("1000.00")
                    .compareTo(result.availableBalance()));
        }

        @Test
        @DisplayName("Credit of zero amount succeeds")
        void creditZeroAmount() {
            accountRepository.setAccount(
                    defaultAccount(new BigDecimal("500.00"),
                            new BigDecimal("500.00")));

            CreditDebitResult result = dbcrFun.process(
                    tellerCredit(BigDecimal.ZERO));

            assertTrue(result.success());
            assertEquals(0, new BigDecimal("500.00")
                    .compareTo(result.availableBalance()));
        }
    }

    // ---------------------------------------------------------------
    // Debit (Withdrawal) Tests
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Debit (withdrawal) operations")
    class DebitTests {

        @Test
        @DisplayName("Teller debit subtracts amount from both balances")
        void tellerDebitUpdatesBalances() {
            accountRepository.setAccount(
                    defaultAccount(new BigDecimal("500.00"),
                            new BigDecimal("500.00")));

            CreditDebitResult result = dbcrFun.process(
                    tellerDebit(new BigDecimal("200.00")));

            assertTrue(result.success());
            assertEquals(0, new BigDecimal("300.00")
                    .compareTo(result.availableBalance()));
            assertEquals(0, new BigDecimal("300.00")
                    .compareTo(result.actualBalance()));
        }

        @Test
        @DisplayName("Teller debit writes DEB transaction to PROCTRAN")
        void tellerDebitWritesDebitTransaction() {
            accountRepository.setAccount(
                    defaultAccount(new BigDecimal("1000.00"),
                            new BigDecimal("1000.00")));

            dbcrFun.process(tellerDebit(new BigDecimal("300.00")));

            assertEquals(1, proctranRepository.getInsertedRecords().size());
            ProcessedTransactionRecord record =
                    proctranRepository.getInsertedRecords().get(0);
            assertEquals("DEB", record.type());
            assertEquals("COUNTER WTHDRW", record.description());
            assertEquals(0, new BigDecimal("-300.00")
                    .compareTo(record.amount()));
        }

        @Test
        @DisplayName("Payment debit writes PDR transaction to PROCTRAN")
        void paymentDebitWritesPaymentDebitTransaction() {
            accountRepository.setAccount(
                    defaultAccount(new BigDecimal("1000.00"),
                            new BigDecimal("1000.00")));

            dbcrFun.process(paymentDebit(
                    new BigDecimal("100.00"), "UTILITY BILL  "));

            assertEquals(1, proctranRepository.getInsertedRecords().size());
            ProcessedTransactionRecord record =
                    proctranRepository.getInsertedRecords().get(0);
            assertEquals("PDR", record.type());
            assertEquals("UTILITY BILL  ", record.description());
        }

        @Test
        @DisplayName("Teller debit allows overdraft (no insufficient funds check)")
        void tellerDebitAllowsOverdraft() {
            accountRepository.setAccount(
                    defaultAccount(new BigDecimal("100.00"),
                            new BigDecimal("100.00")));

            CreditDebitResult result = dbcrFun.process(
                    tellerDebit(new BigDecimal("500.00")));

            assertTrue(result.success());
            assertEquals(0, new BigDecimal("-400.00")
                    .compareTo(result.availableBalance()));
        }
    }

    // ---------------------------------------------------------------
    // Insufficient Funds Tests
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Insufficient funds validation")
    class InsufficientFundsTests {

        @Test
        @DisplayName("Payment debit with insufficient funds returns fail code 3")
        void paymentDebitInsufficientFunds() {
            accountRepository.setAccount(
                    defaultAccount(new BigDecimal("50.00"),
                            new BigDecimal("50.00")));

            CreditDebitResult result = dbcrFun.process(
                    paymentDebit(new BigDecimal("100.00"), "BILL PAY"));

            assertFalse(result.success());
            assertEquals(FailCode.INSUFFICIENT_FUNDS, result.failCode());
        }

        @Test
        @DisplayName("Payment debit exactly matching balance succeeds")
        void paymentDebitExactBalance() {
            accountRepository.setAccount(
                    defaultAccount(new BigDecimal("100.00"),
                            new BigDecimal("100.00")));

            CreditDebitResult result = dbcrFun.process(
                    paymentDebit(new BigDecimal("100.00"), "EXACT PAY"));

            assertTrue(result.success());
            assertEquals(0, BigDecimal.ZERO
                    .compareTo(result.availableBalance()));
        }

        @Test
        @DisplayName("Teller debit does NOT check insufficient funds")
        void tellerDebitIgnoresInsufficientFunds() {
            accountRepository.setAccount(
                    defaultAccount(new BigDecimal("50.00"),
                            new BigDecimal("50.00")));

            CreditDebitResult result = dbcrFun.process(
                    tellerDebit(new BigDecimal("100.00")));

            assertTrue(result.success());
        }
    }

    // ---------------------------------------------------------------
    // Account Type Restriction Tests (MORTGAGE / LOAN via PAYMENT)
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Account type restrictions for PAYMENT facility")
    class AccountTypeRestrictionTests {

        @Test
        @DisplayName("Payment debit from MORTGAGE account returns fail code 4")
        void paymentDebitFromMortgage() {
            AccountRecord mortgageAccount = new AccountRecord(
                    "ACCT", "0000000001", SORT_CODE, ACCOUNT_NUMBER,
                    "MORTGAGE", new BigDecimal("1.50"),
                    "2023-01-15", 0,
                    "2024-01-01", "2024-02-01",
                    new BigDecimal("10000.00"), new BigDecimal("10000.00")
            );
            accountRepository.setAccount(mortgageAccount);

            CreditDebitResult result = dbcrFun.process(
                    paymentDebit(new BigDecimal("100.00"), "MORTGAGE PAY"));

            assertFalse(result.success());
            assertEquals(FailCode.OPERATION_NOT_ALLOWED, result.failCode());
        }

        @Test
        @DisplayName("Payment debit from LOAN account returns fail code 4")
        void paymentDebitFromLoan() {
            AccountRecord loanAccount = new AccountRecord(
                    "ACCT", "0000000001", SORT_CODE, ACCOUNT_NUMBER,
                    "LOAN", new BigDecimal("2.00"),
                    "2023-01-15", 0,
                    "2024-01-01", "2024-02-01",
                    new BigDecimal("5000.00"), new BigDecimal("5000.00")
            );
            accountRepository.setAccount(loanAccount);

            CreditDebitResult result = dbcrFun.process(
                    paymentDebit(new BigDecimal("100.00"), "LOAN PAY"));

            assertFalse(result.success());
            assertEquals(FailCode.OPERATION_NOT_ALLOWED, result.failCode());
        }

        @Test
        @DisplayName("Payment credit to MORTGAGE account returns fail code 4")
        void paymentCreditToMortgage() {
            AccountRecord mortgageAccount = new AccountRecord(
                    "ACCT", "0000000001", SORT_CODE, ACCOUNT_NUMBER,
                    "MORTGAGE", new BigDecimal("1.50"),
                    "2023-01-15", 0,
                    "2024-01-01", "2024-02-01",
                    new BigDecimal("10000.00"), new BigDecimal("10000.00")
            );
            accountRepository.setAccount(mortgageAccount);

            CreditDebitResult result = dbcrFun.process(
                    paymentCredit(new BigDecimal("500.00"), "MORTGAGE IN"));

            assertFalse(result.success());
            assertEquals(FailCode.OPERATION_NOT_ALLOWED, result.failCode());
        }

        @Test
        @DisplayName("Payment credit to LOAN account returns fail code 4")
        void paymentCreditToLoan() {
            AccountRecord loanAccount = new AccountRecord(
                    "ACCT", "0000000001", SORT_CODE, ACCOUNT_NUMBER,
                    "LOAN", new BigDecimal("2.00"),
                    "2023-01-15", 0,
                    "2024-01-01", "2024-02-01",
                    new BigDecimal("5000.00"), new BigDecimal("5000.00")
            );
            accountRepository.setAccount(loanAccount);

            CreditDebitResult result = dbcrFun.process(
                    paymentCredit(new BigDecimal("200.00"), "LOAN IN"));

            assertFalse(result.success());
            assertEquals(FailCode.OPERATION_NOT_ALLOWED, result.failCode());
        }

        @Test
        @DisplayName("Teller debit from MORTGAGE account succeeds")
        void tellerDebitFromMortgage() {
            AccountRecord mortgageAccount = new AccountRecord(
                    "ACCT", "0000000001", SORT_CODE, ACCOUNT_NUMBER,
                    "MORTGAGE", new BigDecimal("1.50"),
                    "2023-01-15", 0,
                    "2024-01-01", "2024-02-01",
                    new BigDecimal("10000.00"), new BigDecimal("10000.00")
            );
            accountRepository.setAccount(mortgageAccount);

            CreditDebitResult result = dbcrFun.process(
                    tellerDebit(new BigDecimal("100.00")));

            assertTrue(result.success());
        }

        @Test
        @DisplayName("Teller credit to LOAN account succeeds")
        void tellerCreditToLoan() {
            AccountRecord loanAccount = new AccountRecord(
                    "ACCT", "0000000001", SORT_CODE, ACCOUNT_NUMBER,
                    "LOAN", new BigDecimal("2.00"),
                    "2023-01-15", 0,
                    "2024-01-01", "2024-02-01",
                    new BigDecimal("5000.00"), new BigDecimal("5000.00")
            );
            accountRepository.setAccount(loanAccount);

            CreditDebitResult result = dbcrFun.process(
                    tellerCredit(new BigDecimal("1000.00")));

            assertTrue(result.success());
        }
    }

    // ---------------------------------------------------------------
    // Account Not Found Tests
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Account not found")
    class AccountNotFoundTests {

        @Test
        @DisplayName("Non-existent account returns fail code 1")
        void accountNotFound() {
            CreditDebitResult result = dbcrFun.process(
                    tellerCredit(new BigDecimal("100.00")));

            assertFalse(result.success());
            assertEquals(FailCode.ACCOUNT_NOT_FOUND, result.failCode());
        }

        @Test
        @DisplayName("No PROCTRAN record written when account not found")
        void noProctranWhenAccountNotFound() {
            dbcrFun.process(tellerCredit(new BigDecimal("100.00")));

            assertTrue(proctranRepository.getInsertedRecords().isEmpty());
        }
    }

    // ---------------------------------------------------------------
    // Database Error Tests
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Database error handling")
    class DatabaseErrorTests {

        @Test
        @DisplayName("Account lookup DB error returns fail code 2")
        void accountLookupDbError() {
            accountRepository.setThrowOnFind(
                    new DataAccessException("DB2 error", -911));

            CreditDebitResult result = dbcrFun.process(
                    tellerCredit(new BigDecimal("100.00")));

            assertFalse(result.success());
            assertEquals(FailCode.DB_ERROR, result.failCode());
        }

        @Test
        @DisplayName("Account update DB error returns fail code 2")
        void accountUpdateDbError() {
            accountRepository.setAccount(
                    defaultAccount(new BigDecimal("500.00"),
                            new BigDecimal("500.00")));
            accountRepository.setThrowOnUpdate(
                    new DataAccessException("DB2 error", -803));

            CreditDebitResult result = dbcrFun.process(
                    tellerCredit(new BigDecimal("100.00")));

            assertFalse(result.success());
            assertEquals(FailCode.DB_ERROR, result.failCode());
        }

        @Test
        @DisplayName("PROCTRAN insert DB error returns fail code 2")
        void proctranInsertDbError() {
            accountRepository.setAccount(
                    defaultAccount(new BigDecimal("500.00"),
                            new BigDecimal("500.00")));
            proctranRepository.setThrowOnInsert(
                    new DataAccessException("PROCTRAN error", -805));

            CreditDebitResult result = dbcrFun.process(
                    tellerCredit(new BigDecimal("100.00")));

            assertFalse(result.success());
            assertEquals(FailCode.DB_ERROR, result.failCode());
        }

        @Test
        @DisplayName("Storm drain condition (SQLCODE 923) is logged")
        void stormDrainConditionLogged() {
            accountRepository.setThrowOnFind(
                    new DataAccessException("Connection lost", 923));

            CreditDebitResult result = dbcrFun.process(
                    tellerCredit(new BigDecimal("100.00")));

            assertFalse(result.success());
            assertEquals(FailCode.DB_ERROR, result.failCode());
        }

        @Test
        @DisplayName("Account update is persisted with correct balances")
        void accountUpdatePersistsCorrectBalances() {
            accountRepository.setAccount(
                    defaultAccount(new BigDecimal("1000.00"),
                            new BigDecimal("1000.00")));

            dbcrFun.process(tellerCredit(new BigDecimal("250.00")));

            AccountRecord updated = accountRepository.getLastUpdatedAccount();
            assertNotNull(updated);
            assertEquals(0, new BigDecimal("1250.00")
                    .compareTo(updated.availableBalance()));
            assertEquals(0, new BigDecimal("1250.00")
                    .compareTo(updated.actualBalance()));
        }
    }

    // ---------------------------------------------------------------
    // Sort Code Tests
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Sort code handling")
    class SortCodeTests {

        @Test
        @DisplayName("Result always contains the bank sort code")
        void resultContainsSortCode() {
            accountRepository.setAccount(
                    defaultAccount(new BigDecimal("500.00"),
                            new BigDecimal("500.00")));

            CreditDebitResult result = dbcrFun.process(
                    tellerCredit(new BigDecimal("100.00")));

            assertEquals("987654", result.sortCode());
        }

        @Test
        @DisplayName("Failure result also contains sort code")
        void failureResultContainsSortCode() {
            CreditDebitResult result = dbcrFun.process(
                    tellerCredit(new BigDecimal("100.00")));

            assertFalse(result.success());
            assertEquals("987654", result.sortCode());
        }
    }

    // ---------------------------------------------------------------
    // Origin Description Truncation Tests
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Origin description handling")
    class OriginDescriptionTests {

        @Test
        @DisplayName("Origin description truncated to 14 characters")
        void originDescriptionTruncatedTo14Chars() {
            accountRepository.setAccount(
                    defaultAccount(new BigDecimal("1000.00"),
                            new BigDecimal("1000.00")));

            dbcrFun.process(paymentCredit(
                    new BigDecimal("50.00"),
                    "THIS IS A VERY LONG DESCRIPTION"));

            ProcessedTransactionRecord record =
                    proctranRepository.getInsertedRecords().get(0);
            assertEquals(14, record.description().length());
            assertEquals("THIS IS A VERY", record.description());
        }

        @Test
        @DisplayName("Short origin description is not truncated")
        void shortOriginNotTruncated() {
            accountRepository.setAccount(
                    defaultAccount(new BigDecimal("1000.00"),
                            new BigDecimal("1000.00")));

            dbcrFun.process(paymentCredit(
                    new BigDecimal("50.00"), "SHORT"));

            ProcessedTransactionRecord record =
                    proctranRepository.getInsertedRecords().get(0);
            assertEquals("SHORT", record.description());
        }

        @Test
        @DisplayName("Null origin description results in empty string")
        void nullOriginResultsInEmpty() {
            accountRepository.setAccount(
                    defaultAccount(new BigDecimal("1000.00"),
                            new BigDecimal("1000.00")));

            dbcrFun.process(paymentCredit(
                    new BigDecimal("50.00"), null));

            ProcessedTransactionRecord record =
                    proctranRepository.getInsertedRecords().get(0);
            assertEquals("", record.description());
        }
    }

    // ---------------------------------------------------------------
    // Convenience Constructor Test
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Default constructor")
    class DefaultConstructorTests {

        @Test
        @DisplayName("Two-argument constructor works with defaults")
        void twoArgConstructorWorks() {
            DbcrFun defaultDbcrFun = new DbcrFun(
                    accountRepository, proctranRepository);

            accountRepository.setAccount(
                    defaultAccount(new BigDecimal("500.00"),
                            new BigDecimal("500.00")));

            CreditDebitResult result = defaultDbcrFun.process(
                    tellerCredit(new BigDecimal("100.00")));

            assertTrue(result.success());
        }
    }

    // ---------------------------------------------------------------
    // Record / Enum Tests
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("Data model tests")
    class DataModelTests {

        @Test
        @DisplayName("CreditDebitRequest correctly identifies debit")
        void requestIdentifiesDebit() {
            CreditDebitRequest request = new CreditDebitRequest(
                    ACCOUNT_NUMBER, new BigDecimal("-100.00"),
                    FacilityType.TELLER, null);
            assertTrue(request.isDebit());
            assertFalse(request.isCredit());
            assertFalse(request.isFromPayment());
        }

        @Test
        @DisplayName("CreditDebitRequest correctly identifies credit")
        void requestIdentifiesCredit() {
            CreditDebitRequest request = new CreditDebitRequest(
                    ACCOUNT_NUMBER, new BigDecimal("100.00"),
                    FacilityType.PAYMENT, "ORIGIN");
            assertFalse(request.isDebit());
            assertTrue(request.isCredit());
            assertTrue(request.isFromPayment());
        }

        @Test
        @DisplayName("FacilityType.fromCode returns correct values")
        void facilityTypeFromCode() {
            assertEquals(FacilityType.PAYMENT,
                    FacilityType.fromCode(496));
            assertEquals(FacilityType.TELLER,
                    FacilityType.fromCode(0));
            assertEquals(FacilityType.TELLER,
                    FacilityType.fromCode(999));
        }

        @Test
        @DisplayName("FailCode has correct code values")
        void failCodeValues() {
            assertEquals("0", FailCode.NONE.getCode());
            assertEquals("1", FailCode.ACCOUNT_NOT_FOUND.getCode());
            assertEquals("2", FailCode.DB_ERROR.getCode());
            assertEquals("3", FailCode.INSUFFICIENT_FUNDS.getCode());
            assertEquals("4", FailCode.OPERATION_NOT_ALLOWED.getCode());
        }

        @Test
        @DisplayName("TransactionType has correct code values")
        void transactionTypeValues() {
            assertEquals("DEB", TransactionType.DEBIT.getCode());
            assertEquals("CRE", TransactionType.CREDIT.getCode());
            assertEquals("PDR", TransactionType.PAYMENT_DEBIT.getCode());
            assertEquals("PCR", TransactionType.PAYMENT_CREDIT.getCode());
        }

        @Test
        @DisplayName("AccountRecord identifies mortgage correctly")
        void accountRecordMortgage() {
            AccountRecord account = new AccountRecord(
                    "ACCT", "0000000001", SORT_CODE, ACCOUNT_NUMBER,
                    "MORTGAGE", BigDecimal.ONE, "2023-01-01", 0,
                    "2024-01-01", "2024-02-01",
                    BigDecimal.ZERO, BigDecimal.ZERO);
            assertTrue(account.isMortgage());
            assertFalse(account.isLoan());
        }

        @Test
        @DisplayName("AccountRecord identifies loan correctly (with trailing spaces)")
        void accountRecordLoan() {
            AccountRecord account = new AccountRecord(
                    "ACCT", "0000000001", SORT_CODE, ACCOUNT_NUMBER,
                    "LOAN    ", BigDecimal.ONE, "2023-01-01", 0,
                    "2024-01-01", "2024-02-01",
                    BigDecimal.ZERO, BigDecimal.ZERO);
            assertFalse(account.isMortgage());
            assertTrue(account.isLoan());
        }

        @Test
        @DisplayName("AccountRecord.withUpdatedBalances creates new record")
        void withUpdatedBalances() {
            AccountRecord original = defaultAccount(
                    new BigDecimal("500.00"), new BigDecimal("500.00"));
            AccountRecord updated = original.withUpdatedBalances(
                    new BigDecimal("600.00"), new BigDecimal("600.00"));

            assertEquals(0, new BigDecimal("600.00")
                    .compareTo(updated.availableBalance()));
            assertEquals(0, new BigDecimal("600.00")
                    .compareTo(updated.actualBalance()));
            assertEquals(original.customerNumber(),
                    updated.customerNumber());
            assertEquals(original.accountType(), updated.accountType());
        }

        @Test
        @DisplayName("CreditDebitResult.failure creates failed result")
        void resultFailure() {
            CreditDebitResult result = CreditDebitResult.failure(
                    FailCode.INSUFFICIENT_FUNDS, SORT_CODE);
            assertFalse(result.success());
            assertEquals(FailCode.INSUFFICIENT_FUNDS, result.failCode());
            assertNull(result.availableBalance());
            assertNull(result.actualBalance());
        }

        @Test
        @DisplayName("CreditDebitResult.success creates successful result")
        void resultSuccess() {
            CreditDebitResult result = CreditDebitResult.success(
                    new BigDecimal("100.00"), new BigDecimal("100.00"),
                    SORT_CODE);
            assertTrue(result.success());
            assertEquals(FailCode.NONE, result.failCode());
            assertNotNull(result.availableBalance());
        }

        @Test
        @DisplayName("DataAccessException storm drain detection")
        void dataAccessExceptionStormDrain() {
            DataAccessException stormDrain =
                    new DataAccessException("Connection lost", 923);
            assertTrue(stormDrain.isStormDrainCondition());
            assertEquals(923, stormDrain.getSqlCode());

            DataAccessException normal =
                    new DataAccessException("Some error", -803);
            assertFalse(normal.isStormDrainCondition());
        }

        @Test
        @DisplayName("DataAccessException with cause")
        void dataAccessExceptionWithCause() {
            RuntimeException cause = new RuntimeException("root");
            DataAccessException ex =
                    new DataAccessException("Wrapped", -911, cause);
            assertEquals(-911, ex.getSqlCode());
            assertEquals(cause, ex.getCause());
        }

        @Test
        @DisplayName("ProcessedTransactionRecord eyecatcher constant")
        void proctranEyecatcher() {
            assertEquals("PRTR",
                    ProcessedTransactionRecord.EYECATCHER_VALUE);
        }
    }

    // ---------------------------------------------------------------
    // Stub Implementations
    // ---------------------------------------------------------------

    private static class StubAccountRepository
            implements AccountRepository {

        private AccountRecord account;
        private AccountRecord lastUpdatedAccount;
        private DataAccessException throwOnFind;
        private DataAccessException throwOnUpdate;

        void setAccount(AccountRecord account) {
            this.account = account;
        }

        void setThrowOnFind(DataAccessException e) {
            this.throwOnFind = e;
        }

        void setThrowOnUpdate(DataAccessException e) {
            this.throwOnUpdate = e;
        }

        AccountRecord getLastUpdatedAccount() {
            return lastUpdatedAccount;
        }

        @Override
        public Optional<AccountRecord> findBySortCodeAndAccountNumber(
                String sortCode, String accountNumber)
                throws DataAccessException {
            if (throwOnFind != null) {
                throw throwOnFind;
            }
            return Optional.ofNullable(account);
        }

        @Override
        public void updateAccount(AccountRecord account)
                throws DataAccessException {
            if (throwOnUpdate != null) {
                throw throwOnUpdate;
            }
            this.lastUpdatedAccount = account;
        }
    }

    private static class StubProctranRepository
            implements ProcessedTransactionRepository {

        private final List<ProcessedTransactionRecord> insertedRecords =
                new ArrayList<>();
        private DataAccessException throwOnInsert;

        void setThrowOnInsert(DataAccessException e) {
            this.throwOnInsert = e;
        }

        List<ProcessedTransactionRecord> getInsertedRecords() {
            return insertedRecords;
        }

        @Override
        public void insertTransaction(ProcessedTransactionRecord record)
                throws DataAccessException {
            if (throwOnInsert != null) {
                throw throwOnInsert;
            }
            insertedRecords.add(record);
        }
    }
}
