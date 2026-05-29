/*
 * Copyright IBM Corp. 2023
 */
package com.ibm.cics.cip.bank.migrated.bnk1dac;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AccountInquiryDataTest {

    @Test
    @DisplayName("Default construction initializes to empty/zero values")
    void defaultConstruction() {
        AccountInquiryData data = new AccountInquiryData();
        assertEquals("", data.getEye());
        assertEquals("", data.getCustomerNumber());
        assertEquals("", data.getSortCode());
        assertEquals(0, data.getAccountNumber());
        assertEquals("", data.getAccountType());
        assertEquals(BigDecimal.ZERO, data.getInterestRate());
        assertEquals(0, data.getOpened());
        assertEquals(0, data.getOverdraft());
        assertEquals(0, data.getLastStatementDate());
        assertEquals(0, data.getNextStatementDate());
        assertEquals(BigDecimal.ZERO, data.getAvailableBalance());
        assertEquals(BigDecimal.ZERO, data.getActualBalance());
        assertEquals(' ', data.getSuccess());
    }

    @Test
    @DisplayName("isEyeValid returns true only for ACCT")
    void isEyeValid() {
        AccountInquiryData data = new AccountInquiryData();
        assertFalse(data.isEyeValid());

        data.setEye("ACCT");
        assertTrue(data.isEyeValid());

        data.setEye("XXXX");
        assertFalse(data.isEyeValid());
    }

    @Test
    @DisplayName("isAccountFound returns false when type blank, rate zero, success N")
    void isAccountFoundFalseWhenNotFound() {
        AccountInquiryData data = new AccountInquiryData();
        data.setAccountType("");
        data.setInterestRate(BigDecimal.ZERO);
        data.setSuccess('N');
        assertFalse(data.isAccountFound());
    }

    @Test
    @DisplayName("isAccountFound returns true when type is non-blank")
    void isAccountFoundTrueWithType() {
        AccountInquiryData data = new AccountInquiryData();
        data.setAccountType("SAVINGS");
        data.setInterestRate(BigDecimal.ZERO);
        data.setSuccess('N');
        assertTrue(data.isAccountFound());
    }

    @Test
    @DisplayName("isAccountFound returns true when rate is non-zero")
    void isAccountFoundTrueWithRate() {
        AccountInquiryData data = new AccountInquiryData();
        data.setAccountType("");
        data.setInterestRate(new BigDecimal("1.00"));
        data.setSuccess('N');
        assertTrue(data.isAccountFound());
    }

    @Test
    @DisplayName("isAccountFound returns true when success is not N")
    void isAccountFoundTrueWithSuccess() {
        AccountInquiryData data = new AccountInquiryData();
        data.setAccountType("");
        data.setInterestRate(BigDecimal.ZERO);
        data.setSuccess('Y');
        assertTrue(data.isAccountFound());
    }

    @Test
    @DisplayName("equals and hashCode contract")
    void equalsAndHashCode() {
        AccountInquiryData a = new AccountInquiryData();
        a.setEye("ACCT");
        a.setAccountNumber(12345678);

        AccountInquiryData b = new AccountInquiryData();
        b.setEye("ACCT");
        b.setAccountNumber(12345678);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

        b.setAccountNumber(99999999);
        assertNotEquals(a, b);
    }

    @Test
    @DisplayName("toString includes key fields")
    void toStringIncludesFields() {
        AccountInquiryData data = new AccountInquiryData();
        data.setAccountNumber(12345678);
        data.setCustomerNumber("CUST001");
        data.setAccountType("ISA");
        data.setSuccess('Y');

        String str = data.toString();
        assertTrue(str.contains("12345678"));
        assertTrue(str.contains("CUST001"));
        assertTrue(str.contains("ISA"));
    }

    @Test
    @DisplayName("All setters and getters round-trip correctly")
    void settersGettersRoundTrip() {
        AccountInquiryData data = new AccountInquiryData();
        data.setEye("ACCT");
        data.setCustomerNumber("C123");
        data.setSortCode("S456");
        data.setAccountNumber(42);
        data.setAccountType("CURRENT");
        data.setInterestRate(new BigDecimal("3.25"));
        data.setOpened(10102020);
        data.setOverdraft(1000);
        data.setLastStatementDate(1012024);
        data.setNextStatementDate(1022024);
        data.setAvailableBalance(new BigDecimal("500.00"));
        data.setActualBalance(new BigDecimal("600.00"));
        data.setSuccess('Y');

        assertEquals("ACCT", data.getEye());
        assertEquals("C123", data.getCustomerNumber());
        assertEquals("S456", data.getSortCode());
        assertEquals(42, data.getAccountNumber());
        assertEquals("CURRENT", data.getAccountType());
        assertEquals(new BigDecimal("3.25"), data.getInterestRate());
        assertEquals(10102020, data.getOpened());
        assertEquals(1000, data.getOverdraft());
        assertEquals(1012024, data.getLastStatementDate());
        assertEquals(1022024, data.getNextStatementDate());
        assertEquals(new BigDecimal("500.00"), data.getAvailableBalance());
        assertEquals(new BigDecimal("600.00"), data.getActualBalance());
        assertEquals('Y', data.getSuccess());
    }
}
