/*
 * Copyright IBM Corp. 2023
 */
package com.ibm.cics.cip.bank.migrated.bnk1dac;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisplayAccountControllerTest {

    @Mock
    private AccountService accountService;

    private DisplayAccountController controller;
    private ScreenField screenField;

    @BeforeEach
    void setUp() {
        controller = new DisplayAccountController(accountService);
        screenField = new ScreenField();
    }

    private AccountInquiryData buildFoundAccount() {
        AccountInquiryData data = new AccountInquiryData();
        data.setEye("ACCT");
        data.setCustomerNumber("0000012345");
        data.setSortCode("987654");
        data.setAccountNumber(12345678);
        data.setAccountType("SAVINGS");
        data.setInterestRate(new BigDecimal("4.50"));
        data.setOpened(15062020);
        data.setOverdraft(500);
        data.setLastStatementDate(1052024);
        data.setNextStatementDate(1062024);
        data.setAvailableBalance(new BigDecimal("1500.75"));
        data.setActualBalance(new BigDecimal("2000.00"));
        data.setSuccess('Y');
        return data;
    }

    private AccountInquiryData buildNotFoundAccount() {
        AccountInquiryData data = new AccountInquiryData();
        data.setAccountType("");
        data.setInterestRate(BigDecimal.ZERO);
        data.setSuccess('N');
        return data;
    }

    private CommArea buildPopulatedCommArea() {
        CommArea comm = new CommArea();
        comm.setEye("ACCT");
        comm.setSortCode("987654");
        comm.setAccountNumber(12345678);
        comm.setCustomerNumber("0000012345");
        return comm;
    }

    // ===================================================================
    // PREMIERE SECTION — processRequest
    // ===================================================================

    @Nested
    @DisplayName("PREMIERE SECTION — First Time Entry (EIBCALEN = 0)")
    class FirstTimeEntry {

        @Test
        @DisplayName("Null commArea sends erased map with empty fields")
        void nullCommAreaSendsErasedMap() {
            DisplayAccountController.ProcessResult result =
                    controller.processRequest(
                            DisplayAccountController.AidKey.ENTER,
                            null, screenField);

            assertEquals(DisplayAccountController.SendMode.ERASE,
                    result.sendMode());
            assertNotNull(result.updatedCommArea());
            assertFalse(result.terminateSession());
            assertFalse(result.returnToMenu());
        }

        @Test
        @DisplayName("First entry clears screen fields")
        void firstEntryClearsScreen() {
            screenField.setMessage("stale message");
            controller.processRequest(
                    DisplayAccountController.AidKey.ENTER,
                    null, screenField);
            assertEquals("", screenField.getMessage());
        }
    }

    @Nested
    @DisplayName("AID Key Handling")
    class AidKeyHandling {

        @ParameterizedTest
        @EnumSource(value = DisplayAccountController.AidKey.class,
                names = {"PA1", "PA2", "PA3"})
        @DisplayName("PA keys continue without processing")
        void paKeysContinue(DisplayAccountController.AidKey paKey) {
            CommArea comm = new CommArea();
            DisplayAccountController.ProcessResult result =
                    controller.processRequest(paKey, comm, screenField);

            assertEquals(DisplayAccountController.SendMode.DATAONLY,
                    result.sendMode());
            assertFalse(result.terminateSession());
            verifyNoInteractions(accountService);
        }

        @Test
        @DisplayName("PF3 returns to main menu")
        void pf3ReturnsToMenu() {
            CommArea comm = new CommArea();
            DisplayAccountController.ProcessResult result =
                    controller.processRequest(
                            DisplayAccountController.AidKey.PF3,
                            comm, screenField);

            assertTrue(result.returnToMenu());
            assertFalse(result.terminateSession());
        }

        @Test
        @DisplayName("PF12 sends termination message and terminates session")
        void pf12TerminatesSession() {
            CommArea comm = new CommArea();
            DisplayAccountController.ProcessResult result =
                    controller.processRequest(
                            DisplayAccountController.AidKey.PF12,
                            comm, screenField);

            assertTrue(result.terminateSession());
            assertFalse(result.returnToMenu());
            assertEquals(DisplayAccountController.MSG_SESSION_ENDED,
                    screenField.getMessage());
        }

        @Test
        @DisplayName("CLEAR erases screen and terminates")
        void clearErasesAndTerminates() {
            screenField.setMessage("something");
            CommArea comm = new CommArea();
            DisplayAccountController.ProcessResult result =
                    controller.processRequest(
                            DisplayAccountController.AidKey.CLEAR,
                            comm, screenField);

            assertTrue(result.terminateSession());
            assertEquals("", screenField.getMessage());
        }

        @Test
        @DisplayName("OTHER key shows invalid key message with alarm")
        void otherKeyShowsInvalidMessage() {
            CommArea comm = new CommArea();
            DisplayAccountController.ProcessResult result =
                    controller.processRequest(
                            DisplayAccountController.AidKey.OTHER,
                            comm, screenField);

            assertEquals(DisplayAccountController.SendMode.DATAONLY_ALARM,
                    result.sendMode());
            assertEquals(DisplayAccountController.MSG_INVALID_KEY,
                    screenField.getMessage());
        }
    }

    // ===================================================================
    // EDIT-DATA SECTION — editData
    // ===================================================================

    @Nested
    @DisplayName("EDIT-DATA SECTION — Input Validation")
    class EditDataSection {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("Blank/null account input is invalid")
        void blankInputIsInvalid(String input) {
            screenField.setAccountNumberInput(input);
            boolean valid = controller.editData(screenField);

            assertFalse(valid);
            assertEquals(DisplayAccountController.MSG_ENTER_ACCOUNT_NUMBER,
                    screenField.getMessage());
        }

        @ParameterizedTest
        @ValueSource(strings = {"ABCDEFGH", "12AB56", "!@#$%^&*"})
        @DisplayName("Non-numeric account input is invalid")
        void nonNumericInputIsInvalid(String input) {
            screenField.setAccountNumberInput(input);
            boolean valid = controller.editData(screenField);

            assertFalse(valid);
            assertEquals(DisplayAccountController.MSG_ENTER_ACCOUNT_NUMBER,
                    screenField.getMessage());
        }

        @ParameterizedTest
        @ValueSource(strings = {"12345678", "00000001", "99999999", "1"})
        @DisplayName("Numeric account input is valid")
        void numericInputIsValid(String input) {
            screenField.setAccountNumberInput(input);
            boolean valid = controller.editData(screenField);

            assertTrue(valid);
        }

        @Test
        @DisplayName("Numeric input with leading/trailing spaces is valid")
        void numericInputWithSpacesIsValid() {
            screenField.setAccountNumberInput("  12345678  ");
            boolean valid = controller.editData(screenField);

            assertTrue(valid);
        }
    }

    // ===================================================================
    // VALIDATE-DATA SECTION — validateData
    // ===================================================================

    @Nested
    @DisplayName("VALIDATE-DATA SECTION — Deletion Pre-Validation")
    class ValidateDataSection {

        @Test
        @DisplayName("Null commArea is invalid for deletion")
        void nullCommAreaInvalid() {
            assertFalse(controller.validateData(null));
        }

        @Test
        @DisplayName("Empty/zeroed commArea is invalid for deletion")
        void emptyCommAreaInvalid() {
            CommArea comm = new CommArea();
            assertFalse(controller.validateData(comm));
        }

        @Test
        @DisplayName("CommArea with zero sortCode is invalid")
        void zeroSortCodeInvalid() {
            CommArea comm = new CommArea();
            comm.setSortCode("000000");
            comm.setAccountNumber(12345678);
            assertFalse(controller.validateData(comm));
        }

        @Test
        @DisplayName("CommArea with zero accountNumber is invalid")
        void zeroAccountNumberInvalid() {
            CommArea comm = new CommArea();
            comm.setSortCode("987654");
            comm.setAccountNumber(0);
            assertFalse(controller.validateData(comm));
        }

        @Test
        @DisplayName("CommArea with valid sortCode and accountNumber is valid")
        void populatedCommAreaIsValid() {
            CommArea comm = buildPopulatedCommArea();
            assertTrue(controller.validateData(comm));
        }
    }

    // ===================================================================
    // GET-ACC-DATA SECTION — getAccountData
    // ===================================================================

    @Nested
    @DisplayName("GET-ACC-DATA SECTION — Account Inquiry")
    class GetAccountDataSection {

        @Test
        @DisplayName("Successful inquiry populates all screen fields")
        void successfulInquiryPopulatesFields() {
            AccountInquiryData found = buildFoundAccount();
            when(accountService.inquireAccount(12345678)).thenReturn(found);

            screenField.setAccountNumberInput("12345678");
            controller.getAccountData(screenField);

            assertEquals("987654", screenField.getSortCode());
            assertEquals("0000012345", screenField.getCustomerNumber());
            assertEquals("12345678", screenField.getAccountNumberDisplay());
            assertEquals("SAVINGS", screenField.getAccountType());
            assertEquals(new BigDecimal("4.50"), screenField.getInterestRate());
            assertEquals("15", screenField.getOpenedDay());
            assertEquals("06", screenField.getOpenedMonth());
            assertEquals("2020", screenField.getOpenedYear());
            assertEquals("500", screenField.getOverdraft());
            assertEquals(DisplayAccountController.MSG_DELETE_PROMPT,
                    screenField.getMessage());
        }

        @Test
        @DisplayName("Account not found sets error and clears fields")
        void accountNotFoundSetsError() {
            when(accountService.inquireAccount(99999999))
                    .thenReturn(buildNotFoundAccount());

            screenField.setAccountNumberInput("99999999");
            controller.getAccountData(screenField);

            assertEquals(DisplayAccountController.MSG_ACCOUNT_NOT_FOUND,
                    screenField.getMessage());
            assertEquals("", screenField.getSortCode());
            assertEquals("", screenField.getCustomerNumber());
        }

        @Test
        @DisplayName("Non-numeric display number handled gracefully")
        void nonNumericAccountHandled() {
            screenField.setAccountNumberInput("NOTNUM");
            controller.getAccountData(screenField);

            assertEquals(DisplayAccountController.MSG_ENTER_ACCOUNT_NUMBER,
                    screenField.getMessage());
            verifyNoInteractions(accountService);
        }

        @Test
        @DisplayName("AccountServiceException propagates for ABEND")
        void serviceExceptionPropagates() {
            when(accountService.inquireAccount(anyInt()))
                    .thenThrow(new AccountServiceException(
                            "LINK INQACC FAIL", 16, 0));

            screenField.setAccountNumberInput("12345678");

            AccountServiceException ex = assertThrows(
                    AccountServiceException.class,
                    () -> controller.getAccountData(screenField));
            assertEquals(16, ex.getResponseCode());
        }
    }

    // ===================================================================
    // DEL-ACC-DATA SECTION — deleteAccountData
    // ===================================================================

    @Nested
    @DisplayName("DEL-ACC-DATA SECTION — Account Deletion")
    class DeleteAccountDataSection {

        @Test
        @DisplayName("Successful deletion clears fields and shows message")
        void successfulDeletion() {
            when(accountService.deleteAccount(12345678))
                    .thenReturn(AccountDeleteResult.success("987654"));

            screenField.setAccountNumberDisplay("12345678");
            controller.deleteAccountData(screenField);

            assertEquals("Account 12345678 was successfully deleted.",
                    screenField.getMessage());
            assertEquals("", screenField.getSortCode());
            assertEquals("", screenField.getCustomerNumber());
        }

        @Test
        @DisplayName("Delete fail code '1' — account not found")
        void deleteNotFound() {
            when(accountService.deleteAccount(11111111))
                    .thenReturn(AccountDeleteResult.failure('1', "987654"));

            screenField.setAccountNumberDisplay("11111111");
            controller.deleteAccountData(screenField);

            assertEquals(DisplayAccountController.MSG_DELETE_NOT_FOUND,
                    screenField.getMessage());
            assertEquals("987654", screenField.getSortCode());
        }

        @Test
        @DisplayName("Delete fail code '2' — datastore error")
        void deleteDatastoreError() {
            when(accountService.deleteAccount(22222222))
                    .thenReturn(AccountDeleteResult.failure('2', "123456"));

            screenField.setAccountNumberDisplay("22222222");
            controller.deleteAccountData(screenField);

            assertEquals(DisplayAccountController.MSG_DELETE_DATASTORE_ERROR,
                    screenField.getMessage());
        }

        @Test
        @DisplayName("Delete fail code '3' — delete error")
        void deleteDeleteError() {
            when(accountService.deleteAccount(33333333))
                    .thenReturn(AccountDeleteResult.failure('3', "111111"));

            screenField.setAccountNumberDisplay("33333333");
            controller.deleteAccountData(screenField);

            assertEquals(DisplayAccountController.MSG_DELETE_ERROR,
                    screenField.getMessage());
        }

        @Test
        @DisplayName("Delete unknown fail code treated as delete error")
        void deleteUnknownFailCode() {
            when(accountService.deleteAccount(44444444))
                    .thenReturn(AccountDeleteResult.failure('9', "222222"));

            screenField.setAccountNumberDisplay("44444444");
            controller.deleteAccountData(screenField);

            assertEquals(DisplayAccountController.MSG_DELETE_ERROR,
                    screenField.getMessage());
        }
    }

    // ===================================================================
    // COMMAREA Management
    // ===================================================================

    @Nested
    @DisplayName("COMMAREA Management")
    class CommAreaManagement {

        @Test
        @DisplayName("buildUpdatedCommArea copies inquiry data when eye is ACCT")
        void copiesInquiryData() {
            AccountInquiryData found = buildFoundAccount();
            when(accountService.inquireAccount(12345678)).thenReturn(found);

            screenField.setAccountNumberInput("12345678");
            CommArea comm = new CommArea();
            controller.processRequest(
                    DisplayAccountController.AidKey.ENTER, comm, screenField);

            CommArea updated = controller.processRequest(
                    DisplayAccountController.AidKey.ENTER, comm, screenField)
                    .updatedCommArea();

            assertNotNull(updated);
        }

        @Test
        @DisplayName("buildUpdatedCommArea returns empty when no inquiry")
        void emptyWhenNoInquiry() {
            CommArea comm = new CommArea();
            DisplayAccountController.ProcessResult result =
                    controller.processRequest(
                            DisplayAccountController.AidKey.OTHER,
                            comm, screenField);

            CommArea updated = result.updatedCommArea();
            assertNotNull(updated);
            assertEquals("", updated.getEye());
        }

        @Test
        @DisplayName("After successful inquiry, commArea has ACCT eye and fields")
        void afterInquiryCommAreaPopulated() {
            AccountInquiryData found = buildFoundAccount();
            when(accountService.inquireAccount(12345678)).thenReturn(found);

            screenField.setAccountNumberInput("12345678");
            CommArea comm = new CommArea();
            DisplayAccountController.ProcessResult result =
                    controller.processRequest(
                            DisplayAccountController.AidKey.ENTER,
                            comm, screenField);

            CommArea updated = result.updatedCommArea();
            assertEquals("ACCT", updated.getEye());
            assertEquals("0000012345", updated.getCustomerNumber());
            assertEquals("987654", updated.getSortCode());
            assertEquals(12345678, updated.getAccountNumber());
            assertEquals("SAVINGS", updated.getAccountType());
            assertEquals(new BigDecimal("4.50"), updated.getInterestRate());
            assertEquals(15062020, updated.getOpened());
            assertEquals(500, updated.getOverdraft());
        }
    }

    // ===================================================================
    // End-to-End Flow Tests
    // ===================================================================

    @Nested
    @DisplayName("End-to-End Flows")
    class EndToEndFlows {

        @Test
        @DisplayName("Full lookup-then-delete flow")
        void lookupThenDelete() {
            AccountInquiryData found = buildFoundAccount();
            when(accountService.inquireAccount(12345678)).thenReturn(found);
            when(accountService.deleteAccount(12345678))
                    .thenReturn(AccountDeleteResult.success("987654"));

            // Step 1: First entry
            DisplayAccountController.ProcessResult r1 =
                    controller.processRequest(
                            DisplayAccountController.AidKey.ENTER,
                            null, screenField);
            assertEquals(DisplayAccountController.SendMode.ERASE,
                    r1.sendMode());

            // Step 2: Enter account number
            screenField.setAccountNumberInput("12345678");
            CommArea comm = r1.updatedCommArea();
            DisplayAccountController.ProcessResult r2 =
                    controller.processRequest(
                            DisplayAccountController.AidKey.ENTER,
                            comm, screenField);
            assertEquals("SAVINGS", screenField.getAccountType());
            assertEquals(DisplayAccountController.MSG_DELETE_PROMPT,
                    screenField.getMessage());

            // Step 3: PF5 to delete
            CommArea comm2 = r2.updatedCommArea();
            DisplayAccountController.ProcessResult r3 =
                    controller.processRequest(
                            DisplayAccountController.AidKey.PF5,
                            comm2, screenField);
            assertTrue(screenField.getMessage()
                    .contains("successfully deleted"));
        }

        @Test
        @DisplayName("Inquiry then PF3 exits to menu")
        void inquiryThenExit() {
            AccountInquiryData found = buildFoundAccount();
            when(accountService.inquireAccount(12345678)).thenReturn(found);

            screenField.setAccountNumberInput("12345678");
            CommArea comm = new CommArea();
            controller.processRequest(
                    DisplayAccountController.AidKey.ENTER,
                    comm, screenField);

            DisplayAccountController.ProcessResult result =
                    controller.processRequest(
                            DisplayAccountController.AidKey.PF3,
                            comm, screenField);
            assertTrue(result.returnToMenu());
        }
    }

    // ===================================================================
    // Utility Methods
    // ===================================================================

    @Nested
    @DisplayName("Utility Methods")
    class UtilityMethods {

        @Test
        @DisplayName("splitDateToScreen parses DDMMYYYY correctly")
        void splitDateCorrectly() {
            String[] day = new String[1];
            String[] month = new String[1];
            String[] year = new String[1];

            DisplayAccountController.splitDateToScreen(25122023,
                    d -> day[0] = d, m -> month[0] = m, y -> year[0] = y);

            assertEquals("25", day[0]);
            assertEquals("12", month[0]);
            assertEquals("2023", year[0]);
        }

        @Test
        @DisplayName("splitDateToScreen with zero gives empty strings")
        void splitDateZero() {
            String[] day = new String[1];
            String[] month = new String[1];
            String[] year = new String[1];

            DisplayAccountController.splitDateToScreen(0,
                    d -> day[0] = d, m -> month[0] = m, y -> year[0] = y);

            assertEquals("", day[0]);
            assertEquals("", month[0]);
            assertEquals("", year[0]);
        }

        @Test
        @DisplayName("splitDateToScreen pads short dates")
        void splitDatePads() {
            String[] day = new String[1];
            String[] month = new String[1];
            String[] year = new String[1];

            DisplayAccountController.splitDateToScreen(1012024,
                    d -> day[0] = d, m -> month[0] = m, y -> year[0] = y);

            assertEquals("01", day[0]);
            assertEquals("01", month[0]);
            assertEquals("2024", year[0]);
        }

        @Test
        @DisplayName("formatBalance positive value")
        void formatBalancePositive() {
            assertEquals("+1500.75",
                    DisplayAccountController.formatBalance(
                            new BigDecimal("1500.75")));
        }

        @Test
        @DisplayName("formatBalance negative value")
        void formatBalanceNegative() {
            assertEquals("-250.00",
                    DisplayAccountController.formatBalance(
                            new BigDecimal("-250.00")));
        }

        @Test
        @DisplayName("formatBalance zero")
        void formatBalanceZero() {
            assertEquals("+0.00",
                    DisplayAccountController.formatBalance(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("formatBalance null")
        void formatBalanceNull() {
            assertEquals("+0000000000.00",
                    DisplayAccountController.formatBalance(null));
        }
    }

    // ===================================================================
    // Constructor Validation
    // ===================================================================

    @Test
    @DisplayName("Constructor rejects null AccountService")
    void constructorRejectsNull() {
        assertThrows(NullPointerException.class,
                () -> new DisplayAccountController(null));
    }
}
