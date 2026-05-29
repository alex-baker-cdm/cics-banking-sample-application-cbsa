/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from COBOL: src/base/cobol_src/BNK1UAC.cbl
 * Program: BNK1UAC — Update Account (BMS Screen)
 * Author: Jon Collett (original COBOL)
 *
 * This class preserves all business logic from the original COBOL program
 * including validation rules, account inquiry, account update, date validation,
 * and the balance conversion logic from screen format to numeric.
 *
 * COBOL sections mapped to Java methods:
 *   PREMIERE          -> handleTransaction()
 *   PROCESS-MAP       -> processMap()
 *   EDIT-DATA         -> editData()
 *   VALIDATE-DATA     -> validateData()
 *   INQ-ACC-DATA      -> inquireAccountData()
 *   UPD-ACC-DATA      -> updateAccountData()
 *   SEND-MAP          -> (handled by caller via UpdateAccountOutput)
 *   SEND-TERMINATION  -> (returns terminal message)
 *
 * COBOL AID key handling:
 *   ENTER  -> AidKey.ENTER  (inquiry flow)
 *   PF5    -> AidKey.PF5    (update flow)
 *   PF3    -> AidKey.PF3    (return to main menu)
 *   PF12   -> AidKey.PF12   (session termination)
 *   CLEAR  -> AidKey.CLEAR  (clear screen)
 *   PA1-3  -> AidKey.PA     (no action)
 */
package com.ibm.cics.cip.bank.migrated.bnk1uac;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;
import java.util.logging.Logger;

public final class Bnk1uac {

    private static final Logger LOG = Logger.getLogger(Bnk1uac.class.getName());
    private static final String END_OF_SESSION_MESSAGE = "Session Ended";
    private static final BigDecimal MAX_INTEREST_RATE = new BigDecimal("9999.99");
    private static final Set<String> VALID_ACCOUNT_TYPES = Set.of(
            "CURRENT", "SAVING", "LOAN", "MORTGAGE", "ISA"
    );

    public enum AidKey {
        ENTER, PF3, PF5, PF12, CLEAR, PA, OTHER
    }

    public sealed interface TransactionResult permits
            TransactionResult.SendMap,
            TransactionResult.ReturnToMenu,
            TransactionResult.SessionTerminated,
            TransactionResult.ScreenCleared,
            TransactionResult.Continue {

        record SendMap(UpdateAccountOutput output, SendMode sendMode) implements TransactionResult {}
        record ReturnToMenu(String transactionId) implements TransactionResult {}
        record SessionTerminated(String message) implements TransactionResult {}
        record ScreenCleared() implements TransactionResult {}
        record Continue() implements TransactionResult {}
    }

    public enum SendMode {
        ERASE,
        DATA_ONLY,
        DATA_ONLY_ALARM
    }

    private final AccountService accountService;

