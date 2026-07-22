      ******************************************************************
      * CICSVSAM - test double for the EXEC CICS file-control (VSAM KSDS)
      * verbs used by the customer read/update programs.
      *
      * A process-resident, keyed in-memory KSDS store (mirrors the
      * CICSCONT container-store pattern). Because GnuCOBOL keeps a
      * called module resident for the life of the run unit, the same
      * store is shared between the test driver (which SEEDs fixture
      * records) and the program under test (which READs / REWRITEs /
      * browses it).
      *
      * Called (via cicsPreprocessor.py) as:
      *   CALL 'CICSVSAM' USING BY CONTENT  op(8) file(8)
      *        BY REFERENCE ridfld|OMITTED record|OMITTED resp resp2
      *
      * Operations (op, 8 chars, space padded):
      *   File-control verbs routed by the preprocessor -
      *     READ     keyed read (RIDFLD -> record)
      *     RDUPD    keyed READ ... UPDATE (remembers the key for REWRITE)
      *     REWRITE  overwrite the record last read for update
      *     WRITE    insert a new record (keyed by RIDFLD)
      *     STARTBR  open a browse positioned at/around RIDFLD
      *     READNEXT next record in ascending key order
      *     READPREV previous record in descending key order
      *     ENDBR    close the browse
      *   Driver-only control ops (called directly from the test driver) -
      *     RESET    empty the store and clear all state
      *     SEED     insert/replace a fixture record (key taken from the
      *              record image, cols 5-20 = CUSTOMER-KEY)
      *     FORCERSP script the RESP returned by the NEXT file verb (the
      *              desired value is passed in via the resp operand) so a
      *              test can drive an arbitrary error path
      *
      * RESP values follow the real CICS/VSAM conditions so the programs'
      * control flow is exercised unchanged: NORMAL=0, NOTFND=13,
      * DUPREC=14, ENDFILE=20.
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. CICSVSAM.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 WS-RESP-NORMAL          PIC S9(8) COMP VALUE 0.
       01 WS-RESP-NOTFND          PIC S9(8) COMP VALUE 13.
       01 WS-RESP-DUPREC          PIC S9(8) COMP VALUE 14.
       01 WS-RESP-ENDFILE         PIC S9(8) COMP VALUE 20.

       01 WS-STORE.
          05 WS-COUNT             PIC 9(4)     VALUE 0.
          05 WS-SLOT OCCURS 64 TIMES INDEXED BY WS-IX.
             10 WS-SLOT-USED      PIC X        VALUE 'N'.
             10 WS-SLOT-KEY       PIC X(16)    VALUE SPACES.
             10 WS-SLOT-DATA      PIC X(259)   VALUE SPACES.

       01 WS-BROWSE.
          05 WS-BR-ACTIVE         PIC X        VALUE 'N'.
          05 WS-BR-FRESH          PIC X        VALUE 'N'.
          05 WS-BR-START          PIC X(16)    VALUE SPACES.
          05 WS-BR-LAST           PIC X(16)    VALUE SPACES.

       01 WS-UPD-KEY              PIC X(16)    VALUE SPACES.
       01 WS-UPD-SET             PIC X        VALUE 'N'.

       01 WS-FORCED-RESP         PIC S9(8) COMP VALUE -1.

       01 WS-BEST-IX             PIC 9(4)     VALUE 0.
       01 WS-HIT-IX              PIC 9(4)     VALUE 0.
       01 WS-BOUND               PIC X(16)    VALUE SPACES.
       01 WS-INCLUSIVE           PIC X        VALUE 'N'.

       LINKAGE SECTION.
       01 LK-OP                  PIC X(8).
       01 LK-FILE                PIC X(8).
       01 LK-RID                 PIC X(16).
       01 LK-REC                 PIC X(259).
       01 LK-RESP                PIC S9(8) COMP.
       01 LK-RESP2               PIC S9(8) COMP.

       PROCEDURE DIVISION USING LK-OP LK-FILE
                                OPTIONAL LK-RID OPTIONAL LK-REC
                                LK-RESP LK-RESP2.
       A010.
      *    Driver-only control ops never set RESP through the normal path.
           EVALUATE LK-OP
              WHEN 'RESET   '
                 PERFORM DO-RESET
                 GOBACK
              WHEN 'SEED    '
                 PERFORM DO-SEED
                 GOBACK
              WHEN 'FORCERSP'
                 MOVE LK-RESP TO WS-FORCED-RESP
                 MOVE 0 TO LK-RESP
                 MOVE 0 TO LK-RESP2
                 GOBACK
           END-EVALUATE

           MOVE 0 TO LK-RESP2

      *    A scripted RESP overrides the next file verb exactly once.
           IF WS-FORCED-RESP >= 0
              MOVE WS-FORCED-RESP TO LK-RESP
              MOVE -1 TO WS-FORCED-RESP
              GOBACK
           END-IF

           MOVE WS-RESP-NORMAL TO LK-RESP

           EVALUATE LK-OP
              WHEN 'READ    '
                 PERFORM DO-READ
              WHEN 'RDUPD   '
                 PERFORM DO-READ-UPDATE
              WHEN 'REWRITE '
                 PERFORM DO-REWRITE
              WHEN 'WRITE   '
                 PERFORM DO-WRITE
              WHEN 'STARTBR '
                 PERFORM DO-STARTBR
              WHEN 'READNEXT'
                 PERFORM DO-READNEXT
              WHEN 'READPREV'
                 PERFORM DO-READPREV
              WHEN 'ENDBR   '
                 PERFORM DO-ENDBR
              WHEN OTHER
                 MOVE WS-RESP-NOTFND TO LK-RESP
           END-EVALUATE

           GOBACK.

      *-----------------------------------------------------------------
       DO-RESET.
           MOVE 0 TO WS-COUNT
           PERFORM VARYING WS-IX FROM 1 BY 1 UNTIL WS-IX > 64
              MOVE 'N'    TO WS-SLOT-USED(WS-IX)
              MOVE SPACES TO WS-SLOT-KEY(WS-IX)
              MOVE SPACES TO WS-SLOT-DATA(WS-IX)
           END-PERFORM
           MOVE 'N' TO WS-BR-ACTIVE
           MOVE 'N' TO WS-BR-FRESH
           MOVE 'N' TO WS-UPD-SET
           MOVE -1  TO WS-FORCED-RESP
           MOVE 0 TO LK-RESP
           MOVE 0 TO LK-RESP2.

      *    SEED derives the key from the record image (CUSTOMER-KEY sits
      *    at cols 5-20, right after the 4-byte eyecatcher).
       DO-SEED.
           MOVE 0 TO LK-RESP
           MOVE 0 TO LK-RESP2
           MOVE LK-REC(5:16) TO WS-BOUND
           PERFORM FIND-BY-KEY
           IF WS-HIT-IX > 0
              MOVE LK-REC TO WS-SLOT-DATA(WS-HIT-IX)
           ELSE
              PERFORM CLAIM-FREE-SLOT
              IF WS-HIT-IX > 0
                 MOVE 'Y'          TO WS-SLOT-USED(WS-HIT-IX)
                 MOVE LK-REC(5:16) TO WS-SLOT-KEY(WS-HIT-IX)
                 MOVE LK-REC       TO WS-SLOT-DATA(WS-HIT-IX)
                 ADD 1 TO WS-COUNT
              ELSE
                 MOVE 1 TO LK-RESP
              END-IF
           END-IF.

      *-----------------------------------------------------------------
       DO-READ.
           MOVE LK-RID TO WS-BOUND
           PERFORM FIND-BY-KEY
           IF WS-HIT-IX > 0
              MOVE WS-SLOT-DATA(WS-HIT-IX) TO LK-REC
           ELSE
              MOVE WS-RESP-NOTFND TO LK-RESP
           END-IF.

       DO-READ-UPDATE.
           PERFORM DO-READ
           IF LK-RESP = WS-RESP-NORMAL
              MOVE LK-RID TO WS-UPD-KEY
              MOVE 'Y'    TO WS-UPD-SET
           END-IF.

       DO-REWRITE.
           IF WS-UPD-SET NOT = 'Y'
              MOVE WS-RESP-NOTFND TO LK-RESP
           ELSE
              MOVE WS-UPD-KEY TO WS-BOUND
              PERFORM FIND-BY-KEY
              IF WS-HIT-IX > 0
                 MOVE LK-REC TO WS-SLOT-DATA(WS-HIT-IX)
              ELSE
                 MOVE WS-RESP-NOTFND TO LK-RESP
              END-IF
              MOVE 'N' TO WS-UPD-SET
           END-IF.

       DO-WRITE.
           MOVE LK-RID TO WS-BOUND
           PERFORM FIND-BY-KEY
           IF WS-HIT-IX > 0
              MOVE WS-RESP-DUPREC TO LK-RESP
           ELSE
              PERFORM CLAIM-FREE-SLOT
              IF WS-HIT-IX > 0
                 MOVE 'Y'    TO WS-SLOT-USED(WS-HIT-IX)
                 MOVE LK-RID TO WS-SLOT-KEY(WS-HIT-IX)
                 MOVE LK-REC TO WS-SLOT-DATA(WS-HIT-IX)
                 ADD 1 TO WS-COUNT
              ELSE
                 MOVE 1 TO LK-RESP
              END-IF
           END-IF.

      *-----------------------------------------------------------------
       DO-STARTBR.
           IF WS-COUNT = 0
              MOVE WS-RESP-NOTFND TO LK-RESP
           ELSE
              MOVE 'Y'    TO WS-BR-ACTIVE
              MOVE 'Y'    TO WS-BR-FRESH
              MOVE LK-RID TO WS-BR-START
              MOVE SPACES TO WS-BR-LAST
           END-IF.

       DO-READNEXT.
           IF WS-BR-FRESH = 'Y'
              MOVE WS-BR-START TO WS-BOUND
              MOVE 'Y'         TO WS-INCLUSIVE
           ELSE
              MOVE WS-BR-LAST  TO WS-BOUND
              MOVE 'N'         TO WS-INCLUSIVE
           END-IF
           PERFORM FIND-NEXT
           IF WS-BEST-IX > 0
              MOVE 'N'                      TO WS-BR-FRESH
              MOVE WS-SLOT-KEY(WS-BEST-IX)  TO WS-BR-LAST
              MOVE WS-SLOT-KEY(WS-BEST-IX)  TO LK-RID
              MOVE WS-SLOT-DATA(WS-BEST-IX) TO LK-REC
           ELSE
              MOVE WS-RESP-ENDFILE TO LK-RESP
           END-IF.

       DO-READPREV.
           IF WS-BR-FRESH = 'Y'
              MOVE WS-BR-START TO WS-BOUND
              MOVE 'Y'         TO WS-INCLUSIVE
           ELSE
              MOVE WS-BR-LAST  TO WS-BOUND
              MOVE 'N'         TO WS-INCLUSIVE
           END-IF
           PERFORM FIND-PREV
           IF WS-BEST-IX > 0
              MOVE 'N'                      TO WS-BR-FRESH
              MOVE WS-SLOT-KEY(WS-BEST-IX)  TO WS-BR-LAST
              MOVE WS-SLOT-KEY(WS-BEST-IX)  TO LK-RID
              MOVE WS-SLOT-DATA(WS-BEST-IX) TO LK-REC
           ELSE
              MOVE WS-RESP-ENDFILE TO LK-RESP
           END-IF.

       DO-ENDBR.
           MOVE 'N' TO WS-BR-ACTIVE
           MOVE 'N' TO WS-BR-FRESH.

      *-----------------------------------------------------------------
      * Search helpers. WS-HIT-IX / WS-BEST-IX = 0 mean "no match".
      *-----------------------------------------------------------------
       FIND-BY-KEY.
           MOVE 0 TO WS-HIT-IX
           PERFORM VARYING WS-IX FROM 1 BY 1 UNTIL WS-IX > 64
              IF WS-SLOT-USED(WS-IX) = 'Y'
                 AND WS-SLOT-KEY(WS-IX) = WS-BOUND
                 MOVE WS-IX TO WS-HIT-IX
                 EXIT PERFORM
              END-IF
           END-PERFORM.

       CLAIM-FREE-SLOT.
           MOVE 0 TO WS-HIT-IX
           PERFORM VARYING WS-IX FROM 1 BY 1 UNTIL WS-IX > 64
              IF WS-SLOT-USED(WS-IX) = 'N'
                 MOVE WS-IX TO WS-HIT-IX
                 EXIT PERFORM
              END-IF
           END-PERFORM.

      *    Smallest key that is >= WS-BOUND (inclusive) or > WS-BOUND.
       FIND-NEXT.
           MOVE 0 TO WS-BEST-IX
           PERFORM VARYING WS-IX FROM 1 BY 1 UNTIL WS-IX > 64
              IF WS-SLOT-USED(WS-IX) = 'Y'
                 IF (WS-INCLUSIVE = 'Y'
                       AND WS-SLOT-KEY(WS-IX) >= WS-BOUND)
                    OR (WS-INCLUSIVE = 'N'
                       AND WS-SLOT-KEY(WS-IX) > WS-BOUND)
                    IF WS-BEST-IX = 0
                       OR WS-SLOT-KEY(WS-IX) < WS-SLOT-KEY(WS-BEST-IX)
                       MOVE WS-IX TO WS-BEST-IX
                    END-IF
                 END-IF
              END-IF
           END-PERFORM.

      *    Largest key that is <= WS-BOUND (inclusive) or < WS-BOUND.
       FIND-PREV.
           MOVE 0 TO WS-BEST-IX
           PERFORM VARYING WS-IX FROM 1 BY 1 UNTIL WS-IX > 64
              IF WS-SLOT-USED(WS-IX) = 'Y'
                 IF (WS-INCLUSIVE = 'Y'
                       AND WS-SLOT-KEY(WS-IX) <= WS-BOUND)
                    OR (WS-INCLUSIVE = 'N'
                       AND WS-SLOT-KEY(WS-IX) < WS-BOUND)
                    IF WS-BEST-IX = 0
                       OR WS-SLOT-KEY(WS-IX) > WS-SLOT-KEY(WS-BEST-IX)
                       MOVE WS-IX TO WS-BEST-IX
                    END-IF
                 END-IF
              END-IF
           END-PERFORM.
