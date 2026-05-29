/*
 *
 *    Copyright IBM Corp. 2023
 *
 *
 */

package com.ibm.cics.cip.bank.migrated.creditagency;

/**
 * Represents the data passed via CICS channel/container to the credit agency
 * program. This maps to the COBOL WS-CONT-IN structure in CRDTAGY1.cbl.
 *
 * <pre>
 * COBOL layout (WS-CONT-IN):
 *   03 WS-CONT-IN-EYECATCHER      PIC X(4)
 *   03 WS-CONT-IN-KEY
 *      05 WS-CONT-IN-SORTCODE     PIC 9(6)  DISPLAY
 *      05 WS-CONT-IN-NUMBER       PIC 9(10) DISPLAY
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

    public static final int EYECATCHER_LEN = 4;
    public static final int SORTCODE_LEN = 6;
    public static final int NUMBER_LEN = 10;
    public static final int NAME_LEN = 60;
    public static final int ADDRESS_LEN = 160;
    public static final int DATE_OF_BIRTH_LEN = 8;
    public static final int CREDIT_SCORE_LEN = 3;
    public static final int CS_REVIEW_DATE_LEN = 8;
    public static final int SUCCESS_LEN = 1;
    public static final int FAIL_CODE_LEN = 1;

    public static final int TOTAL_LENGTH = EYECATCHER_LEN + SORTCODE_LEN
            + NUMBER_LEN + NAME_LEN + ADDRESS_LEN + DATE_OF_BIRTH_LEN
            + CREDIT_SCORE_LEN + CS_REVIEW_DATE_LEN + SUCCESS_LEN
            + FAIL_CODE_LEN;

    public static final int MIN_CREDIT_SCORE = 1;
    public static final int MAX_CREDIT_SCORE = 999;

    private String eyecatcher;
    private String sortCode;
    private String number;
    private String name;
    private String address;
    private String dateOfBirth;
    private int birthDay;
    private int birthMonth;
    private int birthYear;
    private int creditScore;
    private String csReviewDate;
    private String success;
    private String failCode;

    public CreditCheckData() {
        this.eyecatcher = "";
        this.sortCode = "";
        this.number = "";
        this.name = "";
        this.address = "";
        this.dateOfBirth = "";
        this.creditScore = 0;
        this.csReviewDate = "";
        this.success = "";
        this.failCode = "";
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

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
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

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
        if (dateOfBirth != null && dateOfBirth.length() == DATE_OF_BIRTH_LEN) {
            this.birthDay = Integer.parseInt(dateOfBirth.substring(0, 2));
            this.birthMonth = Integer.parseInt(dateOfBirth.substring(2, 4));
            this.birthYear = Integer.parseInt(dateOfBirth.substring(4, 8));
        }
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

    public String getCsReviewDate() {
        return csReviewDate;
    }

    public void setCsReviewDate(String csReviewDate) {
        this.csReviewDate = csReviewDate;
    }

    public String getSuccess() {
        return success;
    }

    public void setSuccess(String success) {
        this.success = success;
    }

    public String getFailCode() {
        return failCode;
    }

    public void setFailCode(String failCode) {
        this.failCode = failCode;
    }

    @Override
    public String toString() {
        return "CreditCheckData["
                + "eyecatcher=" + eyecatcher
                + ", sortCode=" + sortCode
                + ", number=" + number
                + ", name=" + name
                + ", creditScore=" + creditScore
                + ", dateOfBirth=" + dateOfBirth
                + ", csReviewDate=" + csReviewDate
                + ", success=" + success
                + ", failCode=" + failCode
                + "]";
    }
}
