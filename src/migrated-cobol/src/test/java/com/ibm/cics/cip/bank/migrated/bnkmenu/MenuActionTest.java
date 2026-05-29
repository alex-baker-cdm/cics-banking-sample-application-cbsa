/*
 * Copyright IBM Corp. 2023
 */
package com.ibm.cics.cip.bank.migrated.bnkmenu;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuActionTest {

    @ParameterizedTest
    @CsvSource({
            "1, ODCS, DISPLAY_CUSTOMER",
            "2, ODAC, DISPLAY_ACCOUNT",
            "3, OCCS, CREATE_CUSTOMER",
            "4, OCAC, CREATE_ACCOUNT",
            "5, OUAC, UPDATE_ACCOUNT",
            "6, OCRA, CREDIT_DEBIT",
            "7, OTFN, TRANSFER_FUNDS",
            "A, OCCA, LOOKUP_ACCOUNTS"
    })
    @DisplayName("Each code maps to the correct transaction ID")
    void shouldMapCodeToTransactionId(String code, String expectedTransId,
            String expectedName) {
        MenuAction action = MenuAction.fromCode(code);

        assertNotNull(action);
        assertEquals(expectedTransId, action.getTransactionId());
        assertEquals(expectedName, action.name());
        assertEquals(code, action.getCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "2", "3", "4", "5", "6", "7", "A"})
    @DisplayName("All valid codes should be recognized")
    void shouldRecognizeValidCodes(String code) {
        assertTrue(MenuAction.isValidCode(code));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "8", "9", "B", "a", " ", "X"})
    @DisplayName("Invalid codes should be rejected")
    void shouldRejectInvalidCodes(String code) {
        assertFalse(MenuAction.isValidCode(code));
    }

    @Test
    @DisplayName("fromCode returns null for unknown code")
    void shouldReturnNullForUnknownCode() {
        assertNull(MenuAction.fromCode("Z"));
    }

    @Test
    @DisplayName("All enum values have non-empty descriptions")
    void shouldHaveDescriptions() {
        for (MenuAction action : MenuAction.values()) {
            assertNotNull(action.getDescription());
            assertFalse(action.getDescription().isEmpty());
        }
    }

    @Test
    @DisplayName("Should have exactly 8 menu options")
    void shouldHaveEightOptions() {
        assertEquals(8, MenuAction.values().length);
    }
}
