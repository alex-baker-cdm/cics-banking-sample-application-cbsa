/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.migrated.cobol.updateaccount;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents the output of the Update Account operation.
 * Maps to the COBOL COMMAREA output fields defined in UPDACC.cpy.
 *
 * <p>On success, all account fields are populated from the database.
 * On failure, only {@code success} is {@code false} and
 * {@code failureMessage} describes the reason.
 */
public final class UpdateAccountResponse {

    private boolean success;
    private String eyeCatcher;
    private String customerNumber;
    private String sortCode;
    private String accountNumber;
    private String accountType;
    private BigDecimal interestRate;
    private LocalDate dateOpened;
    private int overdraftLimit;
    private LocalDate lastStatementDate;
    private LocalDate nextStatementDate;
    private BigDecimal availableBalance;
    private BigDecimal actualBalance;
    private String failureMessage;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
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

    public String getFailureMessage() {
        return failureMessage;
    }

    public void setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
    }

    @Override
    public String toString() {
        return "UpdateAccountResponse["
                + "success=" + success
                + ", eyeCatcher=" + eyeCatcher
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
                + "]";
    }
}
