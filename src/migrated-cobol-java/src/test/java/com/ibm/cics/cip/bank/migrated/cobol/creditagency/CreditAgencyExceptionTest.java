/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.migrated.cobol.creditagency;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class CreditAgencyExceptionTest {

    @Test
    @DisplayName("message-only constructor")
    void messageOnly() {
        CreditAgencyException ex = new CreditAgencyException("test error");

        assertEquals("test error", ex.getMessage());
        assertNull(ex.getAbendInfo());
        assertNull(ex.getCause());
    }

    @Test
    @DisplayName("message and cause constructor")
    void messageAndCause() {
        Throwable cause = new RuntimeException("root cause");
        CreditAgencyException ex = new CreditAgencyException(
                "test error", cause);

        assertEquals("test error", ex.getMessage());
        assertNull(ex.getAbendInfo());
        assertSame(cause, ex.getCause());
    }

    @Test
    @DisplayName("message and abend info constructor")
    void messageAndAbendInfo() {
        AbendInfo info = new AbendInfo(
                1L, 1, "", "", "", "", "PLOP", "CRDTAGY2",
                0, 0, 0, "");
        CreditAgencyException ex = new CreditAgencyException(
                "abend occurred", info);

        assertEquals("abend occurred", ex.getMessage());
        assertNotNull(ex.getAbendInfo());
        assertEquals("PLOP", ex.getAbendInfo().abendCode());
        assertNull(ex.getCause());
    }

    @Test
    @DisplayName("full constructor with message, abend info, and cause")
    void fullConstructor() {
        AbendInfo info = new AbendInfo(
                1L, 1, "", "", "", "", "PLOP", "CRDTAGY2",
                16, 5, 0, "delay error");
        Throwable cause = new InterruptedException("interrupted");
        CreditAgencyException ex = new CreditAgencyException(
                "abend", info, cause);

        assertEquals("abend", ex.getMessage());
        assertNotNull(ex.getAbendInfo());
        assertEquals(16, ex.getAbendInfo().respCode());
        assertSame(cause, ex.getCause());
    }

    @Test
    @DisplayName("is a RuntimeException")
    void isRuntimeException() {
        CreditAgencyException ex = new CreditAgencyException("test");
        assertInstanceOf(RuntimeException.class, ex);
    }
}
