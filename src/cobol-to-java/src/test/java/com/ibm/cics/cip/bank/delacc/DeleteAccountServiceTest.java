/*
 * Copyright IBM Corp. 2023
 *
 * Comprehensive tests for the Java 21 migration of DELACC.cbl.
 * Each test maps to a specific business logic path in the original COBOL.
 */
package com.ibm.cics.cip.bank.delacc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteAccountServiceTest {

    private static final String SORT_CODE = "987654";
    private static final String ACCOUNT_NUMBER = "00000042";
    private static final String CUSTOMER_NUMBER = "0000012345";

    @Mock
    private AccountDao accountDao;

    @Mock
    private ProcessedTransactionDao processedTransactionDao;

    private DeleteAccountService service;

    @BeforeEach
    void setUp() {
        service = new DeleteAccountService(
            accountDao, processedTransactionDao, SORT_CODE);
    }

    private AccountRecord createSampleAccountRecord() {
        return new AccountRecord(
            "ACCT",
            CUSTOMER_NUMBER,
            SORT_CODE,
            ACCOUNT_NUMBER,
            "SAVING",
            new BigDecimal("3.50"),
            LocalDate.of(2020, 1, 15),
            1000,
            LocalDate.of(2023, 6, 1),
            LocalDate.of(2023, 7, 1),
            new BigDecimal("5000.00"),
            new BigDecimal("4500.00")
        );
    }

    @Nested
    @DisplayName("Successful delete path (COBOL: SQLCODE=0 for read, delete, and PROCTRAN write)")
    class SuccessfulDelete {

        @Test
        @DisplayName("Returns success with account data when account exists and delete succeeds")
        void successfulDeleteReturnsAccountData() {
            AccountRecord record = createSampleAccountRecord();
            when(accountDao.findByAccountNumberAndSortCode(ACCOUNT_NUMBER, SORT_CODE))
                .thenReturn(Optional.of(record));
            when(accountDao.deleteByAccountNumberAndSortCode(ACCOUNT_NUMBER, SORT_CODE))
                .thenReturn(true);

            DeleteAccountResult result = service.deleteAccount(
                new DeleteAccountRequest(ACCOUNT_NUMBER));

            assertTrue(result.isDeleteSuccess());
            assertEquals(" ", result.getDeleteFailCode());
            assertEquals("ACCT", result.getEyeCatcher());
            assertEquals(CUSTOMER_NUMBER, result.getCustomerNumber());
            assertEquals(SORT_CODE, result.getSortCode());
            assertEquals(ACCOUNT_NUMBER, result.getAccountNumber());
            assertEquals("SAVING", result.getAccountType());
            assertEquals(new BigDecimal("3.50"), result.getInterestRate());
            assertEquals(LocalDate.of(2020, 1, 15), result.getOpened());
            assertEquals(1000, result.getOverdraftLimit());
            assertEquals(LocalDate.of(2023, 6, 1), result.getLastStatementDate());
            assertEquals(LocalDate.of(2023, 7, 1), result.getNextStatementDate());
            assertEquals(new BigDecimal("5000.00"), result.getAvailableBalance());
            assertEquals(new BigDecimal("4500.00"), result.getActualBalance());
        }

        @Test
        @DisplayName("Writes PROCTRAN record after successful delete")
        void writesProcessedTransactionOnSuccess() {
            AccountRecord record = createSampleAccountRecord();
            when(accountDao.findByAccountNumberAndSortCode(ACCOUNT_NUMBER, SORT_CODE))
                .thenReturn(Optional.of(record));
            when(accountDao.deleteByAccountNumberAndSortCode(ACCOUNT_NUMBER, SORT_CODE))
                .thenReturn(true);

            service.deleteAccount(new DeleteAccountRequest(ACCOUNT_NUMBER));

            ArgumentCaptor<String> sortCodeCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> accNumCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> descCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<BigDecimal> amountCaptor = ArgumentCaptor.forClass(BigDecimal.class);

            verify(processedTransactionDao).insertDeleteAccountTransaction(
                sortCodeCaptor.capture(),
                accNumCaptor.capture(),
                any(LocalDate.class),
                any(LocalTime.class),
                anyString(),
                typeCaptor.capture(),
                descCaptor.capture(),
                amountCaptor.capture());

            assertEquals(SORT_CODE, sortCodeCaptor.getValue());
            assertEquals(ACCOUNT_NUMBER, accNumCaptor.getValue());
            assertEquals("ODA", typeCaptor.getValue());
            assertEquals(new BigDecimal("4500.00"), amountCaptor.getValue());
        }

        @Test
        @DisplayName("PROCTRAN type is ODA (Branch Delete Account)")
        void transactionTypeIsBranchDeleteAccount() {
            AccountRecord record = createSampleAccountRecord();
            when(accountDao.findByAccountNumberAndSortCode(ACCOUNT_NUMBER, SORT_CODE))
                .thenReturn(Optional.of(record));
            when(accountDao.deleteByAccountNumberAndSortCode(ACCOUNT_NUMBER, SORT_CODE))
                .thenReturn(true);

            service.deleteAccount(new DeleteAccountRequest(ACCOUNT_NUMBER));

            verify(processedTransactionDao).insertDeleteAccountTransaction(
                anyString(), anyString(), any(), any(), anyString(),
                eq(DeleteAccountService.TRANSACTION_TYPE_BRANCH_DELETE),
                anyString(), any());
        }
    }

    @Nested
    @DisplayName("Account not found path (COBOL: SQLCODE=+100)")
    class AccountNotFound {

        @Test
        @DisplayName("Returns failure with code '1' when account not found")
        void returnsFailureCode1WhenNotFound() {
            when(accountDao.findByAccountNumberAndSortCode(ACCOUNT_NUMBER, SORT_CODE))
                .thenReturn(Optional.empty());

            DeleteAccountResult result = service.deleteAccount(
                new DeleteAccountRequest(ACCOUNT_NUMBER));

            assertFalse(result.isDeleteSuccess());
            assertEquals("1", result.getDeleteFailCode());
            assertEquals(SORT_CODE, result.getSortCode());
            assertEquals(ACCOUNT_NUMBER, result.getAccountNumber());
        }

        @Test
        @DisplayName("Does not attempt delete when account not found")
        void doesNotAttemptDeleteWhenNotFound() {
            when(accountDao.findByAccountNumberAndSortCode(ACCOUNT_NUMBER, SORT_CODE))
                .thenReturn(Optional.empty());

            service.deleteAccount(new DeleteAccountRequest(ACCOUNT_NUMBER));

            verify(accountDao, never())
                .deleteByAccountNumberAndSortCode(anyString(), anyString());
        }

        @Test
        @DisplayName("Does not write PROCTRAN when account not found")
        void doesNotWriteProctranWhenNotFound() {
            when(accountDao.findByAccountNumberAndSortCode(ACCOUNT_NUMBER, SORT_CODE))
                .thenReturn(Optional.empty());

            service.deleteAccount(new DeleteAccountRequest(ACCOUNT_NUMBER));

            verifyNoInteractions(processedTransactionDao);
        }
    }

    @Nested
    @DisplayName("Delete failure path (COBOL: SQLCODE != 0 on DELETE)")
    class DeleteFailure {

        @Test
        @DisplayName("Returns failure with code '3' when DB delete fails")
        void returnsFailureCode3WhenDeleteFails() {
            AccountRecord record = createSampleAccountRecord();
            when(accountDao.findByAccountNumberAndSortCode(ACCOUNT_NUMBER, SORT_CODE))
                .thenReturn(Optional.of(record));
            when(accountDao.deleteByAccountNumberAndSortCode(ACCOUNT_NUMBER, SORT_CODE))
                .thenReturn(false);

            DeleteAccountResult result = service.deleteAccount(
                new DeleteAccountRequest(ACCOUNT_NUMBER));

            assertFalse(result.isDeleteSuccess());
            assertEquals("3", result.getDeleteFailCode());
        }

        @Test
        @DisplayName("Does not write PROCTRAN when delete fails")
        void doesNotWriteProctranWhenDeleteFails() {
            AccountRecord record = createSampleAccountRecord();
            when(accountDao.findByAccountNumberAndSortCode(ACCOUNT_NUMBER, SORT_CODE))
                .thenReturn(Optional.of(record));
            when(accountDao.deleteByAccountNumberAndSortCode(ACCOUNT_NUMBER, SORT_CODE))
                .thenReturn(false);

            service.deleteAccount(new DeleteAccountRequest(ACCOUNT_NUMBER));

            verifyNoInteractions(processedTransactionDao);
        }

        @Test
        @DisplayName("Still returns account data even when delete fails")
        void returnsAccountDataEvenWhenDeleteFails() {
            AccountRecord record = createSampleAccountRecord();
            when(accountDao.findByAccountNumberAndSortCode(ACCOUNT_NUMBER, SORT_CODE))
                .thenReturn(Optional.of(record));
            when(accountDao.deleteByAccountNumberAndSortCode(ACCOUNT_NUMBER, SORT_CODE))
                .thenReturn(false);

            DeleteAccountResult result = service.deleteAccount(
                new DeleteAccountRequest(ACCOUNT_NUMBER));

            assertEquals("ACCT", result.getEyeCatcher());
            assertEquals(CUSTOMER_NUMBER, result.getCustomerNumber());
            assertEquals(ACCOUNT_NUMBER, result.getAccountNumber());
        }
    }

    @Nested
    @DisplayName("Abend paths (COBOL: EXEC CICS ABEND)")
    class AbendPaths {

        @Test
        @DisplayName("Throws HRAC abend when account read encounters SQL error")
        void throwsHracAbendOnReadSqlError() {
            when(accountDao.findByAccountNumberAndSortCode(ACCOUNT_NUMBER, SORT_CODE))
                .thenThrow(new RuntimeException("DB2 connection failed"));

            DeleteAccountAbendException ex = assertThrows(
                DeleteAccountAbendException.class,
                () -> service.deleteAccount(new DeleteAccountRequest(ACCOUNT_NUMBER)));

            assertEquals("HRAC", ex.getAbendCode());
            assertTrue(ex.getMessage().contains("RAD010"));
            assertTrue(ex.getMessage().contains(ACCOUNT_NUMBER));
        }

        @Test
        @DisplayName("Throws HRAC abend when delete encounters SQL error")
        void throwsHracAbendOnDeleteSqlError() {
            AccountRecord record = createSampleAccountRecord();
            when(accountDao.findByAccountNumberAndSortCode(ACCOUNT_NUMBER, SORT_CODE))
                .thenReturn(Optional.of(record));
            when(accountDao.deleteByAccountNumberAndSortCode(ACCOUNT_NUMBER, SORT_CODE))
                .thenThrow(new RuntimeException("DB2 delete failed"));

            DeleteAccountAbendException ex = assertThrows(
                DeleteAccountAbendException.class,
                () -> service.deleteAccount(new DeleteAccountRequest(ACCOUNT_NUMBER)));

            assertEquals("HRAC", ex.getAbendCode());
            assertTrue(ex.getMessage().contains("DADB010"));
        }

        @Test
        @DisplayName("Throws HWPT abend when PROCTRAN write fails")
        void throwsHwptAbendOnProctranWriteFailure() {
            AccountRecord record = createSampleAccountRecord();
            when(accountDao.findByAccountNumberAndSortCode(ACCOUNT_NUMBER, SORT_CODE))
                .thenReturn(Optional.of(record));
            when(accountDao.deleteByAccountNumberAndSortCode(ACCOUNT_NUMBER, SORT_CODE))
                .thenReturn(true);
            doThrow(new RuntimeException("PROCTRAN insert failed"))
                .when(processedTransactionDao)
                .insertDeleteAccountTransaction(
                    anyString(), anyString(), any(), any(),
                    anyString(), anyString(), anyString(), any());

            DeleteAccountAbendException ex = assertThrows(
                DeleteAccountAbendException.class,
                () -> service.deleteAccount(new DeleteAccountRequest(ACCOUNT_NUMBER)));

            assertEquals("HWPT", ex.getAbendCode());
            assertTrue(ex.getMessage().contains("WPD010"));
        }
    }

    @Nested
    @DisplayName("PROCTRAN description format (COBOL: PROC-TRAN-DESC-DELACC)")
    class ProctranDescription {

        @Test
        @DisplayName("Description matches COBOL PROC-TRAN-DESC-DELACC layout")
        void descriptionMatchesCobolLayout() {
            DeleteAccountResult result = new DeleteAccountResult();
            result.setCustomerNumber("0000012345");
            result.setAccountType("SAVING");
            result.setLastStatementDate(LocalDate.of(2023, 6, 1));
            result.setNextStatementDate(LocalDate.of(2023, 7, 1));

            String desc = service.buildDeleteDescription(result);

            assertEquals(40, desc.length());
            assertEquals("0000012345", desc.substring(0, 10));
            assertEquals("SAVING  ", desc.substring(10, 18));
            assertEquals("01062023", desc.substring(18, 26));
            assertEquals("01072023", desc.substring(26, 34));
            assertEquals("DELETE", desc.substring(34, 40));
            assertTrue(desc.endsWith("DELETE"));
        }

        @Test
        @DisplayName("Description pads customer number to 10 digits")
        void padsCustomerNumberToTenDigits() {
            DeleteAccountResult result = new DeleteAccountResult();
            result.setCustomerNumber("123");
            result.setAccountType("ISA");
            result.setLastStatementDate(LocalDate.of(2023, 1, 15));
            result.setNextStatementDate(LocalDate.of(2023, 2, 15));

            String desc = service.buildDeleteDescription(result);

            assertTrue(desc.startsWith("0000000123"));
        }

        @Test
        @DisplayName("Description pads account type to 8 characters")
        void padsAccountTypeToEightChars() {
            DeleteAccountResult result = new DeleteAccountResult();
            result.setCustomerNumber("0000012345");
            result.setAccountType("ISA");
            result.setLastStatementDate(LocalDate.of(2023, 1, 15));
            result.setNextStatementDate(LocalDate.of(2023, 2, 15));

            String desc = service.buildDeleteDescription(result);

            assertEquals("ISA     ", desc.substring(10, 18));
        }

        @Test
        @DisplayName("Description handles null dates with zeros")
        void handlesNullDatesWithZeros() {
            DeleteAccountResult result = new DeleteAccountResult();
            result.setCustomerNumber("0000012345");
            result.setAccountType("SAVING");
            result.setLastStatementDate(null);
            result.setNextStatementDate(null);

            String desc = service.buildDeleteDescription(result);

            assertTrue(desc.contains("00000000"));
        }
    }

    @Nested
    @DisplayName("Input validation")
    class InputValidation {

        @Test
        @DisplayName("Rejects null request")
        void rejectsNullRequest() {
            assertThrows(NullPointerException.class,
                () -> service.deleteAccount(null));
        }

        @Test
        @DisplayName("Rejects null account number")
        void rejectsNullAccountNumber() {
            assertThrows(NullPointerException.class,
                () -> new DeleteAccountRequest(null));
        }

        @Test
        @DisplayName("Rejects account number of wrong length")
        void rejectsWrongLengthAccountNumber() {
            assertThrows(IllegalArgumentException.class,
                () -> new DeleteAccountRequest("123"));
        }

        @Test
        @DisplayName("Accepts exactly 8-digit account number")
        void acceptsEightDigitAccountNumber() {
            DeleteAccountRequest request = new DeleteAccountRequest("12345678");
            assertEquals("12345678", request.accountNumber());
        }
    }

    @Nested
    @DisplayName("Constructor validation")
    class ConstructorValidation {

        @Test
        @DisplayName("Rejects null AccountDao")
        void rejectsNullAccountDao() {
            assertThrows(NullPointerException.class,
                () -> new DeleteAccountService(null, processedTransactionDao));
        }

        @Test
        @DisplayName("Rejects null ProcessedTransactionDao")
        void rejectsNullProcessedTransactionDao() {
            assertThrows(NullPointerException.class,
                () -> new DeleteAccountService(accountDao, null));
        }

        @Test
        @DisplayName("Default sort code is 987654")
        void defaultSortCodeIs987654() {
            DeleteAccountService defaultService =
                new DeleteAccountService(accountDao, processedTransactionDao);
            assertEquals("987654", defaultService.getSortCode());
        }

        @Test
        @DisplayName("Custom sort code is preserved")
        void customSortCodeIsPreserved() {
            DeleteAccountService customService = new DeleteAccountService(
                accountDao, processedTransactionDao, "123456");
            assertEquals("123456", customService.getSortCode());
        }
    }

    @Nested
    @DisplayName("DeleteAccountResult factory methods")
    class ResultFactoryMethods {

        @Test
        @DisplayName("notFound sets correct fields")
        void notFoundSetsCorrectFields() {
            DeleteAccountResult result =
                DeleteAccountResult.notFound(SORT_CODE, ACCOUNT_NUMBER);

            assertFalse(result.isDeleteSuccess());
            assertEquals("1", result.getDeleteFailCode());
            assertEquals(SORT_CODE, result.getSortCode());
            assertEquals(ACCOUNT_NUMBER, result.getAccountNumber());
            assertNull(result.getEyeCatcher());
        }

        @Test
        @DisplayName("fromAccountRecord copies all fields and sets success")
        void fromAccountRecordCopiesAllFields() {
            AccountRecord record = createSampleAccountRecord();
            DeleteAccountResult result =
                DeleteAccountResult.fromAccountRecord(record);

            assertTrue(result.isDeleteSuccess());
            assertEquals(" ", result.getDeleteFailCode());
            assertEquals("ACCT", result.getEyeCatcher());
            assertEquals(CUSTOMER_NUMBER, result.getCustomerNumber());
            assertEquals(SORT_CODE, result.getSortCode());
            assertEquals(ACCOUNT_NUMBER, result.getAccountNumber());
            assertEquals("SAVING", result.getAccountType());
            assertEquals(new BigDecimal("3.50"), result.getInterestRate());
            assertEquals(LocalDate.of(2020, 1, 15), result.getOpened());
            assertEquals(1000, result.getOverdraftLimit());
            assertEquals(new BigDecimal("5000.00"), result.getAvailableBalance());
            assertEquals(new BigDecimal("4500.00"), result.getActualBalance());
        }

        @Test
        @DisplayName("deleteFailed sets code '3' and preserves account data")
        void deleteFailedSetsCode3() {
            AccountRecord record = createSampleAccountRecord();
            DeleteAccountResult result =
                DeleteAccountResult.deleteFailed(record);

            assertFalse(result.isDeleteSuccess());
            assertEquals("3", result.getDeleteFailCode());
            assertEquals("ACCT", result.getEyeCatcher());
            assertEquals(CUSTOMER_NUMBER, result.getCustomerNumber());
        }
    }

    @Nested
    @DisplayName("AccountRecord validation")
    class AccountRecordValidation {

        @Test
        @DisplayName("Rejects null customer number")
        void rejectsNullCustomerNumber() {
            assertThrows(NullPointerException.class, () ->
                new AccountRecord("ACCT", null, SORT_CODE, ACCOUNT_NUMBER,
                    "SAVING", BigDecimal.ZERO, null, 0, null, null,
                    BigDecimal.ZERO, BigDecimal.ZERO));
        }

        @Test
        @DisplayName("Rejects null sort code")
        void rejectsNullSortCode() {
            assertThrows(NullPointerException.class, () ->
                new AccountRecord("ACCT", CUSTOMER_NUMBER, null, ACCOUNT_NUMBER,
                    "SAVING", BigDecimal.ZERO, null, 0, null, null,
                    BigDecimal.ZERO, BigDecimal.ZERO));
        }

        @Test
        @DisplayName("Rejects null account number")
        void rejectsNullAccountNumber() {
            assertThrows(NullPointerException.class, () ->
                new AccountRecord("ACCT", CUSTOMER_NUMBER, SORT_CODE, null,
                    "SAVING", BigDecimal.ZERO, null, 0, null, null,
                    BigDecimal.ZERO, BigDecimal.ZERO));
        }
    }

    @Nested
    @DisplayName("DeleteAccountAbendException")
    class AbendExceptionTests {

        @Test
        @DisplayName("Preserves abend code")
        void preservesAbendCode() {
            DeleteAccountAbendException ex =
                new DeleteAccountAbendException("HRAC", "test message");
            assertEquals("HRAC", ex.getAbendCode());
            assertEquals("test message", ex.getMessage());
        }

        @Test
        @DisplayName("Preserves cause")
        void preservesCause() {
            RuntimeException cause = new RuntimeException("root cause");
            DeleteAccountAbendException ex =
                new DeleteAccountAbendException("HWPT", "test", cause);
            assertEquals("HWPT", ex.getAbendCode());
            assertSame(cause, ex.getCause());
        }
    }

    @Nested
    @DisplayName("Edge cases for different account types")
    class AccountTypeEdgeCases {

        @Test
        @DisplayName("Handles CURRENT account type")
        void handlesCurrentAccountType() {
            AccountRecord record = new AccountRecord(
                "ACCT", CUSTOMER_NUMBER, SORT_CODE, ACCOUNT_NUMBER,
                "CURRENT", new BigDecimal("1.25"),
                LocalDate.of(2019, 3, 20), 5000,
                LocalDate.of(2023, 11, 30), LocalDate.of(2023, 12, 31),
                new BigDecimal("15000.00"), new BigDecimal("14500.00"));

            when(accountDao.findByAccountNumberAndSortCode(ACCOUNT_NUMBER, SORT_CODE))
                .thenReturn(Optional.of(record));
            when(accountDao.deleteByAccountNumberAndSortCode(ACCOUNT_NUMBER, SORT_CODE))
                .thenReturn(true);

            DeleteAccountResult result = service.deleteAccount(
                new DeleteAccountRequest(ACCOUNT_NUMBER));

            assertTrue(result.isDeleteSuccess());
            assertEquals("CURRENT", result.getAccountType());
            assertEquals(new BigDecimal("14500.00"), result.getActualBalance());
        }

        @Test
        @DisplayName("Handles ISA account type with zero balance")
        void handlesIsaAccountWithZeroBalance() {
            AccountRecord record = new AccountRecord(
                "ACCT", CUSTOMER_NUMBER, SORT_CODE, ACCOUNT_NUMBER,
                "ISA", new BigDecimal("2.00"),
                LocalDate.of(2021, 4, 1), 0,
                LocalDate.of(2023, 3, 31), LocalDate.of(2023, 4, 30),
                BigDecimal.ZERO, BigDecimal.ZERO);

            when(accountDao.findByAccountNumberAndSortCode(ACCOUNT_NUMBER, SORT_CODE))
                .thenReturn(Optional.of(record));
            when(accountDao.deleteByAccountNumberAndSortCode(ACCOUNT_NUMBER, SORT_CODE))
                .thenReturn(true);

            DeleteAccountResult result = service.deleteAccount(
                new DeleteAccountRequest(ACCOUNT_NUMBER));

            assertTrue(result.isDeleteSuccess());
            assertEquals("ISA", result.getAccountType());
            assertEquals(BigDecimal.ZERO, result.getActualBalance());
        }

        @Test
        @DisplayName("Handles negative balance (overdrawn account)")
        void handlesNegativeBalance() {
            AccountRecord record = new AccountRecord(
                "ACCT", CUSTOMER_NUMBER, SORT_CODE, ACCOUNT_NUMBER,
                "CURRENT", new BigDecimal("1.50"),
                LocalDate.of(2022, 7, 10), 2000,
                LocalDate.of(2023, 8, 31), LocalDate.of(2023, 9, 30),
                new BigDecimal("-500.00"), new BigDecimal("-500.00"));

            when(accountDao.findByAccountNumberAndSortCode(ACCOUNT_NUMBER, SORT_CODE))
                .thenReturn(Optional.of(record));
            when(accountDao.deleteByAccountNumberAndSortCode(ACCOUNT_NUMBER, SORT_CODE))
                .thenReturn(true);

            DeleteAccountResult result = service.deleteAccount(
                new DeleteAccountRequest(ACCOUNT_NUMBER));

            assertTrue(result.isDeleteSuccess());
            assertEquals(new BigDecimal("-500.00"), result.getActualBalance());
        }
    }
}
