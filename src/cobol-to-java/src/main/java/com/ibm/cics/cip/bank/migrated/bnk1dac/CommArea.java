/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from WS-COMM-AREA / DFHCOMMAREA in BNK1DAC.cbl.
 */
package com.ibm.cics.cip.bank.migrated.bnk1dac;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Communication area passed between pseudo-conversational transactions.
 * Corresponds to WS-COMM-AREA (lines 108-124) and DFHCOMMAREA (lines 168-184)
 * in the original COBOL source.
 *
 * <p>Field mapping:
 * <pre>
 *   WS-COMM-EYE          -> eye           (4 chars)
 *   WS-COMM-CUSTNO       -> customerNumber(10 chars)
 *   WS-COMM-SCODE        -> sortCode      (6 chars)
 *   WS-COMM-ACCNO        -> accountNumber (8 digits)
 *   WS-COMM-ACC-TYPE     -> accountType   (8 chars)
 *   WS-COMM-INT-RATE     -> interestRate  (PIC 9(4)V99)
 *   WS-COMM-OPENED       -> opened        (PIC 9(8))
 *   WS-COMM-OVERDRAFT    -> overdraft     (PIC 9(8))
 *   WS-COMM-LAST-STMT-DT -> lastStatementDate (PIC 9(8))
 *   WS-COMM-NEXT-STMT-DT -> nextStatementDate (PIC 9(8))
 *   WS-COMM-AVAIL-BAL    -> availableBalance   (PIC S9(10)V99)
 *   WS-COMM-ACTUAL-BAL   -> actualBalance      (PIC S9(10)V99)
 *   WS-COMM-SUCCESS      -> success       (1 char)
 *   WS-COMM-FAIL-CD      -> failCode      (1 char)
 *   WS-COMM-DEL-SUCCESS  -> deleteSuccess (1 char)
 *   WS-COMM-DEL-FAIL-CD  -> deleteFailCode(1 char)
 * </pre>
 */
public class CommArea {

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
    private char success;
    private char failCode;
    private char deleteSuccess;
    private char deleteFailCode;

    public CommArea() {
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
        this.success = ' ';
        this.failCode = ' ';
        this.deleteSuccess = ' ';
        this.deleteFailCode = ' ';
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

    public char getSuccess() {
        return success;
    }

    public void setSuccess(char success) {
        this.success = success;
    }

    public char getFailCode() {
        return failCode;
    }

    public void setFailCode(char failCode) {
        this.failCode = failCode;
    }

    public char getDeleteSuccess() {
        return deleteSuccess;
    }

    public void setDeleteSuccess(char deleteSuccess) {
        this.deleteSuccess = deleteSuccess;
    }

    public char getDeleteFailCode() {
        return deleteFailCode;
    }

    public void setDeleteFailCode(char deleteFailCode) {
        this.deleteFailCode = deleteFailCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CommArea that)) return false;
        return accountNumber == that.accountNumber
                && opened == that.opened
                && overdraft == that.overdraft
                && lastStatementDate == that.lastStatementDate
                && nextStatementDate == that.nextStatementDate
                && success == that.success
                && failCode == that.failCode
                && deleteSuccess == that.deleteSuccess
                && deleteFailCode == that.deleteFailCode
                && Objects.equals(eye, that.eye)
                && Objects.equals(customerNumber, that.customerNumber)
                && Objects.equals(sortCode, that.sortCode)
                && Objects.equals(accountType, that.accountType)
                && Objects.equals(interestRate, that.interestRate)
                && Objects.equals(availableBalance, that.availableBalance)
                && Objects.equals(actualBalance, that.actualBalance);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eye, customerNumber, sortCode, accountNumber,
                accountType, interestRate, opened, overdraft,
                lastStatementDate, nextStatementDate,
                availableBalance, actualBalance,
                success, failCode, deleteSuccess, deleteFailCode);
    }

    @Override
    public String toString() {
        return "CommArea[eye=" + eye
                + ", accNo=" + accountNumber
                + ", sortCode=" + sortCode + "]";
    }
}
