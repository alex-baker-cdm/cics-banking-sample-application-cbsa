      ******************************************************************
      * CICSABND - test double for EXEC CICS ABEND ABCODE(..).
      *
      * Records the abend code. Only reached on an error path that the
      * required happy-path tests do not drive.
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. CICSABND.

       DATA DIVISION.
       LINKAGE SECTION.
       01 LK-ABCODE              PIC X(4).

       PROCEDURE DIVISION USING LK-ABCODE.
       A010.
           DISPLAY "CICSABND (stub): ABEND " LK-ABCODE
           GOBACK.
