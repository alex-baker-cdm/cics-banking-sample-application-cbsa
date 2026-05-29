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
 * Tests for {@link ContainerData}, the Java mapping of the COBOL
 * WS-CONT-IN record.
 */
class ContainerDataTest {

    @Test
    @DisplayName("All-args constructor sets every field correctly")
    void allArgsConstructor() {
        LocalDate dob = LocalDate.of(1985, 3, 20);
        LocalDate review = LocalDate.of(2024, 12, 1);

        ContainerData cd = new ContainerData(
                "EYEC", "987654", "0000000001",
                "John Smith", "456 Oak Avenue",
                dob, 750, review, "Y", " ");

        assertEquals("EYEC", cd.getEyecatcher());
        assertEquals("987654", cd.getSortCode());
        assertEquals("0000000001", cd.getAccountNumber());
        assertEquals("John Smith", cd.getCustomerName());
        assertEquals("456 Oak Avenue", cd.getCustomerAddress());
        assertEquals(dob, cd.getDateOfBirth());
        assertEquals(750, cd.getCreditScore());
        assertEquals(review, cd.getCsReviewDate());
        assertEquals("Y", cd.getSuccessFlag());
        assertEquals(" ", cd.getFailCode());
    }

    @Test
    @DisplayName("No-arg constructor creates object with null/zero defaults")
    void noArgConstructor() {
        ContainerData cd = new ContainerData();
        assertNull(cd.getEyecatcher());
        assertNull(cd.getSortCode());
        assertNull(cd.getAccountNumber());
        assertNull(cd.getCustomerName());
        assertNull(cd.getCustomerAddress());
        assertNull(cd.getDateOfBirth());
        assertEquals(0, cd.getCreditScore());
        assertNull(cd.getCsReviewDate());
        assertNull(cd.getSuccessFlag());
        assertNull(cd.getFailCode());
    }

    @Test
    @DisplayName("Setters update fields correctly")
    void setters() {
        ContainerData cd = new ContainerData();
        cd.setEyecatcher("TEST");
        cd.setSortCode("123456");
        cd.setAccountNumber("9999999999");
        cd.setCustomerName("Alice");
        cd.setCustomerAddress("789 Pine Road");
        cd.setDateOfBirth(LocalDate.of(2000, 1, 1));
        cd.setCreditScore(500);
        cd.setCsReviewDate(LocalDate.of(2025, 6, 15));
        cd.setSuccessFlag("N");
        cd.setFailCode("X");

        assertEquals("TEST", cd.getEyecatcher());
        assertEquals("123456", cd.getSortCode());
        assertEquals("9999999999", cd.getAccountNumber());
        assertEquals("Alice", cd.getCustomerName());
        assertEquals("789 Pine Road", cd.getCustomerAddress());
        assertEquals(LocalDate.of(2000, 1, 1), cd.getDateOfBirth());
        assertEquals(500, cd.getCreditScore());
        assertEquals(LocalDate.of(2025, 6, 15), cd.getCsReviewDate());
        assertEquals("N", cd.getSuccessFlag());
        assertEquals("X", cd.getFailCode());
    }

    @Test
    @DisplayName("equals and hashCode contract")
    void equalsAndHashCode() {
        LocalDate dob = LocalDate.of(1990, 7, 4);
        LocalDate review = LocalDate.of(2024, 2, 28);

        ContainerData a = new ContainerData(
                "EYEC", "987654", "0000000001",
                "Jane", "Addr", dob, 800, review, "Y", " ");
        ContainerData b = new ContainerData(
                "EYEC", "987654", "0000000001",
                "Jane", "Addr", dob, 800, review, "Y", " ");
        ContainerData c = new ContainerData(
                "DIFF", "987654", "0000000001",
                "Jane", "Addr", dob, 800, review, "Y", " ");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }

    @Test
    @DisplayName("equals returns false for null and different types")
    void equalsEdgeCases() {
        ContainerData cd = new ContainerData(
                "EYEC", "987654", "0000000001",
                "Jane", "Addr", LocalDate.now(), 100, LocalDate.now(),
                "Y", " ");
        assertNotEquals(null, cd);
        assertNotEquals("string", cd);
    }

    @Test
    @DisplayName("toString includes key fields")
    void toStringContainsFields() {
        ContainerData cd = new ContainerData(
                "EYEC", "987654", "0000000001",
                "Jane", "Addr", LocalDate.now(), 500, LocalDate.now(),
                "Y", " ");
        String str = cd.toString();
        assertTrue(str.contains("EYEC"));
        assertTrue(str.contains("987654"));
        assertTrue(str.contains("0000000001"));
        assertTrue(str.contains("500"));
    }
}
