      ******************************************************************
      * CICSLINK - test double for EXEC CICS LINK PROGRAM(..) COMMAREA(..)
      *
      * Most programs LINK only on an error path (to ABNDPROC), which the
      * happy-path tests do not exercise; for those this stub just records
      * the invocation without pulling in the linked program.
      *
      * Two programs are LINKed on a normal path and get canned responses
      * so the caller can proceed without pulling in the real program (and
      * its own Db2 dependencies):
      *
      *   INQCUST  - customer validation. Returns a valid ("found")
      *              customer (INQCUST-INQ-SUCCESS = 'Y'). Force the
      *              not-found path with CBSA_TEST_INQCUST_SUCCESS=N.
      *
      *   INQACCCU - customer's account count (CREACC uses it to enforce
      *              the max-accounts-per-customer rule). Returns
      *              COMM-SUCCESS and a NUMBER-OF-ACCOUNTS the test
      *              supplies via the environment:
      *                CBSA_TEST_INQACCCU_COUNT   (default 1)
      *                CBSA_TEST_INQACCCU_SUCCESS (default Y; N -> failure)
      *              A driver can set these at run time (DISPLAY .. UPON
      *              ENVIRONMENT-NAME / ENVIRONMENT-VALUE) to exercise the
      *              under-limit, at-limit and error paths in one run.
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. CICSLINK.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 WS-ENV-SUCCESS          PIC X(4)  VALUE SPACES.
       01 WS-ENV-COUNT            PIC X(16) VALUE SPACES.

       LINKAGE SECTION.
       01 LK-PROGRAM              PIC X(8).
       01 LK-COMMAREA.
          COPY INQCUST.
      * A fixed-length prefix view of the INQACCCU COMMAREA (the full
      * layout has an OCCURS DEPENDING ON, which cannot be REDEFINES-ed;
      * only these leading fields are needed to drive CREACC's rule).
       01 LK-ACCCU-VIEW REDEFINES LK-COMMAREA.
          05 LK-ACC-NUMBER-OF-ACCOUNTS  PIC S9(8) BINARY.
          05 LK-ACC-CUSTOMER-NUMBER     PIC 9(10).
          05 LK-ACC-COMM-SUCCESS        PIC X.
          05 LK-ACC-COMM-FAIL-CODE      PIC X.
          05 LK-ACC-CUSTOMER-FOUND      PIC X.

       PROCEDURE DIVISION USING LK-PROGRAM LK-COMMAREA.
       A010.
           EVALUATE LK-PROGRAM
              WHEN 'INQCUST '
                 PERFORM DO-INQCUST
              WHEN 'INQACCCU'
                 PERFORM DO-INQACCCU
              WHEN OTHER
                 DISPLAY "CICSLINK (stub): LINK to " LK-PROGRAM
           END-EVALUATE
           GOBACK.

       DO-INQCUST.
           MOVE SPACES TO WS-ENV-SUCCESS
           ACCEPT WS-ENV-SUCCESS FROM ENVIRONMENT
              "CBSA_TEST_INQCUST_SUCCESS"
           END-ACCEPT
           IF WS-ENV-SUCCESS = 'N' OR WS-ENV-SUCCESS = 'n'
              MOVE 'N' TO INQCUST-INQ-SUCCESS
              MOVE '1' TO INQCUST-INQ-FAIL-CD
           ELSE
              MOVE 'Y' TO INQCUST-INQ-SUCCESS
              MOVE '0' TO INQCUST-INQ-FAIL-CD
           END-IF.

       DO-INQACCCU.
           MOVE 1 TO LK-ACC-NUMBER-OF-ACCOUNTS
           MOVE SPACES TO WS-ENV-COUNT
           ACCEPT WS-ENV-COUNT FROM ENVIRONMENT
              "CBSA_TEST_INQACCCU_COUNT"
           END-ACCEPT
           IF WS-ENV-COUNT NOT = SPACES
              MOVE FUNCTION NUMVAL(WS-ENV-COUNT)
                 TO LK-ACC-NUMBER-OF-ACCOUNTS
           END-IF

           MOVE SPACES TO WS-ENV-SUCCESS
           ACCEPT WS-ENV-SUCCESS FROM ENVIRONMENT
              "CBSA_TEST_INQACCCU_SUCCESS"
           END-ACCEPT
           IF WS-ENV-SUCCESS = 'N' OR WS-ENV-SUCCESS = 'n'
              MOVE 'N' TO LK-ACC-COMM-SUCCESS
           ELSE
              MOVE 'Y' TO LK-ACC-COMM-SUCCESS
           END-IF
           MOVE 'Y' TO LK-ACC-CUSTOMER-FOUND.
