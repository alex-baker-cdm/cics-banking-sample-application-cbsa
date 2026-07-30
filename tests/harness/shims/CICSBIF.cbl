      ******************************************************************
      * CICSBIF - test double for EXEC CICS BIF DEEDIT FIELD(x).
      *
      * DEEDIT strips every non-numeric character from the field, then
      * right-justifies the surviving digits and zero-fills on the left,
      * within the original field length - so the result is always all
      * digits (which is what the callers test with IS NUMERIC).
      *
      *   CALL 'CICSBIF' USING BY CONTENT len(9(9)COMP-5)
      *                        BY REFERENCE field
      *
      * cicsPreprocessor.py emits ``MOVE LENGTH OF field TO WS-SHIM-LEN``
      * before the call so the true field length is passed.
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. CICSBIF.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 WS-I                    PIC 9(9) COMP-5.
       01 WS-N                    PIC 9(9) COMP-5 VALUE 0.
       01 WS-START                PIC 9(9) COMP-5.
       01 WS-DIGITS               PIC X(4096).

       LINKAGE SECTION.
       01 LK-LEN                  PIC 9(9) COMP-5.
       01 LK-FIELD                PIC X(4096).

       PROCEDURE DIVISION USING LK-LEN LK-FIELD.
       A010.
           MOVE 0 TO WS-N
           MOVE SPACES TO WS-DIGITS
           IF LK-LEN = 0
              GOBACK
           END-IF
           PERFORM VARYING WS-I FROM 1 BY 1 UNTIL WS-I > LK-LEN
              IF LK-FIELD(WS-I:1) IS NUMERIC
                 ADD 1 TO WS-N
                 MOVE LK-FIELD(WS-I:1) TO WS-DIGITS(WS-N:1)
              END-IF
           END-PERFORM

      *    Rebuild the field: all zeros, then digits right-justified.
           MOVE ALL '0' TO LK-FIELD(1:LK-LEN)
           IF WS-N > 0
              COMPUTE WS-START = LK-LEN - WS-N + 1
              MOVE WS-DIGITS(1:WS-N) TO LK-FIELD(WS-START:WS-N)
           END-IF
           GOBACK.
