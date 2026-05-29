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
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for {@link CreditCheckAgency3}, the Java 21 migration of
 * CRDTAGY3.cbl.
 */
class CreditCheckAgency3Test {

    private static final CreditCheckAgency3.Delayer NO_OP_DELAYER = seconds -> {};

    private CreditCheckData createSampleData() {
        return new CreditCheckData(
                "CRDA",
                "987654",
                "0000000001",
                "John Smith",
                "123 Main Street, Springfield, IL 62701",
                LocalDate.of(1985, 6, 15),
                0,
                LocalDate.of(2024, 1, 15),
                "Y",
                " "
        );
    }

    @Nested
    @DisplayName("processCreditCheck - happy path")
    class ProcessCreditCheckHappyPath {

        @Test
        @DisplayName("should generate a credit score between 1 and 999")
        void shouldGenerateCreditScoreInValidRange() {
            CreditCheckAgency3 agency = new CreditCheckAgency3(12345L, NO_OP_DELAYER);
            CreditCheckData data = createSampleData();

            CreditCheckData result = agency.processCreditCheck(data);

            assertTrue(result.getCreditScore() >= 1,
                    "Credit score should be at least 1");
            assertTrue(result.getCreditScore() <= 999,
                    "Credit score should be at most 999");
        }

        @Test
        @DisplayName("should update the credit score field in the data")
        void shouldUpdateCreditScoreInData() {
            CreditCheckAgency3 agency = new CreditCheckAgency3(42L, NO_OP_DELAYER);
            CreditCheckData data = createSampleData();
            assertEquals(0, data.getCreditScore());

            agency.processCreditCheck(data);

            assertNotEquals(0, data.getCreditScore(),
                    "Credit score should be updated from initial 0");
        }

        @Test
        @DisplayName("should return the same data instance with updated score")
        void shouldReturnSameDataInstance() {
            CreditCheckAgency3 agency = new CreditCheckAgency3(99L, NO_OP_DELAYER);
            CreditCheckData data = createSampleData();

            CreditCheckData result = agency.processCreditCheck(data);

            assertSame(data, result, "Should return the same data object");
        }

        @Test
        @DisplayName("should preserve all non-score fields")
        void shouldPreserveNonScoreFields() {
            CreditCheckAgency3 agency = new CreditCheckAgency3(100L, NO_OP_DELAYER);
            CreditCheckData data = createSampleData();

            agency.processCreditCheck(data);

            assertEquals("CRDA", data.getEyecatcher());
            assertEquals("987654", data.getSortCode());
            assertEquals("0000000001", data.getAccountNumber());
            assertEquals("John Smith", data.getCustomerName());
            assertEquals("123 Main Street, Springfield, IL 62701",
                    data.getCustomerAddress());
            assertEquals(LocalDate.of(1985, 6, 15), data.getDateOfBirth());
            assertEquals(LocalDate.of(2024, 1, 15),
                    data.getCreditScoreReviewDate());
            assertEquals("Y", data.getSuccessFlag());
            assertEquals(" ", data.getFailCode());
        }

        @Test
        @DisplayName("should invoke the delayer with a value between 1 and 3")
        void shouldInvokeDelayerWithCorrectRange() {
            AtomicInteger delayedSeconds = new AtomicInteger(0);
            CreditCheckAgency3.Delayer capturingDelayer =
                    delayedSeconds::set;

            CreditCheckAgency3 agency = new CreditCheckAgency3(
                    555L, capturingDelayer);
            CreditCheckData data = createSampleData();

            agency.processCreditCheck(data);

            int delay = delayedSeconds.get();
            assertTrue(delay >= 1 && delay <= 3,
                    "Delay should be between 1 and 3, got: " + delay);
        }
    }

    @Nested
    @DisplayName("processCreditCheck - error handling")
    class ProcessCreditCheckErrorHandling {

        @Test
        @DisplayName("should throw CreditCheckAgencyException when data is null")
        void shouldThrowExceptionWhenDataIsNull() {
            CreditCheckAgency3 agency = new CreditCheckAgency3(1L, NO_OP_DELAYER);

            CreditCheckAgencyException ex = assertThrows(
                    CreditCheckAgencyException.class,
                    () -> agency.processCreditCheck(null));

            assertEquals("PLOP", ex.getAbendCode());
            assertTrue(ex.getMessage().contains("UNABLE TO GET CONTAINER"));
        }

        @Test
        @DisplayName("should throw CreditCheckAgencyException when delay is interrupted")
        void shouldThrowExceptionWhenDelayInterrupted() {
            CreditCheckAgency3.Delayer interruptingDelayer = seconds -> {
                throw new InterruptedException("Simulated interrupt");
            };

            CreditCheckAgency3 agency = new CreditCheckAgency3(
                    1L, interruptingDelayer);
            CreditCheckData data = createSampleData();

            CreditCheckAgencyException ex = assertThrows(
                    CreditCheckAgencyException.class,
                    () -> agency.processCreditCheck(data));

            assertEquals("PLOP", ex.getAbendCode());
            assertTrue(ex.getMessage().contains("delay messed up"));
            assertInstanceOf(InterruptedException.class, ex.getCause());
        }

