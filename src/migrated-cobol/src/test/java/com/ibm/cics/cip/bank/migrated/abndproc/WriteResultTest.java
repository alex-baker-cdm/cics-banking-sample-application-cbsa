/*
 * Copyright IBM Corp. 2023
 */
package com.ibm.cics.cip.bank.migrated.abndproc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WriteResultTest {

    @Test
    void successIsInstanceOfWriteResult() {
        WriteResult result = new WriteResult.Success();
        assertInstanceOf(WriteResult.class, result);
        assertInstanceOf(WriteResult.Success.class, result);
    }

    @Test
    void failureIsInstanceOfWriteResult() {
        WriteResult result = new WriteResult.Failure(16, 104);
        assertInstanceOf(WriteResult.class, result);
        assertInstanceOf(WriteResult.Failure.class, result);
    }

    @Test
    void failurePreservesRespCodes() {
        WriteResult.Failure failure = new WriteResult.Failure(22, 44);
        assertEquals(22, failure.respCode());
        assertEquals(44, failure.resp2Code());
    }

    @Test
    void canPatternMatchOnWriteResult() {
        WriteResult success = new WriteResult.Success();
        WriteResult failure = new WriteResult.Failure(16, 104);

        String successMessage = switch (success) {
            case WriteResult.Success s -> "ok";
            case WriteResult.Failure f -> "fail:" + f.respCode();
        };
        assertEquals("ok", successMessage);

        String failMessage = switch (failure) {
            case WriteResult.Success s -> "ok";
            case WriteResult.Failure f -> "fail:" + f.respCode();
        };
        assertEquals("fail:16", failMessage);
    }

    @Test
    void successRecordsAreEqual() {
        assertEquals(new WriteResult.Success(), new WriteResult.Success());
    }

    @Test
    void failureRecordsWithSameCodesAreEqual() {
        assertEquals(new WriteResult.Failure(16, 104), new WriteResult.Failure(16, 104));
    }

    @Test
    void failureRecordsWithDifferentCodesAreNotEqual() {
        assertNotEquals(new WriteResult.Failure(16, 104), new WriteResult.Failure(22, 0));
    }
}
