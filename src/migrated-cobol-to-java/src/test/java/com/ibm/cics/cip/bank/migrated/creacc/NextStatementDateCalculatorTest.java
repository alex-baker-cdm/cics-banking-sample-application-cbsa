/*
 * Copyright IBM Corp. 2023
 */

package com.ibm.cics.cip.bank.migrated.creacc;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NextStatementDateCalculatorTest {

    @Nested
    @DisplayName("Simple +30 Days Calculation (DB2/COMMAREA)")
    class SimpleCalculationTests {

        @Test
        @DisplayName("Adds 30 days to a regular date")
        void adds30Days() {
            LocalDate date = LocalDate.of(2024, 3, 15);
            LocalDate result =
                    NextStatementDateCalculator.calculateNextStatementDate(
                            date);
            assertEquals(LocalDate.of(2024, 4, 14), result);
        }

        @Test
        @DisplayName("Crosses month boundary correctly")
        void crossesMonthBoundary() {
            LocalDate date = LocalDate.of(2024, 1, 15);
            LocalDate result =
                    NextStatementDateCalculator.calculateNextStatementDate(
                            date);
            assertEquals(LocalDate.of(2024, 2, 14), result);
        }

        @Test
        @DisplayName("Crosses year boundary correctly")
        void crossesYearBoundary() {
            LocalDate date = LocalDate.of(2024, 12, 15);
            LocalDate result =
                    NextStatementDateCalculator.calculateNextStatementDate(
                            date);
            assertEquals(LocalDate.of(2025, 1, 14), result);
        }

        @Test
        @DisplayName("Handles January 31")
        void handlesJanuary31() {
            LocalDate date = LocalDate.of(2024, 1, 31);
            LocalDate result =
                    NextStatementDateCalculator.calculateNextStatementDate(
                            date);
            assertEquals(LocalDate.of(2024, 3, 1), result);
        }

        @Test
        @DisplayName("Handles February 28 in non-leap year")
        void handlesFebruary28NonLeap() {
            LocalDate date = LocalDate.of(2023, 2, 28);
            LocalDate result =
                    NextStatementDateCalculator.calculateNextStatementDate(
                            date);
            assertEquals(LocalDate.of(2023, 3, 30), result);
        }

        @Test
        @DisplayName("Handles February 29 in leap year")
        void handlesFebruary29Leap() {
            LocalDate date = LocalDate.of(2024, 2, 29);
            LocalDate result =
                    NextStatementDateCalculator.calculateNextStatementDate(
                            date);
            assertEquals(LocalDate.of(2024, 3, 30), result);
        }

        @ParameterizedTest
        @CsvSource({
                "2024-01-01, 2024-01-31",
                "2024-06-30, 2024-07-30",
                "2024-11-30, 2024-12-30",
                "2024-12-31, 2025-01-30"
        })
        @DisplayName("Various date boundary tests")
        void variousBoundaryTests(String input, String expected) {
            LocalDate date = LocalDate.parse(input);
            LocalDate result =
                    NextStatementDateCalculator.calculateNextStatementDate(
                            date);
            assertEquals(LocalDate.parse(expected), result);
        }
    }

    @Nested
    @DisplayName("Month-Dependent Calculation (CALCULATE-DATES)")
    class MonthDependentCalculationTests {

        @Test
        @DisplayName("Adds 30 days for January")
        void adds30ForJanuary() {
            LocalDate date = LocalDate.of(2024, 1, 15);
            LocalDate result =
                    NextStatementDateCalculator
                            .calculateMonthDependentNextDate(date);
            assertEquals(LocalDate.of(2024, 2, 14), result);
        }

        @Test
        @DisplayName("Adds 28 days for February in non-leap year")
        void adds28ForFebruaryNonLeap() {
            LocalDate date = LocalDate.of(2023, 2, 15);
            LocalDate result =
                    NextStatementDateCalculator
                            .calculateMonthDependentNextDate(date);
            assertEquals(LocalDate.of(2023, 3, 15), result);
        }

        @Test
        @DisplayName("Adds 29 days for February in leap year")
        void adds29ForFebruaryLeap() {
            LocalDate date = LocalDate.of(2024, 2, 15);
            LocalDate result =
                    NextStatementDateCalculator
                            .calculateMonthDependentNextDate(date);
            assertEquals(LocalDate.of(2024, 3, 15), result);
        }

        @Test
        @DisplayName("Adds 30 days for March")
        void adds30ForMarch() {
            LocalDate date = LocalDate.of(2024, 3, 15);
            LocalDate result =
                    NextStatementDateCalculator
                            .calculateMonthDependentNextDate(date);
            assertEquals(LocalDate.of(2024, 4, 14), result);
        }

        @Test
        @DisplayName("Adds 30 days for April")
        void adds30ForApril() {
            LocalDate date = LocalDate.of(2024, 4, 15);
            LocalDate result =
                    NextStatementDateCalculator
                            .calculateMonthDependentNextDate(date);
            assertEquals(LocalDate.of(2024, 5, 15), result);
        }

        @Test
        @DisplayName("Adds 30 days for all non-February months")
        void adds30ForNonFebruaryMonths() {
            int[] months = {1, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
            for (int month : months) {
                LocalDate date = LocalDate.of(2024, month, 10);
                LocalDate result =
                        NextStatementDateCalculator
                                .calculateMonthDependentNextDate(date);
                assertEquals(date.plusDays(30), result,
                        "Failed for month " + month);
            }
        }
    }

    @Nested
    @DisplayName("Leap Year Detection")
    class LeapYearTests {

        @ParameterizedTest
        @ValueSource(ints = {2024, 2020, 2000, 1600, 2400})
        @DisplayName("Identifies leap years correctly")
        void identifiesLeapYears(int year) {
            assertTrue(NextStatementDateCalculator.isLeapYear(year),
                    year + " should be a leap year");
        }

        @ParameterizedTest
        @ValueSource(ints = {2023, 2021, 1900, 2100, 2200, 2300})
        @DisplayName("Identifies non-leap years correctly")
        void identifiesNonLeapYears(int year) {
            assertFalse(NextStatementDateCalculator.isLeapYear(year),
                    year + " should not be a leap year");
        }

        @Test
        @DisplayName("Year divisible by 4 but not 100 is leap year")
        void divisibleBy4NotBy100() {
            assertTrue(NextStatementDateCalculator.isLeapYear(2024));
        }

        @Test
        @DisplayName("Year divisible by 100 but not 400 is not leap year")
        void divisibleBy100NotBy400() {
            assertFalse(NextStatementDateCalculator.isLeapYear(1900));
        }

        @Test
        @DisplayName("Year divisible by 400 is leap year")
        void divisibleBy400() {
            assertTrue(NextStatementDateCalculator.isLeapYear(2000));
        }

        @Test
        @DisplayName("Year not divisible by 4 is not leap year")
        void notDivisibleBy4() {
            assertFalse(NextStatementDateCalculator.isLeapYear(2023));
        }
    }
}
