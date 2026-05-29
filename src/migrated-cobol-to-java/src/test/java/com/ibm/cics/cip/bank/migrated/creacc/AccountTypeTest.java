/*
 * Copyright IBM Corp. 2023
 */

package com.ibm.cics.cip.bank.migrated.creacc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class AccountTypeTest {

    @ParameterizedTest
    @CsvSource({
            "ISA, ISA",
            "MORTGAGE, MORTGAGE",
            "SAVING, SAVING",
            "CURRENT, CURRENT",
            "LOAN, LOAN"
    })
    @DisplayName("Matches valid account types exactly")
    void matchesExactTypes(String input, String expectedCode) {
        AccountType result = AccountType.fromString(input);
        assertNotNull(result);
        assertEquals(expectedCode, result.getCode());
    }

    @Test
    @DisplayName("ISA matches when input has trailing characters")
    void isaMatchesWithTrailing() {
        AccountType result = AccountType.fromString("ISA     ");
        assertNotNull(result);
        assertEquals(AccountType.ISA, result);
    }

    @Test
    @DisplayName("MORTGAGE matches with exact 8 characters")
    void mortgageMatchesExact() {
        AccountType result = AccountType.fromString("MORTGAGE");
        assertNotNull(result);
        assertEquals(AccountType.MORTGAGE, result);
    }

    @Test
    @DisplayName("SAVING matches with trailing spaces")
    void savingMatchesWithTrailing() {
        AccountType result = AccountType.fromString("SAVING  ");
        assertNotNull(result);
        assertEquals(AccountType.SAVING, result);
    }

    @Test
    @DisplayName("CURRENT matches with trailing space")
    void currentMatchesWithTrailing() {
        AccountType result = AccountType.fromString("CURRENT ");
        assertNotNull(result);
        assertEquals(AccountType.CURRENT, result);
    }

    @Test
    @DisplayName("LOAN matches with trailing spaces")
    void loanMatchesWithTrailing() {
        AccountType result = AccountType.fromString("LOAN    ");
        assertNotNull(result);
        assertEquals(AccountType.LOAN, result);
    }

    @ParameterizedTest
    @ValueSource(strings = {"INVALID", "CHECKING", "SAVI", "MOR", "",
            "IS", "LOA", "CURREN", "SAVIN", "MORTGAG"})
    @DisplayName("Returns null for invalid account types")
    void returnsNullForInvalid(String input) {
        assertNull(AccountType.fromString(input));
    }

    @ParameterizedTest
    @NullSource
    @DisplayName("Returns null for null input")
    void returnsNullForNull(String input) {
        assertNull(AccountType.fromString(input));
    }

    @Test
    @DisplayName("Enum has exactly 5 values")
    void hasFiveValues() {
        assertEquals(5, AccountType.values().length);
    }
}
