/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.creditagency;

/**
 * Data class representing the container input/output structure for the
 * credit agency program. This is the Java equivalent of the COBOL
 * WS-CONT-IN record structure used in CRDTAGY5.cbl.
 *
 * <p>COBOL layout:
 * <pre>
 *   01 WS-CONT-IN.
 *      03 WS-CONT-IN-EYECATCHER      PIC X(4).
 *      03 WS-CONT-IN-KEY.
 *         05 WS-CONT-IN-SORTCODE     PIC 9(6) DISPLAY.
 *         05 WS-CONT-IN-NUMBER       PIC 9(10) DISPLAY.
 *      03 WS-CONT-IN-NAME            PIC X(60).
 *      03 WS-CONT-IN-ADDRESS         PIC X(160).
 *      03 WS-CONT-IN-DATE-OF-BIRTH   PIC 9(8).
 *      03 WS-CONT-IN-CREDIT-SCORE    PIC 999.
 *      03 WS-CONT-IN-CS-REVIEW-DATE  PIC 9(8).
 *      03 WS-CONT-IN-SUCCESS         PIC X.
 *      03 WS-CONT-IN-FAIL-CODE       PIC X.
 * </pre>
 */
public class CreditAgencyRequest {

    private String eyecatcher;
    private int sortCode;
    private long accountNumber;
    private String customerName;
    private String customerAddress;
    private int dateOfBirth;
    private int birthDay;
    private int birthMonth;
    private int birthYear;
    private int creditScore;
    private int creditScoreReviewDate;
    private String successFlag;
    private String failCode;

    public CreditAgencyRequest() {
    }

    public CreditAgencyRequest(String eyecatcher, int sortCode, long accountNumber,
                               String customerName, String customerAddress,
                               int dateOfBirth, int creditScore,
                               int creditScoreReviewDate, String successFlag,
                               String failCode) {
        this.eyecatcher = eyecatcher;
        this.sortCode = sortCode;
        this.accountNumber = accountNumber;
        this.customerName = customerName;
        this.customerAddress = customerAddress;
        setDateOfBirth(dateOfBirth);
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

    public int getSortCode() {
        return sortCode;
    }

    public void setSortCode(int sortCode) {
        this.sortCode = sortCode;
    }

    public long getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(long accountNumber) {
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

    public int getDateOfBirth() {
        return dateOfBirth;
    }

    /**
     * Sets the date of birth as an 8-digit integer (DDMMYYYY format).
     * Also decomposes into day, month, year components matching the
     * COBOL REDEFINES group WS-CONT-IN-DOB-GROUP.
     */
    public void setDateOfBirth(int dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
        this.birthDay = dateOfBirth / 1000000;
        this.birthMonth = (dateOfBirth / 10000) % 100;
        this.birthYear = dateOfBirth % 10000;
    }

    public int getBirthDay() {
        return birthDay;
    }

    public int getBirthMonth() {
        return birthMonth;
    }

    public int getBirthYear() {
        return birthYear;
    }

    public int getCreditScore() {
        return creditScore;
    }

    public void setCreditScore(int creditScore) {
        this.creditScore = creditScore;
    }

    public int getCreditScoreReviewDate() {
        return creditScoreReviewDate;
    }

    public void setCreditScoreReviewDate(int creditScoreReviewDate) {
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
    public String toString() {
        return "CreditAgencyRequest{" +
                "eyecatcher='" + eyecatcher + '\'' +
                ", sortCode=" + sortCode +
                ", accountNumber=" + accountNumber +
                ", customerName='" + customerName + '\'' +
                ", dateOfBirth=" + dateOfBirth +
                ", creditScore=" + creditScore +
                ", creditScoreReviewDate=" + creditScoreReviewDate +
                ", successFlag='" + successFlag + '\'' +
                ", failCode='" + failCode + '\'' +
                '}';
    }
}
