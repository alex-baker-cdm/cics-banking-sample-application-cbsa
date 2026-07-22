      ******************************************************************
      * CICSTIME - test double for EXEC CICS ASKTIME ABSTIME(..).
      *
      * Returns a fixed, deterministic ABSTIME so time-dependent paths
      * are reproducible under test.
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. CICSTIME.

       DATA DIVISION.
       LINKAGE SECTION.
       01 LK-ABSTIME             PIC S9(15) COMP-3.

       PROCEDURE DIVISION USING LK-ABSTIME.
       A010.
           MOVE 0 TO LK-ABSTIME
           GOBACK.
