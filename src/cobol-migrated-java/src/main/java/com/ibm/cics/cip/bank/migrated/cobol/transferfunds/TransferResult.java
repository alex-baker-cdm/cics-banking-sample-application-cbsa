/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.migrated.cobol.transferfunds;

import java.math.BigDecimal;

/**
 * Immutable result of a fund transfer operation. Returned by
 * {@link TransferFundsService#processTransfer}. Maps to the output fields that
 * BNK1TFN.cbl writes back to BMS screen map BNK1TFO.
 */
public final class TransferResult {

    private final boolean successful;
    private final String message;
    private final String fromAccountNumber;
    private final String fromSortCode;
    private final String toAccountNumber;
    private final String toSortCode;
    private final BigDecimal fromAvailableBalance;
    private final BigDecimal fromActualBalance;
    private final BigDecimal toAvailableBalance;
    private final BigDecimal toActualBalance;

    private TransferResult(Builder builder) {
        this.successful = builder.successful;
        this.message = builder.message;
        this.fromAccountNumber = builder.fromAccountNumber;
        this.fromSortCode = builder.fromSortCode;
        this.toAccountNumber = builder.toAccountNumber;
        this.toSortCode = builder.toSortCode;
        this.fromAvailableBalance = builder.fromAvailableBalance;
        this.fromActualBalance = builder.fromActualBalance;
        this.toAvailableBalance = builder.toAvailableBalance;
        this.toActualBalance = builder.toActualBalance;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getMessage() {
        return message;
    }

    public String getFromAccountNumber() {
        return fromAccountNumber;
    }

    public String getFromSortCode() {
        return fromSortCode;
    }

    public String getToAccountNumber() {
        return toAccountNumber;
    }

    public String getToSortCode() {
        return toSortCode;
    }

    public BigDecimal getFromAvailableBalance() {
        return fromAvailableBalance;
    }

    public BigDecimal getFromActualBalance() {
        return fromActualBalance;
    }

    public BigDecimal getToAvailableBalance() {
        return toAvailableBalance;
    }

    public BigDecimal getToActualBalance() {
        return toActualBalance;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private boolean successful;
        private String message;
        private String fromAccountNumber;
        private String fromSortCode;
        private String toAccountNumber;
        private String toSortCode;
        private BigDecimal fromAvailableBalance = BigDecimal.ZERO;
        private BigDecimal fromActualBalance = BigDecimal.ZERO;
        private BigDecimal toAvailableBalance = BigDecimal.ZERO;
        private BigDecimal toActualBalance = BigDecimal.ZERO;

        private Builder() {
        }

        public Builder successful(boolean successful) {
            this.successful = successful;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder fromAccountNumber(String fromAccountNumber) {
            this.fromAccountNumber = fromAccountNumber;
            return this;
        }

        public Builder fromSortCode(String fromSortCode) {
            this.fromSortCode = fromSortCode;
            return this;
        }

        public Builder toAccountNumber(String toAccountNumber) {
            this.toAccountNumber = toAccountNumber;
            return this;
        }

        public Builder toSortCode(String toSortCode) {
            this.toSortCode = toSortCode;
            return this;
        }

        public Builder fromAvailableBalance(BigDecimal fromAvailableBalance) {
            this.fromAvailableBalance = fromAvailableBalance;
            return this;
        }

        public Builder fromActualBalance(BigDecimal fromActualBalance) {
            this.fromActualBalance = fromActualBalance;
            return this;
        }

        public Builder toAvailableBalance(BigDecimal toAvailableBalance) {
            this.toAvailableBalance = toAvailableBalance;
            return this;
        }

        public Builder toActualBalance(BigDecimal toActualBalance) {
            this.toActualBalance = toActualBalance;
            return this;
        }

        public TransferResult build() {
            return new TransferResult(this);
        }
    }
}
