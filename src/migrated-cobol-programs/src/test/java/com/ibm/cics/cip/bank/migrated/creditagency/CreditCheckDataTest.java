/*
 *
 *    Copyright IBM Corp. 2023
 *
 *
 */

package com.ibm.cics.cip.bank.migrated.creditagency;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CreditCheckDataTest {

    @Test
    void defaultConstructorInitializesEmptyFields() {
        CreditCheckData data = new CreditCheckData();
        assertEquals("", data.getEyecatcher());
        assertEquals("", data.getSortCode());
        assertEquals("", data.getNumber());
        assertEquals("", data.getName());
        assertEquals("", data.getAddress());
        assertEquals("", data.getDateOfBirth());
        assertEquals(0, data.getCreditScore());
        assertEquals("", data.getCsReviewDate());
        assertEquals("", data.getSuccess());
        assertEquals("", data.getFailCode());
    }

    @Test
    void setDateOfBirthParsesComponentFields() {
        CreditCheckData data = new CreditCheckData();
        data.setDateOfBirth("15051990");
        assertEquals(15, data.getBirthDay());
        assertEquals(5, data.getBirthMonth());
        assertEquals(1990, data.getBirthYear());
    }

    @Test
    void setDateOfBirthWithLeadingZeros() {
        CreditCheckData data = new CreditCheckData();
        data.setDateOfBirth("01012000");
        assertEquals(1, data.getBirthDay());
        assertEquals(1, data.getBirthMonth());
        assertEquals(2000, data.getBirthYear());
    }

    @Test
    void setDateOfBirthWithInvalidLengthDoesNotParse() {
        CreditCheckData data = new CreditCheckData();
        data.setDateOfBirth("123");
        assertEquals(0, data.getBirthDay());
        assertEquals(0, data.getBirthMonth());
        assertEquals(0, data.getBirthYear());
    }

    @Test
    void setDateOfBirthWithNullDoesNotThrow() {
        CreditCheckData data = new CreditCheckData();
        assertDoesNotThrow(() -> data.setDateOfBirth(null));
    }

    @Test
    void gettersAndSettersWorkCorrectly() {
        CreditCheckData data = new CreditCheckData();

        data.setEyecatcher("CUST");
        assertEquals("CUST", data.getEyecatcher());

        data.setSortCode("987654");
        assertEquals("987654", data.getSortCode());

        data.setNumber("0000012345");
        assertEquals("0000012345", data.getNumber());

        data.setName("Test Customer");
        assertEquals("Test Customer", data.getName());

        data.setAddress("456 Oak Avenue");
        assertEquals("456 Oak Avenue", data.getAddress());

        data.setCreditScore(750);
        assertEquals(750, data.getCreditScore());

        data.setCsReviewDate("20230615");
        assertEquals("20230615", data.getCsReviewDate());

        data.setSuccess("Y");
        assertEquals("Y", data.getSuccess());

        data.setFailCode("N");
        assertEquals("N", data.getFailCode());
    }

    @Test
    void toStringContainsAllKeyFields() {
        CreditCheckData data = new CreditCheckData();
        data.setEyecatcher("CUST");
        data.setSortCode("987654");
        data.setNumber("0000012345");
        data.setName("John Doe");
        data.setCreditScore(850);

        String str = data.toString();
        assertTrue(str.contains("CUST"));
        assertTrue(str.contains("987654"));
        assertTrue(str.contains("0000012345"));
        assertTrue(str.contains("John Doe"));
        assertTrue(str.contains("850"));
    }

    @Test
    void totalLengthMatchesCobolRecordLength() {
        assertEquals(4 + 6 + 10 + 60 + 160 + 8 + 3 + 8 + 1 + 1,
                CreditCheckData.TOTAL_LENGTH);
    }

    @Test
    void creditScoreBoundsMatchCobolDefinition() {
        assertEquals(1, CreditCheckData.MIN_CREDIT_SCORE);
        assertEquals(999, CreditCheckData.MAX_CREDIT_SCORE);
    }

    @Test
    void fieldLengthsMatchCobolPicClauses() {
        assertEquals(4, CreditCheckData.EYECATCHER_LEN);
        assertEquals(6, CreditCheckData.SORTCODE_LEN);
        assertEquals(10, CreditCheckData.NUMBER_LEN);
        assertEquals(60, CreditCheckData.NAME_LEN);
        assertEquals(160, CreditCheckData.ADDRESS_LEN);
        assertEquals(8, CreditCheckData.DATE_OF_BIRTH_LEN);
        assertEquals(3, CreditCheckData.CREDIT_SCORE_LEN);
        assertEquals(8, CreditCheckData.CS_REVIEW_DATE_LEN);
        assertEquals(1, CreditCheckData.SUCCESS_LEN);
        assertEquals(1, CreditCheckData.FAIL_CODE_LEN);
    }
}
