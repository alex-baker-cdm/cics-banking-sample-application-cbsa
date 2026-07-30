      ******************************************************************
      * crdtagy5Test - isolated unit test for CRDTAGY5 (dummy credit
      * agency #5) running off-CICS through the GnuCOBOL shim harness.
      *
      * CRDTAGY5 is byte-for-byte near-identical to CRDTAGY1; the only
      * behavioural difference is the container name it reads/writes:
      * CIPE (CRDTAGY1 uses CIPA) on the same channel CIPCREDCHANN.
      * See tests/harness/README.md ("CRDTAGY family") for the full list
      * of byte-level differences between the five agencies.
      *
      * Strategy: seed the CIPE container via CICSCONT, invoke CRDTAGY5
      * (GET container -> compute score -> PUT container), then GET the
      * container back to inspect the score. CRDTAGY5 seeds RANDOM from
      * EIBTASKN, pinned by CBSA_TEST_TASKN, so the result is
      * deterministic; the EXEC CICS DELAY is a no-op under test.
      *
      * Assertions:
      *   1. happy path : 1 <= score           (lower boundary)
      *   2. boundary   : score <= 999          (upper boundary)
      *   3. determinism: two runs, same seed -> identical score
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. CRDTAGY5TEST.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
      * Container layout - must match WS-CONT-IN inside CRDTAGY5.
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

       01 WS-CONTAINER-NAME             PIC X(16) VALUE 'CIPE'.
       01 WS-CHANNEL-NAME               PIC X(16) VALUE 'CIPCREDCHANN'.
       01 WS-LEN                        PIC S9(8) COMP VALUE 0.
       01 WS-RESP                       PIC S9(8) COMP VALUE 0.
       01 WS-RESP2                      PIC S9(8) COMP VALUE 0.

       01 WS-SCORE-1                    PIC 999 VALUE 0.
       01 WS-SCORE-2                    PIC 999 VALUE 0.
       01 WS-FAILURES                   PIC 9(4) VALUE 0.

       PROCEDURE DIVISION.
       MAIN-A.
           DISPLAY "=== crdtagy5Test : CRDTAGY5 (credit agency) ==="

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
              DISPLAY "RESULT: crdtagy5Test PASSED"
              MOVE 0 TO RETURN-CODE
           ELSE
              DISPLAY "RESULT: crdtagy5Test FAILED (" WS-FAILURES ")"
              MOVE 1 TO RETURN-CODE
           END-IF

           STOP RUN.

      * Seed the input container, run CRDTAGY5, read the result back.
       RUN-ONCE.
           INITIALIZE WS-CONT-IN
           MOVE "CIPE"       TO WS-CONT-IN-EYECATCHER
           MOVE 987654       TO WS-CONT-IN-SORTCODE
           MOVE 1234567890   TO WS-CONT-IN-NUMBER
           MOVE 0            TO WS-CONT-IN-CREDIT-SCORE
           COMPUTE WS-LEN = LENGTH OF WS-CONT-IN

           CALL 'CICSCONT' USING BY CONTENT 'PUT'
                BY REFERENCE WS-CONTAINER-NAME WS-CHANNEL-NAME
                BY REFERENCE WS-CONT-IN WS-LEN WS-RESP WS-RESP2

           CALL 'CRDTAGY5'

           COMPUTE WS-LEN = LENGTH OF WS-CONT-IN
           CALL 'CICSCONT' USING BY CONTENT 'GET'
                BY REFERENCE WS-CONTAINER-NAME WS-CHANNEL-NAME
                BY REFERENCE WS-CONT-IN WS-LEN WS-RESP WS-RESP2.
