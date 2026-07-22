      ******************************************************************
      * CICSINIT - populate the EIB shim before program logic runs.
      *
      * Injected by cicsPreprocessor.py as the first statement of a
      * translated program. Makes the task number (and therefore any
      * RANDOM seed derived from EIBTASKN) injectable and deterministic
      * via the CBSA_TEST_TASKN environment variable.
      *
      * For the presentation (BMS) programs it also seeds EIBAID and
      * EIBCALEN from the resident CICSAID holder, so a test driver can
      * simulate which key was pressed and whether a COMMAREA was passed.
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. CICSINIT.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 WS-ENV-TASKN            PIC X(16)  VALUE SPACES.
       01 WS-TASKN-NUM            PIC 9(7)   VALUE 0.
       01 WS-AID                  PIC X       VALUE QUOTE.
       01 WS-CALEN                PIC S9(4) COMP VALUE 0.

       LINKAGE SECTION.
       COPY DFHEIBLK.

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

      *    Seed the attention id / commarea length from the resident
      *    holder the driver populated (defaults: ENTER, length 0).
           CALL 'CICSAID' USING 'GET' WS-AID WS-CALEN
           MOVE WS-AID   TO EIBAID
           MOVE WS-CALEN TO EIBCALEN

           GOBACK.
