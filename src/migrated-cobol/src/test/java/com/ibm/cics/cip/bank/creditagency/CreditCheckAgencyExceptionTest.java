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
 * Tests for {@link CreditCheckAgencyException}.
 */
class CreditCheckAgencyExceptionTest {

    @Test
    @DisplayName("should store message and abend code")
    void shouldStoreMessageAndAbendCode() {
        CreditCheckAgencyException ex = new CreditCheckAgencyException(
                "Something went wrong", "PLOP");

        assertEquals("Something went wrong", ex.getMessage());
        assertEquals("PLOP", ex.getAbendCode());
        assertNull(ex.getCause());
    }

    @Test
    @DisplayName("should store message, abend code, and cause")
    void shouldStoreMessageAbendCodeAndCause() {
        InterruptedException cause = new InterruptedException("interrupted");
        CreditCheckAgencyException ex = new CreditCheckAgencyException(
                "Delay failed", "PLOP", cause);

        assertEquals("Delay failed", ex.getMessage());
        assertEquals("PLOP", ex.getAbendCode());
        assertSame(cause, ex.getCause());
    }

    @Test
    @DisplayName("should be a RuntimeException")
    void shouldBeRuntimeException() {
        CreditCheckAgencyException ex = new CreditCheckAgencyException(
                "test", "CODE");

        assertInstanceOf(RuntimeException.class, ex);
    }
}
