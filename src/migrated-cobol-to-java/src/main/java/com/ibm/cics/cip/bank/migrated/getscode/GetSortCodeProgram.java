/*
 *
 *    Copyright IBM Corp. 2023
 *
 *    Migrated from COBOL to Java 21.
 *    Original COBOL source: GETSCODE.cbl
 *    Original author: James O'Grady
 *
 *    This program retrieves the bank's sort code. In the original COBOL,
 *    it is linked via EXEC CICS LINK and returns the sort code in the
 *    DFHCOMMAREA. The sort code value originates from the SORTCODE copybook
 *    (SORTCODE.cpy: PIC 9(6) VALUE 987654).
 *
 *    COBOL procedure:
 *        MOVE LITERAL-SORTCODE TO SORTCODE OF DFHCOMMAREA.
 *        EXEC CICS RETURN END-EXEC.
 *
 */

package com.ibm.cics.cip.bank.migrated.getscode;

public final class GetSortCodeProgram {

    static final String SORT_CODE = "987654";

    public GetSortCodeCommarea execute(GetSortCodeCommarea commarea) {
        commarea.setSortCode(SORT_CODE);
        return commarea;
    }

    public GetSortCodeCommarea execute() {
        return execute(new GetSortCodeCommarea());
    }

    public String getSortCode() {
        return SORT_CODE;
    }
}
