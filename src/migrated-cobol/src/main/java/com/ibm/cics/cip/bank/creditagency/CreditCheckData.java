/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.creditagency;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Data transfer object representing the container data structure used by
 * the credit check agency programs. Maps to the COBOL WS-CONT-IN structure
 * in CRDTAGY3.cbl.
 *
 * <pre>
 * COBOL layout:
 *   03 WS-CONT-IN-EYECATCHER      PIC X(4)
 *   03 WS-CONT-IN-KEY
 *      05 WS-CONT-IN-SORTCODE     PIC 9(6)
 *      05 WS-CONT-IN-NUMBER       PIC 9(10)
 *   03 WS-CONT-IN-NAME            PIC X(60)
 *   03 WS-CONT-IN-ADDRESS         PIC X(160)
 *   03 WS-CONT-IN-DATE-OF-BIRTH   PIC 9(8)
 *   03 WS-CONT-IN-CREDIT-SCORE    PIC 999
 *   03 WS-CONT-IN-CS-REVIEW-DATE  PIC 9(8)
 *   03 WS-CONT-IN-SUCCESS         PIC X
 *   03 WS-CONT-IN-FAIL-CODE       PIC X
 * </pre>
 */
public class CreditCheckData {

    private String eyecatcher;
    private String sortCode;
    private String accountNumber;
    private String customerName;
    private String customerAddress;
    private LocalDate dateOfBirth;
    private int creditScore;
    private LocalDate creditScoreReviewDate;
    private String successFlag;
    private String failCode;

    public CreditCheckData() {
    }

    public CreditCheckData(String eyecatcher, String sortCode, String accountNumber,
                           String customerName, String customerAddress,
                           LocalDate dateOfBirth, int creditScore,
                           LocalDate creditScoreReviewDate, String successFlag,
                           String failCode) {
        this.eyecatcher = eyecatcher;
        this.sortCode = sortCode;
        this.accountNumber = accountNumber;
        this.customerName = customerName;
        this.customerAddress = customerAddress;
        this.dateOfBirth = dateOfBirth;
        this.creditScore = creditScore;
        this.creditScoreReviewDate = creditScoreReviewDate;
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

    public LocalDate getCreditScoreReviewDate() {
        return creditScoreReviewDate;
    }

    public void setCreditScoreReviewDate(LocalDate creditScoreReviewDate) {
        this.creditScoreReviewDate = creditScoreReviewDate;
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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CreditCheckData that = (CreditCheckData) o;
        return creditScore == that.creditScore
                && Objects.equals(eyecatcher, that.eyecatcher)
                && Objects.equals(sortCode, that.sortCode)
                && Objects.equals(accountNumber, that.accountNumber)
                && Objects.equals(customerName, that.customerName)
                && Objects.equals(customerAddress, that.customerAddress)
                && Objects.equals(dateOfBirth, that.dateOfBirth)
                && Objects.equals(creditScoreReviewDate, that.creditScoreReviewDate)
                && Objects.equals(successFlag, that.successFlag)
                && Objects.equals(failCode, that.failCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eyecatcher, sortCode, accountNumber, customerName,
                customerAddress, dateOfBirth, creditScore, creditScoreReviewDate,
                successFlag, failCode);
    }

    @Override
    public String toString() {
        return "CreditCheckData{" +
                "eyecatcher='" + eyecatcher + '\'' +
                ", sortCode='" + sortCode + '\'' +
                ", accountNumber='" + accountNumber + '\'' +
                ", customerName='" + customerName + '\'' +
                ", creditScore=" + creditScore +
                ", successFlag='" + successFlag + '\'' +
                ", failCode='" + failCode + '\'' +
                '}';
    }
}
