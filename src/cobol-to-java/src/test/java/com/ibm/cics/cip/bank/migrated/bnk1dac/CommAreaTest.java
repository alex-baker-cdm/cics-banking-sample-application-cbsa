/*
 * Copyright IBM Corp. 2023
 */
package com.ibm.cics.cip.bank.migrated.bnk1dac;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommAreaTest {

    @Test
    @DisplayName("Default construction initializes all fields")
    void defaultConstruction() {
        CommArea comm = new CommArea();
        assertEquals("", comm.getEye());
        assertEquals("", comm.getCustomerNumber());
        assertEquals("", comm.getSortCode());
        assertEquals(0, comm.getAccountNumber());
        assertEquals("", comm.getAccountType());
        assertEquals(BigDecimal.ZERO, comm.getInterestRate());
        assertEquals(0, comm.getOpened());
        assertEquals(0, comm.getOverdraft());
        assertEquals(0, comm.getLastStatementDate());
        assertEquals(0, comm.getNextStatementDate());
        assertEquals(BigDecimal.ZERO, comm.getAvailableBalance());
        assertEquals(BigDecimal.ZERO, comm.getActualBalance());
        assertEquals(' ', comm.getSuccess());
        assertEquals(' ', comm.getFailCode());
        assertEquals(' ', comm.getDeleteSuccess());
        assertEquals(' ', comm.getDeleteFailCode());
    }

    @Test
    @DisplayName("All setters and getters round-trip")
    void settersGettersRoundTrip() {
        CommArea comm = new CommArea();
        comm.setEye("ACCT");
        comm.setCustomerNumber("C001");
        comm.setSortCode("123456");
        comm.setAccountNumber(87654321);
        comm.setAccountType("ISA");
        comm.setInterestRate(new BigDecimal("2.50"));
        comm.setOpened(1012020);
        comm.setOverdraft(2000);
        comm.setLastStatementDate(1042024);
        comm.setNextStatementDate(1052024);
        comm.setAvailableBalance(new BigDecimal("100.00"));
        comm.setActualBalance(new BigDecimal("200.00"));
        comm.setSuccess('Y');
        comm.setFailCode('0');
        comm.setDeleteSuccess('Y');
        comm.setDeleteFailCode(' ');

        assertEquals("ACCT", comm.getEye());
        assertEquals("C001", comm.getCustomerNumber());
        assertEquals("123456", comm.getSortCode());
        assertEquals(87654321, comm.getAccountNumber());
        assertEquals("ISA", comm.getAccountType());
        assertEquals(new BigDecimal("2.50"), comm.getInterestRate());
        assertEquals(1012020, comm.getOpened());
        assertEquals(2000, comm.getOverdraft());
        assertEquals(1042024, comm.getLastStatementDate());
        assertEquals(1052024, comm.getNextStatementDate());
        assertEquals(new BigDecimal("100.00"), comm.getAvailableBalance());
        assertEquals(new BigDecimal("200.00"), comm.getActualBalance());
        assertEquals('Y', comm.getSuccess());
        assertEquals('0', comm.getFailCode());
        assertEquals('Y', comm.getDeleteSuccess());
        assertEquals(' ', comm.getDeleteFailCode());
    }

    @Test
    @DisplayName("equals and hashCode contract")
    void equalsAndHashCode() {
        CommArea a = new CommArea();
        a.setEye("ACCT");
        a.setAccountNumber(100);

        CommArea b = new CommArea();
        b.setEye("ACCT");
        b.setAccountNumber(100);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

        b.setAccountNumber(200);
        assertNotEquals(a, b);
    }

    @Test
    @DisplayName("toString includes key fields")
    void toStringIncludesFields() {
        CommArea comm = new CommArea();
        comm.setEye("ACCT");
        comm.setAccountNumber(12345678);
        comm.setSortCode("987654");

        String str = comm.toString();
        assertTrue(str.contains("ACCT"));
        assertTrue(str.contains("12345678"));
        assertTrue(str.contains("987654"));
    }
}
