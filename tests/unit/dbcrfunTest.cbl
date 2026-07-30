      ******************************************************************
      * dbcrfunTest - isolated unit test for DBCRFUN (cash pay-in /
      * withdrawal: SELECT ACCOUNT, apply amount, UPDATE, INSERT
      * PROCTRAN).
      *
      * Time and EIBTASKN are fixed by the CICS doubles (CICSTIME /
      * CICSFTIM / CBSA_TEST_TASKN) so the run is deterministic.
      *
      * Assertions:
      *   1. credit (Teller +250) : success, balances +250, PROCTRAN
      *                             'CRE' row for +250.00.
      *   2. debit  (Teller -400) : success, balances -400, PROCTRAN
      *                             'DEB' row for -400.00.
      *   3. payment debit, insufficient funds : rejected (fail '3'),
      *                             balance untouched, no PROCTRAN row.
      *   4. unknown account       : rejected (fail '1').
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. DBCRFUNTEST.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 COMM-AREA-PAYDBCR.
          COPY PAYDBCR.

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
       01 OP-COUNT                        PIC X(8) VALUE "COUNT".
       01 OP-GETLAST                      PIC X(8) VALUE "GETLAST".

       01 WS-FAILURES                     PIC 9(4) VALUE 0.

       PROCEDURE DIVISION.
       MAIN-A.
           DISPLAY "=== dbcrfunTest : DBCRFUN (pay-in / withdrawal) ==="

      *    ---- Test 1: Teller credit ----
           PERFORM SEED-TABLES
           INITIALIZE COMM-AREA-PAYDBCR
           MOVE "00000001" TO COMM-ACCNO
           MOVE 250.00 TO COMM-AMT
           CALL 'DBCRFUN' USING COMM-AREA-PAYDBCR
           IF COMM-SUCCESS = 'Y' AND COMM-AV-BAL = 1250.00
              AND COMM-ACT-BAL = 1250.00
              DISPLAY "PASS: credit applied, balance now 1250.00"
           ELSE
              DISPLAY "FAIL: credit (succ=" COMM-SUCCESS
                 " av=" COMM-AV-BAL ")"
              ADD 1 TO WS-FAILURES
           END-IF
           PERFORM CHECK-LAST-PROC
           IF FIXP-TYPE = "CRE" AND FIXP-AMOUNT = 250.00
              DISPLAY "PASS: PROCTRAN CRE row for 250.00"
           ELSE
              DISPLAY "FAIL: PROCTRAN row (type=" FIXP-TYPE
                 " amt=" FIXP-AMOUNT ")"
              ADD 1 TO WS-FAILURES
           END-IF

      *    ---- Test 2: Teller debit ----
           PERFORM SEED-TABLES
           INITIALIZE COMM-AREA-PAYDBCR
           MOVE "00000001" TO COMM-ACCNO
           MOVE -400.00 TO COMM-AMT
           CALL 'DBCRFUN' USING COMM-AREA-PAYDBCR
           IF COMM-SUCCESS = 'Y' AND COMM-AV-BAL = 600.00
              DISPLAY "PASS: debit applied, balance now 600.00"
           ELSE
              DISPLAY "FAIL: debit (succ=" COMM-SUCCESS
                 " av=" COMM-AV-BAL ")"
              ADD 1 TO WS-FAILURES
           END-IF
           PERFORM CHECK-LAST-PROC
           IF FIXP-TYPE = "DEB" AND FIXP-AMOUNT = -400.00
              DISPLAY "PASS: PROCTRAN DEB row for -400.00"
           ELSE
              DISPLAY "FAIL: PROCTRAN row (type=" FIXP-TYPE
                 " amt=" FIXP-AMOUNT ")"
              ADD 1 TO WS-FAILURES
           END-IF

      *    ---- Test 3: payment debit, insufficient funds ----
           PERFORM SEED-TABLES
           INITIALIZE COMM-AREA-PAYDBCR
           MOVE "00000001" TO COMM-ACCNO
           MOVE -5000.00 TO COMM-AMT
           MOVE 496 TO COMM-FACILTYPE
           CALL 'DBCRFUN' USING COMM-AREA-PAYDBCR
           IF COMM-SUCCESS = 'N' AND COMM-FAIL-CODE = '3'
              DISPLAY "PASS: insufficient funds rejected (fail '3')"
           ELSE
              DISPLAY "FAIL: insufficient not rejected (succ="
                 COMM-SUCCESS " fc=" COMM-FAIL-CODE ")"
              ADD 1 TO WS-FAILURES
           END-IF
           MOVE "00000001" TO WS-AN
           CALL 'DB2ACC' USING OP-SELKEY WS-SC WS-AN WS-CN
                               FIX-ROW SQLCA
           CALL 'DB2PROC' USING OP-COUNT FIX-PROC SQLCA
           IF FIX-AVAILBAL = 1000.00 AND SQLERRD(1) = 0
              DISPLAY "PASS: balance untouched, no PROCTRAN row"
           ELSE
              DISPLAY "FAIL: side effect on reject path"
              ADD 1 TO WS-FAILURES
           END-IF

      *    ---- Test 4: unknown account ----
           PERFORM SEED-TABLES
           INITIALIZE COMM-AREA-PAYDBCR
           MOVE "00000099" TO COMM-ACCNO
           MOVE 100.00 TO COMM-AMT
           CALL 'DBCRFUN' USING COMM-AREA-PAYDBCR
           IF COMM-SUCCESS = 'N' AND COMM-FAIL-CODE = '1'
              DISPLAY "PASS: unknown account rejected (fail '1')"
           ELSE
              DISPLAY "FAIL: unknown account (succ=" COMM-SUCCESS
                 " fc=" COMM-FAIL-CODE ")"
              ADD 1 TO WS-FAILURES
           END-IF

           IF WS-FAILURES = 0
              DISPLAY "RESULT: dbcrfunTest PASSED"
              MOVE 0 TO RETURN-CODE
           ELSE
              DISPLAY "RESULT: dbcrfunTest FAILED (" WS-FAILURES ")"
              MOVE 1 TO RETURN-CODE
           END-IF
           STOP RUN.

       CHECK-LAST-PROC.
           CALL 'DB2PROC' USING OP-GETLAST FIX-PROC SQLCA.

       SEED-TABLES.
           CALL 'DB2ACC' USING OP-CLEAR WS-SC WS-AN WS-CN
                               FIX-ROW SQLCA
           CALL 'DB2PROC' USING OP-CLEAR FIX-PROC SQLCA
           MOVE "ACCT"       TO FIX-EYE
           MOVE "0000000001" TO FIX-CUSTNO
           MOVE "987654"     TO FIX-SORTCODE
           MOVE "00000001"   TO FIX-ACCNO
           MOVE "CURRENT "   TO FIX-TYPE
           MOVE 1.50         TO FIX-RATE
           MOVE "2023-08-15" TO FIX-OPENED
           MOVE 500          TO FIX-OVERDRAFT
           MOVE "2024-01-01" TO FIX-LASTSTMT
           MOVE "2024-02-01" TO FIX-NEXTSTMT
           MOVE 1000.00      TO FIX-AVAILBAL
           MOVE 1000.00      TO FIX-ACTUALBAL
           CALL 'DB2ACC' USING OP-INSERT WS-SC WS-AN WS-CN
                               FIX-ROW SQLCA.
