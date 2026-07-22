      ******************************************************************
      * CICSLINK - test double for EXEC CICS LINK PROGRAM(..) COMMAREA(..)
      *
      * Most programs LINK only on an error path (to ABNDPROC), which the
      * happy-path tests do not exercise; for those this stub just records
      * the invocation without pulling in the linked program.
      *
      * INQACCCU, however, LINKs INQCUST on its normal path to validate
      * the customer. Rather than pull in the real INQCUST (and its own
      * Db2 dependencies), this stub maps the COMMAREA to the INQCUST
      * layout and returns a valid ("found") customer, so INQACCCU can
      * proceed to read the customer's accounts. A test can force the
      * not-found path with CBSA_TEST_INQCUST_SUCCESS=N.
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. CICSLINK.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 WS-ENV-SUCCESS          PIC X(4) VALUE SPACES.

       LINKAGE SECTION.
       01 LK-PROGRAM              PIC X(8).
       01 LK-COMMAREA.
          COPY INQCUST.

       PROCEDURE DIVISION USING LK-PROGRAM LK-COMMAREA.
       A010.
           IF LK-PROGRAM = 'INQCUST '
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
           ELSE
              DISPLAY "CICSLINK (stub): LINK to " LK-PROGRAM
           END-IF
           GOBACK.
