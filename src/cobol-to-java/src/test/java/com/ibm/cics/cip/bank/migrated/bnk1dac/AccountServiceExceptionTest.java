/*
 * Copyright IBM Corp. 2023
 */
package com.ibm.cics.cip.bank.migrated.bnk1dac;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AccountServiceExceptionTest {

    @Test
    @DisplayName("Message-only constructor")
    void messageOnly() {
        AccountServiceException ex =
                new AccountServiceException("test error");
        assertEquals("test error", ex.getMessage());
        assertEquals(0, ex.getResponseCode());
        assertEquals(0, ex.getResponseCode2());
    }

    @Test
    @DisplayName("Constructor with response codes")
    void withResponseCodes() {
        AccountServiceException ex =
                new AccountServiceException("fail", 16, 2);
        assertEquals("fail", ex.getMessage());
        assertEquals(16, ex.getResponseCode());
        assertEquals(2, ex.getResponseCode2());
    }

    @Test
    @DisplayName("Constructor with cause")
    void withCause() {
        RuntimeException cause = new RuntimeException("root");
        AccountServiceException ex =
                new AccountServiceException("wrap", cause);
        assertEquals("wrap", ex.getMessage());
        assertSame(cause, ex.getCause());
        assertEquals(0, ex.getResponseCode());
    }
}
