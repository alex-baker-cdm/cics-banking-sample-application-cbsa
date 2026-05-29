/*
 * Copyright IBM Corp. 2023
 */
package com.ibm.cics.cip.bank.migrated.bnk1cra;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.ibm.cics.cip.bank.migrated.bnk1cra.AmountValidator.ValidationResult;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AmountValidator, covering all validation paths from the
 * COBOL VALIDATE-AMOUNT section.
 */
class AmountValidatorTest {

    @ParameterizedTest
    @NullAndEmptySource
    void shouldRejectNullOrEmptyInput(String input) {
        ValidationResult result = AmountValidator.validate(input);
        assertFalse(result.valid());
        assertEquals("The Amount entered must be numeric.", result.message());
    }

    @Test
    void shouldRejectAllSpaces() {
        ValidationResult result = AmountValidator.validate("   ");
        assertFalse(result.valid());
        assertEquals("The Amount entered must be numeric.", result.message());
    }

    @Test
    void shouldAcceptPureIntegerAmount() {
        ValidationResult result = AmountValidator.validate("12345");
        assertTrue(result.valid());
        assertEquals(new BigDecimal("12345"), result.amount());
    }

    @Test
    void shouldAcceptDecimalAmountWithTwoPlaces() {
        ValidationResult result = AmountValidator.validate("100.50");
        assertTrue(result.valid());
        assertEquals(new BigDecimal("100.50"), result.amount());
    }

    @Test
    void shouldAcceptDecimalAmountWithOnePlace() {
        ValidationResult result = AmountValidator.validate("99.5");
        assertTrue(result.valid());
        assertEquals(new BigDecimal("99.5"), result.amount());
    }

    @Test
    void shouldAcceptAmountWithLeadingSpaces() {
        ValidationResult result = AmountValidator.validate("   500");
        assertTrue(result.valid());
        assertEquals(new BigDecimal("500"), result.amount());
    }

    @Test
    void shouldAcceptAmountWithTrailingSpaces() {
        ValidationResult result = AmountValidator.validate("500   ");
        assertTrue(result.valid());
        assertEquals(new BigDecimal("500"), result.amount());
    }

    @Test
    void shouldRejectAmountWithEmbeddedSpaces() {
        ValidationResult result = AmountValidator.validate("50 0");
        assertFalse(result.valid());
        assertEquals("Please supply a numeric amount without embedded spaces.",
                result.message());
    }

    @Test
    void shouldRejectNonNumericCharacters() {
        ValidationResult result = AmountValidator.validate("12A34");
        assertFalse(result.valid());
        assertEquals("Please supply a numeric amount.", result.message());
    }

    @Test
    void shouldRejectMultipleDecimalPoints() {
        ValidationResult result = AmountValidator.validate("12.34.56");
        assertFalse(result.valid());
        assertEquals("Use one decimal point for amount only.", result.message());
    }

    @Test
    void shouldRejectMoreThanTwoDecimalPlaces() {
        ValidationResult result = AmountValidator.validate("100.123");
        assertFalse(result.valid());
        assertEquals("Only up to two decimal places are supported.", result.message());
    }

    @Test
    void shouldAcceptExactlyTwoDecimalPlaces() {
        ValidationResult result = AmountValidator.validate("100.12");
        assertTrue(result.valid());
        assertEquals(new BigDecimal("100.12"), result.amount());
    }

    @Test
    void shouldRejectZeroAmount() {
        ValidationResult result = AmountValidator.validate("0");
        assertFalse(result.valid());
        assertEquals("Please supply a non-zero amount.", result.message());
    }

    @Test
    void shouldRejectZeroDecimalAmount() {
        ValidationResult result = AmountValidator.validate("0.00");
        assertFalse(result.valid());
        assertEquals("Please supply a non-zero amount.", result.message());
    }

    @Test
    void shouldAcceptLargeAmount() {
        ValidationResult result = AmountValidator.validate("9999999999.99");
        assertTrue(result.valid());
        assertEquals(new BigDecimal("9999999999.99"), result.amount());
    }

    @Test
    void shouldAcceptSmallDecimalAmount() {
        ValidationResult result = AmountValidator.validate("0.01");
        assertTrue(result.valid());
        assertEquals(new BigDecimal("0.01"), result.amount());
    }

    @ParameterizedTest
    @ValueSource(strings = {"$100", "#50", "100,000"})
    void shouldRejectSpecialCharacters(String input) {
        ValidationResult result = AmountValidator.validate(input);
        assertFalse(result.valid());
    }

    @Test
    void shouldRejectThreeDecimalPlaces() {
        ValidationResult result = AmountValidator.validate("10.999");
        assertFalse(result.valid());
        assertEquals("Only up to two decimal places are supported.", result.message());
    }

    @Test
    void shouldAcceptAmountStartingWithDecimal() {
        ValidationResult result = AmountValidator.validate(".50");
        assertTrue(result.valid());
        assertEquals(new BigDecimal("0.50"), result.amount());
    }
}
