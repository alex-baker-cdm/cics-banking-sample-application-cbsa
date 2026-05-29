/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.migrated.cobol.transferfunds;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Java 21 migration of the COBOL program BNK1TFN.cbl — Transfer Funds between
 * accounts (BMS screen handler).
 *
 * <p>This class encapsulates all business logic that was embedded in the COBOL
 * source: input validation (EDIT-DATA), amount validation (VALIDATE-AMOUNT),
 * delegation to the transfer function (GET-ACC-DATA linking to XFRFUN), and
 * result/error mapping.
 *
 * <p>Presentation-layer concerns (CICS BMS SEND MAP / RECEIVE MAP, terminal
 * key handling, ABEND processing) are intentionally excluded because they are
 * infrastructure-specific and not portable.
 */
public class TransferFundsService {

    private static final Logger logger = Logger.getLogger(
            TransferFundsService.class.getName());

    private static final String ZERO_ACCOUNT = "00000000";
    private static final int ACCOUNT_NUMBER_LENGTH = 8;
    private static final int MAX_DECIMAL_PLACES = 2;

    private final TransferFunctionGateway transferFunctionGateway;

    public TransferFundsService(TransferFunctionGateway transferFunctionGateway) {
        this.transferFunctionGateway = Objects.requireNonNull(
                transferFunctionGateway, "transferFunctionGateway");
    }

    /**
     * Validates inputs and executes a fund transfer between two accounts.
     * Equivalent to PROCESS-MAP → EDIT-DATA → GET-ACC-DATA in BNK1TFN.cbl.
     *
     * @param fromAccountNumber the FROM account number (raw input from screen)
     * @param toAccountNumber   the TO account number (raw input from screen)
     * @param amountInput       the amount as entered by the user (raw string)
     * @return a {@link TransferResult} describing success/failure with balances
     */
    public TransferResult processTransfer(String fromAccountNumber,
                                          String toAccountNumber,
                                          String amountInput) {
        logger.entering(getClass().getName(), "processTransfer");

        // --- EDIT-DATA equivalent ---
        String validationMessage = editData(fromAccountNumber,
                toAccountNumber, amountInput);
        if (validationMessage != null) {
            logger.log(Level.INFO, "Validation failed: {0}",
                    validationMessage);
            return TransferResult.builder()
                    .successful(false)
                    .message(validationMessage)
                    .build();
        }

        BigDecimal amount = validateAmount(amountInput);
        if (amount == null) {
            // validateAmount sets the message via the returned null sentinel;
            // the actual message was already captured.  Re-run to get it.
            String amountMessage = getAmountValidationMessage(amountInput);
            return TransferResult.builder()
                    .successful(false)
                    .message(amountMessage)
                    .build();
        }

        // --- GET-ACC-DATA equivalent ---
        return getAccountData(
                normalizeAccountNumber(fromAccountNumber),
                normalizeAccountNumber(toAccountNumber),
                amount);
    }

    // ------------------------------------------------------------------
    // EDIT-DATA section (lines 428-475 of BNK1TFN.cbl)
    // ------------------------------------------------------------------

    /**
     * Validates the FROM/TO account numbers.
     *
     * @return an error message, or {@code null} when valid
     */
    String editData(String fromAccount, String toAccount,
                    String amountInput) {

        String normalizedFrom = deedit(fromAccount);
        if (!isNumeric(normalizedFrom)) {
            return "Please enter a FROM account no";
        }

        String normalizedTo = deedit(toAccount);
        if (!isNumeric(normalizedTo)) {
            return "Please enter a TO account no";
        }

        if (normalizedFrom.equals(normalizedTo)) {
            return "The FROM & TO account should be different";
        }

        if (ZERO_ACCOUNT.equals(padLeft(normalizedFrom, ACCOUNT_NUMBER_LENGTH))
                || ZERO_ACCOUNT.equals(
                        padLeft(normalizedTo, ACCOUNT_NUMBER_LENGTH))) {
            return "Account no 00000000 is not valid";
        }

        return null;
    }

