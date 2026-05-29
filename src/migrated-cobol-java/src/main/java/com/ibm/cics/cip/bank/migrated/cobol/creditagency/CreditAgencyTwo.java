/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.migrated.cobol.creditagency;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Java 21 migration of the COBOL program CRDTAGY2.cbl — Credit Check Agency
 * Program 2.
 *
 * <p>This is a dummy credit agency used for credit scoring. It simulates an
 * external credit agency by:
 * <ol>
 *   <li>Delaying for a random duration (1–2 seconds) to emulate network
 *       latency when called asynchronously</li>
 *   <li>Generating a random credit score between 1 and 998</li>
 *   <li>Returning the input data with the newly generated credit score</li>
 * </ol>
 *
 * <p>In the original CICS application, five credit agency programs
 * (CRDTAGY1–CRDTAGY5) are invoked asynchronously via the CICS Async API.
 * Each uses a different container (CIPA–CIPE) on the shared channel
 * CIPCREDCHANN. This program (agency 2) uses container <b>CIPB</b>.
 *
 * <p>The parent program waits up to 3 seconds for responses, so the random
 * delay means roughly 1-in-4 chance this agency won't respond in time —
 * emulating real-world unreliable external services.
 *
 * <h3>COBOL-to-Java Mapping</h3>
 * <table>
 *   <tr><th>COBOL construct</th><th>Java equivalent</th></tr>
 *   <tr><td>EXEC CICS DELAY</td><td>{@link DelayStrategy} (Thread.sleep)</td></tr>
 *   <tr><td>EXEC CICS GET/PUT CONTAINER</td><td>Method input / return value</td></tr>
 *   <tr><td>FUNCTION RANDOM</td><td>{@link Random}</td></tr>
 *   <tr><td>EXEC CICS ABEND</td><td>{@link CreditAgencyException}</td></tr>
 *   <tr><td>EIBTASKN (seed)</td><td>taskNumber parameter</td></tr>
 *   <tr><td>WS-CONT-IN</td><td>{@link CreditCheckData}</td></tr>
 *   <tr><td>ABNDINFO-REC</td><td>{@link AbendInfo}</td></tr>
 * </table>
 */
public class CreditAgencyTwo {

    private static final Logger logger =
            Logger.getLogger(CreditAgencyTwo.class.getName());

    static final int DELAY_RANGE_MIN = 1;
    static final int DELAY_RANGE_MAX = 3;
    static final int CREDIT_SCORE_RANGE_MIN = 1;
    static final int CREDIT_SCORE_RANGE_MAX = 999;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    private final DelayStrategy delayStrategy;

    /**
     * Strategy for introducing a delay, extracted for testability.
     * In the COBOL original, this was {@code EXEC CICS DELAY FOR SECONDS(n)}.
     */
    @FunctionalInterface
    public interface DelayStrategy {
        void delay(int seconds) throws InterruptedException;
    }

    /**
     * Constructs a CreditAgencyTwo with the default delay strategy
     * ({@link Thread#sleep}).
     */
    public CreditAgencyTwo() {
        this(seconds -> Thread.sleep(seconds * 1000L));
    }

    /**
     * Constructs a CreditAgencyTwo with a custom delay strategy.
     *
     * @param delayStrategy the strategy to use for delaying execution
     */
    public CreditAgencyTwo(DelayStrategy delayStrategy) {
        this.delayStrategy = delayStrategy;
    }

    /**
     * Processes a credit check request by generating a random credit score.
     *
     * <p>This method faithfully reproduces the COBOL PROCEDURE DIVISION logic:
     * <ol>
     *   <li>Seeds a random number generator with the task number
     *       (equivalent to {@code MOVE EIBTASKN TO WS-SEED})</li>
     *   <li>Computes a random delay using the formula
     *       {@code ((3-1) * RANDOM(SEED)) + 1} and sleeps</li>
     *   <li>Computes a random credit score using the formula
     *       {@code ((999-1) * RANDOM) + 1}</li>
     *   <li>Returns the input data with the updated credit score</li>
     * </ol>
     *
     * @param input      the credit check data received from the parent program
     * @param taskNumber the CICS task number used to seed the random generator
     * @return a new {@link CreditCheckData} with the generated credit score
     * @throws CreditAgencyException if the delay is interrupted (equivalent to
     *                               CICS ABEND 'PLOP')
     * @throws IllegalArgumentException if input is null
     */
    public CreditCheckData processCreditCheck(CreditCheckData input,
            long taskNumber) {
        if (input == null) {
            logger.severe("CRDTAGY2 - UNABLE TO GET CONTAINER. "
                    + "Input data is null");
            throw new IllegalArgumentException(
                    "Credit check data must not be null");
        }

        Random random = new Random(taskNumber);

        int delaySeconds = computeDelay(random);
        logger.log(Level.FINE, () -> "CRDTAGY2 - Delaying for "
                + delaySeconds + " second(s)");

        try {
            delayStrategy.delay(delaySeconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            AbendInfo abendInfo = buildAbendInfo(taskNumber, 0, 0);
            logger.severe("*** The delay messed up ! ***");
            throw new CreditAgencyException(
                    "A010 - *** The delay messed up! ***", abendInfo, e);
        }

        int creditScore = computeCreditScore(random);
        logger.log(Level.FINE, () -> "CRDTAGY2 - Generated credit score: "
                + creditScore);

        return input.withCreditScore(creditScore);
    }

    /**
     * Computes the delay duration in seconds using the COBOL formula:
     * {@code ((3 - 1) * FUNCTION RANDOM(WS-SEED)) + 1}.
     * Result is truncated to an integer, yielding 1 or 2.
     */
    int computeDelay(Random random) {
        return (int) ((DELAY_RANGE_MAX - DELAY_RANGE_MIN)
                * random.nextDouble()) + DELAY_RANGE_MIN;
    }

    /**
     * Computes the credit score using the COBOL formula:
     * {@code ((999 - 1) * FUNCTION RANDOM) + 1}.
     * Result is truncated to an integer in the range [1, 998].
     */
    int computeCreditScore(Random random) {
        return (int) ((CREDIT_SCORE_RANGE_MAX - CREDIT_SCORE_RANGE_MIN)
                * random.nextDouble()) + CREDIT_SCORE_RANGE_MIN;
    }

    private AbendInfo buildAbendInfo(long taskNumber, int respCode,
            int resp2Code) {
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        long utimeMillis = Instant.now().toEpochMilli();

        return new AbendInfo(
                utimeMillis,
                (int) (taskNumber % 10_000),
                "",
                "",
                now.format(DATE_FORMATTER),
                now.format(TIME_FORMATTER),
                AbendInfo.DEFAULT_ABEND_CODE,
                AbendInfo.PROGRAM_NAME,
                respCode,
                resp2Code,
                0,
                "A010  - *** The delay messed up! ***"
                        + " EIBRESP=" + respCode
                        + " RESP2=" + resp2Code
        );
    }
}
