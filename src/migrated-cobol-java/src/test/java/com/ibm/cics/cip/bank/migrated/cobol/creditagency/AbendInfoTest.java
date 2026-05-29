/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.migrated.cobol.creditagency;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class AbendInfoTest {

    @Test
    @DisplayName("constants match COBOL program values")
    void constants() {
        assertEquals("PLOP", AbendInfo.DEFAULT_ABEND_CODE);
        assertEquals("CRDTAGY2", AbendInfo.PROGRAM_NAME);
        assertEquals(600, AbendInfo.FREEFORM_MAX_LENGTH);
    }

    @Test
    @DisplayName("record fields are accessible")
    void recordFields() {
        AbendInfo info = new AbendInfo(
                1000L, 1234, "CICSA", "OCR2",
                "15.06.2025", "14:30:00", "PLOP", "CRDTAGY2",
                16, 0, 0, "A010 - test abend");

        assertEquals(1000L, info.utimeKey());
        assertEquals(1234, info.taskNumber());
        assertEquals("CICSA", info.applId());
        assertEquals("OCR2", info.tranId());
        assertEquals("15.06.2025", info.date());
        assertEquals("14:30:00", info.time());
        assertEquals("PLOP", info.abendCode());
        assertEquals("CRDTAGY2", info.program());
        assertEquals(16, info.respCode());
        assertEquals(0, info.resp2Code());
        assertEquals(0, info.sqlCode());
        assertEquals("A010 - test abend", info.freeformText());
    }

    @Test
    @DisplayName("two records with same values are equal")
    void equality() {
        AbendInfo a = new AbendInfo(
                1L, 1, "A", "B", "D", "T", "C", "P", 0, 0, 0, "F");
        AbendInfo b = new AbendInfo(
                1L, 1, "A", "B", "D", "T", "C", "P", 0, 0, 0, "F");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
