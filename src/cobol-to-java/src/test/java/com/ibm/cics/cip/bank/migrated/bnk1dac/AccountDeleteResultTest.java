/*
 * Copyright IBM Corp. 2023
 */
package com.ibm.cics.cip.bank.migrated.bnk1dac;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AccountDeleteResultTest {

    @Test
    @DisplayName("success factory creates successful result")
    void successFactory() {
        AccountDeleteResult result = AccountDeleteResult.success("987654");
        assertTrue(result.successful());
        assertEquals(AccountDeleteResult.NO_FAIL, result.failCode());
        assertEquals("987654", result.sortCode());
    }

    @Test
    @DisplayName("failure factory creates failed result with code")
    void failureFactory() {
        AccountDeleteResult result =
                AccountDeleteResult.failure('1', "123456");
        assertFalse(result.successful());
        assertEquals('1', result.failCode());
        assertEquals("123456", result.sortCode());
    }

    @Test
    @DisplayName("Fail code constants match COBOL values")
    void failCodeConstants() {
        assertEquals('1', AccountDeleteResult.FAIL_NOT_FOUND);
        assertEquals('2', AccountDeleteResult.FAIL_DATASTORE_ERROR);
        assertEquals('3', AccountDeleteResult.FAIL_DELETE_ERROR);
        assertEquals(' ', AccountDeleteResult.NO_FAIL);
    }

    @Test
    @DisplayName("Record equals and hashCode")
    void equalsHashCode() {
        AccountDeleteResult a = AccountDeleteResult.success("111111");
        AccountDeleteResult b = AccountDeleteResult.success("111111");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

        AccountDeleteResult c = AccountDeleteResult.failure('2', "111111");
        assertNotEquals(a, c);
    }
}
