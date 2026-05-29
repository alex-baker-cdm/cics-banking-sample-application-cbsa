/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.migrated.createaccount;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CommAreaTest {

    @Test
    @DisplayName("Default constructor initializes to zeroed values")
    void defaultConstructor() {
        CommArea commArea = new CommArea();
        assertEquals("0000000000", commArea.getCustomerNumber());
        assertEquals("", commArea.getAccountType());
        assertEquals(BigDecimal.ZERO, commArea.getInterestRate());
        assertEquals(0, commArea.getOverdraftLimit());
    }

    @Test
    @DisplayName("Parameterized constructor sets all fields")
    void parameterizedConstructor() {
        CommArea commArea = new CommArea("1234567890", "ISA",
                new BigDecimal("3.50"), 1000);
        assertEquals("1234567890", commArea.getCustomerNumber());
        assertEquals("ISA", commArea.getAccountType());
        assertEquals(new BigDecimal("3.50"), commArea.getInterestRate());
        assertEquals(1000, commArea.getOverdraftLimit());
    }

    @Test
    @DisplayName("Initialize resets all fields to defaults")
    void initializeResetsFields() {
        CommArea commArea = new CommArea("1234567890", "CURRENT",
                new BigDecimal("5.00"), 2000);
        commArea.initialize();

        assertEquals("0000000000", commArea.getCustomerNumber());
        assertEquals("", commArea.getAccountType());
        assertEquals(BigDecimal.ZERO, commArea.getInterestRate());
        assertEquals(0, commArea.getOverdraftLimit());
    }

    @Test
    @DisplayName("Setters update fields correctly")
    void settersWork() {
        CommArea commArea = new CommArea();
        commArea.setCustomerNumber("9999999999");
        commArea.setAccountType("MORTGAGE");
        commArea.setInterestRate(new BigDecimal("7.25"));
        commArea.setOverdraftLimit(5000);

        assertEquals("9999999999", commArea.getCustomerNumber());
        assertEquals("MORTGAGE", commArea.getAccountType());
        assertEquals(new BigDecimal("7.25"), commArea.getInterestRate());
        assertEquals(5000, commArea.getOverdraftLimit());
    }
}
