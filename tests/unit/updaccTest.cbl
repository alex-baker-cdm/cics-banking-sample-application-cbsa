      ******************************************************************
      * updaccTest - isolated unit test for UPDACC (Db2 SELECT + UPDATE).
      *
      * Seeds one ACCOUNT row (sort code 987654) into the DB2ACC double,
      * then drives UPDACC through the SQL harness.
      *
      * Assertions:
      *   1. happy : update succeeds; COMMAREA reflects the new fields;
      *              the change is persisted in the table (re-SELECT).
      *   2. edge  : unknown account number -> COMM-SUCCESS = 'N'.
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. UPDACCTEST.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
      * COMMAREA layout - matches src/base/cobol_copy/UPDACC.cpy.
       01 WS-COMM.
          COPY UPDACC.

      * Fixture / inspection row for the DB2ACC double.
       01 FIX-ROW.
          COPY HOSTACCT.

       01 WS-SC                           PIC X(6)  VALUE "987654".
       01 WS-AN                           PIC X(8)  VALUE SPACES.
       01 WS-CN                           PIC X(10) VALUE SPACES.
       01 WS-SQL                          PIC S9(9) COMP-5 VALUE 0.

       01 OP-CLEAR                        PIC X(8) VALUE "CLEAR".
       01 OP-INSERT                       PIC X(8) VALUE "INSERT".
       01 OP-SELKEY                       PIC X(8) VALUE "SELKEY".
       01 OP-SETSQL                       PIC X(8) VALUE "SETSQL".

       01 WS-FAILURES                     PIC 9(4) VALUE 0.

       PROCEDURE DIVISION.
       MAIN-A.
           DISPLAY "=== updaccTest : UPDACC (SELECT + UPDATE) ==="

      *    ---- Test 1: update an existing account ----
           PERFORM SEED-TABLE

           INITIALIZE WS-COMM
           MOVE 1          TO COMM-ACCNO
           MOVE "LOAN    "  TO COMM-ACC-TYPE
           MOVE 2.75       TO COMM-INT-RATE
           MOVE 750        TO COMM-OVERDRAFT
           CALL 'UPDACC' USING WS-COMM

           DISPLAY "    success flag = [" COMM-SUCCESS "]"
           IF COMM-SUCCESS = 'Y'
              DISPLAY "PASS: update reported success"
           ELSE
              DISPLAY "FAIL: update did not succeed"
              ADD 1 TO WS-FAILURES
           END-IF

           IF COMM-ACC-TYPE = "LOAN    " AND COMM-OVERDRAFT = 750
              DISPLAY "PASS: COMMAREA shows updated fields"
           ELSE
              DISPLAY "FAIL: COMMAREA fields not updated"
              ADD 1 TO WS-FAILURES
           END-IF

      *    Re-SELECT the row to prove the UPDATE was persisted.
           MOVE "00000001" TO WS-AN
           CALL 'DB2ACC' USING OP-SELKEY WS-SC WS-AN WS-CN
                               FIX-ROW WS-SQL
           IF WS-SQL = 0 AND FIX-TYPE = "LOAN    "
              AND FIX-OVERDRAFT = 750 AND FIX-RATE = 2.75
              DISPLAY "PASS: row rewritten in table"
           ELSE
              DISPLAY "FAIL: row not persisted (type=[" FIX-TYPE "])"
              ADD 1 TO WS-FAILURES
           END-IF

      *    ---- Test 2: account not found ----
           PERFORM SEED-TABLE
           INITIALIZE WS-COMM
           MOVE 99         TO COMM-ACCNO
           MOVE "SAVING  "  TO COMM-ACC-TYPE
           MOVE 1.00       TO COMM-INT-RATE
           MOVE 10         TO COMM-OVERDRAFT
           CALL 'UPDACC' USING WS-COMM

           DISPLAY "    success flag = [" COMM-SUCCESS "]"
           IF COMM-SUCCESS = 'N'
              DISPLAY "PASS: unknown account flagged (success = N)"
           ELSE
              DISPLAY "FAIL: unknown account not flagged"
              ADD 1 TO WS-FAILURES
           END-IF

      *    ---- Test 3: scripted Db2 error on SELECT (SETSQL) ----
           PERFORM SEED-TABLE
           MOVE -911 TO WS-SQL
           CALL 'DB2ACC' USING OP-SETSQL WS-SC WS-AN WS-CN
                               FIX-ROW WS-SQL
           INITIALIZE WS-COMM
           MOVE 1          TO COMM-ACCNO
           MOVE "LOAN    "  TO COMM-ACC-TYPE
           MOVE 2.75       TO COMM-INT-RATE
           MOVE 750        TO COMM-OVERDRAFT
           CALL 'UPDACC' USING WS-COMM

           DISPLAY "    success flag = [" COMM-SUCCESS "]"
           IF COMM-SUCCESS = 'N'
              DISPLAY "PASS: scripted SQLCODE error -> success = N"
           ELSE
              DISPLAY "FAIL: scripted error not surfaced"
              ADD 1 TO WS-FAILURES
           END-IF

           IF WS-FAILURES = 0
              DISPLAY "RESULT: updaccTest PASSED"
              MOVE 0 TO RETURN-CODE
           ELSE
              DISPLAY "RESULT: updaccTest FAILED (" WS-FAILURES ")"
              MOVE 1 TO RETURN-CODE
           END-IF
           STOP RUN.

      *    Reset the double and insert a single known account row.
       SEED-TABLE.
           CALL 'DB2ACC' USING OP-CLEAR WS-SC WS-AN WS-CN
                               FIX-ROW WS-SQL
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
                               FIX-ROW WS-SQL.
