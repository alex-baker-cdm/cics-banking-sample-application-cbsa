      ******************************************************************
      * CICSFTIM - test double for EXEC CICS FORMATTIME.
      *
      * Returns fixed, deterministic date/time components so formatting
      * paths are reproducible under test.
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. CICSFTIM.

       DATA DIVISION.
       LINKAGE SECTION.
       01 LK-ABSTIME             PIC S9(15) COMP-3.
       01 LK-DDMMYYYY            PIC X(10).
       01 LK-TIME                PIC 9(6).

       PROCEDURE DIVISION USING LK-ABSTIME LK-DDMMYYYY LK-TIME.
       A010.
           MOVE "01.01.2023" TO LK-DDMMYYYY
           MOVE 120000       TO LK-TIME
           GOBACK.
