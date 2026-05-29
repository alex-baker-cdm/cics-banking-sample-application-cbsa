/*
 *
 *    Copyright IBM Corp. 2023
 *
 *    Equivalent of the DFHCOMMAREA / WS-COMM-AREA in BNK1CAC.cbl
 *    Used for transaction-to-transaction communication.
 *
 */
package com.ibm.cics.cip.bank.migrated.createaccount;

import java.math.BigDecimal;

public class CommArea {

    private String customerNumber;
    private String accountType;
    private BigDecimal interestRate;
    private int overdraftLimit;

    public CommArea() {
        this.customerNumber = "0000000000";
        this.accountType = "";
        this.interestRate = BigDecimal.ZERO;
        this.overdraftLimit = 0;
    }

    public CommArea(String customerNumber, String accountType,
                    BigDecimal interestRate, int overdraftLimit) {
        this.customerNumber = customerNumber;
        this.accountType = accountType;
        this.interestRate = interestRate;
        this.overdraftLimit = overdraftLimit;
    }

    public String getCustomerNumber() {
        return customerNumber;
    }

    public void setCustomerNumber(String customerNumber) {
        this.customerNumber = customerNumber;
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

    public int getOverdraftLimit() {
        return overdraftLimit;
    }

    public void setOverdraftLimit(int overdraftLimit) {
        this.overdraftLimit = overdraftLimit;
    }

    public void initialize() {
        this.customerNumber = "0000000000";
        this.accountType = "";
        this.interestRate = BigDecimal.ZERO;
        this.overdraftLimit = 0;
    }
}
