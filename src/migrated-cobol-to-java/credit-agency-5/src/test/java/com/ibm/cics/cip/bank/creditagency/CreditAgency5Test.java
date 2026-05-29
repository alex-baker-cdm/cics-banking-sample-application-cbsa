/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.creditagency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for CreditAgency5, the Java migration of CRDTAGY5.cbl.
 */
class CreditAgency5Test {

    private CreditAgencyRequest sampleRequest;
    private CreditAgency5.DelayService noOpDelay;

    @BeforeEach
    void setUp() {
        sampleRequest = new CreditAgencyRequest(
                "CRDA", 987654, 1234567890L,
                "John Smith",
                "123 Main Street, London",
                15061985, 0, 20230615, "Y", " "
        );
        noOpDelay = seconds -> {};
    }

    @Nested
    @DisplayName("processRequest - main business logic")
    class ProcessRequestTests {

        @Test
        @DisplayName("should generate a credit score between 1 and 999")
        void shouldGenerateCreditScoreInRange() {
            CreditAgency5 agency = new CreditAgency5(new Random(42), noOpDelay);

            CreditAgencyRequest result = agency.processRequest(sampleRequest);

            assertTrue(result.getCreditScore() >= CreditAgency5.MIN_CREDIT_SCORE,
                    "Credit score should be >= 1, was: " + result.getCreditScore());
            assertTrue(result.getCreditScore() <= CreditAgency5.MAX_CREDIT_SCORE,
                    "Credit score should be <= 999, was: " + result.getCreditScore());
        }

        @Test
        @DisplayName("should update the credit score in the request object")
        void shouldUpdateCreditScoreInRequest() {
            CreditAgency5 agency = new CreditAgency5(new Random(12345), noOpDelay);

            assertEquals(0, sampleRequest.getCreditScore());
            agency.processRequest(sampleRequest);
            assertNotEquals(0, sampleRequest.getCreditScore());
        }

        @Test
        @DisplayName("should preserve all other fields in the request")
        void shouldPreserveOtherFields() {
            CreditAgency5 agency = new CreditAgency5(new Random(99), noOpDelay);

            agency.processRequest(sampleRequest);

            assertEquals("CRDA", sampleRequest.getEyecatcher());
            assertEquals(987654, sampleRequest.getSortCode());
            assertEquals(1234567890L, sampleRequest.getAccountNumber());
            assertEquals("John Smith", sampleRequest.getCustomerName());
            assertEquals("123 Main Street, London", sampleRequest.getCustomerAddress());
            assertEquals(15061985, sampleRequest.getDateOfBirth());
            assertEquals(20230615, sampleRequest.getCreditScoreReviewDate());
            assertEquals("Y", sampleRequest.getSuccessFlag());
            assertEquals(" ", sampleRequest.getFailCode());
        }

        @Test
        @DisplayName("should return the same request object reference")
        void shouldReturnSameRequestReference() {
            CreditAgency5 agency = new CreditAgency5(new Random(1), noOpDelay);

            CreditAgencyRequest result = agency.processRequest(sampleRequest);

            assertSame(sampleRequest, result);
        }

        @RepeatedTest(100)
        @DisplayName("credit score should always be within valid range (stress test)")
        void creditScoreShouldAlwaysBeInRange() {
            long seed = System.nanoTime();
            CreditAgency5 agency = new CreditAgency5(new Random(seed), noOpDelay);

            CreditAgencyRequest result = agency.processRequest(sampleRequest);

            assertTrue(result.getCreditScore() >= 1 && result.getCreditScore() <= 999,
                    "Score out of range: " + result.getCreditScore() + " (seed=" + seed + ")");
        }
    }

    @Nested
    @DisplayName("computeDelay - random delay generation")
    class ComputeDelayTests {

        @RepeatedTest(100)
        @DisplayName("delay should be between 1 and 3 seconds")
        void delayShouldBeInRange() {
            CreditAgency5 agency = new CreditAgency5(
                    new Random(System.nanoTime()), noOpDelay);

            int delay = agency.computeDelay();

            assertTrue(delay >= CreditAgency5.MIN_DELAY_SECONDS,
                    "Delay should be >= 1, was: " + delay);
            assertTrue(delay <= CreditAgency5.MAX_DELAY_SECONDS,
                    "Delay should be <= 3, was: " + delay);
        }

        @Test
        @DisplayName("should use deterministic output with fixed seed")
        void shouldBeDeterministicWithFixedSeed() {
            CreditAgency5 agency1 = new CreditAgency5(new Random(42), noOpDelay);
            CreditAgency5 agency2 = new CreditAgency5(new Random(42), noOpDelay);

            assertEquals(agency1.computeDelay(), agency2.computeDelay());
        }
    }

