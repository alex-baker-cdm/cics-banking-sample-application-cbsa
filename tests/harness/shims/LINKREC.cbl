      ******************************************************************
      * LINKREC - resident capture/scripting store behind CICSLINK.
      *
      * Lets a test drive the single business-program LINK a presentation
      * program makes: the driver SCRIPTs the COMMAREA the linked program
      * should "return", and after invoking the screen it reads back the
      * COMMAREA the screen actually passed in (GET). CICSLINK forwards
      * every non-INQCUST LINK here (CALL op): it captures the inbound
      * COMMAREA and, if a response was scripted for that program, copies
      * it back over the COMMAREA.
      *
      *   CALL 'LINKREC' USING BY CONTENT  op(8)
      *                        BY REFERENCE prog(8) len(9(9)COMP-5) data
      *
      *   CALL   - CICSLINK: capture inbound COMMAREA + apply scripted reply.
      *   SCRIPT - driver: register the reply COMMAREA for ``prog``.
      *   GET    - driver: read back the captured program/len/COMMAREA.
      *   RESET  - driver: clear captured + scripted state.
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. LINKREC.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 WS-CAP-PROG             PIC X(8)        VALUE SPACES.
       01 WS-CAP-LEN              PIC 9(9) COMP-5 VALUE 0.
       01 WS-CAP-DATA             PIC X(4096).
       01 WS-CAP-COUNT            PIC 9(4) COMP-5 VALUE 0.

       01 WS-SCR-FLAG             PIC X           VALUE 'N'.
       01 WS-SCR-PROG             PIC X(8)        VALUE SPACES.
       01 WS-SCR-LEN              PIC 9(9) COMP-5 VALUE 0.
       01 WS-SCR-DATA             PIC X(4096).

       LINKAGE SECTION.
       01 LK-OP                   PIC X(8).
       01 LK-PROG                 PIC X(8).
       01 LK-LEN                  PIC 9(9) COMP-5.
       01 LK-DATA                 PIC X(4096).

       PROCEDURE DIVISION USING LK-OP LK-PROG LK-LEN LK-DATA.
       A010.
           EVALUATE LK-OP
              WHEN 'CALL    '
                 ADD 1 TO WS-CAP-COUNT
                 MOVE LK-PROG TO WS-CAP-PROG
                 MOVE LK-LEN  TO WS-CAP-LEN
                 IF LK-LEN > 0
                    MOVE LK-DATA(1:LK-LEN) TO WS-CAP-DATA(1:LK-LEN)
                 END-IF
                 IF WS-SCR-FLAG = 'Y' AND WS-SCR-PROG = LK-PROG
                    IF LK-LEN > 0
                       MOVE WS-SCR-DATA(1:LK-LEN) TO LK-DATA(1:LK-LEN)
                    END-IF
                 END-IF

              WHEN 'SCRIPT  '
                 MOVE 'Y'     TO WS-SCR-FLAG
                 MOVE LK-PROG TO WS-SCR-PROG
                 MOVE LK-LEN  TO WS-SCR-LEN
                 IF LK-LEN > 0
                    MOVE LK-DATA(1:LK-LEN) TO WS-SCR-DATA(1:LK-LEN)
                 END-IF

              WHEN 'GET     '
                 MOVE WS-CAP-PROG TO LK-PROG
                 MOVE WS-CAP-LEN  TO LK-LEN
                 IF WS-CAP-LEN > 0
                    MOVE WS-CAP-DATA(1:LK-LEN) TO LK-DATA(1:LK-LEN)
                 END-IF

              WHEN 'RESET   '
                 MOVE 'N'     TO WS-SCR-FLAG
                 MOVE SPACES  TO WS-SCR-PROG
                 MOVE 0       TO WS-SCR-LEN
                 MOVE SPACES  TO WS-CAP-PROG
                 MOVE 0       TO WS-CAP-LEN
                 MOVE 0       TO WS-CAP-COUNT

              WHEN OTHER
                 CONTINUE
           END-EVALUATE
           GOBACK.
