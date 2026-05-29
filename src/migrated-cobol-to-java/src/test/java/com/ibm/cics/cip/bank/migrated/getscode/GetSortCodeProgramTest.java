/*
 *
 *    Copyright IBM Corp. 2023
 *
 */

package com.ibm.cics.cip.bank.migrated.getscode;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class GetSortCodeProgramTest {

    private static final String EXPECTED_SORT_CODE = "987654";

    @Test
    void executeWithExistingCommareaSetsSortCode() {
        GetSortCodeProgram program = new GetSortCodeProgram();
        GetSortCodeCommarea commarea = new GetSortCodeCommarea();

        GetSortCodeCommarea result = program.execute(commarea);

        assertEquals(EXPECTED_SORT_CODE, result.getSortCode());
    }

    @Test
    void executeWithExistingCommareaReturnsSameInstance() {
        GetSortCodeProgram program = new GetSortCodeProgram();
        GetSortCodeCommarea commarea = new GetSortCodeCommarea();

        GetSortCodeCommarea result = program.execute(commarea);

        assertNotNull(result);
        assertEquals(commarea, result);
    }

    @Test
    void executeWithoutCommareaCreatesFreshInstance() {
        GetSortCodeProgram program = new GetSortCodeProgram();

        GetSortCodeCommarea result = program.execute();

        assertNotNull(result);
        assertEquals(EXPECTED_SORT_CODE, result.getSortCode());
    }

    @Test
    void executeOverwritesPreviousSortCodeValue() {
        GetSortCodeProgram program = new GetSortCodeProgram();
        GetSortCodeCommarea commarea = new GetSortCodeCommarea("000000");

        program.execute(commarea);

        assertEquals(EXPECTED_SORT_CODE, commarea.getSortCode());
    }

    @Test
    void getSortCodeReturnsConstantValue() {
        GetSortCodeProgram program = new GetSortCodeProgram();

        assertEquals(EXPECTED_SORT_CODE, program.getSortCode());
    }

    @Test
    void sortCodeIsExactlySixCharacters() {
        GetSortCodeProgram program = new GetSortCodeProgram();

        assertEquals(6, program.getSortCode().length());
    }

    @Test
    void sortCodeIsNumericString() {
        GetSortCodeProgram program = new GetSortCodeProgram();
        String sortCode = program.getSortCode();

        for (char c : sortCode.toCharArray()) {
            assertEquals(true, Character.isDigit(c),
                    "Expected digit but found: " + c);
        }
    }

    @Test
    void multipleExecutionsReturnSameValue() {
        GetSortCodeProgram program = new GetSortCodeProgram();

        GetSortCodeCommarea result1 = program.execute();
        GetSortCodeCommarea result2 = program.execute();

        assertEquals(result1.getSortCode(), result2.getSortCode());
        assertNotSame(result1, result2);
    }

    @Test
    void multipleInstancesReturnSameValue() {
        GetSortCodeProgram program1 = new GetSortCodeProgram();
        GetSortCodeProgram program2 = new GetSortCodeProgram();

        assertEquals(program1.getSortCode(), program2.getSortCode());
    }

    @Test
    void executeCommareaMatchesDirectGetSortCode() {
        GetSortCodeProgram program = new GetSortCodeProgram();

        GetSortCodeCommarea result = program.execute();

        assertEquals(program.getSortCode(), result.getSortCode());
    }

    @Test
    void sortCodeMatchesCobolCopybookValue() {
        assertEquals("987654", GetSortCodeProgram.SORT_CODE);
    }
}
