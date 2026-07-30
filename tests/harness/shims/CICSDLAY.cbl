      ******************************************************************
      * CICSDLAY - test double for EXEC CICS DELAY.
      *
      * Under test the delay is a no-op (returns immediately) so tests
      * run fast and deterministically. Set CBSA_TEST_DELAY_MODE=real to
      * actually sleep for the requested number of seconds when a genuine
      * wait is wanted. The RESP/RESP2 outputs are always set to NORMAL.
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. CICSDLAY.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 WS-MODE                 PIC X(8)   VALUE SPACES.
       01 WS-SECS-DISP            PIC 9(8)   VALUE 0.
       01 WS-CMD                  PIC X(32)  VALUE SPACES.

       LINKAGE SECTION.
       01 LK-SECONDS              PIC S9(8) COMP.
       01 LK-RESP                 PIC S9(8) COMP.
       01 LK-RESP2                PIC S9(8) COMP.

       PROCEDURE DIVISION USING LK-SECONDS
                                OPTIONAL LK-RESP OPTIONAL LK-RESP2.
       A010.
           IF LK-RESP IS NOT OMITTED
              MOVE 0 TO LK-RESP
           END-IF
           IF LK-RESP2 IS NOT OMITTED
              MOVE 0 TO LK-RESP2
           END-IF

           MOVE SPACES TO WS-MODE
           ACCEPT WS-MODE FROM ENVIRONMENT "CBSA_TEST_DELAY_MODE"
           END-ACCEPT

           IF WS-MODE = "real" AND LK-SECONDS > 0
      *       STRING needs display text, not the raw binary COMP bytes,
      *       so convert the seconds to a zoned-decimal field first.
              MOVE LK-SECONDS TO WS-SECS-DISP
              MOVE SPACES TO WS-CMD
              STRING "sleep " DELIMITED BY SIZE
                     WS-SECS-DISP DELIMITED BY SIZE
                     INTO WS-CMD
              END-STRING
              CALL "SYSTEM" USING WS-CMD
           END-IF

           GOBACK.
