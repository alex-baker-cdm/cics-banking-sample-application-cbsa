/*
 *
 *    Copyright IBM Corp. 2023
 *
 *
 */

package com.ibm.cics.cip.bank.migrated.creditagency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class CreditAgencyOneTest {

    private StubDelayService delayService;
    private StubContainerService containerService;
    private StubAbendHandler abendHandler;
    private Random random;

    @BeforeEach
    void setUp() {
        delayService = new StubDelayService();
        containerService = new StubContainerService();
        abendHandler = new StubAbendHandler();
        random = new Random(42);
    }

    private CreditAgencyOne createAgency() {
        return new CreditAgencyOne(delayService, containerService,
                abendHandler, random);
    }

    @Test
    void runSuccessfullyGeneratesCreditScoreAndReturnsSuccess() {
        CreditCheckData inputData = createSampleData();
        containerService.setDataToReturn(inputData);

        CreditAgencyOne agency = createAgency();
        CreditAgencyOne.ProcessingResult result = agency.run(12345L);

        assertInstanceOf(CreditAgencyOne.ProcessingResult.Success.class,
                result);
        CreditAgencyOne.ProcessingResult.Success success =
                (CreditAgencyOne.ProcessingResult.Success) result;

        int creditScore = success.data().getCreditScore();
        assertTrue(creditScore >= CreditCheckData.MIN_CREDIT_SCORE,
                "Credit score should be >= 1, was " + creditScore);
        assertTrue(creditScore <= CreditCheckData.MAX_CREDIT_SCORE,
                "Credit score should be <= 999, was " + creditScore);

        assertTrue(delayService.wasDelayed(),
                "Delay service should have been called");
        assertTrue(containerService.wasGetCalled(),
                "Container GET should have been called");
        assertTrue(containerService.wasPutCalled(),
                "Container PUT should have been called");
    }

    @Test
    void runSetsCorrectContainerAndChannelNames() {
        containerService.setDataToReturn(createSampleData());

        CreditAgencyOne agency = createAgency();
        agency.run(1L);

        assertEquals(CreditAgencyOne.CONTAINER_NAME,
                containerService.getLastGetContainerName());
        assertEquals(CreditAgencyOne.CHANNEL_NAME,
                containerService.getLastGetChannelName());
        assertEquals(CreditAgencyOne.CONTAINER_NAME,
                containerService.getLastPutContainerName());
        assertEquals(CreditAgencyOne.CHANNEL_NAME,
                containerService.getLastPutChannelName());
    }

    @Test
    void runReturnsAbendedWhenDelayFails() {
        delayService.setShouldFail(true, 16, 0);

        CreditAgencyOne agency = createAgency();
        CreditAgencyOne.ProcessingResult result = agency.run(1L);

        assertInstanceOf(CreditAgencyOne.ProcessingResult.Abended.class,
                result);
        CreditAgencyOne.ProcessingResult.Abended abended =
                (CreditAgencyOne.ProcessingResult.Abended) result;
        assertEquals(CreditAgencyOne.ABEND_CODE, abended.abendCode());

        assertTrue(abendHandler.wasHandled(),
                "Abend handler should have been invoked");
        assertEquals(CreditAgencyOne.ABEND_HANDLER_PROGRAM,
                abendHandler.getLastProgramName());
    }

    @Test
    void runPopulatesAbendInfoOnDelayFailure() {
        delayService.setShouldFail(true, 16, 42);

        CreditAgencyOne agency = createAgency();
        agency.run(9999L);

        AbendInfo info = abendHandler.getLastAbendInfo();
        assertNotNull(info);
        assertEquals(16, info.getRespCode());
        assertEquals(42, info.getResp2Code());
        assertEquals(9999, info.getTaskNoKey());
        assertEquals(CreditAgencyOne.ABEND_CODE, info.getCode());
        assertEquals(0, info.getSqlCode());
        assertTrue(info.getFreeform().contains(
                "A010  - *** The delay messed up! ***"));
        assertTrue(info.getFreeform().contains("EIBRESP=16"));
        assertTrue(info.getFreeform().contains("RESP2=42"));
        assertFalse(info.getDate().isEmpty());
        assertFalse(info.getTime().isEmpty());
    }

    @Test
    void runReturnsFailedWhenGetContainerFails() {
        containerService.setGetShouldFail(true, 22, 5);

        CreditAgencyOne agency = createAgency();
        CreditAgencyOne.ProcessingResult result = agency.run(1L);

        assertInstanceOf(CreditAgencyOne.ProcessingResult.Failed.class,
                result);
        CreditAgencyOne.ProcessingResult.Failed failed =
                (CreditAgencyOne.ProcessingResult.Failed) result;
        assertEquals(22, failed.respCode());
        assertEquals(5, failed.resp2Code());

        assertFalse(containerService.wasPutCalled(),
                "Container PUT should not be called after GET failure");
    }

    @Test
    void runReturnsFailedWhenPutContainerFails() {
        containerService.setDataToReturn(createSampleData());
        containerService.setPutShouldFail(true, 70, 1);

        CreditAgencyOne agency = createAgency();
        CreditAgencyOne.ProcessingResult result = agency.run(1L);

        assertInstanceOf(CreditAgencyOne.ProcessingResult.Failed.class,
                result);
        CreditAgencyOne.ProcessingResult.Failed failed =
                (CreditAgencyOne.ProcessingResult.Failed) result;
        assertEquals(70, failed.respCode());
        assertEquals(1, failed.resp2Code());
    }

    @Test
    void computeDelaySecondsReturnsValueBetween1And3() {
        CreditAgencyOne agency = createAgency();
        for (int i = 0; i < 1000; i++) {
            int delay = agency.computeDelaySeconds();
            assertTrue(delay >= CreditAgencyOne.DELAY_MIN_SECONDS,
                    "Delay should be >= 1, was " + delay);
            assertTrue(delay <= CreditAgencyOne.DELAY_MAX_SECONDS,
                    "Delay should be <= 3, was " + delay);
        }
    }

    @Test
    void generateCreditScoreReturnsBetween1And999() {
        CreditAgencyOne agency = createAgency();
        for (int i = 0; i < 1000; i++) {
            int score = agency.generateCreditScore();
            assertTrue(score >= CreditCheckData.MIN_CREDIT_SCORE,
                    "Score should be >= 1, was " + score);
            assertTrue(score <= CreditCheckData.MAX_CREDIT_SCORE,
                    "Score should be <= 999, was " + score);
        }
    }

    @Test
    void runWithSameSeedProducesDeterministicResults() {
        containerService.setDataToReturn(createSampleData());
        CreditAgencyOne agency1 = new CreditAgencyOne(
                new StubDelayService(), containerService,
                abendHandler, new Random());
        CreditAgencyOne.ProcessingResult result1 = agency1.run(42L);

        containerService.setDataToReturn(createSampleData());
        containerService.resetCalls();
        CreditAgencyOne agency2 = new CreditAgencyOne(
                new StubDelayService(), containerService,
                abendHandler, new Random());
        CreditAgencyOne.ProcessingResult result2 = agency2.run(42L);

        CreditAgencyOne.ProcessingResult.Success success1 =
                (CreditAgencyOne.ProcessingResult.Success) result1;
        CreditAgencyOne.ProcessingResult.Success success2 =
                (CreditAgencyOne.ProcessingResult.Success) result2;
        assertEquals(success1.data().getCreditScore(),
                success2.data().getCreditScore(),
                "Same seed should produce same credit score");
    }

    @Test
    void runPreservesInputDataExceptCreditScore() {
        CreditCheckData inputData = createSampleData();
        containerService.setDataToReturn(inputData);

        CreditAgencyOne agency = createAgency();
        CreditAgencyOne.ProcessingResult result = agency.run(1L);

        CreditAgencyOne.ProcessingResult.Success success =
                (CreditAgencyOne.ProcessingResult.Success) result;
        CreditCheckData outputData = success.data();

        assertEquals("CUST", outputData.getEyecatcher());
        assertEquals("987654", outputData.getSortCode());
        assertEquals("0000012345", outputData.getNumber());
        assertEquals("John Doe", outputData.getName());
        assertEquals("123 Main Street", outputData.getAddress());
        assertEquals("15051990", outputData.getDateOfBirth());
        assertEquals("20230615", outputData.getCsReviewDate());
        assertEquals("Y", outputData.getSuccess());
        assertEquals(" ", outputData.getFailCode());
    }

    @Test
    void containerPutReceivesUpdatedCreditScore() {
        CreditCheckData inputData = createSampleData();
        inputData.setCreditScore(0);
        containerService.setDataToReturn(inputData);

        CreditAgencyOne agency = createAgency();
        agency.run(1L);

        CreditCheckData putData = containerService.getLastPutData();
        assertNotNull(putData);
        assertTrue(putData.getCreditScore() >= CreditCheckData.MIN_CREDIT_SCORE);
        assertTrue(putData.getCreditScore()
                <= CreditCheckData.MAX_CREDIT_SCORE);
    }

    @RepeatedTest(10)
    void delayServiceReceivesValueInExpectedRange() {
        containerService.setDataToReturn(createSampleData());

        Random freshRandom = new Random();
        CreditAgencyOne agency = new CreditAgencyOne(
                delayService, containerService, abendHandler, freshRandom);
        agency.run(System.nanoTime());

        int delayUsed = delayService.getLastDelaySeconds();
        assertTrue(delayUsed >= CreditAgencyOne.DELAY_MIN_SECONDS);
        assertTrue(delayUsed <= CreditAgencyOne.DELAY_MAX_SECONDS);
    }

    @Test
    void defaultConstructorCreatesValidInstance() {
        CreditAgencyOne agency = new CreditAgencyOne();
        assertNotNull(agency);
    }

    @Test
    void processingResultSealedInterfaceCoversAllCases() {
        CreditAgencyOne.ProcessingResult success =
                CreditAgencyOne.ProcessingResult.success(new CreditCheckData());
        CreditAgencyOne.ProcessingResult failed =
                CreditAgencyOne.ProcessingResult.failed(1, 2);
        CreditAgencyOne.ProcessingResult abended =
                CreditAgencyOne.ProcessingResult.abended("TEST");

        assertInstanceOf(
                CreditAgencyOne.ProcessingResult.Success.class, success);
        assertInstanceOf(
                CreditAgencyOne.ProcessingResult.Failed.class, failed);
        assertInstanceOf(
                CreditAgencyOne.ProcessingResult.Abended.class, abended);
    }

    @Test
    void processingResultPatternMatchingWithSwitch() {
        CreditCheckData data = createSampleData();
        CreditAgencyOne.ProcessingResult result =
                CreditAgencyOne.ProcessingResult.success(data);

        String description = switch (result) {
            case CreditAgencyOne.ProcessingResult.Success s ->
                    "Score: " + s.data().getCreditScore();
            case CreditAgencyOne.ProcessingResult.Failed f ->
                    "Failed: " + f.respCode();
            case CreditAgencyOne.ProcessingResult.Abended a ->
                    "Abended: " + a.abendCode();
        };

        assertTrue(description.startsWith("Score:"));
    }

    @Test
    void constantsMatchCobolValues() {
        assertEquals("CIPA", CreditAgencyOne.CONTAINER_NAME);
        assertEquals("CIPCREDCHANN", CreditAgencyOne.CHANNEL_NAME);
        assertEquals("CRDTAGY1", CreditAgencyOne.PROGRAM_NAME);
        assertEquals("PLOP", CreditAgencyOne.ABEND_CODE);
        assertEquals("ABNDPROC", CreditAgencyOne.ABEND_HANDLER_PROGRAM);
        assertEquals(1, CreditAgencyOne.DELAY_MIN_SECONDS);
        assertEquals(3, CreditAgencyOne.DELAY_MAX_SECONDS);
    }

    @Test
    void delayExceptionCarriesRespCodes() {
        CreditAgencyOne.DelayException ex =
                new CreditAgencyOne.DelayException(16, 42);
        assertEquals(16, ex.getRespCode());
        assertEquals(42, ex.getResp2Code());
        assertTrue(ex.getMessage().contains("16"));
        assertTrue(ex.getMessage().contains("42"));
    }

    @Test
    void containerExceptionCarriesRespCodes() {
        CreditAgencyOne.ContainerException ex =
                new CreditAgencyOne.ContainerException(22, 5);
        assertEquals(22, ex.getRespCode());
        assertEquals(5, ex.getResp2Code());
        assertTrue(ex.getMessage().contains("22"));
        assertTrue(ex.getMessage().contains("5"));
    }

    private CreditCheckData createSampleData() {
        CreditCheckData data = new CreditCheckData();
        data.setEyecatcher("CUST");
        data.setSortCode("987654");
        data.setNumber("0000012345");
        data.setName("John Doe");
        data.setAddress("123 Main Street");
        data.setDateOfBirth("15051990");
        data.setCreditScore(750);
        data.setCsReviewDate("20230615");
        data.setSuccess("Y");
        data.setFailCode(" ");
        return data;
    }

    // --- Stubs ---

    static class StubDelayService implements CreditAgencyOne.DelayService {

        private boolean shouldFail;
        private int failRespCode;
        private int failResp2Code;
        private boolean delayed;
        private int lastDelaySeconds;

        @Override
        public void delay(int seconds) throws CreditAgencyOne.DelayException {
            lastDelaySeconds = seconds;
            delayed = true;
            if (shouldFail) {
                throw new CreditAgencyOne.DelayException(
                        failRespCode, failResp2Code);
            }
        }

        void setShouldFail(boolean shouldFail, int respCode, int resp2Code) {
            this.shouldFail = shouldFail;
            this.failRespCode = respCode;
            this.failResp2Code = resp2Code;
        }

        boolean wasDelayed() {
            return delayed;
        }

        int getLastDelaySeconds() {
            return lastDelaySeconds;
        }
    }

    static class StubContainerService
            implements CreditAgencyOne.ContainerService {

        private CreditCheckData dataToReturn;
        private boolean getCalled;
        private boolean putCalled;
        private boolean getShouldFail;
        private int getFailRespCode;
        private int getFailResp2Code;
        private boolean putShouldFail;
        private int putFailRespCode;
        private int putFailResp2Code;
        private String lastGetContainerName;
        private String lastGetChannelName;
        private String lastPutContainerName;
        private String lastPutChannelName;
        private CreditCheckData lastPutData;

        @Override
        public CreditCheckData getContainer(String containerName,
                                            String channelName)
                throws CreditAgencyOne.ContainerException {
            getCalled = true;
            lastGetContainerName = containerName;
            lastGetChannelName = channelName;
            if (getShouldFail) {
                throw new CreditAgencyOne.ContainerException(
                        getFailRespCode, getFailResp2Code);
            }
            return dataToReturn;
        }

        @Override
        public void putContainer(String containerName, String channelName,
                                 CreditCheckData data)
                throws CreditAgencyOne.ContainerException {
            putCalled = true;
            lastPutContainerName = containerName;
            lastPutChannelName = channelName;
            lastPutData = data;
            if (putShouldFail) {
                throw new CreditAgencyOne.ContainerException(
                        putFailRespCode, putFailResp2Code);
            }
        }

        void setDataToReturn(CreditCheckData data) {
            this.dataToReturn = data;
        }

        void setGetShouldFail(boolean fail, int resp, int resp2) {
            this.getShouldFail = fail;
            this.getFailRespCode = resp;
            this.getFailResp2Code = resp2;
        }

        void setPutShouldFail(boolean fail, int resp, int resp2) {
            this.putShouldFail = fail;
            this.putFailRespCode = resp;
            this.putFailResp2Code = resp2;
        }

        boolean wasGetCalled() {
            return getCalled;
        }

        boolean wasPutCalled() {
            return putCalled;
        }

        String getLastGetContainerName() {
            return lastGetContainerName;
        }

        String getLastGetChannelName() {
            return lastGetChannelName;
        }

        String getLastPutContainerName() {
            return lastPutContainerName;
        }

        String getLastPutChannelName() {
            return lastPutChannelName;
        }

        CreditCheckData getLastPutData() {
            return lastPutData;
        }

        void resetCalls() {
            getCalled = false;
            putCalled = false;
            lastPutData = null;
        }
    }

    static class StubAbendHandler implements CreditAgencyOne.AbendHandler {

        private boolean handled;
        private String lastProgramName;
        private AbendInfo lastAbendInfo;

        @Override
        public void handleAbend(String programName, AbendInfo abendInfo) {
            handled = true;
            lastProgramName = programName;
            lastAbendInfo = abendInfo;
        }

        boolean wasHandled() {
            return handled;
        }

        String getLastProgramName() {
            return lastProgramName;
        }

        AbendInfo getLastAbendInfo() {
            return lastAbendInfo;
        }
    }
}