    // ------------------------------------------------------------------
    // VALIDATE-AMOUNT section (lines 985-1204 of BNK1TFN.cbl)
    // ------------------------------------------------------------------

    /**
     * Validates and parses the user-entered amount string exactly as the COBOL
     * VALIDATE-AMOUNT section does, returning the parsed value or {@code null}
     * if invalid.
     */
    BigDecimal validateAmount(String amountInput) {
        if (amountInput == null || amountInput.isEmpty()) {
            return null;
        }

        String trimmed = amountInput.strip();
        if (trimmed.isEmpty()) {
            return null;
        }

        // Fast path: if the raw input (non-stripped) is purely numeric, parse
        // it directly — mirrors the COBOL IS NUMERIC check on AMTI(1:AMTL).
        if (isNumeric(amountInput)) {
            BigDecimal fast = new BigDecimal(amountInput.strip());
            if (fast.compareTo(BigDecimal.ZERO) <= 0) {
                return null;
            }
            return fast;
        }

        // Strip leading and trailing spaces (COBOL UNSTRING + REVERSE logic)
        String stripped = trimmed;

        if (containsMinus(stripped)) {
            return null;
        }

        if (containsEmbeddedSpaces(stripped)) {
            return null;
        }

        if (!isNumericWithDecimal(stripped)) {
            return null;
        }

        long pointCount = countChar(stripped, '.');
        if (pointCount > 1) {
            return null;
        }

        if (pointCount == 1) {
            int dotIndex = stripped.indexOf('.');
            String afterDot = stripped.substring(dotIndex + 1);
            long digitsAfterDot = countDigits(afterDot);
            if (digitsAfterDot > MAX_DECIMAL_PLACES) {
                return null;
            }
        }

        BigDecimal parsed = new BigDecimal(stripped);
        if (parsed.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        return parsed;
    }

    /**
     * Returns the human-readable validation error for the given amount input.
     * Mirrors each {@code GO TO VA999} branch in the COBOL source.
     */
    String getAmountValidationMessage(String amountInput) {
        if (amountInput == null || amountInput.isEmpty()) {
            return "The Amount entered must be numeric.";
        }

        String trimmed = amountInput.strip();
        if (trimmed.isEmpty()) {
            return "The Amount entered must be numeric.";
        }

        if (isNumeric(amountInput)) {
            BigDecimal fast = new BigDecimal(trimmed);
            if (fast.compareTo(BigDecimal.ZERO) <= 0) {
                return "Please supply a positive amount.";
            }
            return null;
        }

        String stripped = trimmed;

        if (containsMinus(stripped)) {
            return "Please supply a positive amount.";
        }

        if (containsEmbeddedSpaces(stripped)) {
            return "Please supply a numeric amount without embedded spaces.";
        }

        if (!isNumericWithDecimal(stripped)) {
            return "Please supply a numeric amount.";
        }

        long pointCount = countChar(stripped, '.');
        if (pointCount > 1) {
            return "Use one decimal point for amount only.";
        }

        if (pointCount == 1) {
            int dotIndex = stripped.indexOf('.');
            String afterDot = stripped.substring(dotIndex + 1);
            long digitsAfterDot = countDigits(afterDot);
            if (digitsAfterDot > MAX_DECIMAL_PLACES) {
                return "Only up to two decimal places are supported.";
            }
        }

        BigDecimal parsed = new BigDecimal(stripped);
        if (parsed.compareTo(BigDecimal.ZERO) == 0) {
            return "Please supply a non-zero amount.";
        }

        return null;
    }

    // ------------------------------------------------------------------
    // GET-ACC-DATA section (lines 478-659 of BNK1TFN.cbl)
    // ------------------------------------------------------------------

    private TransferResult getAccountData(String fromAccount,
                                          String toAccount,
                                          BigDecimal amount) {
        logger.entering(getClass().getName(), "getAccountData");

        TransferFunctionRequest request = new TransferFunctionRequest(
                fromAccount, toAccount, amount);

        TransferFunctionResponse response;
        try {
            response = transferFunctionGateway.execute(request);
        } catch (RuntimeException ex) {
            logger.log(Level.SEVERE,
                    "Transfer function gateway invocation failed", ex);
            return TransferResult.builder()
                    .successful(false)
                    .message("Sorry but the transfer could not be applied"
                            + " due to an unexpected error.")
                    .build();
        }

        TransferResult.Builder resultBuilder = TransferResult.builder()
                .fromAccountNumber(response.fromAccountNumber())
                .fromSortCode(response.fromSortCode())
                .toAccountNumber(response.toAccountNumber())
                .toSortCode(response.toSortCode());

        if (response.success() == 'N') {
            resultBuilder.successful(false);
            resultBuilder.fromAvailableBalance(BigDecimal.ZERO);
            resultBuilder.fromActualBalance(BigDecimal.ZERO);
            resultBuilder.toAvailableBalance(BigDecimal.ZERO);
            resultBuilder.toActualBalance(BigDecimal.ZERO);

            String errorMessage = switch (response.failCode()) {
                case '1' -> "Sorry the FROM ACCOUNT no was not found."
                        + " Transfer not applied.";
                case '2' -> "Sorry the TO ACCOUNT no was not found."
                        + " Transfer not applied.";
                case '3' -> "Sorry but the transfer could not be applied"
                        + " due to an unexpected error.";
                case '4' -> "Please supply an amount greater than zero.";
                default -> "Sorry but the transfer could not be applied"
                        + " due to an error.";
            };
            resultBuilder.message(errorMessage);
            return resultBuilder.build();
        }

        if (response.success() != 'Y') {
            resultBuilder.successful(false);
            resultBuilder.fromAvailableBalance(BigDecimal.ZERO);
            resultBuilder.fromActualBalance(BigDecimal.ZERO);
            resultBuilder.toAvailableBalance(BigDecimal.ZERO);
            resultBuilder.toActualBalance(BigDecimal.ZERO);
            resultBuilder.message("Sorry but the transfer could not be applied"
                    + " unable to determine success.");
            return resultBuilder.build();
        }

        resultBuilder.successful(true);
        resultBuilder.message("Transfer successfully applied.");
        resultBuilder.fromActualBalance(response.fromActualBalance());
        resultBuilder.fromAvailableBalance(response.fromAvailableBalance());
        resultBuilder.toActualBalance(response.toActualBalance());
        resultBuilder.toAvailableBalance(response.toAvailableBalance());

        logger.exiting(getClass().getName(), "getAccountData");
        return resultBuilder.build();
    }

    // ------------------------------------------------------------------
    // Utility methods (DEEDIT, numeric checks, etc.)
    // ------------------------------------------------------------------

    /**
     * Simulates EXEC CICS BIF DEEDIT — strips non-numeric formatting
     * characters (commas, currency symbols, spaces) leaving only digits and
     * optionally a leading minus sign.
     */
    static String deedit(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (Character.isDigit(c) || (c == '-' && sb.isEmpty())) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    static boolean isNumeric(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String trimmed = value.strip();
        for (int i = 0; i < trimmed.length(); i++) {
            if (!Character.isDigit(trimmed.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    static String normalizeAccountNumber(String raw) {
        String deedited = deedit(raw);
        return padLeft(deedited, ACCOUNT_NUMBER_LENGTH);
    }

    private static String padLeft(String value, int length) {
        if (value.length() >= length) {
            return value;
        }
        return "0".repeat(length - value.length()) + value;
    }

    private static boolean containsMinus(String value) {
        return value.indexOf('-') >= 0;
    }

    private static boolean containsEmbeddedSpaces(String value) {
        return value.indexOf(' ') >= 0;
    }

    private static boolean isNumericWithDecimal(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!Character.isDigit(c) && c != '.') {
                return false;
            }
        }
        return true;
    }

    private static long countChar(String value, char target) {
        return value.chars().filter(c -> c == target).count();
    }

    private static long countDigits(String value) {
        return value.chars().filter(Character::isDigit).count();
    }
}
