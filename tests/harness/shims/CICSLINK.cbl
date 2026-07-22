      ******************************************************************
      * CICSLINK - test double for EXEC CICS LINK PROGRAM(..) COMMAREA(..)
      *
      * The Layer-0 programs only LINK on an error path (to ABNDPROC),
      * which the required happy-path tests do not exercise. This stub
      * records the invocation so a test could assert it was reached,
      * without pulling in the linked program's CICS/SQL dependencies.
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. CICSLINK.

       DATA DIVISION.
       LINKAGE SECTION.
       01 LK-PROGRAM              PIC X(8).
       01 LK-COMMAREA             PIC X(32768).

       PROCEDURE DIVISION USING LK-PROGRAM LK-COMMAREA.
       A010.
           DISPLAY "CICSLINK (stub): LINK to " LK-PROGRAM
           GOBACK.
