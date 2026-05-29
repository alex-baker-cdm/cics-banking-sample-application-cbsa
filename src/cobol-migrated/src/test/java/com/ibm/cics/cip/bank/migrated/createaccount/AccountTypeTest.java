/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.migrated.createaccount;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AccountTypeTest {

    @ParameterizedTest
    @CsvSource({
            "ISA, ISA",
            "isa, ISA",
            "CURRENT, CURRENT",
            "current, CURRENT",
            "LOAN, LOAN",
            "loan, LOAN",
            "SAVING, SAVING",
            "saving, SAVING",
            "MORTGAGE, MORTGAGE",
            "mortgage, MORTGAGE"
    })
    @DisplayName("Resolves valid account types (case insensitive)")
    void resolvesValidTypes(String input, String expected) {
        Optional<AccountType> result = AccountType.fromString(input);
        assertTrue(result.isPresent());
        assertEquals(expected, result.get().getValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ISA_____", "isa_____", "CURRENT_", "current_",
            "LOAN____", "loan____", "SAVING__", "saving__"})
    @DisplayName("Handles BMS underscore padding")
    void handlesBmsPadding(String input) {
        Optional<AccountType> result = AccountType.fromString(input);
        assertTrue(result.isPresent());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ISA     ", "CURRENT ", "LOAN    ", "SAVING  "})
    @DisplayName("Handles trailing space padding")
    void handlesTrailingSpaces(String input) {
        Optional<AccountType> result = AccountType.fromString(input);
        assertTrue(result.isPresent());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"CHECKING", "DEBIT", "INVALID", "   "})
    @DisplayName("Returns empty for invalid account types")
    void returnsEmptyForInvalid(String input) {
        Optional<AccountType> result = AccountType.fromString(input);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("All enum values have correct string representation")
    void allEnumValues() {
        assertEquals("ISA", AccountType.ISA.getValue());
        assertEquals("CURRENT", AccountType.CURRENT.getValue());
        assertEquals("LOAN", AccountType.LOAN.getValue());
        assertEquals("SAVING", AccountType.SAVING.getValue());
        assertEquals("MORTGAGE", AccountType.MORTGAGE.getValue());
    }
}
