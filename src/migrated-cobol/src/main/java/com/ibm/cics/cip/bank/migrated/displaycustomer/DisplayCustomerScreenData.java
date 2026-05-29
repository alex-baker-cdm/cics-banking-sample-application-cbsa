package com.ibm.cics.cip.bank.migrated.displaycustomer;

/**
 * Represents the BMS map fields for the BNK1DC screen (BNK1DCM mapset).
 * Combines both input (I suffix) and output (O suffix) fields from the COBOL map.
 *
 * Migrated from: BNK1DCM.bms / BNK1DCM COPY
 */
public class DisplayCustomerScreenData {

    private String customerNumberInput;
    private String sortCode;
    private String customerNumber2;
    private String customerName;
    private String customerAddress1;
    private String customerAddress2;
    private String customerAddress3;
    private String dobDay;
    private String dobMonth;
    private String dobYear;
    private String creditScore;
    private String csReviewDateDay;
    private String csReviewDateMonth;
    private String csReviewDateYear;
    private String message;

    public DisplayCustomerScreenData() {
        clear();
    }

    public void clear() {
        this.customerNumberInput = "";
        this.sortCode = "";
        this.customerNumber2 = "";
        this.customerName = "";
        this.customerAddress1 = "";
        this.customerAddress2 = "";
        this.customerAddress3 = "";
        this.dobDay = "";
        this.dobMonth = "";
        this.dobYear = "";
        this.creditScore = "";
        this.csReviewDateDay = "";
        this.csReviewDateMonth = "";
        this.csReviewDateYear = "";
        this.message = "";
    }

    public String getCustomerNumberInput() {
        return customerNumberInput;
    }

    public void setCustomerNumberInput(String customerNumberInput) {
        this.customerNumberInput = customerNumberInput;
    }

    public String getSortCode() {
        return sortCode;
    }

    public void setSortCode(String sortCode) {
        this.sortCode = sortCode;
    }

    public String getCustomerNumber2() {
        return customerNumber2;
    }

    public void setCustomerNumber2(String customerNumber2) {
        this.customerNumber2 = customerNumber2;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerAddress1() {
        return customerAddress1;
    }

    public void setCustomerAddress1(String customerAddress1) {
        this.customerAddress1 = customerAddress1;
    }

    public String getCustomerAddress2() {
        return customerAddress2;
    }

    public void setCustomerAddress2(String customerAddress2) {
        this.customerAddress2 = customerAddress2;
    }

    public String getCustomerAddress3() {
        return customerAddress3;
    }

    public void setCustomerAddress3(String customerAddress3) {
        this.customerAddress3 = customerAddress3;
    }

    public String getDobDay() {
        return dobDay;
    }

    public void setDobDay(String dobDay) {
        this.dobDay = dobDay;
    }

    public String getDobMonth() {
        return dobMonth;
    }

    public void setDobMonth(String dobMonth) {
        this.dobMonth = dobMonth;
    }

    public String getDobYear() {
        return dobYear;
    }

    public void setDobYear(String dobYear) {
        this.dobYear = dobYear;
    }

    public String getCreditScore() {
        return creditScore;
    }

    public void setCreditScore(String creditScore) {
        this.creditScore = creditScore;
    }

    public String getCsReviewDateDay() {
        return csReviewDateDay;
    }

    public void setCsReviewDateDay(String csReviewDateDay) {
        this.csReviewDateDay = csReviewDateDay;
    }

    public String getCsReviewDateMonth() {
        return csReviewDateMonth;
    }

    public void setCsReviewDateMonth(String csReviewDateMonth) {
        this.csReviewDateMonth = csReviewDateMonth;
    }

    public String getCsReviewDateYear() {
        return csReviewDateYear;
    }

    public void setCsReviewDateYear(String csReviewDateYear) {
        this.csReviewDateYear = csReviewDateYear;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
