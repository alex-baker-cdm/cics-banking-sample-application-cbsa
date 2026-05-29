/*
 *
 *    Copyright IBM Corp. 2023
 *
 */

package com.ibm.cics.cip.bank.migrated.createcustomer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class CustomerTitleTest {

    @ParameterizedTest(name = "fromInput(\"{0}\") = {1}")
    @CsvSource({
            "Mr,        MR",
            "MR,        MR",
            "mr,        MR",
            "Mrs,       MRS",
            "MRS,       MRS",
            "mrs,       MRS",
            "Miss,      MISS",
            "MISS,      MISS",
            "miss,      MISS",
            "Ms,        MS",
            "MS,        MS",
            "ms,        MS",
            "Dr,        DR",
            "DR,        DR",
            "dr,        DR",
            "Drs,       DRS",
            "DRS,       DRS",
            "drs,       DRS",
            "Professor, PROFESSOR",
            "PROFESSOR, PROFESSOR",
            "Lord,      LORD",
            "LORD,      LORD",
            "lord,      LORD",
            "Sir,       SIR",
            "SIR,       SIR",
            "sir,       SIR",
            "Lady,      LADY",
            "LADY,      LADY",
            "lady,      LADY"
    })
    void shouldResolveTitlesFromVariousCasings(String input,
                                               CustomerTitle expected) {
        assertEquals(expected, CustomerTitle.fromInput(input));
    }

    @ParameterizedTest(name = "fromInput(\"{0}\") handles underscores/spaces")
    @CsvSource({
            "MR________,    MR",
            "Mr________,    MR",
            "mr________,    MR",
            "MRS_______,    MRS",
            "MISS______,    MISS",
            "Miss      ,    MISS",
            "MS________,    MS",
            "DR________,    DR",
            "Dr        ,    DR",
            "DRS_______,    DRS",
            "PROFESSOR_,    PROFESSOR",
            "Professor ,    PROFESSOR",
            "LORD______,    LORD",
            "Lord      ,    LORD",
            "SIR_______,    SIR",
            "Sir       ,    SIR",
            "LADY______,    LADY",
            "Lady      ,    LADY"
    })
    void shouldHandleUnderscoresAndTrailingSpaces(String input,
                                                   CustomerTitle expected) {
        assertEquals(expected, CustomerTitle.fromInput(input));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"King", "Queen", "123", "___", "   "})
    void shouldReturnNullForInvalidTitles(String input) {
        assertNull(CustomerTitle.fromInput(input));
    }

    @Test
    void shouldProvideCorrectDisplayNames() {
        assertEquals("Mr", CustomerTitle.MR.displayName());
        assertEquals("Mrs", CustomerTitle.MRS.displayName());
        assertEquals("Professor", CustomerTitle.PROFESSOR.displayName());
    }

    @Test
    void shouldListAllValidTitles() {
        String list = CustomerTitle.validTitlesList();
        assertTrue(list.contains("Mr"));
        assertTrue(list.contains("Mrs"));
        assertTrue(list.contains("Miss"));
        assertTrue(list.contains("Ms"));
        assertTrue(list.contains("Dr"));
        assertTrue(list.contains("Drs"));
        assertTrue(list.contains("Professor"));
        assertTrue(list.contains("Lord"));
        assertTrue(list.contains("Sir"));
        assertTrue(list.contains("Lady"));
    }
}
