      ******************************************************************
      * inqcustTest - isolated unit test for INQCUST running off-CICS
      * through the GnuCOBOL shim harness.
      *
      * INQCUST returns a CUSTOMER record for a given customer number.
      * Besides a plain keyed READ it has two special inputs:
      *   - 0000000000 : pick a RANDOM customer (< highest in use). The
      *                  RNG is FUNCTION RANDOM(EIBTASKN), pinned by the
      *                  harness via CBSA_TEST_TASKN, so it is deterministic.
      *   - 9999999999 : return the LAST (highest-key) customer, found by
      *                  a STARTBR(HIGH-VALUES) + READPREV browse.
      * All three paths run against the CICSVSAM KSDS double.
      *
      * Fixtures are keyed on sort code 987654 (from the SORTCODE copybook,
      * which INQCUST also uses).
      *
      * Assertions:
      *   1. happy path : known customer returned (name/number match)
      *   2. not found  : unknown customer -> SUCCESS='N', FAIL-CD='1',
      *                   name+address blanked
      *   3. last cust  : 9999999999 -> highest-key customer via browse
      *   4. random     : 0000000000 -> a seeded customer, in range and
      *                   deterministic under a fixed CBSA_TEST_TASKN
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. INQCUSTTEST.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
      * COMMAREA layout - matches src/base/cobol_copy/INQCUST.cpy.
       01 WS-COMM.
          COPY INQCUST.

      * A CUSTOMER record image (259 bytes) used for seeding fixtures.
       01 WS-CUST.
          COPY CUSTOMER.

       01 WS-FILE                     PIC X(8)  VALUE 'CUSTOMER'.
       01 WS-RESP                     PIC S9(8) COMP VALUE 0.
       01 WS-RESP2                    PIC S9(8) COMP VALUE 0.

       01 WS-I                        PIC 9(4)  VALUE 0.
       01 WS-I-DIGIT                  PIC 9     VALUE 0.
       01 WS-NCUST                    PIC 9(4)  VALUE 0.
       01 WS-NAME-BUILD               PIC X(60) VALUE SPACES.

       01 WS-RAND-1                   PIC 9(10) VALUE 0.
       01 WS-RAND-2                   PIC 9(10) VALUE 0.

       01 WS-FAILURES                 PIC 9(4) VALUE 0.

       PROCEDURE DIVISION.
       MAIN-A.
           DISPLAY "=== inqcustTest : INQCUST (VSAM inquiry) ==="

           PERFORM TEST-HAPPY-PATH
           PERFORM TEST-NOT-FOUND
           PERFORM TEST-LAST-CUSTOMER
           PERFORM TEST-RANDOM-CUSTOMER

           IF WS-FAILURES = 0
              DISPLAY "RESULT: inqcustTest PASSED"
              MOVE 0 TO RETURN-CODE
           ELSE
              DISPLAY "RESULT: inqcustTest FAILED (" WS-FAILURES ")"
              MOVE 1 TO RETURN-CODE
           END-IF

           STOP RUN.

      *-----------------------------------------------------------------
       TEST-HAPPY-PATH.
           PERFORM RESET-STORE
           MOVE 5 TO WS-NCUST
           PERFORM SEED-RANGE

           INITIALIZE WS-COMM
           MOVE 3 TO INQCUST-CUSTNO
           CALL 'INQCUST' USING WS-COMM

           DISPLAY "    happy: success=[" INQCUST-INQ-SUCCESS
                   "] custno=[" INQCUST-CUSTNO
                   "] name=[" FUNCTION TRIM(INQCUST-NAME) "]"
           IF INQCUST-INQ-SUCCESS = 'Y'
              AND INQCUST-CUSTNO = 3
              AND INQCUST-NAME = 'Customer Number 3'
              DISPLAY "PASS: known customer 3 returned correctly"
           ELSE
              DISPLAY "FAIL: customer 3 not returned as expected"
              ADD 1 TO WS-FAILURES
           END-IF.

      *-----------------------------------------------------------------
       TEST-NOT-FOUND.
           PERFORM RESET-STORE
           MOVE 5 TO WS-NCUST
           PERFORM SEED-RANGE

           INITIALIZE WS-COMM
           MOVE 88 TO INQCUST-CUSTNO
           CALL 'INQCUST' USING WS-COMM

           DISPLAY "    not-found: success=[" INQCUST-INQ-SUCCESS
                   "] fail-cd=[" INQCUST-INQ-FAIL-CD "]"
           IF INQCUST-INQ-SUCCESS = 'N'
              AND INQCUST-INQ-FAIL-CD = '1'
              AND INQCUST-NAME = SPACES
              AND INQCUST-ADDR = SPACES
              DISPLAY "PASS: unknown customer -> N/1, name+addr blank"
           ELSE
              DISPLAY "FAIL: expected N/1 with blanked name/address"
              ADD 1 TO WS-FAILURES
           END-IF.

      *-----------------------------------------------------------------
      * 9999999999 -> highest-key customer, resolved through the
      * STARTBR(HIGH-VALUES)/READPREV/ENDBR browse.
       TEST-LAST-CUSTOMER.
           PERFORM RESET-STORE
           MOVE 5 TO WS-NCUST
           PERFORM SEED-RANGE

           INITIALIZE WS-COMM
           MOVE 9999999999 TO INQCUST-CUSTNO
           CALL 'INQCUST' USING WS-COMM

           DISPLAY "    last: success=[" INQCUST-INQ-SUCCESS
                   "] custno=[" INQCUST-CUSTNO "]"
           IF INQCUST-INQ-SUCCESS = 'Y' AND INQCUST-CUSTNO = 5
              DISPLAY "PASS: last-customer browse returned highest (5)"
           ELSE
              DISPLAY "FAIL: expected highest customer 5 via browse"
              ADD 1 TO WS-FAILURES
           END-IF.

      *-----------------------------------------------------------------
      * 0000000000 -> random customer < highest in use. Deterministic
      * under a fixed seed; with a contiguous 1..5 range every draw hits
      * a seeded record, so the read always succeeds.
       TEST-RANDOM-CUSTOMER.
           PERFORM RESET-STORE
           MOVE 5 TO WS-NCUST
           PERFORM SEED-RANGE

           INITIALIZE WS-COMM
           MOVE 0 TO INQCUST-CUSTNO
           CALL 'INQCUST' USING WS-COMM
           MOVE INQCUST-CUSTNO TO WS-RAND-1

           INITIALIZE WS-COMM
           MOVE 0 TO INQCUST-CUSTNO
           CALL 'INQCUST' USING WS-COMM
           MOVE INQCUST-CUSTNO TO WS-RAND-2

           DISPLAY "    random: draw1=[" WS-RAND-1
                   "] draw2=[" WS-RAND-2 "]"
           IF WS-RAND-1 >= 1 AND WS-RAND-1 <= 5
              DISPLAY "PASS: random customer within seeded range 1..5"
           ELSE
              DISPLAY "FAIL: random customer out of range"
              ADD 1 TO WS-FAILURES
           END-IF
           IF WS-RAND-1 = WS-RAND-2
              DISPLAY "PASS: random draw deterministic under fixed seed"
           ELSE
              DISPLAY "FAIL: random draw not deterministic"
              ADD 1 TO WS-FAILURES
           END-IF.

      *-----------------------------------------------------------------
      * Helpers
      *-----------------------------------------------------------------
      * Seed a contiguous block of customers numbered 1..WS-NCUST.
       SEED-RANGE.
           PERFORM VARYING WS-I FROM 1 BY 1 UNTIL WS-I > WS-NCUST
              INITIALIZE WS-CUST
              SET CUSTOMER-EYECATCHER-VALUE OF WS-CUST TO TRUE
              MOVE 987654 TO CUSTOMER-SORTCODE OF WS-CUST
              MOVE WS-I   TO CUSTOMER-NUMBER   OF WS-CUST
              MOVE WS-I   TO WS-I-DIGIT
              MOVE SPACES TO WS-NAME-BUILD
              STRING 'Customer Number ' DELIMITED BY SIZE
                     WS-I-DIGIT DELIMITED BY SIZE
                     INTO WS-NAME-BUILD
              END-STRING
              MOVE WS-NAME-BUILD TO CUSTOMER-NAME OF WS-CUST
              MOVE '2 Test Avenue, Hursley' TO
                     CUSTOMER-ADDRESS OF WS-CUST
              MOVE 19900101 TO CUSTOMER-DATE-OF-BIRTH OF WS-CUST
              MOVE 700      TO CUSTOMER-CREDIT-SCORE   OF WS-CUST
              MOVE 20240101 TO CUSTOMER-CS-REVIEW-DATE OF WS-CUST
              CALL 'CICSVSAM' USING
                   BY CONTENT 'SEED    '
                   BY CONTENT WS-FILE
                   BY REFERENCE OMITTED
                   BY REFERENCE WS-CUST
                   BY REFERENCE WS-RESP
                   BY REFERENCE WS-RESP2
           END-PERFORM.

       RESET-STORE.
           CALL 'CICSVSAM' USING
                BY CONTENT 'RESET   '
                BY CONTENT WS-FILE
                BY REFERENCE OMITTED
                BY REFERENCE OMITTED
                BY REFERENCE WS-RESP
                BY REFERENCE WS-RESP2.
