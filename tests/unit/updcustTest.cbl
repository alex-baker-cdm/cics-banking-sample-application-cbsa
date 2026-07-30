      ******************************************************************
      * updcustTest - isolated unit test for UPDCUST running off-CICS
      * through the GnuCOBOL shim harness.
      *
      * UPDCUST reads a CUSTOMER record for update (EXEC CICS READ ...
      * UPDATE), applies the supplied name/address, and REWRITEs it. The
      * test seeds a fixture customer into the CICSVSAM KSDS double, drives
      * UPDCUST via its COMMAREA, and inspects both the returned COMMAREA
      * and the record left in the store.
      *
      * Fixtures are keyed on sort code 987654 (from the SORTCODE copybook,
      * which UPDCUST also uses).
      *
      * Assertions:
      *   1. happy path  : update succeeds (COMM-UPD-SUCCESS = 'Y')
      *   2. happy path  : the stored record now holds the new name+address
      *   3. not found   : unknown customer -> SUCCESS='N', FAIL-CD='1'
      *   4. error path  : scripted RESP on the READ -> SUCCESS='N',
      *                    FAIL-CD='2' (non-NOTFND file error)
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. UPDCUSTTEST.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
      * COMMAREA layout - matches src/base/cobol_copy/UPDCUST.cpy.
       01 WS-COMM.
          COPY UPDCUST.

      * A CUSTOMER record image (259 bytes) for seeding + read-back.
       01 WS-CUST.
          COPY CUSTOMER.

      * 16-byte KSDS key = sort code (6) + customer number (10).
       01 WS-KEY.
          05 WS-KEY-SORT              PIC 9(6).
          05 WS-KEY-NUM               PIC 9(10).

       01 WS-FILE                     PIC X(8)  VALUE 'CUSTOMER'.
       01 WS-RESP                     PIC S9(8) COMP VALUE 0.
       01 WS-RESP2                    PIC S9(8) COMP VALUE 0.

       01 WS-NEW-NAME                 PIC X(60)
              VALUE 'Mr John Smith'.
       01 WS-NEW-ADDR                 PIC X(160)
              VALUE '10 New Street, Winchester'.

       01 WS-FAILURES                 PIC 9(4) VALUE 0.

       PROCEDURE DIVISION.
       MAIN-A.
           DISPLAY "=== updcustTest : UPDCUST (VSAM update) ==="

           PERFORM TEST-HAPPY-PATH
           PERFORM TEST-NOT-FOUND
           PERFORM TEST-SCRIPTED-ERROR

           IF WS-FAILURES = 0
              DISPLAY "RESULT: updcustTest PASSED"
              MOVE 0 TO RETURN-CODE
           ELSE
              DISPLAY "RESULT: updcustTest FAILED (" WS-FAILURES ")"
              MOVE 1 TO RETURN-CODE
           END-IF

           STOP RUN.

      *-----------------------------------------------------------------
       TEST-HAPPY-PATH.
           PERFORM RESET-STORE
           MOVE 3 TO WS-KEY-NUM
           PERFORM SEED-CUSTOMER

           PERFORM INIT-COMM
           MOVE "0000000003"  TO COMM-CUSTNO
           MOVE WS-NEW-NAME   TO COMM-NAME
           MOVE WS-NEW-ADDR   TO COMM-ADDR

           CALL 'UPDCUST' USING WS-COMM

           DISPLAY "    update success flag = [" COMM-UPD-SUCCESS "]"
           IF COMM-UPD-SUCCESS = 'Y'
              DISPLAY "PASS: update reported success"
           ELSE
              DISPLAY "FAIL: expected success, fail-cd ["
                      COMM-UPD-FAIL-CD "]"
              ADD 1 TO WS-FAILURES
           END-IF

      *    Read the record back out of the store and check it changed.
           PERFORM READ-BACK
           IF WS-RESP = 0
              AND CUSTOMER-NAME OF WS-CUST = WS-NEW-NAME
              AND CUSTOMER-ADDRESS OF WS-CUST = WS-NEW-ADDR
              DISPLAY "PASS: stored record holds new name + address"
           ELSE
              DISPLAY "FAIL: stored record not updated: name=["
                      CUSTOMER-NAME OF WS-CUST "]"
              ADD 1 TO WS-FAILURES
           END-IF.

      *-----------------------------------------------------------------
       TEST-NOT-FOUND.
           PERFORM RESET-STORE
           MOVE 3 TO WS-KEY-NUM
           PERFORM SEED-CUSTOMER

           PERFORM INIT-COMM
           MOVE "0000000099"  TO COMM-CUSTNO
           MOVE WS-NEW-NAME   TO COMM-NAME
           MOVE WS-NEW-ADDR   TO COMM-ADDR

           CALL 'UPDCUST' USING WS-COMM

           DISPLAY "    not-found success=[" COMM-UPD-SUCCESS
                   "] fail-cd=[" COMM-UPD-FAIL-CD "]"
           IF COMM-UPD-SUCCESS = 'N' AND COMM-UPD-FAIL-CD = '1'
              DISPLAY "PASS: unknown customer -> fail code 1"
           ELSE
              DISPLAY "FAIL: expected N/1 for unknown customer"
              ADD 1 TO WS-FAILURES
           END-IF.

      *-----------------------------------------------------------------
       TEST-SCRIPTED-ERROR.
           PERFORM RESET-STORE
           MOVE 3 TO WS-KEY-NUM
           PERFORM SEED-CUSTOMER

      *    Script the next file verb (the READ UPDATE) to return a
      *    non-NOTFND error so UPDCUST takes its generic-failure branch.
           MOVE 2 TO WS-RESP
           CALL 'CICSVSAM' USING
                BY CONTENT 'FORCERSP'
                BY CONTENT WS-FILE
                BY REFERENCE OMITTED
                BY REFERENCE OMITTED
                BY REFERENCE WS-RESP
                BY REFERENCE WS-RESP2

           PERFORM INIT-COMM
           MOVE "0000000003"  TO COMM-CUSTNO
           MOVE WS-NEW-NAME   TO COMM-NAME
           MOVE WS-NEW-ADDR   TO COMM-ADDR

           CALL 'UPDCUST' USING WS-COMM

           DISPLAY "    scripted-error success=[" COMM-UPD-SUCCESS
                   "] fail-cd=[" COMM-UPD-FAIL-CD "]"
           IF COMM-UPD-SUCCESS = 'N' AND COMM-UPD-FAIL-CD = '2'
              DISPLAY "PASS: scripted file error -> fail code 2"
           ELSE
              DISPLAY "FAIL: expected N/2 for scripted file error"
              ADD 1 TO WS-FAILURES
           END-IF.

      *-----------------------------------------------------------------
      * Helpers
      *-----------------------------------------------------------------
       INIT-COMM.
           INITIALIZE WS-COMM
           MOVE 'CUST'   TO COMM-EYE
           MOVE '987654' TO COMM-SCODE.

       SEED-CUSTOMER.
      *    Build a fixture CUSTOMER record and insert it by key.
           INITIALIZE WS-CUST
           SET CUSTOMER-EYECATCHER-VALUE OF WS-CUST TO TRUE
           MOVE 987654       TO CUSTOMER-SORTCODE OF WS-CUST
           MOVE WS-KEY-NUM   TO CUSTOMER-NUMBER   OF WS-CUST
           MOVE 'Mrs Jane Doe'        TO CUSTOMER-NAME OF WS-CUST
           MOVE '1 Old Road, Bristol' TO CUSTOMER-ADDRESS OF WS-CUST
           MOVE 19800101     TO CUSTOMER-DATE-OF-BIRTH OF WS-CUST
           MOVE 750          TO CUSTOMER-CREDIT-SCORE  OF WS-CUST
           MOVE 20240101     TO CUSTOMER-CS-REVIEW-DATE OF WS-CUST
           CALL 'CICSVSAM' USING
                BY CONTENT 'SEED    '
                BY CONTENT WS-FILE
                BY REFERENCE OMITTED
                BY REFERENCE WS-CUST
                BY REFERENCE WS-RESP
                BY REFERENCE WS-RESP2.

       READ-BACK.
           INITIALIZE WS-CUST
           MOVE 987654     TO WS-KEY-SORT
           MOVE 3          TO WS-KEY-NUM
           CALL 'CICSVSAM' USING
                BY CONTENT 'READ    '
                BY CONTENT WS-FILE
                BY REFERENCE WS-KEY
                BY REFERENCE WS-CUST
                BY REFERENCE WS-RESP
                BY REFERENCE WS-RESP2.

       RESET-STORE.
           CALL 'CICSVSAM' USING
                BY CONTENT 'RESET   '
                BY CONTENT WS-FILE
                BY REFERENCE OMITTED
                BY REFERENCE OMITTED
                BY REFERENCE WS-RESP
                BY REFERENCE WS-RESP2.
