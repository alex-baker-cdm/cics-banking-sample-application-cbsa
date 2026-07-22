      ******************************************************************
      * CICSABND - test double for EXEC CICS ABEND, with abend capture.
      *
      * On the mainframe EXEC CICS ABEND terminates the task. Off-CICS the
      * cicsPreprocessor.py translates it to a CALL to this resident
      * module followed by GOBACK, so the program under test stops at the
      * abend point (control does not fall through) and the driver regains
      * control. Because GnuCOBOL keeps the module loaded for the life of
      * the run unit, the last abend code is remembered in working storage
      * and a driver can read / reset it to assert failure paths.
      *
      * Two call shapes (positional):
      *
      *   Abend (emitted by the preprocessor for EXEC CICS ABEND):
      *     CALL 'CICSABND' USING BY CONTENT 'ABEND   ' abcode
      *       - records abcode as the last abend and raises the flag.
      *
      *   Driver control ops:
      *     CALL 'CICSABND' USING BY CONTENT 'READ    '
      *          BY REFERENCE abcode-out flag-out
      *       - returns the last abend code and 'Y'/'N' occurred flag.
      *     CALL 'CICSABND' USING BY CONTENT 'RESET   '
      *          BY REFERENCE abcode-out flag-out
      *       - clears the captured abend (abcode-out/flag-out optional).
      *
      * abcode is 4 bytes (CICS ABEND codes, e.g. 'SAME', 'HROL', 'TO  ').
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. CICSABND.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 WS-LAST-ABCODE         PIC X(4)  VALUE SPACES.
       01 WS-ABEND-FLAG          PIC X     VALUE 'N'.

       LINKAGE SECTION.
       01 LK-OP                  PIC X(8).
       01 LK-ABCODE              PIC X(4).
       01 LK-FLAG                PIC X.

       PROCEDURE DIVISION USING LK-OP LK-ABCODE OPTIONAL LK-FLAG.
       A010.
           EVALUATE LK-OP
              WHEN 'ABEND   '
                 MOVE LK-ABCODE TO WS-LAST-ABCODE
                 MOVE 'Y'       TO WS-ABEND-FLAG
                 DISPLAY "CICSABND: ABEND " WS-LAST-ABCODE
              WHEN 'READ    '
                 MOVE WS-LAST-ABCODE TO LK-ABCODE
                 IF LK-FLAG IS NOT OMITTED
                    MOVE WS-ABEND-FLAG TO LK-FLAG
                 END-IF
              WHEN 'RESET   '
                 MOVE SPACES TO WS-LAST-ABCODE
                 MOVE 'N'    TO WS-ABEND-FLAG
                 MOVE SPACES TO LK-ABCODE
                 IF LK-FLAG IS NOT OMITTED
                    MOVE WS-ABEND-FLAG TO LK-FLAG
                 END-IF
              WHEN OTHER
                 DISPLAY "CICSABND: unknown op [" LK-OP "]"
           END-EVALUATE
           GOBACK.