    @Nested
    @DisplayName("generateCreditScore - credit score generation")
    class GenerateCreditScoreTests {

        @RepeatedTest(100)
        @DisplayName("score should be between 1 and 999")
        void scoreShouldBeInRange() {
            CreditAgency5 agency = new CreditAgency5(
                    new Random(System.nanoTime()), noOpDelay);
            agency.computeDelay();

            int score = agency.generateCreditScore();

            assertTrue(score >= 1 && score <= 999,
                    "Score out of range: " + score);
        }

        @Test
        @DisplayName("should produce deterministic scores with fixed seed")
        void shouldBeDeterministicWithFixedSeed() {
            CreditAgency5 agency1 = new CreditAgency5(new Random(42), noOpDelay);
            CreditAgency5 agency2 = new CreditAgency5(new Random(42), noOpDelay);

            agency1.computeDelay();
            agency2.computeDelay();

            assertEquals(agency1.generateCreditScore(), agency2.generateCreditScore());
        }

        @ParameterizedTest
        @ValueSource(longs = {1L, 100L, 999L, 12345L, Long.MAX_VALUE - 1})
        @DisplayName("should work with various seeds")
        void shouldWorkWithVariousSeeds(long seed) {
            CreditAgency5 agency = new CreditAgency5(new Random(seed), noOpDelay);
            agency.computeDelay();

            int score = agency.generateCreditScore();

            assertTrue(score >= 1 && score <= 999);
        }
    }

    @Nested
    @DisplayName("performDelay - delay execution")
    class PerformDelayTests {

        @Test
        @DisplayName("should invoke delay service with correct seconds")
        void shouldInvokeDelayServiceWithCorrectSeconds() {
            AtomicInteger delayedSeconds = new AtomicInteger(0);
            CreditAgency5.DelayService capturingDelay = delayedSeconds::set;
            CreditAgency5 agency = new CreditAgency5(new Random(42), capturingDelay);

            int expectedDelay = agency.computeDelay();

            CreditAgency5 agency2 = new CreditAgency5(new Random(42), capturingDelay);
            agency2.processRequest(sampleRequest);

            assertEquals(expectedDelay, delayedSeconds.get());
        }

        @Test
        @DisplayName("should throw CreditAgencyException when delay is interrupted")
        void shouldThrowOnInterruption() {
            CreditAgency5.DelayService failingDelay = seconds -> {
                throw new InterruptedException("Simulated interruption");
            };
            CreditAgency5 agency = new CreditAgency5(new Random(42), failingDelay);

            CreditAgencyException exception = assertThrows(
                    CreditAgencyException.class,
                    () -> agency.processRequest(sampleRequest)
            );

            assertEquals(CreditAgency5.ABEND_CODE, exception.getAbendCode());
            assertNotNull(exception.getAbendInfo());
            assertEquals("PLOP", exception.getAbendInfo().getCode());
            assertEquals("CRDTAGY5", exception.getAbendInfo().getProgram());
            assertEquals(0, exception.getAbendInfo().getSqlCode());
            assertTrue(exception.getMessage().contains("delay messed up"));
        }

        @Test
        @DisplayName("should set interrupted flag on thread when delay fails")
        void shouldSetInterruptedFlagOnFailure() {
            CreditAgency5.DelayService failingDelay = seconds -> {
                throw new InterruptedException("Simulated interruption");
            };
            CreditAgency5 agency = new CreditAgency5(new Random(42), failingDelay);

            assertThrows(CreditAgencyException.class,
                    () -> agency.processRequest(sampleRequest));

            assertTrue(Thread.currentThread().isInterrupted());
            Thread.interrupted();
        }

        @Test
        @DisplayName("abendInfo should contain date and time information")
        void abendInfoShouldContainDateAndTime() {
            CreditAgency5.DelayService failingDelay = seconds -> {
                throw new InterruptedException("Simulated interruption");
            };
            CreditAgency5 agency = new CreditAgency5(new Random(42), failingDelay);

            CreditAgencyException exception = assertThrows(
                    CreditAgencyException.class,
                    () -> agency.processRequest(sampleRequest)
            );

            AbendInfo info = exception.getAbendInfo();
            assertNotNull(info.getDate());
            assertNotNull(info.getTime());
            assertTrue(info.getDate().matches("\\d{2}/\\d{2}/\\d{4}"));
            assertTrue(info.getTime().matches("\\d{2}:\\d{2}:\\d{2}"));
        }
    }

    @Nested
    @DisplayName("Constructor - seed-based initialization")
    class ConstructorTests {

