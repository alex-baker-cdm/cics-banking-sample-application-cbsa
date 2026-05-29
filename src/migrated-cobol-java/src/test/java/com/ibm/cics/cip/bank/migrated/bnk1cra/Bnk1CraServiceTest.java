/*
 * Copyright IBM Corp. 2023
 */
package com.ibm.cics.cip.bank.migrated.bnk1cra;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.ibm.cics.cip.bank.migrated.bnk1cra.AccountOperationService.AccountOperationRequest;
import com.ibm.cics.cip.bank.migrated.bnk1cra.AccountOperationService.AccountOperationResponse;
import com.ibm.cics.cip.bank.migrated.bnk1cra.Bnk1CraService.OriginContext;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for Bnk1CraService, covering all business logic paths
 * from the original COBOL program BNK1CRA.
 */
class Bnk1CraServiceTest {

    private static final OriginContext TEST_ORIGIN = new OriginContext(
            "TESTAPPL", "TESTUSER", "TESTFACL", "TESTNET", 0);

    private AccountOperationRequest capturedRequest;
    private AccountOperationResponse stubbedResponse;
    private Bnk1CraService service;

    @BeforeEach
    void setUp() {
        capturedRequest = null;
        stubbedResponse = new AccountOperationResponse(
                true, null, "123456", new BigDecimal("1000.00"), new BigDecimal("950.00"));

        AccountOperationService stubService = request -> {
            capturedRequest = request;
            return stubbedResponse;
        };

        service = new Bnk1CraService(stubService);
    }

    @Nested
    class InputValidation {

        @Test
        void shouldRejectNullAccountNumber() {
            CreditDebitRequest request = new CreditDebitRequest(null, '+', new BigDecimal("100.00"));
            CreditDebitResult result = service.processCreditDebit(request, TEST_ORIGIN);

            assertFalse(result.success());
            assertEquals("Please enter an account number.", result.message());
        }

        @Test
        void shouldRejectBlankAccountNumber() {
            CreditDebitRequest request = new CreditDebitRequest("        ", '+', new BigDecimal("100.00"));
            CreditDebitResult result = service.processCreditDebit(request, TEST_ORIGIN);

            assertFalse(result.success());
            assertEquals("Please enter an account number.", result.message());
        }

        @Test
        void shouldRejectNonNumericAccountNumber() {
            CreditDebitRequest request = new CreditDebitRequest("ABC12345", '+', new BigDecimal("100.00"));
            CreditDebitResult result = service.processCreditDebit(request, TEST_ORIGIN);

            assertFalse(result.success());
            assertEquals("Please enter an account number.", result.message());
        }

        @Test
        void shouldRejectZeroAccountNumber() {
            CreditDebitRequest request = new CreditDebitRequest("00000000", '+', new BigDecimal("100.00"));
            CreditDebitResult result = service.processCreditDebit(request, TEST_ORIGIN);

            assertFalse(result.success());
            assertEquals("Please enter a non zero account number.", result.message());
        }

        @Test
        void shouldRejectInvalidSign() {
            CreditDebitRequest request = new CreditDebitRequest("12345678", '*', new BigDecimal("100.00"));
            CreditDebitResult result = service.processCreditDebit(request, TEST_ORIGIN);

            assertFalse(result.success());
            assertEquals("Please enter + or - preceding the amount", result.message());
        }

        @Test
        void shouldRejectNullAmount() {
            CreditDebitRequest request = new CreditDebitRequest("12345678", '+', null);
            CreditDebitResult result = service.processCreditDebit(request, TEST_ORIGIN);

            assertFalse(result.success());
            assertEquals("The Amount entered must be numeric.", result.message());
        }

        @Test
        void shouldAcceptValidCreditRequest() {
            CreditDebitRequest request = new CreditDebitRequest("12345678", '+', new BigDecimal("100.00"));
            CreditDebitResult result = service.processCreditDebit(request, TEST_ORIGIN);

            assertTrue(result.success());
        }

        @Test
        void shouldAcceptValidDebitRequest() {
            CreditDebitRequest request = new CreditDebitRequest("12345678", '-', new BigDecimal("50.00"));
            CreditDebitResult result = service.processCreditDebit(request, TEST_ORIGIN);

            assertTrue(result.success());
        }

        @Test
        void shouldAcceptAccountWithFormattingChars() {
            CreditDebitRequest request = new CreditDebitRequest("1234,567", '+', new BigDecimal("100.00"));
            CreditDebitResult result = service.processCreditDebit(request, TEST_ORIGIN);

            assertTrue(result.success());
        }
    }

    @Nested
    class CreditDebitProcessing {

        @Test
        void shouldPassPositiveAmountForCredit() {
            CreditDebitRequest request = new CreditDebitRequest("12345678", '+', new BigDecimal("250.00"));
            service.processCreditDebit(request, TEST_ORIGIN);

            assertNotNull(capturedRequest);
            assertEquals("12345678", capturedRequest.accountNumber());
            assertEquals(new BigDecimal("250.00"), capturedRequest.amount());
        }

