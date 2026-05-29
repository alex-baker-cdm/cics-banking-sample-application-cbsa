/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.migrated.updatecustomer;

/**
 * Represents a customer record in the data store, corresponding to the
 * CUSTOMER copybook (CUSTOMER.cpy) in the original COBOL application.
 *
 * <p>Field sizes mirror the COBOL PIC definitions:
 * <ul>
 *   <li>eyecatcher: PIC X(4) - always "CUST"</li>
 *   <li>sortCode: PIC 9(6)</li>
 *   <li>customerNumber: PIC 9(10)</li>
 *   <li>customerName: PIC X(60)</li>
 *   <li>customerAddress: PIC X(160)</li>
 *   <li>dateOfBirth: PIC 9(8) - format DDMMYYYY</li>
 *   <li>creditScore: PIC 999</li>
 *   <li>creditScoreReviewDate: PIC 9(8) - format DDMMYYYY</li>
 * </ul>
 */
public final class CustomerRecord {

    private String eyecatcher;
    private String sortCode;
    private String customerNumber;
    private String customerName;
    private String customerAddress;
    private String dateOfBirth;
    private int creditScore;
    private String creditScoreReviewDate;

    public CustomerRecord() {
    }

    public CustomerRecord(String eyecatcher, String sortCode,
                          String customerNumber, String customerName,
                          String customerAddress, String dateOfBirth,
                          int creditScore, String creditScoreReviewDate) {
        this.eyecatcher = eyecatcher;
        this.sortCode = sortCode;
        this.customerNumber = customerNumber;
        this.customerName = customerName;
        this.customerAddress = customerAddress;
        this.dateOfBirth = dateOfBirth;
        this.creditScore = creditScore;
        this.creditScoreReviewDate = creditScoreReviewDate;
    }

    public String getEyecatcher() {
        return eyecatcher;
    }

    public void setEyecatcher(String eyecatcher) {
        this.eyecatcher = eyecatcher;
    }

    public String getSortCode() {
        return sortCode;
    }

    public void setSortCode(String sortCode) {
        this.sortCode = sortCode;
    }

    public String getCustomerNumber() {
        return customerNumber;
    }

    public void setCustomerNumber(String customerNumber) {
        this.customerNumber = customerNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerAddress() {
        return customerAddress;
    }

    public void setCustomerAddress(String customerAddress) {
        this.customerAddress = customerAddress;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public int getCreditScore() {
        return creditScore;
    }

    public void setCreditScore(int creditScore) {
        this.creditScore = creditScore;
    }

    public String getCreditScoreReviewDate() {
        return creditScoreReviewDate;
    }

    public void setCreditScoreReviewDate(String creditScoreReviewDate) {
        this.creditScoreReviewDate = creditScoreReviewDate;
    }

    @Override
    public String toString() {
        return "CustomerRecord[eyecatcher=" + eyecatcher
                + ", sortCode=" + sortCode
                + ", customerNumber=" + customerNumber
                + ", customerName=" + customerName
                + ", customerAddress=" + customerAddress
                + ", dateOfBirth=" + dateOfBirth
                + ", creditScore=" + creditScore
                + ", creditScoreReviewDate=" + creditScoreReviewDate + "]";
    }
}
