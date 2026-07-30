      ******************************************************************
      * CICSVSAM - test double for the EXEC CICS file-control (VSAM KSDS)
      * verbs used by the CBSA programs under test.
      *
      * A process-resident, keyed in-memory KSDS store (mirrors the
      * CICSCONT container-store pattern). Because GnuCOBOL keeps a
      * called module resident for the life of the run unit, the same
      * store is shared between the test driver (which SEEDs fixture
      * records) and the program under test (which READs / REWRITEs /
      * WRITEs / browses it).
      *
      * MULTIPLE FILES: the store serves any number of named KSDS files
      * (e.g. CUSTOMER and ABNDFILE) side by side. Each slot is tagged
      * with the file it belongs to, and every file has its own
      * "geometry" - the key offset + key length within the record image
      * and the record length. The geometry drives key extraction (SEED),
      * key comparison (all ops) and how many bytes are copied to/from the
      * caller's buffer, so files with different record layouts coexist
      * without interfering.
      *
      * CUSTOMER geometry is built in as the default (key at cols 5-20 =
      * CUSTOMER-KEY, 16-byte key, 259-byte record) so the existing
      * customer tests need no changes. Any other file must be registered
      * once via the DEFFILE control op before use (see below).
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
      *     DELETE   remove the record last read for update (TOKEN/UPDATE
      *              semantics), or the record named by RIDFLD if supplied
      *     STARTBR  open a browse positioned at/around RIDFLD
      *     READNEXT next record in ascending key order
      *     READPREV previous record in descending key order
      *     ENDBR    close the browse
      *   Driver-only control ops (called directly from the test driver) -
      *     RESET    empty the data store and clear cursor/update/forced
      *              state (the file registry is preserved).
      *     DEFFILE  register/override a file's geometry. The 12-char
      *              RIDFLD carries three zoned numbers: key offset (4) +
      *              key length (4) + record length (4), 1-based. e.g.
      *              "000100120681" = key at col 1, 12 bytes, 681-byte
      *              record (ABNDFILE). Must be issued before the file is
      *              used; CUSTOMER is pre-registered.
      *     SEED     insert/replace a fixture record (key taken from the
      *              record image at the file's key offset/length).
      *     FORCERSP script the RESP returned by the NEXT file verb (the
      *              desired value is passed in via the resp operand) so a
      *              test can drive an arbitrary error path.
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

      * Slot store. Each slot is tagged with its owning file so several
      * files share the same fixed pool without colliding.
       01 WS-STORE.
          05 WS-COUNT             PIC 9(4)     VALUE 0.
          05 WS-SLOT OCCURS 64 TIMES INDEXED BY WS-IX.
             10 WS-SLOT-USED      PIC X        VALUE 'N'.
             10 WS-SLOT-FILE      PIC X(8)     VALUE SPACES.
             10 WS-SLOT-KEY       PIC X(16)    VALUE SPACES.
             10 WS-SLOT-DATA      PIC X(1024)  VALUE SPACES.

      * File registry: name + geometry (all offsets/lengths 1-based).
       01 WS-FILEREG.
          05 WS-INITDONE          PIC X        VALUE 'N'.
          05 WS-NFILES            PIC 9(2)     VALUE 0.
          05 WS-FDEF OCCURS 16 TIMES INDEXED BY WS-FX.
             10 WS-FDEF-NAME      PIC X(8)     VALUE SPACES.
             10 WS-FDEF-KEYOFF    PIC 9(4)     VALUE 1.
             10 WS-FDEF-KEYLEN    PIC 9(4)     VALUE 16.
             10 WS-FDEF-RECLEN    PIC 9(4)     VALUE 259.

      * Geometry of the file for the current call (resolved from LK-FILE).
       01 WS-CUR-FILE             PIC X(8)     VALUE SPACES.
       01 WS-CUR-KEYOFF           PIC 9(4)     VALUE 1.
       01 WS-CUR-KEYLEN           PIC 9(4)     VALUE 16.
       01 WS-CUR-RECLEN           PIC 9(4)     VALUE 259.

      * DEFFILE geometry parse buffer (mapped from LK-RID(1:12)).
       01 WS-PARSE.
          05 WS-P-KEYOFF          PIC 9(4).
          05 WS-P-KEYLEN          PIC 9(4).
          05 WS-P-RECLEN          PIC 9(4).

       01 WS-BROWSE.
          05 WS-BR-ACTIVE         PIC X        VALUE 'N'.
          05 WS-BR-FRESH          PIC X        VALUE 'N'.
          05 WS-BR-START          PIC X(16)    VALUE SPACES.
          05 WS-BR-LAST           PIC X(16)    VALUE SPACES.

       01 WS-UPD-KEY              PIC X(16)    VALUE SPACES.
       01 WS-UPD-FILE             PIC X(8)     VALUE SPACES.
       01 WS-UPD-SET              PIC X        VALUE 'N'.

       01 WS-FORCED-RESP          PIC S9(8) COMP VALUE -1.

      * Sinks used when the caller omits RESP / RESP2 (e.g. CRECUST's
      * CUSTOMER-control READ ... UPDATE and REWRITE take neither). The
      * omitted linkage items are re-pointed here so the body can MOVE to
      * LK-RESP / LK-RESP2 unconditionally.
       01 WS-RESP-SINK            PIC S9(8) COMP VALUE 0.
       01 WS-RESP2-SINK           PIC S9(8) COMP VALUE 0.

       01 WS-BEST-IX              PIC 9(4)     VALUE 0.
       01 WS-HIT-IX               PIC 9(4)     VALUE 0.
       01 WS-FCOUNT               PIC 9(4)     VALUE 0.
       01 WS-BOUND                PIC X(16)    VALUE SPACES.
       01 WS-INCLUSIVE            PIC X        VALUE 'N'.

       LINKAGE SECTION.
       01 LK-OP                  PIC X(8).
       01 LK-FILE                PIC X(8).
       01 LK-RID                 PIC X(16).
       01 LK-REC                 PIC X(1024).
       01 LK-RESP                PIC S9(8) COMP.
       01 LK-RESP2               PIC S9(8) COMP.

       PROCEDURE DIVISION USING LK-OP LK-FILE
                                OPTIONAL LK-RID OPTIONAL LK-REC
                                OPTIONAL LK-RESP OPTIONAL LK-RESP2.
       A010.
           PERFORM ENSURE-INIT

           IF LK-RESP IS OMITTED
              SET ADDRESS OF LK-RESP TO ADDRESS OF WS-RESP-SINK
           END-IF
           IF LK-RESP2 IS OMITTED
              SET ADDRESS OF LK-RESP2 TO ADDRESS OF WS-RESP2-SINK
           END-IF

      *    Driver-only control ops never set RESP through the normal path.
           EVALUATE LK-OP
              WHEN 'RESET   '
                 PERFORM DO-RESET
                 GOBACK
              WHEN 'DEFFILE '
                 PERFORM DO-DEFFILE
                 GOBACK
              WHEN 'SEED    '
                 PERFORM RESOLVE-FILE
                 PERFORM DO-SEED
                 GOBACK
              WHEN 'FORCERSP'
                 MOVE LK-RESP TO WS-FORCED-RESP
                 MOVE 0 TO LK-RESP
                 MOVE 0 TO LK-RESP2
                 GOBACK
           END-EVALUATE

           PERFORM RESOLVE-FILE
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
              WHEN 'DELETE  '
                 PERFORM DO-DELETE
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
      * Registry / geometry helpers.
      *-----------------------------------------------------------------
      *    Pre-register CUSTOMER once so existing customer tests need no
      *    DEFFILE. RESET preserves the registry, so this runs once.
       ENSURE-INIT.
           IF WS-INITDONE NOT = 'Y'
              MOVE 'Y' TO WS-INITDONE
              MOVE 0   TO WS-NFILES
              PERFORM VARYING WS-FX FROM 1 BY 1 UNTIL WS-FX > 16
                 MOVE SPACES TO WS-FDEF-NAME(WS-FX)
                 MOVE 1   TO WS-FDEF-KEYOFF(WS-FX)
                 MOVE 16  TO WS-FDEF-KEYLEN(WS-FX)
                 MOVE 259 TO WS-FDEF-RECLEN(WS-FX)
              END-PERFORM
              ADD 1 TO WS-NFILES
              MOVE 'CUSTOMER' TO WS-FDEF-NAME(WS-NFILES)
              MOVE 5   TO WS-FDEF-KEYOFF(WS-NFILES)
              MOVE 16  TO WS-FDEF-KEYLEN(WS-NFILES)
              MOVE 259 TO WS-FDEF-RECLEN(WS-NFILES)
           END-IF.

      *    Register or override the geometry for the file named in
      *    LK-FILE; parameters carried in LK-RID(1:12).
       DO-DEFFILE.
           MOVE LK-RID(1:12) TO WS-PARSE
           PERFORM FIND-FILE
           IF WS-HIT-IX = 0
              ADD 1 TO WS-NFILES
              MOVE WS-NFILES TO WS-HIT-IX
              MOVE LK-FILE   TO WS-FDEF-NAME(WS-HIT-IX)
           END-IF
           MOVE WS-P-KEYOFF TO WS-FDEF-KEYOFF(WS-HIT-IX)
           MOVE WS-P-KEYLEN TO WS-FDEF-KEYLEN(WS-HIT-IX)
           MOVE WS-P-RECLEN TO WS-FDEF-RECLEN(WS-HIT-IX)
           MOVE 0 TO LK-RESP
           MOVE 0 TO LK-RESP2.

      *    Locate LK-FILE in the registry (WS-HIT-IX = 0 if absent).
       FIND-FILE.
           MOVE 0 TO WS-HIT-IX
           PERFORM VARYING WS-FX FROM 1 BY 1 UNTIL WS-FX > WS-NFILES
              IF WS-FDEF-NAME(WS-FX) = LK-FILE
                 MOVE WS-FX TO WS-HIT-IX
                 EXIT PERFORM
              END-IF
           END-PERFORM.

      *    Resolve current-call geometry from LK-FILE (CUSTOMER defaults
      *    if the file was never registered).
       RESOLVE-FILE.
           MOVE LK-FILE TO WS-CUR-FILE
           PERFORM FIND-FILE
           IF WS-HIT-IX > 0
              MOVE WS-FDEF-KEYOFF(WS-HIT-IX) TO WS-CUR-KEYOFF
              MOVE WS-FDEF-KEYLEN(WS-HIT-IX) TO WS-CUR-KEYLEN
              MOVE WS-FDEF-RECLEN(WS-HIT-IX) TO WS-CUR-RECLEN
           ELSE
              MOVE 5   TO WS-CUR-KEYOFF
              MOVE 16  TO WS-CUR-KEYLEN
              MOVE 259 TO WS-CUR-RECLEN
           END-IF.

      *-----------------------------------------------------------------
       DO-RESET.
           MOVE 0 TO WS-COUNT
           PERFORM VARYING WS-IX FROM 1 BY 1 UNTIL WS-IX > 64
              MOVE 'N'    TO WS-SLOT-USED(WS-IX)
              MOVE SPACES TO WS-SLOT-FILE(WS-IX)
              MOVE SPACES TO WS-SLOT-KEY(WS-IX)
              MOVE SPACES TO WS-SLOT-DATA(WS-IX)
           END-PERFORM
           MOVE 'N' TO WS-BR-ACTIVE
           MOVE 'N' TO WS-BR-FRESH
           MOVE 'N' TO WS-UPD-SET
           MOVE -1  TO WS-FORCED-RESP
           MOVE 0 TO LK-RESP
           MOVE 0 TO LK-RESP2.

      *    SEED derives the key from the record image at the file's
      *    configured key offset/length.
       DO-SEED.
           MOVE 0 TO LK-RESP
           MOVE 0 TO LK-RESP2
           MOVE SPACES TO WS-BOUND
           MOVE LK-REC(WS-CUR-KEYOFF:WS-CUR-KEYLEN)
                TO WS-BOUND(1:WS-CUR-KEYLEN)
           PERFORM FIND-BY-KEY
           IF WS-HIT-IX > 0
              MOVE SPACES TO WS-SLOT-DATA(WS-HIT-IX)
              MOVE LK-REC(1:WS-CUR-RECLEN)
                   TO WS-SLOT-DATA(WS-HIT-IX)(1:WS-CUR-RECLEN)
           ELSE
              PERFORM CLAIM-FREE-SLOT
              IF WS-HIT-IX > 0
                 MOVE 'Y'          TO WS-SLOT-USED(WS-HIT-IX)
                 MOVE WS-CUR-FILE  TO WS-SLOT-FILE(WS-HIT-IX)
                 MOVE SPACES       TO WS-SLOT-KEY(WS-HIT-IX)
                 MOVE WS-BOUND(1:WS-CUR-KEYLEN)
                      TO WS-SLOT-KEY(WS-HIT-IX)(1:WS-CUR-KEYLEN)
                 MOVE SPACES       TO WS-SLOT-DATA(WS-HIT-IX)
                 MOVE LK-REC(1:WS-CUR-RECLEN)
                      TO WS-SLOT-DATA(WS-HIT-IX)(1:WS-CUR-RECLEN)
                 ADD 1 TO WS-COUNT
              ELSE
                 MOVE 1 TO LK-RESP
              END-IF
           END-IF.

      *-----------------------------------------------------------------
       DO-READ.
           MOVE SPACES TO WS-BOUND
           MOVE LK-RID(1:WS-CUR-KEYLEN) TO WS-BOUND(1:WS-CUR-KEYLEN)
           PERFORM FIND-BY-KEY
           IF WS-HIT-IX > 0
              MOVE WS-SLOT-DATA(WS-HIT-IX)(1:WS-CUR-RECLEN)
                   TO LK-REC(1:WS-CUR-RECLEN)
           ELSE
              MOVE WS-RESP-NOTFND TO LK-RESP
           END-IF.

       DO-READ-UPDATE.
           PERFORM DO-READ
           IF LK-RESP = WS-RESP-NORMAL
              MOVE SPACES TO WS-UPD-KEY
              MOVE LK-RID(1:WS-CUR-KEYLEN)
                   TO WS-UPD-KEY(1:WS-CUR-KEYLEN)
              MOVE WS-CUR-FILE TO WS-UPD-FILE
              MOVE 'Y'    TO WS-UPD-SET
           END-IF.

       DO-REWRITE.
           IF WS-UPD-SET NOT = 'Y' OR WS-UPD-FILE NOT = WS-CUR-FILE
              MOVE WS-RESP-NOTFND TO LK-RESP
           ELSE
              MOVE WS-UPD-KEY TO WS-BOUND
              PERFORM FIND-BY-KEY
              IF WS-HIT-IX > 0
                 MOVE SPACES TO WS-SLOT-DATA(WS-HIT-IX)
                 MOVE LK-REC(1:WS-CUR-RECLEN)
                      TO WS-SLOT-DATA(WS-HIT-IX)(1:WS-CUR-RECLEN)
              ELSE
                 MOVE WS-RESP-NOTFND TO LK-RESP
              END-IF
              MOVE 'N' TO WS-UPD-SET
           END-IF.

       DO-WRITE.
           MOVE SPACES TO WS-BOUND
           MOVE LK-RID(1:WS-CUR-KEYLEN) TO WS-BOUND(1:WS-CUR-KEYLEN)
           PERFORM FIND-BY-KEY
           IF WS-HIT-IX > 0
              MOVE WS-RESP-DUPREC TO LK-RESP
           ELSE
              PERFORM CLAIM-FREE-SLOT
              IF WS-HIT-IX > 0
                 MOVE 'Y'         TO WS-SLOT-USED(WS-HIT-IX)
                 MOVE WS-CUR-FILE TO WS-SLOT-FILE(WS-HIT-IX)
                 MOVE SPACES      TO WS-SLOT-KEY(WS-HIT-IX)
                 MOVE WS-BOUND(1:WS-CUR-KEYLEN)
                      TO WS-SLOT-KEY(WS-HIT-IX)(1:WS-CUR-KEYLEN)
                 MOVE SPACES      TO WS-SLOT-DATA(WS-HIT-IX)
                 MOVE LK-REC(1:WS-CUR-RECLEN)
                      TO WS-SLOT-DATA(WS-HIT-IX)(1:WS-CUR-RECLEN)
                 ADD 1 TO WS-COUNT
              ELSE
                 MOVE 1 TO LK-RESP
              END-IF
           END-IF.

      *    DELETE removes the record identified by the update TOKEN (the
      *    record last READ ... UPDATE, as DELCUS does), or by RIDFLD when
      *    a key is supplied directly.
       DO-DELETE.
           MOVE SPACES TO WS-BOUND
           IF LK-RID IS NOT OMITTED
              AND LK-RID(1:WS-CUR-KEYLEN) NOT = SPACES
              MOVE LK-RID(1:WS-CUR-KEYLEN) TO WS-BOUND(1:WS-CUR-KEYLEN)
           ELSE
              IF WS-UPD-SET NOT = 'Y' OR WS-UPD-FILE NOT = WS-CUR-FILE
                 MOVE WS-RESP-NOTFND TO LK-RESP
                 GO TO DO-DELETE-EXIT
              END-IF
              MOVE WS-UPD-KEY(1:WS-CUR-KEYLEN)
                   TO WS-BOUND(1:WS-CUR-KEYLEN)
           END-IF
           PERFORM FIND-BY-KEY
           IF WS-HIT-IX > 0
              MOVE 'N'    TO WS-SLOT-USED(WS-HIT-IX)
              MOVE SPACES TO WS-SLOT-FILE(WS-HIT-IX)
              MOVE SPACES TO WS-SLOT-KEY(WS-HIT-IX)
              MOVE SPACES TO WS-SLOT-DATA(WS-HIT-IX)
              SUBTRACT 1 FROM WS-COUNT
           ELSE
              MOVE WS-RESP-NOTFND TO LK-RESP
           END-IF
           MOVE 'N' TO WS-UPD-SET.
       DO-DELETE-EXIT.
           EXIT.

      *-----------------------------------------------------------------
       DO-STARTBR.
           PERFORM COUNT-FILE
           IF WS-FCOUNT = 0
              MOVE WS-RESP-NOTFND TO LK-RESP
           ELSE
              MOVE 'Y'    TO WS-BR-ACTIVE
              MOVE 'Y'    TO WS-BR-FRESH
              MOVE SPACES TO WS-BR-START
              MOVE LK-RID(1:WS-CUR-KEYLEN)
                   TO WS-BR-START(1:WS-CUR-KEYLEN)
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
              MOVE WS-SLOT-KEY(WS-BEST-IX)(1:WS-CUR-KEYLEN)
                   TO LK-RID(1:WS-CUR-KEYLEN)
              MOVE WS-SLOT-DATA(WS-BEST-IX)(1:WS-CUR-RECLEN)
                   TO LK-REC(1:WS-CUR-RECLEN)
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
              MOVE WS-SLOT-KEY(WS-BEST-IX)(1:WS-CUR-KEYLEN)
                   TO LK-RID(1:WS-CUR-KEYLEN)
              MOVE WS-SLOT-DATA(WS-BEST-IX)(1:WS-CUR-RECLEN)
                   TO LK-REC(1:WS-CUR-RECLEN)
           ELSE
              MOVE WS-RESP-ENDFILE TO LK-RESP
           END-IF.

       DO-ENDBR.
           MOVE 'N' TO WS-BR-ACTIVE
           MOVE 'N' TO WS-BR-FRESH.

      *-----------------------------------------------------------------
      * Search helpers. All matching is scoped to the current file and
      * compares only the file's key length. WS-HIT-IX / WS-BEST-IX = 0
      * mean "no match".
      *-----------------------------------------------------------------
       FIND-BY-KEY.
           MOVE 0 TO WS-HIT-IX
           PERFORM VARYING WS-IX FROM 1 BY 1 UNTIL WS-IX > 64
              IF WS-SLOT-USED(WS-IX) = 'Y'
                 AND WS-SLOT-FILE(WS-IX) = WS-CUR-FILE
                 AND WS-SLOT-KEY(WS-IX)(1:WS-CUR-KEYLEN)
                     = WS-BOUND(1:WS-CUR-KEYLEN)
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

       COUNT-FILE.
           MOVE 0 TO WS-FCOUNT
           PERFORM VARYING WS-IX FROM 1 BY 1 UNTIL WS-IX > 64
              IF WS-SLOT-USED(WS-IX) = 'Y'
                 AND WS-SLOT-FILE(WS-IX) = WS-CUR-FILE
                 ADD 1 TO WS-FCOUNT
              END-IF
           END-PERFORM.

      *    Smallest key that is >= WS-BOUND (inclusive) or > WS-BOUND.
       FIND-NEXT.
           MOVE 0 TO WS-BEST-IX
           PERFORM VARYING WS-IX FROM 1 BY 1 UNTIL WS-IX > 64
              IF WS-SLOT-USED(WS-IX) = 'Y'
                 AND WS-SLOT-FILE(WS-IX) = WS-CUR-FILE
                 IF (WS-INCLUSIVE = 'Y'
                       AND WS-SLOT-KEY(WS-IX)(1:WS-CUR-KEYLEN)
                           >= WS-BOUND(1:WS-CUR-KEYLEN))
                    OR (WS-INCLUSIVE = 'N'
                       AND WS-SLOT-KEY(WS-IX)(1:WS-CUR-KEYLEN)
                           > WS-BOUND(1:WS-CUR-KEYLEN))
                    IF WS-BEST-IX = 0
                       OR WS-SLOT-KEY(WS-IX)(1:WS-CUR-KEYLEN)
                          < WS-SLOT-KEY(WS-BEST-IX)(1:WS-CUR-KEYLEN)
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
                 AND WS-SLOT-FILE(WS-IX) = WS-CUR-FILE
                 IF (WS-INCLUSIVE = 'Y'
                       AND WS-SLOT-KEY(WS-IX)(1:WS-CUR-KEYLEN)
                           <= WS-BOUND(1:WS-CUR-KEYLEN))
                    OR (WS-INCLUSIVE = 'N'
                       AND WS-SLOT-KEY(WS-IX)(1:WS-CUR-KEYLEN)
                           < WS-BOUND(1:WS-CUR-KEYLEN))
                    IF WS-BEST-IX = 0
                       OR WS-SLOT-KEY(WS-IX)(1:WS-CUR-KEYLEN)
                          > WS-SLOT-KEY(WS-BEST-IX)(1:WS-CUR-KEYLEN)
                       MOVE WS-IX TO WS-BEST-IX
                    END-IF
                 END-IF
              END-IF
           END-PERFORM.
