/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from COBOL: BNK1UAC.cbl — BNK1UAI (map input fields)
 *
 * Represents the screen input data received from the BMS map.
 * Maps the COBOL fields: ACCNOI, ACCNO2I, CUSTNOI, SORTCI,
 * ACTYPEI, INTRTI, OPENDDI/OPENMMI/OPENYYI, OVERDRI,
 * LSTMTDDI/LSTMTMMI/LSTMTYYI, NSTMTDDI/NSTMTMMI/NSTMTYYI,
 * AVBALI, ACTBALI.
 */
package com.ibm.cics.cip.bank.migrated.bnk1uac;

public final class UpdateAccountInput {

    private ScreenField accountNumber;
    private ScreenField accountNumber2;
    private ScreenField customerNumber;
    private ScreenField sortCode;
    private ScreenField accountType;
    private ScreenField interestRate;
    private ScreenField openedDay;
    private ScreenField openedMonth;
    private ScreenField openedYear;
    private ScreenField overdraft;
    private ScreenField lastStatementDay;
    private ScreenField lastStatementMonth;
    private ScreenField lastStatementYear;
    private ScreenField nextStatementDay;
    private ScreenField nextStatementMonth;
    private ScreenField nextStatementYear;
    private ScreenField availableBalance;
    private ScreenField actualBalance;

    public UpdateAccountInput() {
        this.accountNumber = new ScreenField("", 0);
        this.accountNumber2 = new ScreenField("", 0);
        this.customerNumber = new ScreenField("", 0);
        this.sortCode = new ScreenField("", 0);
        this.accountType = new ScreenField("", 0);
        this.interestRate = new ScreenField("", 0);
        this.openedDay = new ScreenField("", 0);
        this.openedMonth = new ScreenField("", 0);
        this.openedYear = new ScreenField("", 0);
        this.overdraft = new ScreenField("", 0);
        this.lastStatementDay = new ScreenField("", 0);
        this.lastStatementMonth = new ScreenField("", 0);
        this.lastStatementYear = new ScreenField("", 0);
        this.nextStatementDay = new ScreenField("", 0);
        this.nextStatementMonth = new ScreenField("", 0);
        this.nextStatementYear = new ScreenField("", 0);
        this.availableBalance = new ScreenField("", 0);
        this.actualBalance = new ScreenField("", 0);
    }

    public ScreenField getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(ScreenField accountNumber) {
        this.accountNumber = accountNumber;
    }

    public ScreenField getAccountNumber2() {
        return accountNumber2;
    }

    public void setAccountNumber2(ScreenField accountNumber2) {
        this.accountNumber2 = accountNumber2;
    }

    public ScreenField getCustomerNumber() {
        return customerNumber;
    }

    public void setCustomerNumber(ScreenField customerNumber) {
        this.customerNumber = customerNumber;
    }

    public ScreenField getSortCode() {
        return sortCode;
    }

    public void setSortCode(ScreenField sortCode) {
        this.sortCode = sortCode;
    }

    public ScreenField getAccountType() {
        return accountType;
    }

    public void setAccountType(ScreenField accountType) {
        this.accountType = accountType;
    }

    public ScreenField getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(ScreenField interestRate) {
        this.interestRate = interestRate;
    }

    public ScreenField getOpenedDay() {
        return openedDay;
    }

    public void setOpenedDay(ScreenField openedDay) {
        this.openedDay = openedDay;
    }

    public ScreenField getOpenedMonth() {
        return openedMonth;
    }

    public void setOpenedMonth(ScreenField openedMonth) {
        this.openedMonth = openedMonth;
    }

    public ScreenField getOpenedYear() {
        return openedYear;
    }

    public void setOpenedYear(ScreenField openedYear) {
        this.openedYear = openedYear;
    }

    public ScreenField getOverdraft() {
        return overdraft;
    }

    public void setOverdraft(ScreenField overdraft) {
        this.overdraft = overdraft;
    }

    public ScreenField getLastStatementDay() {
        return lastStatementDay;
    }

    public void setLastStatementDay(ScreenField lastStatementDay) {
        this.lastStatementDay = lastStatementDay;
    }

    public ScreenField getLastStatementMonth() {
        return lastStatementMonth;
    }

    public void setLastStatementMonth(ScreenField lastStatementMonth) {
        this.lastStatementMonth = lastStatementMonth;
    }

    public ScreenField getLastStatementYear() {
        return lastStatementYear;
    }

    public void setLastStatementYear(ScreenField lastStatementYear) {
        this.lastStatementYear = lastStatementYear;
    }

    public ScreenField getNextStatementDay() {
        return nextStatementDay;
    }

    public void setNextStatementDay(ScreenField nextStatementDay) {
        this.nextStatementDay = nextStatementDay;
    }

    public ScreenField getNextStatementMonth() {
        return nextStatementMonth;
    }

    public void setNextStatementMonth(ScreenField nextStatementMonth) {
        this.nextStatementMonth = nextStatementMonth;
    }

    public ScreenField getNextStatementYear() {
        return nextStatementYear;
    }

    public void setNextStatementYear(ScreenField nextStatementYear) {
        this.nextStatementYear = nextStatementYear;
    }

    public ScreenField getAvailableBalance() {
        return availableBalance;
    }

    public void setAvailableBalance(ScreenField availableBalance) {
        this.availableBalance = availableBalance;
    }

    public ScreenField getActualBalance() {
        return actualBalance;
    }

    public void setActualBalance(ScreenField actualBalance) {
        this.actualBalance = actualBalance;
    }
}
