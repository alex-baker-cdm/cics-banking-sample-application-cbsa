      ******************************************************************
      * delcusTest - isolated unit test for DELCUS (delete a customer and
      * cascade-delete the customer's accounts).
      *
      * DELCUS: LINK INQCUST (validate) -> LINK INQACCCU (enumerate the
      * customer's accounts) -> LINK DELACC once per account -> DELETE the
      * CUSTOMER VSAM record -> INSERT an 'ODC' PROCTRAN audit row.
      *
      * All three LINKed programs are the CICSLINK double: the driver
      * scripts the INQACCCU account list (LNKADDAC), forces the INQCUST
      * result (LNKINQOK), and reads back exactly which accounts DELACC was
      * asked to delete, in order (LNKGETCN / LNKGETAC). The CUSTOMER record
      * lives in the CICSVSAM double.
      *
      * Assertions:
      *   1. happy : customer with 3 accounts -> success; DELACC LINKed 3
      *              times with the scripted account keys, in order; the
      *              CUSTOMER record is gone; one 'ODC' PROCTRAN row.
      *   2. edge  : customer-not-found (INQCUST 'N') -> success = 'N',
      *              fail code '1'; no DELACC LINK; the CUSTOMER record is
      *              untouched; no PROCTRAN row.
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. DELCUSTEST.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
      * DELCUS COMMAREA - matches src/base/cobol_copy/DELCUS.cpy.
       01 WS-COMM.
          COPY DELCUS.

      * CUSTOMER record image (for seeding / reading back).
       01 CUST-IMG.
          COPY CUSTOMER.

      * PROCTRAN audit-row inspection view.
       01 FIX-PROC.
          COPY HOSTPROC.
       COPY SQLCA.

      * CICSLINK driver-control block (matches CICSLINK CTRL-VIEW).
       01 LNK-CTRL.
          03 LC-INQOK                    PIC X     VALUE 'Y'.
          03 LC-NACC                     PIC 9(4)  VALUE 0.
          03 LC-IDX                      PIC 9(4)  VALUE 0.
          03 LC-ACCNO                    PIC 9(8)  VALUE 0.

      * Dummy COMMAREA length for the 3-arg CICSLINK ABI (control ops
      * carry the CTRL block as the 3rd arg; the length is unused there).
       01 WS-LNK-LEN                     PIC 9(9) COMP-5 VALUE 0.
       01 LNK-RESET                      PIC X(8)  VALUE 'LNKRESET'.
       01 LNK-ADDAC                      PIC X(8)  VALUE 'LNKADDAC'.
       01 LNK-INQOK                      PIC X(8)  VALUE 'LNKINQOK'.
       01 LNK-GETCN                      PIC X(8)  VALUE 'LNKGETCN'.
       01 LNK-GETAC                      PIC X(8)  VALUE 'LNKGETAC'.

      * CICSVSAM control / inspection call operands.
       01 V-OP                            PIC X(8)  VALUE SPACES.
       01 V-FILE                          PIC X(8)  VALUE 'CUSTOMER'.
       01 V-KEY                           PIC X(16) VALUE SPACES.
       01 V-RESP                          PIC S9(8) COMP VALUE 0.
       01 V-RESP2                         PIC S9(8) COMP VALUE 0.

      * DB2PROC control operands.
       01 P-OP-CLEAR                      PIC X(8)  VALUE 'CLEAR'.
       01 P-OP-COUNT                      PIC X(8)  VALUE 'COUNT'.
       01 P-OP-GETLAST                    PIC X(8)  VALUE 'GETLAST'.

      * Scripted account numbers for the happy path.
       01 WS-ACCS.
          05 FILLER                       PIC 9(8) VALUE 00000111.
          05 FILLER                       PIC 9(8) VALUE 00000222.
          05 FILLER                       PIC 9(8) VALUE 00000333.
       01 WS-ACCS-R REDEFINES WS-ACCS.
          05 WS-ACC OCCURS 3 TIMES         PIC 9(8).

       01 WS-I                             PIC 9(4) VALUE 0.
       01 WS-FAILURES                      PIC 9(4) VALUE 0.
       01 WS-FAILURES-ACC                  PIC 9(4) VALUE 0.

       PROCEDURE DIVISION.
       MAIN-A.
           DISPLAY "=== delcusTest : DELCUS (cascade delete) ==="

      *    ---- Test 1: customer with 3 accounts ----
           CALL 'CICSLINK' USING LNK-RESET WS-LNK-LEN LNK-CTRL
           CALL 'DB2PROC' USING P-OP-CLEAR FIX-PROC SQLCA
           PERFORM SEED-CUSTOMER
           MOVE 'Y' TO LC-INQOK
           CALL 'CICSLINK' USING LNK-INQOK WS-LNK-LEN LNK-CTRL
           PERFORM VARYING WS-I FROM 1 BY 1 UNTIL WS-I > 3
              MOVE WS-ACC(WS-I) TO LC-ACCNO
              CALL 'CICSLINK' USING LNK-ADDAC WS-LNK-LEN LNK-CTRL
           END-PERFORM

           INITIALIZE WS-COMM
           MOVE '0000000022' TO COMM-CUSTNO
           CALL 'DELCUS' USING WS-COMM

           DISPLAY "    success=[" COMM-DEL-SUCCESS "] fail=["
                   COMM-DEL-FAIL-CD "]"
           IF COMM-DEL-SUCCESS = 'Y' AND COMM-DEL-FAIL-CD = ' '
              DISPLAY "PASS: DELCUS reported success"
           ELSE
              DISPLAY "FAIL: DELCUS did not succeed"
              ADD 1 TO WS-FAILURES
           END-IF

      *    DELACC must have been LINKed once per account.
           CALL 'CICSLINK' USING LNK-GETCN WS-LNK-LEN LNK-CTRL
           IF LC-NACC = 3
              DISPLAY "PASS: DELACC LINKed 3 times (one per account)"
           ELSE
              DISPLAY "FAIL: DELACC link count = " LC-NACC
              ADD 1 TO WS-FAILURES
           END-IF

      *    ...with the right account keys, in order.
           MOVE 0 TO WS-FAILURES-ACC
           PERFORM VARYING WS-I FROM 1 BY 1 UNTIL WS-I > 3
              MOVE WS-I TO LC-IDX
              CALL 'CICSLINK' USING LNK-GETAC WS-LNK-LEN LNK-CTRL
              IF LC-ACCNO NOT = WS-ACC(WS-I)
                 ADD 1 TO WS-FAILURES-ACC
              END-IF
           END-PERFORM
           IF WS-FAILURES-ACC = 0
              DISPLAY "PASS: DELACC keys match the scripted accounts"
           ELSE
              DISPLAY "FAIL: DELACC keys wrong"
              ADD 1 TO WS-FAILURES
           END-IF

      *    The CUSTOMER record must now be gone.
           PERFORM READ-CUSTOMER-22
           IF V-RESP = 13
              DISPLAY "PASS: CUSTOMER record deleted after cascade"
           ELSE
              DISPLAY "FAIL: CUSTOMER record still present (resp="
                 V-RESP ")"
              ADD 1 TO WS-FAILURES
           END-IF

      *    Exactly one PROCTRAN 'ODC' audit row.
           CALL 'DB2PROC' USING P-OP-COUNT FIX-PROC SQLCA
           CALL 'DB2PROC' USING P-OP-GETLAST FIX-PROC SQLCA
           IF SQLERRD(1) = 1 AND FIXP-TYPE = 'ODC'
              DISPLAY "PASS: one PROCTRAN 'ODC' audit row written"
           ELSE
              DISPLAY "FAIL: PROCTRAN audit wrong (count " SQLERRD(1)
                 " type [" FIXP-TYPE "])"
              ADD 1 TO WS-FAILURES
           END-IF

      *    ---- Test 2: customer not found ----
           CALL 'CICSLINK' USING LNK-RESET WS-LNK-LEN LNK-CTRL
           CALL 'DB2PROC' USING P-OP-CLEAR FIX-PROC SQLCA
           PERFORM SEED-CUSTOMER
           MOVE 'N' TO LC-INQOK
           CALL 'CICSLINK' USING LNK-INQOK WS-LNK-LEN LNK-CTRL

           INITIALIZE WS-COMM
           MOVE '0000000022' TO COMM-CUSTNO
           CALL 'DELCUS' USING WS-COMM

           DISPLAY "    success=[" COMM-DEL-SUCCESS "] fail=["
                   COMM-DEL-FAIL-CD "]"
           IF COMM-DEL-SUCCESS = 'N' AND COMM-DEL-FAIL-CD = '1'
              DISPLAY "PASS: customer-not-found flagged (N, fail '1')"
           ELSE
              DISPLAY "FAIL: not-found not flagged correctly"
              ADD 1 TO WS-FAILURES
           END-IF

           CALL 'CICSLINK' USING LNK-GETCN WS-LNK-LEN LNK-CTRL
           PERFORM READ-CUSTOMER-22
           CALL 'DB2PROC' USING P-OP-COUNT FIX-PROC SQLCA
           IF LC-NACC = 0 AND V-RESP = 0 AND SQLERRD(1) = 0
              DISPLAY "PASS: nothing deleted, no audit row on not-found"
           ELSE
              DISPLAY "FAIL: side effect on not-found (delacc " LC-NACC
                 " custresp " V-RESP " audit " SQLERRD(1) ")"
              ADD 1 TO WS-FAILURES
           END-IF

           IF WS-FAILURES = 0
              DISPLAY "RESULT: delcusTest PASSED"
              MOVE 0 TO RETURN-CODE
           ELSE
              DISPLAY "RESULT: delcusTest FAILED (" WS-FAILURES ")"
              MOVE 1 TO RETURN-CODE
           END-IF
           STOP RUN.

      *-----------------------------------------------------------------
      * Reset the CUSTOMER file and seed the customer to be deleted
      * (key = sort code 987654 / customer number 22).
       SEED-CUSTOMER.
           MOVE 'RESET   ' TO V-OP
           CALL 'CICSVSAM' USING V-OP V-FILE V-KEY CUST-IMG
                V-RESP V-RESP2
           INITIALIZE CUST-IMG
           MOVE 'CUST' TO CUSTOMER-EYECATCHER
           MOVE 987654 TO CUSTOMER-SORTCODE
           MOVE 22 TO CUSTOMER-NUMBER OF CUST-IMG
           MOVE 'MR JOHN DOE' TO CUSTOMER-NAME
           MOVE '1 THE STREET' TO CUSTOMER-ADDRESS
           MOVE 15071980 TO CUSTOMER-DATE-OF-BIRTH
           MOVE 500 TO CUSTOMER-CREDIT-SCORE
           MOVE 01012025 TO CUSTOMER-CS-REVIEW-DATE
           MOVE 'SEED    ' TO V-OP
           CALL 'CICSVSAM' USING V-OP V-FILE V-KEY CUST-IMG
                V-RESP V-RESP2.

       READ-CUSTOMER-22.
           MOVE 987654 TO CUSTOMER-SORTCODE
           MOVE 22 TO CUSTOMER-NUMBER OF CUST-IMG
           MOVE CUSTOMER-KEY TO V-KEY
           MOVE 'READ    ' TO V-OP
           CALL 'CICSVSAM' USING V-OP V-FILE V-KEY CUST-IMG
                V-RESP V-RESP2.
