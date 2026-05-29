/*
 * Copyright IBM Corp. 2023
 */

package com.ibm.cics.cip.bank.migrated.creacc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import com.ibm.cics.cip.bank.migrated.creacc.repository.AccountControlRepository;
import com.ibm.cics.cip.bank.migrated.creacc.repository.AccountRepository;
import com.ibm.cics.cip.bank.migrated.creacc.repository.ProcessedTransactionRepository;
import com.ibm.cics.cip.bank.migrated.creacc.service.AccountCountService;
import com.ibm.cics.cip.bank.migrated.creacc.service.CustomerInquiryService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateAccountProcessTest {

    private static final String SORT_CODE = "987654";
    private static final String CUSTOMER_NUMBER = "0000000001";
    private static final String ACCOUNT_TYPE = "SAVING";
    private static final BigDecimal INTEREST_RATE = new BigDecimal("1.50");
    private static final int OVERDRAFT_LIMIT = 1000;
    private static final BigDecimal AVAILABLE_BALANCE = new BigDecimal("500.00");
    private static final BigDecimal ACTUAL_BALANCE = new BigDecimal("500.00");
    private static final LocalDate TEST_DATE = LocalDate.of(2024, 3, 15);
    private static final LocalTime TEST_TIME = LocalTime.of(14, 30, 45);

    @Mock
    private CustomerInquiryService customerInquiryService;

    @Mock
    private AccountCountService accountCountService;

    @Mock
    private AccountControlRepository accountControlRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private ProcessedTransactionRepository processedTransactionRepository;

    private CreateAccountProcess process;

    @BeforeEach
    void setUp() {
        process = new CreateAccountProcess(
                customerInquiryService,
                accountCountService,
                accountControlRepository,
                accountRepository,
                processedTransactionRepository,
                SORT_CODE
        );
    }

    private CreateAccountRequest defaultRequest() {
        return new CreateAccountRequest(
                CUSTOMER_NUMBER, SORT_CODE, ACCOUNT_TYPE,
                INTEREST_RATE, OVERDRAFT_LIMIT,
                AVAILABLE_BALANCE, ACTUAL_BALANCE
        );
    }

    private void stubHappyPath(long lastAccountNumber) {
        when(customerInquiryService.customerExists(CUSTOMER_NUMBER))
                .thenReturn(true);
        when(accountCountService.countAccounts(CUSTOMER_NUMBER))
                .thenReturn(3);

        String accountLastKey = SORT_CODE
                + CreateAccountProcess.ACCOUNT_LAST_SUFFIX;
        String accountCountKey = SORT_CODE
                + CreateAccountProcess.ACCOUNT_COUNT_SUFFIX;

        when(accountControlRepository.getControlValue(accountLastKey))
                .thenReturn(lastAccountNumber);
        when(accountControlRepository.updateControlValue(
                eq(accountLastKey), eq(lastAccountNumber + 1)))
                .thenReturn(true);
        when(accountControlRepository.getControlValue(accountCountKey))
                .thenReturn(100L);
        when(accountControlRepository.updateControlValue(
                eq(accountCountKey), eq(101L)))
                .thenReturn(true);

        when(accountRepository.insertAccount(
                anyString(), anyString(), anyString(), anyString(),
                anyString(), any(), anyString(), anyInt(),
                anyString(), anyString(), any(), any()))
                .thenReturn(true);

        when(processedTransactionRepository.insertTransaction(
                anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn(true);
    }

    @Nested
    @DisplayName("Successful Account Creation")
    class SuccessTests {

        @Test
        @DisplayName("Creates account with all valid inputs")
        void successfulAccountCreation() {
            stubHappyPath(99L);
            CreateAccountResult result = process.execute(
                    defaultRequest(), TEST_DATE, TEST_TIME);

            assertTrue(result.success());
            assertEquals(" ", result.failCode());
            assertEquals("ACCT", result.eyecatcher());
            assertEquals(CUSTOMER_NUMBER, result.customerNumber());
            assertEquals(SORT_CODE, result.sortCode());
            assertEquals("00000100", result.accountNumber());
            assertEquals(ACCOUNT_TYPE, result.accountType());
            assertEquals(INTEREST_RATE, result.interestRate());
            assertEquals("15032024", result.opened());
            assertEquals(OVERDRAFT_LIMIT, result.overdraftLimit());
            assertEquals("15032024", result.lastStatementDate());
            assertEquals("14042024", result.nextStatementDate());
            assertEquals(AVAILABLE_BALANCE, result.availableBalance());
            assertEquals(ACTUAL_BALANCE, result.actualBalance());
        }

        @Test
        @DisplayName("Account number is derived from last 8 digits of counter")
        void accountNumberFromLast8Digits() {
            stubHappyPath(1234567890123455L);
            CreateAccountResult result = process.execute(
                    defaultRequest(), TEST_DATE, TEST_TIME);

            assertTrue(result.success());
            assertEquals("90123456", result.accountNumber());
        }

        @Test
        @DisplayName("Account created when customer has exactly 9 accounts")
        void allowsUpToNineExistingAccounts() {
            when(customerInquiryService.customerExists(CUSTOMER_NUMBER))
                    .thenReturn(true);
            when(accountCountService.countAccounts(CUSTOMER_NUMBER))
                    .thenReturn(9);

            String accountLastKey = SORT_CODE
                    + CreateAccountProcess.ACCOUNT_LAST_SUFFIX;
            String accountCountKey = SORT_CODE
                    + CreateAccountProcess.ACCOUNT_COUNT_SUFFIX;

            when(accountControlRepository.getControlValue(accountLastKey))
                    .thenReturn(50L);
            when(accountControlRepository.updateControlValue(
                    eq(accountLastKey), eq(51L))).thenReturn(true);
            when(accountControlRepository.getControlValue(accountCountKey))
                    .thenReturn(50L);
            when(accountControlRepository.updateControlValue(
                    eq(accountCountKey), eq(51L))).thenReturn(true);
            when(accountRepository.insertAccount(
                    anyString(), anyString(), anyString(), anyString(),
                    anyString(), any(), anyString(), anyInt(),
                    anyString(), anyString(), any(), any()))
                    .thenReturn(true);
            when(processedTransactionRepository.insertTransaction(
                    anyString(), anyString(), anyString(), anyString(),
                    anyString(), anyString(), anyString(), anyString(),
                    any())).thenReturn(true);

            CreateAccountResult result = process.execute(
                    defaultRequest(), TEST_DATE, TEST_TIME);

            assertTrue(result.success());
        }

        @Test
        @DisplayName("Inserts correct values into ACCOUNT table")
        void verifiesAccountInsertParameters() {
            stubHappyPath(99L);
            process.execute(defaultRequest(), TEST_DATE, TEST_TIME);

            verify(accountRepository).insertAccount(
                    "ACCT",
                    CUSTOMER_NUMBER,
                    SORT_CODE,
                    "00000100",
                    ACCOUNT_TYPE,
                    INTEREST_RATE,
                    "15.03.2024",
                    OVERDRAFT_LIMIT,
                    "15.03.2024",
                    "14.04.2024",
                    AVAILABLE_BALANCE,
                    ACTUAL_BALANCE
            );
        }

        @Test
        @DisplayName("Inserts correct PROCTRAN record")
        void verifiesProctranInsertParameters() {
            stubHappyPath(99L);
            process.execute(defaultRequest(), TEST_DATE, TEST_TIME);

            verify(processedTransactionRepository).insertTransaction(
                    eq("PRTR"),
                    eq(SORT_CODE),
                    eq("00000100"),
                    eq("15.03.2024"),
                    eq("143045"),
                    anyString(),
                    eq("OCA"),
                    anyString(),
                    eq(BigDecimal.ZERO)
            );
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "ISA", "MORTGAGE", "SAVING", "CURRENT", "LOAN"
        })
        @DisplayName("Accepts all valid account types")
        void acceptsValidAccountTypes(String type) {
            when(customerInquiryService.customerExists(CUSTOMER_NUMBER))
                    .thenReturn(true);
            when(accountCountService.countAccounts(CUSTOMER_NUMBER))
                    .thenReturn(0);

            String accountLastKey = SORT_CODE
                    + CreateAccountProcess.ACCOUNT_LAST_SUFFIX;
            String accountCountKey = SORT_CODE
                    + CreateAccountProcess.ACCOUNT_COUNT_SUFFIX;

            when(accountControlRepository.getControlValue(accountLastKey))
                    .thenReturn(0L);
            when(accountControlRepository.updateControlValue(
                    eq(accountLastKey), eq(1L))).thenReturn(true);
            when(accountControlRepository.getControlValue(accountCountKey))
                    .thenReturn(0L);
            when(accountControlRepository.updateControlValue(
                    eq(accountCountKey), eq(1L))).thenReturn(true);
            when(accountRepository.insertAccount(
                    anyString(), anyString(), anyString(), anyString(),
                    anyString(), any(), anyString(), anyInt(),
                    anyString(), anyString(), any(), any()))
                    .thenReturn(true);
            when(processedTransactionRepository.insertTransaction(
                    anyString(), anyString(), anyString(), anyString(),
                    anyString(), anyString(), anyString(), anyString(),
                    any())).thenReturn(true);

            CreateAccountRequest req = new CreateAccountRequest(
                    CUSTOMER_NUMBER, SORT_CODE, type,
                    INTEREST_RATE, OVERDRAFT_LIMIT,
                    AVAILABLE_BALANCE, ACTUAL_BALANCE);

            CreateAccountResult result = process.execute(
                    req, TEST_DATE, TEST_TIME);

            assertTrue(result.success());
        }
    }

    @Nested
    @DisplayName("Customer Validation Failures")
    class CustomerValidationTests {

        @Test
        @DisplayName("Fails with code 1 when customer does not exist")
        void failsWhenCustomerNotFound() {
            when(customerInquiryService.customerExists(CUSTOMER_NUMBER))
                    .thenReturn(false);

            CreateAccountResult result = process.execute(
                    defaultRequest(), TEST_DATE, TEST_TIME);

            assertFalse(result.success());
            assertEquals(FailCode.CUSTOMER_NOT_FOUND, result.failCode());
        }

        @Test
        @DisplayName("Does not proceed to account count when customer fails")
        void doesNotCountAccountsOnCustomerFailure() {
            when(customerInquiryService.customerExists(CUSTOMER_NUMBER))
                    .thenReturn(false);

            process.execute(defaultRequest(), TEST_DATE, TEST_TIME);

            verify(accountCountService, never())
                    .countAccounts(anyString());
        }
    }

    @Nested
    @DisplayName("Account Count Failures")
    class AccountCountTests {

        @Test
        @DisplayName("Fails with code 9 when account count returns error")
        void failsWhenAccountCountErrors() {
            when(customerInquiryService.customerExists(CUSTOMER_NUMBER))
                    .thenReturn(true);
            when(accountCountService.countAccounts(CUSTOMER_NUMBER))
                    .thenReturn(-1);

            CreateAccountResult result = process.execute(
                    defaultRequest(), TEST_DATE, TEST_TIME);

            assertFalse(result.success());
            assertEquals(FailCode.ACCOUNT_COUNT_ERROR, result.failCode());
        }

        @Test
        @DisplayName("Fails with code 8 when customer has more than 9 accounts")
        void failsWhenTooManyAccounts() {
            when(customerInquiryService.customerExists(CUSTOMER_NUMBER))
                    .thenReturn(true);
            when(accountCountService.countAccounts(CUSTOMER_NUMBER))
                    .thenReturn(10);

            CreateAccountResult result = process.execute(
                    defaultRequest(), TEST_DATE, TEST_TIME);

            assertFalse(result.success());
            assertEquals(FailCode.MAX_ACCOUNTS_EXCEEDED, result.failCode());
        }

        @Test
        @DisplayName("Does not attempt DB access when count fails")
        void doesNotAccessDbOnCountFailure() {
            when(customerInquiryService.customerExists(CUSTOMER_NUMBER))
                    .thenReturn(true);
            when(accountCountService.countAccounts(CUSTOMER_NUMBER))
                    .thenReturn(-1);

            process.execute(defaultRequest(), TEST_DATE, TEST_TIME);

            verify(accountControlRepository, never())
                    .getControlValue(anyString());
        }
    }

    @Nested
    @DisplayName("Account Type Validation")
    class AccountTypeTests {

        @ParameterizedTest
        @ValueSource(strings = {"INVALID", "CHECKING", "SAVI", "MOR", ""})
        @DisplayName("Fails with code A for invalid account types")
        void failsForInvalidAccountTypes(String invalidType) {
            when(customerInquiryService.customerExists(CUSTOMER_NUMBER))
                    .thenReturn(true);
            when(accountCountService.countAccounts(CUSTOMER_NUMBER))
                    .thenReturn(0);

            CreateAccountRequest req = new CreateAccountRequest(
                    CUSTOMER_NUMBER, SORT_CODE, invalidType,
                    INTEREST_RATE, OVERDRAFT_LIMIT,
                    AVAILABLE_BALANCE, ACTUAL_BALANCE);

            CreateAccountResult result = process.execute(
                    req, TEST_DATE, TEST_TIME);

            assertFalse(result.success());
            assertEquals(FailCode.INVALID_ACCOUNT_TYPE, result.failCode());
        }
    }

    @Nested
    @DisplayName("Control Table / Named Counter Failures")
    class ControlTableTests {

        @Test
        @DisplayName("Fails with code 3 when ACCOUNT-LAST read fails")
        void failsWhenAccountLastReadFails() {
            when(customerInquiryService.customerExists(CUSTOMER_NUMBER))
                    .thenReturn(true);
            when(accountCountService.countAccounts(CUSTOMER_NUMBER))
                    .thenReturn(0);

            String accountLastKey = SORT_CODE
                    + CreateAccountProcess.ACCOUNT_LAST_SUFFIX;
            when(accountControlRepository.getControlValue(accountLastKey))
                    .thenReturn(-1L);

            CreateAccountResult result = process.execute(
                    defaultRequest(), TEST_DATE, TEST_TIME);

            assertFalse(result.success());
            assertEquals(FailCode.ENQ_FAILED, result.failCode());
        }

        @Test
        @DisplayName("Fails with code 3 when ACCOUNT-LAST update fails")
        void failsWhenAccountLastUpdateFails() {
            when(customerInquiryService.customerExists(CUSTOMER_NUMBER))
                    .thenReturn(true);
            when(accountCountService.countAccounts(CUSTOMER_NUMBER))
                    .thenReturn(0);

            String accountLastKey = SORT_CODE
                    + CreateAccountProcess.ACCOUNT_LAST_SUFFIX;
            when(accountControlRepository.getControlValue(accountLastKey))
                    .thenReturn(50L);
            when(accountControlRepository.updateControlValue(
                    eq(accountLastKey), eq(51L))).thenReturn(false);

            CreateAccountResult result = process.execute(
                    defaultRequest(), TEST_DATE, TEST_TIME);

            assertFalse(result.success());
            assertEquals(FailCode.ENQ_FAILED, result.failCode());
        }

        @Test
        @DisplayName("Fails with code 3 when ACCOUNT-COUNT read fails")
        void failsWhenAccountCountReadFails() {
            when(customerInquiryService.customerExists(CUSTOMER_NUMBER))
                    .thenReturn(true);
            when(accountCountService.countAccounts(CUSTOMER_NUMBER))
                    .thenReturn(0);

            String accountLastKey = SORT_CODE
                    + CreateAccountProcess.ACCOUNT_LAST_SUFFIX;
            String accountCountKey = SORT_CODE
                    + CreateAccountProcess.ACCOUNT_COUNT_SUFFIX;

            when(accountControlRepository.getControlValue(accountLastKey))
                    .thenReturn(50L);
            when(accountControlRepository.updateControlValue(
                    eq(accountLastKey), eq(51L))).thenReturn(true);
            when(accountControlRepository.getControlValue(accountCountKey))
                    .thenReturn(-1L);

            CreateAccountResult result = process.execute(
                    defaultRequest(), TEST_DATE, TEST_TIME);

            assertFalse(result.success());
            assertEquals(FailCode.ENQ_FAILED, result.failCode());
        }

        @Test
        @DisplayName("Fails with code 3 when ACCOUNT-COUNT update fails")
        void failsWhenAccountCountUpdateFails() {
            when(customerInquiryService.customerExists(CUSTOMER_NUMBER))
                    .thenReturn(true);
            when(accountCountService.countAccounts(CUSTOMER_NUMBER))
                    .thenReturn(0);

            String accountLastKey = SORT_CODE
                    + CreateAccountProcess.ACCOUNT_LAST_SUFFIX;
            String accountCountKey = SORT_CODE
                    + CreateAccountProcess.ACCOUNT_COUNT_SUFFIX;

            when(accountControlRepository.getControlValue(accountLastKey))
                    .thenReturn(50L);
            when(accountControlRepository.updateControlValue(
                    eq(accountLastKey), eq(51L))).thenReturn(true);
            when(accountControlRepository.getControlValue(accountCountKey))
                    .thenReturn(100L);
            when(accountControlRepository.updateControlValue(
                    eq(accountCountKey), eq(101L))).thenReturn(false);

            CreateAccountResult result = process.execute(
                    defaultRequest(), TEST_DATE, TEST_TIME);

            assertFalse(result.success());
            assertEquals(FailCode.ENQ_FAILED, result.failCode());
        }
    }

    @Nested
    @DisplayName("Account Insert Failures")
    class AccountInsertTests {

        @Test
        @DisplayName("Fails with code 7 when DB2 account insert fails")
        void failsWhenAccountInsertFails() {
            when(customerInquiryService.customerExists(CUSTOMER_NUMBER))
                    .thenReturn(true);
            when(accountCountService.countAccounts(CUSTOMER_NUMBER))
                    .thenReturn(0);

            String accountLastKey = SORT_CODE
                    + CreateAccountProcess.ACCOUNT_LAST_SUFFIX;
            String accountCountKey = SORT_CODE
                    + CreateAccountProcess.ACCOUNT_COUNT_SUFFIX;

            when(accountControlRepository.getControlValue(accountLastKey))
                    .thenReturn(50L);
            when(accountControlRepository.updateControlValue(
                    eq(accountLastKey), eq(51L))).thenReturn(true);
            when(accountControlRepository.getControlValue(accountCountKey))
                    .thenReturn(100L);
            when(accountControlRepository.updateControlValue(
                    eq(accountCountKey), eq(101L))).thenReturn(true);

            when(accountRepository.insertAccount(
                    anyString(), anyString(), anyString(), anyString(),
                    anyString(), any(), anyString(), anyInt(),
                    anyString(), anyString(), any(), any()))
                    .thenReturn(false);

            CreateAccountResult result = process.execute(
                    defaultRequest(), TEST_DATE, TEST_TIME);

            assertFalse(result.success());
            assertEquals(FailCode.ACCOUNT_INSERT_FAILED, result.failCode());
        }

        @Test
        @DisplayName("Does not write PROCTRAN when account insert fails")
        void doesNotWriteProctranOnInsertFailure() {
            when(customerInquiryService.customerExists(CUSTOMER_NUMBER))
                    .thenReturn(true);
            when(accountCountService.countAccounts(CUSTOMER_NUMBER))
                    .thenReturn(0);

            String accountLastKey = SORT_CODE
                    + CreateAccountProcess.ACCOUNT_LAST_SUFFIX;
            String accountCountKey = SORT_CODE
                    + CreateAccountProcess.ACCOUNT_COUNT_SUFFIX;

            when(accountControlRepository.getControlValue(accountLastKey))
                    .thenReturn(50L);
            when(accountControlRepository.updateControlValue(
                    eq(accountLastKey), eq(51L))).thenReturn(true);
            when(accountControlRepository.getControlValue(accountCountKey))
                    .thenReturn(100L);
            when(accountControlRepository.updateControlValue(
                    eq(accountCountKey), eq(101L))).thenReturn(true);

            when(accountRepository.insertAccount(
                    anyString(), anyString(), anyString(), anyString(),
                    anyString(), any(), anyString(), anyInt(),
                    anyString(), anyString(), any(), any()))
                    .thenReturn(false);

            process.execute(defaultRequest(), TEST_DATE, TEST_TIME);

            verify(processedTransactionRepository, never())
                    .insertTransaction(
                            anyString(), anyString(), anyString(),
                            anyString(), anyString(), anyString(),
                            anyString(), anyString(), any());
        }
    }

    @Nested
    @DisplayName("Date Calculations")
    class DateCalculationTests {

        @Test
        @DisplayName("Next statement date is 30 days after opening")
        void nextStatementDateIs30DaysAfter() {
            stubHappyPath(0L);

            // March 15, 2024 + 30 = April 14, 2024
            CreateAccountResult result = process.execute(
                    defaultRequest(), TEST_DATE, TEST_TIME);

            assertEquals("14042024", result.nextStatementDate());
        }

        @Test
        @DisplayName("Handles year boundary correctly")
        void handlesYearBoundary() {
            stubHappyPath(0L);

            LocalDate dec15 = LocalDate.of(2024, 12, 15);
            CreateAccountResult result = process.execute(
                    defaultRequest(), dec15, TEST_TIME);

            // Dec 15 + 30 = Jan 14, 2025
            assertEquals("14012025", result.nextStatementDate());
        }

        @Test
        @DisplayName("Handles February 28 correctly in non-leap year")
        void handlesFebruary28NonLeapYear() {
            stubHappyPath(0L);

            LocalDate feb28 = LocalDate.of(2023, 2, 28);
            CreateAccountResult result = process.execute(
                    defaultRequest(), feb28, TEST_TIME);

            // Feb 28 + 30 = Mar 30
            assertEquals("30032023", result.nextStatementDate());
        }

        @Test
        @DisplayName("Handles leap day correctly")
        void handlesLeapDay() {
            stubHappyPath(0L);

            LocalDate feb29 = LocalDate.of(2024, 2, 29);
            CreateAccountResult result = process.execute(
                    defaultRequest(), feb29, TEST_TIME);

            // Feb 29 + 30 = Mar 30
            assertEquals("30032024", result.nextStatementDate());
        }

        @Test
        @DisplayName("Opened date matches today")
        void openedDateMatchesToday() {
            stubHappyPath(0L);

            CreateAccountResult result = process.execute(
                    defaultRequest(), TEST_DATE, TEST_TIME);

            assertEquals("15032024", result.opened());
        }

        @Test
        @DisplayName("Last statement date matches today")
        void lastStatementDateMatchesToday() {
            stubHappyPath(0L);

            CreateAccountResult result = process.execute(
                    defaultRequest(), TEST_DATE, TEST_TIME);

            assertEquals("15032024", result.lastStatementDate());
        }
    }

    @Nested
    @DisplayName("Account Number Formatting")
    class AccountNumberFormattingTests {

        @Test
        @DisplayName("Zero-pads small account numbers to 8 digits")
        void zeroPadsSmallNumbers() {
            assertEquals("00000001",
                    CreateAccountProcess.formatAccountNumber(1));
        }

        @Test
        @DisplayName("Takes last 8 digits for large numbers")
        void takesLast8DigitsForLargeNumbers() {
            assertEquals("23456789",
                    CreateAccountProcess.formatAccountNumber(
                            1234567823456789L));
        }

        @Test
        @DisplayName("Handles exactly 8-digit numbers")
        void handlesExactly8Digits() {
            assertEquals("12345678",
                    CreateAccountProcess.formatAccountNumber(12345678));
        }

        @Test
        @DisplayName("Handles zero")
        void handlesZero() {
            assertEquals("00000000",
                    CreateAccountProcess.formatAccountNumber(0));
        }
    }

    @Nested
    @DisplayName("PROCTRAN Description Building")
    class ProctranDescriptionTests {

        @Test
        @DisplayName("Description is exactly 40 characters")
        void descriptionIs40Characters() {
            String desc = CreateAccountProcess.buildProctranDescription(
                    "0000000001", "SAVING  ", "15032024", "14042024");
            assertEquals(40, desc.length());
        }

        @Test
        @DisplayName("Description contains customer number in positions 1-10")
        void descriptionContainsCustomerNumber() {
            String desc = CreateAccountProcess.buildProctranDescription(
                    "0000000001", "SAVING", "15032024", "14042024");
            assertEquals("0000000001", desc.substring(0, 10));
        }

        @Test
        @DisplayName("Description contains account type in positions 11-18")
        void descriptionContainsAccountType() {
            String desc = CreateAccountProcess.buildProctranDescription(
                    "0000000001", "SAVING", "15032024", "14042024");
            assertEquals("SAVING  ", desc.substring(10, 18));
        }

        @Test
        @DisplayName("Description contains last stmt date in positions 19-26")
        void descriptionContainsLastStmtDate() {
            String desc = CreateAccountProcess.buildProctranDescription(
                    "0000000001", "SAVING", "15032024", "14042024");
            assertEquals("15032024", desc.substring(18, 26));
        }

        @Test
        @DisplayName("Description contains next stmt date in positions 27-34")
        void descriptionContainsNextStmtDate() {
            String desc = CreateAccountProcess.buildProctranDescription(
                    "0000000001", "SAVING", "15032024", "14042024");
            assertEquals("14042024", desc.substring(26, 34));
        }

        @Test
        @DisplayName("Description has trailing spaces in positions 35-40")
        void descriptionHasTrailingSpaces() {
            String desc = CreateAccountProcess.buildProctranDescription(
                    "0000000001", "SAVING", "15032024", "14042024");
            assertEquals("      ", desc.substring(34, 40));
        }

        @Test
        @DisplayName("Pads short account type to 8 characters")
        void padsShortAccountType() {
            String desc = CreateAccountProcess.buildProctranDescription(
                    "0000000001", "ISA", "15032024", "14042024");
            assertEquals("ISA     ", desc.substring(10, 18));
        }

        @Test
        @DisplayName("Truncates long account type to 8 characters")
        void truncatesLongAccountType() {
            String desc = CreateAccountProcess.buildProctranDescription(
                    "0000000001", "MORTGAGES", "15032024", "14042024");
            assertEquals("MORTGAGE", desc.substring(10, 18));
        }
    }

    @Nested
    @DisplayName("Default Sort Code")
    class DefaultSortCodeTests {

        @Test
        @DisplayName("Uses default sort code 987654 from SORTCODE copybook")
        void usesDefaultSortCode() {
            CreateAccountProcess defaultProcess = new CreateAccountProcess(
                    customerInquiryService,
                    accountCountService,
                    accountControlRepository,
                    accountRepository,
                    processedTransactionRepository
            );

            when(customerInquiryService.customerExists(CUSTOMER_NUMBER))
                    .thenReturn(true);
            when(accountCountService.countAccounts(CUSTOMER_NUMBER))
                    .thenReturn(0);

            String accountLastKey = "987654"
                    + CreateAccountProcess.ACCOUNT_LAST_SUFFIX;
            String accountCountKey = "987654"
                    + CreateAccountProcess.ACCOUNT_COUNT_SUFFIX;

            when(accountControlRepository.getControlValue(accountLastKey))
                    .thenReturn(0L);
            when(accountControlRepository.updateControlValue(
                    eq(accountLastKey), eq(1L))).thenReturn(true);
            when(accountControlRepository.getControlValue(accountCountKey))
                    .thenReturn(0L);
            when(accountControlRepository.updateControlValue(
                    eq(accountCountKey), eq(1L))).thenReturn(true);
            when(accountRepository.insertAccount(
                    anyString(), anyString(), anyString(), anyString(),
                    anyString(), any(), anyString(), anyInt(),
                    anyString(), anyString(), any(), any()))
                    .thenReturn(true);
            when(processedTransactionRepository.insertTransaction(
                    anyString(), anyString(), anyString(), anyString(),
                    anyString(), anyString(), anyString(), anyString(),
                    any())).thenReturn(true);

            CreateAccountResult result = defaultProcess.execute(
                    defaultRequest(), TEST_DATE, TEST_TIME);

            assertTrue(result.success());
            assertEquals("987654", result.sortCode());
        }
    }

    @Nested
    @DisplayName("End-to-End Flow Verification")
    class EndToEndTests {

        @Test
        @DisplayName("Calls services in correct order")
        void verifiesCallOrder() {
            stubHappyPath(0L);
            process.execute(defaultRequest(), TEST_DATE, TEST_TIME);

            var inOrder = org.mockito.Mockito.inOrder(
                    customerInquiryService,
                    accountCountService,
                    accountControlRepository,
                    accountRepository,
                    processedTransactionRepository
            );

            inOrder.verify(customerInquiryService)
                    .customerExists(CUSTOMER_NUMBER);
            inOrder.verify(accountCountService)
                    .countAccounts(CUSTOMER_NUMBER);
            inOrder.verify(accountControlRepository)
                    .getControlValue(SORT_CODE
                            + CreateAccountProcess.ACCOUNT_LAST_SUFFIX);
            inOrder.verify(accountControlRepository)
                    .updateControlValue(anyString(), anyLong());
            inOrder.verify(accountControlRepository)
                    .getControlValue(SORT_CODE
                            + CreateAccountProcess.ACCOUNT_COUNT_SUFFIX);
            inOrder.verify(accountControlRepository)
                    .updateControlValue(anyString(), anyLong());
            inOrder.verify(accountRepository)
                    .insertAccount(
                            anyString(), anyString(), anyString(),
                            anyString(), anyString(), any(), anyString(),
                            anyInt(), anyString(), anyString(), any(),
                            any());
            inOrder.verify(processedTransactionRepository)
                    .insertTransaction(
                            anyString(), anyString(), anyString(),
                            anyString(), anyString(), anyString(),
                            anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Returns correct result with no-arg execute")
        void noArgExecuteReturnsResult() {
            when(customerInquiryService.customerExists(CUSTOMER_NUMBER))
                    .thenReturn(true);
            when(accountCountService.countAccounts(CUSTOMER_NUMBER))
                    .thenReturn(0);

            String accountLastKey = SORT_CODE
                    + CreateAccountProcess.ACCOUNT_LAST_SUFFIX;
            String accountCountKey = SORT_CODE
                    + CreateAccountProcess.ACCOUNT_COUNT_SUFFIX;

            when(accountControlRepository.getControlValue(accountLastKey))
                    .thenReturn(0L);
            when(accountControlRepository.updateControlValue(
                    eq(accountLastKey), eq(1L))).thenReturn(true);
            when(accountControlRepository.getControlValue(accountCountKey))
                    .thenReturn(0L);
            when(accountControlRepository.updateControlValue(
                    eq(accountCountKey), eq(1L))).thenReturn(true);
            when(accountRepository.insertAccount(
                    anyString(), anyString(), anyString(), anyString(),
                    anyString(), any(), anyString(), anyInt(),
                    anyString(), anyString(), any(), any()))
                    .thenReturn(true);
            when(processedTransactionRepository.insertTransaction(
                    anyString(), anyString(), anyString(), anyString(),
                    anyString(), anyString(), anyString(), anyString(),
                    any())).thenReturn(true);

            CreateAccountResult result = process.execute(defaultRequest());

            assertTrue(result.success());
            assertNotNull(result.opened());
            assertNotNull(result.lastStatementDate());
            assertNotNull(result.nextStatementDate());
        }
    }

    @Nested
    @DisplayName("Failure Result Properties")
    class FailureResultTests {

        @Test
        @DisplayName("Failure result preserves request data")
        void failurePreservesRequestData() {
            CreateAccountResult result = CreateAccountResult.failure(
                    FailCode.CUSTOMER_NOT_FOUND, defaultRequest());

            assertFalse(result.success());
            assertEquals(FailCode.CUSTOMER_NOT_FOUND, result.failCode());
            assertEquals(CUSTOMER_NUMBER, result.customerNumber());
            assertEquals(SORT_CODE, result.sortCode());
            assertNull(result.accountNumber());
            assertNull(result.eyecatcher());
        }

        @Test
        @DisplayName("Simple failure result has null fields")
        void simpleFailureHasNullFields() {
            CreateAccountResult result = CreateAccountResult.failure(
                    FailCode.ENQ_FAILED);

            assertFalse(result.success());
            assertEquals(FailCode.ENQ_FAILED, result.failCode());
            assertNull(result.customerNumber());
            assertNull(result.sortCode());
            assertNull(result.accountNumber());
        }
    }
}