        @Test
        @DisplayName("should set interrupt flag when delay is interrupted")
        void shouldSetInterruptFlagWhenDelayInterrupted() {
            CreditCheckAgency3.Delayer interruptingDelayer = seconds -> {
                throw new InterruptedException("Simulated interrupt");
            };

            CreditCheckAgency3 agency = new CreditCheckAgency3(
                    1L, interruptingDelayer);
            CreditCheckData data = createSampleData();

            try {
                agency.processCreditCheck(data);
            } catch (CreditCheckAgencyException e) {
                // expected
            }

            assertTrue(Thread.currentThread().isInterrupted(),
                    "Interrupt flag should be set");
            // Clear interrupt for test cleanup
            Thread.interrupted();
        }
    }

    @Nested
    @DisplayName("generateDelaySeconds")
    class GenerateDelaySeconds {

        @RepeatedTest(50)
        @DisplayName("should always produce value between 1 and 3 inclusive")
        void shouldProduceValueInRange() {
            long seed = System.nanoTime();
            CreditCheckAgency3 agency = new CreditCheckAgency3(seed, NO_OP_DELAYER);

            int delay = agency.generateDelaySeconds();

            assertTrue(delay >= 1 && delay <= 3,
                    "Delay should be 1-3, got: " + delay + " (seed=" + seed + ")");
        }
    }

    @Nested
    @DisplayName("generateCreditScore")
    class GenerateCreditScore {

        @RepeatedTest(100)
        @DisplayName("should always produce value between 1 and 999 inclusive")
        void shouldProduceValueInRange() {
            long seed = System.nanoTime();
            CreditCheckAgency3 agency = new CreditCheckAgency3(seed, NO_OP_DELAYER);
            // Consume the first random call (used for delay)
            agency.generateDelaySeconds();

            int score = agency.generateCreditScore();

            assertTrue(score >= 1 && score <= 999,
                    "Score should be 1-999, got: " + score + " (seed=" + seed + ")");
        }
    }

    @Nested
    @DisplayName("Deterministic behavior with same seed")
    class DeterministicBehavior {

        @ParameterizedTest
        @ValueSource(longs = {1L, 42L, 12345L, 999999L, Long.MAX_VALUE})
        @DisplayName("should produce same results with same seed")
        void shouldProduceSameResultsWithSameSeed(long seed) {
            CreditCheckAgency3 agency1 = new CreditCheckAgency3(seed, NO_OP_DELAYER);
            CreditCheckAgency3 agency2 = new CreditCheckAgency3(seed, NO_OP_DELAYER);

            CreditCheckData data1 = createSampleData();
            CreditCheckData data2 = createSampleData();

            agency1.processCreditCheck(data1);
            agency2.processCreditCheck(data2);

            assertEquals(data1.getCreditScore(), data2.getCreditScore(),
                    "Same seed should produce same credit score");
        }

        @Test
        @DisplayName("different seeds should generally produce different scores")
        void differentSeedsShouldProduceDifferentScores() {
            CreditCheckAgency3 agency1 = new CreditCheckAgency3(1L, NO_OP_DELAYER);
            CreditCheckAgency3 agency2 = new CreditCheckAgency3(99999L, NO_OP_DELAYER);

            CreditCheckData data1 = createSampleData();
            CreditCheckData data2 = createSampleData();

            agency1.processCreditCheck(data1);
            agency2.processCreditCheck(data2);

            // While not guaranteed, with these seeds they should differ
            // This is a statistical test — unlikely to be the same
            assertNotEquals(data1.getCreditScore(), data2.getCreditScore(),
                    "Different seeds should typically produce different scores");
        }
    }

    @Nested
    @DisplayName("Constants")
    class Constants {

        @Test
        @DisplayName("should have correct container name")
        void shouldHaveCorrectContainerName() {
            assertEquals("CIPC", CreditCheckAgency3.CONTAINER_NAME);
        }

        @Test
        @DisplayName("should have correct channel name")
        void shouldHaveCorrectChannelName() {
            assertEquals("CIPCREDCHANN", CreditCheckAgency3.CHANNEL_NAME);
        }

        @Test
        @DisplayName("should have correct abend code")
        void shouldHaveCorrectAbendCode() {
            assertEquals("PLOP", CreditCheckAgency3.ABEND_CODE);
        }
    }

    @Nested
    @DisplayName("Integration-style test")
    class IntegrationTest {

        @Test
        @DisplayName("should process multiple sequential credit checks")
        void shouldProcessMultipleSequentialCreditChecks() {
            CreditCheckAgency3 agency = new CreditCheckAgency3(42L, NO_OP_DELAYER);

            for (int i = 0; i < 10; i++) {
                CreditCheckData data = createSampleData();
                data.setAccountNumber(String.format("%010d", i));

                CreditCheckData result = agency.processCreditCheck(data);

                assertTrue(result.getCreditScore() >= 1);
                assertTrue(result.getCreditScore() <= 999);
                assertEquals(String.format("%010d", i), result.getAccountNumber());
            }
        }

        @Test
        @DisplayName("should work with minimal data populated")
        void shouldWorkWithMinimalData() {
            CreditCheckAgency3 agency = new CreditCheckAgency3(1L, NO_OP_DELAYER);
            CreditCheckData data = new CreditCheckData();

            CreditCheckData result = agency.processCreditCheck(data);

            assertTrue(result.getCreditScore() >= 1);
            assertTrue(result.getCreditScore() <= 999);
        }
    }
}
