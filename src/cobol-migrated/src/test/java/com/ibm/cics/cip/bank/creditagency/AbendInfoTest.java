/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.creditagency;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the {@link AbendInfo} record.
 */
class AbendInfoTest {

    @Test
    @DisplayName("Record stores all fields correctly")
    void recordFieldAccess() {
        AbendInfo info = new AbendInfo(
                123456789L, 42,
                "APPLID01", "TRN1",
                "29.05.2024", "14:30:00",
                "PLOP", "CRDTAGY4",
                16, 0, 0,
                "A010  - *** The delay messed up! ***");

        assertEquals(123456789L, info.utimeKey());
        assertEquals(42, info.taskNumber());
        assertEquals("APPLID01", info.applId());
        assertEquals("TRN1", info.tranId());
        assertEquals("29.05.2024", info.date());
        assertEquals("14:30:00", info.time());
        assertEquals("PLOP", info.abendCode());
        assertEquals("CRDTAGY4", info.program());
        assertEquals(16, info.respCode());
        assertEquals(0, info.resp2Code());
        assertEquals(0, info.sqlCode());
        assertTrue(info.freeform().contains("delay messed up"));
    }

    @Test
    @DisplayName("Record equals and hashCode are value-based")
    void equalsAndHashCode() {
        AbendInfo a = new AbendInfo(1L, 1, "APP", "TRN",
                "01.01.2024", "12:00:00", "PLOP", "PGM",
                0, 0, 0, "msg");
        AbendInfo b = new AbendInfo(1L, 1, "APP", "TRN",
                "01.01.2024", "12:00:00", "PLOP", "PGM",
                0, 0, 0, "msg");
        AbendInfo c = new AbendInfo(2L, 1, "APP", "TRN",
                "01.01.2024", "12:00:00", "PLOP", "PGM",
                0, 0, 0, "msg");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }

    @Test
    @DisplayName("Record toString includes field values")
    void toStringIncludesFields() {
        AbendInfo info = new AbendInfo(1L, 1, "APP", "TRN",
                "01.01.2024", "12:00:00", "PLOP", "CRDTAGY4",
                0, 0, 0, "test message");
        String str = info.toString();
        assertTrue(str.contains("PLOP"));
        assertTrue(str.contains("CRDTAGY4"));
        assertTrue(str.contains("test message"));
    }
}
