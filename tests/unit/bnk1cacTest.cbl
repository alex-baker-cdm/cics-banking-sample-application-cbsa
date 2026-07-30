      ******************************************************************
      * bnk1cacTest - isolated unit test for BNK1CAC (Create Account).
      *
      * BNK1CAC RECEIVEs the create-account map, runs rich field
      * validation (EDIT-DATA), then LINKs CREACC with a SUBPGM-PARMS
      * COMMAREA and displays the result.
      *
      * The driver scripts CREACC's reply via CICSLINK/LINKREC, invokes
      * the screen and asserts the COMMAREA the screen passed to CREACC.
      * The edge case exercises a representative validation failure
      * (missing customer number) and asserts no business LINK is made.
      *
      * SUBPGM-PARMS below mirrors the layout in src/base/cobol_src/
      * BNK1CAC.cbl.
      *
      * Assertions:
      *   1. happy : valid fields -> correct CREACC COMMAREA + success.
      *   2. edge  : missing customer number -> validation error, no LINK.
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. BNK1CACTEST.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
       COPY BNK1CAM.
       COPY DFHAID.

      * DFHCOMMAREA passed to BNK1CAC (32 bytes).
       01 WS-COMMAREA.
          03 CA-CUSTNO            PIC 9(10)        VALUE 0.
          03 CA-ACCTYPE           PIC X(8)         VALUE SPACES.
          03 CA-INTRT             PIC 9(4)V99      VALUE 0.
          03 CA-OVERDR            PIC 9(8)         VALUE 0.

      * Mirror of BNK1CAC's SUBPGM-PARMS (the CREACC COMMAREA).
       01 SUBPGM-PARMS.
          03 SUBPGM-EYECATCHER    PIC X(4).
          03 SUBPGM-CUSTNO        PIC 9(10).
          03 SUBPGM-KEY.
             05 SUBPGM-SORTCODE      PIC 9(6) DISPLAY.
             05 SUBPGM-NUMBER        PIC 9(8) DISPLAY.
          03 SUBPGM-ACC-TYPE      PIC X(8).
          03 SUBPGM-INT-RT        PIC 9(4)V99.
          03 SUBPGM-OPENED        PIC 9(8).
          03 SUBPGM-OVERDR-LIM    PIC 9(8).
          03 SUBPGM-LAST-STMT-DT  PIC 9(8).
          03 SUBPGM-NEXT-STMT-DT  PIC 9(8).
          03 SUBPGM-AVAIL-BAL     PIC S9(10)V99.
          03 SUBPGM-ACT-BAL       PIC S9(10)V99.
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

       01 WS-CREACC               PIC X(8)         VALUE 'CREACC  '.
       01 OP-RESET                PIC X(8)         VALUE 'RESET   '.
       01 OP-PUTIN                PIC X(8)         VALUE 'PUTIN   '.
       01 OP-GETOUT               PIC X(8)         VALUE 'GETOUT  '.
       01 OP-GET                  PIC X(8)         VALUE 'GET     '.
       01 OP-SCRIPT               PIC X(8)         VALUE 'SCRIPT  '.
       01 OP-SET                  PIC X(3)         VALUE 'SET'.

       01 WS-FAILURES             PIC 9(4)         VALUE 0.

       PROCEDURE DIVISION.
       MAIN-A.
           DISPLAY "=== bnk1cacTest : BNK1CAC (create account) ==="

      *    ---- Test 1: valid new account -> CREACC + success ----
           PERFORM RESET-STATE

           INITIALIZE SUBPGM-PARMS
           MOVE 'Y' TO SUBPGM-SUCCESS
           MOVE LENGTH OF SUBPGM-PARMS TO WS-LEN
           CALL 'LINKREC' USING OP-SCRIPT WS-CREACC WS-LEN SUBPGM-PARMS

           MOVE LOW-VALUES TO BNK1CAI
           MOVE '0000000123' TO CUSTNOI
           MOVE 10 TO CUSTNOL
           MOVE 'CURRENT ' TO ACCTYPI
           MOVE 7  TO ACCTYPL
           MOVE '0002' TO INTRTI
           MOVE 4  TO INTRTL
           MOVE '00001000' TO OVERDRI
           MOVE 8  TO OVERDRL
           MOVE LENGTH OF BNK1CAI TO WS-LEN
           CALL 'CICSBMS' USING OP-PUTIN WS-LEN BNK1CAI WS-R1 WS-R2
           MOVE 32 TO WS-CALEN
           CALL 'CICSAID' USING OP-SET DFHENTER WS-CALEN

           CALL 'BNK1CAC' USING WS-COMMAREA

           MOVE LENGTH OF BNK1CAI TO WS-LEN
           CALL 'CICSBMS' USING OP-GETOUT WS-LEN BNK1CAI WS-R1 WS-R2
           INITIALIZE SUBPGM-PARMS
           MOVE SPACES TO WS-CAP-PROG
           CALL 'LINKREC' USING OP-GET WS-CAP-PROG WS-CAP-LEN
                                SUBPGM-PARMS

           DISPLAY "    linked-to = [" WS-CAP-PROG "]"
           DISPLAY "    eyecatcher= [" SUBPGM-EYECATCHER "]"
           DISPLAY "    custno    = [" SUBPGM-CUSTNO "]"
           DISPLAY "    acc-type  = [" SUBPGM-ACC-TYPE "]"
           DISPLAY "    int-rt    = [" SUBPGM-INT-RT "]"
           DISPLAY "    overdr    = [" SUBPGM-OVERDR-LIM "]"
           DISPLAY "    message   = [" MESSAGEO "]"
           IF WS-CAP-PROG = 'CREACC  '
              AND SUBPGM-EYECATCHER = 'ACCT'
              AND SUBPGM-CUSTNO = 123
              AND SUBPGM-ACC-TYPE = 'CURRENT '
              AND SUBPGM-INT-RT = 2.00
              AND SUBPGM-OVERDR-LIM = 1000
              AND MESSAGEO (1:40) =
                  "The Account has been successfully create"
              DISPLAY "PASS: valid fields -> correct CREACC COMMAREA"
           ELSE
              DISPLAY "FAIL: create path did not match expectations"
              ADD 1 TO WS-FAILURES
           END-IF

      *    ---- Test 2: missing customer number -> error, no LINK ----
           PERFORM RESET-STATE
           MOVE LOW-VALUES TO BNK1CAI
           MOVE SPACES TO CUSTNOI
           MOVE 0  TO CUSTNOL
           MOVE 'CURRENT ' TO ACCTYPI
           MOVE 7  TO ACCTYPL
           MOVE '0002' TO INTRTI
           MOVE 4  TO INTRTL
           MOVE '00001000' TO OVERDRI
           MOVE 8  TO OVERDRL
           MOVE LENGTH OF BNK1CAI TO WS-LEN
           CALL 'CICSBMS' USING OP-PUTIN WS-LEN BNK1CAI WS-R1 WS-R2
           MOVE 32 TO WS-CALEN
           CALL 'CICSAID' USING OP-SET DFHENTER WS-CALEN

           CALL 'BNK1CAC' USING WS-COMMAREA

           MOVE LENGTH OF BNK1CAI TO WS-LEN
           CALL 'CICSBMS' USING OP-GETOUT WS-LEN BNK1CAI WS-R1 WS-R2
           MOVE SPACES TO WS-CAP-PROG
           CALL 'LINKREC' USING OP-GET WS-CAP-PROG WS-CAP-LEN
                                SUBPGM-PARMS
           DISPLAY "    message   = [" MESSAGEO "]"
           DISPLAY "    linked-to = [" WS-CAP-PROG "]"
           IF MESSAGEO (1:32) = "Please enter a 10 digit Customer"
              AND WS-CAP-PROG = SPACES
              DISPLAY "PASS: missing custno -> error shown, no LINK"
           ELSE
              DISPLAY "FAIL: validation failure not handled as expected"
              ADD 1 TO WS-FAILURES
           END-IF

           IF WS-FAILURES = 0
              DISPLAY "RESULT: bnk1cacTest PASSED"
              MOVE 0 TO RETURN-CODE
           ELSE
              DISPLAY "RESULT: bnk1cacTest FAILED (" WS-FAILURES ")"
              MOVE 1 TO RETURN-CODE
           END-IF
           STOP RUN.

       RESET-STATE.
           MOVE LENGTH OF BNK1CAI TO WS-LEN
           CALL 'CICSBMS'  USING OP-RESET WS-LEN BNK1CAI WS-R1 WS-R2
           CALL 'CICSRETN' USING OP-RESET WS-TRANID WS-RETLEN WS-RETBUF
           CALL 'LINKREC'  USING OP-RESET WS-CAP-PROG WS-CAP-LEN
                                 SUBPGM-PARMS.
