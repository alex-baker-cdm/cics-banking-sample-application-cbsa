      ******************************************************************
      * CICSLINK - test double for EXEC CICS LINK PROGRAM(..) COMMAREA(..)
      *
      * cicsPreprocessor.py now passes the COMMAREA length too:
      *
      *   CALL 'CICSLINK' USING BY CONTENT  prog(8) len(9(9)COMP-5)
      *                         BY REFERENCE commarea
      *
      * so the COMMAREA can be handled generically regardless of its
      * layout. Two behaviours:
      *
      *   * INQCUST - INQACCCU LINKs INQCUST on its normal path to validate
      *     a customer. Rather than pull in the real INQCUST (+ its Db2
      *     deps) this stub maps the COMMAREA to the INQCUST layout and
      *     returns a valid ("found") customer. CBSA_TEST_INQCUST_SUCCESS=N
      *     forces the not-found path.
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
       01 WS-ENV-SUCCESS          PIC X(4) VALUE SPACES.
       01 WS-INQ.
          COPY INQCUST.
       01 WS-INQ-LEN              PIC 9(9) COMP-5.

       LINKAGE SECTION.
       01 LK-PROGRAM              PIC X(8).
       01 LK-LEN                  PIC 9(9) COMP-5.
       01 LK-COMMAREA             PIC X(4096).

       PROCEDURE DIVISION USING LK-PROGRAM LK-LEN LK-COMMAREA.
       A010.
           IF LK-PROGRAM = 'INQCUST '
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
              END-IF
           ELSE
              CALL 'LINKREC' USING 'CALL    '
                   LK-PROGRAM LK-LEN LK-COMMAREA
           END-IF
           GOBACK.
