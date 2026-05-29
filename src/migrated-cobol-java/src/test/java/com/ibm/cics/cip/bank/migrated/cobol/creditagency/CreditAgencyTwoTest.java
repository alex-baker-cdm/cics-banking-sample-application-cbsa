/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.migrated.cobol.creditagency;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;

import static org.junit.jupiter.api.Assertions.*;

class CreditAgencyTwoTest {

    private static final long SAMPLE_TASK_NUMBER = 12345L;

    private static CreditCheckData sampleInput() {
        return new CreditCheckData(
                "CRDA",
                987654,
                1234567890L,
                "John Smith",
                "123 Main Street, Springfield",
                15061985,
                0,
                25122025,
                " ",
                " "
        );
    }

    private static CreditAgencyTwo agencyWithNoDelay() {
        return new CreditAgencyTwo(seconds -> { });
    }

    @Nested
    @DisplayName("processCreditCheck")
    class ProcessCreditCheck {

        @Test
        @DisplayName("returns data with a generated credit score")
        void returnsCreditScore() {
            CreditAgencyTwo agency = agencyWithNoDelay();
            CreditCheckData result = agency.processCreditCheck(
                    sampleInput(), SAMPLE_TASK_NUMBER);

            assertTrue(result.creditScore() >= CreditAgencyTwo.CREDIT_SCORE_RANGE_MIN,
                    "Credit score should be >= 1, was " + result.creditScore());
            assertTrue(result.creditScore() < CreditAgencyTwo.CREDIT_SCORE_RANGE_MAX,
                    "Credit score should be < 999, was " + result.creditScore());
        }

        @Test
        @DisplayName("preserves all input fields except credit score")
        void preservesInputFields() {
            CreditAgencyTwo agency = agencyWithNoDelay();
            CreditCheckData input = sampleInput();
            CreditCheckData result = agency.processCreditCheck(
                    input, SAMPLE_TASK_NUMBER);

            assertEquals(input.eyecatcher(), result.eyecatcher());
            assertEquals(input.sortCode(), result.sortCode());
            assertEquals(input.accountNumber(), result.accountNumber());
            assertEquals(input.name(), result.name());
            assertEquals(input.address(), result.address());
            assertEquals(input.dateOfBirth(), result.dateOfBirth());
            assertEquals(input.csReviewDate(), result.csReviewDate());
            assertEquals(input.success(), result.success());
            assertEquals(input.failCode(), result.failCode());
            assertNotEquals(0, result.creditScore(),
                    "Credit score should have been updated from 0");
        }

        @Test
        @DisplayName("produces deterministic results for the same task number")
        void deterministicWithSameTaskNumber() {
            CreditAgencyTwo agency = agencyWithNoDelay();
            CreditCheckData input = sampleInput();

            CreditCheckData result1 = agency.processCreditCheck(
                    input, SAMPLE_TASK_NUMBER);
            CreditCheckData result2 = agency.processCreditCheck(
                    input, SAMPLE_TASK_NUMBER);

            assertEquals(result1.creditScore(), result2.creditScore(),
                    "Same task number should produce same credit score");
        }

        @Test
        @DisplayName("produces different results for different task numbers")
        void differentTaskNumbersDifferentScores() {
            CreditAgencyTwo agency = agencyWithNoDelay();
            CreditCheckData input = sampleInput();

            CreditCheckData result1 = agency.processCreditCheck(input, 1L);
            CreditCheckData result2 = agency.processCreditCheck(input, 99999L);

            assertNotEquals(result1.creditScore(), result2.creditScore(),
                    "Different task numbers should (usually) produce "
                    + "different credit scores");
        }

        @Test
        @DisplayName("invokes the delay strategy with computed delay")
        void invokesDelayStrategy() {
            AtomicInteger delayedSeconds = new AtomicInteger(-1);
            CreditAgencyTwo agency = new CreditAgencyTwo(
                    delayedSeconds::set);

            agency.processCreditCheck(sampleInput(), SAMPLE_TASK_NUMBER);

            int delay = delayedSeconds.get();
            assertTrue(delay >= CreditAgencyTwo.DELAY_RANGE_MIN
                            && delay < CreditAgencyTwo.DELAY_RANGE_MAX,
                    "Delay should be in [1, 3), was " + delay);
        }

