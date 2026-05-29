/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.cobol.inquire.account;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Represents an account record as defined by the ACCOUNT copybook (ACCOUNT.cpy).
 *
 * Mirrors the COBOL data structure:
 *   03 ACCOUNT-DATA.
 *     05 ACCOUNT-EYE-CATCHER        PIC X(4).
 *     05 ACCOUNT-CUST-NO            PIC 9(10).
 *     05 ACCOUNT-SORT-CODE          PIC 9(6).
 *     05 ACCOUNT-NUMBER             PIC 9(8).
 *     05 ACCOUNT-TYPE               PIC X(8).
 *     05 ACCOUNT-INTEREST-RATE      PIC 9(4)V99.
 *     05 ACCOUNT-OPENED             PIC 9(8).
 *     05 ACCOUNT-OVERDRAFT-LIMIT    PIC 9(8).
 *     05 ACCOUNT-LAST-STMT-DATE     PIC 9(8).
 *     05 ACCOUNT-NEXT-STMT-DATE     PIC 9(8).
 *     05 ACCOUNT-AVAILABLE-BALANCE  PIC S9(10)V99.
 *     05 ACCOUNT-ACTUAL-BALANCE     PIC S9(10)V99.
 */
public class AccountData {

    private String eyeCatcher;
    private long customerNumber;
    private int sortCode;
    private int accountNumber;
    private String accountType;
    private BigDecimal interestRate;
    private LocalDate dateOpened;
    private int overdraftLimit;
    private LocalDate lastStatementDate;
    private LocalDate nextStatementDate;
    private BigDecimal availableBalance;
    private BigDecimal actualBalance;

    public AccountData() {
        this.eyeCatcher = "";
        this.customerNumber = 0;
        this.sortCode = 0;
        this.accountNumber = 0;
        this.accountType = "";
        this.interestRate = BigDecimal.ZERO;
        this.dateOpened = null;
        this.overdraftLimit = 0;
        this.lastStatementDate = null;
        this.nextStatementDate = null;
        this.availableBalance = BigDecimal.ZERO;
        this.actualBalance = BigDecimal.ZERO;
    }

    public String getEyeCatcher() {
        return eyeCatcher;
    }

    public void setEyeCatcher(String eyeCatcher) {
        this.eyeCatcher = eyeCatcher;
    }

    public long getCustomerNumber() {
        return customerNumber;
    }

    public void setCustomerNumber(long customerNumber) {
        this.customerNumber = customerNumber;
    }

    public int getSortCode() {
        return sortCode;
    }

    public void setSortCode(int sortCode) {
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

    public LocalDate getDateOpened() {
        return dateOpened;
    }

    public void setDateOpened(LocalDate dateOpened) {
        this.dateOpened = dateOpened;
    }

    public int getOverdraftLimit() {
        return overdraftLimit;
    }

    public void setOverdraftLimit(int overdraftLimit) {
        this.overdraftLimit = overdraftLimit;
    }

    public LocalDate getLastStatementDate() {
        return lastStatementDate;
    }

    public void setLastStatementDate(LocalDate lastStatementDate) {
        this.lastStatementDate = lastStatementDate;
    }

    public LocalDate getNextStatementDate() {
        return nextStatementDate;
    }

    public void setNextStatementDate(LocalDate nextStatementDate) {
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

    public boolean hasValidAccountType() {
        return accountType != null
                && !accountType.isBlank()
                && !accountType.equals("\u0000".repeat(accountType.length()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccountData that = (AccountData) o;
        return customerNumber == that.customerNumber
                && sortCode == that.sortCode
                && accountNumber == that.accountNumber
                && overdraftLimit == that.overdraftLimit
                && Objects.equals(eyeCatcher, that.eyeCatcher)
                && Objects.equals(accountType, that.accountType)
                && Objects.equals(interestRate, that.interestRate)
                && Objects.equals(dateOpened, that.dateOpened)
                && Objects.equals(lastStatementDate, that.lastStatementDate)
                && Objects.equals(nextStatementDate, that.nextStatementDate)
                && Objects.equals(availableBalance, that.availableBalance)
                && Objects.equals(actualBalance, that.actualBalance);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eyeCatcher, customerNumber, sortCode, accountNumber,
                accountType, interestRate, dateOpened, overdraftLimit,
                lastStatementDate, nextStatementDate, availableBalance,
                actualBalance);
    }

    @Override
    public String toString() {
        return "AccountData["
                + "eyeCatcher=" + eyeCatcher
                + ", customerNumber=" + customerNumber
                + ", sortCode=" + sortCode
                + ", accountNumber=" + accountNumber
                + ", accountType=" + accountType
                + ", interestRate=" + interestRate
                + ", dateOpened=" + dateOpened
                + ", overdraftLimit=" + overdraftLimit
                + ", lastStatementDate=" + lastStatementDate
                + ", nextStatementDate=" + nextStatementDate
                + ", availableBalance=" + availableBalance
                + ", actualBalance=" + actualBalance
                + ']';
    }
}
