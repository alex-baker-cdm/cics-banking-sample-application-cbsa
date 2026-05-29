/*
 * Copyright IBM Corp. 2023
 */
package com.ibm.cics.cip.bank.migrated.bnkmenu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuScreenDataTest {

    private MenuScreenData screenData;

    @BeforeEach
    void setUp() {
        screenData = new MenuScreenData();
    }

    @Test
    @DisplayName("Default state has empty fields")
    void shouldHaveEmptyDefaults() {
        assertEquals("", screenData.getCompany());
        assertEquals("", screenData.getAction());
        assertEquals("", screenData.getMessage());
    }

    @Test
    @DisplayName("clearFields resets all fields")
    void shouldClearAllFields() {
        screenData.setCompany("Test Bank");
        screenData.setAction("1");
        screenData.setMessage("Hello");

        screenData.clearFields();

        assertEquals("", screenData.getCompany());
        assertEquals("", screenData.getAction());
        assertEquals("", screenData.getMessage());
    }

    @Test
    @DisplayName("Getters and setters work correctly")
    void shouldSetAndGetValues() {
        screenData.setCompany("CBSA");
        screenData.setAction("A");
        screenData.setMessage("Select option");

        assertEquals("CBSA", screenData.getCompany());
        assertEquals("A", screenData.getAction());
        assertEquals("Select option", screenData.getMessage());
    }

    @Test
    @DisplayName("toString includes field values")
    void shouldIncludeFieldsInToString() {
        screenData.setAction("3");
        screenData.setMessage("test message");

        String result = screenData.toString();

        assertTrue(result.contains("3"));
        assertTrue(result.contains("test message"));
    }
}
