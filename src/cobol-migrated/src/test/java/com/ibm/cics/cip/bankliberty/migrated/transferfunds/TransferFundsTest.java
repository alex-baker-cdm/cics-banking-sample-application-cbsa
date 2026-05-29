/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bankliberty.migrated.transferfunds;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferFundsTest {

    private static final String SORT_CODE = "987654";
    private static final String FROM_ACCOUNT = "00001111";
    private static final String TO_ACCOUNT = "00002222";

    @Mock
    private AccountRepository repository;

    private TransferFunds transferFunds;

    @BeforeEach
    void setUp() {
        transferFunds = new TransferFunds(repository);
    }

    private AccountRecord createAccount(String sortCode, String accountNumber,
                                         BigDecimal availableBalance,
                                         BigDecimal actualBalance) {
        return new AccountRecord(
                "ACCT", "0000000001",
                sortCode, accountNumber,
                "SAVINGS", new BigDecimal("1.50"),
                "01.01.2023", 0,
                "01.01.2023", "01.02.2023",
                availableBalance, actualBalance);
    }

    @Nested
    @DisplayName("Successful Transfer Tests")
    class SuccessfulTransfers {

        @Test
        @DisplayName("Transfer succeeds when FROM < TO (FROM updated first)")
        void transferSucceedsFromLessThanTo() throws DataAccessException {
            AccountRecord fromAccount = createAccount(
                    SORT_CODE, FROM_ACCOUNT,
                    new BigDecimal("1000.00"), new BigDecimal("1000.00"));
            AccountRecord toAccount = createAccount(
                    SORT_CODE, TO_ACCOUNT,
                    new BigDecimal("500.00"), new BigDecimal("500.00"));

            when(repository.findAccount(SORT_CODE, FROM_ACCOUNT))
                    .thenReturn(Optional.of(fromAccount));
            when(repository.findAccount(SORT_CODE, TO_ACCOUNT))
                    .thenReturn(Optional.of(toAccount));

            TransferFundsRequest request = new TransferFundsRequest(
                    FROM_ACCOUNT, SORT_CODE, TO_ACCOUNT, SORT_CODE,
                    new BigDecimal("250.00"));

            TransferFundsResult result = transferFunds.transfer(request);

            assertTrue(result.success());
            assertEquals(TransferFundsResult.FailCode.NONE, result.failCode());
            assertEquals(new BigDecimal("750.00"), result.fromAvailableBalance());
            assertEquals(new BigDecimal("750.00"), result.fromActualBalance());
            assertEquals(new BigDecimal("750.00"), result.toAvailableBalance());
            assertEquals(new BigDecimal("750.00"), result.toActualBalance());

            verify(repository, times(2)).updateAccount(any());
            verify(repository).writeProcessedTransaction(any());
        }

        @Test
        @DisplayName("Transfer succeeds when FROM > TO (TO updated first)")
        void transferSucceedsFromGreaterThanTo() throws DataAccessException {
            String highAccount = "00009999";
            String lowAccount = "00000001";

            AccountRecord fromAccount = createAccount(
                    SORT_CODE, highAccount,
                    new BigDecimal("2000.00"), new BigDecimal("2000.00"));
            AccountRecord toAccount = createAccount(
                    SORT_CODE, lowAccount,
                    new BigDecimal("300.00"), new BigDecimal("300.00"));

            when(repository.findAccount(SORT_CODE, highAccount))
                    .thenReturn(Optional.of(fromAccount));
            when(repository.findAccount(SORT_CODE, lowAccount))
                    .thenReturn(Optional.of(toAccount));

            TransferFundsRequest request = new TransferFundsRequest(
                    highAccount, SORT_CODE, lowAccount, SORT_CODE,
                    new BigDecimal("100.00"));

            TransferFundsResult result = transferFunds.transfer(request);

            assertTrue(result.success());
            assertEquals(new BigDecimal("1900.00"),
                    result.fromAvailableBalance());
            assertEquals(new BigDecimal("1900.00"),
                    result.fromActualBalance());
            assertEquals(new BigDecimal("400.00"),
                    result.toAvailableBalance());
            assertEquals(new BigDecimal("400.00"),
                    result.toActualBalance());

            verify(repository, times(2)).updateAccount(any());
            verify(repository).writeProcessedTransaction(any());
        }

        @Test
        @DisplayName("Transfer succeeds with decimal amount")
        void transferSucceedsWithDecimalAmount() throws DataAccessException {
            AccountRecord fromAccount = createAccount(
                    SORT_CODE, FROM_ACCOUNT,
                    new BigDecimal("1000.50"), new BigDecimal("1000.50"));
            AccountRecord toAccount = createAccount(
                    SORT_CODE, TO_ACCOUNT,
                    new BigDecimal("500.25"), new BigDecimal("500.25"));

            when(repository.findAccount(SORT_CODE, FROM_ACCOUNT))
                    .thenReturn(Optional.of(fromAccount));
            when(repository.findAccount(SORT_CODE, TO_ACCOUNT))
                    .thenReturn(Optional.of(toAccount));

            TransferFundsRequest request = new TransferFundsRequest(
                    FROM_ACCOUNT, SORT_CODE, TO_ACCOUNT, SORT_CODE,
                    new BigDecimal("99.99"));

            TransferFundsResult result = transferFunds.transfer(request);

            assertTrue(result.success());
            assertEquals(new BigDecimal("900.51"),
                    result.fromAvailableBalance());
            assertEquals(new BigDecimal("600.24"),
                    result.toAvailableBalance());
        }

        @Test
        @DisplayName("Transfer succeeds with large amount")
        void transferSucceedsWithLargeAmount() throws DataAccessException {
            AccountRecord fromAccount = createAccount(
                    SORT_CODE, FROM_ACCOUNT,
                    new BigDecimal("9999999999.99"),
                    new BigDecimal("9999999999.99"));
            AccountRecord toAccount = createAccount(
                    SORT_CODE, TO_ACCOUNT,
                    new BigDecimal("0.01"), new BigDecimal("0.01"));

            when(repository.findAccount(SORT_CODE, FROM_ACCOUNT))
                    .thenReturn(Optional.of(fromAccount));
            when(repository.findAccount(SORT_CODE, TO_ACCOUNT))
                    .thenReturn(Optional.of(toAccount));

            TransferFundsRequest request = new TransferFundsRequest(
                    FROM_ACCOUNT, SORT_CODE, TO_ACCOUNT, SORT_CODE,
                    new BigDecimal("5000000000.00"));

            TransferFundsResult result = transferFunds.transfer(request);

            assertTrue(result.success());
            assertEquals(new BigDecimal("4999999999.99"),
                    result.fromAvailableBalance());
            assertEquals(new BigDecimal("5000000000.01"),
                    result.toAvailableBalance());
        }

        @Test
        @DisplayName("Transfer with minimum positive amount (0.01)")
        void transferSucceedsWithMinimumAmount() throws DataAccessException {
            AccountRecord fromAccount = createAccount(
                    SORT_CODE, FROM_ACCOUNT,
                    new BigDecimal("100.00"), new BigDecimal("100.00"));
            AccountRecord toAccount = createAccount(
                    SORT_CODE, TO_ACCOUNT,
                    new BigDecimal("200.00"), new BigDecimal("200.00"));

            when(repository.findAccount(SORT_CODE, FROM_ACCOUNT))
                    .thenReturn(Optional.of(fromAccount));
            when(repository.findAccount(SORT_CODE, TO_ACCOUNT))
                    .thenReturn(Optional.of(toAccount));

            TransferFundsRequest request = new TransferFundsRequest(
                    FROM_ACCOUNT, SORT_CODE, TO_ACCOUNT, SORT_CODE,
                    new BigDecimal("0.01"));

            TransferFundsResult result = transferFunds.transfer(request);

            assertTrue(result.success());
            assertEquals(new BigDecimal("99.99"),
                    result.fromAvailableBalance());
            assertEquals(new BigDecimal("200.01"),
                    result.toAvailableBalance());
        }
    }

    @Nested
    @DisplayName("Amount Validation Tests")
    class AmountValidation {

        @Test
        @DisplayName("Transfer fails with zero amount (fail code '4')")
        void transferFailsWithZeroAmount() {
            TransferFundsRequest request = new TransferFundsRequest(
                    FROM_ACCOUNT, SORT_CODE, TO_ACCOUNT, SORT_CODE,
                    BigDecimal.ZERO);

            TransferFundsResult result = transferFunds.transfer(request);

            assertFalse(result.success());
            assertEquals(TransferFundsResult.FailCode.INVALID_AMOUNT,
                    result.failCode());
        }

        @Test
        @DisplayName("Transfer fails with negative amount (fail code '4')")
        void transferFailsWithNegativeAmount() {
            TransferFundsRequest request = new TransferFundsRequest(
                    FROM_ACCOUNT, SORT_CODE, TO_ACCOUNT, SORT_CODE,
                    new BigDecimal("-100.00"));

            TransferFundsResult result = transferFunds.transfer(request);

            assertFalse(result.success());
            assertEquals(TransferFundsResult.FailCode.INVALID_AMOUNT,
                    result.failCode());
        }
    }

    @Nested
    @DisplayName("Same Account Validation Tests")
    class SameAccountValidation {

        @Test
        @DisplayName("Transfer abends when FROM and TO are the same account")
        void transferAbendsForSameAccount() {
            TransferFundsRequest request = new TransferFundsRequest(
                    FROM_ACCOUNT, SORT_CODE, FROM_ACCOUNT, SORT_CODE,
                    new BigDecimal("100.00"));

            TransferFundsException ex = assertThrows(
                    TransferFundsException.class,
                    () -> transferFunds.transfer(request));

            assertEquals("SAME", ex.getAbendCode());
        }
    }

    @Nested
    @DisplayName("FROM Account Not Found Tests")
    class FromAccountNotFound {

        @Test
        @DisplayName("Transfer fails when FROM account not found (fail code '1')")
        void transferFailsFromAccountNotFound() throws DataAccessException {
            when(repository.findAccount(SORT_CODE, FROM_ACCOUNT))
                    .thenReturn(Optional.empty());

            TransferFundsRequest request = new TransferFundsRequest(
                    FROM_ACCOUNT, SORT_CODE, TO_ACCOUNT, SORT_CODE,
                    new BigDecimal("100.00"));

            TransferFundsResult result = transferFunds.transfer(request);

            assertFalse(result.success());
            assertEquals(TransferFundsResult.FailCode.FROM_ACCOUNT_NOT_FOUND,
                    result.failCode());
            verify(repository).rollback();
        }

        @Test
        @DisplayName("Transfer fails when FROM account not found in TO-first path")
        void transferFailsFromNotFoundInToFirstPath()
                throws DataAccessException {
            String highAccount = "00009999";
            String lowAccount = "00000001";

            AccountRecord toAccount = createAccount(
                    SORT_CODE, lowAccount,
                    new BigDecimal("500.00"), new BigDecimal("500.00"));

            when(repository.findAccount(SORT_CODE, lowAccount))
                    .thenReturn(Optional.of(toAccount));
            when(repository.findAccount(SORT_CODE, highAccount))
                    .thenReturn(Optional.empty());

            TransferFundsRequest request = new TransferFundsRequest(
                    highAccount, SORT_CODE, lowAccount, SORT_CODE,
                    new BigDecimal("100.00"));

            TransferFundsResult result = transferFunds.transfer(request);

            assertFalse(result.success());
            assertEquals(TransferFundsResult.FailCode.FROM_ACCOUNT_NOT_FOUND,
                    result.failCode());
            verify(repository).rollback();
        }
    }

    @Nested
    @DisplayName("TO Account Not Found Tests")
    class ToAccountNotFound {

        @Test
        @DisplayName("Transfer fails when TO account not found (fail code '2') and rolls back")
        void transferFailsToAccountNotFound() throws DataAccessException {
            AccountRecord fromAccount = createAccount(
                    SORT_CODE, FROM_ACCOUNT,
                    new BigDecimal("1000.00"), new BigDecimal("1000.00"));

            when(repository.findAccount(SORT_CODE, FROM_ACCOUNT))
                    .thenReturn(Optional.of(fromAccount));
            when(repository.findAccount(SORT_CODE, TO_ACCOUNT))
                    .thenReturn(Optional.empty());

            TransferFundsRequest request = new TransferFundsRequest(
                    FROM_ACCOUNT, SORT_CODE, TO_ACCOUNT, SORT_CODE,
                    new BigDecimal("100.00"));

            TransferFundsResult result = transferFunds.transfer(request);

            assertFalse(result.success());
            assertEquals(TransferFundsResult.FailCode.TO_ACCOUNT_NOT_FOUND,
                    result.failCode());
            verify(repository).rollback();
        }

        @Test
        @DisplayName("Transfer fails when TO not found in TO-first path")
        void transferFailsToNotFoundInToFirstPath()
                throws DataAccessException {
            String highAccount = "00009999";
            String lowAccount = "00000001";

            when(repository.findAccount(SORT_CODE, lowAccount))
                    .thenReturn(Optional.empty());

            TransferFundsRequest request = new TransferFundsRequest(
                    highAccount, SORT_CODE, lowAccount, SORT_CODE,
                    new BigDecimal("100.00"));

            TransferFundsResult result = transferFunds.transfer(request);

            assertFalse(result.success());
            assertEquals(TransferFundsResult.FailCode.TO_ACCOUNT_NOT_FOUND,
                    result.failCode());
            verify(repository).rollback();
        }
    }

    @Nested
    @DisplayName("Database Error Tests")
    class DatabaseErrors {

        @Test
        @DisplayName("Transfer abends with DB error reading FROM account (abend 'FROM')")
        void transferAbendsDbErrorReadingFromAccount()
                throws DataAccessException {
            when(repository.findAccount(SORT_CODE, FROM_ACCOUNT))
                    .thenThrow(new DataAccessException("DB error", -904));

            TransferFundsRequest request = new TransferFundsRequest(
                    FROM_ACCOUNT, SORT_CODE, TO_ACCOUNT, SORT_CODE,
                    new BigDecimal("100.00"));

            TransferFundsException ex = assertThrows(
                    TransferFundsException.class,
                    () -> transferFunds.transfer(request));

            assertEquals("FROM", ex.getAbendCode());
        }

        @Test
        @DisplayName("Transfer abends with DB error updating FROM account (abend 'FROM')")
        void transferAbendsDbErrorUpdatingFromAccount()
                throws DataAccessException {
            AccountRecord fromAccount = createAccount(
                    SORT_CODE, FROM_ACCOUNT,
                    new BigDecimal("1000.00"), new BigDecimal("1000.00"));

            when(repository.findAccount(SORT_CODE, FROM_ACCOUNT))
                    .thenReturn(Optional.of(fromAccount));
            doThrow(new DataAccessException("Update error", -904))
                    .when(repository).updateAccount(any());

            TransferFundsRequest request = new TransferFundsRequest(
                    FROM_ACCOUNT, SORT_CODE, TO_ACCOUNT, SORT_CODE,
                    new BigDecimal("100.00"));

            TransferFundsException ex = assertThrows(
                    TransferFundsException.class,
                    () -> transferFunds.transfer(request));

            assertEquals("FROM", ex.getAbendCode());
        }

        @Test
        @DisplayName("Transfer abends with DB error reading TO account (abend 'RUF2')")
        void transferAbendsDbErrorReadingToAccount()
                throws DataAccessException {
            AccountRecord fromAccount = createAccount(
                    SORT_CODE, FROM_ACCOUNT,
                    new BigDecimal("1000.00"), new BigDecimal("1000.00"));

            when(repository.findAccount(SORT_CODE, FROM_ACCOUNT))
                    .thenReturn(Optional.of(fromAccount));
            when(repository.findAccount(SORT_CODE, TO_ACCOUNT))
                    .thenThrow(new DataAccessException("DB error", -904));

            TransferFundsRequest request = new TransferFundsRequest(
                    FROM_ACCOUNT, SORT_CODE, TO_ACCOUNT, SORT_CODE,
                    new BigDecimal("100.00"));

            TransferFundsException ex = assertThrows(
                    TransferFundsException.class,
                    () -> transferFunds.transfer(request));

            assertEquals("RUF2", ex.getAbendCode());
        }

        @Test
        @DisplayName("Transfer abends with DB error updating TO account (abend 'RUF3')")
        void transferAbendsDbErrorUpdatingToAccount()
                throws DataAccessException {
            AccountRecord fromAccount = createAccount(
                    SORT_CODE, FROM_ACCOUNT,
                    new BigDecimal("1000.00"), new BigDecimal("1000.00"));
            AccountRecord toAccount = createAccount(
                    SORT_CODE, TO_ACCOUNT,
                    new BigDecimal("500.00"), new BigDecimal("500.00"));

            when(repository.findAccount(SORT_CODE, FROM_ACCOUNT))
                    .thenReturn(Optional.of(fromAccount));
            when(repository.findAccount(SORT_CODE, TO_ACCOUNT))
                    .thenReturn(Optional.of(toAccount));
            doNothing().doThrow(new DataAccessException("Update error", -904))
                    .when(repository).updateAccount(any());

            TransferFundsRequest request = new TransferFundsRequest(
                    FROM_ACCOUNT, SORT_CODE, TO_ACCOUNT, SORT_CODE,
                    new BigDecimal("100.00"));

            TransferFundsException ex = assertThrows(
                    TransferFundsException.class,
                    () -> transferFunds.transfer(request));

            assertEquals("RUF3", ex.getAbendCode());
        }

        @Test
        @DisplayName("Storm drain is logged for SQLCODE 923 (connection lost) then abends")
        void stormDrainLoggedForConnectionLost() throws DataAccessException {
            when(repository.findAccount(SORT_CODE, FROM_ACCOUNT))
                    .thenThrow(new DataAccessException(
                            "DB2 Connection lost", 923));

            TransferFundsRequest request = new TransferFundsRequest(
                    FROM_ACCOUNT, SORT_CODE, TO_ACCOUNT, SORT_CODE,
                    new BigDecimal("100.00"));

            TransferFundsException ex = assertThrows(
                    TransferFundsException.class,
                    () -> transferFunds.transfer(request));

            assertEquals("FROM", ex.getAbendCode());
        }
    }

    @Nested
    @DisplayName("Rollback Failure Tests")
    class RollbackFailures {

        @Test
        @DisplayName("Rollback failure abends with 'HROL'")
        void rollbackFailureAbendsHrol() throws DataAccessException {
            when(repository.findAccount(SORT_CODE, FROM_ACCOUNT))
                    .thenReturn(Optional.empty());
            doThrow(new DataAccessException("Rollback failed"))
                    .when(repository).rollback();

            TransferFundsRequest request = new TransferFundsRequest(
                    FROM_ACCOUNT, SORT_CODE, TO_ACCOUNT, SORT_CODE,
                    new BigDecimal("100.00"));

            TransferFundsException ex = assertThrows(
                    TransferFundsException.class,
                    () -> transferFunds.transfer(request));

            assertEquals("HROL", ex.getAbendCode());
        }

        @Test
        @DisplayName("Rollback failure when TO account not found after FROM updated")
        void rollbackFailureToNotFoundAfterFromUpdated()
                throws DataAccessException {
            AccountRecord fromAccount = createAccount(
                    SORT_CODE, FROM_ACCOUNT,
                    new BigDecimal("1000.00"), new BigDecimal("1000.00"));

            when(repository.findAccount(SORT_CODE, FROM_ACCOUNT))
                    .thenReturn(Optional.of(fromAccount));
            when(repository.findAccount(SORT_CODE, TO_ACCOUNT))
                    .thenReturn(Optional.empty());
            doThrow(new DataAccessException("Rollback failed"))
                    .when(repository).rollback();

            TransferFundsRequest request = new TransferFundsRequest(
                    FROM_ACCOUNT, SORT_CODE, TO_ACCOUNT, SORT_CODE,
                    new BigDecimal("100.00"));

            TransferFundsException ex = assertThrows(
                    TransferFundsException.class,
                    () -> transferFunds.transfer(request));

            assertEquals("HROL", ex.getAbendCode());
        }
    }

    @Nested
    @DisplayName("PROCTRAN Write Tests")
    class ProctranTests {

        @Test
        @DisplayName("PROCTRAN write failure abends with 'WPCD'")
        void proctranWriteFailureAbends() throws DataAccessException {
            AccountRecord fromAccount = createAccount(
                    SORT_CODE, FROM_ACCOUNT,
                    new BigDecimal("1000.00"), new BigDecimal("1000.00"));
            AccountRecord toAccount = createAccount(
                    SORT_CODE, TO_ACCOUNT,
                    new BigDecimal("500.00"), new BigDecimal("500.00"));

            when(repository.findAccount(SORT_CODE, FROM_ACCOUNT))
                    .thenReturn(Optional.of(fromAccount));
            when(repository.findAccount(SORT_CODE, TO_ACCOUNT))
                    .thenReturn(Optional.of(toAccount));
            doThrow(new DataAccessException("PROCTRAN write failed"))
                    .when(repository).writeProcessedTransaction(any());

            TransferFundsRequest request = new TransferFundsRequest(
                    FROM_ACCOUNT, SORT_CODE, TO_ACCOUNT, SORT_CODE,
                    new BigDecimal("100.00"));

            TransferFundsException ex = assertThrows(
                    TransferFundsException.class,
                    () -> transferFunds.transfer(request));

            assertEquals("WPCD", ex.getAbendCode());
        }

        @Test
        @DisplayName("PROCTRAN record has correct transfer type and description")
        void proctranRecordHasCorrectContent() throws DataAccessException {
            AccountRecord fromAccount = createAccount(
                    SORT_CODE, FROM_ACCOUNT,
                    new BigDecimal("1000.00"), new BigDecimal("1000.00"));
            AccountRecord toAccount = createAccount(
                    SORT_CODE, TO_ACCOUNT,
                    new BigDecimal("500.00"), new BigDecimal("500.00"));

            when(repository.findAccount(SORT_CODE, FROM_ACCOUNT))
                    .thenReturn(Optional.of(fromAccount));
            when(repository.findAccount(SORT_CODE, TO_ACCOUNT))
                    .thenReturn(Optional.of(toAccount));

            TransferFundsRequest request = new TransferFundsRequest(
                    FROM_ACCOUNT, SORT_CODE, TO_ACCOUNT, SORT_CODE,
                    new BigDecimal("250.00"));

            transferFunds.transfer(request);

            verify(repository).writeProcessedTransaction(
                    argThat(transaction -> {
                        assertEquals("PRTR", transaction.eyeCatcher());
                        assertEquals(SORT_CODE, transaction.sortCode());
                        assertEquals(FROM_ACCOUNT, transaction.accountNumber());
                        assertEquals("TFR", transaction.type());
                        assertEquals(new BigDecimal("250.00"),
                                transaction.amount());
                        assertTrue(transaction.description()
                                .startsWith("TRANSFER"));
                        assertTrue(transaction.description()
                                .contains(SORT_CODE));
                        assertTrue(transaction.description()
                                .contains(TO_ACCOUNT));
                        return true;
                    }));
        }
    }

    @Nested
    @DisplayName("Ordering Logic Tests")
    class OrderingLogic {

        @Test
        @DisplayName("FROM account updated first when FROM < TO")
        void fromUpdatedFirstWhenSmaller() throws DataAccessException {
            String smallAccount = "00000001";
            String largeAccount = "00000002";

            AccountRecord fromAccount = createAccount(
                    SORT_CODE, smallAccount,
                    new BigDecimal("1000.00"), new BigDecimal("1000.00"));
            AccountRecord toAccount = createAccount(
                    SORT_CODE, largeAccount,
                    new BigDecimal("500.00"), new BigDecimal("500.00"));

            when(repository.findAccount(SORT_CODE, smallAccount))
                    .thenReturn(Optional.of(fromAccount));
            when(repository.findAccount(SORT_CODE, largeAccount))
                    .thenReturn(Optional.of(toAccount));

            TransferFundsRequest request = new TransferFundsRequest(
                    smallAccount, SORT_CODE, largeAccount, SORT_CODE,
                    new BigDecimal("100.00"));

            TransferFundsResult result = transferFunds.transfer(request);

            assertTrue(result.success());

            var inOrder = inOrder(repository);
            inOrder.verify(repository).findAccount(SORT_CODE, smallAccount);
            inOrder.verify(repository).updateAccount(
                    argThat(a -> a.getAccountNumber().equals(smallAccount)));
            inOrder.verify(repository).findAccount(SORT_CODE, largeAccount);
            inOrder.verify(repository).updateAccount(
                    argThat(a -> a.getAccountNumber().equals(largeAccount)));
        }

        @Test
        @DisplayName("TO account updated first when FROM > TO")
        void toUpdatedFirstWhenFromIsLarger() throws DataAccessException {
            String smallAccount = "00000001";
            String largeAccount = "00000002";

            AccountRecord fromAccount = createAccount(
                    SORT_CODE, largeAccount,
                    new BigDecimal("1000.00"), new BigDecimal("1000.00"));
            AccountRecord toAccount = createAccount(
                    SORT_CODE, smallAccount,
                    new BigDecimal("500.00"), new BigDecimal("500.00"));

            when(repository.findAccount(SORT_CODE, smallAccount))
                    .thenReturn(Optional.of(toAccount));
            when(repository.findAccount(SORT_CODE, largeAccount))
                    .thenReturn(Optional.of(fromAccount));

            TransferFundsRequest request = new TransferFundsRequest(
                    largeAccount, SORT_CODE, smallAccount, SORT_CODE,
                    new BigDecimal("100.00"));

            TransferFundsResult result = transferFunds.transfer(request);

            assertTrue(result.success());

            var inOrder = inOrder(repository);
            inOrder.verify(repository).findAccount(SORT_CODE, smallAccount);
            inOrder.verify(repository).updateAccount(
                    argThat(a -> a.getAccountNumber().equals(smallAccount)));
            inOrder.verify(repository).findAccount(SORT_CODE, largeAccount);
            inOrder.verify(repository).updateAccount(
                    argThat(a -> a.getAccountNumber().equals(largeAccount)));
        }
    }

    @Nested
    @DisplayName("No Overdraft Limit Check Tests")
    class NoOverdraftLimitCheck {

        @Test
        @DisplayName("Transfer succeeds even if FROM balance goes negative")
        void transferSucceedsWithNegativeBalance() throws DataAccessException {
            AccountRecord fromAccount = createAccount(
                    SORT_CODE, FROM_ACCOUNT,
                    new BigDecimal("50.00"), new BigDecimal("50.00"));
            AccountRecord toAccount = createAccount(
                    SORT_CODE, TO_ACCOUNT,
                    new BigDecimal("100.00"), new BigDecimal("100.00"));

            when(repository.findAccount(SORT_CODE, FROM_ACCOUNT))
                    .thenReturn(Optional.of(fromAccount));
            when(repository.findAccount(SORT_CODE, TO_ACCOUNT))
                    .thenReturn(Optional.of(toAccount));

            TransferFundsRequest request = new TransferFundsRequest(
                    FROM_ACCOUNT, SORT_CODE, TO_ACCOUNT, SORT_CODE,
                    new BigDecimal("200.00"));

            TransferFundsResult result = transferFunds.transfer(request);

            assertTrue(result.success());
            assertEquals(new BigDecimal("-150.00"),
                    result.fromAvailableBalance());
            assertEquals(new BigDecimal("-150.00"),
                    result.fromActualBalance());
            assertEquals(new BigDecimal("300.00"),
                    result.toAvailableBalance());
        }
    }

    @Nested
    @DisplayName("Request Validation Tests")
    class RequestValidation {

        @Test
        @DisplayName("Request rejects null fromAccountNumber")
        void requestRejectsNullFromAccount() {
            assertThrows(IllegalArgumentException.class,
                    () -> new TransferFundsRequest(
                            null, SORT_CODE, TO_ACCOUNT, SORT_CODE,
                            new BigDecimal("100.00")));
        }

        @Test
        @DisplayName("Request rejects blank fromAccountNumber")
        void requestRejectsBlankFromAccount() {
            assertThrows(IllegalArgumentException.class,
                    () -> new TransferFundsRequest(
                            "  ", SORT_CODE, TO_ACCOUNT, SORT_CODE,
                            new BigDecimal("100.00")));
        }

        @Test
        @DisplayName("Request rejects null amount")
        void requestRejectsNullAmount() {
            assertThrows(IllegalArgumentException.class,
                    () -> new TransferFundsRequest(
                            FROM_ACCOUNT, SORT_CODE, TO_ACCOUNT, SORT_CODE,
                            null));
        }

        @Test
        @DisplayName("Request rejects null toSortCode")
        void requestRejectsNullToSortCode() {
            assertThrows(IllegalArgumentException.class,
                    () -> new TransferFundsRequest(
                            FROM_ACCOUNT, SORT_CODE, TO_ACCOUNT, null,
                            new BigDecimal("100.00")));
        }
    }

    @Nested
    @DisplayName("Transfer Description Tests")
    class TransferDescription {

        @Test
        @DisplayName("Description is 40 chars: 26-char header + 6-char sort code + 8-char account")
        void descriptionFormat() {
            String desc = TransferFunds.buildTransferDescription(
                    "987654", "00002222");

            assertEquals(40, desc.length());
            assertEquals("TRANSFER", desc.substring(0, 8).trim());
            assertEquals("987654", desc.substring(26, 32));
            assertEquals("00002222", desc.substring(32, 40));
        }

        @Test
        @DisplayName("Description pads short sort code and account")
        void descriptionPadsShortValues() {
            String desc = TransferFunds.buildTransferDescription("123", "456");

            assertEquals(40, desc.length());
            assertTrue(desc.startsWith("TRANSFER"));
            assertEquals("123   ", desc.substring(26, 32));
            assertEquals("456     ", desc.substring(32, 40));
        }
    }

    @Nested
    @DisplayName("Result Factory Tests")
    class ResultFactory {

        @Test
        @DisplayName("Success result has correct values")
        void successResult() {
            TransferFundsResult result = TransferFundsResult.success(
                    new BigDecimal("100.00"), new BigDecimal("100.00"),
                    new BigDecimal("200.00"), new BigDecimal("200.00"));

            assertTrue(result.success());
            assertEquals(TransferFundsResult.FailCode.NONE, result.failCode());
            assertEquals(new BigDecimal("100.00"),
                    result.fromAvailableBalance());
            assertEquals(new BigDecimal("200.00"),
                    result.toAvailableBalance());
        }

        @Test
        @DisplayName("Failure result has zero balances")
        void failureResult() {
            TransferFundsResult result = TransferFundsResult.failure(
                    TransferFundsResult.FailCode.FROM_ACCOUNT_NOT_FOUND);

            assertFalse(result.success());
            assertEquals(
                    TransferFundsResult.FailCode.FROM_ACCOUNT_NOT_FOUND,
                    result.failCode());
            assertEquals(BigDecimal.ZERO, result.fromAvailableBalance());
            assertEquals(BigDecimal.ZERO, result.toAvailableBalance());
        }
    }

    @Nested
    @DisplayName("DataAccessException Tests")
    class DataAccessExceptionTests {

        @Test
        @DisplayName("isConnectionLost returns true for SQLCODE 923")
        void connectionLostForSqlcode923() {
            DataAccessException ex =
                    new DataAccessException("Connection lost", 923);
            assertTrue(ex.isConnectionLost());
            assertEquals(923, ex.getSqlCode());
        }

        @Test
        @DisplayName("isConnectionLost returns false for other SQLCODEs")
        void notConnectionLostForOtherCodes() {
            DataAccessException ex =
                    new DataAccessException("Other error", -904);
            assertFalse(ex.isConnectionLost());
        }
    }

    @Nested
    @DisplayName("TransferFundsException Tests")
    class TransferFundsExceptionTests {

        @Test
        @DisplayName("Exception preserves abend code")
        void exceptionPreservesAbendCode() {
            TransferFundsException ex =
                    new TransferFundsException("SAME", "test");
            assertEquals("SAME", ex.getAbendCode());
            assertEquals("test", ex.getMessage());
        }

        @Test
        @DisplayName("Exception preserves cause")
        void exceptionPreservesCause() {
            RuntimeException cause = new RuntimeException("root cause");
            TransferFundsException ex =
                    new TransferFundsException("HROL", "test", cause);
            assertEquals(cause, ex.getCause());
        }
    }

    @Nested
    @DisplayName("AccountRecord Tests")
    class AccountRecordTests {

        @Test
        @DisplayName("Default constructor creates empty record")
        void defaultConstructor() {
            AccountRecord record = new AccountRecord();
            assertNull(record.getEyeCatcher());
            assertNull(record.getAvailableBalance());
        }

        @Test
        @DisplayName("Full constructor sets all fields")
        void fullConstructor() {
            AccountRecord record = createAccount(
                    SORT_CODE, FROM_ACCOUNT,
                    new BigDecimal("1000.00"), new BigDecimal("1000.00"));

            assertEquals("ACCT", record.getEyeCatcher());
            assertEquals("0000000001", record.getCustomerNumber());
            assertEquals(SORT_CODE, record.getSortCode());
            assertEquals(FROM_ACCOUNT, record.getAccountNumber());
            assertEquals("SAVINGS", record.getAccountType());
            assertEquals(new BigDecimal("1.50"), record.getInterestRate());
            assertEquals("01.01.2023", record.getDateOpened());
            assertEquals(0, record.getOverdraftLimit());
            assertEquals("01.01.2023", record.getLastStatementDate());
            assertEquals("01.02.2023", record.getNextStatementDate());
            assertEquals(new BigDecimal("1000.00"),
                    record.getAvailableBalance());
            assertEquals(new BigDecimal("1000.00"),
                    record.getActualBalance());
        }

        @Test
        @DisplayName("Setters update fields correctly")
        void settersWork() {
            AccountRecord record = new AccountRecord();
            record.setEyeCatcher("ACCT");
            record.setCustomerNumber("123");
            record.setSortCode("111111");
            record.setAccountNumber("22222222");
            record.setAccountType("CHECKING");
            record.setInterestRate(new BigDecimal("2.00"));
            record.setDateOpened("02.02.2023");
            record.setOverdraftLimit(500);
            record.setLastStatementDate("03.03.2023");
            record.setNextStatementDate("04.04.2023");
            record.setAvailableBalance(new BigDecimal("999.99"));
            record.setActualBalance(new BigDecimal("888.88"));

            assertEquals("ACCT", record.getEyeCatcher());
            assertEquals("111111", record.getSortCode());
            assertEquals(500, record.getOverdraftLimit());
            assertEquals(new BigDecimal("999.99"),
                    record.getAvailableBalance());
        }
    }

    @Nested
    @DisplayName("ProcessedTransactionRecord Tests")
    class ProcessedTransactionRecordTests {

        @Test
        @DisplayName("Constants have correct COBOL-equivalent values")
        void constantsCorrect() {
            assertEquals("PRTR",
                    ProcessedTransactionRecord.EYECATCHER_VALUE);
            assertEquals("TFR",
                    ProcessedTransactionRecord.TYPE_TRANSFER);
            assertEquals("TRANSFER",
                    ProcessedTransactionRecord.DESCRIPTION_TRANSFER_PREFIX);
        }

        @Test
        @DisplayName("Record stores all fields")
        void recordStoresFields() {
            ProcessedTransactionRecord record =
                    new ProcessedTransactionRecord(
                            "PRTR", "987654", "00001111",
                            "29.05.2026", "143000", "000000000001",
                            "TFR", "TRANSFER  description     ",
                            new BigDecimal("100.00"));

            assertEquals("PRTR", record.eyeCatcher());
            assertEquals("987654", record.sortCode());
            assertEquals("00001111", record.accountNumber());
            assertEquals("TFR", record.type());
            assertEquals(new BigDecimal("100.00"), record.amount());
        }
    }
}
