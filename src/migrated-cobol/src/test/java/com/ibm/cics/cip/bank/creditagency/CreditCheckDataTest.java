/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.creditagency;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link CreditCheckData} data transfer object.
 */
class CreditCheckDataTest {

    @Test
    @DisplayName("should create data with all-args constructor")
    void shouldCreateDataWithAllArgsConstructor() {
        LocalDate dob = LocalDate.of(1990, 3, 25);
        LocalDate reviewDate = LocalDate.of(2024, 2, 10);

        CreditCheckData data = new CreditCheckData(
                "CRDA", "987654", "0000000042",
                "Jane Doe", "456 Oak Ave",
                dob, 750, reviewDate, "Y", " ");

        assertEquals("CRDA", data.getEyecatcher());
        assertEquals("987654", data.getSortCode());
        assertEquals("0000000042", data.getAccountNumber());
        assertEquals("Jane Doe", data.getCustomerName());
        assertEquals("456 Oak Ave", data.getCustomerAddress());
        assertEquals(dob, data.getDateOfBirth());
        assertEquals(750, data.getCreditScore());
        assertEquals(reviewDate, data.getCreditScoreReviewDate());
        assertEquals("Y", data.getSuccessFlag());
        assertEquals(" ", data.getFailCode());
    }

    @Test
    @DisplayName("should create data with no-args constructor and setters")
    void shouldCreateDataWithNoArgsConstructor() {
        CreditCheckData data = new CreditCheckData();
        data.setEyecatcher("TEST");
        data.setSortCode("123456");
        data.setAccountNumber("0000000099");
        data.setCustomerName("Bob");
        data.setCustomerAddress("789 Pine Rd");
        data.setDateOfBirth(LocalDate.of(2000, 12, 31));
        data.setCreditScore(500);
        data.setCreditScoreReviewDate(LocalDate.of(2024, 6, 1));
        data.setSuccessFlag("N");
        data.setFailCode("X");

        assertEquals("TEST", data.getEyecatcher());
        assertEquals("123456", data.getSortCode());
        assertEquals("0000000099", data.getAccountNumber());
        assertEquals("Bob", data.getCustomerName());
        assertEquals("789 Pine Rd", data.getCustomerAddress());
        assertEquals(LocalDate.of(2000, 12, 31), data.getDateOfBirth());
        assertEquals(500, data.getCreditScore());
        assertEquals(LocalDate.of(2024, 6, 1), data.getCreditScoreReviewDate());
        assertEquals("N", data.getSuccessFlag());
        assertEquals("X", data.getFailCode());
    }

    @Test
    @DisplayName("should correctly implement equals")
    void shouldImplementEquals() {
        LocalDate dob = LocalDate.of(1985, 6, 15);
        LocalDate review = LocalDate.of(2024, 1, 15);

        CreditCheckData data1 = new CreditCheckData(
                "CRDA", "987654", "0000000001",
                "John", "123 Main St", dob, 500, review, "Y", " ");
        CreditCheckData data2 = new CreditCheckData(
                "CRDA", "987654", "0000000001",
                "John", "123 Main St", dob, 500, review, "Y", " ");

        assertEquals(data1, data2);
        assertEquals(data1.hashCode(), data2.hashCode());
    }

    @Test
    @DisplayName("should detect inequality when credit score differs")
    void shouldDetectInequalityOnDifferentScore() {
        LocalDate dob = LocalDate.of(1985, 6, 15);
        LocalDate review = LocalDate.of(2024, 1, 15);

        CreditCheckData data1 = new CreditCheckData(
                "CRDA", "987654", "0000000001",
                "John", "123 Main St", dob, 500, review, "Y", " ");
        CreditCheckData data2 = new CreditCheckData(
                "CRDA", "987654", "0000000001",
                "John", "123 Main St", dob, 700, review, "Y", " ");

        assertNotEquals(data1, data2);
    }

    @Test
    @DisplayName("should produce readable toString")
    void shouldProduceReadableToString() {
        CreditCheckData data = new CreditCheckData();
        data.setEyecatcher("TEST");
        data.setSortCode("987654");
        data.setCreditScore(750);

        String str = data.toString();
        assertTrue(str.contains("TEST"));
        assertTrue(str.contains("987654"));
        assertTrue(str.contains("750"));
    }

    @Test
    @DisplayName("should handle null fields gracefully")
    void shouldHandleNullFieldsGracefully() {
        CreditCheckData data = new CreditCheckData();

        assertNull(data.getEyecatcher());
        assertNull(data.getSortCode());
        assertNull(data.getAccountNumber());
        assertNull(data.getCustomerName());
        assertNull(data.getCustomerAddress());
        assertNull(data.getDateOfBirth());
        assertEquals(0, data.getCreditScore());
        assertNull(data.getCreditScoreReviewDate());
        assertNull(data.getSuccessFlag());
        assertNull(data.getFailCode());
    }
}
