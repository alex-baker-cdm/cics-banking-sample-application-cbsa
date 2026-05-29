/*
 * Copyright IBM Corp. 2023
 */
package com.ibm.cics.cip.bank.migrated.bnkmenu;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbndInfoTest {

    private AbndInfo abndInfo;

    @BeforeEach
    void setUp() {
        abndInfo = new AbndInfo();
    }

    @Test
    @DisplayName("Initialize should reset all fields to defaults")
    void shouldInitializeAllFields() {
        abndInfo.setUtimeKey(999);
        abndInfo.setTaskNoKey(42);
        abndInfo.setApplId("APPID");
        abndInfo.setTranId("TRXN");
        abndInfo.setDate("29.05.2026");
        abndInfo.setTime("14:30:00");
        abndInfo.setCode("HBNK");
        abndInfo.setProgram("BNKMENU");
        abndInfo.setRespCode(16);
        abndInfo.setResp2Code(0);
        abndInfo.setSqlCode(-805);
        abndInfo.setFreeform("some error");

        abndInfo.initialize();

        assertEquals(0, abndInfo.getUtimeKey());
        assertEquals(0, abndInfo.getTaskNoKey());
        assertEquals("", abndInfo.getApplId());
        assertEquals("", abndInfo.getTranId());
        assertEquals("", abndInfo.getDate());
        assertEquals("", abndInfo.getTime());
        assertEquals("", abndInfo.getCode());
        assertEquals("", abndInfo.getProgram());
        assertEquals(0, abndInfo.getRespCode());
        assertEquals(0, abndInfo.getResp2Code());
        assertEquals(0, abndInfo.getSqlCode());
        assertEquals("", abndInfo.getFreeform());
    }

    @Test
    @DisplayName("populateTimeDate formats date as dd.MM.yyyy")
    void shouldFormatDate() {
        LocalDateTime dateTime = LocalDateTime.of(2026, 5, 29, 14, 30, 45);

        abndInfo.populateTimeDate(dateTime);

        assertEquals("29.05.2026", abndInfo.getDate());
    }

    @Test
    @DisplayName("populateTimeDate formats time as HH:mm:ss")
    void shouldFormatTime() {
        LocalDateTime dateTime = LocalDateTime.of(2026, 5, 29, 14, 30, 45);

        abndInfo.populateTimeDate(dateTime);

        assertEquals("14:30:45", abndInfo.getTime());
    }

    @Test
    @DisplayName("toString contains all fields")
    void shouldIncludeAllFieldsInToString() {
        abndInfo.setApplId("TESTAPP");
        abndInfo.setCode("HBNK");

        String result = abndInfo.toString();

        assertTrue(result.contains("TESTAPP"));
        assertTrue(result.contains("HBNK"));
    }
}
