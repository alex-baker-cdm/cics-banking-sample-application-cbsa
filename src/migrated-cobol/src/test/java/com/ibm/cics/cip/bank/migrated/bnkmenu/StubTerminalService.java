/*
 * Copyright IBM Corp. 2023
 */
package com.ibm.cics.cip.bank.migrated.bnkmenu;

class StubTerminalService implements TerminalService {

    int sendMapCount;
    SendMode lastSendMode;
    boolean sendMapShouldFail;

    int receiveMapCount;
    String receiveActionInput = "";
    boolean receiveMapFailure;
    boolean receiveMapFatalFailure;

    String lastTerminationMessage;
    boolean sendTerminationShouldFail;

    String lastReturnTransId;
    boolean returnWithTransIdShouldFail;

    int returnImmediateCount;
    String lastReturnImmediateTransId;
    boolean returnImmediateShouldFail;

    boolean sendControlEraseCalled;
    boolean returnToCicsCalled;

    AbndInfo lastAbndInfo;
    boolean abendTaskCalled;
    String lastAbendCode;

    @Override
    public void sendMap(MenuScreenData screenData, SendMode mode)
            throws CicsOperationException {
        if (sendMapShouldFail) {
            throw new CicsOperationException("SEND MAP failed", 99, 88);
        }
        sendMapCount++;
        lastSendMode = mode;
    }

    @Override
    public MenuScreenData receiveMap() throws CicsOperationException {
        receiveMapCount++;
        if (receiveMapFailure) {
            throw new MapFailException(36, 0);
        }
        if (receiveMapFatalFailure) {
            throw new CicsOperationException("RECEIVE MAP failed", 99, 88);
        }
        MenuScreenData data = new MenuScreenData();
        data.setAction(receiveActionInput);
        return data;
    }

    @Override
    public void sendTerminationMessage(String message)
            throws CicsOperationException {
        if (sendTerminationShouldFail) {
            throw new CicsOperationException("SEND TEXT failed", 99, 88);
        }
        lastTerminationMessage = message;
    }

    @Override
    public void returnWithTransId(String transId)
            throws CicsOperationException {
        if (returnWithTransIdShouldFail) {
            throw new CicsOperationException(
                    "RETURN TRANSID failed", 99, 88);
        }
        lastReturnTransId = transId;
    }

    @Override
    public void returnImmediate(String transId)
            throws CicsOperationException {
        if (returnImmediateShouldFail) {
            throw new CicsOperationException(
                    "RETURN IMMEDIATE failed", 99, 88);
        }
        returnImmediateCount++;
        lastReturnImmediateTransId = transId;
    }

    @Override
    public void sendControlErase() throws CicsOperationException {
        sendControlEraseCalled = true;
    }

    @Override
    public void returnToCics() throws CicsOperationException {
        returnToCicsCalled = true;
    }

    @Override
    public void linkAbendHandler(AbndInfo abndInfo)
            throws CicsOperationException {
        lastAbndInfo = abndInfo;
    }

    @Override
    public void abendTask(String abendCode) {
        abendTaskCalled = true;
        lastAbendCode = abendCode;
    }

    @Override
    public String getApplId() {
        return "TESTAPPL";
    }

    @Override
    public int getTaskNumber() {
        return 12345;
    }

    @Override
    public String getTransId() {
        return "OMEN";
    }

    @Override
    public String getProgramName() {
        return "BNKMENU";
    }

    @Override
    public int getCommAreaLength() {
        return 1;
    }
}
