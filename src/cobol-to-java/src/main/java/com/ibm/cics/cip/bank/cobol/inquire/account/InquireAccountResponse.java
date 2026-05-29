/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.cobol.inquire.account;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents the output portion of the INQACC COMMAREA (INQACC.cpy).
 *
 * COBOL fields mapped:
 *   03 INQACC-EYE           PIC X(4)
 *   03 INQACC-CUSTNO        PIC 9(10)
 *   03 INQACC-SCODE         PIC 9(6)
 *   03 INQACC-ACCNO         PIC 9(8)
 *   03 INQACC-ACC-TYPE      PIC X(8)
 *   03 INQACC-INT-RATE      PIC 9(4)V99
 *   03 INQACC-OPENED        PIC 9(8)
 *   03 INQACC-OVERDRAFT     PIC 9(8)
 *   03 INQACC-LAST-STMT-DT  PIC 9(8)
 *   03 INQACC-NEXT-STMT-DT  PIC 9(8)
 *   03 INQACC-AVAIL-BAL     PIC S9(10)V99
 *   03 INQACC-ACTUAL-BAL    PIC S9(10)V99
 *   03 INQACC-SUCCESS       PIC X
 */
public class InquireAccountResponse {

    private final boolean success;
    private final String eyeCatcher;
    private final long customerNumber;
    private final int sortCode;
    private final int accountNumber;
    private final String accountType;
    private final BigDecimal interestRate;
    private final LocalDate dateOpened;
    private final int overdraftLimit;
    private final LocalDate lastStatementDate;
    private final LocalDate nextStatementDate;
    private final BigDecimal availableBalance;
    private final BigDecimal actualBalance;

    private InquireAccountResponse(Builder builder) {
        this.success = builder.success;
        this.eyeCatcher = builder.eyeCatcher;
        this.customerNumber = builder.customerNumber;
        this.sortCode = builder.sortCode;
        this.accountNumber = builder.accountNumber;
        this.accountType = builder.accountType;
        this.interestRate = builder.interestRate;
        this.dateOpened = builder.dateOpened;
        this.overdraftLimit = builder.overdraftLimit;
        this.lastStatementDate = builder.lastStatementDate;
        this.nextStatementDate = builder.nextStatementDate;
        this.availableBalance = builder.availableBalance;
        this.actualBalance = builder.actualBalance;
    }

    public static InquireAccountResponse failure() {
        return new Builder().success(false).build();
    }

    public static InquireAccountResponse fromAccountData(AccountData data) {
        return new Builder()
                .success(true)
                .eyeCatcher(data.getEyeCatcher())
                .customerNumber(data.getCustomerNumber())
                .sortCode(data.getSortCode())
                .accountNumber(data.getAccountNumber())
                .accountType(data.getAccountType())
                .interestRate(data.getInterestRate())
                .dateOpened(data.getDateOpened())
                .overdraftLimit(data.getOverdraftLimit())
                .lastStatementDate(data.getLastStatementDate())
                .nextStatementDate(data.getNextStatementDate())
                .availableBalance(data.getAvailableBalance())
                .actualBalance(data.getActualBalance())
                .build();
    }

    public boolean isSuccess() {
        return success;
    }

    public String getEyeCatcher() {
        return eyeCatcher;
    }

    public long getCustomerNumber() {
        return customerNumber;
    }

    public int getSortCode() {
        return sortCode;
    }

    public int getAccountNumber() {
        return accountNumber;
    }

    public String getAccountType() {
        return accountType;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public LocalDate getDateOpened() {
        return dateOpened;
    }

    public int getOverdraftLimit() {
        return overdraftLimit;
    }

    public LocalDate getLastStatementDate() {
        return lastStatementDate;
    }

    public LocalDate getNextStatementDate() {
        return nextStatementDate;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public BigDecimal getActualBalance() {
        return actualBalance;
    }

    @Override
    public String toString() {
        return "InquireAccountResponse["
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
                + ']';
    }

    public static class Builder {

        private boolean success;
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

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder eyeCatcher(String eyeCatcher) {
            this.eyeCatcher = eyeCatcher;
            return this;
        }

        public Builder customerNumber(long customerNumber) {
            this.customerNumber = customerNumber;
            return this;
        }

        public Builder sortCode(int sortCode) {
            this.sortCode = sortCode;
            return this;
        }

        public Builder accountNumber(int accountNumber) {
            this.accountNumber = accountNumber;
            return this;
        }

        public Builder accountType(String accountType) {
            this.accountType = accountType;
            return this;
        }

        public Builder interestRate(BigDecimal interestRate) {
            this.interestRate = interestRate;
            return this;
        }

        public Builder dateOpened(LocalDate dateOpened) {
            this.dateOpened = dateOpened;
            return this;
        }

        public Builder overdraftLimit(int overdraftLimit) {
            this.overdraftLimit = overdraftLimit;
            return this;
        }

        public Builder lastStatementDate(LocalDate lastStatementDate) {
            this.lastStatementDate = lastStatementDate;
            return this;
        }

        public Builder nextStatementDate(LocalDate nextStatementDate) {
            this.nextStatementDate = nextStatementDate;
            return this;
        }

        public Builder availableBalance(BigDecimal availableBalance) {
            this.availableBalance = availableBalance;
            return this;
        }

        public Builder actualBalance(BigDecimal actualBalance) {
            this.actualBalance = actualBalance;
            return this;
        }

        public InquireAccountResponse build() {
            return new InquireAccountResponse(this);
        }
    }
}
