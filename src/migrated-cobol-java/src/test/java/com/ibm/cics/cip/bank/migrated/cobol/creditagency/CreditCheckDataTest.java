/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.migrated.cobol.creditagency;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

class CreditCheckDataTest {

    private static CreditCheckData sampleData() {
        return new CreditCheckData(
                "CRDA",
                987654,
                1234567890L,
                "Jane Doe",
                "456 Oak Avenue, Metropolis",
                15061985,
                750,
                25122025,
                "Y",
                " "
        );
    }

    @Nested
    @DisplayName("Constants")
    class Constants {

        @Test
        @DisplayName("container name is CIPB (agency 2)")
        void containerName() {
            assertEquals("CIPB", CreditCheckData.CONTAINER_NAME);
        }

        @Test
        @DisplayName("channel name is CIPCREDCHANN")
        void channelName() {
            assertEquals("CIPCREDCHANN", CreditCheckData.CHANNEL_NAME);
        }

        @Test
        @DisplayName("field length constants match COBOL PIC clauses")
        void fieldLengths() {
            assertEquals(4, CreditCheckData.EYECATCHER_LENGTH);
            assertEquals(6, CreditCheckData.SORT_CODE_MAX_DIGITS);
            assertEquals(10, CreditCheckData.ACCOUNT_NUMBER_MAX_DIGITS);
            assertEquals(60, CreditCheckData.NAME_MAX_LENGTH);
            assertEquals(160, CreditCheckData.ADDRESS_MAX_LENGTH);
            assertEquals(1, CreditCheckData.MIN_CREDIT_SCORE);
            assertEquals(999, CreditCheckData.MAX_CREDIT_SCORE);
        }
    }

    @Nested
    @DisplayName("Record accessors")
    class RecordAccessors {

        @Test
        @DisplayName("all fields are accessible")
        void allFieldsAccessible() {
            CreditCheckData data = sampleData();

            assertEquals("CRDA", data.eyecatcher());
            assertEquals(987654, data.sortCode());
            assertEquals(1234567890L, data.accountNumber());
            assertEquals("Jane Doe", data.name());
            assertEquals("456 Oak Avenue, Metropolis", data.address());
            assertEquals(15061985, data.dateOfBirth());
            assertEquals(750, data.creditScore());
            assertEquals(25122025, data.csReviewDate());
            assertEquals("Y", data.success());
            assertEquals(" ", data.failCode());
        }
    }

    @Nested
    @DisplayName("withCreditScore")
    class WithCreditScore {

        @Test
        @DisplayName("returns new record with updated credit score")
        void updatedCreditScore() {
            CreditCheckData original = sampleData();
            CreditCheckData updated = original.withCreditScore(500);

            assertEquals(500, updated.creditScore());
            assertNotEquals(original.creditScore(), updated.creditScore());
        }

        @Test
        @DisplayName("preserves all other fields")
        void preservesOtherFields() {
            CreditCheckData original = sampleData();
            CreditCheckData updated = original.withCreditScore(500);

            assertEquals(original.eyecatcher(), updated.eyecatcher());
            assertEquals(original.sortCode(), updated.sortCode());
            assertEquals(original.accountNumber(), updated.accountNumber());
            assertEquals(original.name(), updated.name());
            assertEquals(original.address(), updated.address());
            assertEquals(original.dateOfBirth(), updated.dateOfBirth());
            assertEquals(original.csReviewDate(), updated.csReviewDate());
            assertEquals(original.success(), updated.success());
            assertEquals(original.failCode(), updated.failCode());
        }

        @Test
        @DisplayName("original record is not modified (immutability)")
        void originalUnchanged() {
            CreditCheckData original = sampleData();
            int originalScore = original.creditScore();
            original.withCreditScore(999);

            assertEquals(originalScore, original.creditScore());
        }
    }

    @Nested
    @DisplayName("Date of birth parsing (DDMMYYYY)")
    class DateOfBirthParsing {

        @Test
        @DisplayName("parses day from DDMMYYYY format")
        void birthDay() {
            CreditCheckData data = sampleData();
            assertEquals(15, data.birthDay());
        }

        @Test
        @DisplayName("parses month from DDMMYYYY format")
        void birthMonth() {
            CreditCheckData data = sampleData();
            assertEquals(6, data.birthMonth());
        }

        @Test
        @DisplayName("parses year from DDMMYYYY format")
        void birthYear() {
            CreditCheckData data = sampleData();
            assertEquals(1985, data.birthYear());
        }

        @Test
        @DisplayName("handles first day of year")
        void firstDayOfYear() {
            CreditCheckData data = new CreditCheckData(
                    "TEST", 0, 0L, "", "", 1012000,
                    0, 0, "", "");
            assertEquals(1, data.birthDay());
            assertEquals(1, data.birthMonth());
            assertEquals(2000, data.birthYear());
        }

        @Test
        @DisplayName("handles last day of year")
        void lastDayOfYear() {
            CreditCheckData data = new CreditCheckData(
                    "TEST", 0, 0L, "", "", 31121999,
                    0, 0, "", "");
            assertEquals(31, data.birthDay());
            assertEquals(12, data.birthMonth());
            assertEquals(1999, data.birthYear());
        }
    }

    @Nested
    @DisplayName("Review date parsing (DDMMYYYY)")
    class ReviewDateParsing {

        @Test
        @DisplayName("parses review day")
        void reviewDay() {
            CreditCheckData data = sampleData();
            assertEquals(25, data.reviewDay());
        }

        @Test
        @DisplayName("parses review month")
        void reviewMonth() {
            CreditCheckData data = sampleData();
            assertEquals(12, data.reviewMonth());
        }

        @Test
        @DisplayName("parses review year")
        void reviewYear() {
            CreditCheckData data = sampleData();
            assertEquals(2025, data.reviewYear());
        }
    }

    @Nested
    @DisplayName("Record equality and toString")
    class RecordEquality {

        @Test
        @DisplayName("two records with same values are equal")
        void equalRecords() {
            CreditCheckData a = sampleData();
            CreditCheckData b = sampleData();
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("records with different credit scores are not equal")
        void unequalRecords() {
            CreditCheckData a = sampleData();
            CreditCheckData b = a.withCreditScore(1);
            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("toString contains field values")
        void toStringContainsFields() {
            String str = sampleData().toString();
            assertTrue(str.contains("987654"));
            assertTrue(str.contains("Jane Doe"));
        }
    }
}
