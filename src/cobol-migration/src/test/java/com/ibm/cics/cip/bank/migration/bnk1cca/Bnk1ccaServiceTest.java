/*
 * Copyright IBM Corp. 2023
 */
package com.ibm.cics.cip.bank.migration.bnk1cca;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Bnk1ccaServiceTest {

    private Bnk1ccaService service;
    private StubAccountInquiryService stubService;

    @BeforeEach
    void setUp() {
        stubService = new StubAccountInquiryService();
        service = new Bnk1ccaService(stubService);
    }

    @Nested
    @DisplayName("processInput - first invocation (no commarea)")
    class FirstInvocation {

        @Test
        @DisplayName("should send empty map with erase on first entry")
        void firstEntry() {
            ScreenOutput output = service.processInput(AidKey.ENTER, null, null);
            assertEquals(ScreenAction.SEND_ERASE, output.screenAction());
            assertEquals("", output.message());
            assertEquals(10, output.accountLines().size());
            assertTrue(output.accountLines().stream().allMatch(String::isEmpty));
        }
    }

    @Nested
    @DisplayName("processInput - AID key handling")
    class AidKeyHandling {

        private final CommArea existingCommArea = CommArea.empty();

        @ParameterizedTest
        @EnumSource(value = AidKey.class, names = {"PA1", "PA2", "PA3"})
        @DisplayName("PA keys should result in no action")
        void paKeys(AidKey paKey) {
            ScreenOutput output = service.processInput(paKey, null, existingCommArea);
            assertEquals(ScreenAction.NO_ACTION, output.screenAction());
        }

        @Test
        @DisplayName("PF3 should return to main menu")
        void pf3Key() {
            ScreenOutput output = service.processInput(AidKey.PF3, null, existingCommArea);
            assertEquals(ScreenAction.RETURN_TO_MENU, output.screenAction());
        }

        @Test
        @DisplayName("PF12 should terminate session")
        void pf12Key() {
            ScreenOutput output = service.processInput(AidKey.PF12, null, existingCommArea);
            assertEquals(ScreenAction.TERMINATE_SESSION, output.screenAction());
            assertEquals(ScreenOutput.END_OF_SESSION_MESSAGE, output.message());
        }

        @Test
        @DisplayName("AID key should terminate session")
        void aidKey() {
            ScreenOutput output = service.processInput(AidKey.AID, null, existingCommArea);
            assertEquals(ScreenAction.TERMINATE_SESSION, output.screenAction());
            assertEquals("Session Ended", output.message());
        }

        @Test
        @DisplayName("CLEAR should erase screen")
        void clearKey() {
            ScreenOutput output = service.processInput(AidKey.CLEAR, null, existingCommArea);
            assertEquals(ScreenAction.CLEAR_SCREEN, output.screenAction());
        }
    }

    @Nested
    @DisplayName("editData - customer number validation")
    class EditData {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "abc", "12345abc", "!@#$"})
        @DisplayName("should reject non-numeric customer numbers")
        void invalidCustomerNumbers(String input) {
            String result = service.editData(input);
            assertNotNull(result);
            assertEquals("Please enter a customer number.", result);
        }

        @ParameterizedTest
        @ValueSource(strings = {"0000000001", "1234567890", "0", "12345"})
        @DisplayName("should accept numeric customer numbers")
        void validCustomerNumbers(String input) {
            assertNull(service.editData(input));
        }
    }

    @Nested
    @DisplayName("processMap - end-to-end map processing")
    class ProcessMap {

        @Test
        @DisplayName("should show validation error for non-numeric input")
        void nonNumericInput() {
            ScreenOutput output = service.processMap("abc");
            assertEquals(ScreenAction.SEND_DATAONLY_ALARM, output.screenAction());
            assertEquals("Please enter a customer number.", output.message());
            assertEquals(10, output.accountLines().size());
        }

        @Test
        @DisplayName("should show customer not found message")
        void customerNotFound() {
            stubService.setResult(new AccountInquiryResult(
                    0, "9999999999", false, ' ', false, Collections.emptyList()));

            ScreenOutput output = service.processMap("9999999999");
            assertEquals(ScreenAction.SEND_DATAONLY_ALARM, output.screenAction());
            assertEquals("Unable to find customer 9999999999", output.message());
        }

        @Test
        @DisplayName("should show no accounts message when customer exists but has no accounts")
        void noAccounts() {
            stubService.setResult(new AccountInquiryResult(
                    0, "0000000001", true, ' ', true, Collections.emptyList()));

            ScreenOutput output = service.processMap("0000000001");
            assertEquals(ScreenAction.SEND_DATAONLY_ALARM, output.screenAction());
            assertEquals("No accounts found for customer", output.message());
        }

        @Test
        @DisplayName("should show error message when inquiry fails for found customer")
        void inquiryError() {
            stubService.setResult(new AccountInquiryResult(
                    1, "0000000001", false, 'E', true, List.of(createSampleAccount())));

            ScreenOutput output = service.processMap("0000000001");
            assertEquals(ScreenAction.SEND_DATAONLY_ALARM, output.screenAction());
            assertEquals("Error accessing accounts for customer 0000000001.", output.message());
        }

        @Test
        @DisplayName("should display single account successfully")
        void singleAccount() {
            AccountDetail account = createSampleAccount();
            stubService.setResult(new AccountInquiryResult(
                    1, "0000000001", true, ' ', true, List.of(account)));

            ScreenOutput output = service.processMap("0000000001");
            assertEquals(ScreenAction.SEND_DATAONLY_ALARM, output.screenAction());
            assertEquals(" 1 accounts found", output.message());
            assertFalse(output.accountLines().get(0).isEmpty());
            assertTrue(output.accountLines().get(1).isEmpty());
        }

        @Test
        @DisplayName("should display multiple accounts up to maximum of 10")
        void multipleAccounts() {
            List<AccountDetail> accounts = new java.util.ArrayList<>();
            for (int i = 0; i < 12; i++) {
                accounts.add(createAccountWithNumber(10000000 + i));
            }
            stubService.setResult(new AccountInquiryResult(
                    12, "0000000001", true, ' ', true, accounts));

            ScreenOutput output = service.processMap("0000000001");
            assertEquals(ScreenAction.SEND_DATAONLY_ALARM, output.screenAction());
            assertEquals("12 accounts found", output.message());

            for (int i = 0; i < 10; i++) {
                assertFalse(output.accountLines().get(i).isEmpty(),
                        "Account line " + i + " should be populated");
            }
        }

        @Test
        @DisplayName("should propagate AccountInquiryException from service")
        void inquiryServiceThrows() {
            stubService.setException(new AccountInquiryException(
                    "GCD010 - LINK INQACCCU FAIL", 99, 0));

            assertThrows(AccountInquiryException.class, () -> service.processMap("0000000001"));
        }
    }

    @Nested
    @DisplayName("getCustomerData - account retrieval and formatting")
    class GetCustomerData {

        @Test
        @DisplayName("should format account count with leading space for single digit")
        void singleDigitCount() {
            AccountDetail account = createSampleAccount();
            stubService.setResult(new AccountInquiryResult(
                    3, "0000000001", true, ' ', true,
                    List.of(account, account, account)));

            ScreenOutput output = service.getCustomerData("0000000001");
            assertTrue(output.message().startsWith(" 3"));
        }

        @Test
        @DisplayName("should format account count without leading space for double digit")
        void doubleDigitCount() {
            List<AccountDetail> accounts = new java.util.ArrayList<>();
            for (int i = 0; i < 10; i++) {
                accounts.add(createAccountWithNumber(10000000 + i));
            }
            stubService.setResult(new AccountInquiryResult(
                    10, "0000000001", true, ' ', true, accounts));

            ScreenOutput output = service.getCustomerData("0000000001");
            assertTrue(output.message().startsWith("10"));
        }

        @Test
        @DisplayName("should clear all account lines when customer not found")
        void customerNotFoundClearsLines() {
            stubService.setResult(new AccountInquiryResult(
                    0, "9999999999", false, ' ', false, Collections.emptyList()));

            ScreenOutput output = service.getCustomerData("9999999999");
            assertEquals(10, output.accountLines().size());
            assertTrue(output.accountLines().stream().allMatch(String::isEmpty));
        }
    }

    @Nested
    @DisplayName("formatAccountLine - account display formatting")
    class FormatAccountLine {

        @Test
        @DisplayName("should format positive balances with + sign")
        void positiveBalances() {
            AccountDetail detail = new AccountDetail(
                    "ACCT", "0000000001", "987654", 12345678,
                    "SAVING  ", new BigDecimal("4.50"),
                    LocalDate.of(2020, 1, 15), 1000,
                    LocalDate.of(2024, 6, 1), LocalDate.of(2024, 7, 1),
                    new BigDecimal("5000.50"), new BigDecimal("4500.25"));

            String line = service.formatAccountLine(detail);
            assertTrue(line.contains("987654"));
            assertTrue(line.contains("12345678"));
            assertTrue(line.contains("SAVING  "));
            assertTrue(line.contains("+0000005000.50"));
            assertTrue(line.contains("+0000004500.25"));
        }

        @Test
        @DisplayName("should format negative balances with - sign")
        void negativeBalances() {
            AccountDetail detail = new AccountDetail(
                    "ACCT", "0000000001", "987654", 12345678,
                    "CURRENT ", new BigDecimal("2.00"),
                    LocalDate.of(2020, 1, 15), 500,
                    LocalDate.of(2024, 6, 1), LocalDate.of(2024, 7, 1),
                    new BigDecimal("-150.75"), new BigDecimal("-200.00"));

            String line = service.formatAccountLine(detail);
            assertTrue(line.contains("-0000000150.75"));
            assertTrue(line.contains("-0000000200.00"));
        }

        @Test
        @DisplayName("should format zero balance with + sign")
        void zeroBalance() {
            AccountDetail detail = new AccountDetail(
                    "ACCT", "0000000001", "987654", 12345678,
                    "SAVING  ", BigDecimal.ZERO,
                    LocalDate.of(2020, 1, 15), 0,
                    LocalDate.of(2024, 6, 1), LocalDate.of(2024, 7, 1),
                    BigDecimal.ZERO, BigDecimal.ZERO);

            String line = service.formatAccountLine(detail);
            assertTrue(line.contains("+0000000000.00"));
        }

        @Test
        @DisplayName("should format large balances correctly")
        void largeBalance() {
            AccountDetail detail = new AccountDetail(
                    "ACCT", "0000000001", "987654", 12345678,
                    "SAVING  ", new BigDecimal("3.50"),
                    LocalDate.of(2020, 1, 15), 0,
                    LocalDate.of(2024, 6, 1), LocalDate.of(2024, 7, 1),
                    new BigDecimal("9999999999.99"), new BigDecimal("1234567890.12"));

            String line = service.formatAccountLine(detail);
            assertTrue(line.contains("+9999999999.99"));
            assertTrue(line.contains("+1234567890.12"));
        }

        @Test
        @DisplayName("should produce correct column layout matching COBOL STRING output")
        void columnLayout() {
            AccountDetail detail = new AccountDetail(
                    "ACCT", "0000000001", "123456", 99887766,
                    "ISA     ", new BigDecimal("1.25"),
                    LocalDate.of(2021, 3, 10), 0,
                    LocalDate.of(2024, 1, 1), LocalDate.of(2024, 4, 1),
                    new BigDecimal("100.00"), new BigDecimal("100.00"));

            String line = service.formatAccountLine(detail);

            assertEquals("123456", line.substring(0, 6));
            assertEquals("      ", line.substring(6, 12));
            assertEquals("99887766", line.substring(12, 20));
            assertEquals("         ", line.substring(20, 29));
            assertEquals("ISA     ", line.substring(29, 37));
            assertEquals("       ", line.substring(37, 44));
            assertEquals("+", line.substring(44, 45));
            assertEquals("0000000100", line.substring(45, 55));
            assertEquals(".", line.substring(55, 56));
            assertEquals("00", line.substring(56, 58));
            assertEquals("  ", line.substring(58, 60));
            assertEquals("+", line.substring(60, 61));
            assertEquals("0000000100", line.substring(61, 71));
            assertEquals(".", line.substring(71, 72));
            assertEquals("00", line.substring(72, 74));
        }
    }

    @Nested
    @DisplayName("formatBalance - balance value formatting")
    class FormatBalance {

        @Test
        @DisplayName("should pad integer part to 10 digits")
        void paddedInteger() {
            assertEquals("0000000100.50", service.formatBalance(new BigDecimal("100.50")));
        }

        @Test
        @DisplayName("should pad decimal part to 2 digits")
        void paddedDecimal() {
            assertEquals("0000000001.00", service.formatBalance(new BigDecimal("1.00")));
        }

        @Test
        @DisplayName("should handle zero")
        void zeroValue() {
            assertEquals("0000000000.00", service.formatBalance(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("should use absolute value for negative amounts")
        void negativeValue() {
            assertEquals("0000000250.75", service.formatBalance(new BigDecimal("-250.75")));
        }

        @Test
        @DisplayName("should handle maximum representable value")
        void maxValue() {
            assertEquals("9999999999.99", service.formatBalance(new BigDecimal("9999999999.99")));
        }
    }

    @Nested
    @DisplayName("formatAccountCount - count display formatting")
    class FormatAccountCount {

        @Test
        @DisplayName("should prepend space for single digit")
        void singleDigit() {
            assertEquals(" 1", service.formatAccountCount(1));
            assertEquals(" 9", service.formatAccountCount(9));
        }

        @Test
        @DisplayName("should not prepend space for double digit")
        void doubleDigit() {
            assertEquals("10", service.formatAccountCount(10));
            assertEquals("20", service.formatAccountCount(20));
        }

        @Test
        @DisplayName("should format zero with leading space")
        void zero() {
            assertEquals(" 0", service.formatAccountCount(0));
        }
    }

    // --- Helper methods ---

    private AccountDetail createSampleAccount() {
        return new AccountDetail(
                "ACCT", "0000000001", "987654", 12345678,
                "SAVING  ", new BigDecimal("4.50"),
                LocalDate.of(2020, 1, 15), 1000,
                LocalDate.of(2024, 6, 1), LocalDate.of(2024, 7, 1),
                new BigDecimal("5000.50"), new BigDecimal("4500.25"));
    }

    private AccountDetail createAccountWithNumber(int accountNumber) {
        return new AccountDetail(
                "ACCT", "0000000001", "987654", accountNumber,
                "SAVING  ", new BigDecimal("4.50"),
                LocalDate.of(2020, 1, 15), 1000,
                LocalDate.of(2024, 6, 1), LocalDate.of(2024, 7, 1),
                new BigDecimal("5000.50"), new BigDecimal("4500.25"));
    }

    /**
     * Stub implementation of AccountInquiryService for testing.
     */
    static class StubAccountInquiryService implements AccountInquiryService {

        private AccountInquiryResult result;
        private AccountInquiryException exception;

        void setResult(AccountInquiryResult result) {
            this.result = result;
            this.exception = null;
        }

        void setException(AccountInquiryException exception) {
            this.exception = exception;
            this.result = null;
        }

        @Override
        public AccountInquiryResult inquireAccountsByCustomer(String customerNumber, int maxAccounts) {
            if (exception != null) {
                throw exception;
            }
            return result;
        }
    }
}
