      ******************************************************************
      * CICSCONT - test double for EXEC CICS GET/PUT CONTAINER.
      *
      * A stateful, process-resident container store keyed by container
      * name. Because GnuCOBOL keeps a called module resident, the same
      * store is shared between the program under test and the test
      * driver, so a driver can PUT an input container, invoke the
      * program (which GETs it, updates it and PUTs it back), then GET
      * the result to make assertions.
      *
      * Called (via the preprocessor) as:
      *   CALL 'CICSCONT' USING BY CONTENT 'GET'|'PUT'
      *        BY REFERENCE name channel data flength resp resp2
      *
      * resp = 0 on success; resp = 1 (CONTAINERERR) if a GET misses.
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. CICSCONT.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 WS-STORE.
          05 WS-SLOT OCCURS 16 TIMES INDEXED BY WS-IX.
             10 WS-SLOT-USED     PIC X       VALUE 'N'.
             10 WS-SLOT-NAME     PIC X(16)   VALUE SPACES.
             10 WS-SLOT-LEN      PIC 9(8)    VALUE 0.
             10 WS-SLOT-DATA     PIC X(32768) VALUE SPACES.
       01 WS-FOUND               PIC X       VALUE 'N'.

       LINKAGE SECTION.
       01 LK-OP                  PIC X(3).
       01 LK-NAME                PIC X(16).
       01 LK-CHANNEL             PIC X(16).
       01 LK-DATA                PIC X(32768).
       01 LK-FLEN                PIC S9(8) COMP.
       01 LK-RESP                PIC S9(8) COMP.
       01 LK-RESP2               PIC S9(8) COMP.

       PROCEDURE DIVISION USING LK-OP LK-NAME LK-CHANNEL LK-DATA
                                LK-FLEN LK-RESP LK-RESP2.
       A010.
           MOVE 0 TO LK-RESP
           MOVE 0 TO LK-RESP2
           MOVE 'N' TO WS-FOUND

           EVALUATE LK-OP
              WHEN 'PUT'
                 PERFORM DO-PUT
              WHEN 'GET'
                 PERFORM DO-GET
              WHEN OTHER
                 MOVE 1 TO LK-RESP
           END-EVALUATE

           GOBACK.

       DO-PUT.
      *    Reuse an existing slot for this name, else claim a free one.
           PERFORM VARYING WS-IX FROM 1 BY 1 UNTIL WS-IX > 16
              IF WS-SLOT-USED(WS-IX) = 'Y'
                 AND WS-SLOT-NAME(WS-IX) = LK-NAME
                 MOVE 'Y' TO WS-FOUND
                 PERFORM STORE-SLOT
                 EXIT PERFORM
              END-IF
           END-PERFORM
           IF WS-FOUND = 'N'
              PERFORM VARYING WS-IX FROM 1 BY 1 UNTIL WS-IX > 16
                 IF WS-SLOT-USED(WS-IX) = 'N'
                    MOVE 'Y' TO WS-FOUND
                    PERFORM STORE-SLOT
                    EXIT PERFORM
                 END-IF
              END-PERFORM
           END-IF
           IF WS-FOUND = 'N'
              MOVE 1 TO LK-RESP
           END-IF.

       STORE-SLOT.
           MOVE 'Y'     TO WS-SLOT-USED(WS-IX)
           MOVE LK-NAME TO WS-SLOT-NAME(WS-IX)
           MOVE LK-FLEN TO WS-SLOT-LEN(WS-IX)
           MOVE LK-DATA(1:LK-FLEN) TO WS-SLOT-DATA(WS-IX)(1:LK-FLEN).

       DO-GET.
           PERFORM VARYING WS-IX FROM 1 BY 1 UNTIL WS-IX > 16
              IF WS-SLOT-USED(WS-IX) = 'Y'
                 AND WS-SLOT-NAME(WS-IX) = LK-NAME
                 MOVE 'Y' TO WS-FOUND
                 MOVE WS-SLOT-DATA(WS-IX)(1:LK-FLEN)
                                        TO LK-DATA(1:LK-FLEN)
                 EXIT PERFORM
              END-IF
           END-PERFORM
           IF WS-FOUND = 'N'
              MOVE 1 TO LK-RESP
           END-IF.
