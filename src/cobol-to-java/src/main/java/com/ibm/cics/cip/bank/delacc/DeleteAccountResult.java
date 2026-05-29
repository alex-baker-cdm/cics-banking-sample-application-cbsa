/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from COBOL copybook DELACC.cpy (DELACC-COMMAREA output fields).
 * Contains the result of a delete-account operation.
 */
package com.ibm.cics.cip.bank.delacc;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class DeleteAccountResult {

    private String eyeCatcher;
    private String customerNumber;
    private String sortCode;
    private String accountNumber;
    private String accountType;
    private BigDecimal interestRate;
    private LocalDate opened;
    private int overdraftLimit;
    private LocalDate lastStatementDate;
    private LocalDate nextStatementDate;
    private BigDecimal availableBalance;
    private BigDecimal actualBalance;
    private boolean deleteSuccess;
    private String deleteFailCode;

    public DeleteAccountResult() {
        this.deleteSuccess = false;
        this.deleteFailCode = " ";
    }

    public static DeleteAccountResult fromAccountRecord(AccountRecord record) {
        DeleteAccountResult result = new DeleteAccountResult();
        result.eyeCatcher = record.eyeCatcher();
        result.customerNumber = record.customerNumber();
        result.sortCode = record.sortCode();
        result.accountNumber = record.accountNumber();
        result.accountType = record.accountType();
        result.interestRate = record.interestRate();
        result.opened = record.opened();
        result.overdraftLimit = record.overdraftLimit();
        result.lastStatementDate = record.lastStatementDate();
        result.nextStatementDate = record.nextStatementDate();
        result.availableBalance = record.availableBalance();
        result.actualBalance = record.actualBalance();
        result.deleteSuccess = true;
        result.deleteFailCode = " ";
        return result;
    }

    public static DeleteAccountResult notFound(String sortCode, String accountNumber) {
        DeleteAccountResult result = new DeleteAccountResult();
        result.sortCode = sortCode;
        result.accountNumber = accountNumber;
        result.deleteSuccess = false;
        result.deleteFailCode = "1";
        return result;
    }

    public static DeleteAccountResult deleteFailed(AccountRecord record) {
        DeleteAccountResult result = fromAccountRecord(record);
        result.deleteSuccess = false;
        result.deleteFailCode = "3";
        return result;
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

    public LocalDate getOpened() {
        return opened;
    }

    public void setOpened(LocalDate opened) {
        this.opened = opened;
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

    public boolean isDeleteSuccess() {
        return deleteSuccess;
    }

    public void setDeleteSuccess(boolean deleteSuccess) {
        this.deleteSuccess = deleteSuccess;
    }

    public String getDeleteFailCode() {
        return deleteFailCode;
    }

    public void setDeleteFailCode(String deleteFailCode) {
        this.deleteFailCode = deleteFailCode;
    }
}
