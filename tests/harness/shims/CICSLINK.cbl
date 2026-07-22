      ******************************************************************
      * CICSLINK - test double for EXEC CICS LINK PROGRAM(..) COMMAREA(..)
      *
      * cicsPreprocessor.py passes the COMMAREA length too:
      *
      *   CALL 'CICSLINK' USING BY CONTENT  prog(8) len(9(9)COMP-5)
      *                         BY REFERENCE commarea
      *
      * so the COMMAREA can be handled generically regardless of its
      * layout. Behaviours:
      *
      *   * INQCUST  - LINKed on the normal path to validate a customer
      *     (e.g. by INQACCCU). Rather than pull in the real INQCUST (+ its
      *     Db2 deps) this stub maps the COMMAREA to the INQCUST layout and
      *     returns a valid ("found") customer. CBSA_TEST_INQCUST_SUCCESS=N
      *     forces the not-found path.
      *
      *   * INQACCCU - customer's account count (CREACC uses it to enforce
      *     the max-accounts-per-customer rule). Returns COMM-SUCCESS and a
      *     NUMBER-OF-ACCOUNTS the test supplies via the environment:
      *       CBSA_TEST_INQACCCU_COUNT   (default 1)
      *       CBSA_TEST_INQACCCU_SUCCESS (default Y; N -> failure)
      *     A driver sets these at run time (DISPLAY .. UPON
      *     ENVIRONMENT-NAME / ENVIRONMENT-VALUE) to exercise the
      *     under-limit, at-limit and error paths in one run.
      *
      *   * everything else (the single business LINK each presentation
      *     screen makes, plus ABNDPROC) is forwarded to the resident
      *     LINKREC store, which captures the inbound COMMAREA and applies
      *     any COMMAREA a test scripted for that program. This is how a
      *     screen test asserts "the right COMMAREA was passed to DBCRFUN /
      *     CREACC / XFRFUN" and injects the business program's reply.
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. CICSLINK.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 WS-ENV-SUCCESS          PIC X(4)  VALUE SPACES.
       01 WS-ENV-COUNT            PIC X(16) VALUE SPACES.
       01 WS-INQ.
          COPY INQCUST.
       01 WS-INQ-LEN              PIC 9(9) COMP-5.

       LINKAGE SECTION.
       01 LK-PROGRAM              PIC X(8).
       01 LK-LEN                  PIC 9(9) COMP-5.
       01 LK-COMMAREA             PIC X(4096).
      * A fixed-length prefix view of the INQACCCU COMMAREA (the full
      * layout has an OCCURS DEPENDING ON, which cannot be REDEFINES-ed;
      * only these leading fields are needed to drive CREACC's rule).
       01 LK-ACCCU-VIEW REDEFINES LK-COMMAREA.
          05 LK-ACC-NUMBER-OF-ACCOUNTS  PIC S9(8) BINARY.
          05 LK-ACC-CUSTOMER-NUMBER     PIC 9(10).
          05 LK-ACC-COMM-SUCCESS        PIC X.
          05 LK-ACC-COMM-FAIL-CODE      PIC X.
          05 LK-ACC-CUSTOMER-FOUND      PIC X.

       PROCEDURE DIVISION USING LK-PROGRAM LK-LEN LK-COMMAREA.
       A010.
           EVALUATE LK-PROGRAM
              WHEN 'INQCUST '
                 PERFORM DO-INQCUST
              WHEN 'INQACCCU'
                 PERFORM DO-INQACCCU
              WHEN OTHER
                 CALL 'LINKREC' USING 'CALL    '
                      LK-PROGRAM LK-LEN LK-COMMAREA
           END-EVALUATE
           GOBACK.

       DO-INQCUST.
           MOVE LENGTH OF WS-INQ TO WS-INQ-LEN
           IF LK-LEN < WS-INQ-LEN
              MOVE LK-LEN TO WS-INQ-LEN
           END-IF
           IF WS-INQ-LEN > 0
              MOVE LK-COMMAREA(1:WS-INQ-LEN) TO WS-INQ(1:WS-INQ-LEN)
           END-IF
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
           END-IF
           IF WS-INQ-LEN > 0
              MOVE WS-INQ(1:WS-INQ-LEN) TO LK-COMMAREA(1:WS-INQ-LEN)
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
