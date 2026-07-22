      ******************************************************************
      * inqaccTest - isolated unit test for INQACC (Db2 cursor SELECT).
      *
      * INQACC opens a keyed cursor over the ACCOUNT table, fetches the
      * single matching row and copies it to the COMMAREA. A missing row
      * (FETCH -> SQLCODE +100) leaves ACCOUNT-TYPE blank, which INQACC
      * reports as INQACC-SUCCESS = 'N'.
      *
      * Assertions:
      *   1. happy : existing account returned with correct fields.
      *   2. edge  : unknown account -> INQACC-SUCCESS = 'N'.
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. INQACCTEST.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
      * COMMAREA layout - matches src/base/cobol_copy/INQACC.cpy
      * (that copybook defines its own 01 INQACC-COMMAREA).
       COPY INQACC.

      * Fixture row for the DB2ACC double.
       01 FIX-ROW.
          COPY HOSTACCT.

       01 WS-SC                           PIC X(6)  VALUE SPACES.
       01 WS-AN                           PIC X(8)  VALUE SPACES.
       01 WS-CN                           PIC X(10) VALUE SPACES.
       01 WS-SQL                          PIC S9(9) COMP-5 VALUE 0.

       01 OP-CLEAR                        PIC X(8) VALUE "CLEAR".
       01 OP-INSERT                       PIC X(8) VALUE "INSERT".

       01 WS-FAILURES                     PIC 9(4) VALUE 0.

       PROCEDURE DIVISION.
       MAIN-A.
           DISPLAY "=== inqaccTest : INQACC (cursor SELECT) ==="
           PERFORM SEED-TABLE

      *    ---- Test 1: existing account ----
           INITIALIZE INQACC-COMMAREA
           MOVE 1 TO INQACC-ACCNO
           CALL 'INQACC' USING INQACC-COMMAREA

           DISPLAY "    success  = [" INQACC-SUCCESS "]"
           DISPLAY "    acc-type = [" INQACC-ACC-TYPE "]"
           DISPLAY "    opened   = [" INQACC-OPENED "]"
           IF INQACC-SUCCESS = 'Y'
              AND INQACC-ACC-TYPE = "ISA     "
              AND INQACC-ACCNO = 1
              AND INQACC-SCODE = 987654
              AND INQACC-OPENED = 15082023
              AND INQACC-AVAIL-BAL = 1234.56
              DISPLAY "PASS: existing account returned correctly"
           ELSE
              DISPLAY "FAIL: account fields incorrect"
              ADD 1 TO WS-FAILURES
           END-IF

      *    ---- Test 2: unknown account ----
           INITIALIZE INQACC-COMMAREA
           MOVE 12345678 TO INQACC-ACCNO
           CALL 'INQACC' USING INQACC-COMMAREA

           DISPLAY "    success  = [" INQACC-SUCCESS "]"
           IF INQACC-SUCCESS = 'N'
              DISPLAY "PASS: unknown account -> success = N"
           ELSE
              DISPLAY "FAIL: unknown account not flagged"
              ADD 1 TO WS-FAILURES
           END-IF

           IF WS-FAILURES = 0
              DISPLAY "RESULT: inqaccTest PASSED"
              MOVE 0 TO RETURN-CODE
           ELSE
              DISPLAY "RESULT: inqaccTest FAILED (" WS-FAILURES ")"
              MOVE 1 TO RETURN-CODE
           END-IF
           STOP RUN.

       SEED-TABLE.
           CALL 'DB2ACC' USING OP-CLEAR WS-SC WS-AN WS-CN
                               FIX-ROW WS-SQL
           MOVE "ACCT"       TO FIX-EYE
           MOVE "0000000001" TO FIX-CUSTNO
           MOVE "987654"     TO FIX-SORTCODE
           MOVE "00000001"   TO FIX-ACCNO
           MOVE "ISA     "   TO FIX-TYPE
           MOVE 1.75         TO FIX-RATE
           MOVE "2023-08-15" TO FIX-OPENED
           MOVE 500          TO FIX-OVERDRAFT
           MOVE "2024-01-01" TO FIX-LASTSTMT
           MOVE "2024-02-01" TO FIX-NEXTSTMT
           MOVE 1234.56      TO FIX-AVAILBAL
           MOVE 1234.56      TO FIX-ACTUALBAL
           CALL 'DB2ACC' USING OP-INSERT WS-SC WS-AN WS-CN
                               FIX-ROW WS-SQL.