    public Bnk1uac(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * Main entry point — maps COBOL PREMIERE SECTION.
     *
     * @param aidKey        the AID key pressed by the user
     * @param isFirstTime   true if EIBCALEN = 0 (first invocation)
     * @param input         the screen input data (may be null on first time)
     * @return the transaction result indicating what the caller should do
     */
    public TransactionResult handleTransaction(AidKey aidKey,
                                               boolean isFirstTime,
                                               UpdateAccountInput input) {
        if (isFirstTime) {
            UpdateAccountOutput output = new UpdateAccountOutput();
            return new TransactionResult.SendMap(output, SendMode.ERASE);
        }

        return switch (aidKey) {
            case PA -> new TransactionResult.Continue();
            case PF3 -> new TransactionResult.ReturnToMenu("OMEN");
            case PF12 -> new TransactionResult.SessionTerminated(END_OF_SESSION_MESSAGE);
            case CLEAR -> new TransactionResult.ScreenCleared();
            case ENTER, PF5 -> processMap(aidKey, input);
            case OTHER -> {
                UpdateAccountOutput output = new UpdateAccountOutput();
                output.setMessage("Invalid key pressed.");
                yield new TransactionResult.SendMap(output, SendMode.DATA_ONLY_ALARM);
            }
        };
    }

    /**
     * Maps COBOL PROCESS-MAP SECTION.
     */
    TransactionResult processMap(AidKey aidKey, UpdateAccountInput input) {
        UpdateAccountOutput output = new UpdateAccountOutput();
        boolean validData = true;

        if (aidKey == AidKey.ENTER) {
            String editMessage = editData(input);
            if (editMessage != null) {
                output.setMessage(editMessage);
                validData = false;
            }

            if (validData) {
                inquireAccountData(input, output);
            }
        }

        if (aidKey == AidKey.PF5) {
            String validateMessage = validateData(input);
            if (validateMessage != null) {
                output.setMessage(validateMessage);
                validData = false;
            }

            if (validData) {
                updateAccountData(input, output);
            }
        }

        return new TransactionResult.SendMap(output, SendMode.DATA_ONLY_ALARM);
    }

    /**
     * Maps COBOL EDIT-DATA SECTION (ED010).
     * Validates the account number is numeric.
     *
     * @return error message if invalid, null if valid
     */
    String editData(UpdateAccountInput input) {
        String accountNumber = input.getAccountNumber().value();
        if (!isNumeric(accountNumber)) {
            return "Please enter an account number.";
        }
        return null;
    }

    /**
     * Maps COBOL VALIDATE-DATA SECTION (VD010).
     * Performs comprehensive validation on all updatable fields.
     *
     * @return error message if invalid, null if valid
     */
    String validateData(UpdateAccountInput input) {
        String accountTypeValidation = validateAccountType(input);
        if (accountTypeValidation != null) {
            return accountTypeValidation;
        }

        String interestRateValidation = validateInterestRate(input);
        if (interestRateValidation != null) {
            return interestRateValidation;
        }

        String overdraftValidation = validateOverdraft(input);
        if (overdraftValidation != null) {
            return overdraftValidation;
        }

        String lastStmtValidation = validateLastStatementDate(input);
        if (lastStmtValidation != null) {
            return lastStmtValidation;
        }

        String nextStmtValidation = validateNextStatementDate(input);
        if (nextStmtValidation != null) {
            return nextStmtValidation;
        }

        return null;
    }

    /**
     * Validates account type is one of: CURRENT, SAVING, LOAN, MORTGAGE, ISA.
     */
    String validateAccountType(UpdateAccountInput input) {
        String accountType = input.getAccountType().value();
        if (accountType == null) {
            return "Account Type must be CURRENT, SAVING, LOAN, MORTGAGE or ISA. Then press PF5.";
        }

        String trimmed = accountType.trim();
        if (!VALID_ACCOUNT_TYPES.contains(trimmed)) {
            return "Account Type must be CURRENT, SAVING, LOAN, MORTGAGE or ISA. Then press PF5.";
        }
        return null;
    }

    /**
     * Validates interest rate with the same rules as the COBOL VALIDATE-DATA section:
     * - Must be non-empty
     * - Must contain only digits, '.', '-', '+', and spaces
     * - At most one decimal point
     * - At most two decimal places
     * - Value must be >= 0 and <= 9999.99
     * - LOAN and MORTGAGE types cannot have zero interest
     */
    String validateInterestRate(UpdateAccountInput input) {
        ScreenField interestField = input.getInterestRate();

        if (interestField.length() == 0) {
            return "Please supply a numeric interest rate then press PF5.";
        }

        String rateStr = interestField.value().substring(0,
                Math.min(interestField.length(), interestField.value().length()));

        if (!isNumeric(rateStr)) {
            int numCountTotal = countValidChars(rateStr);

            if (numCountTotal < interestField.length()) {
                return "Please supply a numeric interest rate";
            }

            long pointCount = countDecimalPoints(rateStr);

            if (pointCount > 1) {
                return "Use one decimal point for interest rate only";
            }

            if (pointCount == 1) {
                int charsAfterPoint = countCharsAfterDecimalPoint(rateStr);
                if (charsAfterPoint > 2) {
                    int digitsAfterPoint = countDigitsAfterDecimalPoint(rateStr);
                    if (digitsAfterPoint > 2) {
                        return "Only up to two decimal places are supported";
                    }
                }
            }
        }

        BigDecimal rateValue;
        try {
            rateValue = parseNumericValue(rateStr);
        } catch (NumberFormatException e) {
            return "Please supply a numeric interest rate";
        }

        if (rateValue.compareTo(BigDecimal.ZERO) < 0) {
            return "Please supply a zero or positive interest rate";
        }

        if (rateValue.compareTo(MAX_INTEREST_RATE) > 0) {
            return "Please supply an interest rate less than 9999.99%";
        }

        String accountType = input.getAccountType().value();
        if (accountType != null) {
            String trimmedType = accountType.trim();
            boolean isZeroRate = rateValue.compareTo(BigDecimal.ZERO) == 0;

            if (isZeroRate && ("LOAN".equals(trimmedType) || "MORTGAGE".equals(trimmedType))) {
                return "Interest rate cannot be 0 with this account type. Correct and press PF5.";
            }
        }

        return null;
    }

    /**
     * Validates the overdraft field is numeric and non-empty.
     */
    String validateOverdraft(UpdateAccountInput input) {
        ScreenField overdraftField = input.getOverdraft();

        if (overdraftField.length() == 0) {
            return "Overdraft must be numeric. Correct and press PF5.";
        }

        String overdraftStr = overdraftField.value().substring(0,
                Math.min(overdraftField.length(), overdraftField.value().length()));

        if (!isNumeric(overdraftStr)) {
            return "Overdraft must be numeric. Correct and press PF5.";
        }

        return null;
    }

    /**
     * Validates the last statement date components are numeric and form a valid date.
     */
    String validateLastStatementDate(UpdateAccountInput input) {
        if (!isNumeric(input.getLastStatementDay().value())
                || !isNumeric(input.getLastStatementMonth().value())
                || !isNumeric(input.getLastStatementYear().value())) {
            return "Last statement date must be numeric";
        }

        int day = Integer.parseInt(input.getLastStatementDay().value().trim());
        int month = Integer.parseInt(input.getLastStatementMonth().value().trim());

        if (day > 31 || day == 0 || month > 12 || month == 0) {
            return "Incorrect date for LAST STATEMENT.";
        }

        if (!isValidDayForMonth(day, month)) {
            return "Incorrect date for LAST STATEMENT.";
        }

        return null;
    }

    /**
     * Validates the next statement date components are numeric and form a valid date.
     */
    String validateNextStatementDate(UpdateAccountInput input) {
        if (!isNumeric(input.getNextStatementDay().value())
                || !isNumeric(input.getNextStatementMonth().value())
                || !isNumeric(input.getNextStatementYear().value())) {
            return "Next statement date must be numeric";
        }

        int day = Integer.parseInt(input.getNextStatementDay().value().trim());
        int month = Integer.parseInt(input.getNextStatementMonth().value().trim());

        if (day > 31 || day == 0 || month > 12 || month == 0) {
            return "Incorrect date for NEXT STATEMENT.";
        }

        if (!isValidDayForMonth(day, month)) {
            return "Incorrect date for NEXT STATEMENT.";
        }

        return null;
    }

    /**
     * Maps COBOL INQ-ACC-DATA SECTION (IAD010).
     * Links to INQACC to retrieve account data.
     */
    void inquireAccountData(UpdateAccountInput input, UpdateAccountOutput output) {
        int accountNumber = Integer.parseInt(input.getAccountNumber().value().trim());

        AccountCommArea commArea = accountService.inquireAccount(accountNumber);

        if (!commArea.isAccountFound()) {
            output.setMessage("This account number could not be found");
        } else {
            output.setMessage("Please amend fields and hit <pf5> to apply changes");
            output.populateFromCommArea(commArea);
        }
    }

    /**
     * Maps COBOL UPD-ACC-DATA SECTION (UAD010).
     * Constructs the COMMAREA from screen input and links to UPDACC.
     */
    void updateAccountData(UpdateAccountInput input, UpdateAccountOutput output) {
        AccountCommArea commArea = new AccountCommArea();

        int accountNumber = Integer.parseInt(input.getAccountNumber().value().trim());
        if (accountNumber == 99999999) {
            accountNumber = Integer.parseInt(input.getAccountNumber2().value().trim());
        }
        commArea.setAccountNumber(accountNumber);

        commArea.setCustomerNumber(input.getCustomerNumber().value());
        commArea.setSortCode(input.getSortCode().value());
        commArea.setAccountType(input.getAccountType().value());

        BigDecimal interestRate = parseNumericValue(input.getInterestRate().value()
                .substring(0, Math.min(input.getInterestRate().length(),
                        input.getInterestRate().value().length())));
        commArea.setInterestRate(interestRate.setScale(2, RoundingMode.HALF_UP));

        int openedDate = composeDateValue(
                input.getOpenedDay().value(),
                input.getOpenedMonth().value(),
                input.getOpenedYear().value());
        commArea.setOpened(openedDate);

        String overdraftStr = input.getOverdraft().value().substring(0,
                Math.min(input.getOverdraft().length(), input.getOverdraft().value().length()));
        commArea.setOverdraft(parseIntValue(overdraftStr));

        int lastStmt = composeDateValue(
                input.getLastStatementDay().value(),
                input.getLastStatementMonth().value(),
                input.getLastStatementYear().value());
        commArea.setLastStatementDate(lastStmt);

        int nextStmt = composeDateValue(
                input.getNextStatementDay().value(),
                input.getNextStatementMonth().value(),
                input.getNextStatementYear().value());
        commArea.setNextStatementDate(nextStmt);

        commArea.setAvailableBalance(
                convertScreenBalance(input.getAvailableBalance().value()));
        commArea.setActualBalance(
                convertScreenBalance(input.getActualBalance().value()));

        commArea.setSuccess(" ");

        AccountCommArea result = accountService.updateAccount(commArea);

        if ("N".equals(result.getSuccess())) {
            output.setMessage("Update unsuccessful, try again later.");
        } else {
            output.setMessage("Account update successfully applied.");
            output.populateFromCommArea(result);
        }
    }

    /**
     * Converts a screen-format balance string to BigDecimal.
     *
     * Maps the COBOL balance conversion logic (lines 920-951):
     * The screen format is: sign(1) + integer(10) + '.' + decimal(2) = 14 chars
     * e.g. "+0000001234.56" or "-0000001234.56"
     */
    static BigDecimal convertScreenBalance(String screenValue) {
        if (screenValue == null || screenValue.isBlank()) {
            return BigDecimal.ZERO;
        }

        String padded = screenValue.length() < 14
                ? String.format("%14s", screenValue)
                : screenValue.substring(0, 14);

        char sign = padded.charAt(0);
        String integerPartStr = padded.substring(1, 11);
        String decimalPartStr = padded.substring(12, 14);

        long integerPart;
        int decimalPart;
        try {
            integerPart = Long.parseLong(integerPartStr.trim());
            decimalPart = Integer.parseInt(decimalPartStr.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }

        BigDecimal decimalValue = BigDecimal.valueOf(decimalPart)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal total = BigDecimal.valueOf(integerPart).add(decimalValue);

        if (sign == '-') {
            total = total.negate();
        }

        return total.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Composes a COBOL-style date integer (DDMMYYYY) from string components.
     */
    static int composeDateValue(String day, String month, String year) {
        int dd = parseIntValue(day);
        int mm = parseIntValue(month);
        int yyyy = parseIntValue(year);
        return dd * 1000000 + mm * 10000 + yyyy;
    }

    /**
     * Validates that the day is valid for the given month.
     * Matches the COBOL logic which checks:
     * - Day 31 is invalid for months 9 (Sep), 4 (Apr), 6 (Jun), 11 (Nov)
     * - Day > 29 is invalid for month 2 (Feb)
     */
    static boolean isValidDayForMonth(int day, int month) {
        if (day == 31 && (month == 9 || month == 4 || month == 6 || month == 11)) {
            return false;
        }
        return day <= 29 || month != 2;
    }

    static boolean isNumeric(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (char c : value.toCharArray()) {
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    static int countValidChars(String value) {
        int count = 0;
        for (char c : value.toCharArray()) {
            if ((c >= '0' && c <= '9') || c == '.' || c == '-' || c == '+' || c == ' ') {
                count++;
            }
        }
        return count;
    }

    static long countDecimalPoints(String value) {
        return value.chars().filter(c -> c == '.').count();
    }

    static int countCharsAfterDecimalPoint(String value) {
        int dotIndex = value.indexOf('.');
        if (dotIndex < 0) {
            return 0;
        }
        return value.length() - dotIndex - 1;
    }

    static int countDigitsAfterDecimalPoint(String value) {
        int dotIndex = value.indexOf('.');
        if (dotIndex < 0) {
            return 0;
        }
        int count = 0;
        for (int i = dotIndex + 1; i < value.length(); i++) {
            char c = value.charAt(i);
            if ((c >= '0' && c <= '9') || c == '-' || c == '+') {
                count++;
            }
        }
        return count;
    }

    static BigDecimal parseNumericValue(String value) {
        String cleaned = value.trim().replace(" ", "");
        if (cleaned.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(cleaned);
    }

    static int parseIntValue(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
