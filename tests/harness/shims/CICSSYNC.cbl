      ******************************************************************
      * CICSSYNC - test double for EXEC CICS SYNCPOINT [ROLLBACK].
      *
      * The account programs issue SYNCPOINT ROLLBACK only on Db2/VSAM
      * error paths, checking RESP against DFHRESP(NORMAL). Off-CICS the
      * rollback is a no-op; this stub simply reports NORMAL so the
      * program's post-syncpoint control flow behaves deterministically.
      *
      * Called (via the preprocessor) as:
      *   CALL 'CICSSYNC' USING BY REFERENCE resp resp2
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. CICSSYNC.

       DATA DIVISION.
       LINKAGE SECTION.
       01 LK-RESP                 PIC S9(8) COMP.
       01 LK-RESP2                PIC S9(8) COMP.

       PROCEDURE DIVISION USING LK-RESP LK-RESP2.
       A010.
           MOVE 0 TO LK-RESP
           MOVE 0 TO LK-RESP2
           GOBACK.
