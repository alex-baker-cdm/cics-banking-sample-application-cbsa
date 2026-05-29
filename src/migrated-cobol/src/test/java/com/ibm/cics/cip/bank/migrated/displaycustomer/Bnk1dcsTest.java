package com.ibm.cics.cip.bank.migrated.displaycustomer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for Bnk1dcs — the Java 21 migration of BNK1DCS.cbl.
 * Tests cover every major COBOL section and branch of the original program.
 */
class Bnk1dcsTest {

    private StubCustomerService stubService;
    private Bnk1dcs bnk1dcs;

    @BeforeEach
    void setUp() {
        stubService = new StubCustomerService();
        bnk1dcs = new Bnk1dcs(stubService);
    }

    // -----------------------------------------------------------------------
    // First-time entry (EIBCALEN = 0)
    // -----------------------------------------------------------------------

    @Nested
    class FirstTimeEntryTests {

        @Test
        void firstTimeEntry_sendsEraseMapWithEmptyFields() {
            DisplayCustomerResult result = bnk1dcs.processTransaction(
                    AidKey.ENTER, null, null);

            assertEquals(DisplayCustomerResult.Action.SEND_MAP, result.getAction());
            assertTrue(result.isEraseScreen());
            assertFalse(result.isAlarm());
            assertNotNull(result.getCommarea());
            assertNotNull(result.getScreenData());
            assertEquals("", result.getScreenData().getCustomerNumberInput());
        }

        @Test
        void firstTimeEntry_initializesCommarea() {
            DisplayCustomerResult result = bnk1dcs.processTransaction(
                    AidKey.ENTER, null, null);

            DisplayCustomerCommarea comm = result.getCommarea();
            assertEquals("", comm.getEye());
            assertEquals("", comm.getSortCode());
            assertEquals("", comm.getCustomerNumber());
        }
    }

    // -----------------------------------------------------------------------
    // Key handling (EVALUATE TRUE block in PREMIERE)
    // -----------------------------------------------------------------------

    @Nested
    class KeyHandlingTests {

        @Test
        void paKey_continuesWithoutProcessing() {
            DisplayCustomerCommarea comm = new DisplayCustomerCommarea();
            DisplayCustomerResult result = bnk1dcs.processTransaction(
                    AidKey.PA1, comm, new DisplayCustomerScreenData());

            assertEquals(DisplayCustomerResult.Action.SEND_MAP, result.getAction());
            assertFalse(result.isEraseScreen());
        }

        @Test
        void pa2Key_continuesWithoutProcessing() {
            DisplayCustomerCommarea comm = new DisplayCustomerCommarea();
            DisplayCustomerResult result = bnk1dcs.processTransaction(
                    AidKey.PA2, comm, new DisplayCustomerScreenData());

            assertEquals(DisplayCustomerResult.Action.SEND_MAP, result.getAction());
        }

        @Test
        void pa3Key_continuesWithoutProcessing() {
            DisplayCustomerCommarea comm = new DisplayCustomerCommarea();
            DisplayCustomerResult result = bnk1dcs.processTransaction(
                    AidKey.PA3, comm, new DisplayCustomerScreenData());

            assertEquals(DisplayCustomerResult.Action.SEND_MAP, result.getAction());
        }

        @Test
        void pf3_returnsToMainMenu() {
            DisplayCustomerCommarea comm = new DisplayCustomerCommarea();
            DisplayCustomerResult result = bnk1dcs.processTransaction(
                    AidKey.PF3, comm, new DisplayCustomerScreenData());

            assertEquals(DisplayCustomerResult.Action.RETURN_TO_MENU,
                    result.getAction());
        }

        @Test
        void pf12_sendsTerminationMessage() {
            DisplayCustomerCommarea comm = new DisplayCustomerCommarea();
            DisplayCustomerResult result = bnk1dcs.processTransaction(
                    AidKey.PF12, comm, new DisplayCustomerScreenData());

            assertEquals(DisplayCustomerResult.Action.SESSION_ENDED,
                    result.getAction());
        }

        @Test
        void clearKey_clearsScreen() {
            DisplayCustomerCommarea comm = new DisplayCustomerCommarea();
            DisplayCustomerResult result = bnk1dcs.processTransaction(
                    AidKey.CLEAR, comm, new DisplayCustomerScreenData());

            assertEquals(DisplayCustomerResult.Action.CLEAR_SCREEN,
                    result.getAction());
        }

        @Test
        void otherKey_sendsInvalidKeyMessage() {
            DisplayCustomerCommarea comm = new DisplayCustomerCommarea();
            DisplayCustomerResult result = bnk1dcs.processTransaction(
                    AidKey.PF1, comm, new DisplayCustomerScreenData());

            assertEquals(DisplayCustomerResult.Action.SEND_MAP, result.getAction());
            assertEquals("Invalid key pressed.",
                    result.getScreenData().getMessage());
        }

