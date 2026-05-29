/*
 * Copyright IBM Corp. 2023
 */
package com.ibm.cics.cip.bank.migrated.abndproc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AbndRecordTest {

    private static final AbndVsamKey TEST_KEY = new AbndVsamKey(1000L, "0001");

    @Test
    void constructsWithValidFields() {
        AbndRecord record = new AbndRecord(
                TEST_KEY, "CICSA001", "ABND", "29.05.2026", "14:30:00",
                "ASRA", "XFRFUN", 0, 0, 0, "Test freeform text");
        assertEquals(TEST_KEY, record.vsamKey());
        assertEquals("CICSA001", record.applId());
        assertEquals("ABND", record.tranId());
        assertEquals("29.05.2026", record.date());
        assertEquals("14:30:00", record.time());
        assertEquals("ASRA", record.code());
        assertEquals("XFRFUN", record.program());
        assertEquals(0, record.respCode());
        assertEquals(0, record.resp2Code());
        assertEquals(0, record.sqlCode());
        assertEquals("Test freeform text", record.freeform());
    }

    @Test
    void truncatesApplIdExceedingMaxLength() {
        AbndRecord record = new AbndRecord(
                TEST_KEY, "LONGAPPLID", "ABND", "29.05.2026", "14:30:00",
                "ASRA", "XFRFUN", 0, 0, 0, "text");
        assertEquals("LONGAPPL", record.applId());
    }

    @Test
    void truncatesTranIdExceedingMaxLength() {
        AbndRecord record = new AbndRecord(
                TEST_KEY, "CICSA", "LONGTR", "29.05.2026", "14:30:00",
                "ASRA", "XFRFUN", 0, 0, 0, "text");
        assertEquals("LONG", record.tranId());
    }

    @Test
    void truncatesDateExceedingMaxLength() {
        AbndRecord record = new AbndRecord(
                TEST_KEY, "CICSA", "ABND", "29.05.2026XX", "14:30:00",
                "ASRA", "XFRFUN", 0, 0, 0, "text");
        assertEquals("29.05.2026", record.date());
    }

    @Test
    void truncatesTimeExceedingMaxLength() {
        AbndRecord record = new AbndRecord(
                TEST_KEY, "CICSA", "ABND", "29.05.2026", "14:30:00.999",
                "ASRA", "XFRFUN", 0, 0, 0, "text");
        assertEquals("14:30:00", record.time());
    }

    @Test
    void truncatesCodeExceedingMaxLength() {
        AbndRecord record = new AbndRecord(
                TEST_KEY, "CICSA", "ABND", "29.05.2026", "14:30:00",
                "ASRAXX", "XFRFUN", 0, 0, 0, "text");
        assertEquals("ASRA", record.code());
    }

    @Test
    void truncatesProgramExceedingMaxLength() {
        AbndRecord record = new AbndRecord(
                TEST_KEY, "CICSA", "ABND", "29.05.2026", "14:30:00",
                "ASRA", "LONGPROGRAMNAME", 0, 0, 0, "text");
        assertEquals("LONGPROG", record.program());
    }

    @Test
    void truncatesFreeformExceedingMaxLength() {
        String longFreeform = "X".repeat(700);
        AbndRecord record = new AbndRecord(
                TEST_KEY, "CICSA", "ABND", "29.05.2026", "14:30:00",
                "ASRA", "XFRFUN", 0, 0, 0, longFreeform);
        assertEquals(600, record.freeform().length());
    }

    @Test
    void preservesShorterFieldValues() {
        AbndRecord record = new AbndRecord(
                TEST_KEY, "CI", "AB", "29.05", "14", "AS", "XF", 0, 0, 0, "hi");
        assertEquals("CI", record.applId());
        assertEquals("AB", record.tranId());
        assertEquals("29.05", record.date());
        assertEquals("14", record.time());
        assertEquals("AS", record.code());
        assertEquals("XF", record.program());
        assertEquals("hi", record.freeform());
    }

    @Test
    void rejectsNullVsamKey() {
        assertThrows(NullPointerException.class, () -> new AbndRecord(
                null, "CICSA", "ABND", "29.05.2026", "14:30:00",
                "ASRA", "XFRFUN", 0, 0, 0, "text"));
    }

    @Test
    void rejectsNullApplId() {
        assertThrows(NullPointerException.class, () -> new AbndRecord(
                TEST_KEY, null, "ABND", "29.05.2026", "14:30:00",
                "ASRA", "XFRFUN", 0, 0, 0, "text"));
    }

    @Test
    void rejectsNullTranId() {
        assertThrows(NullPointerException.class, () -> new AbndRecord(
                TEST_KEY, "CICSA", null, "29.05.2026", "14:30:00",
                "ASRA", "XFRFUN", 0, 0, 0, "text"));
    }

    @Test
    void rejectsNullFreeform() {
        assertThrows(NullPointerException.class, () -> new AbndRecord(
                TEST_KEY, "CICSA", "ABND", "29.05.2026", "14:30:00",
                "ASRA", "XFRFUN", 0, 0, 0, null));
    }

    @Test
    void acceptsNegativeRespCodes() {
        AbndRecord record = new AbndRecord(
                TEST_KEY, "CICSA", "ABND", "29.05.2026", "14:30:00",
                "ASRA", "XFRFUN", -1, -2, -805, "text");
        assertEquals(-1, record.respCode());
        assertEquals(-2, record.resp2Code());
        assertEquals(-805, record.sqlCode());
    }

    @Test
    void acceptsEmptyStringFields() {
        AbndRecord record = new AbndRecord(
                TEST_KEY, "", "", "", "", "", "", 0, 0, 0, "");
        assertEquals("", record.applId());
        assertEquals("", record.tranId());
    }

    @Test
    void equalRecordsAreEqual() {
        AbndRecord r1 = new AbndRecord(
                TEST_KEY, "CICSA", "ABND", "29.05.2026", "14:30:00",
                "ASRA", "XFRFUN", 10, 20, -805, "freeform text");
        AbndRecord r2 = new AbndRecord(
                TEST_KEY, "CICSA", "ABND", "29.05.2026", "14:30:00",
                "ASRA", "XFRFUN", 10, 20, -805, "freeform text");
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }
}
