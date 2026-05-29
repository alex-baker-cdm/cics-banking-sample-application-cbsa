/*
 *
 *    Copyright IBM Corp. 2023
 *
 *
 */

package com.ibm.cics.cip.bank.migrated.creditagency;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Java 21 migration of CRDTAGY1.cbl (Credit Check Agency Program 1).
 *
 * This is a dummy credit agency program used for credit scoring. Data is
 * passed to the program via a channel and container. The program delays for
 * a random amount of time (between 1 and 3 seconds) and generates a random
 * credit score (between 1 and 999).
 *
 * The purpose of the initial delay is to emulate data being returned to the
 * parent program (this program is driven using the Async API). There is a
 * delay of 3 seconds in the parent, so there is a 1 in 4 chance that data
 * will be returned within the overall 3-second delay, emulating not always
 * being able to get a timely reply back to the parent.
 *
 * Original COBOL: src/base/cobol_src/CRDTAGY1.cbl
 * Original Author: Jon Collett
 */
public class CreditAgencyOne {

    private static final Logger logger =
            Logger.getLogger(CreditAgencyOne.class.getName());

    static final String CONTAINER_NAME = "CIPA";
    static final String CHANNEL_NAME = "CIPCREDCHANN";
    static final String PROGRAM_NAME = "CRDTAGY1";
    static final String ABEND_CODE = "PLOP";
    static final String ABEND_HANDLER_PROGRAM = "ABNDPROC";

    static final int DELAY_MIN_SECONDS = 1;
    static final int DELAY_MAX_SECONDS = 3;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Random random;
    private final DelayService delayService;
    private final ContainerService containerService;
    private final AbendHandler abendHandler;

    public CreditAgencyOne(DelayService delayService,
                           ContainerService containerService,
                           AbendHandler abendHandler,
                           Random random) {
        this.delayService = delayService;
        this.containerService = containerService;
        this.abendHandler = abendHandler;
        this.random = random;
    }

    public CreditAgencyOne() {
        this(new DefaultDelayService(),
             new DefaultContainerService(),
             new DefaultAbendHandler(),
             new Random());
    }

    /**
     * Main entry point, equivalent to PROCEDURE DIVISION / PREMIERE SECTION.
     *
     * @param taskNumber the CICS task number (EIBTASKN), used as random seed
     * @return the result of processing, containing the updated credit check
     *         data or error information
     */
    public ProcessingResult run(long taskNumber) {
        random.setSeed(taskNumber);

        int delaySeconds = computeDelaySeconds();

        try {
            delayService.delay(delaySeconds);
        } catch (DelayException e) {
            logger.log(Level.SEVERE, "*** The delay messed up ! ***", e);
            handleDelayAbend(e, taskNumber);
            return ProcessingResult.abended(ABEND_CODE);
        }

        CreditCheckData data;
        try {
            data = containerService.getContainer(CONTAINER_NAME, CHANNEL_NAME);
        } catch (ContainerException e) {
            logger.log(Level.SEVERE,
                    "CRDTAGY1 - UNABLE TO GET CONTAINER. RESP={0}, RESP2={1}"
                    + " CONTAINER={2} CHANNEL={3}",
                    new Object[]{e.getRespCode(), e.getResp2Code(),
                            CONTAINER_NAME, CHANNEL_NAME});
            return ProcessingResult.failed(e.getRespCode(), e.getResp2Code());
        }

        int newCreditScore = generateCreditScore();
        data.setCreditScore(newCreditScore);

        try {
            containerService.putContainer(CONTAINER_NAME, CHANNEL_NAME, data);
        } catch (ContainerException e) {
            logger.log(Level.SEVERE,
                    "CRDTAGY1 - UNABLE TO PUT CONTAINER. RESP={0}, RESP2={1}"
                    + " CONTAINER={2} CHANNEL={3}",
                    new Object[]{e.getRespCode(), e.getResp2Code(),
                            CONTAINER_NAME, CHANNEL_NAME});
            return ProcessingResult.failed(e.getRespCode(), e.getResp2Code());
        }

        return ProcessingResult.success(data);
    }

    /**
     * Computes random delay between DELAY_MIN_SECONDS and DELAY_MAX_SECONDS.
     * Mirrors COBOL: COMPUTE WS-DELAY-AMT = ((3 - 1) * FUNCTION RANDOM(WS-SEED)) + 1
     */
    int computeDelaySeconds() {
        return (int) (((DELAY_MAX_SECONDS - DELAY_MIN_SECONDS)
                * random.nextDouble()) + DELAY_MIN_SECONDS);
    }