        @Test
        void shouldPassNegativeAmountForDebit() {
            CreditDebitRequest request = new CreditDebitRequest("12345678", '-', new BigDecimal("250.00"));
            service.processCreditDebit(request, TEST_ORIGIN);

            assertNotNull(capturedRequest);
            assertEquals("12345678", capturedRequest.accountNumber());
            assertEquals(new BigDecimal("-250.00"), capturedRequest.amount());
        }

        @Test
        void shouldPassOriginContextToService() {
            CreditDebitRequest request = new CreditDebitRequest("12345678", '+', new BigDecimal("100.00"));
            service.processCreditDebit(request, TEST_ORIGIN);

            assertNotNull(capturedRequest);
            assertEquals("TESTAPPL", capturedRequest.applId());
            assertEquals("TESTUSER", capturedRequest.userId());
            assertEquals("TESTFACL", capturedRequest.facilityName());
            assertEquals("TESTNET", capturedRequest.networkId());
            assertEquals(0, capturedRequest.facilityType());
        }

        @Test
        void shouldReturnSuccessWithBalances() {
            CreditDebitRequest request = new CreditDebitRequest("12345678", '+', new BigDecimal("100.00"));
            CreditDebitResult result = service.processCreditDebit(request, TEST_ORIGIN);

            assertTrue(result.success());
            assertEquals("Amount successfully applied to the account.", result.message());
            assertEquals("12345678", result.accountNumber());
            assertEquals("123456", result.sortCode());
            assertEquals(new BigDecimal("1000.00"), result.availableBalance());
            assertEquals(new BigDecimal("950.00"), result.actualBalance());
        }

        @Test
        void shouldHandleNullOriginContext() {
            CreditDebitRequest request = new CreditDebitRequest("12345678", '+', new BigDecimal("100.00"));
            CreditDebitResult result = service.processCreditDebit(request, null);

            assertTrue(result.success());
            assertEquals("", capturedRequest.applId());
            assertEquals("", capturedRequest.userId());
        }
    }

    @Nested
    class FailureHandling {

        @Test
        void shouldHandleAccountNotFound() {
            stubbedResponse = new AccountOperationResponse(
                    false, '1', "654321", null, null);

            CreditDebitRequest request = new CreditDebitRequest("99999999", '+', new BigDecimal("100.00"));
            CreditDebitResult result = service.processCreditDebit(request, TEST_ORIGIN);

            assertFalse(result.success());
            assertTrue(result.message().contains("ACCOUNT no was not found"));
            assertTrue(result.message().contains("654321"));
        }

        @Test
        void shouldHandleUnexpectedError() {
            stubbedResponse = new AccountOperationResponse(
                    false, '2', null, null, null);

            CreditDebitRequest request = new CreditDebitRequest("12345678", '+', new BigDecimal("100.00"));
            CreditDebitResult result = service.processCreditDebit(request, TEST_ORIGIN);

            assertFalse(result.success());
            assertTrue(result.message().contains("unexpected error"));
        }

        @Test
        void shouldHandleInsufficientFunds() {
            stubbedResponse = new AccountOperationResponse(
                    false, '3', null, null, null);

            CreditDebitRequest request = new CreditDebitRequest("12345678", '-', new BigDecimal("99999.00"));
            CreditDebitResult result = service.processCreditDebit(request, TEST_ORIGIN);

            assertFalse(result.success());
            assertTrue(result.message().contains("insufficient funds"));
        }

        @Test
        void shouldHandleUnknownFailCode() {
            stubbedResponse = new AccountOperationResponse(
                    false, '9', null, null, null);

            CreditDebitRequest request = new CreditDebitRequest("12345678", '+', new BigDecimal("100.00"));
            CreditDebitResult result = service.processCreditDebit(request, TEST_ORIGIN);

            assertFalse(result.success());
            assertTrue(result.message().contains("unexpected error"));
            assertTrue(result.message().contains("9"));
        }

        @Test
        void shouldHandleNullFailCode() {
            stubbedResponse = new AccountOperationResponse(
                    false, null, null, null, null);

            CreditDebitRequest request = new CreditDebitRequest("12345678", '+', new BigDecimal("100.00"));
            CreditDebitResult result = service.processCreditDebit(request, TEST_ORIGIN);

            assertFalse(result.success());
            assertTrue(result.message().contains("unexpected error"));
        }
    }

    @Nested
    class FailCodeEnum {

        @Test
        void shouldLookupAccountNotFound() {
            assertEquals(FailCode.ACCOUNT_NOT_FOUND, FailCode.fromCode('1'));
        }

        @Test
        void shouldLookupUnexpectedError() {
            assertEquals(FailCode.UNEXPECTED_ERROR, FailCode.fromCode('2'));
        }

        @Test
        void shouldLookupInsufficientFunds() {
            assertEquals(FailCode.INSUFFICIENT_FUNDS, FailCode.fromCode('3'));
        }

        @Test
        void shouldReturnNullForUnknownCode() {
            assertNull(FailCode.fromCode('X'));
        }
    }
}
