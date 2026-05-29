/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.migrated.cobol.transferfunds;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransferFundsServiceTest {

    private TransferFunctionResponse successResponse;
    private TransferFunctionGateway successGateway;
    private TransferFundsService service;

    @BeforeEach
    void setUp() {
        successResponse = new TransferFunctionResponse(
                "12345678", "987654", "87654321", "123456",
                new BigDecimal("900.00"), new BigDecimal("900.00"),
                new BigDecimal("1100.00"), new BigDecimal("1100.00"),
                ' ', 'Y');

        successGateway = request -> successResponse;
        service = new TransferFundsService(successGateway);
    }

    // ==================================================================
    // EDIT-DATA tests (account number validation)
    // ==================================================================

    @Nested
    @DisplayName("EDIT-DATA — Account Number Validation")
    class EditDataTests {

        @Test
        @DisplayName("Non-numeric FROM account is rejected")
        void nonNumericFromAccountRejected() {
            TransferResult result = service.processTransfer(
                    "ABCDEFGH", "87654321", "100.00");
            assertFalse(result.isSuccessful());
            assertEquals("Please enter a FROM account no",
                    result.getMessage());
        }

        @Test
        @DisplayName("Non-numeric TO account is rejected")
        void nonNumericToAccountRejected() {
            TransferResult result = service.processTransfer(
                    "12345678", "XYZXYZXY", "100.00");
            assertFalse(result.isSuccessful());
            assertEquals("Please enter a TO account no",
                    result.getMessage());
        }

        @Test
        @DisplayName("Same FROM and TO account is rejected")
        void sameFromAndToRejected() {
            TransferResult result = service.processTransfer(
                    "12345678", "12345678", "100.00");
            assertFalse(result.isSuccessful());
            assertEquals("The FROM & TO account should be different",
                    result.getMessage());
        }

        @Test
        @DisplayName("Zero FROM account (00000000) is rejected")
        void zeroFromAccountRejected() {
            TransferResult result = service.processTransfer(
                    "00000000", "87654321", "100.00");
            assertFalse(result.isSuccessful());
            assertEquals("Account no 00000000 is not valid",
                    result.getMessage());
        }

        @Test
        @DisplayName("Zero TO account (00000000) is rejected")
        void zeroToAccountRejected() {
            TransferResult result = service.processTransfer(
                    "12345678", "00000000", "100.00");
            assertFalse(result.isSuccessful());
            assertEquals("Account no 00000000 is not valid",
                    result.getMessage());
        }

        @Test
        @DisplayName("Both zero accounts are rejected")
        void bothZeroAccountsRejected() {
            TransferResult result = service.processTransfer(
                    "00000000", "00000000", "100.00");
            assertFalse(result.isSuccessful());
            // FROM & TO same check comes first
            assertEquals("The FROM & TO account should be different",
                    result.getMessage());
        }

        @Test
        @DisplayName("Empty FROM account is rejected")
        void emptyFromAccountRejected() {
            TransferResult result = service.processTransfer(
                    "", "87654321", "100.00");
            assertFalse(result.isSuccessful());
            assertEquals("Please enter a FROM account no",
                    result.getMessage());
        }

        @Test
        @DisplayName("Null FROM account is rejected")
        void nullFromAccountRejected() {
            TransferResult result = service.processTransfer(
                    null, "87654321", "100.00");
            assertFalse(result.isSuccessful());
            assertEquals("Please enter a FROM account no",
                    result.getMessage());
        }

        @Test
        @DisplayName("FROM account with currency symbols is deedited")
        void fromAccountWithCurrencyDeedited() {
            // DEEDIT strips non-numeric chars, "12,345,678" → "12345678"
            TransferResult result = service.processTransfer(
                    "12,345,678", "87654321", "100.00");
            assertTrue(result.isSuccessful());
        }
    }

    // ==================================================================
    // VALIDATE-AMOUNT tests
    // ==================================================================

    @Nested
    @DisplayName("VALIDATE-AMOUNT — Amount Validation")
    class ValidateAmountTests {

        @Test
        @DisplayName("Valid integer amount is accepted")
        void validIntegerAmount() {
            BigDecimal result = service.validateAmount("100");
            assertNotNull(result);
            assertEquals(new BigDecimal("100"), result);
        }

        @Test
        @DisplayName("Valid decimal amount is accepted")
        void validDecimalAmount() {
            BigDecimal result = service.validateAmount("100.50");
            assertNotNull(result);
            assertEquals(new BigDecimal("100.50"), result);
        }

        @Test
        @DisplayName("Valid amount with one decimal place")
        void validAmountOneDecimalPlace() {
            BigDecimal result = service.validateAmount("99.5");
            assertNotNull(result);
            assertEquals(new BigDecimal("99.5"), result);
        }

        @Test
        @DisplayName("Null amount is rejected")
        void nullAmountRejected() {
            assertNull(service.validateAmount(null));
        }

        @Test
        @DisplayName("Empty amount is rejected")
        void emptyAmountRejected() {
            assertNull(service.validateAmount(""));
        }

        @Test
        @DisplayName("Blank/spaces-only amount is rejected")
        void blankAmountRejected() {
            assertNull(service.validateAmount("   "));
        }

        @Test
        @DisplayName("Zero amount is rejected")
        void zeroAmountRejected() {
            assertNull(service.validateAmount("0"));
        }

        @Test
        @DisplayName("Zero decimal amount is rejected")
        void zeroDecimalAmountRejected() {
            assertNull(service.validateAmount("0.00"));
        }

        @Test
        @DisplayName("Negative amount is rejected")
        void negativeAmountRejected() {
            assertNull(service.validateAmount("-100.00"));
        }

        @Test
        @DisplayName("Amount with embedded spaces is rejected")
        void embeddedSpacesRejected() {
            assertNull(service.validateAmount("10 0.00"));
        }

        @Test
        @DisplayName("Amount with multiple decimal points is rejected")
        void multipleDecimalPointsRejected() {
            assertNull(service.validateAmount("10.0.0"));
        }

        @Test
        @DisplayName("Amount with more than two decimal places is rejected")
        void moreThanTwoDecimalPlacesRejected() {
            assertNull(service.validateAmount("100.123"));
        }

        @Test
        @DisplayName("Amount with non-numeric characters is rejected")
        void nonNumericAmountRejected() {
            assertNull(service.validateAmount("abc"));
        }

        @Test
        @DisplayName("Amount with leading spaces is accepted")
        void leadingSpacesAccepted() {
            BigDecimal result = service.validateAmount("   100.50");
            assertNotNull(result);
            assertEquals(new BigDecimal("100.50"), result);
        }

        @Test
        @DisplayName("Amount with trailing spaces is accepted")
        void trailingSpacesAccepted() {
            BigDecimal result = service.validateAmount("100.50   ");
            assertNotNull(result);
            assertEquals(new BigDecimal("100.50"), result);
        }

        @Test
        @DisplayName("Large amount is accepted")
        void largeAmountAccepted() {
            BigDecimal result = service.validateAmount("9999999999.99");
            assertNotNull(result);
            assertEquals(new BigDecimal("9999999999.99"), result);
        }

        @Test
        @DisplayName("Amount '0.01' (smallest valid) is accepted")
        void smallestValidAmountAccepted() {
            BigDecimal result = service.validateAmount("0.01");
            assertNotNull(result);
            assertEquals(new BigDecimal("0.01"), result);
        }

        @Test
        @DisplayName("Amount with exactly two decimal places")
        void exactlyTwoDecimalPlaces() {
            BigDecimal result = service.validateAmount("50.12");
            assertNotNull(result);
            assertEquals(new BigDecimal("50.12"), result);
        }
    }

    // ==================================================================
    // getAmountValidationMessage tests
    // ==================================================================

    @Nested
    @DisplayName("Amount Validation Messages")
    class AmountValidationMessageTests {

        @Test
        @DisplayName("Null amount gives 'must be numeric' message")
        void nullAmountMessage() {
            assertEquals("The Amount entered must be numeric.",
                    service.getAmountValidationMessage(null));
        }

        @Test
        @DisplayName("Empty amount gives 'must be numeric' message")
        void emptyAmountMessage() {
            assertEquals("The Amount entered must be numeric.",
                    service.getAmountValidationMessage(""));
        }

        @Test
        @DisplayName("Blank amount gives 'must be numeric' message")
        void blankAmountMessage() {
            assertEquals("The Amount entered must be numeric.",
                    service.getAmountValidationMessage("   "));
        }

        @Test
        @DisplayName("Zero amount gives 'positive amount' message")
        void zeroAmountMessage() {
            assertEquals("Please supply a positive amount.",
                    service.getAmountValidationMessage("0"));
        }

        @Test
        @DisplayName("Negative amount gives 'positive amount' message")
        void negativeAmountMessage() {
            assertEquals("Please supply a positive amount.",
                    service.getAmountValidationMessage("-50"));
        }

        @Test
        @DisplayName("Embedded spaces gives 'without embedded spaces' message")
        void embeddedSpacesMessage() {
            assertEquals(
                    "Please supply a numeric amount without embedded spaces.",
                    service.getAmountValidationMessage("10 0"));
        }

        @Test
        @DisplayName("Non-numeric gives 'numeric amount' message")
        void nonNumericMessage() {
            assertEquals("Please supply a numeric amount.",
                    service.getAmountValidationMessage("abc"));
        }

        @Test
        @DisplayName("Multiple decimal points gives specific message")
        void multipleDecimalPointsMessage() {
            assertEquals("Use one decimal point for amount only.",
                    service.getAmountValidationMessage("1.2.3"));
        }

        @Test
        @DisplayName("Too many decimal places gives specific message")
        void tooManyDecimalPlacesMessage() {
            assertEquals("Only up to two decimal places are supported.",
                    service.getAmountValidationMessage("1.234"));
        }

        @Test
        @DisplayName("Zero decimal amount gives 'non-zero' message")
        void zeroDecimalAmountMessage() {
            assertEquals("Please supply a non-zero amount.",
                    service.getAmountValidationMessage("0.00"));
        }

        @Test
        @DisplayName("Valid amount returns null (no error)")
        void validAmountReturnsNull() {
            assertNull(service.getAmountValidationMessage("100.00"));
        }
    }

    // ==================================================================
    // GET-ACC-DATA tests (XFRFUN delegation & error code mapping)
    // ==================================================================

    @Nested
    @DisplayName("GET-ACC-DATA — Transfer Function Delegation")
    class GetAccDataTests {

        @Test
        @DisplayName("Successful transfer returns balances and success message")
        void successfulTransfer() {
            TransferResult result = service.processTransfer(
                    "12345678", "87654321", "100.00");
            assertTrue(result.isSuccessful());
            assertEquals("Transfer successfully applied.",
                    result.getMessage());
            assertEquals("12345678", result.getFromAccountNumber());
            assertEquals("987654", result.getFromSortCode());
            assertEquals("87654321", result.getToAccountNumber());
            assertEquals("123456", result.getToSortCode());
            assertEquals(new BigDecimal("900.00"),
                    result.getFromAvailableBalance());
            assertEquals(new BigDecimal("900.00"),
                    result.getFromActualBalance());
            assertEquals(new BigDecimal("1100.00"),
                    result.getToAvailableBalance());
            assertEquals(new BigDecimal("1100.00"),
                    result.getToActualBalance());
        }

        @Test
        @DisplayName("Fail code '1' — FROM account not found")
        void failCode1FromAccountNotFound() {
            TransferFundsService svc = new TransferFundsService(
                    req -> new TransferFunctionResponse(
                            "12345678", "987654", "87654321", "123456",
                            BigDecimal.ZERO, BigDecimal.ZERO,
                            BigDecimal.ZERO, BigDecimal.ZERO,
                            '1', 'N'));
            TransferResult result = svc.processTransfer(
                    "12345678", "87654321", "100.00");
            assertFalse(result.isSuccessful());
            assertTrue(result.getMessage().contains(
                    "FROM ACCOUNT no was not found"));
        }

        @Test
        @DisplayName("Fail code '2' — TO account not found")
        void failCode2ToAccountNotFound() {
            TransferFundsService svc = new TransferFundsService(
                    req -> new TransferFunctionResponse(
                            "12345678", "987654", "87654321", "123456",
                            BigDecimal.ZERO, BigDecimal.ZERO,
                            BigDecimal.ZERO, BigDecimal.ZERO,
                            '2', 'N'));
            TransferResult result = svc.processTransfer(
                    "12345678", "87654321", "100.00");
            assertFalse(result.isSuccessful());
            assertTrue(result.getMessage().contains(
                    "TO ACCOUNT no was not found"));
        }

        @Test
        @DisplayName("Fail code '3' — unexpected error")
        void failCode3UnexpectedError() {
            TransferFundsService svc = new TransferFundsService(
                    req -> new TransferFunctionResponse(
                            "12345678", "987654", "87654321", "123456",
                            BigDecimal.ZERO, BigDecimal.ZERO,
                            BigDecimal.ZERO, BigDecimal.ZERO,
                            '3', 'N'));
            TransferResult result = svc.processTransfer(
                    "12345678", "87654321", "100.00");
            assertFalse(result.isSuccessful());
            assertTrue(result.getMessage().contains("unexpected error"));
        }

        @Test
        @DisplayName("Fail code '4' — amount must be greater than zero")
        void failCode4AmountNotPositive() {
            TransferFundsService svc = new TransferFundsService(
                    req -> new TransferFunctionResponse(
                            "12345678", "987654", "87654321", "123456",
                            BigDecimal.ZERO, BigDecimal.ZERO,
                            BigDecimal.ZERO, BigDecimal.ZERO,
                            '4', 'N'));
            TransferResult result = svc.processTransfer(
                    "12345678", "87654321", "100.00");
            assertFalse(result.isSuccessful());
            assertTrue(result.getMessage().contains(
                    "amount greater than zero"));
        }

        @Test
        @DisplayName("Unknown fail code gives generic error message")
        void unknownFailCode() {
            TransferFundsService svc = new TransferFundsService(
                    req -> new TransferFunctionResponse(
                            "12345678", "987654", "87654321", "123456",
                            BigDecimal.ZERO, BigDecimal.ZERO,
                            BigDecimal.ZERO, BigDecimal.ZERO,
                            '9', 'N'));
            TransferResult result = svc.processTransfer(
                    "12345678", "87654321", "100.00");
            assertFalse(result.isSuccessful());
            assertTrue(result.getMessage().contains("due to an error"));
        }

        @Test
        @DisplayName("Success not 'Y' or 'N' gives 'unable to determine' msg")
        void successNotYorN() {
            TransferFundsService svc = new TransferFundsService(
                    req -> new TransferFunctionResponse(
                            "12345678", "987654", "87654321", "123456",
                            BigDecimal.ZERO, BigDecimal.ZERO,
                            BigDecimal.ZERO, BigDecimal.ZERO,
                            ' ', 'X'));
            TransferResult result = svc.processTransfer(
                    "12345678", "87654321", "100.00");
            assertFalse(result.isSuccessful());
            assertTrue(result.getMessage().contains(
                    "unable to determine success"));
        }

        @Test
        @DisplayName("Gateway throws RuntimeException — handled gracefully")
        void gatewayThrowsException() {
            TransferFundsService svc = new TransferFundsService(
                    req -> {
                        throw new RuntimeException("Connection lost");
                    });
            TransferResult result = svc.processTransfer(
                    "12345678", "87654321", "100.00");
            assertFalse(result.isSuccessful());
            assertTrue(result.getMessage().contains("unexpected error"));
        }

        @Test
        @DisplayName("Failed transfer zeroes out all balances")
        void failedTransferZeroesBalances() {
            TransferFundsService svc = new TransferFundsService(
                    req -> new TransferFunctionResponse(
                            "12345678", "987654", "87654321", "123456",
                            new BigDecimal("500.00"),
                            new BigDecimal("500.00"),
                            new BigDecimal("500.00"),
                            new BigDecimal("500.00"),
                            '1', 'N'));
            TransferResult result = svc.processTransfer(
                    "12345678", "87654321", "100.00");
            assertFalse(result.isSuccessful());
            assertEquals(BigDecimal.ZERO, result.getFromAvailableBalance());
            assertEquals(BigDecimal.ZERO, result.getFromActualBalance());
            assertEquals(BigDecimal.ZERO, result.getToAvailableBalance());
            assertEquals(BigDecimal.ZERO, result.getToActualBalance());
        }
    }

    // ==================================================================
    // End-to-end integration-style tests
    // ==================================================================

    @Nested
    @DisplayName("End-to-End Transfer Scenarios")
    class EndToEndTests {

        @Test
        @DisplayName("Full valid transfer flow")
        void fullValidTransferFlow() {
            TransferFundsService svc = new TransferFundsService(req -> {
                assertEquals("12345678", req.fromAccountNumber());
                assertEquals("87654321", req.toAccountNumber());
                assertEquals(new BigDecimal("250.50"),
                        req.amount());
                return new TransferFunctionResponse(
                        "12345678", "987654", "87654321", "123456",
                        new BigDecimal("749.50"), new BigDecimal("749.50"),
                        new BigDecimal("1250.50"), new BigDecimal("1250.50"),
                        ' ', 'Y');
            });

            TransferResult result = svc.processTransfer(
                    "12345678", "87654321", "250.50");
            assertTrue(result.isSuccessful());
            assertEquals("Transfer successfully applied.",
                    result.getMessage());
            assertEquals(new BigDecimal("749.50"),
                    result.getFromActualBalance());
            assertEquals(new BigDecimal("1250.50"),
                    result.getToActualBalance());
        }

        @Test
        @DisplayName("Transfer with integer amount (no decimals)")
        void transferWithIntegerAmount() {
            TransferResult result = service.processTransfer(
                    "12345678", "87654321", "500");
            assertTrue(result.isSuccessful());
        }

        @Test
        @DisplayName("Transfer with leading-space amount")
        void transferWithLeadingSpaceAmount() {
            TransferResult result = service.processTransfer(
                    "12345678", "87654321", "  100.00");
            assertTrue(result.isSuccessful());
        }

        @Test
        @DisplayName("Invalid FROM + valid TO + valid amount → FROM error")
        void invalidFromBeatsOtherValidation() {
            TransferResult result = service.processTransfer(
                    "ABCD", "87654321", "100.00");
            assertFalse(result.isSuccessful());
            assertEquals("Please enter a FROM account no",
                    result.getMessage());
        }

        @Test
        @DisplayName("Valid FROM + invalid amount → amount error message")
        void validAccountsInvalidAmount() {
            TransferResult result = service.processTransfer(
                    "12345678", "87654321", "abc");
            assertFalse(result.isSuccessful());
        }
    }

    // ==================================================================
    // DEEDIT utility tests
    // ==================================================================

    @Nested
    @DisplayName("DEEDIT Utility")
    class DeeditTests {

        @Test
        @DisplayName("Strips commas from formatted number")
        void stripsCommas() {
            assertEquals("12345678",
                    TransferFundsService.deedit("12,345,678"));
        }

        @Test
        @DisplayName("Strips currency symbols")
        void stripsCurrencySymbols() {
            assertEquals("12345",
                    TransferFundsService.deedit("$12,345"));
        }

        @Test
        @DisplayName("Preserves leading minus")
        void preservesLeadingMinus() {
            assertEquals("-123",
                    TransferFundsService.deedit("-123"));
        }

        @Test
        @DisplayName("Handles null input")
        void handlesNull() {
            assertEquals("", TransferFundsService.deedit(null));
        }

        @Test
        @DisplayName("Handles empty input")
        void handlesEmpty() {
            assertEquals("", TransferFundsService.deedit(""));
        }

        @Test
        @DisplayName("Plain numeric passes through")
        void plainNumericPassthrough() {
            assertEquals("12345678",
                    TransferFundsService.deedit("12345678"));
        }
    }

    // ==================================================================
    // isNumeric utility tests
    // ==================================================================

    @Nested
    @DisplayName("isNumeric Utility")
    class IsNumericTests {

        @Test
        @DisplayName("Numeric string returns true")
        void numericReturnsTrue() {
            assertTrue(TransferFundsService.isNumeric("12345678"));
        }

        @Test
        @DisplayName("Non-numeric string returns false")
        void nonNumericReturnsFalse() {
            assertFalse(TransferFundsService.isNumeric("123abc"));
        }

        @Test
        @DisplayName("Null returns false")
        void nullReturnsFalse() {
            assertFalse(TransferFundsService.isNumeric(null));
        }

        @Test
        @DisplayName("Blank returns false")
        void blankReturnsFalse() {
            assertFalse(TransferFundsService.isNumeric("   "));
        }

        @Test
        @DisplayName("Empty returns false")
        void emptyReturnsFalse() {
            assertFalse(TransferFundsService.isNumeric(""));
        }

        @Test
        @DisplayName("String with decimal point returns false")
        void decimalReturnsFalse() {
            assertFalse(TransferFundsService.isNumeric("123.45"));
        }
    }

    // ==================================================================
    // TransferResult builder tests
    // ==================================================================

    @Nested
    @DisplayName("TransferResult Builder")
    class TransferResultTests {

        @Test
        @DisplayName("Builder defaults balances to ZERO")
        void builderDefaultsBalancesToZero() {
            TransferResult result = TransferResult.builder()
                    .successful(false)
                    .message("test")
                    .build();
            assertEquals(BigDecimal.ZERO, result.getFromAvailableBalance());
            assertEquals(BigDecimal.ZERO, result.getFromActualBalance());
            assertEquals(BigDecimal.ZERO, result.getToAvailableBalance());
            assertEquals(BigDecimal.ZERO, result.getToActualBalance());
        }

        @Test
        @DisplayName("Builder sets all fields correctly")
        void builderSetsAllFields() {
            TransferResult result = TransferResult.builder()
                    .successful(true)
                    .message("ok")
                    .fromAccountNumber("11111111")
                    .fromSortCode("222222")
                    .toAccountNumber("33333333")
                    .toSortCode("444444")
                    .fromAvailableBalance(new BigDecimal("100"))
                    .fromActualBalance(new BigDecimal("200"))
                    .toAvailableBalance(new BigDecimal("300"))
                    .toActualBalance(new BigDecimal("400"))
                    .build();
            assertTrue(result.isSuccessful());
            assertEquals("ok", result.getMessage());
            assertEquals("11111111", result.getFromAccountNumber());
            assertEquals("222222", result.getFromSortCode());
            assertEquals("33333333", result.getToAccountNumber());
            assertEquals("444444", result.getToSortCode());
            assertEquals(new BigDecimal("100"),
                    result.getFromAvailableBalance());
            assertEquals(new BigDecimal("200"),
                    result.getFromActualBalance());
            assertEquals(new BigDecimal("300"),
                    result.getToAvailableBalance());
            assertEquals(new BigDecimal("400"),
                    result.getToActualBalance());
        }
    }

    // ==================================================================
    // Record tests
    // ==================================================================

    @Nested
    @DisplayName("Transfer Function Records")
    class RecordTests {

        @Test
        @DisplayName("TransferFunctionRequest holds correct values")
        void requestRecordValues() {
            TransferFunctionRequest req = new TransferFunctionRequest(
                    "12345678", "87654321", new BigDecimal("100.00"));
            assertEquals("12345678", req.fromAccountNumber());
            assertEquals("87654321", req.toAccountNumber());
            assertEquals(new BigDecimal("100.00"), req.amount());
        }

        @Test
        @DisplayName("TransferFunctionResponse holds correct values")
        void responseRecordValues() {
            TransferFunctionResponse resp = new TransferFunctionResponse(
                    "12345678", "987654", "87654321", "123456",
                    new BigDecimal("900.00"), new BigDecimal("900.00"),
                    new BigDecimal("1100.00"), new BigDecimal("1100.00"),
                    ' ', 'Y');
            assertEquals("12345678", resp.fromAccountNumber());
            assertEquals("987654", resp.fromSortCode());
            assertEquals("87654321", resp.toAccountNumber());
            assertEquals("123456", resp.toSortCode());
            assertEquals(new BigDecimal("900.00"),
                    resp.fromAvailableBalance());
            assertEquals(new BigDecimal("900.00"),
                    resp.fromActualBalance());
            assertEquals(new BigDecimal("1100.00"),
                    resp.toAvailableBalance());
            assertEquals(new BigDecimal("1100.00"),
                    resp.toActualBalance());
            assertEquals(' ', resp.failCode());
            assertEquals('Y', resp.success());
        }
    }

    // ==================================================================
    // Constructor validation
    // ==================================================================

    @Nested
    @DisplayName("Constructor Validation")
    class ConstructorTests {

        @Test
        @DisplayName("Null gateway throws NullPointerException")
        void nullGatewayThrows() {
            try {
                new TransferFundsService(null);
                assertTrue(false, "Expected NullPointerException");
            } catch (NullPointerException e) {
                assertTrue(e.getMessage().contains(
                        "transferFunctionGateway"));
            }
        }
    }
}
