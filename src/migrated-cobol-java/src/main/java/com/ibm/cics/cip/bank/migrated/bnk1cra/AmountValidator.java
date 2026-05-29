/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from COBOL: BNK1CRA.cbl - VALIDATE-AMOUNT SECTION
 * Original Author: Jon Collett
 */
package com.ibm.cics.cip.bank.migrated.bnk1cra;

import java.math.BigDecimal;

/**
 * Validates monetary amount input according to the rules defined in the
 * COBOL VALIDATE-AMOUNT section of BNK1CRA.
 *
 * <p>Validation rules:
 * <ul>
 *   <li>Amount must not be empty or all spaces</li>
 *   <li>Must contain only digits, at most one decimal point, and no embedded spaces</li>
 *   <li>At most two decimal places after the point</li>
 *   <li>Must be non-zero</li>
 * </ul>
 */
public final class AmountValidator {

    private AmountValidator() {
    }

    /**
     * Result of amount validation.
     *
     * @param valid   whether the amount is valid
     * @param message error message if invalid, empty if valid
     * @param amount  the parsed BigDecimal value if valid, null if invalid
     */
    public record ValidationResult(boolean valid, String message, BigDecimal amount) {

        public static ValidationResult success(BigDecimal amount) {
            return new ValidationResult(true, "", amount);
        }

        public static ValidationResult failure(String message) {
            return new ValidationResult(false, message, null);
        }
    }

    /**
     * Validates the amount string input from the BMS screen.
     * Preserves the exact validation logic from the COBOL VALIDATE-AMOUNT section.
     *
     * @param amountInput the raw amount string from the screen field
     * @return validation result with parsed amount or error message
     */
    public static ValidationResult validate(String amountInput) {
        if (amountInput == null || amountInput.isEmpty()) {
            return ValidationResult.failure("The Amount entered must be numeric.");
        }

        String trimmed = amountInput.strip();

        if (trimmed.isEmpty()) {
            return ValidationResult.failure("The Amount entered must be numeric.");
        }

        if (isNumericInteger(trimmed)) {
            BigDecimal value = new BigDecimal(trimmed);
            if (value.compareTo(BigDecimal.ZERO) == 0) {
                return ValidationResult.failure("Please supply a non-zero amount.");
            }
            return ValidationResult.success(value);
        }

        if (containsEmbeddedSpaces(trimmed)) {
            return ValidationResult.failure(
                    "Please supply a numeric amount without embedded spaces.");
        }

        if (!containsOnlyValidChars(trimmed)) {
            return ValidationResult.failure("Please supply a numeric amount.");
        }

        long decimalPointCount = trimmed.chars().filter(c -> c == '.').count();

        if (decimalPointCount > 1) {
            return ValidationResult.failure("Use one decimal point for amount only.");
        }

        if (decimalPointCount == 1) {
            int pointIndex = trimmed.indexOf('.');
            String afterPoint = trimmed.substring(pointIndex + 1);
            long significantDecimalDigits = countSignificantDecimalDigits(afterPoint);

            if (significantDecimalDigits > 2) {
                return ValidationResult.failure(
                        "Only up to two decimal places are supported.");
            }
        }

        BigDecimal value;
        try {
            value = new BigDecimal(trimmed);
        } catch (NumberFormatException e) {
            return ValidationResult.failure("Please supply a numeric amount.");
        }

        if (value.compareTo(BigDecimal.ZERO) == 0) {
            return ValidationResult.failure("Please supply a non-zero amount.");
        }

        return ValidationResult.success(value);
    }

    private static boolean isNumericInteger(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean containsEmbeddedSpaces(String s) {
        boolean foundNonSpace = false;
        boolean foundSpaceAfterNonSpace = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != ' ') {
                if (foundSpaceAfterNonSpace) {
                    return true;
                }
                foundNonSpace = true;
            } else if (foundNonSpace) {
                foundSpaceAfterNonSpace = true;
            }
        }
        return false;
    }

    private static boolean containsOnlyValidChars(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!Character.isDigit(c) && c != '.') {
                return false;
            }
        }
        return true;
    }

    /**
     * Counts significant (non-trailing-zero beyond 2) decimal digits.
     * Matches the COBOL logic that inspects characters after the decimal point
     * and checks if more than 2 numeric digits exist.
     */
    private static long countSignificantDecimalDigits(String afterPoint) {
        long digitCount = 0;
        for (int i = 0; i < afterPoint.length(); i++) {
            if (Character.isDigit(afterPoint.charAt(i))) {
                digitCount++;
            }
        }
        return digitCount;
    }
}
