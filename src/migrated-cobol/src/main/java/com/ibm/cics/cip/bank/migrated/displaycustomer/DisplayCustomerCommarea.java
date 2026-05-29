package com.ibm.cics.cip.bank.migrated.displaycustomer;

/**
 * Communication area passed between CICS pseudo-conversational interactions.
 * Migrated from: WS-COMM-AREA / DFHCOMMAREA in BNK1DCS.cbl
 */
public class DisplayCustomerCommarea {

    private int terminalUctrans;
    private String eye;
    private String sortCode;
    private String customerNumber;
    private String name;
    private String address;
    private int dob;
    private int creditScore;
    private int csReviewDate;
    private String deleteSuccess;
    private String deleteFailCode;
    private String updateFlag;

    public DisplayCustomerCommarea() {
        initialize();
    }

    public void initialize() {
        this.terminalUctrans = 0;
        this.eye = "";
        this.sortCode = "";
        this.customerNumber = "";
        this.name = "";
        this.address = "";
        this.dob = 0;
        this.creditScore = 0;
        this.csReviewDate = 0;
        this.deleteSuccess = "";
        this.deleteFailCode = "";
        this.updateFlag = "";
    }

    public int getTerminalUctrans() {
        return terminalUctrans;
    }

    public void setTerminalUctrans(int terminalUctrans) {
        this.terminalUctrans = terminalUctrans;
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

    public String getDeleteSuccess() {
        return deleteSuccess;
    }

    public void setDeleteSuccess(String deleteSuccess) {
        this.deleteSuccess = deleteSuccess;
    }

    public String getDeleteFailCode() {
        return deleteFailCode;
    }

    public void setDeleteFailCode(String deleteFailCode) {
        this.deleteFailCode = deleteFailCode;
    }

    public String getUpdateFlag() {
        return updateFlag;
    }

    public void setUpdateFlag(String updateFlag) {
        this.updateFlag = updateFlag;
    }
}
