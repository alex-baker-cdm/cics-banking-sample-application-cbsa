/*
 *
 *    Copyright IBM Corp. 2023
 *
 *
 */

package com.ibm.cics.cip.bank.migrated.creditagency;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AbendInfoTest {

    @Test
    void defaultConstructorInitializesDefaults() {
        AbendInfo info = new AbendInfo();
        assertEquals(0, info.getUtimeKey());
        assertEquals(0, info.getTaskNoKey());
        assertEquals("", info.getApplId());
        assertEquals("", info.getTranId());
        assertEquals("", info.getDate());
        assertEquals("", info.getTime());
        assertEquals("", info.getCode());
        assertEquals("", info.getProgram());
        assertEquals(0, info.getRespCode());
        assertEquals(0, info.getResp2Code());
        assertEquals(0, info.getSqlCode());
        assertEquals("", info.getFreeform());
    }

    @Test
    void gettersAndSettersWorkCorrectly() {
        AbendInfo info = new AbendInfo();

        info.setUtimeKey(123456789L);
        assertEquals(123456789L, info.getUtimeKey());

        info.setTaskNoKey(1234);
        assertEquals(1234, info.getTaskNoKey());

        info.setApplId("CICSPROD");
        assertEquals("CICSPROD", info.getApplId());

        info.setTranId("OCR1");
        assertEquals("OCR1", info.getTranId());

        info.setDate("29.05.2023");
        assertEquals("29.05.2023", info.getDate());

        info.setTime("14:30:00");
        assertEquals("14:30:00", info.getTime());

        info.setCode("PLOP");
        assertEquals("PLOP", info.getCode());

        info.setProgram("CRDTAGY1");
        assertEquals("CRDTAGY1", info.getProgram());

        info.setRespCode(16);
        assertEquals(16, info.getRespCode());

        info.setResp2Code(42);
        assertEquals(42, info.getResp2Code());

        info.setSqlCode(-805);
        assertEquals(-805, info.getSqlCode());

        info.setFreeform("Test freeform text");
        assertEquals("Test freeform text", info.getFreeform());
    }

    @Test
    void toStringContainsAllFields() {
        AbendInfo info = new AbendInfo();
        info.setUtimeKey(100L);
        info.setTaskNoKey(42);
        info.setApplId("APP1");
        info.setTranId("TRN1");
        info.setDate("01.01.2023");
        info.setTime("12:00:00");
        info.setCode("ABCD");
        info.setProgram("TESTPROG");
        info.setRespCode(16);
        info.setResp2Code(5);
        info.setSqlCode(-100);
        info.setFreeform("Error details");

        String str = info.toString();
        assertTrue(str.contains("100"));
        assertTrue(str.contains("42"));
        assertTrue(str.contains("APP1"));
        assertTrue(str.contains("TRN1"));
        assertTrue(str.contains("01.01.2023"));
        assertTrue(str.contains("12:00:00"));
        assertTrue(str.contains("ABCD"));
        assertTrue(str.contains("TESTPROG"));
        assertTrue(str.contains("16"));
        assertTrue(str.contains("-100"));
        assertTrue(str.contains("Error details"));
    }
}