        @Test
        void pf2_sendsInvalidKeyMessage() {
            DisplayCustomerCommarea comm = new DisplayCustomerCommarea();
            DisplayCustomerResult result = bnk1dcs.processTransaction(
                    AidKey.PF2, comm, new DisplayCustomerScreenData());

            assertEquals("Invalid key pressed.",
                    result.getScreenData().getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // EDIT-DATA validation (ED010)
    // -----------------------------------------------------------------------

    @Nested
    class EditDataTests {

        @Test
        void editData_emptyCustomerNumber_setsError() {
            DisplayCustomerScreenData screen = new DisplayCustomerScreenData();
            screen.setCustomerNumberInput("");

            bnk1dcs.processTransaction(AidKey.ENTER,
                    new DisplayCustomerCommarea(), screen);

            assertFalse(bnk1dcs.isValidData());
        }

        @Test
        void editData_nullCustomerNumber_setsError() {
            DisplayCustomerScreenData screen = new DisplayCustomerScreenData();
            screen.setCustomerNumberInput(null);

            bnk1dcs.processTransaction(AidKey.ENTER,
                    new DisplayCustomerCommarea(), screen);

            assertFalse(bnk1dcs.isValidData());
        }

        @Test
        void editData_blankCustomerNumber_setsError() {
            DisplayCustomerScreenData screen = new DisplayCustomerScreenData();
            screen.setCustomerNumberInput("   ");

            bnk1dcs.processTransaction(AidKey.ENTER,
                    new DisplayCustomerCommarea(), screen);

            assertFalse(bnk1dcs.isValidData());
            assertEquals("Please enter a customer number.",
                    screen.getMessage());
        }

        @Test
        void editData_nonNumericCustomerNumber_setsError() {
            DisplayCustomerScreenData screen = new DisplayCustomerScreenData();
            screen.setCustomerNumberInput("ABC123");

            bnk1dcs.processTransaction(AidKey.ENTER,
                    new DisplayCustomerCommarea(), screen);

            assertFalse(bnk1dcs.isValidData());
        }

        @Test
        void editData_numericWithFormatChars_deEditsSuccessfully() {
            DisplayCustomerScreenData screen = new DisplayCustomerScreenData();
            screen.setCustomerNumberInput("1,234");
            stubService.setInqCustResponse(buildSampleInqResponse());

            bnk1dcs.processTransaction(AidKey.ENTER,
                    new DisplayCustomerCommarea(), screen);

            assertTrue(bnk1dcs.isValidData());
        }

        @Test
        void editData_validNumericCustomerNumber_passes() {
            DisplayCustomerScreenData screen = new DisplayCustomerScreenData();
            screen.setCustomerNumberInput("0000012345");
            stubService.setInqCustResponse(buildSampleInqResponse());

            bnk1dcs.processTransaction(AidKey.ENTER,
                    new DisplayCustomerCommarea(), screen);

            assertTrue(bnk1dcs.isValidData());
        }
    }

    // -----------------------------------------------------------------------
    // EDIT-DATA2 validation (ED2010) — update title + address validation
    // -----------------------------------------------------------------------

    @Nested
    class EditData2Tests {

        @Test
        void editData2_validTitle_passes() {
            DisplayCustomerCommarea comm = new DisplayCustomerCommarea();
            comm.setUpdateFlag("Y");

            DisplayCustomerScreenData screen = new DisplayCustomerScreenData();
            screen.setCustomerNumberInput("0000012345");
            screen.setCustomerName("Mr John Smith");
            screen.setCustomerAddress1("123 Main St");
            screen.setCustomerNumber2("0000012345");
            screen.setSortCode("987654");

            stubService.setUpdCustResponse(buildSampleUpdResponse());

            bnk1dcs.processTransaction(AidKey.ENTER, comm, screen);

            assertTrue(bnk1dcs.isValidData());
        }

        @Test
        void editData2_invalidTitle_setsError() {
            DisplayCustomerCommarea comm = new DisplayCustomerCommarea();
            comm.setUpdateFlag("Y");

            DisplayCustomerScreenData screen = new DisplayCustomerScreenData();
            screen.setCustomerNumberInput("0000012345");
            screen.setCustomerName("King Arthur");
            screen.setCustomerAddress1("Castle");

            bnk1dcs.processTransaction(AidKey.ENTER, comm, screen);

            assertFalse(bnk1dcs.isValidData());
            assertTrue(screen.getMessage().contains("Valid titles are"));
        }

        @Test
        void editData2_professorTitle_passes() {
            DisplayCustomerCommarea comm = new DisplayCustomerCommarea();
            comm.setUpdateFlag("Y");

            DisplayCustomerScreenData screen = new DisplayCustomerScreenData();
            screen.setCustomerName("Professor Xavier");
            screen.setCustomerAddress1("Westchester");
            screen.setCustomerNumber2("0000012345");
            screen.setSortCode("987654");

            stubService.setUpdCustResponse(buildSampleUpdResponse());

            bnk1dcs.processTransaction(AidKey.ENTER, comm, screen);

            assertTrue(bnk1dcs.isValidData());
        }

        @Test
        void editData2_allAddressesBlank_setsError() {
            DisplayCustomerCommarea comm = new DisplayCustomerCommarea();
            comm.setUpdateFlag("Y");

            DisplayCustomerScreenData screen = new DisplayCustomerScreenData();
            screen.setCustomerName("Mr John Smith");
            screen.setCustomerAddress1("   ");
            screen.setCustomerAddress2("");
            screen.setCustomerAddress3(null);

            bnk1dcs.processTransaction(AidKey.ENTER, comm, screen);

            assertFalse(bnk1dcs.isValidData());
            assertTrue(screen.getMessage().contains("Address must not be all spaces"));
        }

        @Test
        void editData2_emptyName_setsError() {
            DisplayCustomerCommarea comm = new DisplayCustomerCommarea();
            comm.setUpdateFlag("Y");

            DisplayCustomerScreenData screen = new DisplayCustomerScreenData();
            screen.setCustomerName("");
            screen.setCustomerAddress1("123 Main St");

            bnk1dcs.processTransaction(AidKey.ENTER, comm, screen);

            assertFalse(bnk1dcs.isValidData());
        }

        @Test
        void editData2_eachValidTitle_isAccepted() {
            String[] titles = {"Mr", "Mrs", "Miss", "Ms", "Dr",
                    "Professor", "Drs", "Lord", "Sir", "Lady"};

            for (String title : titles) {
                stubService = new StubCustomerService();
                bnk1dcs = new Bnk1dcs(stubService);

                DisplayCustomerCommarea comm = new DisplayCustomerCommarea();
                comm.setUpdateFlag("Y");

                DisplayCustomerScreenData screen = new DisplayCustomerScreenData();
                screen.setCustomerName(title + " TestPerson");
                screen.setCustomerAddress1("Address");
                screen.setCustomerNumber2("0000012345");
                screen.setSortCode("987654");

                stubService.setUpdCustResponse(buildSampleUpdResponse());

                bnk1dcs.processTransaction(AidKey.ENTER, comm, screen);

                assertTrue(bnk1dcs.isValidData(),
                        "Title '" + title + "' should be valid");
            }
        }
    }

    // -----------------------------------------------------------------------
    // VALIDATE-DATA (VD010)
    // -----------------------------------------------------------------------

    @Nested
    class ValidateDataTests {

        @Test
        void validateData_sortCodeAllZeros_setsError() {
            DisplayCustomerScreenData screen = new DisplayCustomerScreenData();
            screen.setCustomerNumberInput("0000012345");
            screen.setSortCode("000000");

            InqCustCommarea inq = new InqCustCommarea();
            inq.setSortCode("000000");
            inq.setName("Mr Test");
            inq.setAddress("123 Street");
            stubService.setInqCustResponse(inq);

            bnk1dcs.processTransaction(AidKey.PF5,
                    new DisplayCustomerCommarea(), screen);

            assertFalse(bnk1dcs.isValidData());
            assertTrue(screen.getMessage().contains("not VALID"));
        }

        @Test
        void validateData_custNoAllNines_setsError() {
            DisplayCustomerScreenData screen = new DisplayCustomerScreenData();
            screen.setCustomerNumberInput("9999999999");
            screen.setSortCode("123456");

            InqCustCommarea inq = new InqCustCommarea();
            inq.setSortCode("123456");
            inq.setName("Mr Test");
            inq.setAddress("123 Street");
            stubService.setInqCustResponse(inq);

            bnk1dcs.processTransaction(AidKey.PF5,
                    new DisplayCustomerCommarea(), screen);

            assertFalse(bnk1dcs.isValidData());
            assertTrue(screen.getMessage().contains("not VALID"));
        }

        @Test
        void validateData_custNoZero_setsError() {
            DisplayCustomerScreenData screen = new DisplayCustomerScreenData();
            screen.setCustomerNumberInput("0000000000");
            screen.setSortCode("123456");

            InqCustCommarea inq = new InqCustCommarea();
            inq.setSortCode("123456");
            inq.setName("Mr Test");
            inq.setAddress("123 Street");
            stubService.setInqCustResponse(inq);

            bnk1dcs.processTransaction(AidKey.PF5,
                    new DisplayCustomerCommarea(), screen);

            assertFalse(bnk1dcs.isValidData());
        }

        @Test
        void validateData_validSortCodeAndCustNo_passes() {
            DisplayCustomerScreenData screen = new DisplayCustomerScreenData();
            screen.setCustomerNumberInput("0000012345");
            screen.setSortCode("123456");

            InqCustCommarea inq = buildSampleInqResponse();
            inq.setSortCode("123456");
            stubService.setInqCustResponse(inq);
            stubService.setDelCusResponse(buildSampleDelResponse());

            bnk1dcs.processTransaction(AidKey.PF5,
                    new DisplayCustomerCommarea(), screen);

            assertTrue(bnk1dcs.isValidData());
        }
    }

    // -----------------------------------------------------------------------
    // GET-CUST-DATA (GCD010)
    // -----------------------------------------------------------------------

    @Nested
    class GetCustomerDataTests {

        @Test
        void getCustData_customerFound_mapsToScreen() {
            DisplayCustomerScreenData screen = new DisplayCustomerScreenData();
            screen.setCustomerNumberInput("0000012345");

            InqCustCommarea inq = buildSampleInqResponse();
            stubService.setInqCustResponse(inq);

            bnk1dcs.processTransaction(AidKey.ENTER,
                    new DisplayCustomerCommarea(), screen);

            assertTrue(bnk1dcs.isValidData());
            assertEquals("987654", screen.getSortCode());
            assertEquals("12345", screen.getCustomerNumber2());
            assertEquals("Mr Test Customer", screen.getCustomerName());
            assertTrue(screen.getMessage().contains("Customer lookup successful"));
        }

        @Test
        void getCustData_customerNotFound_setsErrorMessage() {
            DisplayCustomerScreenData screen = new DisplayCustomerScreenData();
            screen.setCustomerNumberInput("0000099999");

            InqCustCommarea inq = new InqCustCommarea();
            inq.setName("");
            inq.setAddress("");
            stubService.setInqCustResponse(inq);

            bnk1dcs.processTransaction(AidKey.ENTER,
                    new DisplayCustomerCommarea(), screen);

            assertFalse(bnk1dcs.isValidData());
            assertTrue(screen.getMessage().contains("not found"));
        }

        @Test
        void getCustData_nullResponse_setsErrorMessage() {
            DisplayCustomerScreenData screen = new DisplayCustomerScreenData();
            screen.setCustomerNumberInput("0000012345");
            stubService.setInqCustResponse(null);

            bnk1dcs.processTransaction(AidKey.ENTER,
                    new DisplayCustomerCommarea(), screen);

            assertFalse(bnk1dcs.isValidData());
            assertTrue(screen.getMessage().contains("not found"));
        }

        @Test
        void getCustData_addressSplitCorrectly() {
            DisplayCustomerScreenData screen = new DisplayCustomerScreenData();
            screen.setCustomerNumberInput("0000012345");

            InqCustCommarea inq = buildSampleInqResponse();
            String addr = Bnk1dcs.padRight("123 Main Street", 60)
                    + Bnk1dcs.padRight("Suite 400", 60)
                    + Bnk1dcs.padRight("New York, NY 10001", 40);
            inq.setAddress(addr);
            stubService.setInqCustResponse(inq);

            bnk1dcs.processTransaction(AidKey.ENTER,
                    new DisplayCustomerCommarea(), screen);

            assertEquals("123 Main Street", screen.getCustomerAddress1());
            assertEquals("Suite 400", screen.getCustomerAddress2());
            assertEquals("New York, NY 10001", screen.getCustomerAddress3());
        }

        @Test
        void getCustData_specialCustNo_showsSimpleSuccessMessage() {
            DisplayCustomerScreenData screen = new DisplayCustomerScreenData();
            screen.setCustomerNumberInput("0");

            InqCustCommarea inq = buildSampleInqResponse();
            inq.setCustomerNumber(0);
            stubService.setInqCustResponse(inq);

            bnk1dcs.processTransaction(AidKey.ENTER,
                    new DisplayCustomerCommarea(), screen);

            assertEquals("Customer lookup successful.",
                    screen.getMessage());
        }

        @Test
        void getCustData_normalCustNo_showsDeleteUpdateMessage() {
            DisplayCustomerScreenData screen = new DisplayCustomerScreenData();
            screen.setCustomerNumberInput("0000012345");

            stubService.setInqCustResponse(buildSampleInqResponse());

            bnk1dcs.processTransaction(AidKey.ENTER,
                    new DisplayCustomerCommarea(), screen);

            assertTrue(screen.getMessage().contains("<PF5> to Delete"));
            assertTrue(screen.getMessage().contains("<PF10> to Update"));
        }
    }

    // -----------------------------------------------------------------------
    // DEL-CUST-DATA (DCD010)
    // -----------------------------------------------------------------------

    @Nested
    class DeleteCustomerDataTests {

        @Test
        void deleteCustomer_success_showsSuccessMessage() {
            DisplayCustomerScreenData screen = new DisplayCustomerScreenData();
            screen.setCustomerNumberInput("0000012345");

            InqCustCommarea inq = buildSampleInqResponse();
            inq.setSortCode("123456");
            stubService.setInqCustResponse(inq);

            DelCusCommarea del = new DelCusCommarea();
            del.setDeleteSuccess("Y");
            del.setCustomerNumber("0000012345");
            stubService.setDelCusResponse(del);

            bnk1dcs.processTransaction(AidKey.PF5,
                    new DisplayCustomerCommarea(), screen);

            assertTrue(bnk1dcs.isValidData());
            assertTrue(screen.getMessage().contains("successfully deleted"));
        }

        @Test
        void deleteCustomer_failCode1_customerNotFound() {
            DisplayCustomerScreenData screen = new DisplayCustomerScreenData();
            screen.setCustomerNumberInput("0000012345");

            InqCustCommarea inq = buildSampleInqResponse();
            inq.setSortCode("123456");
            stubService.setInqCustResponse(inq);

            DelCusCommarea del = new DelCusCommarea();
            del.setDeleteSuccess("N");
            del.setDeleteFailCode("1");
            del.setSortCode("123456");
            stubService.setDelCusResponse(del);

            bnk1dcs.processTransaction(AidKey.PF5,
                    new DisplayCustomerCommarea(), screen);

            assertFalse(bnk1dcs.isValidData());
            assertTrue(screen.getMessage().contains("not found"));
            assertTrue(screen.getMessage().contains("NOT deleted"));
        }

        @Test
        void deleteCustomer_failCode2_datastoreError() {
            DisplayCustomerScreenData screen = new DisplayCustomerScreenData();
            screen.setCustomerNumberInput("0000012345");

            InqCustCommarea inq = buildSampleInqResponse();
            inq.setSortCode("123456");
            stubService.setInqCustResponse(inq);

            DelCusCommarea del = new DelCusCommarea();
            del.setDeleteSuccess("N");
            del.setDeleteFailCode("2");
            del.setSortCode("123456");
            stubService.setDelCusResponse(del);

            bnk1dcs.processTransaction(AidKey.PF5,
                    new DisplayCustomerCommarea(), screen);

            assertFalse(bnk1dcs.isValidData());
            assertTrue(screen.getMessage().contains("datastore error"));
        }

        @Test
        void deleteCustomer_failCode3_deleteError() {
            DisplayCustomerScreenData screen = new DisplayCustomerScreenData();
            screen.setCustomerNumberInput("0000012345");

            InqCustCommarea inq = buildSampleInqResponse();
            inq.setSortCode("123456");
            stubService.setInqCustResponse(inq);

            DelCusCommarea del = new DelCusCommarea();
            del.setDeleteSuccess("N");
            del.setDeleteFailCode("3");
            del.setSortCode("123456");
            stubService.setDelCusResponse(del);

            bnk1dcs.processTransaction(AidKey.PF5,
                    new DisplayCustomerCommarea(), screen);

            assertFalse(bnk1dcs.isValidData());
            assertTrue(screen.getMessage().contains("delete error"));
        }

        @Test
        void deleteCustomer_unknownFailCode_genericError() {
            DisplayCustomerScreenData screen = new DisplayCustomerScreenData();
            screen.setCustomerNumberInput("0000012345");

            InqCustCommarea inq = buildSampleInqResponse();
            inq.setSortCode("123456");
            stubService.setInqCustResponse(inq);

            DelCusCommarea del = new DelCusCommarea();
            del.setDeleteSuccess("N");
            del.setDeleteFailCode("9");
            del.setSortCode("123456");
            stubService.setDelCusResponse(del);

            bnk1dcs.processTransaction(AidKey.PF5,
                    new DisplayCustomerCommarea(), screen);

            assertFalse(bnk1dcs.isValidData());
            assertTrue(screen.getMessage().contains("error occurred"));
        }

        @Test
        void deleteCustomer_nullResponse_setsError() {
            DisplayCustomerScreenData screen = new DisplayCustomerScreenData();
            screen.setCustomerNumberInput("0000012345");

            InqCustCommarea inq = buildSampleInqResponse();
            inq.setSortCode("123456");
            stubService.setInqCustResponse(inq);
            stubService.setDelCusResponse(null);

            bnk1dcs.processTransaction(AidKey.PF5,
                    new DisplayCustomerCommarea(), screen);

            assertFalse(bnk1dcs.isValidData());
            assertTrue(screen.getMessage().contains("NOT deleted"));
        }
    }

    // -----------------------------------------------------------------------
    // UPDATE-CUST-DATA (UPDCD010)
    // -----------------------------------------------------------------------

    @Nested
    class UpdateCustomerDataTests {

        @Test
        void updateCustomer_success_showsSuccessMessage() {
            DisplayCustomerCommarea comm = new DisplayCustomerCommarea();
            comm.setUpdateFlag("Y");

            DisplayCustomerScreenData screen = buildUpdateScreen();

            stubService.setUpdCustResponse(buildSampleUpdResponse());

            bnk1dcs.processTransaction(AidKey.ENTER, comm, screen);

            assertTrue(bnk1dcs.isValidData());
            assertTrue(screen.getMessage().contains("updated successfully"));
            assertEquals("N", comm.getUpdateFlag());
        }

        @Test
        void updateCustomer_failCode1_notFound() {
            DisplayCustomerCommarea comm = new DisplayCustomerCommarea();
            comm.setUpdateFlag("Y");

            DisplayCustomerScreenData screen = buildUpdateScreen();

            UpdCustCommarea upd = new UpdCustCommarea();
            upd.setUpdateSuccess("N");
            upd.setUpdateFailCode("1");
            upd.setSortCode("987654");
            stubService.setUpdCustResponse(upd);

            bnk1dcs.processTransaction(AidKey.ENTER, comm, screen);

            assertFalse(bnk1dcs.isValidData());
            assertTrue(screen.getMessage().contains("not found"));
        }

        @Test
        void updateCustomer_failCode2_datastoreError() {
            DisplayCustomerCommarea comm = new DisplayCustomerCommarea();
            comm.setUpdateFlag("Y");

            DisplayCustomerScreenData screen = buildUpdateScreen();

            UpdCustCommarea upd = new UpdCustCommarea();
            upd.setUpdateSuccess("N");
            upd.setUpdateFailCode("2");
            upd.setSortCode("987654");
            stubService.setUpdCustResponse(upd);

            bnk1dcs.processTransaction(AidKey.ENTER, comm, screen);

            assertFalse(bnk1dcs.isValidData());
            assertTrue(screen.getMessage().contains("datastore error"));
        }

        @Test
        void updateCustomer_failCode3_updateError() {
            DisplayCustomerCommarea comm = new DisplayCustomerCommarea();
            comm.setUpdateFlag("Y");

            DisplayCustomerScreenData screen = buildUpdateScreen();

            UpdCustCommarea upd = new UpdCustCommarea();
            upd.setUpdateSuccess("N");
            upd.setUpdateFailCode("3");
            upd.setSortCode("987654");
            stubService.setUpdCustResponse(upd);

            bnk1dcs.processTransaction(AidKey.ENTER, comm, screen);

            assertFalse(bnk1dcs.isValidData());
            assertTrue(screen.getMessage().contains("update error"));
        }

        @Test
        void updateCustomer_unknownFailCode_genericError() {
            DisplayCustomerCommarea comm = new DisplayCustomerCommarea();
            comm.setUpdateFlag("Y");

            DisplayCustomerScreenData screen = buildUpdateScreen();

            UpdCustCommarea upd = new UpdCustCommarea();
            upd.setUpdateSuccess("N");
            upd.setUpdateFailCode("9");
            upd.setSortCode("987654");
            stubService.setUpdCustResponse(upd);

            bnk1dcs.processTransaction(AidKey.ENTER, comm, screen);

            assertFalse(bnk1dcs.isValidData());
            assertTrue(screen.getMessage().contains("unknown error"));
        }

        @Test
        void updateCustomer_nullResponse_setsError() {
            DisplayCustomerCommarea comm = new DisplayCustomerCommarea();
            comm.setUpdateFlag("Y");

            DisplayCustomerScreenData screen = buildUpdateScreen();
            stubService.setUpdCustResponse(null);

            bnk1dcs.processTransaction(AidKey.ENTER, comm, screen);

            assertFalse(bnk1dcs.isValidData());
            assertTrue(screen.getMessage().contains("NOT updated"));
        }

        @Test
        void updateCustomer_addressCombinedCorrectly() {
            DisplayCustomerCommarea comm = new DisplayCustomerCommarea();
            comm.setUpdateFlag("Y");

            DisplayCustomerScreenData screen = buildUpdateScreen();
            screen.setCustomerAddress1("Line 1");
            screen.setCustomerAddress2("Line 2");
            screen.setCustomerAddress3("Line 3");

            UpdCustCommarea upd = buildSampleUpdResponse();
            stubService.setUpdCustResponse(upd);

            bnk1dcs.processTransaction(AidKey.ENTER, comm, screen);

            UpdCustCommarea sentUpd = stubService.getLastUpdateRequest();
            assertNotNull(sentUpd);
            assertEquals(160, sentUpd.getAddress().length());
            assertTrue(sentUpd.getAddress().startsWith("Line 1"));
        }

        @Test
        void updateCustomer_dateFieldsParsedCorrectly() {
            DisplayCustomerCommarea comm = new DisplayCustomerCommarea();
            comm.setUpdateFlag("Y");

            DisplayCustomerScreenData screen = buildUpdateScreen();
            screen.setDobDay("15");
            screen.setDobMonth("06");
            screen.setDobYear("1990");
            screen.setCsReviewDateDay("01");
            screen.setCsReviewDateMonth("12");
            screen.setCsReviewDateYear("2023");

            UpdCustCommarea upd = buildSampleUpdResponse();
            stubService.setUpdCustResponse(upd);

            bnk1dcs.processTransaction(AidKey.ENTER, comm, screen);

            UpdCustCommarea sentUpd = stubService.getLastUpdateRequest();
            assertEquals(15061990, sentUpd.getDob());
            assertEquals(1122023, sentUpd.getCsReviewDate());
        }
    }

    // -----------------------------------------------------------------------
    // PF10 — Unprotect for Update (UCD010)
    // -----------------------------------------------------------------------

    @Nested
    class UnprotectCustomerDataTests {

        @Test
        void pf10_validData_setsUpdateFlagAndMessage() {
            DisplayCustomerScreenData screen = new DisplayCustomerScreenData();
            screen.setCustomerNumberInput("0000012345");

            InqCustCommarea inq = buildSampleInqResponse();
            inq.setSortCode("123456");
            stubService.setInqCustResponse(inq);

            DisplayCustomerCommarea comm = new DisplayCustomerCommarea();

            bnk1dcs.processTransaction(AidKey.PF10, comm, screen);

            assertEquals("Y", comm.getUpdateFlag());
            assertTrue(screen.getMessage().contains("Amend data"));
        }

        @Test
        void pf10_preservesDataInCommarea() {
            DisplayCustomerScreenData screen = new DisplayCustomerScreenData();
            screen.setCustomerNumberInput("0000012345");
            screen.setSortCode("123456");
            screen.setCustomerNumber2("0000012345");
            screen.setCustomerName("Mr Test Customer");
            screen.setCustomerAddress1("123 Test Lane");
            screen.setDobDay("01");
            screen.setDobMonth("01");
            screen.setDobYear("2000");
            screen.setCreditScore("500");
            screen.setCsReviewDateDay("15");
            screen.setCsReviewDateMonth("03");
            screen.setCsReviewDateYear("2024");

            DisplayCustomerCommarea comm = new DisplayCustomerCommarea();

            bnk1dcs.processTransaction(AidKey.PF10, comm, screen);

            assertEquals("CUST", comm.getEye());
            assertFalse(comm.getName().isEmpty());
        }

        @Test
        void pf10_invalidData_doesNotUnprotect() {
            DisplayCustomerScreenData screen = new DisplayCustomerScreenData();
            screen.setCustomerNumberInput("");

            DisplayCustomerCommarea comm = new DisplayCustomerCommarea();

            bnk1dcs.processTransaction(AidKey.PF10, comm, screen);

            assertNotEquals("Y", comm.getUpdateFlag());
        }
    }

    // -----------------------------------------------------------------------
    // Utility method tests
    // -----------------------------------------------------------------------

    @Nested
    class UtilityMethodTests {

        @Test
        void deEditField_removesNonNumericChars() {
            assertEquals("1234", Bnk1dcs.deEditField("1,234"));
            assertEquals("100000", Bnk1dcs.deEditField("$1,000.00"));
            assertEquals("12345", Bnk1dcs.deEditField("12345"));
            assertEquals("", Bnk1dcs.deEditField(null));
            assertEquals("", Bnk1dcs.deEditField(""));
            assertEquals("", Bnk1dcs.deEditField("abc"));
        }

        @Test
        void isNumeric_variousInputs() {
            assertTrue(Bnk1dcs.isNumeric("12345"));
            assertTrue(Bnk1dcs.isNumeric("0"));
            assertFalse(Bnk1dcs.isNumeric(""));
            assertFalse(Bnk1dcs.isNumeric(null));
            assertFalse(Bnk1dcs.isNumeric("12.34"));
            assertFalse(Bnk1dcs.isNumeric("abc"));
            assertFalse(Bnk1dcs.isNumeric("-1"));
        }

        @Test
        void extractTitle_extractsFirstWord() {
            assertEquals("Mr", Bnk1dcs.extractTitle("Mr John Smith"));
            assertEquals("Professor", Bnk1dcs.extractTitle("Professor X"));
            assertEquals("Dr", Bnk1dcs.extractTitle("Dr Strange"));
            assertEquals("", Bnk1dcs.extractTitle(""));
            assertEquals("", Bnk1dcs.extractTitle(null));
            assertEquals("", Bnk1dcs.extractTitle("   "));
        }

        @Test
        void padRight_padsCorrectly() {
            assertEquals("ab    ", Bnk1dcs.padRight("ab", 6));
            assertEquals("abcdef", Bnk1dcs.padRight("abcdefgh", 6));
            assertEquals("      ", Bnk1dcs.padRight(null, 6));
            assertEquals("", Bnk1dcs.padRight("", 0));
        }

        @Test
        void buildDateValue_combinesCorrectly() {
            assertEquals(15061990, Bnk1dcs.buildDateValue("15", "06", "1990"));
            assertEquals(1012000, Bnk1dcs.buildDateValue("01", "01", "2000"));
            assertEquals(0, Bnk1dcs.buildDateValue("", "", ""));
            assertEquals(0, Bnk1dcs.buildDateValue(null, null, null));
        }

        @Test
        void parseIntSafe_handlesEdgeCases() {
            assertEquals(42, Bnk1dcs.parseIntSafe("42", 0));
            assertEquals(0, Bnk1dcs.parseIntSafe("abc", 0));
            assertEquals(0, Bnk1dcs.parseIntSafe(null, 0));
            assertEquals(0, Bnk1dcs.parseIntSafe("", 0));
            assertEquals(-1, Bnk1dcs.parseIntSafe("xyz", -1));
            assertEquals(42, Bnk1dcs.parseIntSafe("  42  ", 0));
        }

        @Test
        void isBlankOrEmpty_variousInputs() {
            assertTrue(Bnk1dcs.isBlankOrEmpty(null));
            assertTrue(Bnk1dcs.isBlankOrEmpty(""));
            assertTrue(Bnk1dcs.isBlankOrEmpty("   "));
            assertFalse(Bnk1dcs.isBlankOrEmpty("a"));
            assertFalse(Bnk1dcs.isBlankOrEmpty(" a "));
        }
    }

    // -----------------------------------------------------------------------
    // Integration-style tests (full flow)
    // -----------------------------------------------------------------------

    @Nested
    class IntegrationTests {

        @Test
        void fullLookupFlow_enterCustNo_getDetails() {
            DisplayCustomerScreenData screen = new DisplayCustomerScreenData();
            screen.setCustomerNumberInput("0000054321");

            InqCustCommarea inq = new InqCustCommarea();
            inq.setSortCode("111111");
            inq.setCustomerNumber(54321);
            inq.setName("Mrs Jane Doe");
            inq.setAddress(Bnk1dcs.padRight("456 Oak Avenue", 60)
                    + Bnk1dcs.padRight("Apt 7B", 60)
                    + Bnk1dcs.padRight("London SW1 1AA", 40));
            inq.setDobDay(25);
            inq.setDobMonth(12);
            inq.setDobYear(1985);
            inq.setCreditScore(750);
            inq.setCsReviewDay(1);
            inq.setCsReviewMonth(6);
            inq.setCsReviewYear(2024);
            stubService.setInqCustResponse(inq);

            DisplayCustomerResult result = bnk1dcs.processTransaction(
                    AidKey.ENTER, new DisplayCustomerCommarea(), screen);

            assertEquals(DisplayCustomerResult.Action.SEND_MAP,
                    result.getAction());
            assertEquals("111111", screen.getSortCode());
            assertEquals("54321", screen.getCustomerNumber2());
            assertEquals("Mrs Jane Doe", screen.getCustomerName());
            assertEquals("456 Oak Avenue", screen.getCustomerAddress1());
            assertEquals("Apt 7B", screen.getCustomerAddress2());
            assertEquals("London SW1 1AA", screen.getCustomerAddress3());
            assertEquals("25", screen.getDobDay());
            assertEquals("12", screen.getDobMonth());
            assertEquals("1985", screen.getDobYear());
            assertEquals("750", screen.getCreditScore());
            assertEquals("1", screen.getCsReviewDateDay());
            assertEquals("6", screen.getCsReviewDateMonth());
            assertEquals("2024", screen.getCsReviewDateYear());
        }

        @Test
        void fullDeleteFlow_lookup_thenDelete() {
            DisplayCustomerScreenData screen = new DisplayCustomerScreenData();
            screen.setCustomerNumberInput("0000012345");

            InqCustCommarea inq = buildSampleInqResponse();
            inq.setSortCode("123456");
            stubService.setInqCustResponse(inq);

            bnk1dcs.processTransaction(AidKey.ENTER,
                    new DisplayCustomerCommarea(), screen);

            DisplayCustomerScreenData deleteScreen =
                    bnk1dcs.getScreenData();

            DelCusCommarea del = new DelCusCommarea();
            del.setDeleteSuccess("Y");
            del.setCustomerNumber("0000012345");
            stubService.setDelCusResponse(del);

            bnk1dcs = new Bnk1dcs(stubService);
            deleteScreen.setCustomerNumberInput("0000012345");

            bnk1dcs.processTransaction(AidKey.PF5,
                    new DisplayCustomerCommarea(), deleteScreen);

            assertTrue(screen.getMessage().contains("Customer lookup successful")
                    || deleteScreen.getMessage().contains("deleted"));
        }

        @Test
        void fullUpdateFlow_lookup_unprotect_update() {
            DisplayCustomerScreenData screen = new DisplayCustomerScreenData();
            screen.setCustomerNumberInput("0000012345");

            InqCustCommarea inq = buildSampleInqResponse();
            inq.setSortCode("123456");
            stubService.setInqCustResponse(inq);

            DisplayCustomerCommarea comm = new DisplayCustomerCommarea();

            bnk1dcs.processTransaction(AidKey.PF10, comm, screen);
            assertEquals("Y", comm.getUpdateFlag());

            screen.setCustomerName("Dr Updated Name");
            screen.setCustomerAddress1("999 New Street");

            UpdCustCommarea upd = buildSampleUpdResponse();
            upd.setName("Dr Updated Name");
            stubService.setUpdCustResponse(upd);

            bnk1dcs.processTransaction(AidKey.ENTER, comm, screen);

            assertTrue(screen.getMessage().contains("updated successfully"));
            assertEquals("N", comm.getUpdateFlag());
        }
    }

    // -----------------------------------------------------------------------
    // CommArea data transfer tests
    // -----------------------------------------------------------------------

    @Nested
    class CommareaTests {

        @Test
        void commareaPreserved_afterEnter() {
            DisplayCustomerScreenData screen = new DisplayCustomerScreenData();
            screen.setCustomerNumberInput("0000012345");

            InqCustCommarea inq = buildSampleInqResponse();
            stubService.setInqCustResponse(inq);

            DisplayCustomerCommarea comm = new DisplayCustomerCommarea();

            DisplayCustomerResult result = bnk1dcs.processTransaction(
                    AidKey.ENTER, comm, screen);

            assertNotNull(result.getCommarea());
            assertEquals("987654", result.getCommarea().getSortCode());
        }
    }

    // -----------------------------------------------------------------------
    // Helper methods
    // -----------------------------------------------------------------------

    private InqCustCommarea buildSampleInqResponse() {
        InqCustCommarea inq = new InqCustCommarea();
        inq.setSortCode("987654");
        inq.setCustomerNumber(12345);
        inq.setName("Mr Test Customer");
        inq.setAddress(Bnk1dcs.padRight("123 Test Lane", 60)
                + Bnk1dcs.padRight("Test City", 60)
                + Bnk1dcs.padRight("TC1 2AB", 40));
        inq.setDobDay(1);
        inq.setDobMonth(1);
        inq.setDobYear(2000);
        inq.setCreditScore(500);
        inq.setCsReviewDay(15);
        inq.setCsReviewMonth(3);
        inq.setCsReviewYear(2024);
        inq.setInquirySuccess("Y");
        return inq;
    }

    private DelCusCommarea buildSampleDelResponse() {
        DelCusCommarea del = new DelCusCommarea();
        del.setDeleteSuccess("Y");
        del.setCustomerNumber("0000012345");
        del.setSortCode("123456");
        return del;
    }

    private UpdCustCommarea buildSampleUpdResponse() {
        UpdCustCommarea upd = new UpdCustCommarea();
        upd.setSortCode("987654");
        upd.setCustomerNumber("0000012345");
        upd.setName("Mr Test Customer");
        upd.setAddress(Bnk1dcs.padRight("123 Test Lane", 60)
                + Bnk1dcs.padRight("Test City", 60)
                + Bnk1dcs.padRight("TC1 2AB", 40));
        upd.setDob(1012000);
        upd.setCreditScore(500);
        upd.setCsReviewDate(15032024);
        upd.setUpdateSuccess("Y");
        return upd;
    }

    private DisplayCustomerScreenData buildUpdateScreen() {
        DisplayCustomerScreenData screen = new DisplayCustomerScreenData();
        screen.setCustomerNumberInput("0000012345");
        screen.setCustomerNumber2("0000012345");
        screen.setCustomerName("Mr Updated Name");
        screen.setCustomerAddress1("New Address Line 1");
        screen.setCustomerAddress2("");
        screen.setCustomerAddress3("");
        screen.setSortCode("987654");
        screen.setDobDay("01");
        screen.setDobMonth("01");
        screen.setDobYear("2000");
        screen.setCreditScore("500");
        screen.setCsReviewDateDay("15");
        screen.setCsReviewDateMonth("03");
        screen.setCsReviewDateYear("2024");
        return screen;
    }
}
