      ******************************************************************
      * CICSINIT - populate the EIB shim before program logic runs.
      *
      * Injected by cicsPreprocessor.py as the first statement of a
      * translated program. Makes the task number (and therefore any
      * RANDOM seed derived from EIBTASKN) injectable and deterministic
      * via the CBSA_TEST_TASKN environment variable.
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. CICSINIT.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 WS-ENV-TASKN            PIC X(16)  VALUE SPACES.
       01 WS-TASKN-NUM            PIC 9(7)   VALUE 0.

       LINKAGE SECTION.
       01 DFHEIB-SHIM.
          05 EIBTASKN   PIC S9(7) COMP-3.
          05 EIBRESP    PIC S9(8) COMP.
          05 EIBRESP2   PIC S9(8) COMP.
          05 EIBTRNID   PIC X(4).

       PROCEDURE DIVISION USING DFHEIB-SHIM.
       A010.
           MOVE 0      TO EIBRESP
           MOVE 0      TO EIBRESP2
           MOVE SPACES TO EIBTRNID

           MOVE SPACES TO WS-ENV-TASKN
           ACCEPT WS-ENV-TASKN FROM ENVIRONMENT "CBSA_TEST_TASKN"
           END-ACCEPT

           IF WS-ENV-TASKN = SPACES
              MOVE 1 TO EIBTASKN
           ELSE
              MOVE FUNCTION NUMVAL(WS-ENV-TASKN) TO WS-TASKN-NUM
              MOVE WS-TASKN-NUM TO EIBTASKN
           END-IF

           GOBACK.
