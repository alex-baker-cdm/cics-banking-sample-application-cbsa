      ******************************************************************
      * CICSSYNC - test double for EXEC CICS SYNCPOINT [ROLLBACK].
      *
      * Off-CICS there is no recoverable resource to commit or back out,
      * so the syncpoint is a no-op that always reports NORMAL. Only
      * reached on the VSAM RLS storm-drain error path in INQCUST, which
      * the unit tests do not exercise.
      *
      * Called (via cicsPreprocessor.py) as:
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
