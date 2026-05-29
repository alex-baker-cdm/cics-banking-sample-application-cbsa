/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.migrated.createaccount;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class CreateAccountControllerTest {

    private CreateAccountController controller;
    private StubAccountCreationService stubService;

    @BeforeEach
    void setUp() {
        stubService = new StubAccountCreationService();
        controller = new CreateAccountController(stubService);
    }

    @Test
    @DisplayName("Constructor rejects null service")
    void constructorRejectsNullService() {
        assertThrows(NullPointerException.class,
                () -> new CreateAccountController(null));
    }

    @Nested
    @DisplayName("Customer Number Validation")
    class CustomerNumberValidation {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("Rejects blank or null customer number")
        void rejectsBlankCustomerNumber(String customerNumber) {
            CreateAccountRequest request = new CreateAccountRequest(
                    customerNumber, "ISA", new BigDecimal("1.50"), 0);
            CreateAccountResult result = controller.processCreateAccount(request);

            assertFalse(result.success());
            assertEquals("Please enter a 10 digit Customer Number",
                    result.failureMessage());
        }

        @ParameterizedTest
        @ValueSource(strings = {"12345", "123", "12345678901", "1"})
        @DisplayName("Rejects customer number with wrong length")
        void rejectsWrongLengthCustomerNumber(String customerNumber) {
            CreateAccountRequest request = new CreateAccountRequest(
                    customerNumber, "ISA", new BigDecimal("1.50"), 0);
            CreateAccountResult result = controller.processCreateAccount(request);

            assertFalse(result.success());
            assertEquals("Please enter a 10 digit Customer Number",
                    result.failureMessage());
        }

        @ParameterizedTest
        @ValueSource(strings = {"123456789A", "ABCDEFGHIJ", "12345 6789", "12-4567890"})
        @DisplayName("Rejects non-numeric customer number")
        void rejectsNonNumericCustomerNumber(String customerNumber) {
            CreateAccountRequest request = new CreateAccountRequest(
                    customerNumber, "ISA", new BigDecimal("1.50"), 0);
            CreateAccountResult result = controller.processCreateAccount(request);

            assertFalse(result.success());
            assertEquals("Please enter a numeric Customer number",
                    result.failureMessage());
        }

        @Test
        @DisplayName("Accepts valid 10-digit numeric customer number")
        void acceptsValidCustomerNumber() {
            CreateAccountRequest request = new CreateAccountRequest(
                    "0000000001", "ISA", new BigDecimal("1.50"), 0);
            stubService.setSuccessResult();

            CreateAccountResult result = controller.processCreateAccount(request);
            assertTrue(result.success());
        }
    }

    @Nested
    @DisplayName("Account Type Validation")
    class AccountTypeValidation {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("Rejects blank or null account type")
        void rejectsBlankAccountType(String accountType) {
            CreateAccountRequest request = new CreateAccountRequest(
                    "0000000001", accountType, new BigDecimal("1.50"), 0);
            CreateAccountResult result = controller.processCreateAccount(request);

            assertFalse(result.success());
            assertEquals("Account Type should be ISA,CURRENT,LOAN,SAVING or MORTGAGE",
                    result.failureMessage());
        }

        @ParameterizedTest
        @ValueSource(strings = {"CHECKING", "DEBIT", "CREDIT", "INVALID", "XYZ"})
        @DisplayName("Rejects invalid account types")
        void rejectsInvalidAccountType(String accountType) {
            CreateAccountRequest request = new CreateAccountRequest(
                    "0000000001", accountType, new BigDecimal("1.50"), 0);
            CreateAccountResult result = controller.processCreateAccount(request);

            assertFalse(result.success());
            assertEquals("Account Type should be ISA,CURRENT,LOAN,SAVING or MORTGAGE",
                    result.failureMessage());
        }

        @ParameterizedTest
        @ValueSource(strings = {"ISA", "isa", "CURRENT", "current", "LOAN", "loan",
                "SAVING", "saving", "MORTGAGE", "mortgage"})
        @DisplayName("Accepts valid account types (case insensitive)")
        void acceptsValidAccountTypes(String accountType) {
            CreateAccountRequest request = new CreateAccountRequest(
                    "0000000001", accountType, new BigDecimal("1.50"), 0);
            stubService.setSuccessResult();

            CreateAccountResult result = controller.processCreateAccount(request);
            assertTrue(result.success());
        }

        @ParameterizedTest
        @ValueSource(strings = {"ISA_____", "isa_____", "CURRENT_", "current_",
                "LOAN____", "loan____", "SAVING__", "saving__"})
        @DisplayName("Accepts account types with trailing underscores (BMS pad chars)")
        void acceptsAccountTypesWithUnderscores(String accountType) {
            CreateAccountRequest request = new CreateAccountRequest(
                    "0000000001", accountType, new BigDecimal("1.50"), 0);
            stubService.setSuccessResult();

            CreateAccountResult result = controller.processCreateAccount(request);
            assertTrue(result.success());
        }
    }

    @Nested
    @DisplayName("Interest Rate Validation")
    class InterestRateValidation {

        @Test
        @DisplayName("Rejects null interest rate")
        void rejectsNullInterestRate() {
            CreateAccountRequest request = new CreateAccountRequest(
                    "0000000001", "ISA", null, 0);
            CreateAccountResult result = controller.processCreateAccount(request);

            assertFalse(result.success());
            assertEquals("Please supply a numeric interest rate",
                    result.failureMessage());
        }

        @Test
        @DisplayName("Rejects negative interest rate")
        void rejectsNegativeInterestRate() {
            CreateAccountRequest request = new CreateAccountRequest(
                    "0000000001", "ISA", new BigDecimal("-1.00"), 0);
            CreateAccountResult result = controller.processCreateAccount(request);

            assertFalse(result.success());
            assertEquals("Please supply a zero or positive interest rate",
                    result.failureMessage());
        }

        @Test
        @DisplayName("Rejects interest rate greater than 9999.99")
        void rejectsInterestRateTooHigh() {
            CreateAccountRequest request = new CreateAccountRequest(
                    "0000000001", "ISA", new BigDecimal("10000.00"), 0);
            CreateAccountResult result = controller.processCreateAccount(request);

            assertFalse(result.success());
            assertEquals("Please supply an interest rate less than 9999.99%",
                    result.failureMessage());
        }

        @Test
        @DisplayName("Rejects interest rate with more than 2 decimal places")
        void rejectsMoreThanTwoDecimalPlaces() {
            CreateAccountRequest request = new CreateAccountRequest(
                    "0000000001", "ISA", new BigDecimal("1.123"), 0);
            CreateAccountResult result = controller.processCreateAccount(request);

            assertFalse(result.success());
            assertEquals("Only up to two decimal places are supported",
                    result.failureMessage());
        }

        @Test
        @DisplayName("Accepts zero interest rate")
        void acceptsZeroInterestRate() {
            CreateAccountRequest request = new CreateAccountRequest(
                    "0000000001", "ISA", BigDecimal.ZERO, 0);
            stubService.setSuccessResult();

            CreateAccountResult result = controller.processCreateAccount(request);
            assertTrue(result.success());
        }

        @Test
        @DisplayName("Accepts maximum valid interest rate 9999.99")
        void acceptsMaxValidInterestRate() {
            CreateAccountRequest request = new CreateAccountRequest(
                    "0000000001", "ISA", new BigDecimal("9999.99"), 0);
            stubService.setSuccessResult();

            CreateAccountResult result = controller.processCreateAccount(request);
            assertTrue(result.success());
        }

        @Test
        @DisplayName("Accepts interest rate with 1 decimal place")
        void acceptsOneDecimalPlace() {
            CreateAccountRequest request = new CreateAccountRequest(
                    "0000000001", "ISA", new BigDecimal("5.5"), 0);
            stubService.setSuccessResult();

            CreateAccountResult result = controller.processCreateAccount(request);
            assertTrue(result.success());
        }

        @Test
        @DisplayName("Accepts integer interest rate")
        void acceptsIntegerInterestRate() {
            CreateAccountRequest request = new CreateAccountRequest(
                    "0000000001", "ISA", new BigDecimal("5"), 0);
            stubService.setSuccessResult();

            CreateAccountResult result = controller.processCreateAccount(request);
            assertTrue(result.success());
        }
    }

    @Nested
    @DisplayName("Overdraft Limit Validation")
    class OverdraftLimitValidation {

        @Test
        @DisplayName("Rejects negative overdraft limit")
        void rejectsNegativeOverdraft() {
            CreateAccountRequest request = new CreateAccountRequest(
                    "0000000001", "ISA", new BigDecimal("1.50"), -1);
            CreateAccountResult result = controller.processCreateAccount(request);

            assertFalse(result.success());
            assertEquals("Overdraft Limit must be numeric positive int",
                    result.failureMessage());
        }

        @Test
        @DisplayName("Accepts zero overdraft limit")
        void acceptsZeroOverdraft() {
            CreateAccountRequest request = new CreateAccountRequest(
                    "0000000001", "ISA", new BigDecimal("1.50"), 0);
            stubService.setSuccessResult();

            CreateAccountResult result = controller.processCreateAccount(request);
            assertTrue(result.success());
        }

        @Test
        @DisplayName("Accepts positive overdraft limit")
        void acceptsPositiveOverdraft() {
            CreateAccountRequest request = new CreateAccountRequest(
                    "0000000001", "ISA", new BigDecimal("1.50"), 5000);
            stubService.setSuccessResult();

            CreateAccountResult result = controller.processCreateAccount(request);
            assertTrue(result.success());
        }
    }

    @Nested
    @DisplayName("Account Creation Delegation")
    class AccountCreationDelegation {

        @Test
        @DisplayName("Passes correct parameters to creation service")
        void passesCorrectParams() {
            CreateAccountRequest request = new CreateAccountRequest(
                    "0000000001", "CURRENT", new BigDecimal("2.50"), 1000);
            stubService.setSuccessResult();

            controller.processCreateAccount(request);

            CreateAccountParams captured = stubService.getLastParams();
            assertNotNull(captured);
            assertEquals("0000000001", captured.customerNumber());
            assertEquals("000000", captured.sortCode());
            assertEquals("00000000", captured.accountNumber());
            assertEquals("CURRENT", captured.accountType());
            assertEquals(new BigDecimal("2.50"), captured.interestRate());
            assertEquals(1000, captured.overdraftLimit());
        }

        @Test
        @DisplayName("Returns success result from service")
        void returnsSuccessResult() {
            CreateAccountRequest request = new CreateAccountRequest(
                    "0000000001", "ISA", new BigDecimal("3.25"), 500);
            stubService.setSuccessResult();

            CreateAccountResult result = controller.processCreateAccount(request);

            assertTrue(result.success());
            assertNotNull(result.sortCode());
            assertNotNull(result.accountNumber());
        }

        @ParameterizedTest
        @ValueSource(strings = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "A"})
        @DisplayName("Maps failure codes to appropriate messages")
        void mapsFailureCodes(String failCode) {
            CreateAccountRequest request = new CreateAccountRequest(
                    "0000000001", "ISA", new BigDecimal("1.50"), 0);
            stubService.setFailureResult(failCode);

            CreateAccountResult result = controller.processCreateAccount(request);

            assertFalse(result.success());
            assertNotNull(result.failureMessage());
            assertFalse(result.failureMessage().isBlank());
        }

        @Test
        @DisplayName("Handles unknown failure code")
        void handlesUnknownFailureCode() {
            CreateAccountRequest request = new CreateAccountRequest(
                    "0000000001", "ISA", new BigDecimal("1.50"), 0);
            stubService.setFailureResult("Z");

            CreateAccountResult result = controller.processCreateAccount(request);

            assertFalse(result.success());
            assertEquals("The account was not created.", result.failureMessage());
        }
    }

    @Nested
    @DisplayName("Null Request Handling")
    class NullRequestHandling {

        @Test
        @DisplayName("Rejects null request")
        void rejectsNullRequest() {
            CreateAccountResult result = controller.processCreateAccount(null);
            assertFalse(result.success());
            assertEquals("Request must not be null", result.failureMessage());
        }
    }

    @Nested
    @DisplayName("Failure Code Messages - Detailed")
    class FailureCodeMessages {

        @Test
        @DisplayName("Failure code 1 - customer does not exist")
        void failCode1() {
            CreateAccountRequest request = validRequest();
            stubService.setFailureResult("1");
            CreateAccountResult result = controller.processCreateAccount(request);
            assertEquals("The supplied customer number does not exist.",
                    result.failureMessage());
        }

        @Test
        @DisplayName("Failure code 2 - customer data inaccessible")
        void failCode2() {
            CreateAccountRequest request = validRequest();
            stubService.setFailureResult("2");
            CreateAccountResult result = controller.processCreateAccount(request);
            assertEquals("The customer data cannot be accessed, unable to create account.",
                    result.failureMessage());
        }

        @Test
        @DisplayName("Failure code 3 - unable to ENQ")
        void failCode3() {
            CreateAccountRequest request = validRequest();
            stubService.setFailureResult("3");
            CreateAccountResult result = controller.processCreateAccount(request);
            assertEquals("Account record creation failed. (unable to ENQ ACCOUNT NC).",
                    result.failureMessage());
        }

        @Test
        @DisplayName("Failure code 4 - unable to increment")
        void failCode4() {
            CreateAccountRequest request = validRequest();
            stubService.setFailureResult("4");
            CreateAccountResult result = controller.processCreateAccount(request);
            assertEquals("Account record creation failed, (unable to increment ACCOUNT NC).",
                    result.failureMessage());
        }

        @Test
        @DisplayName("Failure code 5 - unable to restore")
        void failCode5() {
            CreateAccountRequest request = validRequest();
            stubService.setFailureResult("5");
            CreateAccountResult result = controller.processCreateAccount(request);
            assertEquals("Account record creation failed, (unable to restore ACCOUNT NC).",
                    result.failureMessage());
        }

        @Test
        @DisplayName("Failure code 6 - unable to WRITE")
        void failCode6() {
            CreateAccountRequest request = validRequest();
            stubService.setFailureResult("6");
            CreateAccountResult result = controller.processCreateAccount(request);
            assertEquals("Account record creation failed, (unable to WRITE to ACCOUNT file).",
                    result.failureMessage());
        }

        @Test
        @DisplayName("Failure code 7 - unable to INSERT")
        void failCode7() {
            CreateAccountRequest request = validRequest();
            stubService.setFailureResult("7");
            CreateAccountResult result = controller.processCreateAccount(request);
            assertEquals("Account record creation failed, (unable to INSERT into ACCOUNT).",
                    result.failureMessage());
        }

        @Test
        @DisplayName("Failure code 8 - too many accounts")
        void failCode8() {
            CreateAccountRequest request = validRequest();
            stubService.setFailureResult("8");
            CreateAccountResult result = controller.processCreateAccount(request);
            assertEquals("Account record creation failed, (too many accounts).",
                    result.failureMessage());
        }

        @Test
        @DisplayName("Failure code 9 - unable to count accounts")
        void failCode9() {
            CreateAccountRequest request = validRequest();
            stubService.setFailureResult("9");
            CreateAccountResult result = controller.processCreateAccount(request);
            assertEquals("Account record creation failed, unable to count accounts.",
                    result.failureMessage());
        }

        @Test
        @DisplayName("Failure code A - account type unsupported")
        void failCodeA() {
            CreateAccountRequest request = validRequest();
            stubService.setFailureResult("A");
            CreateAccountResult result = controller.processCreateAccount(request);
            assertEquals("Account record creation failed, account type unsupported.",
                    result.failureMessage());
        }
    }

    private CreateAccountRequest validRequest() {
        return new CreateAccountRequest(
                "0000000001", "ISA", new BigDecimal("1.50"), 0);
    }

    private static class StubAccountCreationService implements AccountCreationService {
        private CreateAccountParams lastParams;
        private CreateAccountResult resultToReturn;

        void setSuccessResult() {
            resultToReturn = CreateAccountResult.success(
                    "0000000001",
                    "987654",
                    "12345678",
                    "ISA",
                    new BigDecimal("1.50"),
                    0,
                    LocalDate.now(),
                    LocalDate.now(),
                    LocalDate.now().plusMonths(1),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO);
        }

        void setFailureResult(String failCode) {
            resultToReturn = new CreateAccountResult(
                    false, failCode, null, null, null, null,
                    null, 0, null, null, null, null, null);
        }

        @Override
        public CreateAccountResult createAccount(CreateAccountParams params) {
            this.lastParams = params;
            return resultToReturn;
        }

        CreateAccountParams getLastParams() {
            return lastParams;
        }
    }
}
