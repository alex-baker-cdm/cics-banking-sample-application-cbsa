/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.creditagency;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Java 21 migration of the COBOL program <strong>CRDTAGY4</strong>
 * (Credit Check Agency Program 4).
 *
 * <h2>Original COBOL behaviour</h2>
 * <ol>
 *   <li>Generate a random delay between 1 and 3 seconds (simulating an
 *       external credit agency response time) and sleep for that duration.</li>
 *   <li>Retrieve customer data from a CICS container
 *       ({@code CIPD} on channel {@code CIPCREDCHANN}).</li>
 *   <li>Generate a random credit score between 1 and 999.</li>
 *   <li>Update the container data with the new credit score.</li>
 *   <li>Return the updated container data.</li>
 * </ol>
 *
 * <h2>Migration notes</h2>
 * <ul>
 *   <li>CICS container/channel I/O is replaced by method parameter and
 *       return value.</li>
 *   <li>CICS DELAY is replaced by {@link Thread#sleep}.</li>
 *   <li>CICS ABEND is replaced by {@link CreditAgencyException}.</li>
 *   <li>The COBOL {@code FUNCTION RANDOM} behaviour is preserved via
 *       {@link java.util.Random}.</li>
 *   <li>The {@link Random} instance and delay mechanism are injectable for
 *       testing.</li>
 * </ul>
 */
public class CreditCheckAgency4 {

    private static final Logger logger = Logger.getLogger(
            CreditCheckAgency4.class.getName());

    static final String CONTAINER_NAME = "CIPD";
    static final String CHANNEL_NAME = "CIPCREDCHANN";
    static final String ABEND_CODE = "PLOP";
    static final String ABEND_PROGRAM = "ABNDPROC";

    static final int MIN_DELAY_SECONDS = 1;
    static final int MAX_DELAY_SECONDS = 3;
    static final int MIN_CREDIT_SCORE = 1;
    static final int MAX_CREDIT_SCORE = 999;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Random random;
    private final Delayer delayer;

    @FunctionalInterface
    public interface Delayer {
        void delay(int seconds) throws InterruptedException;
    }

    private static final Delayer DEFAULT_DELAYER =
            seconds -> Thread.sleep(seconds * 1000L);

    public CreditCheckAgency4() {
        this(new Random(), DEFAULT_DELAYER);
    }

    public CreditCheckAgency4(long seed) {
        this(new Random(seed), DEFAULT_DELAYER);
    }

    public CreditCheckAgency4(Random random, Delayer delayer) {
        this.random = random;
        this.delayer = delayer;
    }

    /**
     * Process a credit check request, mirroring the COBOL PREMIERE SECTION.
     *
     * <ol>
     *   <li>Sleeps for a random duration (1–3 s) to simulate agency
     *       latency.</li>
     *   <li>Validates the incoming container data.</li>
     *   <li>Generates a random credit score in the range [1, 999].</li>
     *   <li>Returns a new {@link ContainerData} instance with the updated
     *       credit score.</li>
     * </ol>
     *
     * @param request the customer data to score
     * @return a copy of the request with the {@code creditScore} field set
     * @throws CreditAgencyException if the simulated delay is interrupted
     *     or the input data is invalid
     */
    public ContainerData process(ContainerData request) {
        simulateAgencyDelay();
        validateContainerData(request);
        int newCreditScore = generateCreditScore();
        return buildResult(request, newCreditScore);
    }

    int generateDelaySeconds() {
        return random.nextInt(MAX_DELAY_SECONDS - MIN_DELAY_SECONDS + 1)
                + MIN_DELAY_SECONDS;
    }

    int generateCreditScore() {
        return random.nextInt(MAX_CREDIT_SCORE - MIN_CREDIT_SCORE + 1)
                + MIN_CREDIT_SCORE;
    }

    private void simulateAgencyDelay() {
        int delaySeconds = generateDelaySeconds();
        logger.log(Level.FINE, () ->
                "CRDTAGY4: Delaying for " + delaySeconds + " second(s)");

        try {
            delayer.delay(delaySeconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            AbendInfo abendInfo = buildAbendInfo(
                    0, 0,
                    "A010  - *** The delay messed up! ***"
                            + " InterruptedException");
            logger.severe("*** The delay messed up ! ***");
            throw new CreditAgencyException(
                    "CRDTAGY4: Delay interrupted", abendInfo, e);
        }
    }

    private void validateContainerData(ContainerData request) {
        if (request == null) {
            logger.severe("CRDTAGY4 - UNABLE TO GET CONTAINER. "
                    + "CONTAINER=" + CONTAINER_NAME
                    + " CHANNEL=" + CHANNEL_NAME);
            throw new CreditAgencyException(
                    "CRDTAGY4: Container data is null",
                    buildAbendInfo(0, 0,
                            "Container data was null for CONTAINER="
                                    + CONTAINER_NAME
                                    + " CHANNEL=" + CHANNEL_NAME));
        }
    }

    private ContainerData buildResult(ContainerData request,
            int newCreditScore) {
        ContainerData result = new ContainerData();
        result.setEyecatcher(request.getEyecatcher());
        result.setSortCode(request.getSortCode());
        result.setAccountNumber(request.getAccountNumber());
        result.setCustomerName(request.getCustomerName());
        result.setCustomerAddress(request.getCustomerAddress());
        result.setDateOfBirth(request.getDateOfBirth());
        result.setCreditScore(newCreditScore);
        result.setCsReviewDate(request.getCsReviewDate());
        result.setSuccessFlag(request.getSuccessFlag());
        result.setFailCode(request.getFailCode());
        return result;
    }

    private AbendInfo buildAbendInfo(int respCode, int resp2Code,
            String freeform) {
        LocalDate now = LocalDate.now();
        LocalTime time = LocalTime.now();
        return new AbendInfo(
                System.currentTimeMillis(),
                0,
                "",
                "",
                now.format(DATE_FORMATTER),
                time.format(TIME_FORMATTER),
                ABEND_CODE,
                "CRDTAGY4",
                respCode,
                resp2Code,
                0,
                freeform
        );
    }
}
