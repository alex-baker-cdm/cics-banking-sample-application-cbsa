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
 * Tests for {@link CreditAgencyException}.
 */
class CreditAgencyExceptionTest {

    private AbendInfo sampleAbendInfo() {
        return new AbendInfo(1L, 1, "APP", "TRN",
                "01.01.2024", "12:00:00", "PLOP", "CRDTAGY4",
                0, 0, 0, "test");
    }

    @Test
    @DisplayName("Exception carries message and AbendInfo")
    void messageAndAbendInfo() {
        AbendInfo info = sampleAbendInfo();
        CreditAgencyException ex =
                new CreditAgencyException("test error", info);

        assertEquals("test error", ex.getMessage());
        assertSame(info, ex.getAbendInfo());
        assertNull(ex.getCause());
    }

    @Test
    @DisplayName("Exception carries message, AbendInfo, and cause")
    void messageAbendInfoAndCause() {
        AbendInfo info = sampleAbendInfo();
        RuntimeException cause = new RuntimeException("root cause");
        CreditAgencyException ex =
                new CreditAgencyException("wrapped", info, cause);

        assertEquals("wrapped", ex.getMessage());
        assertSame(info, ex.getAbendInfo());
        assertSame(cause, ex.getCause());
    }

    @Test
    @DisplayName("Exception is a RuntimeException")
    void isRuntimeException() {
        CreditAgencyException ex = new CreditAgencyException(
                "msg", sampleAbendInfo());
        assertInstanceOf(RuntimeException.class, ex);
    }
}
