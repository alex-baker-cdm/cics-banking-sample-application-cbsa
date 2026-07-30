      ******************************************************************
      * bnk1craTest - isolated unit test for BNK1CRA (Credit/Debit screen).
      *
      * BNK1CRA RECEIVEs the credit/debit map, validates the account
      * number / sign / amount, then LINKs the business program DBCRFUN
      * with a SUBPGM-PARMS COMMAREA and displays the result.
      *
      * The driver scripts DBCRFUN's reply via the CICSLINK/LINKREC
      * double, invokes the screen and asserts (a) the exact COMMAREA the
      * screen passed to DBCRFUN, and (b) the success message displayed.
      * The edge case asserts a validation error is shown and NO business
      * LINK is made.
      *
      * SUBPGM-PARMS below mirrors the layout in src/base/cobol_src/
      * BNK1CRA.cbl (it is a WORKING-STORAGE record there, not a copybook).
      *
      * Assertions:
      *   1. happy : valid amount -> correct DBCRFUN COMMAREA + success.
      *   2. edge  : zero account -> validation error, no LINK.
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. BNK1CRATEST.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
       COPY BNK1CDM.
       COPY DFHAID.

      * DFHCOMMAREA passed to BNK1CRA (COMM-ACCNO/SIGN/AMT = 21 bytes).
       01 WS-COMMAREA.
          03 CA-ACCNO             PIC X(8)         VALUE SPACES.
          03 CA-SIGN              PIC X            VALUE SPACE.
          03 CA-AMT               PIC 9(12)        VALUE 0.

      * Mirror of BNK1CRA's SUBPGM-PARMS (the DBCRFUN COMMAREA).
       01 SUBPGM-PARMS.
          03 SUBPGM-ACCNO         PIC X(8).
          03 SUBPGM-AMT           PIC S9(10)V99.
          03 SUBPGM-SORTC         PIC 9(6).
          03 SUBPGM-AV-BAL        PIC S9(10)V99.
          03 SUBPGM-ACT-BAL       PIC S9(10)V99.
          03 SUBPGM-ORIGIN.
             05 SUBPGM-APPLID        PIC X(8).
             05 SUBPGM-USERID        PIC X(8).
             05 SUBPGM-FACILITY-NAME PIC X(8).
             05 SUBPGM-NETWRK-ID     PIC X(8).
             05 SUBPGM-FACILTYPE     PIC S9(8) COMP.
             05 FILLER               PIC X(4).
          03 SUBPGM-SUCCESS       PIC X.
          03 SUBPGM-FAIL-CODE     PIC X.

       01 WS-LEN                  PIC 9(9) COMP-5  VALUE 0.
       01 WS-R1                   PIC S9(8) COMP   VALUE 0.
       01 WS-R2                   PIC S9(8) COMP   VALUE 0.
       01 WS-CALEN                PIC S9(4) COMP   VALUE 0.
       01 WS-TRANID               PIC X(4)         VALUE SPACES.
       01 WS-RETLEN               PIC 9(9) COMP-5  VALUE 0.
       01 WS-RETBUF               PIC X(256)       VALUE SPACES.
       01 WS-CAP-PROG             PIC X(8)         VALUE SPACES.
       01 WS-CAP-LEN              PIC 9(9) COMP-5  VALUE 0.

       01 WS-DBCRFUN              PIC X(8)         VALUE 'DBCRFUN '.
       01 OP-RESET                PIC X(8)         VALUE 'RESET   '.
       01 OP-PUTIN                PIC X(8)         VALUE 'PUTIN   '.
       01 OP-GETOUT               PIC X(8)         VALUE 'GETOUT  '.
       01 OP-GET                  PIC X(8)         VALUE 'GET     '.
       01 OP-SCRIPT               PIC X(8)         VALUE 'SCRIPT  '.
       01 OP-SET                  PIC X(3)         VALUE 'SET'.

       01 WS-FAILURES             PIC 9(4)         VALUE 0.

       PROCEDURE DIVISION.
       MAIN-A.
           DISPLAY "=== bnk1craTest : BNK1CRA (credit/debit) ==="

      *    ---- Test 1: valid credit -> DBCRFUN + success ----
           PERFORM RESET-STATE

      *    Script DBCRFUN to report success.
           INITIALIZE SUBPGM-PARMS
           MOVE 'Y' TO SUBPGM-SUCCESS
           MOVE LENGTH OF SUBPGM-PARMS TO WS-LEN
           CALL 'LINKREC' USING OP-SCRIPT WS-DBCRFUN WS-LEN SUBPGM-PARMS

           MOVE LOW-VALUES TO BNK1CDI
           MOVE '00000123' TO ACCNOI
           MOVE 8   TO ACCNOL
           MOVE '+' TO SIGNI
           MOVE 1   TO SIGNL
           MOVE '10000' TO AMTI (1:5)
           MOVE 5   TO AMTL
           MOVE LENGTH OF BNK1CDI TO WS-LEN
           CALL 'CICSBMS' USING OP-PUTIN WS-LEN BNK1CDI WS-R1 WS-R2
           MOVE 21 TO WS-CALEN
           CALL 'CICSAID' USING OP-SET DFHENTER WS-CALEN

           CALL 'BNK1CRA' USING WS-COMMAREA

           MOVE LENGTH OF BNK1CDI TO WS-LEN
           CALL 'CICSBMS' USING OP-GETOUT WS-LEN BNK1CDI WS-R1 WS-R2
           MOVE SPACES TO WS-TRANID
           CALL 'CICSRETN' USING OP-GET WS-TRANID WS-RETLEN WS-RETBUF
           INITIALIZE SUBPGM-PARMS
           MOVE SPACES TO WS-CAP-PROG
           CALL 'LINKREC' USING OP-GET WS-CAP-PROG WS-CAP-LEN
                                SUBPGM-PARMS

           DISPLAY "    linked-to = [" WS-CAP-PROG "]"
           DISPLAY "    sub-accno = [" SUBPGM-ACCNO "]"
           DISPLAY "    sub-amt   = [" SUBPGM-AMT "]"
           DISPLAY "    message   = [" MESSAGEO "]"
           DISPLAY "    routed-to = [" WS-TRANID "]"
           IF WS-CAP-PROG = 'DBCRFUN '
              AND SUBPGM-ACCNO = '00000123'
              AND SUBPGM-AMT = 10000.00
              AND MESSAGEO (1:19) = "Amount successfully"
              AND WS-TRANID = 'OCRA'
              DISPLAY "PASS: valid credit -> correct DBCRFUN COMMAREA"
           ELSE
              DISPLAY "FAIL: credit path did not match expectations"
              ADD 1 TO WS-FAILURES
           END-IF

      *    ---- Test 2: zero account -> validation error, no LINK ----
           PERFORM RESET-STATE
           MOVE LOW-VALUES TO BNK1CDI
           MOVE SPACES TO ACCNOI
           MOVE 8   TO ACCNOL
           MOVE '+' TO SIGNI
           MOVE 1   TO SIGNL
           MOVE '10000' TO AMTI (1:5)
           MOVE 5   TO AMTL
           MOVE LENGTH OF BNK1CDI TO WS-LEN
           CALL 'CICSBMS' USING OP-PUTIN WS-LEN BNK1CDI WS-R1 WS-R2
           MOVE 21 TO WS-CALEN
           CALL 'CICSAID' USING OP-SET DFHENTER WS-CALEN

           CALL 'BNK1CRA' USING WS-COMMAREA

           MOVE LENGTH OF BNK1CDI TO WS-LEN
           CALL 'CICSBMS' USING OP-GETOUT WS-LEN BNK1CDI WS-R1 WS-R2
           MOVE SPACES TO WS-CAP-PROG
           CALL 'LINKREC' USING OP-GET WS-CAP-PROG WS-CAP-LEN
                                SUBPGM-PARMS
           DISPLAY "    message   = [" MESSAGEO "]"
           DISPLAY "    linked-to = [" WS-CAP-PROG "]"
           IF MESSAGEO (1:23) = "Please enter a non zero"
              AND WS-CAP-PROG = SPACES
              DISPLAY "PASS: invalid account -> error shown, no LINK"
           ELSE
              DISPLAY "FAIL: invalid account not handled as expected"
              ADD 1 TO WS-FAILURES
           END-IF

           IF WS-FAILURES = 0
              DISPLAY "RESULT: bnk1craTest PASSED"
              MOVE 0 TO RETURN-CODE
           ELSE
              DISPLAY "RESULT: bnk1craTest FAILED (" WS-FAILURES ")"
              MOVE 1 TO RETURN-CODE
           END-IF
           STOP RUN.

       RESET-STATE.
           MOVE LENGTH OF BNK1CDI TO WS-LEN
           CALL 'CICSBMS'  USING OP-RESET WS-LEN BNK1CDI WS-R1 WS-R2
           CALL 'CICSRETN' USING OP-RESET WS-TRANID WS-RETLEN WS-RETBUF
           CALL 'LINKREC'  USING OP-RESET WS-CAP-PROG WS-CAP-LEN
                                 SUBPGM-PARMS.
