/*
 * Copyright IBM Corp. 2023
 */
package com.ibm.cics.cip.bank.migrated.bnk1dac;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScreenFieldTest {

    @Test
    @DisplayName("Default constructor clears all fields")
    void defaultConstructor() {
        ScreenField sf = new ScreenField();
        assertEquals("", sf.getAccountNumberInput());
        assertEquals("", sf.getCustomerNumber());
        assertEquals("", sf.getSortCode());
        assertEquals("", sf.getAccountNumberDisplay());
        assertEquals("", sf.getAccountType());
        assertEquals(BigDecimal.ZERO, sf.getInterestRate());
        assertEquals("", sf.getOpenedDay());
        assertEquals("", sf.getOpenedMonth());
        assertEquals("", sf.getOpenedYear());
        assertEquals("", sf.getOverdraft());
        assertEquals("", sf.getLastStatementDay());
        assertEquals("", sf.getLastStatementMonth());
        assertEquals("", sf.getLastStatementYear());
        assertEquals("", sf.getNextStatementDay());
        assertEquals("", sf.getNextStatementMonth());
        assertEquals("", sf.getNextStatementYear());
        assertEquals("", sf.getAvailableBalance());
        assertEquals("", sf.getActualBalance());
        assertEquals("", sf.getMessage());
    }

    @Test
    @DisplayName("clear resets all fields after modification")
    void clearResetsFields() {
        ScreenField sf = new ScreenField();
        sf.setAccountNumberInput("12345678");
        sf.setCustomerNumber("CUST001");
        sf.setMessage("Hello");
        sf.setInterestRate(new BigDecimal("5.00"));
        sf.setOverdraft("1000");

        sf.clear();

        assertEquals("", sf.getAccountNumberInput());
        assertEquals("", sf.getCustomerNumber());
        assertEquals("", sf.getMessage());
        assertEquals(BigDecimal.ZERO, sf.getInterestRate());
        assertEquals("", sf.getOverdraft());
    }

    @Test
    @DisplayName("All setters and getters round-trip")
    void settersGettersRoundTrip() {
        ScreenField sf = new ScreenField();
        sf.setAccountNumberInput("99999999");
        sf.setCustomerNumber("C001");
        sf.setSortCode("123456");
        sf.setAccountNumberDisplay("DISP001");
        sf.setAccountType("SAVINGS");
        sf.setInterestRate(new BigDecimal("3.75"));
        sf.setOpenedDay("25");
        sf.setOpenedMonth("12");
        sf.setOpenedYear("2023");
        sf.setOverdraft("500");
        sf.setLastStatementDay("01");
        sf.setLastStatementMonth("05");
        sf.setLastStatementYear("2024");
        sf.setNextStatementDay("01");
        sf.setNextStatementMonth("06");
        sf.setNextStatementYear("2024");
        sf.setAvailableBalance("+1500.75");
        sf.setActualBalance("-250.00");
        sf.setMessage("test message");

        assertEquals("99999999", sf.getAccountNumberInput());
        assertEquals("C001", sf.getCustomerNumber());
        assertEquals("123456", sf.getSortCode());
        assertEquals("DISP001", sf.getAccountNumberDisplay());
        assertEquals("SAVINGS", sf.getAccountType());
        assertEquals(new BigDecimal("3.75"), sf.getInterestRate());
        assertEquals("25", sf.getOpenedDay());
        assertEquals("12", sf.getOpenedMonth());
        assertEquals("2023", sf.getOpenedYear());
        assertEquals("500", sf.getOverdraft());
        assertEquals("01", sf.getLastStatementDay());
        assertEquals("05", sf.getLastStatementMonth());
        assertEquals("2024", sf.getLastStatementYear());
        assertEquals("01", sf.getNextStatementDay());
        assertEquals("06", sf.getNextStatementMonth());
        assertEquals("2024", sf.getNextStatementYear());
        assertEquals("+1500.75", sf.getAvailableBalance());
        assertEquals("-250.00", sf.getActualBalance());
        assertEquals("test message", sf.getMessage());
    }
}
