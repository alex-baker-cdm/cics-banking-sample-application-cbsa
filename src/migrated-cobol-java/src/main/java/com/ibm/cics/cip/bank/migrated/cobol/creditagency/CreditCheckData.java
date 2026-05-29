/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.migrated.cobol.creditagency;

/**
 * Immutable data record representing the container data structure passed to
 * credit agency programs via CICS channels. Migrated from the COBOL copybook
 * WS-CONT-IN used in CRDTAGY2.cbl.
 *
 * <p>Field layout matches the original COBOL structure:
 * <pre>
 * 01 WS-CONT-IN.
 *    03 WS-CONT-IN-EYECATCHER      PIC X(4).
 *    03 WS-CONT-IN-KEY.
 *       05 WS-CONT-IN-SORTCODE     PIC 9(6).
 *       05 WS-CONT-IN-NUMBER       PIC 9(10).
 *    03 WS-CONT-IN-NAME            PIC X(60).
 *    03 WS-CONT-IN-ADDRESS         PIC X(160).
 *    03 WS-CONT-IN-DATE-OF-BIRTH   PIC 9(8).
 *    03 WS-CONT-IN-CREDIT-SCORE    PIC 999.
 *    03 WS-CONT-IN-CS-REVIEW-DATE  PIC 9(8).
 *    03 WS-CONT-IN-SUCCESS         PIC X.
 *    03 WS-CONT-IN-FAIL-CODE       PIC X.
 * </pre>
 *
 * @param eyecatcher     4-character eyecatcher identifier
 * @param sortCode       6-digit bank sort code
 * @param accountNumber  10-digit account number
 * @param name           customer name (max 60 characters)
 * @param address        customer address (max 160 characters)
 * @param dateOfBirth    date of birth as DDMMYYYY integer
 * @param creditScore    credit score (1-999)
 * @param csReviewDate   credit score review date as DDMMYYYY integer
 * @param success        single-character success indicator
 * @param failCode       single-character failure code
 */
public record CreditCheckData(
        String eyecatcher,
        int sortCode,
        long accountNumber,
        String name,
        String address,
        int dateOfBirth,
        int creditScore,
        int csReviewDate,
        String success,
        String failCode
) {

    public static final String CONTAINER_NAME = "CIPB";
    public static final String CHANNEL_NAME = "CIPCREDCHANN";

    public static final int EYECATCHER_LENGTH = 4;
    public static final int SORT_CODE_MAX_DIGITS = 6;
    public static final int ACCOUNT_NUMBER_MAX_DIGITS = 10;
    public static final int NAME_MAX_LENGTH = 60;
    public static final int ADDRESS_MAX_LENGTH = 160;
    public static final int MIN_CREDIT_SCORE = 1;
    public static final int MAX_CREDIT_SCORE = 999;

    public CreditCheckData withCreditScore(int newCreditScore) {
        return new CreditCheckData(
                eyecatcher, sortCode, accountNumber, name, address,
                dateOfBirth, newCreditScore, csReviewDate, success, failCode);
    }

    public int birthDay() {
        return dateOfBirth / 1_000_000;
    }

    public int birthMonth() {
        return (dateOfBirth / 10_000) % 100;
    }

    public int birthYear() {
        return dateOfBirth % 10_000;
    }

    public int reviewDay() {
        return csReviewDate / 1_000_000;
    }

    public int reviewMonth() {
        return (csReviewDate / 10_000) % 100;
    }

    public int reviewYear() {
        return csReviewDate % 10_000;
    }
}
