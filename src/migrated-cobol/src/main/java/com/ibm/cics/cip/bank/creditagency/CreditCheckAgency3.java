/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.creditagency;

import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Java 21 migration of CRDTAGY3.cbl — Credit Check Agency Program 3.
 *
 * <p>This program is a dummy credit agency used for credit scoring. In the
 * original COBOL implementation, data is passed via a CICS channel/container
 * and the program:
 * <ol>
 *   <li>Delays for a random amount of time (between 1 and 3 seconds) to
 *       emulate network latency when communicating with an external credit
 *       agency.</li>
 *   <li>Generates a random credit score between 1 and 999.</li>
 *   <li>Returns the updated data with the new credit score.</li>
 * </ol>
 *
 * <p>The purpose of the initial delay is to emulate data being returned to
 * the parent program (this program is driven using the Async API). There is
 * a delay of 3 seconds in the parent so there is approximately a 1 in 4
 * chance that data will be returned within the overall 3 second window —
 * this emulates not always being able to get a timely reply from the agency.
 *
 * <p>COBOL source: src/base/cobol_src/CRDTAGY3.cbl
 *
 * @see CreditCheckData
 */
public class CreditCheckAgency3 {

    private static final Logger logger = Logger.getLogger(
            CreditCheckAgency3.class.getName());

    static final String CONTAINER_NAME = "CIPC";
    static final String CHANNEL_NAME = "CIPCREDCHANN";
    static final String ABEND_CODE = "PLOP";

    static final int MIN_DELAY_SECONDS = 1;
    static final int MAX_DELAY_SECONDS = 3;
    static final int MIN_CREDIT_SCORE = 1;
    static final int MAX_CREDIT_SCORE = 999;

    private final Random random;
    private final Delayer delayer;

    /**
     * Functional interface for the delay mechanism, allowing the delay to be
     * injected for testability. In production this wraps Thread.sleep; in tests
     * it can be a no-op or a mock.
     */
    @FunctionalInterface
    public interface Delayer {
        void delay(int seconds) throws InterruptedException;
    }

    /**
     * Constructs a CreditCheckAgency3 with the given seed for deterministic
     * random number generation, and a custom delayer.
     *
     * @param seed    the random seed (maps to EIBTASKN in COBOL)
     * @param delayer the delay implementation
     */
    public CreditCheckAgency3(long seed, Delayer delayer) {
        this.random = new Random(seed);
        this.delayer = delayer;
    }

    /**
     * Constructs a CreditCheckAgency3 with the given seed and the default
     * Thread.sleep-based delayer.
     *
     * @param seed the random seed (maps to EIBTASKN in COBOL)
     */
    public CreditCheckAgency3(long seed) {
        this(seed, seconds -> Thread.sleep(seconds * 1000L));
    }

    /**
     * Processes the credit check request. This is the Java equivalent of the
     * COBOL PROCEDURE DIVISION in CRDTAGY3.cbl.
     *
     * <p>The method:
     * <ol>
     *   <li>Generates a random delay between 1 and 3 seconds and waits.</li>
     *   <li>Validates the input data (equivalent to GET CONTAINER).</li>
     *   <li>Generates a random credit score between 1 and 999.</li>
     *   <li>Updates the data with the new credit score.</li>
     * </ol>
     *
     * @param data the credit check data (equivalent to container data)
     * @return the updated credit check data with the generated credit score
     * @throws CreditCheckAgencyException if the delay fails or data is null
     */
    public CreditCheckData processCreditCheck(CreditCheckData data) {
        logger.entering(getClass().getName(), "processCreditCheck");

        int delaySeconds = generateDelaySeconds();
        logger.log(Level.FINE, "Delaying for {0} second(s)", delaySeconds);

        try {
            delayer.delay(delaySeconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.severe("Delay interrupted — equivalent to CICS DELAY failure");
            throw new CreditCheckAgencyException(
                    "A010 - *** The delay messed up! ***", ABEND_CODE, e);
        }

        if (data == null) {
            logger.severe("CRDTAGY3 - UNABLE TO GET CONTAINER. Data is null");
            throw new CreditCheckAgencyException(
                    "CRDTAGY3 - UNABLE TO GET CONTAINER. Data is null",
                    ABEND_CODE);
        }

        int creditScore = generateCreditScore();
        logger.log(Level.FINE, "Generated credit score: {0}", creditScore);

        data.setCreditScore(creditScore);

        logger.exiting(getClass().getName(), "processCreditCheck");
        return data;
    }

    /**
     * Generates a random delay between MIN_DELAY_SECONDS and MAX_DELAY_SECONDS
     * inclusive.
     *
     * <p>Maps to COBOL:
     * <pre>
     * COMPUTE WS-DELAY-AMT = ((3 - 1) * FUNCTION RANDOM(WS-SEED)) + 1.
     * </pre>
     *
     * @return the delay in seconds
     */
    int generateDelaySeconds() {
        return random.nextInt(MAX_DELAY_SECONDS - MIN_DELAY_SECONDS + 1)
                + MIN_DELAY_SECONDS;
    }

    /**
     * Generates a random credit score between MIN_CREDIT_SCORE and
     * MAX_CREDIT_SCORE inclusive.
     *
     * <p>Maps to COBOL:
     * <pre>
     * COMPUTE WS-NEW-CREDSCORE = ((999 - 1) * FUNCTION RANDOM) + 1.
     * </pre>
     *
     * @return the credit score (1-999)
     */
    int generateCreditScore() {
        return random.nextInt(MAX_CREDIT_SCORE - MIN_CREDIT_SCORE + 1)
                + MIN_CREDIT_SCORE;
    }
}
