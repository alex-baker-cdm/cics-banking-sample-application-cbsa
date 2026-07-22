      ******************************************************************
      * xfrfunTest - isolated unit test for XFRFUN (same-bank transfer:
      * two ACCOUNT updates + a PROCTRAN audit row, atomic, with ABEND
      * failure paths and a Db2 -911 deadlock retry loop).
      *
      * The harness lets a driver both capture the CICS abend code
      * (CICSABND READ) and script SQLCODE / SQLERRD(3) (DB2ACC SETSQL),
      * which is what makes XFRFUN's abend and retry paths assertable.
      *
      * Assertions:
      *   1. success  : both balances move, PROCTRAN 'TFR' row written.
      *   2. same acc  : from = to -> XFRFUN abends 'SAME' (captured).
      *   3. to not found : rejected (fail '2'), no balance change.
      *   4. deadlock : first TO read returns -911 / SQLERRD(3)=13172872;
      *                 XFRFUN rolls back, retries, and eventually
      *                 succeeds (balances move, PROCTRAN written).
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. XFRFUNTEST.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 COMM-AREA-XFR.
          COPY XFRFUN.

       01 FIX-ROW.
          COPY HOSTACCT.
       01 FIX-PROC.
          COPY HOSTPROC.
       COPY SQLCA.

       01 WS-SC                           PIC X(6)  VALUE "987654".
       01 WS-AN                           PIC X(8)  VALUE SPACES.
       01 WS-CN                           PIC X(10) VALUE SPACES.

       01 OP-CLEAR                        PIC X(8) VALUE "CLEAR".
       01 OP-INSERT                       PIC X(8) VALUE "INSERT".
       01 OP-SELKEY                       PIC X(8) VALUE "SELKEY".
       01 OP-SETSQL                       PIC X(8) VALUE "SETSQL".
       01 OP-COUNT                        PIC X(8) VALUE "COUNT".
       01 OP-GETLAST                      PIC X(8) VALUE "GETLAST".
       01 OP-READ                         PIC X(8) VALUE "READ".
       01 OP-RESET                        PIC X(8) VALUE "RESET".

       01 WS-ABCODE                       PIC X(4) VALUE SPACES.
       01 WS-FLAG                         PIC X    VALUE SPACES.

       01 WS-INS-ACCNO                    PIC X(8) VALUE SPACES.
       01 WS-INS-BAL                      PIC S9(10)V99 VALUE 0.

       01 WS-FAILURES                     PIC 9(4) VALUE 0.

       PROCEDURE DIVISION.
       MAIN-A.
           DISPLAY "=== xfrfunTest : XFRFUN (transfer + abend + -911)"

      *    ---- Test 1: successful transfer (acc1 -> acc2) ----
           PERFORM CLEAR-TABLES
           MOVE "00000001" TO WS-INS-ACCNO
           MOVE 1000.00    TO WS-INS-BAL
           PERFORM INSERT-ACCT
           MOVE "00000002" TO WS-INS-ACCNO
           MOVE 500.00     TO WS-INS-BAL
           PERFORM INSERT-ACCT

           INITIALIZE COMM-AREA-XFR
           MOVE 1 TO COMM-FACCNO
           MOVE 2 TO COMM-TACCNO
           MOVE 300.00 TO COMM-AMT
           CALL 'XFRFUN' USING COMM-AREA-XFR

           IF COMM-SUCCESS = 'Y'
              DISPLAY "PASS: transfer reported success"
           ELSE
              DISPLAY "FAIL: transfer failed (fc=" COMM-FAIL-CODE ")"
              ADD 1 TO WS-FAILURES
           END-IF
           MOVE "00000001" TO WS-AN
           CALL 'DB2ACC' USING OP-SELKEY WS-SC WS-AN WS-CN
                               FIX-ROW SQLCA
           IF FIX-AVAILBAL = 700.00
              DISPLAY "PASS: FROM account debited to 700.00"
           ELSE
              DISPLAY "FAIL: FROM balance = " FIX-AVAILBAL
              ADD 1 TO WS-FAILURES
           END-IF
           MOVE "00000002" TO WS-AN
           CALL 'DB2ACC' USING OP-SELKEY WS-SC WS-AN WS-CN
                               FIX-ROW SQLCA
           IF FIX-AVAILBAL = 800.00
              DISPLAY "PASS: TO account credited to 800.00"
           ELSE
              DISPLAY "FAIL: TO balance = " FIX-AVAILBAL
              ADD 1 TO WS-FAILURES
           END-IF
           CALL 'DB2PROC' USING OP-COUNT FIX-PROC SQLCA
           CALL 'DB2PROC' USING OP-GETLAST FIX-PROC SQLCA
           IF SQLERRD(1) = 1 AND FIXP-TYPE = "TFR"
              AND FIXP-AMOUNT = 300.00
              DISPLAY "PASS: PROCTRAN TFR row for 300.00"
           ELSE
              DISPLAY "FAIL: PROCTRAN row (n=" SQLERRD(1)
                 " type=" FIXP-TYPE " amt=" FIXP-AMOUNT ")"
              ADD 1 TO WS-FAILURES
           END-IF

      *    ---- Test 2: from = to -> SAME abend captured ----
           PERFORM CLEAR-TABLES
           MOVE "00000001" TO WS-INS-ACCNO
           MOVE 1000.00    TO WS-INS-BAL
           PERFORM INSERT-ACCT
           CALL 'CICSABND' USING OP-RESET WS-ABCODE WS-FLAG

           INITIALIZE COMM-AREA-XFR
           MOVE 1 TO COMM-FACCNO
           MOVE 1 TO COMM-TACCNO
           MOVE 50.00 TO COMM-AMT
           CALL 'XFRFUN' USING COMM-AREA-XFR

           CALL 'CICSABND' USING OP-READ WS-ABCODE WS-FLAG
           IF WS-FLAG = 'Y' AND WS-ABCODE = "SAME"
              DISPLAY "PASS: same-account transfer abended 'SAME'"
           ELSE
              DISPLAY "FAIL: SAME not captured (flag=" WS-FLAG
                 " code=[" WS-ABCODE "])"
              ADD 1 TO WS-FAILURES
           END-IF

      *    ---- Test 3: TO account not found (processed TO-first) ----
           PERFORM CLEAR-TABLES
           MOVE "00000008" TO WS-INS-ACCNO
           MOVE 1000.00    TO WS-INS-BAL
           PERFORM INSERT-ACCT
           CALL 'CICSABND' USING OP-RESET WS-ABCODE WS-FLAG

           INITIALIZE COMM-AREA-XFR
           MOVE 8 TO COMM-FACCNO
           MOVE 3 TO COMM-TACCNO
           MOVE 100.00 TO COMM-AMT
           CALL 'XFRFUN' USING COMM-AREA-XFR

           IF COMM-SUCCESS = 'N' AND COMM-FAIL-CODE = '2'
              DISPLAY "PASS: TO-not-found rejected (fail '2')"
           ELSE
              DISPLAY "FAIL: TO-not-found (succ=" COMM-SUCCESS
                 " fc=" COMM-FAIL-CODE ")"
              ADD 1 TO WS-FAILURES
           END-IF
           MOVE "00000008" TO WS-AN
           CALL 'DB2ACC' USING OP-SELKEY WS-SC WS-AN WS-CN
                               FIX-ROW SQLCA
           CALL 'DB2PROC' USING OP-COUNT FIX-PROC SQLCA
           IF FIX-AVAILBAL = 1000.00 AND SQLERRD(1) = 0
              DISPLAY "PASS: no balance change, no PROCTRAN row"
           ELSE
              DISPLAY "FAIL: side effect on reject path"
              ADD 1 TO WS-FAILURES
           END-IF

      *    ---- Test 4: -911 deadlock on TO read, retry succeeds ----
           PERFORM CLEAR-TABLES
           MOVE "00000001" TO WS-INS-ACCNO
           MOVE 500.00     TO WS-INS-BAL
           PERFORM INSERT-ACCT
           MOVE "00000002" TO WS-INS-ACCNO
           MOVE 1000.00    TO WS-INS-BAL
           PERFORM INSERT-ACCT

      *    Script a one-shot deadlock: FACCNO(2) > TACCNO(1) so XFRFUN
      *    reads the TO account (acc 1) first, which is where the forced
      *    -911 / SQLERRD(3)=13172872 lands.
           MOVE -911 TO SQLCODE
           MOVE 13172872 TO SQLERRD(3)
           CALL 'DB2ACC' USING OP-SETSQL WS-SC WS-AN WS-CN
                               FIX-ROW SQLCA

           INITIALIZE COMM-AREA-XFR
           MOVE 2 TO COMM-FACCNO
           MOVE 1 TO COMM-TACCNO
           MOVE 200.00 TO COMM-AMT
           CALL 'XFRFUN' USING COMM-AREA-XFR

           IF COMM-SUCCESS = 'Y'
              DISPLAY "PASS: transfer succeeded after deadlock retry"
           ELSE
              DISPLAY "FAIL: retry path (fc=" COMM-FAIL-CODE ")"
              ADD 1 TO WS-FAILURES
           END-IF
           MOVE "00000002" TO WS-AN
           CALL 'DB2ACC' USING OP-SELKEY WS-SC WS-AN WS-CN
                               FIX-ROW SQLCA
           IF FIX-AVAILBAL = 800.00
              DISPLAY "PASS: FROM (acc2) debited to 800.00"
           ELSE
              DISPLAY "FAIL: FROM balance = " FIX-AVAILBAL
              ADD 1 TO WS-FAILURES
           END-IF
           MOVE "00000001" TO WS-AN
           CALL 'DB2ACC' USING OP-SELKEY WS-SC WS-AN WS-CN
                               FIX-ROW SQLCA
           IF FIX-AVAILBAL = 700.00
              DISPLAY "PASS: TO (acc1) credited to 700.00"
           ELSE
              DISPLAY "FAIL: TO balance = " FIX-AVAILBAL
              ADD 1 TO WS-FAILURES
           END-IF
           CALL 'DB2PROC' USING OP-COUNT FIX-PROC SQLCA
           IF SQLERRD(1) = 1
              DISPLAY "PASS: exactly one PROCTRAN row after retry"
           ELSE
              DISPLAY "FAIL: PROCTRAN count = " SQLERRD(1)
              ADD 1 TO WS-FAILURES
           END-IF

           IF WS-FAILURES = 0
              DISPLAY "RESULT: xfrfunTest PASSED"
              MOVE 0 TO RETURN-CODE
           ELSE
              DISPLAY "RESULT: xfrfunTest FAILED (" WS-FAILURES ")"
              MOVE 1 TO RETURN-CODE
           END-IF
           STOP RUN.

       CLEAR-TABLES.
           CALL 'DB2ACC' USING OP-CLEAR WS-SC WS-AN WS-CN
                               FIX-ROW SQLCA
           CALL 'DB2PROC' USING OP-CLEAR FIX-PROC SQLCA.

       INSERT-ACCT.
           MOVE "ACCT"       TO FIX-EYE
           MOVE "0000000001" TO FIX-CUSTNO
           MOVE "987654"     TO FIX-SORTCODE
           MOVE WS-INS-ACCNO TO FIX-ACCNO
           MOVE "CURRENT "   TO FIX-TYPE
           MOVE 1.50         TO FIX-RATE
           MOVE "2023-08-15" TO FIX-OPENED
           MOVE 500          TO FIX-OVERDRAFT
           MOVE "2024-01-01" TO FIX-LASTSTMT
           MOVE "2024-02-01" TO FIX-NEXTSTMT
           MOVE WS-INS-BAL   TO FIX-AVAILBAL
           MOVE WS-INS-BAL   TO FIX-ACTUALBAL
           CALL 'DB2ACC' USING OP-INSERT WS-SC WS-AN WS-CN
                               FIX-ROW SQLCA.
