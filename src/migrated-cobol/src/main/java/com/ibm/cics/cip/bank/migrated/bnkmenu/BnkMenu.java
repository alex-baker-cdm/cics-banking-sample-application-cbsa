/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from COBOL: BNKMENU.cbl
 * Original author: Jon Collett
 *
 * This is the BANK MENU program (the first program initiated by the BMS suite).
 * It displays the main menu map, allows the user to select an option, validates
 * the option number/letter, and returns with the appropriate transaction.
 *
 * Menu options:
 *   1 = Display/Delete/Update CUSTOMER information  (ODCS)
 *   2 = Display/Delete ACCOUNT information           (ODAC)
 *   3 = Create CUSTOMER                              (OCCS)
 *   4 = Create ACCOUNT                               (OCAC)
 *   5 = Update ACCOUNT                               (OUAC)
 *   6 = Credit/Debit funds to an ACCOUNT             (OCRA)
 *   7 = Transfer funds                               (OTFN)
 *   A = Look up Accounts with Customer Number        (OCCA)
 */
package com.ibm.cics.cip.bank.migrated.bnkmenu;

import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BnkMenu {

    private static final Logger LOGGER =
            Logger.getLogger(BnkMenu.class.getName());

    static final String PROGRAM_NAME = "BNKMENU";
    static final String RETURN_TRANS_ID = "OMEN";
    static final String ABEND_CODE = "HBNK";
    static final String ABEND_HANDLER_PROGRAM = "ABNDPROC";
    static final String END_OF_SESSION_MESSAGE = "Session Ended";
    static final String INVALID_KEY_MESSAGE = "Invalid key pressed.";
    static final String INVALID_ACTION_MESSAGE =
            "You must enter a valid value (1-7 or A).";

    private final TerminalService terminalService;
    private final MenuScreenData screenData;
    private final AbndInfo abndInfo;
    private boolean validData;
    private String actionInput;

    public BnkMenu(TerminalService terminalService) {
        this.terminalService = terminalService;
        this.screenData = new MenuScreenData();
        this.abndInfo = new AbndInfo();
        this.validData = true;
        this.actionInput = "";
    }

    public void processInput(AidKey aidKey) {
        switch (aidKey) {
            case ENTER -> processMenuMap();
            case PF3, PF12 -> handleTermination();
            case CLEAR -> handleClear();
            case PA1, PA2, PA3 -> { /* continue - no action */ }
            default -> handleInvalidKey();
        }
    }

    public void handleFirstTime() {
        screenData.clearFields();
        sendMap(SendMode.ERASE);
    }

    public void returnToMenu() {
        try {
            terminalService.returnWithTransId(RETURN_TRANS_ID);
        } catch (CicsOperationException e) {
            handleAbend("A010", "RETURN TRANSID(MENU) FAIL", e);
        }
    }

    void processMenuMap() {
        receiveMenuMap();
        editMenuData();

        if (validData) {
            invokeOtherTransaction();
        }

        sendMap(SendMode.DATAONLY_ALARM);
    }

    void receiveMenuMap() {
        try {
            MenuScreenData received = terminalService.receiveMap();
            actionInput = received.getAction();
        } catch (MapFailException e) {
            screenData.clearFields();
            sendMap(SendMode.ERASE);
        } catch (CicsOperationException e) {
            handleAbend("RMM010", "RECEIVE MAP FAIL", e);
        }
    }

    void editMenuData() {
        validData = true;

        if (actionInput == null || !MenuAction.isValidCode(actionInput)) {
            screenData.setMessage(INVALID_ACTION_MESSAGE);
            validData = false;
        }
    }

    void invokeOtherTransaction() {
        MenuAction action = MenuAction.fromCode(actionInput);
        if (action == null) {
            return;
        }

        try {
            terminalService.returnImmediate(action.getTransactionId());
        } catch (CicsOperationException e) {
            handleAbend("IOT010",
                    "RETURN TRANSID(" + action.getTransactionId() + ") FAIL",
                    e);
        }
    }

    void handleTermination() {
        sendTerminationMessage();
        try {
            terminalService.returnToCics();
        } catch (CicsOperationException e) {
            handleAbend("A010", "RETURN FAIL", e);
        }
    }

    void handleClear() {
        try {
            terminalService.sendControlErase();
            terminalService.returnToCics();
        } catch (CicsOperationException e) {
            handleAbend("A010", "CLEAR FAIL", e);
        }
    }

    void handleInvalidKey() {
        screenData.clearFields();
        screenData.setMessage(INVALID_KEY_MESSAGE);
        sendMap(SendMode.DATAONLY_ALARM);
    }

    void sendMap(SendMode mode) {
        try {
            terminalService.sendMap(screenData, mode);
        } catch (CicsOperationException e) {
            String modeLabel = switch (mode) {
                case ERASE -> "SEND MAP ERASE FAIL";
                case DATAONLY -> "SEND MAP DATAONLY FAIL";
                case DATAONLY_ALARM -> "SEND MAP DATAONLY ALARM FAIL";
            };
            handleAbend("SMM010", modeLabel, e);
        }
    }

    void sendTerminationMessage() {
        try {
            terminalService.sendTerminationMessage(END_OF_SESSION_MESSAGE);
        } catch (CicsOperationException e) {
            handleAbend("STM010", "SEND TEXT FAIL", e);
        }
    }

    void handleAbend(String section, String failDescription,
            CicsOperationException cause) {
        abndInfo.initialize();
        abndInfo.setRespCode(cause.getRespCode());
        abndInfo.setResp2Code(cause.getResp2Code());
        abndInfo.setApplId(terminalService.getApplId());
        abndInfo.setTaskNoKey(terminalService.getTaskNumber());
        abndInfo.setTranId(terminalService.getTransId());
        abndInfo.populateTimeDate(LocalDateTime.now());
        abndInfo.setCode(ABEND_CODE);
        abndInfo.setProgram(terminalService.getProgramName());
        abndInfo.setSqlCode(0);
        abndInfo.setFreeform(section + " - " + failDescription
                + " EIBRESP=" + cause.getRespCode()
                + " RESP2=" + cause.getResp2Code());

        try {
            terminalService.linkAbendHandler(abndInfo);
        } catch (CicsOperationException linkEx) {
            LOGGER.log(Level.SEVERE,
                    "Failed to link abend handler: " + linkEx.getMessage(),
                    linkEx);
        }

        String failMsg = PROGRAM_NAME + " - " + section
                + " - " + failDescription;
        LOGGER.log(Level.SEVERE, "{0} RESP={1} RESP2={2}",
                new Object[]{failMsg, cause.getRespCode(),
                        cause.getResp2Code()});

        abendTask();
    }

    void abendTask() {
        terminalService.abendTask(ABEND_CODE);
    }

    public MenuScreenData getScreenData() {
        return screenData;
    }

    public boolean isValidData() {
        return validData;
    }

    String getActionInput() {
        return actionInput;
    }

    void setActionInput(String actionInput) {
        this.actionInput = actionInput;
    }

    AbndInfo getAbndInfo() {
        return abndInfo;
    }
}
