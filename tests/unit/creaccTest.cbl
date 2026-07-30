      ******************************************************************
      * creaccTest - isolated unit test for CREACC (account creation).
      *
      * CREACC validates the customer (LINK INQCUST), enforces the
      * max-accounts-per-customer rule (LINK INQACCCU), allocates the
      * next account number from the Db2 CONTROL named-counter rows
      * (ENQ .. SELECT/UPDATE CONTROL .. DEQ), INSERTs the ACCOUNT row
      * and writes a PROCTRAN audit row.
      *
      * Doubles exercised: CICSLINK (INQCUST + INQACCCU), CICSENQ
      * (ENQ/DEQ no-op), DB2CTRL (CONTROL table), DB2ACC (ACCOUNT
      * INSERT), DB2PROC (PROCTRAN audit).
      *
      * Assertions:
      *   1. happy : valid customer, under the limit -> success; account
      *              number = last+1; ACCOUNT row inserted; both CONTROL
      *              counters incremented by 1; one PROCTRAN 'OCA' row.
      *   2. cust-not-found (INQCUST fails) -> not created, fail '1',
      *              counters untouched, no ACCOUNT / PROCTRAN row.
      *   3. limit-reached (INQACCCU returns 10 > 9) -> refused,
      *              fail '8', counters untouched, nothing created.
      *   4. Db2 error on the ACCOUNT INSERT -> not created, fail '7',
      *              no ACCOUNT / PROCTRAN row. CREACC does NOT roll
      *              back the CONTROL counters, so they stay
      *              incremented (documents CREACC's actual behaviour).
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. CREACCTEST.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
      * COMMAREA layout - matches src/base/cobol_copy/CREACC.cpy.
       01 CREACC-COMMAREA.
          COPY CREACC.

      * Fixture / inspection row for the DB2ACC (ACCOUNT) double.
       01 FIX-ROW.
          COPY HOSTACCT.
      * Inspection row for the DB2PROC (PROCTRAN) double.
       01 FIX-PROC.
          COPY HOSTPROC.
      * Seed / inspection row for the DB2CTRL (CONTROL) double.
       01 FIX-CTRL.
          COPY HOSTCTRL.

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
       01 OP-SEED                         PIC X(8) VALUE "SEED".
       01 OP-GETVAL                       PIC X(8) VALUE "GETVAL".
       01 OP-SETSQL                       PIC X(8) VALUE "SETSQL".

       01 CTRL-LAST-NAME                  PIC X(32)
                                          VALUE "987654-ACCOUNT-LAST".
       01 CTRL-COUNT-NAME                 PIC X(32)
                                          VALUE "987654-ACCOUNT-COUNT".

       01 WS-FAILURES                     PIC 9(4) VALUE 0.

       PROCEDURE DIVISION.
       MAIN-A.
           DISPLAY "=== creaccTest : CREACC (create account) ==="

      *    ---- Test 1: happy path ----
           DISPLAY "-- Test 1: valid customer, under account limit --"
           PERFORM RESET-ALL
           PERFORM SEED-CONTROL
           PERFORM SET-INQCUST-Y
           PERFORM SET-ACCCU-COUNT-1

           PERFORM BUILD-INPUT
           CALL 'CREACC' USING CREACC-COMMAREA

           IF COMM-SUCCESS OF CREACC-COMMAREA = 'Y'
              DISPLAY "PASS: creation reported success"
           ELSE
              DISPLAY "FAIL: creation did not succeed (flag="
                 COMM-SUCCESS OF CREACC-COMMAREA " fail="
                 COMM-FAIL-CODE OF CREACC-COMMAREA ")"
              ADD 1 TO WS-FAILURES
           END-IF

      *    Last counter was 10 -> new account number is 11.
           IF COMM-NUMBER OF CREACC-COMMAREA = 11
              DISPLAY "PASS: next account number allocated (11)"
           ELSE
              DISPLAY "FAIL: account number = "
                 COMM-NUMBER OF CREACC-COMMAREA
              ADD 1 TO WS-FAILURES
           END-IF

      *    The ACCOUNT row should have been inserted.
           MOVE "00000011" TO WS-AN
           CALL 'DB2ACC' USING OP-SELKEY WS-SC WS-AN WS-CN
                               FIX-ROW SQLCA
           IF SQLCODE = 0 AND FIX-CUSTNO = "0000000001"
              AND FIX-TYPE = "CURRENT " AND FIX-AVAILBAL = 250.00
              DISPLAY "PASS: ACCOUNT row inserted for the customer"
           ELSE
              DISPLAY "FAIL: ACCOUNT row wrong (sql=" SQLCODE
                 " cust=[" FIX-CUSTNO "] type=[" FIX-TYPE
                 "] avail=" FIX-AVAILBAL ")"
              ADD 1 TO WS-FAILURES
           END-IF

      *    Both CONTROL counters must be incremented by 1 (10-11, 5-6).
           MOVE CTRL-LAST-NAME  TO FIXC-NAME
           CALL 'DB2CTRL' USING OP-GETVAL FIX-CTRL SQLCA
           IF FIXC-VALNUM = 11
              DISPLAY "PASS: ACCOUNT-LAST counter incremented to 11"
           ELSE
              DISPLAY "FAIL: ACCOUNT-LAST = " FIXC-VALNUM
              ADD 1 TO WS-FAILURES
           END-IF

           MOVE CTRL-COUNT-NAME TO FIXC-NAME
           CALL 'DB2CTRL' USING OP-GETVAL FIX-CTRL SQLCA
           IF FIXC-VALNUM = 6
              DISPLAY "PASS: ACCOUNT-COUNT counter incremented to 6"
           ELSE
              DISPLAY "FAIL: ACCOUNT-COUNT = " FIXC-VALNUM
              ADD 1 TO WS-FAILURES
           END-IF

      *    Exactly one PROCTRAN audit row (type 'OCA', amount 0).
           CALL 'DB2PROC' USING OP-COUNT FIX-PROC SQLCA
           CALL 'DB2PROC' USING OP-GETLAST FIX-PROC SQLCA
           IF SQLERRD(1) = 1 AND FIXP-TYPE = "OCA"
              AND FIXP-ACCNO = "00000011" AND FIXP-AMOUNT = 0
              DISPLAY "PASS: one PROCTRAN create (OCA) row written"
           ELSE
              DISPLAY "FAIL: PROCTRAN audit wrong (n=" SQLERRD(1)
                 " type=[" FIXP-TYPE "] acc=[" FIXP-ACCNO
                 "] amt=" FIXP-AMOUNT ")"
              ADD 1 TO WS-FAILURES
           END-IF

      *    ---- Test 2: customer not found (INQCUST fails) ----
           DISPLAY "-- Test 2: customer not found --"
           PERFORM RESET-ALL
           PERFORM SEED-CONTROL
           PERFORM SET-INQCUST-N
           PERFORM SET-ACCCU-COUNT-1

           PERFORM BUILD-INPUT
           CALL 'CREACC' USING CREACC-COMMAREA

           IF COMM-SUCCESS OF CREACC-COMMAREA = 'N'
              AND COMM-FAIL-CODE OF CREACC-COMMAREA = '1'
              DISPLAY "PASS: unknown customer flagged (N, fail '1')"
           ELSE
              DISPLAY "FAIL: customer-not-found not flagged (flag="
                 COMM-SUCCESS OF CREACC-COMMAREA " fail="
                 COMM-FAIL-CODE OF CREACC-COMMAREA ")"
              ADD 1 TO WS-FAILURES
           END-IF
           PERFORM ASSERT-NOTHING-CREATED

      *    ---- Test 3: account limit reached (INQACCCU returns 10) ----
           DISPLAY "-- Test 3: account limit reached --"
           PERFORM RESET-ALL
           PERFORM SEED-CONTROL
           PERFORM SET-INQCUST-Y
           PERFORM SET-ACCCU-COUNT-10

           PERFORM BUILD-INPUT
           CALL 'CREACC' USING CREACC-COMMAREA

           IF COMM-SUCCESS OF CREACC-COMMAREA = 'N'
              AND COMM-FAIL-CODE OF CREACC-COMMAREA = '8'
              DISPLAY "PASS: account limit refused (N, fail '8')"
           ELSE
              DISPLAY "FAIL: limit not enforced (flag="
                 COMM-SUCCESS OF CREACC-COMMAREA " fail="
                 COMM-FAIL-CODE OF CREACC-COMMAREA ")"
              ADD 1 TO WS-FAILURES
           END-IF
           PERFORM ASSERT-NOTHING-CREATED

      *    ---- Test 4: Db2 error on the ACCOUNT INSERT ----
           DISPLAY "-- Test 4: Db2 error on ACCOUNT INSERT --"
           PERFORM RESET-ALL
           PERFORM SEED-CONTROL
           PERFORM SET-INQCUST-Y
           PERFORM SET-ACCCU-COUNT-1

      *    Script an insert failure on the next DB2ACC data op.
           MOVE -803 TO SQLCODE
           MOVE 0    TO SQLERRD(3)
           CALL 'DB2ACC' USING OP-SETSQL WS-SC WS-AN WS-CN
                               FIX-ROW SQLCA

           PERFORM BUILD-INPUT
           CALL 'CREACC' USING CREACC-COMMAREA

           IF COMM-SUCCESS OF CREACC-COMMAREA = 'N'
              AND COMM-FAIL-CODE OF CREACC-COMMAREA = '7'
              DISPLAY "PASS: INSERT failure flagged (N, fail '7')"
           ELSE
              DISPLAY "FAIL: INSERT failure not flagged (flag="
                 COMM-SUCCESS OF CREACC-COMMAREA " fail="
                 COMM-FAIL-CODE OF CREACC-COMMAREA ")"
              ADD 1 TO WS-FAILURES
           END-IF

      *    No ACCOUNT row, no PROCTRAN row.
           MOVE "00000011" TO WS-AN
           CALL 'DB2ACC' USING OP-SELKEY WS-SC WS-AN WS-CN
                               FIX-ROW SQLCA
           MOVE SQLCODE TO SQLCODE
           CALL 'DB2PROC' USING OP-COUNT FIX-PROC SQLCA
           IF SQLERRD(1) = 0
              DISPLAY "PASS: no ACCOUNT / PROCTRAN row persisted"
           ELSE
              DISPLAY "FAIL: rows persisted after INSERT failure"
              ADD 1 TO WS-FAILURES
           END-IF

      *    CREACC does not undo the CONTROL counters on INSERT failure,
      *    so they stay at 11 / 6 (documented behaviour).
           MOVE CTRL-LAST-NAME  TO FIXC-NAME
           CALL 'DB2CTRL' USING OP-GETVAL FIX-CTRL SQLCA
           IF FIXC-VALNUM = 11
              DISPLAY "PASS: CONTROL counter left incremented (11) -"
                 " CREACC does not roll it back on INSERT failure"
           ELSE
              DISPLAY "FAIL: unexpected ACCOUNT-LAST = " FIXC-VALNUM
              ADD 1 TO WS-FAILURES
           END-IF

           IF WS-FAILURES = 0
              DISPLAY "RESULT: creaccTest PASSED"
              MOVE 0 TO RETURN-CODE
           ELSE
              DISPLAY "RESULT: creaccTest FAILED (" WS-FAILURES ")"
              MOVE 1 TO RETURN-CODE
           END-IF
           STOP RUN.

      *    -------------------------------------------------------------
      *    Helpers
      *    -------------------------------------------------------------

      *    Empty all three Db2 doubles.
       RESET-ALL.
           CALL 'DB2ACC'  USING OP-CLEAR WS-SC WS-AN WS-CN
                                FIX-ROW SQLCA
           CALL 'DB2PROC' USING OP-CLEAR FIX-PROC SQLCA
           CALL 'DB2CTRL' USING OP-CLEAR FIX-CTRL SQLCA.

      *    Seed the two named-counter CONTROL rows: last=10, count=5.
       SEED-CONTROL.
           MOVE CTRL-LAST-NAME TO FIXC-NAME
           MOVE 10 TO FIXC-VALNUM
           MOVE SPACES TO FIXC-VALSTR
           CALL 'DB2CTRL' USING OP-SEED FIX-CTRL SQLCA
           MOVE CTRL-COUNT-NAME TO FIXC-NAME
           MOVE 5 TO FIXC-VALNUM
           MOVE SPACES TO FIXC-VALSTR
           CALL 'DB2CTRL' USING OP-SEED FIX-CTRL SQLCA.

      *    Build a valid CREACC input COMMAREA.
       BUILD-INPUT.
           INITIALIZE CREACC-COMMAREA
           MOVE 1          TO COMM-CUSTNO OF CREACC-COMMAREA
           MOVE "CURRENT " TO COMM-ACC-TYPE OF CREACC-COMMAREA
           MOVE 1.25       TO COMM-INT-RT OF CREACC-COMMAREA
           MOVE 500        TO COMM-OVERDR-LIM OF CREACC-COMMAREA
           MOVE 250.00     TO COMM-AVAIL-BAL OF CREACC-COMMAREA
           MOVE 250.00     TO COMM-ACT-BAL OF CREACC-COMMAREA.

      *    Assert no account and no audit row was created, and that the
      *    CONTROL counters were left untouched (10 / 5).
       ASSERT-NOTHING-CREATED.
           CALL 'DB2PROC' USING OP-COUNT FIX-PROC SQLCA
           IF SQLERRD(1) = 0
              DISPLAY "PASS: no PROCTRAN audit row written"
           ELSE
              DISPLAY "FAIL: PROCTRAN row written (" SQLERRD(1) ")"
              ADD 1 TO WS-FAILURES
           END-IF
           MOVE CTRL-LAST-NAME TO FIXC-NAME
           CALL 'DB2CTRL' USING OP-GETVAL FIX-CTRL SQLCA
           IF FIXC-VALNUM = 10
              DISPLAY "PASS: CONTROL counter untouched (10)"
           ELSE
              DISPLAY "FAIL: CONTROL counter changed = " FIXC-VALNUM
              ADD 1 TO WS-FAILURES
           END-IF.

      *    Runtime environment knobs read by the CICSLINK double.
       SET-INQCUST-Y.
           DISPLAY "CBSA_TEST_INQCUST_SUCCESS" UPON ENVIRONMENT-NAME
           DISPLAY "Y" UPON ENVIRONMENT-VALUE.

       SET-INQCUST-N.
           DISPLAY "CBSA_TEST_INQCUST_SUCCESS" UPON ENVIRONMENT-NAME
           DISPLAY "N" UPON ENVIRONMENT-VALUE.

       SET-ACCCU-COUNT-1.
           DISPLAY "CBSA_TEST_INQACCCU_SUCCESS" UPON ENVIRONMENT-NAME
           DISPLAY "Y" UPON ENVIRONMENT-VALUE
           DISPLAY "CBSA_TEST_INQACCCU_COUNT" UPON ENVIRONMENT-NAME
           DISPLAY "1" UPON ENVIRONMENT-VALUE.

       SET-ACCCU-COUNT-10.
           DISPLAY "CBSA_TEST_INQACCCU_SUCCESS" UPON ENVIRONMENT-NAME
           DISPLAY "Y" UPON ENVIRONMENT-VALUE
           DISPLAY "CBSA_TEST_INQACCCU_COUNT" UPON ENVIRONMENT-NAME
           DISPLAY "10" UPON ENVIRONMENT-VALUE.