        @Test
        @DisplayName("should create agency with long seed (equivalent to EIBTASKN)")
        void shouldCreateWithLongSeed() {
            CreditAgency5 agency = new CreditAgency5(12345L);
            assertNotNull(agency);
        }

        @ParameterizedTest
        @ValueSource(longs = {0L, 1L, -1L, Long.MAX_VALUE, Long.MIN_VALUE})
        @DisplayName("should handle edge case seeds")
        void shouldHandleEdgeCaseSeeds(long seed) {
            CreditAgency5 agency = new CreditAgency5(seed);
            CreditAgencyRequest result = agency.processRequest(sampleRequest);
            assertTrue(result.getCreditScore() >= 1 && result.getCreditScore() <= 999);
        }
    }

    @Nested
    @DisplayName("Constants - verifying COBOL equivalents")
    class ConstantsTests {

        @Test
        @DisplayName("container name should match COBOL value")
        void containerNameShouldMatchCobol() {
            assertEquals("CIPE", CreditAgency5.CONTAINER_NAME);
        }

        @Test
        @DisplayName("channel name should match COBOL value")
        void channelNameShouldMatchCobol() {
            assertEquals("CIPCREDCHANN", CreditAgency5.CHANNEL_NAME);
        }

        @Test
        @DisplayName("abend code should match COBOL value")
        void abendCodeShouldMatchCobol() {
            assertEquals("PLOP", CreditAgency5.ABEND_CODE);
        }

        @Test
        @DisplayName("program name should match COBOL program ID")
        void programNameShouldMatchCobol() {
            assertEquals("CRDTAGY5", CreditAgency5.PROGRAM_NAME);
        }

        @Test
        @DisplayName("sort code constant should match SORTCODE.cpy value")
        void sortCodeShouldMatchCopybook() {
            assertEquals(987654, CreditAgency5.SORT_CODE);
        }
    }

    @Nested
    @DisplayName("Integration - full end-to-end flow")
    class IntegrationTests {

        @Test
        @DisplayName("should complete full processing cycle with realistic data")
        void shouldCompleteFullCycle() {
            CreditAgencyRequest request = new CreditAgencyRequest(
                    "CRDA", 987654, 9999999999L,
                    "Jane Doe",
                    "456 High Street, Manchester, M1 1AA",
                    25121990, 0, 20230101, " ", " "
            );

            CreditAgency5 agency = new CreditAgency5(new Random(777), noOpDelay);
            CreditAgencyRequest result = agency.processRequest(request);

            assertTrue(result.getCreditScore() >= 1);
            assertTrue(result.getCreditScore() <= 999);
            assertEquals("Jane Doe", result.getCustomerName());
            assertEquals(987654, result.getSortCode());
        }

        @Test
        @DisplayName("should handle maximum boundary values for account data")
        void shouldHandleMaxBoundaryValues() {
            CreditAgencyRequest request = new CreditAgencyRequest(
                    "CRDA", 999999, 9999999999L,
                    "A".repeat(60),
                    "B".repeat(160),
                    31122999, 999, 99999999, "Y", "X"
            );

            CreditAgency5 agency = new CreditAgency5(new Random(1), noOpDelay);
            CreditAgencyRequest result = agency.processRequest(request);

            assertTrue(result.getCreditScore() >= 1 && result.getCreditScore() <= 999);
        }

        @Test
        @DisplayName("should handle minimum boundary values for account data")
        void shouldHandleMinBoundaryValues() {
            CreditAgencyRequest request = new CreditAgencyRequest(
                    "    ", 0, 0L,
                    "", "", 1011900, 0, 0, " ", " "
            );

            CreditAgency5 agency = new CreditAgency5(new Random(1), noOpDelay);
            CreditAgencyRequest result = agency.processRequest(request);

            assertTrue(result.getCreditScore() >= 1 && result.getCreditScore() <= 999);
        }

        @Test
        @DisplayName("multiple sequential calls should produce different scores")
        void multipleCallsShouldProduceDifferentScores() {
            CreditAgency5 agency = new CreditAgency5(new Random(42), noOpDelay);

            int score1 = agency.processRequest(createFreshRequest()).getCreditScore();
            int score2 = agency.processRequest(createFreshRequest()).getCreditScore();
            int score3 = agency.processRequest(createFreshRequest()).getCreditScore();

            assertFalse(score1 == score2 && score2 == score3,
                    "Three sequential scores should not all be identical");
        }

        private CreditAgencyRequest createFreshRequest() {
            return new CreditAgencyRequest(
                    "CRDA", 987654, 1234567890L,
                    "Test User", "Test Address",
                    15061985, 0, 20230615, "Y", " "
            );
        }
    }
}
