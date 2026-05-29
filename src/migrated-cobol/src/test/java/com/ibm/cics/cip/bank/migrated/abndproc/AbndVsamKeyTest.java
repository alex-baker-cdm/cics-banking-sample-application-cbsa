/*
 * Copyright IBM Corp. 2023
 */
package com.ibm.cics.cip.bank.migrated.abndproc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AbndVsamKeyTest {

    @Test
    void constructsWithValidValues() {
        AbndVsamKey key = new AbndVsamKey(123456789012345L, "0001");
        assertEquals(123456789012345L, key.utimeKey());
        assertEquals("0001", key.tasknoKey());
    }

    @Test
    void rejectsNullTasknoKey() {
        assertThrows(NullPointerException.class, () -> new AbndVsamKey(0L, null));
    }

    @Test
    void rejectsTasknoKeyExceedingMaxLength() {
        assertThrows(IllegalArgumentException.class, () -> new AbndVsamKey(0L, "12345"));
    }

    @Test
    void acceptsTasknoKeyAtMaxLength() {
        AbndVsamKey key = new AbndVsamKey(0L, "1234");
        assertEquals("1234", key.tasknoKey());
    }

    @Test
    void acceptsShorterTasknoKey() {
        AbndVsamKey key = new AbndVsamKey(0L, "01");
        assertEquals("01", key.tasknoKey());
    }

    @Test
    void toKeyStringConcatenatesUtimeAndPaddedTaskno() {
        AbndVsamKey key = new AbndVsamKey(100L, "42");
        String keyStr = key.toKeyString();
        assertTrue(keyStr.startsWith("100"));
        assertTrue(keyStr.contains("42"));
    }

    @Test
    void negativeUtimeKeyIsAllowed() {
        AbndVsamKey key = new AbndVsamKey(-1L, "0001");
        assertEquals(-1L, key.utimeKey());
    }

    @Test
    void zeroUtimeKeyIsAllowed() {
        AbndVsamKey key = new AbndVsamKey(0L, "0000");
        assertEquals(0L, key.utimeKey());
    }

    @Test
    void equalKeysAreEqual() {
        AbndVsamKey key1 = new AbndVsamKey(100L, "0001");
        AbndVsamKey key2 = new AbndVsamKey(100L, "0001");
        assertEquals(key1, key2);
        assertEquals(key1.hashCode(), key2.hashCode());
    }

    @Test
    void differentKeysAreNotEqual() {
        AbndVsamKey key1 = new AbndVsamKey(100L, "0001");
        AbndVsamKey key2 = new AbndVsamKey(100L, "0002");
        assertNotEquals(key1, key2);
    }
}
