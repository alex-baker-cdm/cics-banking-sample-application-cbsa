      ******************************************************************
      * getscodeTest - isolated unit test for GETSCODE.
      *
      * GETSCODE moves the hard-coded literal sort code (987654, from the
      * SORTCODE copybook) into the COMMAREA and RETURNs. The test passes
      * a COMMAREA shaped like the GETSCODE copybook, invokes it through
      * the shim, and asserts the exact 6-byte result.
      *
      * Assertions:
      *   1. happy path : sort code == "987654"
      *   2. edge       : field is exactly 6 bytes
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. GETSCODETEST.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
      * COMMAREA layout - matches src/base/cobol_copy/GETSCODE.cpy.
       01 WS-COMM.
          03 GETSORTCODEOperation.
             06 SORTCODE                  PIC X(6).

       01 WS-EXPECTED                     PIC X(6) VALUE "987654".
       01 WS-FAILURES                     PIC 9(4) VALUE 0.

       PROCEDURE DIVISION.
       MAIN-A.
           DISPLAY "=== getscodeTest : GETSCODE (sort code) ==="

           MOVE SPACES TO SORTCODE OF WS-COMM
           CALL 'GETSCODE' USING WS-COMM

           DISPLAY "    sort code = [" SORTCODE OF WS-COMM "]"

           IF SORTCODE OF WS-COMM = WS-EXPECTED
              DISPLAY "PASS: exact sort code 987654 returned"
           ELSE
              DISPLAY "FAIL: unexpected sort code"
              ADD 1 TO WS-FAILURES
           END-IF

           IF SORTCODE OF WS-COMM (1:6) = "987654"
              DISPLAY "PASS: sort code is 6 bytes"
           ELSE
              DISPLAY "FAIL: sort code wrong width"
              ADD 1 TO WS-FAILURES
           END-IF

           IF WS-FAILURES = 0
              DISPLAY "RESULT: getscodeTest PASSED"
              MOVE 0 TO RETURN-CODE
           ELSE
              DISPLAY "RESULT: getscodeTest FAILED (" WS-FAILURES ")"
              MOVE 1 TO RETURN-CODE
           END-IF

           STOP RUN.
