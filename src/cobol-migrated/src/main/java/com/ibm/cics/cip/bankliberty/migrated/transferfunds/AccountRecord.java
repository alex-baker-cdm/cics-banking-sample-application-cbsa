/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bankliberty.migrated.transferfunds;

import java.math.BigDecimal;

/**
 * Mutable representation of a row in the ACCOUNT DB2 table.
 * <p>
 * Migrated from the COBOL HOST-ACCOUNT-ROW structure and
 * the ACCOUNT.cpy copybook.
 */
public final class AccountRecord {

    private String eyeCatcher;
    private String customerNumber;
    private String sortCode;
    private String accountNumber;
    private String accountType;
    private BigDecimal interestRate;
    private String dateOpened;
    private int overdraftLimit;
    private String lastStatementDate;
    private String nextStatementDate;
    private BigDecimal availableBalance;
    private BigDecimal actualBalance;

    public AccountRecord() {
    }

    public AccountRecord(String eyeCatcher, String customerNumber,
                          String sortCode, String accountNumber,
                          String accountType, BigDecimal interestRate,
                          String dateOpened, int overdraftLimit,
                          String lastStatementDate, String nextStatementDate,
                          BigDecimal availableBalance, BigDecimal actualBalance) {
        this.eyeCatcher = eyeCatcher;
        this.customerNumber = customerNumber;
        this.sortCode = sortCode;
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.interestRate = interestRate;
        this.dateOpened = dateOpened;
        this.overdraftLimit = overdraftLimit;
        this.lastStatementDate = lastStatementDate;
        this.nextStatementDate = nextStatementDate;
        this.availableBalance = availableBalance;
        this.actualBalance = actualBalance;
    }

    public String getEyeCatcher() {
        return eyeCatcher;
    }

    public void setEyeCatcher(String eyeCatcher) {
        this.eyeCatcher = eyeCatcher;
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

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
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

    public String getDateOpened() {
        return dateOpened;
    }

    public void setDateOpened(String dateOpened) {
        this.dateOpened = dateOpened;
    }

    public int getOverdraftLimit() {
        return overdraftLimit;
    }

    public void setOverdraftLimit(int overdraftLimit) {
        this.overdraftLimit = overdraftLimit;
    }

    public String getLastStatementDate() {
        return lastStatementDate;
    }

    public void setLastStatementDate(String lastStatementDate) {
        this.lastStatementDate = lastStatementDate;
    }

    public String getNextStatementDate() {
        return nextStatementDate;
    }

    public void setNextStatementDate(String nextStatementDate) {
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
}
