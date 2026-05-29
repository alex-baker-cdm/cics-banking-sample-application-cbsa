package com.ibm.cics.cip.bank.migrated.displaycustomer;

/**
 * Corresponds to the INQCUST COPY structure used by the INQCUST sub-program.
 * Migrated from: COBOL copybook INQCUST.cpy
 */
public class InqCustCommarea {

    private String eye;
    private String sortCode;
    private long customerNumber;
    private String name;
    private String address;
    private int dobDay;
    private int dobMonth;
    private int dobYear;
    private int creditScore;
    private int csReviewDay;
    private int csReviewMonth;
    private int csReviewYear;
    private String inquirySuccess;
    private String inquiryFailCode;

    public InqCustCommarea() {
        this.eye = "";
        this.sortCode = "";
        this.customerNumber = 0;
        this.name = "";
        this.address = "";
        this.dobDay = 0;
        this.dobMonth = 0;
        this.dobYear = 0;
        this.creditScore = 0;
        this.csReviewDay = 0;
        this.csReviewMonth = 0;
        this.csReviewYear = 0;
        this.inquirySuccess = "";
        this.inquiryFailCode = "";
    }

    public String getEye() {
        return eye;
    }

    public void setEye(String eye) {
        this.eye = eye;
    }

    public String getSortCode() {
        return sortCode;
    }

    public void setSortCode(String sortCode) {
        this.sortCode = sortCode;
    }

    public long getCustomerNumber() {
        return customerNumber;
    }

    public void setCustomerNumber(long customerNumber) {
        this.customerNumber = customerNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getDobDay() {
        return dobDay;
    }

    public void setDobDay(int dobDay) {
        this.dobDay = dobDay;
    }

    public int getDobMonth() {
        return dobMonth;
    }

    public void setDobMonth(int dobMonth) {
        this.dobMonth = dobMonth;
    }

    public int getDobYear() {
        return dobYear;
    }

    public void setDobYear(int dobYear) {
        this.dobYear = dobYear;
    }

    public int getCreditScore() {
        return creditScore;
    }

    public void setCreditScore(int creditScore) {
        this.creditScore = creditScore;
    }

    public int getCsReviewDay() {
        return csReviewDay;
    }

    public void setCsReviewDay(int csReviewDay) {
        this.csReviewDay = csReviewDay;
    }

    public int getCsReviewMonth() {
        return csReviewMonth;
    }

    public void setCsReviewMonth(int csReviewMonth) {
        this.csReviewMonth = csReviewMonth;
    }

    public int getCsReviewYear() {
        return csReviewYear;
    }

    public void setCsReviewYear(int csReviewYear) {
        this.csReviewYear = csReviewYear;
    }

    public String getInquirySuccess() {
        return inquirySuccess;
    }

    public void setInquirySuccess(String inquirySuccess) {
        this.inquirySuccess = inquirySuccess;
    }

    public String getInquiryFailCode() {
        return inquiryFailCode;
    }

    public void setInquiryFailCode(String inquiryFailCode) {
        this.inquiryFailCode = inquiryFailCode;
    }

    public void initialize() {
        this.eye = "";
        this.sortCode = "";
        this.customerNumber = 0;
        this.name = "";
        this.address = "";
        this.dobDay = 0;
        this.dobMonth = 0;
        this.dobYear = 0;
        this.creditScore = 0;
        this.csReviewDay = 0;
        this.csReviewMonth = 0;
        this.csReviewYear = 0;
        this.inquirySuccess = "";
        this.inquiryFailCode = "";
    }
}
