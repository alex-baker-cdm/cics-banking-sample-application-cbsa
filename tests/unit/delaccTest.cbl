      ******************************************************************
      * delaccTest - isolated unit test for DELACC (Db2 ACCOUNT
      * SELECT + DELETE, then a PROCTRAN audit-row INSERT).
      *
      * Seeds one ACCOUNT row (sort code 987654) into the DB2ACC double,
      * then drives DELACC and inspects both doubles.
      *
      * Assertions:
      *   1. happy : delete succeeds; the ACCOUNT row is gone; a single
      *              PROCTRAN audit row (type 'ODA', amount = the
      *              deleted account's actual balance) was written.
      *   2. edge  : unknown account number -> DELACC-DEL-SUCCESS = 'N',
      *              fail code '1', ACCOUNT row untouched, no audit row.
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. DELACCTEST.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
      * COMMAREA layout - matches src/base/cobol_copy/DELACC.cpy.
       COPY DELACC.

      * Fixture / inspection row for the DB2ACC (ACCOUNT) double.
       01 FIX-ROW.
          COPY HOSTACCT.

      * Inspection row for the DB2PROC (PROCTRAN) double.
       01 FIX-PROC.
          COPY HOSTPROC.

      * SQL communications area shared with the DB2 doubles.
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
           DISPLAY "=== delaccTest : DELACC (DELETE + PROCTRAN) ==="

      *    ---- Test 1: delete an existing account ----
           PERFORM SEED-TABLES

           INITIALIZE DELACC-COMMAREA
           MOVE 1 TO DELACC-ACCNO
           CALL 'DELACC' USING DELACC-COMMAREA

           DISPLAY "    del-success flag = [" DELACC-DEL-SUCCESS "]"
           IF DELACC-DEL-SUCCESS = 'Y'
              DISPLAY "PASS: delete reported success"
           ELSE
              DISPLAY "FAIL: delete did not succeed"
              ADD 1 TO WS-FAILURES
           END-IF

      *    The ACCOUNT row should now be gone.
           MOVE "00000001" TO WS-AN
           CALL 'DB2ACC' USING OP-SELKEY WS-SC WS-AN WS-CN
                               FIX-ROW SQLCA
           IF SQLCODE = +100
              DISPLAY "PASS: account row removed from table"
           ELSE
              DISPLAY "FAIL: account row still present" SQLCODE
              ADD 1 TO WS-FAILURES
           END-IF

      *    Exactly one PROCTRAN audit row should have been written.
           CALL 'DB2PROC' USING OP-COUNT FIX-PROC SQLCA
           IF SQLERRD(1) = 1
              DISPLAY "PASS: one PROCTRAN audit row written"
           ELSE
              DISPLAY "FAIL: PROCTRAN count = " SQLERRD(1)
              ADD 1 TO WS-FAILURES
           END-IF

           CALL 'DB2PROC' USING OP-GETLAST FIX-PROC SQLCA
           IF FIXP-TYPE = "ODA" AND FIXP-ACCNO = "00000001"
              AND FIXP-AMOUNT = 1000.00
              DISPLAY "PASS: audit row is a delete (ODA) for 1000.00"
           ELSE
              DISPLAY "FAIL: audit row wrong (type=[" FIXP-TYPE
                 "] acc=[" FIXP-ACCNO "] amt=" FIXP-AMOUNT ")"
              ADD 1 TO WS-FAILURES
           END-IF

      *    ---- Test 2: account not found ----
           PERFORM SEED-TABLES
           INITIALIZE DELACC-COMMAREA
           MOVE 99 TO DELACC-ACCNO
           CALL 'DELACC' USING DELACC-COMMAREA

           DISPLAY "    del-success flag = [" DELACC-DEL-SUCCESS "]"
           IF DELACC-DEL-SUCCESS = 'N' AND DELACC-DEL-FAIL-CD = '1'
              DISPLAY "PASS: unknown account flagged (N, fail '1')"
           ELSE
              DISPLAY "FAIL: unknown account not flagged correctly"
              ADD 1 TO WS-FAILURES
           END-IF

      *    The seeded account row must be untouched, and no audit row.
           MOVE "00000001" TO WS-AN
           CALL 'DB2ACC' USING OP-SELKEY WS-SC WS-AN WS-CN
                               FIX-ROW SQLCA
           CALL 'DB2PROC' USING OP-COUNT FIX-PROC SQLCA
           IF SQLCODE = 0 AND SQLERRD(1) = 0
              DISPLAY "PASS: account kept, no audit row written"
           ELSE
              DISPLAY "FAIL: unexpected side effect on not-found path"
              ADD 1 TO WS-FAILURES
           END-IF

           IF WS-FAILURES = 0
              DISPLAY "RESULT: delaccTest PASSED"
              MOVE 0 TO RETURN-CODE
           ELSE
              DISPLAY "RESULT: delaccTest FAILED (" WS-FAILURES ")"
              MOVE 1 TO RETURN-CODE
           END-IF
           STOP RUN.

      *    Reset both doubles and insert a single known account row.
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
