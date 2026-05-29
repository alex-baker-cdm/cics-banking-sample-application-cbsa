/*
 * Copyright IBM Corp. 2023
 *
 * Migrated from COBOL: CREACC.cbl CALCULATE-DATES SECTION
 * and WRITE-ACCOUNT-DB2 SECTION (next statement date calculation)
 */

package com.ibm.cics.cip.bank.migrated.creacc;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Calculates the next statement date for a newly created account.
 * <p>
 * The original COBOL uses two date calculations:
 * <ol>
 *   <li>CALCULATE-DATES section: adds a month-dependent number of days
 *       (30 for most months, 28/29 for February with leap year handling)</li>
 *   <li>WRITE-ACCOUNT-DB2 section: simply adds 30 calendar days</li>
 * </ol>
 * The DB2 record and COMMAREA use the simple +30 days calculation.
 * The month-dependent calculation is preserved as
 * {@link #calculateMonthDependentNextDate} for behavioral fidelity.
 */
public final class NextStatementDateCalculator {

    private static final int DEFAULT_DAYS_TO_ADD = 30;

    private NextStatementDateCalculator() {
    }

    /**
     * Calculates next statement date by adding 30 calendar days to the
     * given date. This is the calculation used for the DB2 ACCOUNT record
     * and the COMMAREA output.
     * <p>
     * Mirrors WRITE-ACCOUNT-DB2 lines 802-808:
     * <pre>
     *   COMPUTE WS-INTEGER = WS-INTEGER + 30.
     * </pre>
     *
     * @param today the account opening date
     * @return the date 30 days after today
     */
    public static LocalDate calculateNextStatementDate(LocalDate today) {
        return today.plusDays(DEFAULT_DAYS_TO_ADD);
    }

    /**
     * Month-dependent next statement date calculation preserved from the
     * CALCULATE-DATES SECTION (lines 1098-1206). Adds days based on the
     * current month:
     * <ul>
     *   <li>February: 28 days (29 in a leap year)</li>
     *   <li>All other months: 30 days</li>
     * </ul>
     * <p>
     * The leap year check follows the original COBOL logic:
     * divisible by 4, not by 100 (unless also by 400).
     *
     * @param today the account opening date
     * @return the calculated next statement date
     */
    public static LocalDate calculateMonthDependentNextDate(LocalDate today) {
        int month = today.getMonthValue();
        int year = today.getYear();

        int daysToAdd;
        if (month == 2) {
            daysToAdd = 28;
            if (isLeapYear(year)) {
                daysToAdd = 29;
            }
        } else {
            daysToAdd = DEFAULT_DAYS_TO_ADD;
        }

        return today.plusDays(daysToAdd);
    }

    /**
     * Leap year check matching the original COBOL logic:
     * <pre>
     *   DIVIDE year BY 4 ... REMAINDER LEAP-YEAR
     *   IF LEAP-YEAR = ZERO
     *     DIVIDE year BY 100 ... REMAINDER LEAP-YEAR
     *     IF LEAP-YEAR > 0
     *       -- leap year (divisible by 4, not by 100)
     *     ELSE
     *       DIVIDE year BY 400 ... REMAINDER LEAP-YEAR
     *       IF LEAP-YEAR = ZERO
     *         -- leap year (divisible by 400)
     * </pre>
     */
    static boolean isLeapYear(int year) {
        if (year % 4 != 0) {
            return false;
        }
        if (year % 100 != 0) {
            return true;
        }
        return year % 400 == 0;
    }
}
