/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from COBOL: BNK1UAC.cbl — BNK1UAO (map output fields)
 *
 * Represents the screen output data sent to the BMS map.
 * Maps the COBOL fields: ACCNO2O, CUSTNOO, SORTCO, ACTYPEO,
 * INTRTO, OPENDDO/OPENMMO/OPENYYO, OVERDRO, AVBALO, ACTBALO,
 * MESSAGEO, and related display fields.
 */
package com.ibm.cics.cip.bank.migrated.bnk1uac;

import java.math.BigDecimal;

public final class UpdateAccountOutput {

    private int accountNumber;
    private String customerNumber;
    private String sortCode;
    private String accountType;
    private String interestRate;
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

    public UpdateAccountOutput() {
        this.accountNumber = 0;
        this.customerNumber = "";
        this.sortCode = "";
        this.accountType = "";
        this.interestRate = "";
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

    public void populateFromCommArea(AccountCommArea commArea) {
        this.accountNumber = commArea.getAccountNumber();
        this.customerNumber = commArea.getCustomerNumber();
        this.sortCode = commArea.getSortCode();
        this.accountType = commArea.getAccountType();

        BigDecimal rate = commArea.getInterestRate();
        this.interestRate = formatInterestRate(rate);

        int openedDate = commArea.getOpened();
        this.openedDay = extractDay(openedDate);
        this.openedMonth = extractMonth(openedDate);
        this.openedYear = extractYear(openedDate);

        int lastStmt = commArea.getLastStatementDate();
        this.lastStatementDay = extractDay(lastStmt);
        this.lastStatementMonth = extractMonth(lastStmt);
        this.lastStatementYear = extractYear(lastStmt);

        int nextStmt = commArea.getNextStatementDate();
        this.nextStatementDay = extractDay(nextStmt);
        this.nextStatementMonth = extractMonth(nextStmt);
        this.nextStatementYear = extractYear(nextStmt);

        this.overdraft = String.valueOf(commArea.getOverdraft());

        this.availableBalance = formatBalance(commArea.getAvailableBalance());
        this.actualBalance = formatBalance(commArea.getActualBalance());
    }

    static String formatInterestRate(BigDecimal rate) {
        return "%07.2f".formatted(rate.doubleValue());
    }

    static String formatBalance(BigDecimal balance) {
        String sign = balance.signum() < 0 ? "-" : "+";
        BigDecimal abs = balance.abs();
        return "%s%010.2f".formatted(sign, abs.doubleValue());
    }

    static String extractDay(int dateValue) {
        return "%02d".formatted(dateValue / 1000000);
    }

    static String extractMonth(int dateValue) {
        return "%02d".formatted((dateValue / 10000) % 100);
    }

    static String extractYear(int dateValue) {
        return "%04d".formatted(dateValue % 10000);
    }

    public int getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(int accountNumber) {
        this.accountNumber = accountNumber;
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

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public String getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(String interestRate) {
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
