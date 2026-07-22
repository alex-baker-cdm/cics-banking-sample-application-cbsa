      ******************************************************************
      * abndprocTest - isolated unit test for ABNDPROC (the centralised
      * abend recorder) running off-CICS through the GnuCOBOL shim
      * harness.
      *
      * ABNDPROC takes an ABNDINFO COMMAREA and EXEC CICS WRITEs it to the
      * ABNDFILE VSAM KSDS (12-byte key = ABND-VSAM-KEY). The generalised
      * CICSVSAM double now serves ABNDFILE alongside CUSTOMER: the driver
      * registers ABNDFILE's geometry once via the DEFFILE control op
      * (key at col 1, 12 bytes; 681-byte record), drives ABNDPROC, then
      * READs the record back out of the very same store for assertions.
      *
      * Assertions:
      *   1. happy path : after ABNDPROC, a record exists in ABNDFILE
      *                   under the expected 12-byte key (RESP = NORMAL)
      *   2. happy path : the stored record's content matches the COMMAREA
      *                   ABNDPROC was given (key + code + program +
      *                   applid + freeform)
      *   3. error path : a scripted non-NORMAL RESP on the WRITE makes
      *                   ABNDPROC take its failure branch and NO record
      *                   is persisted (read-back -> NOTFND)
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. ABNDPROCTEST.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
      * COMMAREA passed to ABNDPROC - byte-identical to its DFHCOMMAREA.
       01 WS-COMMAREA.
          COPY ABNDINFO.

      * A second ABNDINFO-shaped buffer for reading the record back.
       01 WS-READBACK.
          COPY ABNDINFO.

       01 WS-ABND-FILE                PIC X(8)  VALUE 'ABNDFILE'.
      * DEFFILE geometry: key offset(4) + key length(4) + rec length(4).
      * ABNDFILE: key at col 1, 12 bytes, 681-byte record.
       01 WS-ABND-GEOM               PIC X(16) VALUE '000100120681'.

       01 WS-RESP                     PIC S9(8) COMP VALUE 0.
       01 WS-RESP2                    PIC S9(8) COMP VALUE 0.
       01 WS-FORCE                    PIC S9(8) COMP VALUE 0.
       01 WS-FAILURES                 PIC 9(4) VALUE 0.

       PROCEDURE DIVISION.
       MAIN-A.
           DISPLAY "=== abndprocTest : ABNDPROC (abend recorder) ==="

           PERFORM DEFINE-FILE
           PERFORM TEST-HAPPY-PATH
           PERFORM TEST-WRITE-ERROR

           IF WS-FAILURES = 0
              DISPLAY "RESULT: abndprocTest PASSED"
              MOVE 0 TO RETURN-CODE
           ELSE
              DISPLAY "RESULT: abndprocTest FAILED (" WS-FAILURES ")"
              MOVE 1 TO RETURN-CODE
           END-IF

           STOP RUN.

      *-----------------------------------------------------------------
       TEST-HAPPY-PATH.
           PERFORM RESET-STORE
           PERFORM BUILD-COMMAREA

           CALL 'ABNDPROC' USING WS-COMMAREA

           PERFORM READ-BACK

           IF WS-RESP = 0
              DISPLAY "PASS: record written to ABNDFILE under its key"
           ELSE
              DISPLAY "FAIL: no ABNDFILE record found, RESP=" WS-RESP
              ADD 1 TO WS-FAILURES
           END-IF

           IF WS-RESP = 0
              AND ABND-TASKNO-KEY OF WS-READBACK
                  = ABND-TASKNO-KEY OF WS-COMMAREA
              AND ABND-CODE OF WS-READBACK = ABND-CODE OF WS-COMMAREA
              AND ABND-PROGRAM OF WS-READBACK
                  = ABND-PROGRAM OF WS-COMMAREA
              AND ABND-APPLID OF WS-READBACK
                  = ABND-APPLID OF WS-COMMAREA
              AND ABND-FREEFORM OF WS-READBACK
                  = ABND-FREEFORM OF WS-COMMAREA
              DISPLAY "PASS: stored record matches the COMMAREA content"
           ELSE
              DISPLAY "FAIL: stored record does not match COMMAREA:"
              DISPLAY "   code=[" ABND-CODE OF WS-READBACK
                      "] program=[" ABND-PROGRAM OF WS-READBACK "]"
              ADD 1 TO WS-FAILURES
           END-IF.

      *-----------------------------------------------------------------
      * Force the WRITE to return a non-NORMAL RESP; ABNDPROC then takes
      * its "unable to write" branch and RETURNs without persisting.
       TEST-WRITE-ERROR.
           PERFORM RESET-STORE
           PERFORM BUILD-COMMAREA

           MOVE 12 TO WS-FORCE
           CALL 'CICSVSAM' USING
                BY CONTENT 'FORCERSP'
                BY CONTENT WS-ABND-FILE
                BY REFERENCE OMITTED
                BY REFERENCE OMITTED
                BY REFERENCE WS-FORCE
                BY REFERENCE WS-RESP2

           CALL 'ABNDPROC' USING WS-COMMAREA

           PERFORM READ-BACK
           IF WS-RESP NOT = 0
              DISPLAY "PASS: scripted write error -> no record kept"
           ELSE
              DISPLAY "FAIL: record persisted despite scripted error"
              ADD 1 TO WS-FAILURES
           END-IF.

      *-----------------------------------------------------------------
      * Helpers
      *-----------------------------------------------------------------
       DEFINE-FILE.
           CALL 'CICSVSAM' USING
                BY CONTENT 'DEFFILE '
                BY CONTENT WS-ABND-FILE
                BY REFERENCE WS-ABND-GEOM
                BY REFERENCE OMITTED
                BY REFERENCE WS-RESP
                BY REFERENCE WS-RESP2.

       BUILD-COMMAREA.
           INITIALIZE WS-COMMAREA
           MOVE 123456789012345 TO ABND-UTIME-KEY  OF WS-COMMAREA
           MOVE 42              TO ABND-TASKNO-KEY OF WS-COMMAREA
           MOVE 'CBSAAPPL'      TO ABND-APPLID     OF WS-COMMAREA
           MOVE 'OABC'          TO ABND-TRANID     OF WS-COMMAREA
           MOVE '22.07.2026'    TO ABND-DATE       OF WS-COMMAREA
           MOVE '18:00:00'      TO ABND-TIME       OF WS-COMMAREA
           MOVE 'PLOP'          TO ABND-CODE       OF WS-COMMAREA
           MOVE 'CRDTAGY1'      TO ABND-PROGRAM    OF WS-COMMAREA
           MOVE +17             TO ABND-RESPCODE   OF WS-COMMAREA
           MOVE +99             TO ABND-RESP2CODE  OF WS-COMMAREA
           MOVE ZERO            TO ABND-SQLCODE    OF WS-COMMAREA
           MOVE 'A010 - *** The delay messed up! *** test'
                                TO ABND-FREEFORM   OF WS-COMMAREA.

      *    Read the record back using the same 12-byte key ABNDPROC wrote.
       READ-BACK.
           INITIALIZE WS-READBACK
           CALL 'CICSVSAM' USING
                BY CONTENT 'READ    '
                BY CONTENT WS-ABND-FILE
                BY REFERENCE ABND-VSAM-KEY OF WS-COMMAREA
                BY REFERENCE WS-READBACK
                BY REFERENCE WS-RESP
                BY REFERENCE WS-RESP2.

       RESET-STORE.
           CALL 'CICSVSAM' USING
                BY CONTENT 'RESET   '
                BY CONTENT WS-ABND-FILE
                BY REFERENCE OMITTED
                BY REFERENCE OMITTED
                BY REFERENCE WS-RESP
                BY REFERENCE WS-RESP2.
