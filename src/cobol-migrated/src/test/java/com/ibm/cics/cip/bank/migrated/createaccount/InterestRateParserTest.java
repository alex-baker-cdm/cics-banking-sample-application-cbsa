/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.migrated.createaccount;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InterestRateParserTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("Returns empty for null/blank input")
    void returnsEmptyForBlank(String input) {
        Optional<BigDecimal> result = InterestRateParser.parse(input);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Parses simple integer")
    void parsesSimpleInteger() {
        Optional<BigDecimal> result = InterestRateParser.parse("5");
        assertTrue(result.isPresent());
        assertEquals(0, new BigDecimal("5").compareTo(result.get()));
    }

    @Test
    @DisplayName("Parses decimal with one digit after point")
    void parsesOneDecimal() {
        Optional<BigDecimal> result = InterestRateParser.parse("1.5");
        assertTrue(result.isPresent());
        assertEquals(0, new BigDecimal("1.5").compareTo(result.get()));
    }

    @Test
    @DisplayName("Parses decimal with two digits after point")
    void parsesTwoDecimals() {
        Optional<BigDecimal> result = InterestRateParser.parse("3.25");
        assertTrue(result.isPresent());
        assertEquals(0, new BigDecimal("3.25").compareTo(result.get()));
    }

    @Test
    @DisplayName("Rejects more than two decimal places")
    void rejectsThreeDecimals() {
        Optional<BigDecimal> result = InterestRateParser.parse("1.123");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Rejects multiple decimal points")
    void rejectsMultipleDecimalPoints() {
        Optional<BigDecimal> result = InterestRateParser.parse("1.2.3");
        assertTrue(result.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "1a2", "12$3", "1,23"})
    @DisplayName("Rejects non-numeric characters")
    void rejectsNonNumeric(String input) {
        Optional<BigDecimal> result = InterestRateParser.parse(input);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Parses zero")
    void parsesZero() {
        Optional<BigDecimal> result = InterestRateParser.parse("0");
        assertTrue(result.isPresent());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.get()));
    }

    @Test
    @DisplayName("Parses large value 9999.99")
    void parsesMaxValue() {
        Optional<BigDecimal> result = InterestRateParser.parse("9999.99");
        assertTrue(result.isPresent());
        assertEquals(0, new BigDecimal("9999.99").compareTo(result.get()));
    }

    @Test
    @DisplayName("Handles leading/trailing spaces")
    void handlesSpaces() {
        Optional<BigDecimal> result = InterestRateParser.parse("  3.25  ");
        assertTrue(result.isPresent());
        assertEquals(0, new BigDecimal("3.25").compareTo(result.get()));
    }
}
