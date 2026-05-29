/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.creditagency;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for {@link CreditCheckAgency4}, the Java 21 migration
 * of COBOL program CRDTAGY4.
 */
class CreditCheckAgency4Test {

    private static final CreditCheckAgency4.Delayer NO_OP_DELAYER =
            seconds -> { };

    private CreditCheckAgency4 agencyWithSeed(long seed) {
        return new CreditCheckAgency4(new Random(seed), NO_OP_DELAYER);
    }

    private ContainerData sampleRequest() {
        return new ContainerData(
                "EYEC",
                "987654",
                "0000000001",
                "Jane Doe",
                "123 Main Street",
                LocalDate.of(1990, 6, 15),
                0,
                LocalDate.of(2024, 1, 31),
                "Y",
                " "
        );
    }

    @Nested
    @DisplayName("Credit score generation")
    class CreditScoreTests {

        @RepeatedTest(100)
        @DisplayName("Credit score is always within [1, 999]")
        void creditScoreWithinRange() {
            CreditCheckAgency4 agency = new CreditCheckAgency4(
                    new Random(), NO_OP_DELAYER);
            ContainerData result = agency.process(sampleRequest());
            assertTrue(result.getCreditScore() >= 1,
                    "Credit score must be >= 1, was "
                            + result.getCreditScore());
            assertTrue(result.getCreditScore() <= 999,
                    "Credit score must be <= 999, was "
                            + result.getCreditScore());
        }

        @Test
        @DisplayName("Deterministic credit score with fixed seed")
        void deterministicCreditScore() {
            CreditCheckAgency4 agency1 = agencyWithSeed(42L);
            CreditCheckAgency4 agency2 = agencyWithSeed(42L);
            ContainerData request = sampleRequest();

            ContainerData result1 = agency1.process(request);
            ContainerData result2 = agency2.process(request);

            assertEquals(result1.getCreditScore(), result2.getCreditScore(),
                    "Same seed must produce the same credit score");
        }

        @ParameterizedTest
        @ValueSource(longs = {0L, 1L, 42L, 100L, Long.MAX_VALUE})
        @DisplayName("Different seeds produce valid scores")
        void differentSeedsProduceValidScores(long seed) {
            CreditCheckAgency4 agency = agencyWithSeed(seed);
            ContainerData result = agency.process(sampleRequest());
            assertTrue(result.getCreditScore() >= 1
                    && result.getCreditScore() <= 999);
        }

        @Test
        @DisplayName("generateCreditScore produces values in [1, 999]")
        void generateCreditScoreDirectly() {
            CreditCheckAgency4 agency = new CreditCheckAgency4(
                    new Random(), NO_OP_DELAYER);
            for (int i = 0; i < 1000; i++) {
                int score = agency.generateCreditScore();
                assertTrue(score >= 1 && score <= 999,
                        "Score out of range: " + score);
            }
        }
    }

    @Nested
    @DisplayName("Delay generation")
    class DelayTests {

        @RepeatedTest(100)
        @DisplayName("Delay seconds within [1, 3]")
        void delaySecondsWithinRange() {
            CreditCheckAgency4 agency = new CreditCheckAgency4(
                    new Random(), NO_OP_DELAYER);
            int delay = agency.generateDelaySeconds();
            assertTrue(delay >= 1 && delay <= 3,
                    "Delay must be 1–3, was " + delay);
        }

        @Test
        @DisplayName("Delay is actually invoked with correct duration")
        void delayIsInvoked() {
            AtomicInteger capturedDelay = new AtomicInteger(-1);
            CreditCheckAgency4.Delayer capturingDelayer =
                    capturedDelay::set;
            CreditCheckAgency4 agency = new CreditCheckAgency4(
                    new Random(99L), capturingDelayer);

            agency.process(sampleRequest());

            int delay = capturedDelay.get();
            assertTrue(delay >= 1 && delay <= 3,
                    "Captured delay must be 1–3, was " + delay);
        }

        @Test
        @DisplayName("Interrupted delay throws CreditAgencyException")
        void interruptedDelayThrowsException() {
            CreditCheckAgency4.Delayer interruptingDelayer = seconds -> {
                throw new InterruptedException("simulated interrupt");
            };
            CreditCheckAgency4 agency = new CreditCheckAgency4(
                    new Random(1L), interruptingDelayer);

            CreditAgencyException ex = assertThrows(
                    CreditAgencyException.class,
                    () -> agency.process(sampleRequest()));

            assertNotNull(ex.getAbendInfo());
            assertEquals("PLOP", ex.getAbendInfo().abendCode());
            assertEquals("CRDTAGY4", ex.getAbendInfo().program());
            assertTrue(ex.getMessage().contains("Delay interrupted"));
            assertTrue(Thread.currentThread().isInterrupted());
            Thread.interrupted();
        }
    }

    @Nested
    @DisplayName("Container data pass-through")
    class ContainerDataTests {

