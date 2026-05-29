/*
 *    Copyright IBM Corp. 2023
 *
 *    Migrated from COBOL copybook DELCUS.cpy (DFHCOMMAREA layout for DELCUS program)
 */
package com.ibm.cics.cip.bank.delcus;

/**
 * Represents the COMMAREA used by the DELCUS COBOL program.
 * Maps the DELCUS.cpy copybook fields.
 * <p>
 * This is a mutable data-transfer object because the COBOL COMMAREA
 * is populated progressively during execution (customer data is written
 * back to the caller on successful deletion).
 */
public final class DeleteCustomerCommarea {

    private String eyeCatcher;
    private String sortCode;
    private String customerNumber;
    private String name;
    private String address;
    private String birthDay;
    private String birthMonth;
    private String birthYear;
    private int creditScore;
    private String csReviewDay;
    private String csReviewMonth;
    private String csReviewYear;
    private boolean deleteSuccess;
    private String deleteFailCode;

    public DeleteCustomerCommarea() {
        this.deleteSuccess = false;
        this.deleteFailCode = " ";
    }

    public String getEyeCatcher() {
        return eyeCatcher;
    }

    public void setEyeCatcher(String eyeCatcher) {
        this.eyeCatcher = eyeCatcher;
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

    public String getBirthDay() {
        return birthDay;
    }

    public void setBirthDay(String birthDay) {
        this.birthDay = birthDay;
    }

    public String getBirthMonth() {
        return birthMonth;
    }

    public void setBirthMonth(String birthMonth) {
        this.birthMonth = birthMonth;
    }

    public String getBirthYear() {
        return birthYear;
    }

    public void setBirthYear(String birthYear) {
        this.birthYear = birthYear;
    }

    public int getCreditScore() {
        return creditScore;
    }

    public void setCreditScore(int creditScore) {
        this.creditScore = creditScore;
    }

    public String getCsReviewDay() {
        return csReviewDay;
    }

    public void setCsReviewDay(String csReviewDay) {
        this.csReviewDay = csReviewDay;
    }

    public String getCsReviewMonth() {
        return csReviewMonth;
    }

    public void setCsReviewMonth(String csReviewMonth) {
        this.csReviewMonth = csReviewMonth;
    }

    public String getCsReviewYear() {
        return csReviewYear;
    }

    public void setCsReviewYear(String csReviewYear) {
        this.csReviewYear = csReviewYear;
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
