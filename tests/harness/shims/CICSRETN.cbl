      ******************************************************************
      * CICSRETN - test double that records EXEC CICS RETURN TRANSID(..)
      * [COMMAREA(..)], i.e. the pseudo-conversational transaction hand-off.
      *
      * A presentation program ends a turn by returning to CICS naming the
      * NEXT transaction to run (and threading state in the COMMAREA). This
      * resident module records the transid it was handed and a copy of the
      * COMMAREA, so a driver can assert which transaction the screen routed
      * to and what state it saved. cicsPreprocessor.py emits the RECORD op
      * for a RETURN TRANSID and then falls through to GOBACK (the CICS task
      * ends), exactly like RETURN without a transid.
      *
      *   RECORD - store transid + COMMAREA (program side).
      *   GET    - read back the last transid + COMMAREA (driver side).
      *   RESET  - forget any recorded hand-off (driver side).
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. CICSRETN.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 WS-COUNT                PIC 9(4) COMP-5 VALUE 0.
       01 WS-TRANID               PIC X(4)        VALUE SPACES.
       01 WS-LEN                  PIC 9(9) COMP-5 VALUE 0.
       01 WS-DATA                 PIC X(4096).

       LINKAGE SECTION.
       01 LK-OP                   PIC X(8).
       01 LK-TRANID               PIC X(4).
       01 LK-LEN                  PIC 9(9) COMP-5.
       01 LK-DATA                 PIC X(4096).

       PROCEDURE DIVISION USING LK-OP LK-TRANID LK-LEN LK-DATA.
       A010.
           EVALUATE LK-OP
              WHEN 'RECORD  '
                 ADD 1 TO WS-COUNT
                 MOVE LK-TRANID TO WS-TRANID
                 MOVE LK-LEN    TO WS-LEN
                 IF LK-LEN > 0
                    MOVE LK-DATA(1:LK-LEN) TO WS-DATA(1:LK-LEN)
                 END-IF

              WHEN 'GET     '
                 MOVE WS-TRANID TO LK-TRANID
                 MOVE WS-LEN    TO LK-LEN
                 IF WS-LEN > 0
                    MOVE WS-DATA(1:WS-LEN) TO LK-DATA(1:WS-LEN)
                 END-IF

              WHEN 'RESET   '
                 MOVE 0      TO WS-COUNT
                 MOVE SPACES TO WS-TRANID
                 MOVE 0      TO WS-LEN

              WHEN OTHER
                 CONTINUE
           END-EVALUATE
           GOBACK.
