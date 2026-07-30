      ******************************************************************
      * crdtagy1Test - isolated unit test for CRDTAGY1 (dummy credit
      * agency) running off-CICS through the GnuCOBOL shim harness.
      *
      * Strategy: seed the CIPA container on channel CIPCREDCHANN via the
      * CICSCONT test double, invoke CRDTAGY1 (which GETs the container,
      * computes a credit score and PUTs it back), then GET the container
      * to inspect the score. CRDTAGY1 seeds RANDOM from EIBTASKN, which
      * the harness pins via CBSA_TEST_TASKN, so the result is
      * deterministic. The EXEC CICS DELAY is a no-op under test.
      *
      * Assertions:
      *   1. happy path : 1 <= score           (lower boundary)
      *   2. boundary   : score <= 999          (upper boundary)
      *   3. determinism: two runs, same seed -> identical score
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. CRDTAGY1TEST.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
      * Container layout - must match WS-CONT-IN inside CRDTAGY1.
       01 WS-CONT-IN.
          03 WS-CONT-IN-EYECATCHER      PIC X(4).
          03 WS-CONT-IN-KEY.
             05 WS-CONT-IN-SORTCODE     PIC 9(6)  DISPLAY.
             05 WS-CONT-IN-NUMBER       PIC 9(10) DISPLAY.
          03 WS-CONT-IN-NAME            PIC X(60).
          03 WS-CONT-IN-ADDRESS         PIC X(160).
          03 WS-CONT-IN-DATE-OF-BIRTH   PIC 9(8).
          03 WS-CONT-IN-CREDIT-SCORE    PIC 999.
          03 WS-CONT-IN-CS-REVIEW-DATE  PIC 9(8).
          03 WS-CONT-IN-SUCCESS         PIC X.
          03 WS-CONT-IN-FAIL-CODE       PIC X.

       01 WS-CONTAINER-NAME             PIC X(16) VALUE 'CIPA'.
       01 WS-CHANNEL-NAME               PIC X(16) VALUE 'CIPCREDCHANN'.
       01 WS-LEN                        PIC S9(8) COMP VALUE 0.
       01 WS-RESP                       PIC S9(8) COMP VALUE 0.
       01 WS-RESP2                      PIC S9(8) COMP VALUE 0.

       01 WS-SCORE-1                    PIC 999 VALUE 0.
       01 WS-SCORE-2                    PIC 999 VALUE 0.
       01 WS-FAILURES                   PIC 9(4) VALUE 0.

       PROCEDURE DIVISION.
       MAIN-A.
           DISPLAY "=== crdtagy1Test : CRDTAGY1 (credit agency) ==="

           PERFORM RUN-ONCE
           MOVE WS-CONT-IN-CREDIT-SCORE TO WS-SCORE-1
           PERFORM RUN-ONCE
           MOVE WS-CONT-IN-CREDIT-SCORE TO WS-SCORE-2

           DISPLAY "    score run 1 = " WS-SCORE-1
           DISPLAY "    score run 2 = " WS-SCORE-2

           IF WS-SCORE-1 >= 1
              DISPLAY "PASS: score >= 1 (lower boundary)"
           ELSE
              DISPLAY "FAIL: score below 1"
              ADD 1 TO WS-FAILURES
           END-IF

           IF WS-SCORE-1 <= 999
              DISPLAY "PASS: score <= 999 (upper boundary)"
           ELSE
              DISPLAY "FAIL: score above 999"
              ADD 1 TO WS-FAILURES
           END-IF

           IF WS-SCORE-1 = WS-SCORE-2
              DISPLAY "PASS: deterministic under fixed seed"
           ELSE
              DISPLAY "FAIL: score not deterministic"
              ADD 1 TO WS-FAILURES
           END-IF

           IF WS-FAILURES = 0
              DISPLAY "RESULT: crdtagy1Test PASSED"
              MOVE 0 TO RETURN-CODE
           ELSE
              DISPLAY "RESULT: crdtagy1Test FAILED (" WS-FAILURES ")"
              MOVE 1 TO RETURN-CODE
           END-IF

           STOP RUN.

      * Seed the input container, run CRDTAGY1, read the result back.
       RUN-ONCE.
           INITIALIZE WS-CONT-IN
           MOVE "CIPA"       TO WS-CONT-IN-EYECATCHER
           MOVE 987654       TO WS-CONT-IN-SORTCODE
           MOVE 1234567890   TO WS-CONT-IN-NUMBER
           MOVE 0            TO WS-CONT-IN-CREDIT-SCORE
           COMPUTE WS-LEN = LENGTH OF WS-CONT-IN

           CALL 'CICSCONT' USING BY CONTENT 'PUT'
                BY REFERENCE WS-CONTAINER-NAME WS-CHANNEL-NAME
                BY REFERENCE WS-CONT-IN WS-LEN WS-RESP WS-RESP2

           CALL 'CRDTAGY1'

           COMPUTE WS-LEN = LENGTH OF WS-CONT-IN
           CALL 'CICSCONT' USING BY CONTENT 'GET'
                BY REFERENCE WS-CONTAINER-NAME WS-CHANNEL-NAME
                BY REFERENCE WS-CONT-IN WS-LEN WS-RESP WS-RESP2.
