/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from COBOL: BNK1UAC.cbl — DFHCOMMAREA / WS-COMM-AREA
 *
 * Maps the COBOL COMMAREA structure used for communication between
 * the BMS transaction and INQACC/UPDACC programs.
 */
package com.ibm.cics.cip.bank.migrated.bnk1uac;

import java.math.BigDecimal;

public final class AccountCommArea {

    private String eye;
    private String customerNumber;
    private String sortCode;
    private int accountNumber;
    private String accountType;
    private BigDecimal interestRate;
    private int opened;
    private int overdraft;
    private int lastStatementDate;
    private int nextStatementDate;
    private BigDecimal availableBalance;
    private BigDecimal actualBalance;
    private String success;

    public AccountCommArea() {
        this.eye = "";
        this.customerNumber = "";
        this.sortCode = "";
        this.accountNumber = 0;
        this.accountType = "";
        this.interestRate = BigDecimal.ZERO;
        this.opened = 0;
        this.overdraft = 0;
        this.lastStatementDate = 0;
        this.nextStatementDate = 0;
        this.availableBalance = BigDecimal.ZERO;
        this.actualBalance = BigDecimal.ZERO;
        this.success = "";
    }

    public String getEye() {
        return eye;
    }

    public void setEye(String eye) {
        this.eye = eye;
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

    public int getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(int accountNumber) {
        this.accountNumber = accountNumber;
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

    public int getOpened() {
        return opened;
    }

    public void setOpened(int opened) {
        this.opened = opened;
    }

    public int getOverdraft() {
        return overdraft;
    }

    public void setOverdraft(int overdraft) {
        this.overdraft = overdraft;
    }

    public int getLastStatementDate() {
        return lastStatementDate;
    }

    public void setLastStatementDate(int lastStatementDate) {
        this.lastStatementDate = lastStatementDate;
    }

    public int getNextStatementDate() {
        return nextStatementDate;
    }

    public void setNextStatementDate(int nextStatementDate) {
        this.nextStatementDate = nextStatementDate;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public void setAvailableBalance(BigDecimal availableBalance) {
        this.availableBalance = availableBalance;
    }

    public BigDecimal getActualBalance() {
        return actualBalance;
    }

    public void setActualBalance(BigDecimal actualBalance) {
        this.actualBalance = actualBalance;
    }

    public String getSuccess() {
        return success;
    }

    public void setSuccess(String success) {
        this.success = success;
    }

    public boolean isAccountFound() {
        return !(accountType.isBlank() && lastStatementDate == 0);
    }
}
