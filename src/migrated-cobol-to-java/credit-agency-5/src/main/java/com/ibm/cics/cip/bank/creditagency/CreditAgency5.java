/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.creditagency;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Java 21 migration of CRDTAGY5.cbl - Credit Check Agency Program 5.
 *
 * <p>This program is a dummy credit agency used for credit scoring.
 * Data is passed to the program via a container/channel mechanism,
 * and the role of this program is to:
 * <ol>
 *   <li>Delay for a random amount of time (between 1 and 3 seconds)
 *       to emulate data being returned from an external agency</li>
 *   <li>Generate a random credit score (between 1 and 999)</li>
 *   <li>Return the updated data</li>
 * </ol>
 *
 * <p>The purpose of the initial delay is to emulate data being returned
 * to the parent program (this program is driven using the Async API).
 * There is a delay of 3 seconds in the parent, so there is approximately
 * a 1 in 4 chance that data will be returned within the overall 3-second
 * delay - this emulates not always being able to get a timely reply.
 *
 * <p>Original COBOL program constants:
 * <ul>
 *   <li>Container name: "CIPE" (padded to 16 chars)</li>
 *   <li>Channel name: "CIPCREDCHANN" (padded to 16 chars)</li>
 *   <li>Abend code: "PLOP"</li>
 *   <li>Sort code constant: 987654</li>
 * </ul>
 *
 * @see CreditAgencyRequest
 * @see AbendInfo
 */
public class CreditAgency5 {

    private static final Logger LOGGER = Logger.getLogger(CreditAgency5.class.getName());

    public static final String CONTAINER_NAME = "CIPE";
    public static final String CHANNEL_NAME = "CIPCREDCHANN";
    public static final String ABEND_CODE = "PLOP";
    public static final String PROGRAM_NAME = "CRDTAGY5";
    public static final String ABEND_HANDLER_PROGRAM = "ABNDPROC";
    public static final int SORT_CODE = 987654;

    static final int MIN_DELAY_SECONDS = 1;
    static final int MAX_DELAY_SECONDS = 3;
    static final int MIN_CREDIT_SCORE = 1;
    static final int MAX_CREDIT_SCORE = 999;

    private final Random random;
    private final DelayService delayService;

    /**
     * Constructs a CreditAgency5 instance with a seeded random number
     * generator and the default delay service.
     *
     * @param seed the random seed (equivalent to EIBTASKN in COBOL)
     */
    public CreditAgency5(long seed) {
        this.random = new Random(seed);
        this.delayService = new ThreadSleepDelayService();
    }

    /**
     * Constructs a CreditAgency5 instance with injectable dependencies
     * for testability.
     *
     * @param random       the random number generator
     * @param delayService the delay service implementation
     */
    public CreditAgency5(Random random, DelayService delayService) {
        this.random = random;
        this.delayService = delayService;
    }

    /**
     * Processes a credit agency request by simulating a delay and generating
     * a random credit score. This is the main entry point equivalent to the
     * COBOL PROCEDURE DIVISION.
     *
     * <p>Business logic flow:
     * <ol>
     *   <li>Generate a random delay between 1 and 3 seconds</li>
     *   <li>Execute the delay (simulating external agency latency)</li>
     *   <li>Generate a random credit score between 1 and 999</li>
     *   <li>Update the request with the new credit score</li>
     * </ol>
     *
     * @param request the credit agency request containing customer data
     * @return the updated request with a newly generated credit score
     * @throws CreditAgencyException if the delay operation fails
     *         (equivalent to CICS ABEND with code "PLOP")
     */
    public CreditAgencyRequest processRequest(CreditAgencyRequest request) {
        LOGGER.log(Level.INFO, "CreditAgency5 - Processing credit check request");

        int delaySeconds = computeDelay();
        LOGGER.log(Level.FINE, "CreditAgency5 - Delay amount: {0} seconds", delaySeconds);

        performDelay(delaySeconds);

        int newCreditScore = generateCreditScore();
        LOGGER.log(Level.FINE, "CreditAgency5 - Generated credit score: {0}", newCreditScore);

        request.setCreditScore(newCreditScore);

        LOGGER.log(Level.INFO, "CreditAgency5 - Credit check complete. Score: {0}", newCreditScore);
        return request;
    }

    /**
     * Computes the random delay amount in seconds.
     * Equivalent to COBOL:
     * {@code COMPUTE WS-DELAY-AMT = ((3 - 1) * FUNCTION RANDOM(WS-SEED)) + 1}
     *
     * @return delay in seconds, between MIN_DELAY_SECONDS and MAX_DELAY_SECONDS
     */
    int computeDelay() {
        double randomValue = random.nextDouble();
        return (int) (((MAX_DELAY_SECONDS - MIN_DELAY_SECONDS) * randomValue) + MIN_DELAY_SECONDS);
    }

    /**
     * Performs the delay to simulate external agency response time.
     * If the delay fails, builds AbendInfo and throws a CreditAgencyException
     * (equivalent to the CICS ABEND in the COBOL original).
     *
     * @param delaySeconds the number of seconds to delay
     * @throws CreditAgencyException if the delay is interrupted
     */
    void performDelay(int delaySeconds) {
        try {
            delayService.delay(delaySeconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            AbendInfo abendInfo = buildAbendInfoForDelayFailure(delaySeconds);

            LOGGER.log(Level.SEVERE, "*** The delay messed up ! ***");
            throw new CreditAgencyException(
                    "A010 - *** The delay messed up! *** delaySeconds=" + delaySeconds,
                    ABEND_CODE, abendInfo);
        }
    }

    /**
     * Generates a random credit score between 1 and 999.
     * Equivalent to COBOL:
     * {@code COMPUTE WS-NEW-CREDSCORE = ((999 - 1) * FUNCTION RANDOM) + 1}
     *
     * @return credit score between MIN_CREDIT_SCORE and MAX_CREDIT_SCORE
     */
    int generateCreditScore() {
        double randomValue = random.nextDouble();
        return (int) (((MAX_CREDIT_SCORE - MIN_CREDIT_SCORE) * randomValue) + MIN_CREDIT_SCORE);
    }

    /**
     * Builds the AbendInfo record for a delay failure, preserving all
     * the diagnostic information that the COBOL original would have captured.
     */
    private AbendInfo buildAbendInfoForDelayFailure(int delaySeconds) {
        AbendInfo abendInfo = new AbendInfo();

        Instant now = Instant.now();
        long epochMillis = now.toEpochMilli();
        LocalDateTime dateTime = LocalDateTime.ofInstant(now, ZoneId.systemDefault());

        abendInfo.setUtimeKey(epochMillis);
        abendInfo.setCode(ABEND_CODE);
        abendInfo.setProgram(PROGRAM_NAME);
        abendInfo.setSqlCode(0);

        String dateStr = dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        abendInfo.setDate(dateStr);

        String timeStr = dateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        abendInfo.setTime(timeStr);

        String freeForm = "A010  - *** The delay messed up! ***"
                + " delaySeconds=" + delaySeconds;
        abendInfo.setFreeForm(freeForm);

        return abendInfo;
    }

    /**
     * Functional interface for the delay mechanism, allowing testability
     * without actual thread sleeping.
     */
    @FunctionalInterface
    public interface DelayService {
        void delay(int seconds) throws InterruptedException;
    }

    /**
     * Default implementation that uses Thread.sleep for the delay.
     */
    public static class ThreadSleepDelayService implements DelayService {
        @Override
        public void delay(int seconds) throws InterruptedException {
            Thread.sleep(seconds * 1000L);
        }
    }
}
