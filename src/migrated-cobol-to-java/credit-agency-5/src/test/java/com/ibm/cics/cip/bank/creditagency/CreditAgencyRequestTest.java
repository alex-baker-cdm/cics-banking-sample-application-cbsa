/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.creditagency;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the CreditAgencyRequest data class, ensuring proper mapping
 * of the COBOL WS-CONT-IN record structure fields.
 */
class CreditAgencyRequestTest {

    @Nested
    @DisplayName("Date of birth decomposition")
    class DateOfBirthTests {

        @Test
        @DisplayName("should decompose DDMMYYYY into day, month, year")
        void shouldDecomposeDateOfBirth() {
            CreditAgencyRequest request = new CreditAgencyRequest();
            request.setDateOfBirth(15061985);

            assertEquals(15, request.getBirthDay());
            assertEquals(6, request.getBirthMonth());
            assertEquals(1985, request.getBirthYear());
        }

        @Test
        @DisplayName("should handle single-digit day and month")
        void shouldHandleSingleDigitDayAndMonth() {
            CreditAgencyRequest request = new CreditAgencyRequest();
            request.setDateOfBirth(1012000);

            assertEquals(1, request.getBirthDay());
            assertEquals(1, request.getBirthMonth());
            assertEquals(2000, request.getBirthYear());
        }

        @Test
        @DisplayName("should handle last day of year")
        void shouldHandleLastDayOfYear() {
            CreditAgencyRequest request = new CreditAgencyRequest();
            request.setDateOfBirth(31121999);

            assertEquals(31, request.getBirthDay());
            assertEquals(12, request.getBirthMonth());
            assertEquals(1999, request.getBirthYear());
        }

        @Test
        @DisplayName("should handle zero date")
        void shouldHandleZeroDate() {
            CreditAgencyRequest request = new CreditAgencyRequest();
            request.setDateOfBirth(0);

            assertEquals(0, request.getBirthDay());
            assertEquals(0, request.getBirthMonth());
            assertEquals(0, request.getBirthYear());
        }
    }

    @Nested
    @DisplayName("Constructor tests")
    class ConstructorTests {

        @Test
        @DisplayName("default constructor should create empty request")
        void defaultConstructorShouldCreateEmptyRequest() {
            CreditAgencyRequest request = new CreditAgencyRequest();

            assertNull(request.getEyecatcher());
            assertEquals(0, request.getSortCode());
            assertEquals(0, request.getAccountNumber());
            assertNull(request.getCustomerName());
            assertEquals(0, request.getCreditScore());
        }

        @Test
        @DisplayName("full constructor should set all fields")
        void fullConstructorShouldSetAllFields() {
            CreditAgencyRequest request = new CreditAgencyRequest(
                    "CRDA", 987654, 1234567890L,
                    "John Smith", "123 Main St",
                    15061985, 750, 20230615, "Y", " "
            );

            assertEquals("CRDA", request.getEyecatcher());
            assertEquals(987654, request.getSortCode());
            assertEquals(1234567890L, request.getAccountNumber());
            assertEquals("John Smith", request.getCustomerName());
            assertEquals("123 Main St", request.getCustomerAddress());
            assertEquals(15061985, request.getDateOfBirth());
            assertEquals(750, request.getCreditScore());
            assertEquals(20230615, request.getCreditScoreReviewDate());
            assertEquals("Y", request.getSuccessFlag());
            assertEquals(" ", request.getFailCode());
        }
    }

    @Nested
    @DisplayName("Getters and setters")
    class GetterSetterTests {

        @Test
        @DisplayName("should set and get eyecatcher")
        void shouldSetAndGetEyecatcher() {
            CreditAgencyRequest request = new CreditAgencyRequest();
            request.setEyecatcher("TEST");
            assertEquals("TEST", request.getEyecatcher());
        }

        @Test
        @DisplayName("should set and get sort code")
        void shouldSetAndGetSortCode() {
            CreditAgencyRequest request = new CreditAgencyRequest();
            request.setSortCode(987654);
            assertEquals(987654, request.getSortCode());
        }

        @Test
        @DisplayName("should set and get account number")
        void shouldSetAndGetAccountNumber() {
            CreditAgencyRequest request = new CreditAgencyRequest();
            request.setAccountNumber(9999999999L);
            assertEquals(9999999999L, request.getAccountNumber());
        }

        @Test
        @DisplayName("should set and get credit score")
        void shouldSetAndGetCreditScore() {
            CreditAgencyRequest request = new CreditAgencyRequest();
            request.setCreditScore(500);
            assertEquals(500, request.getCreditScore());
        }

        @Test
        @DisplayName("should set and get success flag")
        void shouldSetAndGetSuccessFlag() {
            CreditAgencyRequest request = new CreditAgencyRequest();
            request.setSuccessFlag("Y");
            assertEquals("Y", request.getSuccessFlag());
        }

        @Test
        @DisplayName("should set and get fail code")
        void shouldSetAndGetFailCode() {
            CreditAgencyRequest request = new CreditAgencyRequest();
            request.setFailCode("A");
            assertEquals("A", request.getFailCode());
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTests {

        @Test
        @DisplayName("should produce readable string representation")
        void shouldProduceReadableString() {
            CreditAgencyRequest request = new CreditAgencyRequest(
                    "CRDA", 987654, 1234567890L,
                    "John Smith", "123 Main St",
                    15061985, 750, 20230615, "Y", " "
            );

            String str = request.toString();

            assertTrue(str.contains("CRDA"));
            assertTrue(str.contains("987654"));
            assertTrue(str.contains("1234567890"));
            assertTrue(str.contains("John Smith"));
            assertTrue(str.contains("750"));
        }
    }
}
