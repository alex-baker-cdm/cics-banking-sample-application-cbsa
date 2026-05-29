/*
 * Copyright IBM Corp. 2023
 */
package com.ibm.cics.cip.bank.migrated.abndproc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AbndProcessorTest {

    private InMemoryAbndFileStore store;
    private AbndProcessor processor;

    @BeforeEach
    void setUp() {
        store = new InMemoryAbndFileStore();
        processor = new AbndProcessor(store);
    }

    private AbndRecord createTestRecord(long utime, String taskno) {
        return new AbndRecord(
                new AbndVsamKey(utime, taskno),
                "CICSA001",
                "ABND",
                "29.05.2026",
                "14:30:00",
                "ASRA",
                "XFRFUN",
                16,
                104,
                -805,
                "Test abend in XFRFUN performing transfer operation");
    }

    @Test
    void processAbendWritesRecordSuccessfully() {
        AbndRecord record = createTestRecord(1000L, "0001");
        boolean result = processor.processAbend(record);
        assertTrue(result);
        assertEquals(1, store.size());
        assertEquals(record, store.get(record.vsamKey()));
    }

    @Test
    void processAbendReturnsFalseOnDuplicateKey() {
        AbndRecord record1 = createTestRecord(1000L, "0001");
        AbndRecord record2 = new AbndRecord(
                new AbndVsamKey(1000L, "0001"),
                "CICSB002",
                "UPDT",
                "30.05.2026",
                "15:00:00",
                "AICA",
                "UPDCUST",
                0,
                0,
                0,
                "Another abend");

        assertTrue(processor.processAbend(record1));
        assertFalse(processor.processAbend(record2));
        assertEquals(1, store.size());
        assertEquals(record1, store.get(record1.vsamKey()));
    }

    @Test
    void processAbendWritesMultipleDistinctRecords() {
        AbndRecord r1 = createTestRecord(1000L, "0001");
        AbndRecord r2 = createTestRecord(1001L, "0002");
        AbndRecord r3 = createTestRecord(1002L, "0003");

        assertTrue(processor.processAbend(r1));
        assertTrue(processor.processAbend(r2));
        assertTrue(processor.processAbend(r3));
        assertEquals(3, store.size());
    }

    @Test
    void processAbendRejectsNullRecord() {
        assertThrows(NullPointerException.class, () -> processor.processAbend(null));
    }

    @Test
    void constructorRejectsNullStore() {
        assertThrows(NullPointerException.class, () -> new AbndProcessor(null));
    }

    @Test
    void processAbendReturnsFalseWhenStoreReturnsFailure() {
        AbndFileStore failingStore = record -> new WriteResult.Failure(22, 0);
        AbndProcessor failProcessor = new AbndProcessor(failingStore);

        AbndRecord record = createTestRecord(1000L, "0001");
        assertFalse(failProcessor.processAbend(record));
    }

    @Test
    void processAbendHandlesZeroRespCodes() {
        AbndRecord record = new AbndRecord(
                new AbndVsamKey(500L, "0010"),
                "CICSA001",
                "MENU",
                "01.01.2025",
                "00:00:00",
                "ASRA",
                "BNKMENU",
                0,
                0,
                0,
                "");
        assertTrue(processor.processAbend(record));
        assertEquals(record, store.get(record.vsamKey()));
    }

    @Test
    void processAbendPreservesNegativeSqlCode() {
        AbndRecord record = new AbndRecord(
                new AbndVsamKey(999L, "0099"),
                "CICSA001",
                "DBCR",
                "15.06.2026",
                "10:15:30",
                "AEIS",
                "DBCRFUN",
                16,
                104,
                -805,
                "DB2 SQL error -805 table not found");
        assertTrue(processor.processAbend(record));
        AbndRecord stored = store.get(record.vsamKey());
        assertNotNull(stored);
        assertEquals(-805, stored.sqlCode());
    }

    @Test
    void processAbendPreservesMaxLengthFreeform() {
        String maxFreeform = "A".repeat(600);
        AbndRecord record = new AbndRecord(
                new AbndVsamKey(2000L, "0050"),
                "CICSA001",
                "CREA",
                "20.03.2026",
                "12:00:00",
                "ABND",
                "CREACC",
                0,
                0,
                0,
                maxFreeform);
        assertTrue(processor.processAbend(record));
        assertEquals(600, store.get(record.vsamKey()).freeform().length());
    }

    @Test
    void processAbendWithLargeUtimeKey() {
        AbndRecord record = createTestRecord(999999999999999L, "9999");
        assertTrue(processor.processAbend(record));
        assertEquals(record, store.get(record.vsamKey()));
    }

    @Test
    void processAbendWithNegativeUtimeKey() {
        AbndRecord record = createTestRecord(-1L, "0001");
        assertTrue(processor.processAbend(record));
        assertEquals(record, store.get(record.vsamKey()));
    }

    @Test
    void processAbendPreservesAllCommAreaFields() {
        AbndRecord record = new AbndRecord(
                new AbndVsamKey(123456L, "4567"),
                "CICSAPP1",
                "TFN1",
                "15.06.2026",
                "09:45:22",
                "ASRB",
                "BNK1TFN",
                22,
                44,
                -811,
                "RESP=22 RESP2=44 performing XFRFUN transfer debit");
        assertTrue(processor.processAbend(record));

        AbndRecord stored = store.get(record.vsamKey());
        assertNotNull(stored);
        assertEquals("CICSAPP1", stored.applId());
        assertEquals("TFN1", stored.tranId());
        assertEquals("15.06.2026", stored.date());
        assertEquals("09:45:22", stored.time());
        assertEquals("ASRB", stored.code());
        assertEquals("BNK1TFN", stored.program());
        assertEquals(22, stored.respCode());
        assertEquals(44, stored.resp2Code());
        assertEquals(-811, stored.sqlCode());
    }
}
