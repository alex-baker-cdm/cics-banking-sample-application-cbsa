      ******************************************************************
      * bnkmenuTest - isolated unit test for BNKMENU (main menu screen).
      *
      * BNKMENU is a pseudo-conversational 3270 program: it RECEIVEs the
      * menu map, validates the ACTION option and, for a valid option,
      * RETURNs (TRANSID) to the chosen transaction; an invalid option
      * redisplays the map with an error message. It makes no business
      * LINK.
      *
      * The driver simulates the operator input map + AID via the BMS /
      * AID doubles, invokes BNKMENU and inspects the transaction it
      * handed off to (CICSRETN) and the redisplayed message (BMS GETOUT).
      *
      * Assertions:
      *   1. happy : ACTION '2' routes to transaction 'ODAC'.
      *   2. edge  : ACTION '9' redisplays with the validation message
      *              and returns to itself ('OMEN').
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. BNKMENUTEST.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
       COPY BNK1MAI.
       COPY DFHAID.

       01 WS-COMMAREA             PIC X            VALUE SPACE.
       01 WS-LEN                  PIC 9(9) COMP-5  VALUE 0.
       01 WS-R1                   PIC S9(8) COMP   VALUE 0.
       01 WS-R2                   PIC S9(8) COMP   VALUE 0.
       01 WS-CALEN                PIC S9(4) COMP   VALUE 0.
       01 WS-TRANID               PIC X(4)         VALUE SPACES.
       01 WS-RETLEN               PIC 9(9) COMP-5  VALUE 0.
       01 WS-RETBUF               PIC X(256)       VALUE SPACES.

       01 OP-RESET                PIC X(8)         VALUE 'RESET   '.
       01 OP-PUTIN                PIC X(8)         VALUE 'PUTIN   '.
       01 OP-GETOUT               PIC X(8)         VALUE 'GETOUT  '.
       01 OP-GET                  PIC X(8)         VALUE 'GET     '.
       01 OP-SET                  PIC X(3)         VALUE 'SET'.

       01 WS-FAILURES             PIC 9(4)         VALUE 0.

       PROCEDURE DIVISION.
       MAIN-A.
           DISPLAY "=== bnkmenuTest : BNKMENU (menu routing) ==="

      *    ---- Test 1: valid option '2' routes to ODAC ----
           PERFORM RESET-STATE
           MOVE LOW-VALUES TO BNK1MEI
           MOVE '2' TO ACTIONI
           MOVE 1   TO ACTIONL
           MOVE LENGTH OF BNK1MEI TO WS-LEN
           CALL 'CICSBMS' USING OP-PUTIN WS-LEN BNK1MEI WS-R1 WS-R2
           MOVE 1 TO WS-CALEN
           CALL 'CICSAID' USING OP-SET DFHENTER WS-CALEN

           CALL 'BNKMENU' USING WS-COMMAREA

           MOVE SPACES TO WS-TRANID
           CALL 'CICSRETN' USING OP-GET WS-TRANID WS-RETLEN WS-RETBUF
           DISPLAY "    routed-to = [" WS-TRANID "]"
           IF WS-TRANID = 'ODAC'
              DISPLAY "PASS: valid option '2' routed to ODAC"
           ELSE
              DISPLAY "FAIL: expected ODAC, got [" WS-TRANID "]"
              ADD 1 TO WS-FAILURES
           END-IF

      *    ---- Test 2: invalid option '9' redisplays with error ----
           PERFORM RESET-STATE
           MOVE LOW-VALUES TO BNK1MEI
           MOVE '9' TO ACTIONI
           MOVE 1   TO ACTIONL
           MOVE LENGTH OF BNK1MEI TO WS-LEN
           CALL 'CICSBMS' USING OP-PUTIN WS-LEN BNK1MEI WS-R1 WS-R2
           MOVE 1 TO WS-CALEN
           CALL 'CICSAID' USING OP-SET DFHENTER WS-CALEN

           CALL 'BNKMENU' USING WS-COMMAREA

           MOVE LENGTH OF BNK1MEI TO WS-LEN
           CALL 'CICSBMS' USING OP-GETOUT WS-LEN BNK1MEI WS-R1 WS-R2
           MOVE SPACES TO WS-TRANID
           CALL 'CICSRETN' USING OP-GET WS-TRANID WS-RETLEN WS-RETBUF
           DISPLAY "    message   = [" MESSAGEO "]"
           DISPLAY "    routed-to = [" WS-TRANID "]"
           IF MESSAGEO (1:28) = "You must enter a valid value"
              AND WS-TRANID = 'OMEN'
              DISPLAY "PASS: invalid option redisplayed with error"
           ELSE
              DISPLAY "FAIL: invalid option not handled as expected"
              ADD 1 TO WS-FAILURES
           END-IF

           IF WS-FAILURES = 0
              DISPLAY "RESULT: bnkmenuTest PASSED"
              MOVE 0 TO RETURN-CODE
           ELSE
              DISPLAY "RESULT: bnkmenuTest FAILED (" WS-FAILURES ")"
              MOVE 1 TO RETURN-CODE
           END-IF
           STOP RUN.

       RESET-STATE.
           MOVE LENGTH OF BNK1MEI TO WS-LEN
           CALL 'CICSBMS'  USING OP-RESET WS-LEN BNK1MEI WS-R1 WS-R2
           CALL 'CICSRETN' USING OP-RESET WS-TRANID WS-RETLEN WS-RETBUF.
