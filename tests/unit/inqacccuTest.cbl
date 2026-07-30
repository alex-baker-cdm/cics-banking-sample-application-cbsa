      ******************************************************************
      * inqacccuTest - isolated unit test for INQACCCU
      * (LINK INQCUST + Db2 cursor loop into an OCCURS 1..20 array).
      *
      * INQACCCU first LINKs INQCUST to validate the customer (satisfied
      * by the CICSLINK stub, which returns a valid customer), then opens
      * a cursor filtered by customer number + sort code and loops FETCH
      * into the ACCOUNT-DETAILS ODO array, up to the 20-slot maximum.
      *
      * The caller must pre-size NUMBER-OF-ACCOUNTS to 20 so the COMMAREA
      * is allocated for the full array; INQACCCU resets it to the actual
      * count.
      *
      * Assertions:
      *   1. several : all rows returned, NUMBER-OF-ACCOUNTS correct.
      *   2. zero    : customer with no accounts -> success, count 0.
      *   3. sizing  : 20 accounts fill all 20 slots (ODO contract).
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. INQACCCUTEST.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
      * COMMAREA layout - matches src/base/cobol_copy/INQACCCU.cpy.
       01 WS-COMM.
          COPY INQACCCU.

      * Fixture row for the DB2ACC double.
       01 FIX-ROW.
          COPY HOSTACCT.

       01 WS-SC                           PIC X(6)  VALUE SPACES.
       01 WS-AN                           PIC X(8)  VALUE SPACES.
       01 WS-CN                           PIC X(10) VALUE SPACES.
       COPY SQLCA.

       01 OP-CLEAR                        PIC X(8) VALUE "CLEAR".
       01 OP-INSERT                       PIC X(8) VALUE "INSERT".

       01 WS-SEED-CUST                    PIC 9(10) VALUE 0.
       01 WS-SEED-COUNT                   PIC 9(4)  VALUE 0.
       01 WS-IDX                          PIC 9(8)  VALUE 0.

       01 WS-FAILURES                     PIC 9(4) VALUE 0.

       PROCEDURE DIVISION.
       MAIN-A.
           DISPLAY "=== inqacccuTest : INQACCCU (cursor -> ODO) ==="

      *    ---- Test 1: customer with several accounts ----
           MOVE 123 TO WS-SEED-CUST
           MOVE 3   TO WS-SEED-COUNT
           PERFORM SEED-CUSTOMER-ACCOUNTS

           INITIALIZE WS-COMM
           MOVE 20  TO NUMBER-OF-ACCOUNTS
           MOVE 123 TO CUSTOMER-NUMBER
           CALL 'INQACCCU' USING WS-COMM

           DISPLAY "    success = [" COMM-SUCCESS "]  count = "
                   NUMBER-OF-ACCOUNTS
           IF COMM-SUCCESS = 'Y' AND NUMBER-OF-ACCOUNTS = 3
              AND COMM-ACCNO(1) = 1 AND COMM-ACCNO(3) = 3
              DISPLAY "PASS: all 3 accounts returned in ODO array"
           ELSE
              DISPLAY "FAIL: several-accounts case wrong"
              ADD 1 TO WS-FAILURES
           END-IF

      *    ---- Test 2: customer with zero accounts ----
           MOVE 777 TO WS-SEED-CUST
           MOVE 0   TO WS-SEED-COUNT
           PERFORM SEED-CUSTOMER-ACCOUNTS

           INITIALIZE WS-COMM
           MOVE 20  TO NUMBER-OF-ACCOUNTS
           MOVE 777 TO CUSTOMER-NUMBER
           CALL 'INQACCCU' USING WS-COMM

           DISPLAY "    success = [" COMM-SUCCESS "]  count = "
                   NUMBER-OF-ACCOUNTS
           IF COMM-SUCCESS = 'Y' AND NUMBER-OF-ACCOUNTS = 0
              DISPLAY "PASS: zero-accounts -> success, count 0"
           ELSE
              DISPLAY "FAIL: zero-accounts case wrong"
              ADD 1 TO WS-FAILURES
           END-IF

      *    ---- Test 3: 20-slot sizing contract ----
           MOVE 456 TO WS-SEED-CUST
           MOVE 20  TO WS-SEED-COUNT
           PERFORM SEED-CUSTOMER-ACCOUNTS

           INITIALIZE WS-COMM
           MOVE 20  TO NUMBER-OF-ACCOUNTS
           MOVE 456 TO CUSTOMER-NUMBER
           CALL 'INQACCCU' USING WS-COMM

           DISPLAY "    success = [" COMM-SUCCESS "]  count = "
                   NUMBER-OF-ACCOUNTS
           IF COMM-SUCCESS = 'Y' AND NUMBER-OF-ACCOUNTS = 20
              AND COMM-ACCNO(1) = 1 AND COMM-ACCNO(20) = 20
              DISPLAY "PASS: all 20 slots filled (ODO 1..20 contract)"
           ELSE
              DISPLAY "FAIL: 20-slot sizing case wrong"
              ADD 1 TO WS-FAILURES
           END-IF

           IF WS-FAILURES = 0
              DISPLAY "RESULT: inqacccuTest PASSED"
              MOVE 0 TO RETURN-CODE
           ELSE
              DISPLAY "RESULT: inqacccuTest FAILED (" WS-FAILURES ")"
              MOVE 1 TO RETURN-CODE
           END-IF
           STOP RUN.

      *    Reset the double and insert WS-SEED-COUNT accounts, all owned
      *    by customer WS-SEED-CUST, with account numbers 1..count.
       SEED-CUSTOMER-ACCOUNTS.
           CALL 'DB2ACC' USING OP-CLEAR WS-SC WS-AN WS-CN
                               FIX-ROW SQLCA
           MOVE "ACCT"       TO FIX-EYE
           MOVE WS-SEED-CUST TO FIX-CUSTNO
           MOVE "987654"     TO FIX-SORTCODE
           MOVE "BANK    "   TO FIX-TYPE
           MOVE 1.00         TO FIX-RATE
           MOVE "2023-08-15" TO FIX-OPENED
           MOVE 0            TO FIX-OVERDRAFT
           MOVE "2024-01-01" TO FIX-LASTSTMT
           MOVE "2024-02-01" TO FIX-NEXTSTMT
           MOVE 100.00       TO FIX-AVAILBAL
           MOVE 100.00       TO FIX-ACTUALBAL
           PERFORM VARYING WS-IDX FROM 1 BY 1
              UNTIL WS-IDX > WS-SEED-COUNT
              MOVE WS-IDX TO FIX-ACCNO
              CALL 'DB2ACC' USING OP-INSERT WS-SC WS-AN WS-CN
                                  FIX-ROW SQLCA
           END-PERFORM.