        @Test
        @DisplayName("throws CreditAgencyException when delay is interrupted")
        void throwsOnInterrupt() {
            CreditAgencyTwo agency = new CreditAgencyTwo(seconds -> {
                throw new InterruptedException("simulated interrupt");
            });

            CreditAgencyException ex = assertThrows(
                    CreditAgencyException.class,
                    () -> agency.processCreditCheck(
                            sampleInput(), SAMPLE_TASK_NUMBER));

            assertTrue(ex.getMessage().contains("delay messed up"),
                    "Exception message should match COBOL abend text");
            assertNotNull(ex.getAbendInfo(),
                    "AbendInfo should be populated on delay failure");
            assertEquals(AbendInfo.DEFAULT_ABEND_CODE,
                    ex.getAbendInfo().abendCode());
            assertEquals(AbendInfo.PROGRAM_NAME,
                    ex.getAbendInfo().program());
            assertTrue(Thread.currentThread().isInterrupted(),
                    "Thread interrupt flag should be preserved");

            Thread.interrupted();
        }

        @Test
        @DisplayName("throws IllegalArgumentException for null input")
        void throwsOnNullInput() {
            CreditAgencyTwo agency = agencyWithNoDelay();

            assertThrows(IllegalArgumentException.class,
                    () -> agency.processCreditCheck(null, SAMPLE_TASK_NUMBER));
        }

        @RepeatedTest(50)
        @DisplayName("credit score stays within valid range across many runs")
        void creditScoreAlwaysInRange() {
            CreditAgencyTwo agency = agencyWithNoDelay();
            long taskNumber = System.nanoTime();

            CreditCheckData result = agency.processCreditCheck(
                    sampleInput(), taskNumber);

            assertTrue(result.creditScore() >= 1,
                    "Credit score must be >= 1, was " + result.creditScore());
            assertTrue(result.creditScore() <= 998,
                    "Credit score must be <= 998, was " + result.creditScore());
        }
    }

    @Nested
    @DisplayName("computeDelay")
    class ComputeDelay {

        @RepeatedTest(100)
        @DisplayName("delay is always 1 or 2 seconds")
        void delayRange() {
            CreditAgencyTwo agency = agencyWithNoDelay();
            Random random = new Random(System.nanoTime());

            int delay = agency.computeDelay(random);

            assertTrue(delay == 1 || delay == 2,
                    "Delay should be 1 or 2, was " + delay);
        }

        @Test
        @DisplayName("delay is deterministic for same seed")
        void delayDeterministic() {
            CreditAgencyTwo agency = agencyWithNoDelay();

            int delay1 = agency.computeDelay(new Random(42L));
            int delay2 = agency.computeDelay(new Random(42L));

            assertEquals(delay1, delay2);
        }
    }

    @Nested
    @DisplayName("computeCreditScore")
    class ComputeCreditScore {

        @RepeatedTest(100)
        @DisplayName("credit score is always in [1, 998]")
        void creditScoreRange() {
            CreditAgencyTwo agency = agencyWithNoDelay();
            Random random = new Random(System.nanoTime());

            int score = agency.computeCreditScore(random);

            assertTrue(score >= 1 && score <= 998,
                    "Credit score should be in [1, 998], was " + score);
        }

        @Test
        @DisplayName("credit score is deterministic for same seed")
        void creditScoreDeterministic() {
            CreditAgencyTwo agency = agencyWithNoDelay();

            int score1 = agency.computeCreditScore(new Random(42L));
            int score2 = agency.computeCreditScore(new Random(42L));

            assertEquals(score1, score2);
        }
    }

    @Nested
    @DisplayName("Default constructor (real delay)")
    class DefaultConstructor {

        @Test
        @DisplayName("can be constructed with default delay strategy")
        void defaultConstructor() {
            CreditAgencyTwo agency = new CreditAgencyTwo();
            assertNotNull(agency);
        }
    }
}
