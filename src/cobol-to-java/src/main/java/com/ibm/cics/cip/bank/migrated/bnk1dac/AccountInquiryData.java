/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from COBOL copybook INQACC.cpy and PARMS-SUBPGM (BNK1DAC.cbl).
 */
package com.ibm.cics.cip.bank.migrated.bnk1dac;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Data transfer object representing the INQACC COMMAREA used to pass
 * account inquiry results between BNK1DAC and the INQACC/DELACC sub-programs.
 *
 * <p>Field mapping from COBOL:
 * <pre>
 *   INQACC-EYE          -> eye           (4-char eyecatcher, valid = "ACCT")
 *   INQACC-CUSTNO       -> customerNumber
 *   INQACC-SCODE        -> sortCode
 *   INQACC-ACCNO        -> accountNumber
 *   INQACC-ACC-TYPE     -> accountType
 *   INQACC-INT-RATE     -> interestRate  (PIC 9(4)V99 => BigDecimal scale 2)
 *   INQACC-OPENED       -> opened        (DDMMYYYY as int)
 *   INQACC-OVERDRAFT    -> overdraft
 *   INQACC-LAST-STMT-DT -> lastStatementDate (DDMMYYYY as int)
 *   INQACC-NEXT-STMT-DT -> nextStatementDate (DDMMYYYY as int)
 *   INQACC-AVAIL-BAL    -> availableBalance
 *   INQACC-ACTUAL-BAL   -> actualBalance
 *   INQACC-SUCCESS      -> success       ('Y'/'N')
 *   INQACC-PCB1-POINTER -> (not migrated - DB2 PCB pointer)
 * </pre>
 */
public class AccountInquiryData {

    public static final String VALID_EYE = "ACCT";

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

    public AccountInquiryData() {
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
    }

    public boolean isEyeValid() {
        return VALID_EYE.equals(eye);
    }

    public boolean isAccountFound() {
        return !(accountType.isBlank()
                && interestRate.compareTo(BigDecimal.ZERO) == 0
                && success == 'N');
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AccountInquiryData that)) return false;
        return accountNumber == that.accountNumber
                && opened == that.opened
                && overdraft == that.overdraft
                && lastStatementDate == that.lastStatementDate
                && nextStatementDate == that.nextStatementDate
                && success == that.success
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
                availableBalance, actualBalance, success);
    }

    @Override
    public String toString() {
        return "AccountInquiryData[accNo=" + accountNumber
                + ", custNo=" + customerNumber
                + ", type=" + accountType
                + ", success=" + success + "]";
    }
}
