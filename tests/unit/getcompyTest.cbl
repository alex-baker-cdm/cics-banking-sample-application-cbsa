      ******************************************************************
      * getcompyTest - isolated unit test for GETCOMPY.
      *
      * GETCOMPY is a near-pure function: it moves a hard-coded company
      * name into the COMMAREA and RETURNs. The test passes a COMMAREA
      * shaped like the GETCOMPY copybook, invokes it through the shim,
      * and asserts the exact 40-byte result.
      *
      * Assertions:
      *   1. happy path : company name == "CICS Bank Sample Application"
      *   2. edge       : field is exactly 40 bytes, space-padded
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. GETCOMPYTEST.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
      * COMMAREA layout - matches src/base/cobol_copy/GETCOMPY.cpy.
       01 WS-COMM.
          03 GETCompanyOperation.
             06 COMPANY-NAME              PIC X(40).

       01 WS-EXPECTED                     PIC X(40)
              VALUE "CICS Bank Sample Application".
       01 WS-FAILURES                     PIC 9(4) VALUE 0.

       PROCEDURE DIVISION.
       MAIN-A.
           DISPLAY "=== getcompyTest : GETCOMPY (company name) ==="

           MOVE SPACES TO COMPANY-NAME
           CALL 'GETCOMPY' USING WS-COMM

           DISPLAY "    company-name = [" COMPANY-NAME "]"

           IF COMPANY-NAME = WS-EXPECTED
              DISPLAY "PASS: exact company name returned"
           ELSE
              DISPLAY "FAIL: unexpected company name"
              ADD 1 TO WS-FAILURES
           END-IF

      *    Edge: trailing bytes past the 28-char name must be spaces.
           IF COMPANY-NAME (29:12) = SPACES
              DISPLAY "PASS: 40-byte field space-padded"
           ELSE
              DISPLAY "FAIL: field not space-padded"
              ADD 1 TO WS-FAILURES
           END-IF

           IF WS-FAILURES = 0
              DISPLAY "RESULT: getcompyTest PASSED"
              MOVE 0 TO RETURN-CODE
           ELSE
              DISPLAY "RESULT: getcompyTest FAILED (" WS-FAILURES ")"
              MOVE 1 TO RETURN-CODE
           END-IF

           STOP RUN.
