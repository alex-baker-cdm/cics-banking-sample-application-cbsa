      ******************************************************************
      * crecustTest - isolated unit test for CRECUST (create customer):
      * asynchronous credit check, customer-number allocation from the
      * CUSTOMER control record, CUSTOMER VSAM WRITE, and a PROCTRAN
      * audit-row INSERT.
      *
      * The async credit check is emulated deterministically by CICSASYN:
      * the driver scripts the score each of the 5 child agencies returns,
      * and CRECUST's aggregate is the integer average of those scores.
      *
      * The CUSTOMER control record is the special key = sort code 0 /
      * number all-9s record in the CUSTOMER file. CRECUST reads it under
      * update, bumps LAST-CUSTOMER-NUMBER for the new customer, then later
      * bumps NUMBER-OF-CUSTOMERS too.
      *
      * Assertions:
      *   1. happy : valid input -> success; customer number allocated
      *              (seed last=100 -> 101); credit score = avg(scripted)
      *              = 300; CUSTOMER record written with those fields;
      *              control record bumped (count 5->6, last 100->101);
      *              one PROCTRAN 'OCC' audit row.
      *   2. edge  : invalid DOB (birth year < 1601) -> success = 'N',
      *              fail code 'O', no CUSTOMER record written, control
      *              record untouched, no audit row.
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. CRECUSTTEST.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
      * CRECUST COMMAREA - matches src/base/cobol_copy/CRECUST.cpy.
       01 WS-COMM.
          COPY CRECUST.

      * CUSTOMER control-record image (special key-0 record).
       01 CTL-IMG.
          COPY CUSTCTRL.

      * CUSTOMER record image (for seeding / reading back).
       01 CUST-IMG.
          COPY CUSTOMER.

      * PROCTRAN audit-row inspection view.
       01 FIX-PROC.
          COPY HOSTPROC.
       COPY SQLCA.

      * CICSVSAM control / inspection call operands.
       01 V-OP                            PIC X(8)  VALUE SPACES.
       01 V-FILE                          PIC X(8)  VALUE 'CUSTOMER'.
       01 V-KEY                           PIC X(16) VALUE SPACES.
       01 V-RESP                          PIC S9(8) COMP VALUE 0.
       01 V-RESP2                         PIC S9(8) COMP VALUE 0.
       01 WS-CUST-RESP                    PIC S9(8) COMP VALUE 0.

      * CICSASYN driver-control call operands.
       01 A-OP                            PIC X(8)  VALUE SPACES.
       01 A-TRANSID                       PIC X(4)  VALUE SPACES.
       01 A-CHANNEL                       PIC X(16) VALUE SPACES.
       01 A-CHILD                         PIC X(16) VALUE SPACES.
       01 A-ANYTKN                        PIC X(16) VALUE SPACES.
       01 A-COMPSTAT                      PIC S9(8) COMP VALUE 0.
       01 A-ABCODE                        PIC X(4)  VALUE SPACES.
       01 A-RESP                          PIC S9(8) COMP VALUE 0.
       01 A-RESP2                         PIC S9(8) COMP VALUE 0.

      * DB2PROC control operands.
       01 P-OP-CLEAR                      PIC X(8)  VALUE 'CLEAR'.
       01 P-OP-COUNT                      PIC X(8)  VALUE 'COUNT'.
       01 P-OP-GETLAST                    PIC X(8)  VALUE 'GETLAST'.

       01 WS-SLOT                         PIC 9(4)  VALUE 0.
       01 WS-SLOT-TXT                     PIC 9(4)  VALUE 0.
       01 WS-SCORE                        PIC 9(4)  VALUE 0.

       01 WS-FAILURES                     PIC 9(4)  VALUE 0.

       PROCEDURE DIVISION.
       MAIN-A.
           DISPLAY "=== crecustTest : CRECUST (async CS + VSAM) ==="

      *    ---- Test 1: happy path ----
           PERFORM SEED-CONTROL-RECORD
           PERFORM SCRIPT-CREDIT-SCORES
           CALL 'DB2PROC' USING P-OP-CLEAR FIX-PROC SQLCA

           PERFORM BUILD-VALID-INPUT
           MOVE 15071990 TO COMM-DATE-OF-BIRTH
           CALL 'CRECUST' USING WS-COMM

           DISPLAY "  ok=[" COMM-SUCCESS "] fc=[" COMM-FAIL-CODE
              "] num=" COMM-NUMBER " score=" COMM-CREDIT-SCORE
           IF COMM-SUCCESS = 'Y' AND COMM-FAIL-CODE = ' '
              DISPLAY "PASS: CRECUST reported success"
           ELSE
              DISPLAY "FAIL: CRECUST did not succeed"
              ADD 1 TO WS-FAILURES
           END-IF

           IF COMM-NUMBER = 101
              DISPLAY "PASS: next customer number allocated (101)"
           ELSE
              DISPLAY "FAIL: wrong customer number " COMM-NUMBER
              ADD 1 TO WS-FAILURES
           END-IF

           IF COMM-CREDIT-SCORE = 300
              DISPLAY "PASS: credit score = avg(scores) = 300"
           ELSE
              DISPLAY "FAIL: credit score wrong " COMM-CREDIT-SCORE
              ADD 1 TO WS-FAILURES
           END-IF

      *    CUSTOMER VSAM record must have been written.
           PERFORM READ-CUSTOMER-101
           IF V-RESP = 0 AND CUSTOMER-CREDIT-SCORE = 300
              AND CUSTOMER-SORTCODE = 987654
              AND CUSTOMER-NUMBER OF CUST-IMG = 101
              AND CUSTOMER-NAME = COMM-NAME
              DISPLAY "PASS: CUSTOMER record written OK"
           ELSE
              DISPLAY "FAIL: CUSTOMER record wrong (resp=" V-RESP
                 " score=" CUSTOMER-CREDIT-SCORE ")"
              ADD 1 TO WS-FAILURES
           END-IF

      *    Control record must be incremented.
           PERFORM READ-CONTROL-RECORD
           IF NUMBER-OF-CUSTOMERS = 6 AND LAST-CUSTOMER-NUMBER = 101
              DISPLAY "PASS: control record bumped (count 6, last 101)"
           ELSE
              DISPLAY "FAIL: control record wrong (count "
                 NUMBER-OF-CUSTOMERS " last " LAST-CUSTOMER-NUMBER ")"
              ADD 1 TO WS-FAILURES
           END-IF

      *    Exactly one PROCTRAN 'OCC' audit row.
           CALL 'DB2PROC' USING P-OP-COUNT FIX-PROC SQLCA
           CALL 'DB2PROC' USING P-OP-GETLAST FIX-PROC SQLCA
           IF SQLERRD(1) = 1 AND FIXP-TYPE = 'OCC'
              DISPLAY "PASS: one PROCTRAN 'OCC' audit row written"
           ELSE
              DISPLAY "FAIL: PROCTRAN audit wrong (count " SQLERRD(1)
                 " type [" FIXP-TYPE "])"
              ADD 1 TO WS-FAILURES
           END-IF

      *    ---- Test 2: invalid date of birth ----
           PERFORM SEED-CONTROL-RECORD
           PERFORM SCRIPT-CREDIT-SCORES
           CALL 'DB2PROC' USING P-OP-CLEAR FIX-PROC SQLCA

           PERFORM BUILD-VALID-INPUT
           MOVE 15071500 TO COMM-DATE-OF-BIRTH
           CALL 'CRECUST' USING WS-COMM

           DISPLAY "  ok=[" COMM-SUCCESS "] fc=[" COMM-FAIL-CODE "]"
           IF COMM-SUCCESS = 'N' AND COMM-FAIL-CODE = 'O'
              DISPLAY "PASS: invalid DOB rejected (N, fail 'O')"
           ELSE
              DISPLAY "FAIL: invalid DOB not rejected correctly"
              ADD 1 TO WS-FAILURES
           END-IF

           PERFORM READ-CUSTOMER-101
           MOVE V-RESP TO WS-CUST-RESP
           PERFORM READ-CONTROL-RECORD
           CALL 'DB2PROC' USING P-OP-COUNT FIX-PROC SQLCA
           IF WS-CUST-RESP = 13 AND NUMBER-OF-CUSTOMERS = 5
              AND LAST-CUSTOMER-NUMBER = 100 AND SQLERRD(1) = 0
              DISPLAY "PASS: no write; control + audit untouched"
           ELSE
              DISPLAY "FAIL: side effect (resp=" V-RESP
                 " count " NUMBER-OF-CUSTOMERS " last "
                 LAST-CUSTOMER-NUMBER " audit " SQLERRD(1) ")"
              ADD 1 TO WS-FAILURES
           END-IF

           IF WS-FAILURES = 0
              DISPLAY "RESULT: crecustTest PASSED"
              MOVE 0 TO RETURN-CODE
           ELSE
              DISPLAY "RESULT: crecustTest FAILED (" WS-FAILURES ")"
              MOVE 1 TO RETURN-CODE
           END-IF
           STOP RUN.

      *-----------------------------------------------------------------
      * Reset the CUSTOMER file and seed the control record with
      * NUMBER-OF-CUSTOMERS = 5, LAST-CUSTOMER-NUMBER = 100.
       SEED-CONTROL-RECORD.
           MOVE 'RESET   ' TO V-OP
           CALL 'CICSVSAM' USING V-OP V-FILE V-KEY CTL-IMG
                V-RESP V-RESP2
           INITIALIZE CTL-IMG
           MOVE 'CTRL' TO CUSTOMER-CONTROL-EYECATCHER
           MOVE 0 TO CUSTOMER-CONTROL-SORTCODE
           MOVE 9999999999 TO CUSTOMER-CONTROL-NUMBER
           MOVE 5 TO NUMBER-OF-CUSTOMERS
           MOVE 100 TO LAST-CUSTOMER-NUMBER
           MOVE 'SEED    ' TO V-OP
           CALL 'CICSVSAM' USING V-OP V-FILE V-KEY CTL-IMG
                V-RESP V-RESP2.

      * Script the five child credit scores 100/200/300/400/500 (avg 300).
       SCRIPT-CREDIT-SCORES.
           MOVE 'RESET   ' TO A-OP
           CALL 'CICSASYN' USING A-OP A-TRANSID A-CHANNEL A-CHILD
                A-ANYTKN A-COMPSTAT A-ABCODE A-RESP A-RESP2
           PERFORM VARYING WS-SLOT FROM 1 BY 1 UNTIL WS-SLOT > 5
              MOVE 'SETSCORE' TO A-OP
              MOVE WS-SLOT TO WS-SLOT-TXT
              MOVE WS-SLOT-TXT TO A-TRANSID
              COMPUTE A-COMPSTAT = WS-SLOT * 100
              CALL 'CICSASYN' USING A-OP A-TRANSID A-CHANNEL A-CHILD
                   A-ANYTKN A-COMPSTAT A-ABCODE A-RESP A-RESP2
           END-PERFORM.

       BUILD-VALID-INPUT.
           INITIALIZE WS-COMM
           MOVE 'CUST' TO COMM-EYECATCHER
           MOVE 987654 TO COMM-SORTCODE
           MOVE 0 TO COMM-NUMBER
           MOVE 'MRS JANE DOE' TO COMM-NAME
           MOVE '1 THE STREET, ANYTOWN' TO COMM-ADDRESS
           MOVE 0 TO COMM-CREDIT-SCORE
           MOVE 0 TO COMM-CS-REVIEW-DATE
           MOVE SPACE TO COMM-SUCCESS
           MOVE SPACE TO COMM-FAIL-CODE.

       READ-CUSTOMER-101.
           MOVE 987654 TO CUSTOMER-SORTCODE
           MOVE 101 TO CUSTOMER-NUMBER OF CUST-IMG
           MOVE CUSTOMER-KEY TO V-KEY
           MOVE 'READ    ' TO V-OP
           CALL 'CICSVSAM' USING V-OP V-FILE V-KEY CUST-IMG
                V-RESP V-RESP2.

       READ-CONTROL-RECORD.
           MOVE 0 TO CUSTOMER-CONTROL-SORTCODE
           MOVE 9999999999 TO CUSTOMER-CONTROL-NUMBER
           MOVE CUSTOMER-CONTROL-KEY TO V-KEY
           MOVE 'READ    ' TO V-OP
           CALL 'CICSVSAM' USING V-OP V-FILE V-KEY CTL-IMG
                V-RESP V-RESP2.
