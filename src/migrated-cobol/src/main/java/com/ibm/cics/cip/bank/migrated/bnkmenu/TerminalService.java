/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from COBOL: BNKMENU.cbl - CICS terminal I/O abstraction
 *
 * This interface abstracts the CICS terminal operations (SEND MAP, RECEIVE MAP,
 * SEND TEXT, RETURN TRANSID, etc.) so that the business logic in BnkMenu can be
 * tested independently of CICS infrastructure.
 */
package com.ibm.cics.cip.bank.migrated.bnkmenu;

public interface TerminalService {

    void sendMap(MenuScreenData screenData, SendMode mode)
            throws CicsOperationException;

    MenuScreenData receiveMap() throws CicsOperationException;

    void sendTerminationMessage(String message)
            throws CicsOperationException;

    void returnWithTransId(String transId)
            throws CicsOperationException;

    void returnImmediate(String transId)
            throws CicsOperationException;

    void sendControlErase() throws CicsOperationException;

    void returnToCics() throws CicsOperationException;

    void linkAbendHandler(AbndInfo abndInfo)
            throws CicsOperationException;

    void abendTask(String abendCode);

    String getApplId();

    int getTaskNumber();

    String getTransId();

    String getProgramName();

    int getCommAreaLength();
}
