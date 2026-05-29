/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from BNK1DAC.cbl — Display Account BMS screen controller.
 *
 * Original COBOL program: BNK1DAC
 * Author: Jon Collett
 * Transaction ID: ODAC
 * Map: BNK1DA / Mapset: BNK1DAM
 */
package com.ibm.cics.cip.bank.migrated.bnk1dac;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the Display Account screen in the CICS Banking Sample
 * Application. Migrated from COBOL program BNK1DAC.cbl.
 *
 * <p>This class preserves all business logic from the original COBOL program:
 * <ul>
 *   <li>First-time screen initialization with empty fields</li>
 *   <li>Account number input validation (EDIT-DATA section)</li>
 *   <li>Account inquiry via INQACC sub-program (GET-ACC-DATA section)</li>
 *   <li>Account deletion via DELACC sub-program (DEL-ACC-DATA section)</li>
 *   <li>Screen field population from inquiry results</li>
 *   <li>Session COMMAREA management for pseudo-conversational flow</li>
 *   <li>Key handling: Enter (inquiry), PF3 (exit), PF5 (delete),
 *       PF12 (cancel), PA keys (continue), CLEAR (erase)</li>
 * </ul>
 *
 * <p>COBOL section mapping:
 * <pre>
 *   PREMIERE      -> {@link #processRequest(AidKey, CommArea, ScreenField)}
 *   PROCESS-MAP   -> {@link #processMap(AidKey, CommArea, ScreenField)}
 *   EDIT-DATA     -> {@link #editData(ScreenField)}
 *   VALIDATE-DATA -> {@link #validateData(CommArea)}
 *   GET-ACC-DATA  -> {@link #getAccountData(ScreenField)}
 *   DEL-ACC-DATA  -> {@link #deleteAccountData(ScreenField)}
 *   SEND-MAP      -> (presentation concern — handled by caller)
 * </pre>
 */
public class DisplayAccountController {

    private static final Logger logger = Logger.getLogger(
            DisplayAccountController.class.getName());

    static final String MSG_ENTER_ACCOUNT_NUMBER =
            "Please enter an account number.";
    static final String MSG_ACCOUNT_NOT_FOUND =
            "Sorry, but that account number was not found.";
    static final String MSG_DELETE_PROMPT =
            "If you wish to delete the Account press <PF5>.";
    static final String MSG_DELETE_NOT_FOUND =
            "Sorry, but that account number was not found."
                    + " Account NOT deleted.";
    static final String MSG_DELETE_DATASTORE_ERROR =
            "Sorry, but a datastore error occurred."
                    + " Account NOT deleted.";
    static final String MSG_DELETE_ERROR =
            "Sorry, but a delete error occurred."
                    + " Account NOT deleted.";
    static final String MSG_INVALID_KEY =
            "Invalid key pressed.";
    static final String MSG_SESSION_ENDED =
            "Session Ended";
    static final String MSG_LOOKUP_SUCCESS =
            "Account lookup successful.";

    private final AccountService accountService;
    private AccountInquiryData lastInquiryResult;

    public DisplayAccountController(AccountService accountService) {
        this.accountService = Objects.requireNonNull(accountService,
                "accountService must not be null");
    }

    /**
     * Represents the AID (Attention Identifier) key pressed by the user,
     * corresponding to DFHAID values in the original COBOL.
     */
    public enum AidKey {
        ENTER, PF3, PF5, PF12, PA1, PA2, PA3, CLEAR, OTHER
    }

    /**
     * Send mode for the BMS map, corresponding to the SEND-FLAG in COBOL.
     */
    public enum SendMode {
        ERASE, DATAONLY, DATAONLY_ALARM
    }

    /**
     * Result of processing a screen request.
     */
    public record ProcessResult(
            SendMode sendMode,
            CommArea updatedCommArea,
            boolean terminateSession,
            boolean returnToMenu
    ) {
        public static ProcessResult sendErase(CommArea commArea) {
            return new ProcessResult(SendMode.ERASE, commArea, false, false);
        }

        public static ProcessResult sendDataOnlyAlarm(CommArea commArea) {
            return new ProcessResult(SendMode.DATAONLY_ALARM, commArea, false, false);
        }

        public static ProcessResult sendDataOnly(CommArea commArea) {
            return new ProcessResult(SendMode.DATAONLY, commArea, false, false);
        }

        public static ProcessResult ofTerminate() {
            return new ProcessResult(null, null, true, false);
        }

        public static ProcessResult ofReturnToMenu() {
            return new ProcessResult(null, null, false, true);
        }
    }

    /**
     * Main entry point — equivalent to PREMIERE SECTION (A010) in COBOL.
     *
     * <p>Handles the pseudo-conversational flow:
     * <ol>
     *   <li>If first time (commArea is null), send empty map</li>
     *   <li>Evaluate which AID key was pressed</li>
     *   <li>After processing, update the COMMAREA from inquiry results</li>
     * </ol>
     *
     * @param aidKey      the AID key pressed
     * @param commArea    the communication area (null on first entry)
     * @param screenField the screen field data (input from terminal)
     * @return processing result indicating how to send the response
     */
    public ProcessResult processRequest(AidKey aidKey, CommArea commArea,
                                        ScreenField screenField) {
        logger.log(Level.FINE, "processRequest: aidKey={0}, commArea={1}",
                new Object[]{aidKey, commArea});

        lastInquiryResult = null;

        if (commArea == null) {
            screenField.clear();
            CommArea newComm = new CommArea();
            return ProcessResult.sendErase(newComm);
        }

        ProcessResult result;
        switch (aidKey) {
            case PA1, PA2, PA3 -> {
                result = ProcessResult.sendDataOnly(
                        buildUpdatedCommArea(commArea));
            }
            case PF3 -> {
                return ProcessResult.ofReturnToMenu();
            }
            case PF5 -> {
                processMap(aidKey, commArea, screenField);
                result = ProcessResult.sendDataOnlyAlarm(
                        buildUpdatedCommArea(commArea));
            }
            case PF12 -> {
                screenField.setMessage(MSG_SESSION_ENDED);
                return ProcessResult.ofTerminate();
            }
            case CLEAR -> {
                screenField.clear();
                return ProcessResult.ofTerminate();
            }
            case ENTER -> {
                processMap(aidKey, commArea, screenField);
                result = ProcessResult.sendDataOnlyAlarm(
                        buildUpdatedCommArea(commArea));
            }
            default -> {
                screenField.clear();
                screenField.setMessage(MSG_INVALID_KEY);
                result = ProcessResult.sendDataOnlyAlarm(
                        buildUpdatedCommArea(commArea));
            }
        }

        return result;
    }

    /**
     * Equivalent to PROCESS-MAP SECTION (PM010) in COBOL.
     *
     * <p>For ENTER: validates input then retrieves account data.
     * For PF5: validates COMMAREA then deletes the account.
     */
    void processMap(AidKey aidKey, CommArea commArea,
                    ScreenField screenField) {
        if (aidKey == AidKey.ENTER) {
            boolean valid = editData(screenField);
            if (valid) {
                getAccountData(screenField);
            }
        } else if (aidKey == AidKey.PF5) {
            boolean valid = validateData(commArea);
            if (!valid) {
                screenField.setMessage(MSG_ENTER_ACCOUNT_NUMBER);
            } else {
                deleteAccountData(screenField);
            }
        }
    }

    /**
     * Equivalent to EDIT-DATA SECTION (ED010) in COBOL.
     *
     * <p>Validates the account number input field:
     * <ul>
     *   <li>Must not be empty/blank</li>
     *   <li>Must be numeric</li>
     * </ul>
     *
     * @param screenField the screen fields containing user input
     * @return true if data is valid
     */
    boolean editData(ScreenField screenField) {
        String accInput = screenField.getAccountNumberInput();

        if (accInput == null || accInput.isBlank()) {
            screenField.setMessage(MSG_ENTER_ACCOUNT_NUMBER);
            return false;
        }

        String stripped = accInput.strip();
        if (!stripped.matches("\\d+")) {
            screenField.setMessage(MSG_ENTER_ACCOUNT_NUMBER);
            return false;
        }

        return true;
    }

    /**
     * Equivalent to VALIDATE-DATA SECTION (VD010) in COBOL.
     *
     * <p>Validates the COMMAREA has a non-zero sort code and account number,
     * used before allowing a delete operation.
     *
     * @param commArea the communication area
     * @return true if the data is valid for deletion
     */
    boolean validateData(CommArea commArea) {
        if (commArea == null) {
            return false;
        }
        String sortCode = commArea.getSortCode();
        int accNo = commArea.getAccountNumber();

        boolean sortCodeInvalid = sortCode == null
                || sortCode.isBlank()
                || sortCode.equals("000000");
        boolean accNoInvalid = accNo == 0;

        return !sortCodeInvalid && !accNoInvalid;
    }

    /**
     * Equivalent to GET-ACC-DATA SECTION (GAD010) in COBOL.
     *
     * <p>Calls the INQACC sub-program (via {@link AccountService#inquireAccount})
     * and populates the screen fields with the returned data.
     * If the account is not found, clears screen fields and sets an error message.
     */
    void getAccountData(ScreenField screenField) {
        int accountNumber;
        try {
            accountNumber = Integer.parseInt(
                    screenField.getAccountNumberInput().strip());
        } catch (NumberFormatException e) {
            screenField.setMessage(MSG_ENTER_ACCOUNT_NUMBER);
            return;
        }

        AccountInquiryData inquiry = accountService.inquireAccount(accountNumber);
        lastInquiryResult = inquiry;

        if (!inquiry.isAccountFound()) {
            screenField.setMessage(MSG_ACCOUNT_NOT_FOUND);
            clearAccountFields(screenField);
            return;
        }

        populateScreenFromInquiry(screenField, inquiry);
        screenField.setMessage(MSG_DELETE_PROMPT);
    }

    /**
     * Equivalent to DEL-ACC-DATA SECTION (DAD010) in COBOL.
     *
     * <p>Calls the DELACC sub-program (via {@link AccountService#deleteAccount})
     * and handles the result. On success, clears all fields and displays
     * a success message. On failure, displays the appropriate error message.
     */
    void deleteAccountData(ScreenField screenField) {
        int accountNumber;
        try {
            accountNumber = Integer.parseInt(
                    screenField.getAccountNumberDisplay().strip());
        } catch (NumberFormatException e) {
            screenField.setMessage(MSG_ENTER_ACCOUNT_NUMBER);
            return;
        }

        AccountDeleteResult result = accountService.deleteAccount(accountNumber);

        if (!result.successful()) {
            String msg = switch (result.failCode()) {
                case AccountDeleteResult.FAIL_NOT_FOUND -> MSG_DELETE_NOT_FOUND;
                case AccountDeleteResult.FAIL_DATASTORE_ERROR -> MSG_DELETE_DATASTORE_ERROR;
                case AccountDeleteResult.FAIL_DELETE_ERROR -> MSG_DELETE_ERROR;
                default -> MSG_DELETE_ERROR;
            };
            screenField.setMessage(msg);
            screenField.setSortCode(result.sortCode());
            return;
        }

        clearAccountFields(screenField);
        screenField.setMessage(
                "Account " + accountNumber + " was successfully deleted.");
    }

    /**
     * Populates screen fields from an inquiry result.
     * Equivalent to the field-move block in GAD010 lines 664-696.
     */
    void populateScreenFromInquiry(ScreenField screenField,
                                   AccountInquiryData inquiry) {
        screenField.setSortCode(inquiry.getSortCode());
        screenField.setCustomerNumber(inquiry.getCustomerNumber());
        screenField.setAccountNumberDisplay(
                String.valueOf(inquiry.getAccountNumber()));
        screenField.setAccountType(inquiry.getAccountType());
        screenField.setInterestRate(inquiry.getInterestRate());

        splitDateToScreen(inquiry.getOpened(),
                screenField::setOpenedDay,
                screenField::setOpenedMonth,
                screenField::setOpenedYear);

        screenField.setOverdraft(String.valueOf(inquiry.getOverdraft()));

        splitDateToScreen(inquiry.getLastStatementDate(),
                screenField::setLastStatementDay,
                screenField::setLastStatementMonth,
                screenField::setLastStatementYear);

        splitDateToScreen(inquiry.getNextStatementDate(),
                screenField::setNextStatementDay,
                screenField::setNextStatementMonth,
                screenField::setNextStatementYear);

        screenField.setAvailableBalance(
                formatBalance(inquiry.getAvailableBalance()));
        screenField.setActualBalance(
                formatBalance(inquiry.getActualBalance()));
    }

    /**
     * Splits a DDMMYYYY integer date into day, month, year strings.
     * Mirrors the COBOL COMM-OPENED-SPLIT / COMM-LAST-ST-SPLIT /
     * COMM-NEXT-ST-SPLIT redefines.
     */
    static void splitDateToScreen(int ddmmyyyy,
                                  java.util.function.Consumer<String> daySetter,
                                  java.util.function.Consumer<String> monthSetter,
                                  java.util.function.Consumer<String> yearSetter) {
        if (ddmmyyyy <= 0) {
            daySetter.accept("");
            monthSetter.accept("");
            yearSetter.accept("");
            return;
        }
        String padded = String.format("%08d", ddmmyyyy);
        daySetter.accept(padded.substring(0, 2));
        monthSetter.accept(padded.substring(2, 4));
        yearSetter.accept(padded.substring(4, 8));
    }

    /**
     * Formats a balance value for screen display.
     * Mirrors COBOL AVAILABLE-BALANCE-DISPLAY PIC +9(10).99.
     */
    static String formatBalance(BigDecimal balance) {
        if (balance == null) {
            return "+0000000000.00";
        }
        BigDecimal scaled = balance.setScale(2, RoundingMode.HALF_UP);
        String sign = scaled.signum() >= 0 ? "+" : "";
        return sign + scaled.toPlainString();
    }

    /**
     * Clears all account display fields on the screen.
     * Used when account is not found or after successful deletion.
     */
    void clearAccountFields(ScreenField screenField) {
        screenField.setSortCode("");
        screenField.setCustomerNumber("");
        screenField.setAccountNumberDisplay("");
        screenField.setAccountType("");
        screenField.setInterestRate(BigDecimal.ZERO);
        screenField.setOpenedDay("");
        screenField.setOpenedMonth("");
        screenField.setOpenedYear("");
        screenField.setOverdraft("");
        screenField.setLastStatementDay("");
        screenField.setLastStatementMonth("");
        screenField.setLastStatementYear("");
        screenField.setNextStatementDay("");
        screenField.setNextStatementMonth("");
        screenField.setNextStatementYear("");
        screenField.setAvailableBalance(formatBalance(BigDecimal.ZERO));
        screenField.setActualBalance(formatBalance(BigDecimal.ZERO));
    }

    /**
     * Builds an updated COMMAREA from the last inquiry result.
     * Equivalent to the MOVE block after EVALUATE in A010 (lines 274-293).
     */
    CommArea buildUpdatedCommArea(CommArea previousCommArea) {
        if (lastInquiryResult != null && lastInquiryResult.isEyeValid()) {
            CommArea updated = new CommArea();
            updated.setEye(lastInquiryResult.getEye());
            updated.setCustomerNumber(lastInquiryResult.getCustomerNumber());
            updated.setSortCode(lastInquiryResult.getSortCode());
            updated.setAccountNumber(lastInquiryResult.getAccountNumber());
            updated.setAccountType(lastInquiryResult.getAccountType());
            updated.setInterestRate(lastInquiryResult.getInterestRate());
            updated.setOpened(lastInquiryResult.getOpened());
            updated.setOverdraft(lastInquiryResult.getOverdraft());
            updated.setLastStatementDate(lastInquiryResult.getLastStatementDate());
            updated.setNextStatementDate(lastInquiryResult.getNextStatementDate());
            updated.setAvailableBalance(lastInquiryResult.getAvailableBalance());
            updated.setActualBalance(lastInquiryResult.getActualBalance());
            updated.setSuccess(lastInquiryResult.getSuccess());
            return updated;
        }
        return new CommArea();
    }

    AccountInquiryData getLastInquiryResult() {
        return lastInquiryResult;
    }
}
