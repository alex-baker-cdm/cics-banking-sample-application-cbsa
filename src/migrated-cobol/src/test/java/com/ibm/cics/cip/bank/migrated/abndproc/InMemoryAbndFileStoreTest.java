/*
 * Copyright IBM Corp. 2023
 */
package com.ibm.cics.cip.bank.migrated.abndproc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryAbndFileStoreTest {

    private InMemoryAbndFileStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryAbndFileStore();
    }

    private AbndRecord createRecord(long utime, String taskno) {
        return new AbndRecord(
                new AbndVsamKey(utime, taskno),
                "CICSA001", "ABND", "29.05.2026", "14:30:00",
                "ASRA", "XFRFUN", 0, 0, 0, "test");
    }

    @Test
    void writeReturnsSuccessForNewKey() {
        AbndRecord record = createRecord(100L, "0001");
        WriteResult result = store.write(record);
        assertInstanceOf(WriteResult.Success.class, result);
    }

    @Test
    void writeReturnsFailureForDuplicateKey() {
        AbndRecord record1 = createRecord(100L, "0001");
        AbndRecord record2 = createRecord(100L, "0001");
        store.write(record1);
        WriteResult result = store.write(record2);
        assertInstanceOf(WriteResult.Failure.class, result);
        WriteResult.Failure failure = (WriteResult.Failure) result;
        assertEquals(16, failure.respCode());
        assertEquals(104, failure.resp2Code());
    }

    @Test
    void getReturnsStoredRecord() {
        AbndRecord record = createRecord(200L, "0002");
        store.write(record);
        AbndRecord retrieved = store.get(new AbndVsamKey(200L, "0002"));
        assertEquals(record, retrieved);
    }

    @Test
    void getReturnsNullForMissingKey() {
        assertNull(store.get(new AbndVsamKey(999L, "0001")));
    }

    @Test
    void sizeReflectsNumberOfRecords() {
        assertEquals(0, store.size());
        store.write(createRecord(1L, "0001"));
        assertEquals(1, store.size());
        store.write(createRecord(2L, "0002"));
        assertEquals(2, store.size());
    }

    @Test
    void clearRemovesAllRecords() {
        store.write(createRecord(1L, "0001"));
        store.write(createRecord(2L, "0002"));
        assertEquals(2, store.size());
        store.clear();
        assertEquals(0, store.size());
    }

    @Test
    void getAllReturnsUnmodifiableMap() {
        store.write(createRecord(1L, "0001"));
        Map<String, AbndRecord> all = store.getAll();
        assertEquals(1, all.size());
        assertThrows(UnsupportedOperationException.class,
                () -> all.put("key", createRecord(2L, "0002")));
    }

    @Test
    void writeAfterClearSucceeds() {
        AbndRecord record = createRecord(100L, "0001");
        store.write(record);
        store.clear();
        WriteResult result = store.write(record);
        assertInstanceOf(WriteResult.Success.class, result);
        assertEquals(1, store.size());
    }

    @Test
    void differentKeysCanCoexist() {
        store.write(createRecord(100L, "0001"));
        store.write(createRecord(100L, "0002"));
        store.write(createRecord(101L, "0001"));
        assertEquals(3, store.size());
    }
}
