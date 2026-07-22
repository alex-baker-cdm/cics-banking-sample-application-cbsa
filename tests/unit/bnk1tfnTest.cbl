      ******************************************************************
      * bnk1tfnTest - isolated unit test for BNK1TFN (Transfer screen).
      *
      * BNK1TFN RECEIVEs the transfer map, validates the FROM/TO accounts
      * and amount, then LINKs XFRFUN with a SUBPGM-PARMS COMMAREA and
      * displays the result / any business fail code.
      *
      * The driver scripts XFRFUN's reply via CICSLINK/LINKREC, invokes
      * the screen and asserts (a) the COMMAREA passed to XFRFUN, (b) a
      * business fail code surfaced correctly and (c) pins the fail-code-3
      * fall-through: WHEN '3' does NOT ``GO TO GCD999`` like the other
      * fail codes, so the intended "unexpected error" text is immediately
      * overwritten by the trailing "unable to determine success" branch.
      * This test pins that existing (buggy) behaviour; production source
      * is unchanged.
      *
      * SUBPGM-PARMS below mirrors the layout in src/base/cobol_src/
      * BNK1TFN.cbl.
      *
      * Assertions:
      *   1. happy : valid transfer -> correct XFRFUN COMMAREA + success.
      *   2. edge  : fail code 1 -> FROM-account-not-found message.
      *   3. pin   : fail code 3 -> "unable to determine success" (bug).
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. BNK1TFNTEST.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
       COPY BNK1TFM.
       COPY DFHAID.

      * DFHCOMMAREA passed to BNK1TFN (28 bytes).
       01 WS-COMMAREA.
          03 CA-FACCNO            PIC 9(8)         VALUE 0.
          03 CA-TACCNO            PIC 9(8)         VALUE 0.
          03 CA-AMT               PIC 9(12)        VALUE 0.

      * Mirror of BNK1TFN's SUBPGM-PARMS (the XFRFUN COMMAREA).
       01 SUBPGM-PARMS.
          03 SUBPGM-FACCNO        PIC 9(8).
          03 SUBPGM-FSCODE        PIC 9(6).
          03 SUBPGM-TACCNO        PIC 9(8).
          03 SUBPGM-TSCODE        PIC 9(6).
          03 SUBPGM-AMT           PIC S9(10)V99.
          03 SUBPGM-FAVBAL        PIC S9(10)V99.
          03 SUBPGM-FACTBAL       PIC S9(10)V99.
          03 SUBPGM-TAVBAL        PIC S9(10)V99.
          03 SUBPGM-TACTBAL       PIC S9(10)V99.
          03 SUBPGM-FAIL-CODE     PIC X.
          03 SUBPGM-SUCCESS       PIC X.

       01 WS-LEN                  PIC 9(9) COMP-5  VALUE 0.
       01 WS-R1                   PIC S9(8) COMP   VALUE 0.
       01 WS-R2                   PIC S9(8) COMP   VALUE 0.
       01 WS-CALEN                PIC S9(4) COMP   VALUE 0.
       01 WS-TRANID               PIC X(4)         VALUE SPACES.
       01 WS-RETLEN               PIC 9(9) COMP-5  VALUE 0.
       01 WS-RETBUF               PIC X(256)       VALUE SPACES.
       01 WS-CAP-PROG             PIC X(8)         VALUE SPACES.
       01 WS-CAP-LEN              PIC 9(9) COMP-5  VALUE 0.

       01 WS-XFRFUN               PIC X(8)         VALUE 'XFRFUN  '.
       01 OP-RESET                PIC X(8)         VALUE 'RESET   '.
       01 OP-PUTIN                PIC X(8)         VALUE 'PUTIN   '.
       01 OP-GETOUT               PIC X(8)         VALUE 'GETOUT  '.
       01 OP-GET                  PIC X(8)         VALUE 'GET     '.
       01 OP-SCRIPT               PIC X(8)         VALUE 'SCRIPT  '.
       01 OP-SET                  PIC X(3)         VALUE 'SET'.

       01 WS-FAILURES             PIC 9(4)         VALUE 0.

       PROCEDURE DIVISION.
       MAIN-A.
           DISPLAY "=== bnk1tfnTest : BNK1TFN (transfer) ==="

      *    ---- Test 1: valid transfer -> XFRFUN + success ----
           PERFORM RESET-STATE

           INITIALIZE SUBPGM-PARMS
           MOVE 'Y' TO SUBPGM-SUCCESS
           MOVE LENGTH OF SUBPGM-PARMS TO WS-LEN
           CALL 'LINKREC' USING OP-SCRIPT WS-XFRFUN WS-LEN SUBPGM-PARMS

           MOVE LOW-VALUES TO BNK1TFI
           MOVE '00000123' TO FACCNOI
           MOVE 8  TO FACCNOL
           MOVE '00000456' TO TACCNOI
           MOVE 8  TO TACCNOL
           MOVE '10000' TO AMTI (1:5)
           MOVE 5  TO AMTL
           MOVE LENGTH OF BNK1TFI TO WS-LEN
           CALL 'CICSBMS' USING OP-PUTIN WS-LEN BNK1TFI WS-R1 WS-R2
           MOVE 28 TO WS-CALEN
           CALL 'CICSAID' USING OP-SET DFHENTER WS-CALEN

           CALL 'BNK1TFN' USING WS-COMMAREA

           MOVE LENGTH OF BNK1TFI TO WS-LEN
           CALL 'CICSBMS' USING OP-GETOUT WS-LEN BNK1TFI WS-R1 WS-R2
           MOVE SPACES TO WS-TRANID
           CALL 'CICSRETN' USING OP-GET WS-TRANID WS-RETLEN WS-RETBUF
           INITIALIZE SUBPGM-PARMS
           MOVE SPACES TO WS-CAP-PROG
           CALL 'LINKREC' USING OP-GET WS-CAP-PROG WS-CAP-LEN
                                SUBPGM-PARMS

           DISPLAY "    linked-to = [" WS-CAP-PROG "]"
           DISPLAY "    sub-faccno= [" SUBPGM-FACCNO "]"
           DISPLAY "    sub-taccno= [" SUBPGM-TACCNO "]"
           DISPLAY "    sub-amt   = [" SUBPGM-AMT "]"
           DISPLAY "    message   = [" MESSAGEO "]"
           DISPLAY "    routed-to = [" WS-TRANID "]"
           IF WS-CAP-PROG = 'XFRFUN  '
              AND SUBPGM-FACCNO = 00000123
              AND SUBPGM-TACCNO = 00000456
              AND SUBPGM-AMT = 10000.00
              AND MESSAGEO (1:30) = "Transfer successfully applied."
              AND WS-TRANID = 'OTFN'
              DISPLAY "PASS: valid transfer -> correct XFRFUN COMMAREA"
           ELSE
              DISPLAY "FAIL: transfer path did not match expectations"
              ADD 1 TO WS-FAILURES
           END-IF

      *    ---- Test 2: business fail code 1 surfaced ----
           PERFORM RESET-STATE
           INITIALIZE SUBPGM-PARMS
           MOVE 'N' TO SUBPGM-SUCCESS
           MOVE '1' TO SUBPGM-FAIL-CODE
           MOVE LENGTH OF SUBPGM-PARMS TO WS-LEN
           CALL 'LINKREC' USING OP-SCRIPT WS-XFRFUN WS-LEN SUBPGM-PARMS

           PERFORM DRIVE-TRANSFER

           DISPLAY "    message   = [" MESSAGEO "]"
           IF MESSAGEO (1:38) = "Sorry the FROM ACCOUNT no was not foun"
              DISPLAY "PASS: fail code 1 -> FROM-not-found message"
           ELSE
              DISPLAY "FAIL: fail code 1 message incorrect"
              ADD 1 TO WS-FAILURES
           END-IF

      *    ---- Test 3: fail code 3 fall-through (regression pin) ----
           PERFORM RESET-STATE
           INITIALIZE SUBPGM-PARMS
           MOVE 'N' TO SUBPGM-SUCCESS
           MOVE '3' TO SUBPGM-FAIL-CODE
           MOVE LENGTH OF SUBPGM-PARMS TO WS-LEN
           CALL 'LINKREC' USING OP-SCRIPT WS-XFRFUN WS-LEN SUBPGM-PARMS

           PERFORM DRIVE-TRANSFER

           DISPLAY "    message   = [" MESSAGEO "]"
      *    WHEN '3' has no GO TO GCD999, so the intended "unexpected
      *    error" text is overwritten by the trailing branch below it.
           IF MESSAGEO (45:27) = "unable to determine success"
              DISPLAY "PASS: fail code 3 falls through (bug pinned)"
           ELSE
              DISPLAY "FAIL: fail code 3 behaviour changed"
              ADD 1 TO WS-FAILURES
           END-IF

           IF WS-FAILURES = 0
              DISPLAY "RESULT: bnk1tfnTest PASSED"
              MOVE 0 TO RETURN-CODE
           ELSE
              DISPLAY "RESULT: bnk1tfnTest FAILED (" WS-FAILURES ")"
              MOVE 1 TO RETURN-CODE
           END-IF
           STOP RUN.

      * Drive a standard valid-input transfer turn (input already scripted).
       DRIVE-TRANSFER.
           MOVE LOW-VALUES TO BNK1TFI
           MOVE '00000123' TO FACCNOI
           MOVE 8  TO FACCNOL
           MOVE '00000456' TO TACCNOI
           MOVE 8  TO TACCNOL
           MOVE '10000' TO AMTI (1:5)
           MOVE 5  TO AMTL
           MOVE LENGTH OF BNK1TFI TO WS-LEN
           CALL 'CICSBMS' USING OP-PUTIN WS-LEN BNK1TFI WS-R1 WS-R2
           MOVE 28 TO WS-CALEN
           CALL 'CICSAID' USING OP-SET DFHENTER WS-CALEN
           CALL 'BNK1TFN' USING WS-COMMAREA
           MOVE LENGTH OF BNK1TFI TO WS-LEN
           CALL 'CICSBMS' USING OP-GETOUT WS-LEN BNK1TFI WS-R1 WS-R2.

       RESET-STATE.
           MOVE LENGTH OF BNK1TFI TO WS-LEN
           CALL 'CICSBMS'  USING OP-RESET WS-LEN BNK1TFI WS-R1 WS-R2
           CALL 'CICSRETN' USING OP-RESET WS-TRANID WS-RETLEN WS-RETBUF
           CALL 'LINKREC'  USING OP-RESET WS-CAP-PROG WS-CAP-LEN
                                 SUBPGM-PARMS.