    /**
     * Generates a random credit score between 1 and 999.
     * Mirrors COBOL: COMPUTE WS-NEW-CREDSCORE = ((999 - 1) * FUNCTION RANDOM) + 1
     */
    int generateCreditScore() {
        return (int) (((CreditCheckData.MAX_CREDIT_SCORE
                - CreditCheckData.MIN_CREDIT_SCORE)
                * random.nextDouble()) + CreditCheckData.MIN_CREDIT_SCORE);
    }

    private void handleDelayAbend(DelayException e, long taskNumber) {
        AbendInfo abendInfo = new AbendInfo();
        abendInfo.setRespCode(e.getRespCode());
        abendInfo.setResp2Code(e.getResp2Code());

        abendInfo.setTaskNoKey((int) taskNumber);

        LocalDateTime now = LocalDateTime.now();
        abendInfo.setDate(now.format(DATE_FORMATTER));
        abendInfo.setTime(now.format(TIME_FORMATTER));

        abendInfo.setCode(ABEND_CODE);
        abendInfo.setSqlCode(0);

        String freeform = "A010  - *** The delay messed up! ***"
                + " EIBRESP=" + e.getRespCode()
                + " RESP2=" + e.getResp2Code();
        abendInfo.setFreeform(freeform);

        abendHandler.handleAbend(ABEND_HANDLER_PROGRAM, abendInfo);
    }

    /**
     * Encapsulates the result of processing.
     */
    public sealed interface ProcessingResult {

        record Success(CreditCheckData data) implements ProcessingResult {}
        record Failed(int respCode, int resp2Code) implements ProcessingResult {}
        record Abended(String abendCode) implements ProcessingResult {}

        static ProcessingResult success(CreditCheckData data) {
            return new Success(data);
        }

        static ProcessingResult failed(int respCode, int resp2Code) {
            return new Failed(respCode, resp2Code);
        }

        static ProcessingResult abended(String abendCode) {
            return new Abended(abendCode);
        }
    }

    /**
     * Abstracts the CICS DELAY command.
     */
    public interface DelayService {
        void delay(int seconds) throws DelayException;
    }

    /**
     * Abstracts CICS container GET/PUT operations.
     */
    public interface ContainerService {
        CreditCheckData getContainer(String containerName, String channelName)
                throws ContainerException;

        void putContainer(String containerName, String channelName,
                          CreditCheckData data)
                throws ContainerException;
    }

    /**
     * Abstracts the CICS LINK to the abend handler program.
     */
    public interface AbendHandler {
        void handleAbend(String programName, AbendInfo abendInfo);
    }

    /**
     * Exception thrown when the CICS DELAY command fails.
     */
    public static class DelayException extends Exception {

        private final int respCode;
        private final int resp2Code;

        public DelayException(int respCode, int resp2Code) {
            super("Delay failed with RESP=" + respCode
                    + " RESP2=" + resp2Code);
            this.respCode = respCode;
            this.resp2Code = resp2Code;
        }

        public int getRespCode() {
            return respCode;
        }

        public int getResp2Code() {
            return resp2Code;
        }
    }

    /**
     * Exception thrown when a CICS container GET or PUT fails.
     */
    public static class ContainerException extends Exception {

        private final int respCode;
        private final int resp2Code;

        public ContainerException(int respCode, int resp2Code) {
            super("Container operation failed with RESP=" + respCode
                    + " RESP2=" + resp2Code);
            this.respCode = respCode;
            this.resp2Code = resp2Code;
        }

        public int getRespCode() {
            return respCode;
        }

        public int getResp2Code() {
            return resp2Code;
        }
    }

    /**
     * Default delay implementation using Thread.sleep.
     */
    static class DefaultDelayService implements DelayService {
        @Override
        public void delay(int seconds) throws DelayException {
            try {
                Thread.sleep(seconds * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new DelayException(-1, -1);
            }
        }
    }

    /**
     * Default container service (no-op for standalone usage).
     */
    static class DefaultContainerService implements ContainerService {
        @Override
        public CreditCheckData getContainer(String containerName,
                                            String channelName)
                throws ContainerException {
            return new CreditCheckData();
        }

        @Override
        public void putContainer(String containerName, String channelName,
                                 CreditCheckData data)
                throws ContainerException {
            // No-op in standalone mode
        }
    }

    /**
     * Default abend handler that logs the abend information.
     */
    static class DefaultAbendHandler implements AbendHandler {
        @Override
        public void handleAbend(String programName, AbendInfo abendInfo) {
            Logger.getLogger(DefaultAbendHandler.class.getName())
                    .log(Level.SEVERE,
                            "Abend handled by {0}: {1}",
                            new Object[]{programName, abendInfo});
        }
    }
}
