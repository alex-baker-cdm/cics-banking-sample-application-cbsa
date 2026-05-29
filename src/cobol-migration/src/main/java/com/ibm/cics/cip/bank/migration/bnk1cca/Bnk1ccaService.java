/*
 * Copyright IBM Corp. 2023
 *
 * Java 21 migration of BNK1CCA.cbl - Customer Account Lookup BMS screen.
 *
 * Original COBOL program: src/base/cobol_src/BNK1CCA.cbl
 * Author: James O'Grady
 *
 * This program lists accounts belonging to a specified customer number.
 * It receives a customer number from the BNK1ACC BMS map, validates it,
 * calls the INQACCCU sub-program to retrieve account data, and formats
 * the results for display on the terminal screen.
 */
package com.ibm.cics.cip.bank.migration.bnk1cca;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Service encapsulating all business logic from the BNK1CCA COBOL program.
 *
 * <p>The original COBOL program is a pseudo-conversational CICS BMS transaction
 * (transaction ID OCCA) that:
 * <ul>
 *   <li>Displays an empty customer number input screen on first invocation</li>
 *   <li>Validates the customer number is numeric</li>
 *   <li>Links to the INQACCCU program to retrieve accounts for the customer</li>
 *   <li>Formats and displays up to 10 accounts with sort code, account number,
 *       account type, available balance, and actual balance</li>
 *   <li>Handles terminal key presses (PF3=Exit, PF12=Cancel, PA=Continue,
 *       CLEAR=Erase, ENTER=Process)</li>
 * </ul>
 */
public class Bnk1ccaService {

    private static final Logger logger = Logger.getLogger(Bnk1ccaService.class.getName());

    private static final int MAX_INQUIRY_ACCOUNTS = 20;
    private static final String POSITIVE_SIGN = "+";
    private static final String NEGATIVE_SIGN = "-";

    private final AccountInquiryService accountInquiryService;

    public Bnk1ccaService(AccountInquiryService accountInquiryService) {
        this.accountInquiryService = accountInquiryService;
    }

    /**
     * Main entry point - processes a user interaction with the BNK1ACC screen.
     * Maps to the PREMIERE SECTION / A010 paragraph in the COBOL program.
     *
     * @param aidKey         the terminal key pressed by the user
     * @param customerNumber the customer number entered on the screen (may be null on first entry)
     * @param commArea       the communication area from the previous invocation, or null on first entry
     * @return the screen output describing what to display and what action to take
     */
    public ScreenOutput processInput(AidKey aidKey, String customerNumber, CommArea commArea) {
        boolean firstTime = (commArea == null);

        if (firstTime) {
            return buildEmptyScreen();
        }

        return switch (aidKey) {
            case PA1, PA2, PA3 -> buildNoActionOutput();
            case PF3 -> buildReturnToMenu();
            case PF12, AID -> buildTerminateSession();
            case CLEAR -> buildClearScreen();
            case ENTER -> processMap(customerNumber);
            default -> buildInvalidKeyOutput();
        };
    }

    /**
     * Processes the map input when ENTER is pressed.
     * Maps to the PROCESS-MAP SECTION / PM010 paragraph.
     *
     * @param customerNumber the raw customer number from the screen input
     * @return the screen output with account data or error message
     */
    ScreenOutput processMap(String customerNumber) {
        String validationMessage = editData(customerNumber);
        if (validationMessage != null) {
            return new ScreenOutput(
                    validationMessage,
                    emptyAccountLines(),
                    ScreenAction.SEND_DATAONLY_ALARM
            );
        }

        return getCustomerData(customerNumber);
    }

    /**
     * Validates the incoming customer number field.
     * Maps to the EDIT-DATA SECTION / ED010 paragraph.
     *
     * @param customerNumber the customer number input to validate
     * @return null if valid, or an error message string if invalid
     */
    String editData(String customerNumber) {
        if (customerNumber == null || customerNumber.isBlank() || !isNumeric(customerNumber)) {
            return "Please enter a customer number.";
        }
        return null;
    }

    /**
     * Retrieves and formats customer account data.
     * Maps to the GET-CUST-DATA SECTION / GCD010 paragraph.
     *
     * @param customerNumber the validated customer number
     * @return screen output with formatted account lines or an error/info message
     */
    ScreenOutput getCustomerData(String customerNumber) {
        AccountInquiryResult result = accountInquiryService.inquireAccountsByCustomer(
                customerNumber, MAX_INQUIRY_ACCOUNTS);

        if (!result.customerFound()) {
            return new ScreenOutput(
                    "Unable to find customer " + customerNumber,
                    emptyAccountLines(),
                    ScreenAction.SEND_DATAONLY_ALARM
            );
        }

        List<String> accountLines = emptyAccountLines();

        if (result.numberOfAccounts() == 0) {
            return new ScreenOutput(
                    "No accounts found for customer",
                    accountLines,
                    ScreenAction.SEND_DATAONLY_ALARM
            );
        }

        if (!result.success()) {
            return new ScreenOutput(
                    "Error accessing accounts for customer " + customerNumber + ".",
                    accountLines,
                    ScreenAction.SEND_DATAONLY_ALARM
            );
        }

        int displayCount = Math.min(result.numberOfAccounts(), ScreenOutput.MAX_DISPLAY_ACCOUNTS);
        displayCount = Math.min(displayCount, result.accountDetails().size());

        for (int i = 0; i < displayCount; i++) {
            accountLines.set(i, formatAccountLine(result.accountDetails().get(i)));
        }

        String countDisplay = formatAccountCount(result.numberOfAccounts());
        String message = countDisplay + " accounts found";

        return new ScreenOutput(message, accountLines, ScreenAction.SEND_DATAONLY_ALARM);
    }

