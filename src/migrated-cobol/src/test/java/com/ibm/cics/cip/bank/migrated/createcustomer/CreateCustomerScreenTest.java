/*
 *
 *    Copyright IBM Corp. 2023
 *
 */

package com.ibm.cics.cip.bank.migrated.createcustomer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class CreateCustomerScreenTest {

    private CreateCustomerScreen screen;
    private StubCustomerCreator stubCreator;

    @BeforeEach
    void setUp() {
        stubCreator = new StubCustomerCreator();
        screen = new CreateCustomerScreen(stubCreator);
    }

    private CreateCustomerInput validInput() {
        return new CreateCustomerInput(
                "Mr", "John", "A", "Smith",
                "123 High Street", "Apartment 4B", "London",
                "15", "06", "1990",
                null);
    }

    // -----------------------------------------------------------------------
    // EDIT-DATA validation tests — mirrors COBOL lines 576–934
    // -----------------------------------------------------------------------

    @Nested
    class EditDataValidation {

        @Test
        void shouldRejectWhenExistingCustomerNumberIsPresent() {
            CreateCustomerInput input = new CreateCustomerInput(
                    "Mr", "John", "A", "Smith",
                    "123 High Street", null, null,
                    "15", "06", "1990",
                    "0000012345");

            ValidationError error = screen.editData(input);
            assertNotNull(error);
            assertEquals(ValidationError.ScreenField.EXISTING_CUSTOMER_NUMBER,
                    error.field());
            assertTrue(error.message().contains("clear screen"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "____", "__________"})
        void shouldRejectBlankOrUnderscoreTitle(String title) {
            CreateCustomerInput input = new CreateCustomerInput(
                    title, "John", "A", "Smith",
                    "123 High Street", null, null,
                    "15", "06", "1990", null);

            ValidationError error = screen.editData(input);
            assertNotNull(error);
            assertEquals(ValidationError.ScreenField.TITLE, error.field());
            assertTrue(error.message().contains("Valid titles"));
        }

        @Test
        void shouldRejectInvalidTitle() {
            CreateCustomerInput input = new CreateCustomerInput(
                    "King", "John", "A", "Smith",
                    "123 High Street", null, null,
                    "15", "06", "1990", null);

            ValidationError error = screen.editData(input);
            assertNotNull(error);
            assertEquals(ValidationError.ScreenField.TITLE, error.field());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "____________________"})
        void shouldRejectBlankFirstName(String firstName) {
            CreateCustomerInput input = new CreateCustomerInput(
                    "Mr", firstName, "A", "Smith",
                    "123 High Street", null, null,
                    "15", "06", "1990", null);

            ValidationError error = screen.editData(input);
            assertNotNull(error);
            assertEquals(ValidationError.ScreenField.FIRST_NAME, error.field());
            assertTrue(error.message().contains("First Name"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "____________________"})
        void shouldRejectBlankSurname(String surname) {
            CreateCustomerInput input = new CreateCustomerInput(
                    "Mr", "John", "A", surname,
                    "123 High Street", null, null,
                    "15", "06", "1990", null);

            ValidationError error = screen.editData(input);
            assertNotNull(error);
            assertEquals(ValidationError.ScreenField.LAST_NAME, error.field());
            assertTrue(error.message().contains("Surname"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "_test"})
        void shouldRejectInvalidAddressLine1(String addr1) {
            CreateCustomerInput input = new CreateCustomerInput(
                    "Mr", "John", "A", "Smith",
                    addr1, null, null,
                    "15", "06", "1990", null);

            ValidationError error = screen.editData(input);
            assertNotNull(error);
            assertEquals(ValidationError.ScreenField.ADDRESS_LINE_1,
                    error.field());
            assertTrue(error.message().contains("Address Line 1"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"__"})
        void shouldRejectBlankDobDay(String day) {
            CreateCustomerInput input = new CreateCustomerInput(
                    "Mr", "John", "A", "Smith",
                    "123 High Street", null, null,
                    day, "06", "1990", null);

            ValidationError error = screen.editData(input);
            assertNotNull(error);
            assertEquals(ValidationError.ScreenField.DOB_DAY, error.field());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"__"})
        void shouldRejectBlankDobMonth(String month) {
            CreateCustomerInput input = new CreateCustomerInput(
                    "Mr", "John", "A", "Smith",
                    "123 High Street", null, null,
                    "15", month, "1990", null);

            ValidationError error = screen.editData(input);
            assertNotNull(error);
            assertEquals(ValidationError.ScreenField.DOB_MONTH, error.field());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"____", "19", "1"})
        void shouldRejectInvalidDobYear(String year) {
            CreateCustomerInput input = new CreateCustomerInput(
                    "Mr", "John", "A", "Smith",
                    "123 High Street", null, null,
                    "15", "06", year, null);

            ValidationError error = screen.editData(input);
            assertNotNull(error);
            assertEquals(ValidationError.ScreenField.DOB_YEAR, error.field());
        }

        @Test
        void shouldRejectNonNumericDobDay() {
            CreateCustomerInput input = new CreateCustomerInput(
                    "Mr", "John", "A", "Smith",
                    "123 High Street", null, null,
                    "AB", "06", "1990", null);

            ValidationError error = screen.editData(input);
            assertNotNull(error);
            assertEquals(ValidationError.ScreenField.DOB_DAY, error.field());
            assertTrue(error.message().contains("Non numeric"));
        }

        @Test
        void shouldRejectNonNumericDobMonth() {
            CreateCustomerInput input = new CreateCustomerInput(
                    "Mr", "John", "A", "Smith",
                    "123 High Street", null, null,
                    "15", "XY", "1990", null);

            ValidationError error = screen.editData(input);
            assertNotNull(error);
            assertEquals(ValidationError.ScreenField.DOB_MONTH, error.field());
            assertTrue(error.message().contains("Non numeric"));
        }

        @Test
        void shouldRejectNonNumericDobYear() {
            CreateCustomerInput input = new CreateCustomerInput(
                    "Mr", "John", "A", "Smith",
                    "123 High Street", null, null,
                    "15", "06", "ABCD", null);

            ValidationError error = screen.editData(input);
            assertNotNull(error);
            assertEquals(ValidationError.ScreenField.DOB_YEAR, error.field());
            assertTrue(error.message().contains("Non numeric"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"00", "32", "99"})
        void shouldRejectDobDayOutOfRange(String day) {
            CreateCustomerInput input = new CreateCustomerInput(
                    "Mr", "John", "A", "Smith",
                    "123 High Street", null, null,
                    day, "06", "1990", null);

            ValidationError error = screen.editData(input);
            assertNotNull(error);
            assertEquals(ValidationError.ScreenField.DOB_DAY, error.field());
            assertTrue(error.message().contains("valid Date of Birth (DD)"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"00", "13", "99"})
        void shouldRejectDobMonthOutOfRange(String month) {
            CreateCustomerInput input = new CreateCustomerInput(
                    "Mr", "John", "A", "Smith",
                    "123 High Street", null, null,
                    "15", month, "1990", null);

            ValidationError error = screen.editData(input);
            assertNotNull(error);
            assertEquals(ValidationError.ScreenField.DOB_MONTH, error.field());
            assertTrue(error.message().contains("valid Date of Birth (MM)"));
        }

        @Test
        void shouldAcceptValidInput() {
            ValidationError error = screen.editData(validInput());
            assertNull(error);
        }

        @ParameterizedTest
        @CsvSource({
                "01, 01, 2000",
                "31, 12, 1950",
                "15, 06, 1990",
                " 1, 1,  2000"
        })
        void shouldAcceptValidDobBoundaries(String day, String month,
                                            String year) {
            CreateCustomerInput input = new CreateCustomerInput(
                    "Mr", "John", "A", "Smith",
                    "123 High Street", null, null,
                    day, month, year, null);

            assertNull(screen.editData(input));
        }
    }

    // -----------------------------------------------------------------------
    // Title normalisation tests — mirrors COBOL lines 604–781
    // -----------------------------------------------------------------------

    @Nested
    class TitleNormalisation {

        @ParameterizedTest
        @CsvSource({
                "MR________,    Mr",
                "Mr________,    Mr",
                "MR        ,    Mr",
                "Mr         ,   Mr",
                "mr________,    Mr",
                "MRS_______,    Mrs",
                "Mrs_______,    Mrs",
                "MRS       ,    Mrs",
                "Mrs       ,    Mrs",
                "mrs_______,    Mrs",
                "MISS______,    Miss",
                "Miss______,    Miss",
                "MISS      ,    Miss",
                "Miss      ,    Miss",
                "miss______,    Miss",
                "MS________,    Ms",
                "Ms________,    Ms",
                "MS        ,    Ms",
                "Ms        ,    Ms",
                "ms________,    Ms",
                "ms        ,    Ms",
                "DR________,    Dr",
                "Dr________,    Dr",
                "DR        ,    Dr",
                "Dr        ,    Dr",
                "dr        ,    Dr",
                "dr________,    Dr",
                "DRS_______,    Drs",
                "Drs_______,    Drs",
                "DRS       ,    Drs",
                "Drs       ,    Drs",
                "drs       ,    Drs",
                "drs_______,    Drs",
                "PROFESSOR_,    Professor",
                "Professor_,    Professor",
                "PROFESSOR ,    Professor",
                "Professor ,    Professor",
                "LORD______,    Lord",
                "Lord______,    Lord",
                "LORD      ,    Lord",
                "Lord      ,    Lord",
                "lord      ,    Lord",
                "lord______,    Lord",
                "LADY______,    Lady",
                "Lady______,    Lady",
                "LADY      ,    Lady",
                "Lady      ,    Lady",
                "lady      ,    Lady",
                "lady______,    Lady",
                "SIR_______,    Sir",
                "Sir_______,    Sir",
                "SIR       ,    Sir",
                "Sir       ,    Sir",
                "sir       ,    Sir",
                "sir_______,    Sir"
        })
        void shouldNormaliseTitlesMatchingCobolEvaluateBlock(String input,
                                                              String expected) {
            assertEquals(expected, screen.normaliseTitle(input));
        }

        @Test
        void shouldReturnNullForInvalidTitle() {
            assertNull(screen.normaliseTitle("King"));
            assertNull(screen.normaliseTitle("Baron"));
        }
    }

    // -----------------------------------------------------------------------
    // Name assembly tests — mirrors COBOL CRE-CUST-DATA lines 949–961
    // -----------------------------------------------------------------------

    @Nested
    class NameAssembly {

        @Test
        void shouldAssembleFullNameWithInitials() {
            String name = screen.assembleName("Mr", "John", "A", "Smith");
            assertEquals("Mr John A Smith", name);
        }

        @Test
        void shouldAssembleNameWithoutInitials() {
            String name = screen.assembleName("Mrs", "Jane", "", "Doe");
            assertEquals("Mrs Jane Doe", name);
        }

        @Test
        void shouldAssembleNameWithNullInitials() {
            String name = screen.assembleName("Dr", "Alice", null,
                    "Wonderland");
            assertEquals("Dr Alice Wonderland", name);
        }

        @Test
        void shouldReplaceUnderscoresInNameFields() {
            String name = screen.assembleName(
                    "Mr________", "John____", "A_", "Smith_______________");
            assertEquals("Mr John A Smith", name.stripTrailing());
        }

        @Test
        void shouldTruncateNameTo60Characters() {
            String longFirst = "A".repeat(40);
            String longLast = "B".repeat(30);
            String name = screen.assembleName("Professor", longFirst, "XY",
                    longLast);
            assertEquals(60, name.length());
        }
    }

    // -----------------------------------------------------------------------
    // Address assembly tests — mirrors COBOL CRE-CUST-DATA lines 963–970
    // -----------------------------------------------------------------------

    @Nested
    class AddressAssembly {

        @Test
        void shouldAssembleThreeLineAddress() {
            String address = screen.assembleAddress(
                    "123 High Street", "Apartment 4B", "London");
            assertEquals(160, address.length());
            assertTrue(address.startsWith("123 High Street"));
        }

        @Test
        void shouldHandleNullAddressLines() {
            String address = screen.assembleAddress(
                    "123 Main St", null, null);
            assertEquals(160, address.length());
            assertTrue(address.startsWith("123 Main St"));
        }

        @Test
        void shouldReplaceUnderscoresInAddress() {
            String address = screen.assembleAddress(
                    "123_High_Street", "Apt_4B", "London");
            assertEquals(160, address.length());
            assertTrue(address.startsWith("123 High Street"));
        }

        @Test
        void shouldRoundTripAddressThroughSplit() {
            String line1 = "123 High Street";
            String line2 = "Apartment 4B";
            String line3 = "London";
            String assembled = screen.assembleAddress(line1, line2, line3);
            String[] split = screen.splitAddress(assembled);
            assertEquals(3, split.length);
            assertTrue(split[0].startsWith(line1));
            assertTrue(split[1].startsWith(line2));
            assertTrue(split[2].startsWith(line3));
            assertEquals(60, split[0].length());
            assertEquals(60, split[1].length());
            assertEquals(40, split[2].length());
        }
    }

    // -----------------------------------------------------------------------
    // CS Review Date split tests — mirrors COBOL lines 1088–1093
    // -----------------------------------------------------------------------

    @Nested
    class CsReviewDateSplit {

        @Test
        void shouldSplitValidDate() {
            String[] parts = screen.splitCsReviewDate("15062023");
            assertArrayEquals(new String[]{"15", "06", "2023"}, parts);
        }

        @Test
        void shouldHandleNullDate() {
            String[] parts = screen.splitCsReviewDate(null);
            assertArrayEquals(new String[]{"  ", "  ", "    "}, parts);
        }
    }

    // -----------------------------------------------------------------------
    // Result interpretation tests — mirrors COBOL CRE-CUST-DATA lines 1048–1075
    // -----------------------------------------------------------------------

    @Nested
    class ResultInterpretation {

        @Test
        void shouldReturnSuccessMessage() {
            CustomerCreationResult result = new CustomerCreationResult(
                    true, null, "987654", "0000012345", 750,
                    "15062023", 15, 6, 1990,
                    "123 High Street" + " ".repeat(145));
            assertEquals("The Customer record has been successfully created",
                    screen.interpretResultMessage(result));
        }

        @Test
        void shouldReturnTooOldMessage() {
            CustomerCreationResult result = new CustomerCreationResult(
                    false, "O", "", "", 0, "00000000",
                    1, 1, 1900, " ".repeat(160));
            assertEquals(
                    "Sorry, customer is too old. Please check D.O.B.",
                    screen.interpretResultMessage(result));
        }

        @Test
        void shouldReturnFutureDobMessage() {
            CustomerCreationResult result = new CustomerCreationResult(
                    false, "Y", "", "", 0, "00000000",
                    1, 1, 2099, " ".repeat(160));
            assertEquals(
                    "Sorry, customer D.O.B. is in the future.",
                    screen.interpretResultMessage(result));
        }

        @Test
        void shouldReturnInvalidDobMessage() {
            CustomerCreationResult result = new CustomerCreationResult(
                    false, "Z", "", "", 0, "00000000",
                    30, 2, 1990, " ".repeat(160));
            assertEquals("Sorry, customer D.O.B. is invalid.",
                    screen.interpretResultMessage(result));
        }

        @Test
        void shouldReturnGenericFailureForUnknownCode() {
            CustomerCreationResult result = new CustomerCreationResult(
                    false, "X", "", "", 0, "00000000",
                    1, 1, 1990, " ".repeat(160));
            assertEquals(
                    "Sorry but unable to create Customer record",
                    screen.interpretResultMessage(result));
        }
    }

    // -----------------------------------------------------------------------
    // Full processMap integration tests
    // -----------------------------------------------------------------------

    @Nested
    class ProcessMap {

        @Test
        void shouldReturnSuccessResponseForValidInput() {
            stubCreator.setResult(new CustomerCreationResult(
                    true, null, "987654", "0000012345", 750,
                    "15062023", 15, 6, 1990,
                    "123 High Street" + " ".repeat(145)));

            CreateCustomerResponse response = screen.processMap(validInput());
            assertTrue(response.success());
            assertEquals("987654", response.sortCode());
            assertEquals("0000012345", response.customerNumber());
            assertTrue(response.message().contains("successfully created"));
            assertEquals("750", response.creditScore());
            assertEquals("15", response.csReviewDateDay());
            assertEquals("06", response.csReviewDateMonth());
            assertEquals("2023", response.csReviewDateYear());
        }

        @Test
        void shouldReturnValidationErrorWithoutCallingCreator() {
            CreateCustomerInput badInput = new CreateCustomerInput(
                    null, "John", "A", "Smith",
                    "123 High Street", null, null,
                    "15", "06", "1990", null);

            CreateCustomerResponse response = screen.processMap(badInput);
            assertFalse(response.success());
            assertFalse(stubCreator.wasCalled());
        }

        @Test
        void shouldReturnFailureResponseWhenCreationFails() {
            stubCreator.setResult(new CustomerCreationResult(
                    false, "O", "", "", 0, "00000000",
                    15, 6, 1990, " ".repeat(160)));

            CreateCustomerResponse response = screen.processMap(validInput());
            assertFalse(response.success());
            assertTrue(response.message().contains("too old"));
            assertEquals("", response.sortCode());
            assertEquals("", response.customerNumber());
        }

        @Test
        void shouldPassCorrectRequestToCreator() {
            stubCreator.setResult(new CustomerCreationResult(
                    true, null, "987654", "0000012345", 750,
                    "15062023", 15, 6, 1990,
                    "123 High Street" + " ".repeat(145)));

            screen.processMap(validInput());
            assertTrue(stubCreator.wasCalled());

            CustomerCreationRequest captured = stubCreator.getCapturedRequest();
            assertEquals("CUST", captured.eyecatcher());
            assertTrue(captured.name().startsWith("Mr John A Smith"));
            assertEquals(160, captured.address().length());
            assertEquals(15, captured.birthDay());
            assertEquals(6, captured.birthMonth());
            assertEquals(1990, captured.birthYear());
        }

        @Test
        void shouldHandleAllTitleVariantsInFullFlow() {
            stubCreator.setResult(new CustomerCreationResult(
                    true, null, "987654", "0000012345", 800,
                    "01012024", 1, 1, 2000,
                    " ".repeat(160)));

            for (CustomerTitle title : CustomerTitle.values()) {
                CreateCustomerInput input = new CreateCustomerInput(
                        title.displayName(), "Test", null, "User",
                        "1 Test Road", null, null,
                        "01", "01", "2000", null);

                CreateCustomerResponse response = screen.processMap(input);
                assertTrue(response.success(),
                        "Should succeed for title: " + title.displayName());
            }
        }

        @Test
        void shouldHandleOptionalAddressLines() {
            stubCreator.setResult(new CustomerCreationResult(
                    true, null, "111111", "0000099999", 600,
                    "20032024", 10, 3, 1985,
                    "Only Line 1" + " ".repeat(149)));

            CreateCustomerInput input = new CreateCustomerInput(
                    "Ms", "Alice", null, "Wonderland",
                    "42 Rabbit Hole Lane", null, null,
                    "10", "03", "1985", null);

            CreateCustomerResponse response = screen.processMap(input);
            assertTrue(response.success());
        }

        @Test
        void shouldPreserveAddressSplitInResponse() {
            String addr = padRight("10 Downing Street", 60)
                    + padRight("Westminster", 60)
                    + padRight("London", 40);

            stubCreator.setResult(new CustomerCreationResult(
                    true, null, "123456", "0000054321", 900,
                    "01012025", 25, 12, 1980,
                    addr));

            CreateCustomerResponse response = screen.processMap(validInput());
            assertTrue(response.addressLine1().startsWith("10 Downing Street"));
            assertTrue(response.addressLine2().startsWith("Westminster"));
            assertTrue(response.addressLine3().startsWith("London"));
        }
    }

    // -----------------------------------------------------------------------
    // Edge case tests
    // -----------------------------------------------------------------------

    @Nested
    class EdgeCases {

        @Test
        void shouldRejectExistingCustomerNumberWithUnderscores() {
            CreateCustomerInput input = new CreateCustomerInput(
                    "Mr", "John", "A", "Smith",
                    "123 High Street", null, null,
                    "15", "06", "1990",
                    "123");

            ValidationError error = screen.editData(input);
            assertNotNull(error);
            assertEquals(
                    ValidationError.ScreenField.EXISTING_CUSTOMER_NUMBER,
                    error.field());
        }

        @Test
        void shouldAcceptDobDayBoundaries() {
            CreateCustomerInput day1 = new CreateCustomerInput(
                    "Mr", "John", "A", "Smith",
                    "123 High Street", null, null,
                    "01", "06", "1990", null);
            assertNull(screen.editData(day1));

            CreateCustomerInput day31 = new CreateCustomerInput(
                    "Mr", "John", "A", "Smith",
                    "123 High Street", null, null,
                    "31", "06", "1990", null);
            assertNull(screen.editData(day31));
        }

        @Test
        void shouldAcceptDobMonthBoundaries() {
            CreateCustomerInput month1 = new CreateCustomerInput(
                    "Mr", "John", "A", "Smith",
                    "123 High Street", null, null,
                    "15", "01", "1990", null);
            assertNull(screen.editData(month1));

            CreateCustomerInput month12 = new CreateCustomerInput(
                    "Mr", "John", "A", "Smith",
                    "123 High Street", null, null,
                    "15", "12", "1990", null);
            assertNull(screen.editData(month12));
        }

        @Test
        void shouldHandleAddressExactlyAtMaxLengths() {
            String line1 = "A".repeat(60);
            String line2 = "B".repeat(60);
            String line3 = "C".repeat(40);
            String assembled = screen.assembleAddress(line1, line2, line3);
            assertEquals(160, assembled.length());
            assertEquals(line1, assembled.substring(0, 60));
            assertEquals(line2, assembled.substring(60, 120));
            assertEquals(line3, assembled.substring(120, 160));
        }

        @Test
        void shouldTruncateAddressLinesExceedingMaxLength() {
            String line1 = "X".repeat(70);
            String assembled = screen.assembleAddress(line1, "", "");
            assertEquals(160, assembled.length());
            assertEquals("X".repeat(60), assembled.substring(0, 60));
        }
    }

    // --- Test helpers ---

    private static String padRight(String value, int length) {
        if (value.length() >= length) {
            return value.substring(0, length);
        }
        return value + " ".repeat(length - value.length());
    }

    private static class StubCustomerCreator implements CustomerCreator {
        private CustomerCreationResult result;
        private CustomerCreationRequest capturedRequest;
        private boolean called;

        void setResult(CustomerCreationResult result) {
            this.result = result;
        }

        boolean wasCalled() {
            return called;
        }

        CustomerCreationRequest getCapturedRequest() {
            return capturedRequest;
        }

        @Override
        public CustomerCreationResult createCustomer(
                CustomerCreationRequest request) {
            called = true;
            capturedRequest = request;
            return result;
        }
    }
}