        @Test
        @DisplayName("All fields except credit score are preserved")
        void fieldsPreserved() {
            CreditCheckAgency4 agency = agencyWithSeed(42L);
            ContainerData request = sampleRequest();
            ContainerData result = agency.process(request);

            assertEquals("EYEC", result.getEyecatcher());
            assertEquals("987654", result.getSortCode());
            assertEquals("0000000001", result.getAccountNumber());
            assertEquals("Jane Doe", result.getCustomerName());
            assertEquals("123 Main Street", result.getCustomerAddress());
            assertEquals(LocalDate.of(1990, 6, 15),
                    result.getDateOfBirth());
            assertEquals(LocalDate.of(2024, 1, 31),
                    result.getCsReviewDate());
            assertEquals("Y", result.getSuccessFlag());
            assertEquals(" ", result.getFailCode());
        }

        @Test
        @DisplayName("Credit score in result differs from input zero")
        void creditScoreUpdated() {
            CreditCheckAgency4 agency = agencyWithSeed(42L);
            ContainerData request = sampleRequest();
            assertEquals(0, request.getCreditScore());

            ContainerData result = agency.process(request);
            assertTrue(result.getCreditScore() >= 1,
                    "Credit score should be set to a positive value");
        }

        @Test
        @DisplayName("Result is a new object, not the same reference")
        void resultIsNewObject() {
            CreditCheckAgency4 agency = agencyWithSeed(42L);
            ContainerData request = sampleRequest();
            ContainerData result = agency.process(request);
            assertNotSame(request, result);
        }
    }

    @Nested
    @DisplayName("Error handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Null request throws CreditAgencyException")
        void nullRequestThrows() {
            CreditCheckAgency4 agency = new CreditCheckAgency4(
                    new Random(), NO_OP_DELAYER);

            CreditAgencyException ex = assertThrows(
                    CreditAgencyException.class,
                    () -> agency.process(null));

            assertTrue(ex.getMessage().contains("null"));
            assertNotNull(ex.getAbendInfo());
        }

        @Test
        @DisplayName("Exception contains AbendInfo with diagnostic data")
        void exceptionContainsAbendInfo() {
            CreditCheckAgency4.Delayer interruptingDelayer = seconds -> {
                throw new InterruptedException("test");
            };
            CreditCheckAgency4 agency = new CreditCheckAgency4(
                    new Random(), interruptingDelayer);

            CreditAgencyException ex = assertThrows(
                    CreditAgencyException.class,
                    () -> agency.process(sampleRequest()));

            AbendInfo info = ex.getAbendInfo();
            assertNotNull(info.date());
            assertNotNull(info.time());
            assertEquals("PLOP", info.abendCode());
            assertEquals("CRDTAGY4", info.program());
            assertEquals(0, info.sqlCode());
            Thread.interrupted();
        }
    }

    @Nested
    @DisplayName("Constants verification")
    class ConstantsTests {

        @Test
        @DisplayName("Container and channel names match COBOL definitions")
        void containerAndChannelNames() {
            assertEquals("CIPD", CreditCheckAgency4.CONTAINER_NAME);
            assertEquals("CIPCREDCHANN", CreditCheckAgency4.CHANNEL_NAME);
        }

        @Test
        @DisplayName("Abend code matches COBOL definition")
        void abendCode() {
            assertEquals("PLOP", CreditCheckAgency4.ABEND_CODE);
        }

        @Test
        @DisplayName("Delay range matches COBOL definition (1-3 seconds)")
        void delayRange() {
            assertEquals(1, CreditCheckAgency4.MIN_DELAY_SECONDS);
            assertEquals(3, CreditCheckAgency4.MAX_DELAY_SECONDS);
        }

        @Test
        @DisplayName("Credit score range matches COBOL definition (1-999)")
        void creditScoreRange() {
            assertEquals(1, CreditCheckAgency4.MIN_CREDIT_SCORE);
            assertEquals(999, CreditCheckAgency4.MAX_CREDIT_SCORE);
        }
    }

    @Nested
    @DisplayName("Default constructor")
    class DefaultConstructorTests {

        @Test
        @DisplayName("Default constructor creates functional instance")
        void defaultConstructorWorks() {
            CreditCheckAgency4 agency = new CreditCheckAgency4(
                    new Random(), NO_OP_DELAYER);
            ContainerData result = agency.process(sampleRequest());
            assertNotNull(result);
            assertTrue(result.getCreditScore() >= 1
                    && result.getCreditScore() <= 999);
        }

        @Test
        @DisplayName("Seed constructor creates deterministic instance")
        void seedConstructorIsDeterministic() {
            CreditCheckAgency4 a1 = agencyWithSeed(123L);
            CreditCheckAgency4 a2 = agencyWithSeed(123L);
            ContainerData request = sampleRequest();
            assertEquals(
                    a1.process(request).getCreditScore(),
                    a2.process(request).getCreditScore());
        }
    }
}
