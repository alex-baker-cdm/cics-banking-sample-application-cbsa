/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from BNK1DAM.bms — represents BMS screen field values.
 */
package com.ibm.cics.cip.bank.migrated.bnk1dac;

import java.math.BigDecimal;

/**
 * Represents the screen fields of the BNK1DA BMS map.
 * Each field corresponds to a named DFHMDF in BNK1DAM.bms.
 *
 * <p>Field mapping from BMS:
 * <pre>
 *   ACCNO    -> accountNumberInput  (input, 8 chars)
 *   CUSTNO   -> customerNumber      (output, 10 chars)
 *   SORTC    -> sortCode            (output, 6 chars)
 *   ACCNO2   -> accountNumberDisplay(output, 10 chars)
 *   ACTYPE   -> accountType         (output, 8 chars)
 *   INTRT    -> interestRate        (output, PICOUT 9999.99)
 *   OPENDD/MM/YY -> openedDay/Month/Year
 *   OVERDR   -> overdraft           (output, 8 chars)
 *   LSTMTDD/MM/YY -> lastStatementDay/Month/Year
 *   NSTMTDD/MM/YY -> nextStatementDay/Month/Year
 *   AVBAL    -> availableBalance    (output, 14 chars)
 *   ACTBAL   -> actualBalance       (output, 14 chars)
 *   MESSAGE  -> message             (output, 79 chars)
 * </pre>
 */
public class ScreenField {

    private String accountNumberInput;
    private String customerNumber;
    private String sortCode;
    private String accountNumberDisplay;
    private String accountType;
    private BigDecimal interestRate;
    private String openedDay;
    private String openedMonth;
    private String openedYear;
    private String overdraft;
    private String lastStatementDay;
    private String lastStatementMonth;
    private String lastStatementYear;
    private String nextStatementDay;
    private String nextStatementMonth;
    private String nextStatementYear;
    private String availableBalance;
    private String actualBalance;
    private String message;

    public ScreenField() {
        clear();
    }

    public void clear() {
        this.accountNumberInput = "";
        this.customerNumber = "";
        this.sortCode = "";
        this.accountNumberDisplay = "";
        this.accountType = "";
        this.interestRate = BigDecimal.ZERO;
        this.openedDay = "";
        this.openedMonth = "";
        this.openedYear = "";
        this.overdraft = "";
        this.lastStatementDay = "";
        this.lastStatementMonth = "";
        this.lastStatementYear = "";
        this.nextStatementDay = "";
        this.nextStatementMonth = "";
        this.nextStatementYear = "";
        this.availableBalance = "";
        this.actualBalance = "";
        this.message = "";
    }

    public String getAccountNumberInput() {
        return accountNumberInput;
    }

    public void setAccountNumberInput(String accountNumberInput) {
        this.accountNumberInput = accountNumberInput;
    }

    public String getCustomerNumber() {
        return customerNumber;
    }

    public void setCustomerNumber(String customerNumber) {
        this.customerNumber = customerNumber;
    }

    public String getSortCode() {
        return sortCode;
    }

    public void setSortCode(String sortCode) {
        this.sortCode = sortCode;
    }

    public String getAccountNumberDisplay() {
        return accountNumberDisplay;
    }

    public void setAccountNumberDisplay(String accountNumberDisplay) {
        this.accountNumberDisplay = accountNumberDisplay;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }

    public String getOpenedDay() {
        return openedDay;
    }

    public void setOpenedDay(String openedDay) {
        this.openedDay = openedDay;
    }

    public String getOpenedMonth() {
        return openedMonth;
    }

    public void setOpenedMonth(String openedMonth) {
        this.openedMonth = openedMonth;
    }

    public String getOpenedYear() {
        return openedYear;
    }

    public void setOpenedYear(String openedYear) {
        this.openedYear = openedYear;
    }

    public String getOverdraft() {
        return overdraft;
    }

    public void setOverdraft(String overdraft) {
        this.overdraft = overdraft;
    }

    public String getLastStatementDay() {
        return lastStatementDay;
    }

    public void setLastStatementDay(String lastStatementDay) {
        this.lastStatementDay = lastStatementDay;
    }

    public String getLastStatementMonth() {
        return lastStatementMonth;
    }

    public void setLastStatementMonth(String lastStatementMonth) {
        this.lastStatementMonth = lastStatementMonth;
    }

    public String getLastStatementYear() {
        return lastStatementYear;
    }

    public void setLastStatementYear(String lastStatementYear) {
        this.lastStatementYear = lastStatementYear;
    }

    public String getNextStatementDay() {
        return nextStatementDay;
    }

    public void setNextStatementDay(String nextStatementDay) {
        this.nextStatementDay = nextStatementDay;
    }

    public String getNextStatementMonth() {
        return nextStatementMonth;
    }

    public void setNextStatementMonth(String nextStatementMonth) {
        this.nextStatementMonth = nextStatementMonth;
    }

    public String getNextStatementYear() {
        return nextStatementYear;
    }

    public void setNextStatementYear(String nextStatementYear) {
        this.nextStatementYear = nextStatementYear;
    }

    public String getAvailableBalance() {
        return availableBalance;
    }

    public void setAvailableBalance(String availableBalance) {
        this.availableBalance = availableBalance;
    }

    public String getActualBalance() {
        return actualBalance;
    }

    public void setActualBalance(String actualBalance) {
        this.actualBalance = actualBalance;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
