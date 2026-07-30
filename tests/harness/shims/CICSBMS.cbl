      ******************************************************************
      * CICSBMS - test double for the BMS 3270 terminal (SEND/RECEIVE
      * MAP, SEND CONTROL, SEND TEXT).
      *
      * Models the screen as two in-memory byte buffers held in this
      * resident module: an INPUT image (what the operator "keyed") and
      * an OUTPUT image (what the program last displayed). The uniform
      * signature is:
      *
      *   CALL 'CICSBMS' USING BY CONTENT  op(8) len(9(9)COMP-5)
      *                        BY REFERENCE data resp resp2
      *
      * Program-driven ops (emitted by cicsPreprocessor.py):
      *   SEND     - store ``data`` (len bytes) as the output image.
      *   RECEIVE  - copy the input image into ``data``; return the
      *              scripted RECEIVE resp (default NORMAL=0).
      *   TEXT     - SEND TEXT: no map, just succeed (resp=0).
      *   CONTROL  - SEND CONTROL: no-op, succeed (resp=0).
      *
      * Driver-driven ops:
      *   PUTIN    - load the input image (simulate operator input).
      *   GETOUT   - read back the last output image (assert display).
      *   SETRESP  - script the resp RECEIVE will return (len = value,
      *              e.g. MAPFAIL) for the next RECEIVE.
      *   RESET    - clear both images and the scripted resp.
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. CICSBMS.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 WS-IN-LEN               PIC 9(9) COMP-5 VALUE 0.
       01 WS-IN-DATA              PIC X(4096).
       01 WS-OUT-LEN              PIC 9(9) COMP-5 VALUE 0.
       01 WS-OUT-DATA             PIC X(4096).
       01 WS-RECV-RESP            PIC S9(8) COMP  VALUE 0.

       LINKAGE SECTION.
       01 LK-OP                   PIC X(8).
       01 LK-LEN                  PIC 9(9) COMP-5.
       01 LK-DATA                 PIC X(4096).
       01 LK-RESP                 PIC S9(8) COMP.
       01 LK-RESP2                PIC S9(8) COMP.

       PROCEDURE DIVISION USING LK-OP LK-LEN LK-DATA
                                LK-RESP LK-RESP2.
       A010.
           EVALUATE LK-OP
              WHEN 'SEND    '
                 IF LK-LEN > 0
                    MOVE LK-DATA(1:LK-LEN) TO WS-OUT-DATA(1:LK-LEN)
                    MOVE LK-LEN TO WS-OUT-LEN
                 END-IF
                 MOVE 0 TO LK-RESP
                 MOVE 0 TO LK-RESP2

              WHEN 'RECEIVE '
                 IF WS-IN-LEN > 0 AND LK-LEN > 0
                    MOVE WS-IN-DATA(1:LK-LEN) TO LK-DATA(1:LK-LEN)
                 END-IF
                 MOVE WS-RECV-RESP TO LK-RESP
                 MOVE 0            TO LK-RESP2

              WHEN 'TEXT    '
                 MOVE 0 TO LK-RESP
                 MOVE 0 TO LK-RESP2

              WHEN 'CONTROL '
                 MOVE 0 TO LK-RESP
                 MOVE 0 TO LK-RESP2

              WHEN 'PUTIN   '
                 IF LK-LEN > 0
                    MOVE LK-DATA(1:LK-LEN) TO WS-IN-DATA(1:LK-LEN)
                 END-IF
                 MOVE LK-LEN TO WS-IN-LEN

              WHEN 'GETOUT  '
                 IF WS-OUT-LEN > 0 AND LK-LEN > 0
                    MOVE WS-OUT-DATA(1:LK-LEN) TO LK-DATA(1:LK-LEN)
                 END-IF

              WHEN 'SETRESP '
                 MOVE LK-LEN TO WS-RECV-RESP

              WHEN 'RESET   '
                 MOVE 0 TO WS-IN-LEN
                 MOVE 0 TO WS-OUT-LEN
                 MOVE 0 TO WS-RECV-RESP

              WHEN OTHER
                 CONTINUE
           END-EVALUATE
           GOBACK.
