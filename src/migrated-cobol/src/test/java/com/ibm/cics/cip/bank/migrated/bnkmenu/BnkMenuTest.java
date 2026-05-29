/*
 * Copyright IBM Corp. 2023
 */
package com.ibm.cics.cip.bank.migrated.bnkmenu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BnkMenuTest {

    private StubTerminalService terminalService;
    private BnkMenu bnkMenu;

    @BeforeEach
    void setUp() {
        terminalService = new StubTerminalService();
        bnkMenu = new BnkMenu(terminalService);
    }

    @Nested
    @DisplayName("First-time handling (EIBCALEN = 0)")
    class FirstTimeTests {

        @Test
        @DisplayName("Should send map with ERASE mode on first entry")
        void shouldSendEraseMapOnFirstEntry() {
            bnkMenu.handleFirstTime();

            assertEquals(1, terminalService.sendMapCount);
            assertEquals(SendMode.ERASE, terminalService.lastSendMode);
        }

        @Test
        @DisplayName("Should clear screen data on first entry")
        void shouldClearScreenDataOnFirstEntry() {
            bnkMenu.getScreenData().setMessage("old message");
            bnkMenu.handleFirstTime();

            assertEquals("", bnkMenu.getScreenData().getAction());
            assertEquals("", bnkMenu.getScreenData().getMessage());
        }
    }

    @Nested
    @DisplayName("PA key handling")
    class PaKeyTests {

        @ParameterizedTest
        @EnumSource(value = AidKey.class, names = {"PA1", "PA2", "PA3"})
        @DisplayName("Should take no action when PA key is pressed")
        void shouldContinueOnPaKey(AidKey paKey) {
            bnkMenu.processInput(paKey);

            assertEquals(0, terminalService.sendMapCount);
            assertEquals(0, terminalService.returnImmediateCount);
        }
    }

    @Nested
    @DisplayName("Termination handling (PF3, PF12)")
    class TerminationTests {

        @Test
        @DisplayName("Should send termination message on PF3")
        void shouldTerminateOnPf3() {
            bnkMenu.processInput(AidKey.PF3);

            assertEquals(BnkMenu.END_OF_SESSION_MESSAGE,
                    terminalService.lastTerminationMessage);
            assertTrue(terminalService.returnToCicsCalled);
        }

        @Test
        @DisplayName("Should send termination message on PF12")
        void shouldTerminateOnPf12() {
            bnkMenu.processInput(AidKey.PF12);

            assertEquals(BnkMenu.END_OF_SESSION_MESSAGE,
                    terminalService.lastTerminationMessage);
            assertTrue(terminalService.returnToCicsCalled);
        }
    }

    @Nested
    @DisplayName("CLEAR key handling")
    class ClearKeyTests {

        @Test
        @DisplayName("Should send erase control and return on CLEAR")
        void shouldEraseAndReturnOnClear() {
            bnkMenu.processInput(AidKey.CLEAR);

            assertTrue(terminalService.sendControlEraseCalled);
            assertTrue(terminalService.returnToCicsCalled);
        }
    }

    @Nested
    @DisplayName("Invalid key handling")
    class InvalidKeyTests {

        @Test
        @DisplayName("Should show invalid key message for OTHER key")
        void shouldShowInvalidKeyMessage() {
            bnkMenu.processInput(AidKey.OTHER);

            assertEquals(BnkMenu.INVALID_KEY_MESSAGE,
                    bnkMenu.getScreenData().getMessage());
            assertEquals(SendMode.DATAONLY_ALARM,
                    terminalService.lastSendMode);
        }
    }

    @Nested
    @DisplayName("Menu data validation (EDIT-MENU-DATA)")
    class EditMenuDataTests {

        @ParameterizedTest
        @ValueSource(strings = {"1", "2", "3", "4", "5", "6", "7", "A"})
        @DisplayName("Should accept valid menu options")
        void shouldAcceptValidOptions(String validOption) {
            bnkMenu.setActionInput(validOption);
            bnkMenu.editMenuData();

            assertTrue(bnkMenu.isValidData());
        }

        @ParameterizedTest
        @ValueSource(strings = {"0", "8", "9", "B", "Z", "a", " ", ""})
        @DisplayName("Should reject invalid menu options")
        void shouldRejectInvalidOptions(String invalidOption) {
            bnkMenu.setActionInput(invalidOption);
            bnkMenu.editMenuData();

            assertFalse(bnkMenu.isValidData());
            assertEquals(BnkMenu.INVALID_ACTION_MESSAGE,
                    bnkMenu.getScreenData().getMessage());
        }

        @Test
        @DisplayName("Should reject null action input")
        void shouldRejectNullInput() {
            bnkMenu.setActionInput(null);
            bnkMenu.editMenuData();

            assertFalse(bnkMenu.isValidData());
            assertEquals(BnkMenu.INVALID_ACTION_MESSAGE,
                    bnkMenu.getScreenData().getMessage());
        }
    }

    @Nested
    @DisplayName("Transaction invocation (INVOKE-OTHER-TXNS)")
    class InvokeTransactionTests {

        @Test
        @DisplayName("Option 1 should invoke ODCS transaction")
        void shouldInvokeOdcs() {
            bnkMenu.setActionInput("1");
            bnkMenu.invokeOtherTransaction();

            assertEquals("ODCS", terminalService.lastReturnImmediateTransId);
        }

        @Test
        @DisplayName("Option 2 should invoke ODAC transaction")
        void shouldInvokeOdac() {
            bnkMenu.setActionInput("2");
            bnkMenu.invokeOtherTransaction();

            assertEquals("ODAC", terminalService.lastReturnImmediateTransId);
        }

        @Test
        @DisplayName("Option 3 should invoke OCCS transaction")
        void shouldInvokeOccs() {
            bnkMenu.setActionInput("3");
            bnkMenu.invokeOtherTransaction();

            assertEquals("OCCS", terminalService.lastReturnImmediateTransId);
        }

        @Test
        @DisplayName("Option 4 should invoke OCAC transaction")
        void shouldInvokeOcac() {
            bnkMenu.setActionInput("4");
            bnkMenu.invokeOtherTransaction();

            assertEquals("OCAC", terminalService.lastReturnImmediateTransId);
        }

        @Test
        @DisplayName("Option 5 should invoke OUAC transaction")
        void shouldInvokeOuac() {
            bnkMenu.setActionInput("5");
            bnkMenu.invokeOtherTransaction();

            assertEquals("OUAC", terminalService.lastReturnImmediateTransId);
        }

        @Test
        @DisplayName("Option 6 should invoke OCRA transaction")
        void shouldInvokeOcra() {
            bnkMenu.setActionInput("6");
            bnkMenu.invokeOtherTransaction();

            assertEquals("OCRA", terminalService.lastReturnImmediateTransId);
        }

        @Test
        @DisplayName("Option 7 should invoke OTFN transaction")
        void shouldInvokeOtfn() {
            bnkMenu.setActionInput("7");
            bnkMenu.invokeOtherTransaction();

            assertEquals("OTFN", terminalService.lastReturnImmediateTransId);
        }

        @Test
        @DisplayName("Option A should invoke OCCA transaction")
        void shouldInvokeOcca() {
            bnkMenu.setActionInput("A");
            bnkMenu.invokeOtherTransaction();

            assertEquals("OCCA", terminalService.lastReturnImmediateTransId);
        }
    }

    @Nested
    @DisplayName("ENTER key full flow (PROCESS-MENU-MAP)")
    class ProcessMenuMapTests {

        @Test
        @DisplayName("Valid action triggers receive, validate, invoke")
        void shouldProcessValidAction() {
            terminalService.receiveActionInput = "1";

            bnkMenu.processInput(AidKey.ENTER);

            assertEquals(1, terminalService.receiveMapCount);
            assertEquals("ODCS", terminalService.lastReturnImmediateTransId);
        }

        @Test
        @DisplayName("Invalid action shows error and sends map")
        void shouldShowErrorForInvalidAction() {
            terminalService.receiveActionInput = "X";

            bnkMenu.processInput(AidKey.ENTER);

            assertEquals(1, terminalService.receiveMapCount);
            assertEquals(0, terminalService.returnImmediateCount);
            assertEquals(BnkMenu.INVALID_ACTION_MESSAGE,
                    bnkMenu.getScreenData().getMessage());
            assertEquals(SendMode.DATAONLY_ALARM,
                    terminalService.lastSendMode);
        }
    }

    @Nested
    @DisplayName("MAPFAIL handling")
    class MapFailTests {

        @Test
        @DisplayName("Should resend map on MAPFAIL")
        void shouldResendMapOnMapFail() {
            terminalService.receiveMapFailure = true;

            bnkMenu.receiveMenuMap();

            assertEquals(1, terminalService.sendMapCount);
            assertEquals(SendMode.ERASE, terminalService.lastSendMode);
        }
    }

    @Nested
    @DisplayName("Return to menu (RETURN TRANSID)")
    class ReturnToMenuTests {

        @Test
        @DisplayName("Should return with OMEN transaction ID")
        void shouldReturnWithOmenTransId() {
            bnkMenu.returnToMenu();

            assertEquals(BnkMenu.RETURN_TRANS_ID,
                    terminalService.lastReturnTransId);
        }
    }

    @Nested
    @DisplayName("Abend handling")
    class AbendHandlerTests {

        @Test
        @DisplayName("Should populate abend info and invoke handler on error")
        void shouldHandleAbendCorrectly() {
            terminalService.sendMapShouldFail = true;

            bnkMenu.sendMap(SendMode.ERASE);

            assertTrue(terminalService.abendTaskCalled);
            assertEquals(BnkMenu.ABEND_CODE,
                    terminalService.lastAbendCode);
            assertNotNull(terminalService.lastAbndInfo);
            assertEquals(99, terminalService.lastAbndInfo.getRespCode());
            assertEquals(88, terminalService.lastAbndInfo.getResp2Code());
            assertEquals(BnkMenu.ABEND_CODE,
                    terminalService.lastAbndInfo.getCode());
            assertEquals(0, terminalService.lastAbndInfo.getSqlCode());
        }

        @Test
        @DisplayName("Abend freeform includes section and description")
        void shouldFormatFreeformCorrectly() {
            terminalService.sendMapShouldFail = true;

            bnkMenu.sendMap(SendMode.ERASE);

            String freeform = terminalService.lastAbndInfo.getFreeform();
            assertTrue(freeform.contains("SMM010"));
            assertTrue(freeform.contains("SEND MAP ERASE FAIL"));
            assertTrue(freeform.contains("EIBRESP=99"));
            assertTrue(freeform.contains("RESP2=88"));
        }

        @Test
        @DisplayName("Should use correct freeform for DATAONLY failure")
        void shouldDescribeDataonlyFailure() {
            terminalService.sendMapShouldFail = true;

            bnkMenu.sendMap(SendMode.DATAONLY);

            String freeform = terminalService.lastAbndInfo.getFreeform();
            assertTrue(freeform.contains("SEND MAP DATAONLY FAIL"));
        }

        @Test
        @DisplayName("Should use correct freeform for DATAONLY ALARM failure")
        void shouldDescribeDataonlyAlarmFailure() {
            terminalService.sendMapShouldFail = true;

            bnkMenu.sendMap(SendMode.DATAONLY_ALARM);

            String freeform = terminalService.lastAbndInfo.getFreeform();
            assertTrue(freeform.contains("SEND MAP DATAONLY ALARM FAIL"));
        }

        @Test
        @DisplayName("RECEIVE MAP failure triggers abend")
        void shouldAbendOnReceiveMapFailure() {
            terminalService.receiveMapFatalFailure = true;

            bnkMenu.receiveMenuMap();

            assertTrue(terminalService.abendTaskCalled);
            String freeform = terminalService.lastAbndInfo.getFreeform();
            assertTrue(freeform.contains("RMM010"));
            assertTrue(freeform.contains("RECEIVE MAP FAIL"));
        }

        @Test
        @DisplayName("SEND TEXT failure triggers abend")
        void shouldAbendOnSendTextFailure() {
            terminalService.sendTerminationShouldFail = true;

            bnkMenu.sendTerminationMessage();

            assertTrue(terminalService.abendTaskCalled);
            String freeform = terminalService.lastAbndInfo.getFreeform();
            assertTrue(freeform.contains("STM010"));
            assertTrue(freeform.contains("SEND TEXT FAIL"));
        }

        @Test
        @DisplayName("RETURN TRANSID failure triggers abend")
        void shouldAbendOnReturnTransIdFailure() {
            terminalService.returnWithTransIdShouldFail = true;

            bnkMenu.returnToMenu();

            assertTrue(terminalService.abendTaskCalled);
            String freeform = terminalService.lastAbndInfo.getFreeform();
            assertTrue(freeform.contains("A010"));
            assertTrue(freeform.contains("RETURN TRANSID(MENU) FAIL"));
        }

        @Test
        @DisplayName("Transaction invoke failure triggers abend")
        void shouldAbendOnTransactionInvokeFailure() {
            terminalService.returnImmediateShouldFail = true;
            bnkMenu.setActionInput("1");

            bnkMenu.invokeOtherTransaction();

            assertTrue(terminalService.abendTaskCalled);
            String freeform = terminalService.lastAbndInfo.getFreeform();
            assertTrue(freeform.contains("IOT010"));
            assertTrue(freeform.contains("RETURN TRANSID(ODCS) FAIL"));
        }
    }

    @Nested
    @DisplayName("Constants verification")
    class ConstantsTests {

        @Test
        @DisplayName("Program name should be BNKMENU")
        void shouldHaveCorrectProgramName() {
            assertEquals("BNKMENU", BnkMenu.PROGRAM_NAME);
        }

        @Test
        @DisplayName("Return transaction ID should be OMEN")
        void shouldHaveCorrectReturnTransId() {
            assertEquals("OMEN", BnkMenu.RETURN_TRANS_ID);
        }

        @Test
        @DisplayName("Abend code should be HBNK")
        void shouldHaveCorrectAbendCode() {
            assertEquals("HBNK", BnkMenu.ABEND_CODE);
        }

        @Test
        @DisplayName("End-of-session message should match COBOL original")
        void shouldHaveCorrectEndOfSessionMessage() {
            assertEquals("Session Ended", BnkMenu.END_OF_SESSION_MESSAGE);
        }
    }
}
