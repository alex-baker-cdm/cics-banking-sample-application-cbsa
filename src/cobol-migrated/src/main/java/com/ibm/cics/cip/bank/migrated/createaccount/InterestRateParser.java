/*
 *
 *    Copyright IBM Corp. 2023
 *
 */
package com.ibm.cics.cip.bank.migrated.createaccount;

import java.math.BigDecimal;
import java.util.Optional;

public final class InterestRateParser {

    private InterestRateParser() {
    }

    public static Optional<BigDecimal> parse(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }

        String trimmed = input.trim();
        if (!isValidInterestRateFormat(trimmed)) {
            return Optional.empty();
        }

        long decimalPointCount = trimmed.chars().filter(c -> c == '.').count();
        if (decimalPointCount > 1) {
            return Optional.empty();
        }

        if (decimalPointCount == 1) {
            int dotIndex = trimmed.indexOf('.');
            String afterDot = trimmed.substring(dotIndex + 1);
            String numericAfterDot = afterDot.replaceAll("[^0-9]", "");
            if (numericAfterDot.length() > 2) {
                return Optional.empty();
            }
        }

        try {
            BigDecimal value = new BigDecimal(trimmed);
            return Optional.of(value);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static boolean isValidInterestRateFormat(String value) {
        for (char c : value.toCharArray()) {
            if (!Character.isDigit(c) && c != '.' && c != '-' && c != '+' && c != ' ') {
                return false;
            }
        }
        return true;
    }
}
