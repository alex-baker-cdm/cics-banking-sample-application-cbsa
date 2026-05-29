package com.ibm.cics.cip.bank.migrated.displaycustomer;

/**
 * Corresponds to the UPDCUST COPY structure used by the UPDCUST sub-program.
 * Migrated from: COBOL copybook UPDCUST.cpy
 */
public class UpdCustCommarea {

    private String eye;
    private String sortCode;
    private String customerNumber;
    private String name;
    private String address;
    private int dob;
    private int creditScore;
    private int csReviewDate;
    private String updateSuccess;
    private String updateFailCode;

    public UpdCustCommarea() {
        initialize();
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

    public String getCustomerNumber() {
        return customerNumber;
    }

    public void setCustomerNumber(String customerNumber) {
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

    public int getDob() {
        return dob;
    }

    public void setDob(int dob) {
        this.dob = dob;
    }

    public int getCreditScore() {
        return creditScore;
    }

    public void setCreditScore(int creditScore) {
        this.creditScore = creditScore;
    }

    public int getCsReviewDate() {
        return csReviewDate;
    }

    public void setCsReviewDate(int csReviewDate) {
        this.csReviewDate = csReviewDate;
    }

    public int getCsReviewDay() {
        return csReviewDate / 1000000;
    }

    public int getCsReviewMonth() {
        return (csReviewDate / 10000) % 100;
    }

    public int getCsReviewYear() {
        return csReviewDate % 10000;
    }

    public String getUpdateSuccess() {
        return updateSuccess;
    }

    public void setUpdateSuccess(String updateSuccess) {
        this.updateSuccess = updateSuccess;
    }

    public String getUpdateFailCode() {
        return updateFailCode;
    }

    public void setUpdateFailCode(String updateFailCode) {
        this.updateFailCode = updateFailCode;
    }

    public void initialize() {
        this.eye = "";
        this.sortCode = "";
        this.customerNumber = "";
        this.name = "";
        this.address = "";
        this.dob = 0;
        this.creditScore = 0;
        this.csReviewDate = 0;
        this.updateSuccess = "";
        this.updateFailCode = "";
    }
}
