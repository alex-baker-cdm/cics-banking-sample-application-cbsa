/*
 * Copyright IBM Corp. 2023
 *
 * Comprehensive tests for the BNK1UAC COBOL-to-Java migration.
 * Each test maps to a specific section or behavior in the original COBOL program.
 */
package com.ibm.cics.cip.bank.migrated.bnk1uac;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Bnk1uacTest {

    private Bnk1uac bnk1uac;
    private StubAccountService stubService;

    @BeforeEach
    void setUp() {
        stubService = new StubAccountService();
        bnk1uac = new Bnk1uac(stubService);
    }

    // ---------------------------------------------------------------
    // PREMIERE SECTION — handleTransaction()
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("PREMIERE SECTION — handleTransaction()")
    class PremiereSection {

        @Test
        @DisplayName("First time through (EIBCALEN=0) sends empty map with ERASE")
        void firstTimeThrough() {
            var result = bnk1uac.handleTransaction(
                    Bnk1uac.AidKey.ENTER, true, null);

            assertInstanceOf(Bnk1uac.TransactionResult.SendMap.class, result);
            var sendMap = (Bnk1uac.TransactionResult.SendMap) result;
            assertEquals(Bnk1uac.SendMode.ERASE, sendMap.sendMode());
        }

        @Test
        @DisplayName("PA key pressed — continue (no action)")
        void paKey() {
            var result = bnk1uac.handleTransaction(
                    Bnk1uac.AidKey.PA, false, null);
            assertInstanceOf(Bnk1uac.TransactionResult.Continue.class, result);
        }

        @Test
        @DisplayName("PF3 returns to main menu with OMEN transaction")
        void pf3ReturnsToMenu() {
            var result = bnk1uac.handleTransaction(
                    Bnk1uac.AidKey.PF3, false, null);

            assertInstanceOf(Bnk1uac.TransactionResult.ReturnToMenu.class, result);
            var menu = (Bnk1uac.TransactionResult.ReturnToMenu) result;
            assertEquals("OMEN", menu.transactionId());
        }

        @Test
        @DisplayName("PF12 sends session termination message")
        void pf12TerminatesSession() {
            var result = bnk1uac.handleTransaction(
                    Bnk1uac.AidKey.PF12, false, null);

            assertInstanceOf(Bnk1uac.TransactionResult.SessionTerminated.class, result);
            var term = (Bnk1uac.TransactionResult.SessionTerminated) result;
            assertEquals("Session Ended", term.message());
        }

        @Test
        @DisplayName("CLEAR key clears the screen")
        void clearKey() {
            var result = bnk1uac.handleTransaction(
                    Bnk1uac.AidKey.CLEAR, false, null);
            assertInstanceOf(Bnk1uac.TransactionResult.ScreenCleared.class, result);
        }

        @Test
        @DisplayName("OTHER key sends invalid key message with alarm")
        void otherKey() {
            var result = bnk1uac.handleTransaction(
                    Bnk1uac.AidKey.OTHER, false, null);

            assertInstanceOf(Bnk1uac.TransactionResult.SendMap.class, result);
            var sendMap = (Bnk1uac.TransactionResult.SendMap) result;
            assertEquals(Bnk1uac.SendMode.DATA_ONLY_ALARM, sendMap.sendMode());
            assertEquals("Invalid key pressed.", sendMap.output().getMessage());
        }
    }

    // ---------------------------------------------------------------
    // EDIT-DATA SECTION — editData()
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("EDIT-DATA SECTION — editData()")
    class EditDataSection {

        @Test
        @DisplayName("Valid numeric account number passes")
        void validAccountNumber() {
            var input = createInput();
            input.setAccountNumber(new ScreenField("12345678"));
            assertNull(bnk1uac.editData(input));
        }

        @Test
        @DisplayName("Non-numeric account number fails")
        void nonNumericAccountNumber() {
            var input = createInput();
            input.setAccountNumber(new ScreenField("ABCD1234"));
            assertEquals("Please enter an account number.", bnk1uac.editData(input));
        }

        @Test
        @DisplayName("Empty account number fails")
        void emptyAccountNumber() {
            var input = createInput();
            input.setAccountNumber(new ScreenField(""));
            assertEquals("Please enter an account number.", bnk1uac.editData(input));
        }

        @Test
        @DisplayName("Account number with spaces fails")
        void accountNumberWithSpaces() {
            var input = createInput();
            input.setAccountNumber(new ScreenField("1234 678"));
            assertEquals("Please enter an account number.", bnk1uac.editData(input));
        }
    }

    // ---------------------------------------------------------------
    // VALIDATE-DATA SECTION — validateData() & sub-validators
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("VALIDATE-DATA SECTION — Account Type Validation")
    class AccountTypeValidation {

        @Test
        @DisplayName("CURRENT is a valid account type")
        void currentIsValid() {
            assertNull(bnk1uac.validateAccountType(
                    inputWithAccountType("CURRENT ")));
        }

        @Test
        @DisplayName("SAVING is a valid account type")
        void savingIsValid() {
            assertNull(bnk1uac.validateAccountType(
                    inputWithAccountType("SAVING  ")));
        }

        @Test
        @DisplayName("LOAN is a valid account type")
        void loanIsValid() {
            assertNull(bnk1uac.validateAccountType(
                    inputWithAccountType("LOAN    ")));
        }

        @Test
        @DisplayName("MORTGAGE is a valid account type")
        void mortgageIsValid() {
            assertNull(bnk1uac.validateAccountType(
                    inputWithAccountType("MORTGAGE")));
        }

        @Test
        @DisplayName("ISA is a valid account type")
        void isaIsValid() {
            assertNull(bnk1uac.validateAccountType(
                    inputWithAccountType("ISA     ")));
        }

        @Test
        @DisplayName("INVALID type is rejected")
        void invalidTypeRejected() {
            String result = bnk1uac.validateAccountType(
                    inputWithAccountType("INVALID "));
            assertNotNull(result);
            assertTrue(result.contains("Account Type must be"));
        }

        @Test
        @DisplayName("Null account type is rejected")
        void nullTypeRejected() {
            var input = createInput();
            input.setAccountType(new ScreenField(null, 0));
            assertNotNull(bnk1uac.validateAccountType(input));
        }
    }

    @Nested
    @DisplayName("VALIDATE-DATA SECTION — Interest Rate Validation")
    class InterestRateValidation {

        @Test
        @DisplayName("Zero-length interest rate rejected")
        void zeroLengthRejected() {
            var input = createValidInput();
            input.setInterestRate(new ScreenField("", 0));
            String msg = bnk1uac.validateInterestRate(input);
            assertNotNull(msg);
            assertTrue(msg.contains("numeric interest rate"));
        }

        @Test
        @DisplayName("Valid integer interest rate accepted")
        void validInteger() {
            var input = createValidInput();
            input.setInterestRate(new ScreenField("5", 1));
            assertNull(bnk1uac.validateInterestRate(input));
        }

        @Test
        @DisplayName("Valid decimal interest rate accepted")
        void validDecimal() {
            var input = createValidInput();
            input.setInterestRate(new ScreenField("5.25", 4));
            assertNull(bnk1uac.validateInterestRate(input));
        }

        @Test
        @DisplayName("Rate with more than two decimal places rejected")
        void tooManyDecimalPlaces() {
            var input = createValidInput();
            input.setInterestRate(new ScreenField("5.123", 5));
            String msg = bnk1uac.validateInterestRate(input);
            assertNotNull(msg);
            assertTrue(msg.contains("two decimal places"));
        }

        @Test
        @DisplayName("Multiple decimal points rejected")
        void multipleDecimalPoints() {
            var input = createValidInput();
            input.setInterestRate(new ScreenField("5.2.3", 5));
            String msg = bnk1uac.validateInterestRate(input);
            assertNotNull(msg);
            assertTrue(msg.contains("one decimal point"));
        }

        @Test
        @DisplayName("Negative interest rate rejected")
        void negativeRateRejected() {
            var input = createValidInput();
            input.setInterestRate(new ScreenField("-5.25", 5));
            String msg = bnk1uac.validateInterestRate(input);
            assertNotNull(msg);
            assertTrue(msg.contains("zero or positive"));
        }

        @Test
        @DisplayName("Rate exceeding 9999.99 rejected")
        void excessiveRateRejected() {
            var input = createValidInput();
            input.setInterestRate(new ScreenField("10000.00", 8));
            String msg = bnk1uac.validateInterestRate(input);
            assertNotNull(msg);
            assertTrue(msg.contains("less than 9999.99"));
        }

        @Test
        @DisplayName("Rate exactly 9999.99 accepted")
        void maxRateAccepted() {
            var input = createValidInput();
            input.setInterestRate(new ScreenField("9999.99", 7));
            assertNull(bnk1uac.validateInterestRate(input));
        }

        @Test
        @DisplayName("Zero rate for LOAN rejected")
        void zeroRateForLoanRejected() {
            var input = createValidInput();
            input.setAccountType(new ScreenField("LOAN    ", 8));
            input.setInterestRate(new ScreenField("0", 1));
            String msg = bnk1uac.validateInterestRate(input);
            assertNotNull(msg);
            assertTrue(msg.contains("cannot be 0"));
        }

        @Test
        @DisplayName("Zero rate for MORTGAGE rejected")
        void zeroRateForMortgageRejected() {
            var input = createValidInput();
            input.setAccountType(new ScreenField("MORTGAGE", 8));
            input.setInterestRate(new ScreenField("0", 1));
            String msg = bnk1uac.validateInterestRate(input);
            assertNotNull(msg);
            assertTrue(msg.contains("cannot be 0"));
        }

        @Test
        @DisplayName("Zero rate for CURRENT accepted")
        void zeroRateForCurrentAccepted() {
            var input = createValidInput();
            input.setAccountType(new ScreenField("CURRENT ", 8));
            input.setInterestRate(new ScreenField("0", 1));
            assertNull(bnk1uac.validateInterestRate(input));
        }

        @Test
        @DisplayName("Non-numeric characters in rate rejected")
        void nonNumericRejected() {
            var input = createValidInput();
            input.setInterestRate(new ScreenField("ABC", 3));
            String msg = bnk1uac.validateInterestRate(input);
            assertNotNull(msg);
        }
    }

    @Nested
    @DisplayName("VALIDATE-DATA SECTION — Overdraft Validation")
    class OverdraftValidation {

        @Test
        @DisplayName("Valid numeric overdraft accepted")
        void validOverdraft() {
            var input = createFullyValidInput();
            input.setOverdraft(new ScreenField("1000", 4));
            assertNull(bnk1uac.validateOverdraft(input));
        }

        @Test
        @DisplayName("Zero-length overdraft rejected")
        void zeroLengthOverdraft() {
            var input = createFullyValidInput();
            input.setOverdraft(new ScreenField("", 0));
            String msg = bnk1uac.validateOverdraft(input);
            assertNotNull(msg);
            assertTrue(msg.contains("Overdraft must be numeric"));
        }

        @Test
        @DisplayName("Non-numeric overdraft rejected")
        void nonNumericOverdraft() {
            var input = createFullyValidInput();
            input.setOverdraft(new ScreenField("ABC", 3));
            String msg = bnk1uac.validateOverdraft(input);
            assertNotNull(msg);
            assertTrue(msg.contains("Overdraft must be numeric"));
        }
    }

    @Nested
    @DisplayName("VALIDATE-DATA SECTION — Date Validation")
    class DateValidation {

        @Test
        @DisplayName("Valid last statement date accepted")
        void validLastStatement() {
            var input = createFullyValidInput();
            assertNull(bnk1uac.validateLastStatementDate(input));
        }

        @Test
        @DisplayName("Day 0 in last statement date rejected")
        void dayZeroRejected() {
            var input = createFullyValidInput();
            input.setLastStatementDay(new ScreenField("00"));
            String msg = bnk1uac.validateLastStatementDate(input);
            assertNotNull(msg);
            assertTrue(msg.contains("LAST STATEMENT"));
        }

        @Test
        @DisplayName("Day > 31 in last statement date rejected")
        void dayOver31Rejected() {
            var input = createFullyValidInput();
            input.setLastStatementDay(new ScreenField("32"));
            String msg = bnk1uac.validateLastStatementDate(input);
            assertNotNull(msg);
            assertTrue(msg.contains("LAST STATEMENT"));
        }

        @Test
        @DisplayName("Month 0 rejected")
        void monthZeroRejected() {
            var input = createFullyValidInput();
            input.setLastStatementMonth(new ScreenField("00"));
            String msg = bnk1uac.validateLastStatementDate(input);
            assertNotNull(msg);
        }

        @Test
        @DisplayName("Month > 12 rejected")
        void monthOver12Rejected() {
            var input = createFullyValidInput();
            input.setLastStatementMonth(new ScreenField("13"));
            String msg = bnk1uac.validateLastStatementDate(input);
            assertNotNull(msg);
        }

        @Test
        @DisplayName("Day 31 invalid for September (30-day month)")
        void day31InvalidForSept() {
            var input = createFullyValidInput();
            input.setLastStatementDay(new ScreenField("31"));
            input.setLastStatementMonth(new ScreenField("09"));
            String msg = bnk1uac.validateLastStatementDate(input);
            assertNotNull(msg);
        }

        @Test
        @DisplayName("Day 31 invalid for April (30-day month)")
        void day31InvalidForApril() {
            var input = createFullyValidInput();
            input.setLastStatementDay(new ScreenField("31"));
            input.setLastStatementMonth(new ScreenField("04"));
            assertNotNull(bnk1uac.validateLastStatementDate(input));
        }

        @Test
        @DisplayName("Day 31 invalid for June (30-day month)")
        void day31InvalidForJune() {
            var input = createFullyValidInput();
            input.setLastStatementDay(new ScreenField("31"));
            input.setLastStatementMonth(new ScreenField("06"));
            assertNotNull(bnk1uac.validateLastStatementDate(input));
        }

        @Test
        @DisplayName("Day 31 invalid for November (30-day month)")
        void day31InvalidForNov() {
            var input = createFullyValidInput();
            input.setLastStatementDay(new ScreenField("31"));
            input.setLastStatementMonth(new ScreenField("11"));
            assertNotNull(bnk1uac.validateLastStatementDate(input));
        }

        @Test
        @DisplayName("Day 30 invalid for February")
        void day30InvalidForFeb() {
            var input = createFullyValidInput();
            input.setLastStatementDay(new ScreenField("30"));
            input.setLastStatementMonth(new ScreenField("02"));
            assertNotNull(bnk1uac.validateLastStatementDate(input));
        }

        @Test
        @DisplayName("Day 31 valid for January (31-day month)")
        void day31ValidForJan() {
            var input = createFullyValidInput();
            input.setLastStatementDay(new ScreenField("31"));
            input.setLastStatementMonth(new ScreenField("01"));
            assertNull(bnk1uac.validateLastStatementDate(input));
        }

        @Test
        @DisplayName("Non-numeric day in last statement rejected")
        void nonNumericDay() {
            var input = createFullyValidInput();
            input.setLastStatementDay(new ScreenField("AB"));
            String msg = bnk1uac.validateLastStatementDate(input);
            assertNotNull(msg);
            assertTrue(msg.contains("numeric"));
        }

        @Test
        @DisplayName("Next statement date validates same rules")
        void nextStatementDateValidation() {
            var input = createFullyValidInput();
            input.setNextStatementDay(new ScreenField("31"));
            input.setNextStatementMonth(new ScreenField("09"));
            String msg = bnk1uac.validateNextStatementDate(input);
            assertNotNull(msg);
            assertTrue(msg.contains("NEXT STATEMENT"));
        }
    }

    // ---------------------------------------------------------------
    // Full validation pipeline
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Full validateData() pipeline")
    class FullValidation {

        @Test
        @DisplayName("Fully valid input passes all validation")
        void fullyValidInputPasses() {
            var input = createFullyValidInput();
            assertNull(bnk1uac.validateData(input));
        }

        @Test
        @DisplayName("Invalid account type fails early")
        void invalidAccountTypeFailsEarly() {
            var input = createFullyValidInput();
            input.setAccountType(new ScreenField("INVALID ", 8));
            String msg = bnk1uac.validateData(input);
            assertNotNull(msg);
            assertTrue(msg.contains("Account Type"));
        }
    }

    // ---------------------------------------------------------------
    // INQ-ACC-DATA SECTION — inquireAccountData()
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("INQ-ACC-DATA SECTION — inquireAccountData()")
    class InquireAccountSection {

        @Test
        @DisplayName("Found account populates output fields")
        void accountFound() {
            stubService.setInquireResult(createSampleCommArea());

            var input = createInput();
            input.setAccountNumber(new ScreenField("12345678"));
            var output = new UpdateAccountOutput();
            bnk1uac.inquireAccountData(input, output);

            assertTrue(output.getMessage().contains("amend fields"));
            assertEquals(12345678, output.getAccountNumber());
            assertEquals("1234567890", output.getCustomerNumber());
            assertEquals("CURRENT ", output.getAccountType());
        }

        @Test
        @DisplayName("Account not found returns appropriate message")
        void accountNotFound() {
            var notFound = new AccountCommArea();
            notFound.setAccountType("");
            notFound.setLastStatementDate(0);
            stubService.setInquireResult(notFound);

            var input = createInput();
            input.setAccountNumber(new ScreenField("99999999"));
            var output = new UpdateAccountOutput();
            bnk1uac.inquireAccountData(input, output);

            assertTrue(output.getMessage().contains("could not be found"));
        }
    }

    // ---------------------------------------------------------------
    // UPD-ACC-DATA SECTION — updateAccountData()
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("UPD-ACC-DATA SECTION — updateAccountData()")
    class UpdateAccountSection {

        @Test
        @DisplayName("Successful update returns success message")
        void successfulUpdate() {
            var resultCommArea = createSampleCommArea();
            resultCommArea.setSuccess("Y");
            stubService.setUpdateResult(resultCommArea);

            var input = createFullUpdateInput();
            var output = new UpdateAccountOutput();
            bnk1uac.updateAccountData(input, output);

            assertTrue(output.getMessage().contains("successfully applied"));
            assertEquals(12345678, output.getAccountNumber());
        }

        @Test
        @DisplayName("Failed update returns failure message")
        void failedUpdate() {
            var resultCommArea = new AccountCommArea();
            resultCommArea.setSuccess("N");
            stubService.setUpdateResult(resultCommArea);

            var input = createFullUpdateInput();
            var output = new UpdateAccountOutput();
            bnk1uac.updateAccountData(input, output);

            assertTrue(output.getMessage().contains("unsuccessful"));
        }

        @Test
        @DisplayName("Account number 99999999 uses secondary account number")
        void secondaryAccountNumber() {
            var resultCommArea = createSampleCommArea();
            resultCommArea.setSuccess("Y");
            stubService.setUpdateResult(resultCommArea);

            var input = createFullUpdateInput();
            input.setAccountNumber(new ScreenField("99999999", 8));
            input.setAccountNumber2(new ScreenField("11111111", 8));
            var output = new UpdateAccountOutput();
            bnk1uac.updateAccountData(input, output);

            assertEquals(11111111, stubService.getLastUpdateCommArea().getAccountNumber());
        }
    }

    // ---------------------------------------------------------------
    // Balance Conversion — convertScreenBalance()
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Balance Conversion — convertScreenBalance()")
    class BalanceConversion {

        @Test
        @DisplayName("Positive balance conversion")
        void positiveBalance() {
            BigDecimal result = Bnk1uac.convertScreenBalance("+0000001234.56");
            assertEquals(new BigDecimal("1234.56"), result);
        }

        @Test
        @DisplayName("Negative balance conversion")
        void negativeBalance() {
            BigDecimal result = Bnk1uac.convertScreenBalance("-0000001234.56");
            assertEquals(new BigDecimal("-1234.56"), result);
        }

        @Test
        @DisplayName("Zero balance conversion")
        void zeroBalance() {
            BigDecimal result = Bnk1uac.convertScreenBalance("+0000000000.00");
            assertEquals(0, result.compareTo(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("Blank value returns zero")
        void blankReturnsZero() {
            assertEquals(BigDecimal.ZERO, Bnk1uac.convertScreenBalance(""));
            assertEquals(BigDecimal.ZERO, Bnk1uac.convertScreenBalance(null));
            assertEquals(BigDecimal.ZERO, Bnk1uac.convertScreenBalance("   "));
        }

        @Test
        @DisplayName("Large positive balance")
        void largePositiveBalance() {
            BigDecimal result = Bnk1uac.convertScreenBalance("+9999999999.99");
            assertEquals(new BigDecimal("9999999999.99"), result);
        }

        @Test
        @DisplayName("Large negative balance")
        void largeNegativeBalance() {
            BigDecimal result = Bnk1uac.convertScreenBalance("-9999999999.99");
            assertEquals(new BigDecimal("-9999999999.99"), result);
        }
    }

    // ---------------------------------------------------------------
    // Date Utility — composeDateValue()
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Date Utility — composeDateValue()")
    class DateUtility {

        @Test
        @DisplayName("Composes date from DD, MM, YYYY strings")
        void composesDate() {
            assertEquals(15062023, Bnk1uac.composeDateValue("15", "06", "2023"));
        }

        @Test
        @DisplayName("Composes single-digit day and month")
        void composeSingleDigit() {
            assertEquals(1012023, Bnk1uac.composeDateValue("01", "01", "2023"));
        }

        @Test
        @DisplayName("Handles blank components as zero")
        void handlesBlankComponents() {
            assertEquals(0, Bnk1uac.composeDateValue("", "", ""));
        }
    }

    // ---------------------------------------------------------------
    // Output formatting — UpdateAccountOutput
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("UpdateAccountOutput formatting")
    class OutputFormatting {

        @Test
        @DisplayName("Populates all output fields from COMMAREA")
        void populateFromCommArea() {
            var commArea = createSampleCommArea();
            var output = new UpdateAccountOutput();
            output.populateFromCommArea(commArea);

            assertEquals(12345678, output.getAccountNumber());
            assertEquals("1234567890", output.getCustomerNumber());
            assertEquals("987654", output.getSortCode());
            assertEquals("CURRENT ", output.getAccountType());
            assertEquals("0005.25", output.getInterestRate());
            assertEquals("15", output.getOpenedDay());
            assertEquals("06", output.getOpenedMonth());
            assertEquals("2020", output.getOpenedYear());
        }

        @Test
        @DisplayName("Formats interest rate with leading zeros")
        void formatsInterestRate() {
            assertEquals("0005.25",
                    UpdateAccountOutput.formatInterestRate(new BigDecimal("5.25")));
            assertEquals("9999.99",
                    UpdateAccountOutput.formatInterestRate(new BigDecimal("9999.99")));
            assertEquals("0000.00",
                    UpdateAccountOutput.formatInterestRate(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("Formats balance with sign")
        void formatsBalance() {
            String pos = UpdateAccountOutput.formatBalance(new BigDecimal("1234.56"));
            assertTrue(pos.startsWith("+"));
            assertTrue(pos.contains("1234.56"));

            String neg = UpdateAccountOutput.formatBalance(new BigDecimal("-1234.56"));
            assertTrue(neg.startsWith("-"));
            assertTrue(neg.contains("1234.56"));
        }

        @Test
        @DisplayName("Extracts date components correctly")
        void extractsDateComponents() {
            assertEquals("15", UpdateAccountOutput.extractDay(15062020));
            assertEquals("06", UpdateAccountOutput.extractMonth(15062020));
            assertEquals("2020", UpdateAccountOutput.extractYear(15062020));
        }
    }

    // ---------------------------------------------------------------
    // isValidDayForMonth()
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("isValidDayForMonth()")
    class DayForMonthValidation {

        @Test
        @DisplayName("Day 31 valid for 31-day months")
        void day31ValidFor31DayMonths() {
            assertTrue(Bnk1uac.isValidDayForMonth(31, 1));  // Jan
            assertTrue(Bnk1uac.isValidDayForMonth(31, 3));  // Mar
            assertTrue(Bnk1uac.isValidDayForMonth(31, 5));  // May
            assertTrue(Bnk1uac.isValidDayForMonth(31, 7));  // Jul
            assertTrue(Bnk1uac.isValidDayForMonth(31, 8));  // Aug
            assertTrue(Bnk1uac.isValidDayForMonth(31, 10)); // Oct
            assertTrue(Bnk1uac.isValidDayForMonth(31, 12)); // Dec
        }

        @Test
        @DisplayName("Day 31 invalid for 30-day months")
        void day31InvalidFor30DayMonths() {
            assertFalse(Bnk1uac.isValidDayForMonth(31, 4));  // Apr
            assertFalse(Bnk1uac.isValidDayForMonth(31, 6));  // Jun
            assertFalse(Bnk1uac.isValidDayForMonth(31, 9));  // Sep
            assertFalse(Bnk1uac.isValidDayForMonth(31, 11)); // Nov
        }

        @Test
        @DisplayName("Day 30 invalid for February")
        void day30InvalidForFeb() {
            assertFalse(Bnk1uac.isValidDayForMonth(30, 2));
        }

        @Test
        @DisplayName("Day 29 valid for February")
        void day29ValidForFeb() {
            assertTrue(Bnk1uac.isValidDayForMonth(29, 2));
        }
    }

    // ---------------------------------------------------------------
    // isNumeric()
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("isNumeric()")
    class NumericValidation {

        @Test
        @DisplayName("All digits returns true")
        void allDigits() {
            assertTrue(Bnk1uac.isNumeric("12345678"));
        }

        @Test
        @DisplayName("Letters return false")
        void letters() {
            assertFalse(Bnk1uac.isNumeric("ABCD"));
        }

        @Test
        @DisplayName("Empty string returns false")
        void empty() {
            assertFalse(Bnk1uac.isNumeric(""));
        }

        @Test
        @DisplayName("Null returns false")
        void nullValue() {
            assertFalse(Bnk1uac.isNumeric(null));
        }

        @Test
        @DisplayName("Mixed alphanumeric returns false")
        void mixed() {
            assertFalse(Bnk1uac.isNumeric("12AB56"));
        }
    }

    // ---------------------------------------------------------------
    // countValidChars() / countDecimalPoints()
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Utility counting methods")
    class UtilityMethods {

        @Test
        @DisplayName("countValidChars counts digits, dots, signs, spaces")
        void countValidCharsTest() {
            assertEquals(7, Bnk1uac.countValidChars("12.34 +"));
            assertEquals(0, Bnk1uac.countValidChars("ABC"));
        }

        @Test
        @DisplayName("countDecimalPoints counts dots")
        void countDecimalPointsTest() {
            assertEquals(1, Bnk1uac.countDecimalPoints("12.34"));
            assertEquals(2, Bnk1uac.countDecimalPoints("1.2.3"));
            assertEquals(0, Bnk1uac.countDecimalPoints("1234"));
        }

        @Test
        @DisplayName("countCharsAfterDecimalPoint")
        void countCharsAfterDecimalTest() {
            assertEquals(2, Bnk1uac.countCharsAfterDecimalPoint("12.34"));
            assertEquals(3, Bnk1uac.countCharsAfterDecimalPoint("12.345"));
            assertEquals(0, Bnk1uac.countCharsAfterDecimalPoint("1234"));
        }
    }

    // ---------------------------------------------------------------
    // AccountType enum
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("AccountType enum")
    class AccountTypeEnumTest {

        @Test
        @DisplayName("fromString resolves valid types")
        void fromStringValid() {
            assertEquals(AccountType.CURRENT, AccountType.fromString("CURRENT"));
            assertEquals(AccountType.SAVING, AccountType.fromString("SAVING"));
            assertEquals(AccountType.LOAN, AccountType.fromString("LOAN"));
            assertEquals(AccountType.MORTGAGE, AccountType.fromString("MORTGAGE"));
            assertEquals(AccountType.ISA, AccountType.fromString("ISA"));
        }

        @Test
        @DisplayName("fromString handles padded values")
        void fromStringPadded() {
            assertEquals(AccountType.CURRENT, AccountType.fromString("CURRENT "));
            assertEquals(AccountType.ISA, AccountType.fromString("ISA     "));
        }

        @Test
        @DisplayName("fromString returns null for invalid type")
        void fromStringInvalid() {
            assertNull(AccountType.fromString("INVALID"));
            assertNull(AccountType.fromString(null));
        }

        @Test
        @DisplayName("LOAN and MORTGAGE require non-zero interest")
        void requiresNonZeroInterest() {
            assertTrue(AccountType.LOAN.requiresNonZeroInterest());
            assertTrue(AccountType.MORTGAGE.requiresNonZeroInterest());
            assertFalse(AccountType.CURRENT.requiresNonZeroInterest());
            assertFalse(AccountType.SAVING.requiresNonZeroInterest());
            assertFalse(AccountType.ISA.requiresNonZeroInterest());
        }

        @Test
        @DisplayName("paddedLabel returns 8-char padded string")
        void paddedLabel() {
            assertEquals("CURRENT ", AccountType.CURRENT.paddedLabel());
            assertEquals("ISA     ", AccountType.ISA.paddedLabel());
            assertEquals("MORTGAGE", AccountType.MORTGAGE.paddedLabel());
        }
    }

    // ---------------------------------------------------------------
    // AccountCommArea
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("AccountCommArea")
    class CommAreaTest {

        @Test
        @DisplayName("isAccountFound returns true when type and date are set")
        void isAccountFound() {
            var ca = createSampleCommArea();
            assertTrue(ca.isAccountFound());
        }

        @Test
        @DisplayName("isAccountFound returns false when type blank and date zero")
        void isAccountNotFound() {
            var ca = new AccountCommArea();
            ca.setAccountType("");
            ca.setLastStatementDate(0);
            assertFalse(ca.isAccountFound());
        }
    }

    // ---------------------------------------------------------------
    // Bnk1uacException
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Bnk1uacException")
    class ExceptionTest {

        @Test
        @DisplayName("Exception formats message correctly")
        void formatsMessage() {
            var ex = new Bnk1uacException("A010",
                    "RETURN TRANSID(OUAC) FAIL", 16, 0);
            assertTrue(ex.getMessage().contains("BNK1UAC"));
            assertTrue(ex.getMessage().contains("A010"));
            assertTrue(ex.getMessage().contains("RESP=16"));
            assertEquals("HBNK", ex.getAbendCode());
            assertEquals(16, ex.getResponseCode());
            assertEquals(0, ex.getResponse2Code());
        }
    }

    // ---------------------------------------------------------------
    // PROCESS-MAP end-to-end
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("PROCESS-MAP end-to-end flows")
    class ProcessMapEndToEnd {

        @Test
        @DisplayName("ENTER with valid account number performs inquiry")
        void enterWithValidAccount() {
            stubService.setInquireResult(createSampleCommArea());

            var input = createInput();
            input.setAccountNumber(new ScreenField("12345678"));

            var result = bnk1uac.handleTransaction(
                    Bnk1uac.AidKey.ENTER, false, input);

            assertInstanceOf(Bnk1uac.TransactionResult.SendMap.class, result);
            var sendMap = (Bnk1uac.TransactionResult.SendMap) result;
            assertTrue(sendMap.output().getMessage().contains("amend fields"));
            assertEquals(12345678, sendMap.output().getAccountNumber());
        }

        @Test
        @DisplayName("ENTER with invalid account number shows error")
        void enterWithInvalidAccount() {
            var input = createInput();
            input.setAccountNumber(new ScreenField("ABCDEFGH"));

            var result = bnk1uac.handleTransaction(
                    Bnk1uac.AidKey.ENTER, false, input);

            assertInstanceOf(Bnk1uac.TransactionResult.SendMap.class, result);
            var sendMap = (Bnk1uac.TransactionResult.SendMap) result;
            assertTrue(sendMap.output().getMessage().contains("enter an account number"));
        }

        @Test
        @DisplayName("PF5 with valid data performs update")
        void pf5WithValidData() {
            var resultCommArea = createSampleCommArea();
            resultCommArea.setSuccess("Y");
            stubService.setUpdateResult(resultCommArea);

            var input = createFullUpdateInput();

            var result = bnk1uac.handleTransaction(
                    Bnk1uac.AidKey.PF5, false, input);

            assertInstanceOf(Bnk1uac.TransactionResult.SendMap.class, result);
            var sendMap = (Bnk1uac.TransactionResult.SendMap) result;
            assertTrue(sendMap.output().getMessage().contains("successfully applied"));
        }

        @Test
        @DisplayName("PF5 with invalid account type shows validation error")
        void pf5WithInvalidType() {
            var input = createFullUpdateInput();
            input.setAccountType(new ScreenField("INVALID ", 8));

            var result = bnk1uac.handleTransaction(
                    Bnk1uac.AidKey.PF5, false, input);

            assertInstanceOf(Bnk1uac.TransactionResult.SendMap.class, result);
            var sendMap = (Bnk1uac.TransactionResult.SendMap) result;
            assertTrue(sendMap.output().getMessage().contains("Account Type"));
        }
    }

    // ---------------------------------------------------------------
    // Helper methods
    // ---------------------------------------------------------------

    private static UpdateAccountInput createInput() {
        return new UpdateAccountInput();
    }

    private static UpdateAccountInput inputWithAccountType(String type) {
        var input = createInput();
        input.setAccountType(new ScreenField(type, type.length()));
        return input;
    }

    private static UpdateAccountInput createValidInput() {
        var input = createInput();
        input.setAccountType(new ScreenField("CURRENT ", 8));
        input.setInterestRate(new ScreenField("5.25", 4));
        return input;
    }

    private static UpdateAccountInput createFullyValidInput() {
        var input = createValidInput();
        input.setAccountNumber(new ScreenField("12345678", 8));
        input.setOverdraft(new ScreenField("1000", 4));
        input.setLastStatementDay(new ScreenField("15"));
        input.setLastStatementMonth(new ScreenField("06"));
        input.setLastStatementYear(new ScreenField("2023"));
        input.setNextStatementDay(new ScreenField("15"));
        input.setNextStatementMonth(new ScreenField("07"));
        input.setNextStatementYear(new ScreenField("2023"));
        return input;
    }

    private static UpdateAccountInput createFullUpdateInput() {
        var input = createFullyValidInput();
        input.setAccountNumber2(new ScreenField("12345678", 8));
        input.setCustomerNumber(new ScreenField("1234567890"));
        input.setSortCode(new ScreenField("987654"));
        input.setOpenedDay(new ScreenField("15"));
        input.setOpenedMonth(new ScreenField("06"));
        input.setOpenedYear(new ScreenField("2020"));
        input.setAvailableBalance(new ScreenField("+0000001000.00", 14));
        input.setActualBalance(new ScreenField("+0000001000.00", 14));
        return input;
    }

    private static AccountCommArea createSampleCommArea() {
        var ca = new AccountCommArea();
        ca.setEye("ACCT");
        ca.setCustomerNumber("1234567890");
        ca.setSortCode("987654");
        ca.setAccountNumber(12345678);
        ca.setAccountType("CURRENT ");
        ca.setInterestRate(new BigDecimal("5.25"));
        ca.setOpened(15062020);
        ca.setOverdraft(1000);
        ca.setLastStatementDate(15062023);
        ca.setNextStatementDate(15072023);
        ca.setAvailableBalance(new BigDecimal("1000.00"));
        ca.setActualBalance(new BigDecimal("1000.00"));
        ca.setSuccess("Y");
        return ca;
    }

    // ---------------------------------------------------------------
    // Stub AccountService
    // ---------------------------------------------------------------

    private static class StubAccountService implements AccountService {

        private AccountCommArea inquireResult;
        private AccountCommArea updateResult;
        private AccountCommArea lastUpdateCommArea;

        void setInquireResult(AccountCommArea result) {
            this.inquireResult = result;
        }

        void setUpdateResult(AccountCommArea result) {
            this.updateResult = result;
        }

        AccountCommArea getLastUpdateCommArea() {
            return lastUpdateCommArea;
        }

        @Override
        public AccountCommArea inquireAccount(int accountNumber) {
            return inquireResult;
        }

        @Override
        public AccountCommArea updateAccount(AccountCommArea commArea) {
            this.lastUpdateCommArea = commArea;
            return updateResult;
        }
    }
}
