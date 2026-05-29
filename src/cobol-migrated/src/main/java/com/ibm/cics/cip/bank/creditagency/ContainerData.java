/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.creditagency;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Data structure representing the CICS container record used by the credit
 * check agency programs (CRDTAGY1–5).
 *
 * Maps to the COBOL copybook WS-CONT-IN:
 * <pre>
 *   01 WS-CONT-IN.
 *      03 WS-CONT-IN-EYECATCHER      PIC X(4).
 *      03 WS-CONT-IN-KEY.
 *         05 WS-CONT-IN-SORTCODE     PIC 9(6).
 *         05 WS-CONT-IN-NUMBER       PIC 9(10).
 *      03 WS-CONT-IN-NAME            PIC X(60).
 *      03 WS-CONT-IN-ADDRESS         PIC X(160).
 *      03 WS-CONT-IN-DATE-OF-BIRTH   PIC 9(8).
 *      03 WS-CONT-IN-CREDIT-SCORE    PIC 999.
 *      03 WS-CONT-IN-CS-REVIEW-DATE  PIC 9(8).
 *      03 WS-CONT-IN-SUCCESS         PIC X.
 *      03 WS-CONT-IN-FAIL-CODE       PIC X.
 * </pre>
 */
public class ContainerData {

    private String eyecatcher;
    private String sortCode;
    private String accountNumber;
    private String customerName;
    private String customerAddress;
    private LocalDate dateOfBirth;
    private int creditScore;
    private LocalDate csReviewDate;
    private String successFlag;
    private String failCode;

    public ContainerData() {
    }

    public ContainerData(String eyecatcher, String sortCode,
            String accountNumber, String customerName,
            String customerAddress, LocalDate dateOfBirth,
            int creditScore, LocalDate csReviewDate,
            String successFlag, String failCode) {
        this.eyecatcher = eyecatcher;
        this.sortCode = sortCode;
        this.accountNumber = accountNumber;
        this.customerName = customerName;
        this.customerAddress = customerAddress;
        this.dateOfBirth = dateOfBirth;
        this.creditScore = creditScore;
        this.csReviewDate = csReviewDate;
        this.successFlag = successFlag;
        this.failCode = failCode;
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

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
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

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public int getCreditScore() {
        return creditScore;
    }

    public void setCreditScore(int creditScore) {
        this.creditScore = creditScore;
    }

    public LocalDate getCsReviewDate() {
        return csReviewDate;
    }

    public void setCsReviewDate(LocalDate csReviewDate) {
        this.csReviewDate = csReviewDate;
    }

    public String getSuccessFlag() {
        return successFlag;
    }

    public void setSuccessFlag(String successFlag) {
        this.successFlag = successFlag;
    }

    public String getFailCode() {
        return failCode;
    }

    public void setFailCode(String failCode) {
        this.failCode = failCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ContainerData that = (ContainerData) o;
        return creditScore == that.creditScore
                && Objects.equals(eyecatcher, that.eyecatcher)
                && Objects.equals(sortCode, that.sortCode)
                && Objects.equals(accountNumber, that.accountNumber)
                && Objects.equals(customerName, that.customerName)
                && Objects.equals(customerAddress, that.customerAddress)
                && Objects.equals(dateOfBirth, that.dateOfBirth)
                && Objects.equals(csReviewDate, that.csReviewDate)
                && Objects.equals(successFlag, that.successFlag)
                && Objects.equals(failCode, that.failCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eyecatcher, sortCode, accountNumber, customerName,
                customerAddress, dateOfBirth, creditScore, csReviewDate,
                successFlag, failCode);
    }

    @Override
    public String toString() {
        return "ContainerData{"
                + "eyecatcher='" + eyecatcher + '\''
                + ", sortCode='" + sortCode + '\''
                + ", accountNumber='" + accountNumber + '\''
                + ", customerName='" + customerName + '\''
                + ", creditScore=" + creditScore
                + ", successFlag='" + successFlag + '\''
                + ", failCode='" + failCode + '\''
                + '}';
    }
}