    /**
     * Formats a single account detail into a display line matching the original
     * COBOL STRING output in the GCD010 paragraph.
     *
     * <p>Format: {@code SCODE      ACCNO         ACC-TYPE       +/-PPPPPPPPPP.CC  +/-PPPPPPPPPP.CC}
     *
     * @param detail the account detail to format
     * @return the formatted display line
     */
    String formatAccountLine(AccountDetail detail) {
        String sortCode = padRight(detail.sortCode(), 6);
        String accountNumber = padRight(String.valueOf(detail.accountNumber()), 8);
        String accountType = padRight(detail.accountType(), 8);

        String availBalSign = detail.availableBalance().compareTo(BigDecimal.ZERO) < 0
                ? NEGATIVE_SIGN : POSITIVE_SIGN;
        String actBalSign = detail.actualBalance().compareTo(BigDecimal.ZERO) < 0
                ? NEGATIVE_SIGN : POSITIVE_SIGN;

        String availBalFormatted = formatBalance(detail.availableBalance());
        String actBalFormatted = formatBalance(detail.actualBalance());

        return sortCode
                + "      "
                + accountNumber
                + "         "
                + accountType
                + "       "
                + availBalSign
                + availBalFormatted
                + "  "
                + actBalSign
                + actBalFormatted;
    }

    /**
     * Formats a balance value to match the COBOL PIC 9(10)V99 representation:
     * 10-digit integer part followed by "." and 2-digit decimal part.
     */
    String formatBalance(BigDecimal balance) {
        BigDecimal absolute = balance.abs();
        long integerPart = absolute.longValue();
        int decimalPart = absolute.remainder(BigDecimal.ONE)
                .movePointRight(2)
                .intValue();

        String intStr = padLeft(String.valueOf(integerPart), 10, '0');
        String decStr = padLeft(String.valueOf(decimalPart), 2, '0');
        return intStr + "." + decStr;
    }

    /**
     * Formats the account count with leading-space suppression, matching the
     * COBOL PIC Z9 DISPLAY format (space-padded, 2 chars).
     */
    String formatAccountCount(int count) {
        if (count < 10) {
            return " " + count;
        }
        return String.valueOf(count);
    }

    private ScreenOutput buildEmptyScreen() {
        return new ScreenOutput("", emptyAccountLines(), ScreenAction.SEND_ERASE);
    }

    private ScreenOutput buildNoActionOutput() {
        return new ScreenOutput(null, null, ScreenAction.NO_ACTION);
    }

    private ScreenOutput buildReturnToMenu() {
        return new ScreenOutput(null, null, ScreenAction.RETURN_TO_MENU);
    }

    private ScreenOutput buildTerminateSession() {
        return new ScreenOutput(
                ScreenOutput.END_OF_SESSION_MESSAGE,
                null,
                ScreenAction.TERMINATE_SESSION
        );
    }

    private ScreenOutput buildClearScreen() {
        return new ScreenOutput(null, null, ScreenAction.CLEAR_SCREEN);
    }

    private ScreenOutput buildInvalidKeyOutput() {
        return new ScreenOutput(
                "Invalid key pressed.",
                emptyAccountLines(),
                ScreenAction.SEND_DATAONLY_ALARM
        );
    }

    private static List<String> emptyAccountLines() {
        List<String> lines = new ArrayList<>(ScreenOutput.MAX_DISPLAY_ACCOUNTS);
        for (int i = 0; i < ScreenOutput.MAX_DISPLAY_ACCOUNTS; i++) {
            lines.add("");
        }
        return lines;
    }

    private static boolean isNumeric(String str) {
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static String padRight(String str, int length) {
        if (str == null) {
            return " ".repeat(length);
        }
        if (str.length() >= length) {
            return str.substring(0, length);
        }
        return str + " ".repeat(length - str.length());
    }

    private static String padLeft(String str, int length, char padChar) {
        if (str.length() >= length) {
            return str.substring(str.length() - length);
        }
        return String.valueOf(padChar).repeat(length - str.length()) + str;
    }
}
