/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.migrated.createaccount;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidationResultTest {

    @Test
    @DisplayName("ok() creates a valid result")
    void okCreatesValid() {
        ValidationResult result = ValidationResult.ok();
        assertTrue(result.valid());
        assertNull(result.errorMessage());
        assertNull(result.invalidField());
    }

    @Test
    @DisplayName("error() creates an invalid result with message and field")
    void errorCreatesInvalid() {
        ValidationResult result = ValidationResult.error("bad input", "fieldX");
        assertFalse(result.valid());
        assertEquals("bad input", result.errorMessage());
        assertEquals("fieldX", result.invalidField());
    }
}
