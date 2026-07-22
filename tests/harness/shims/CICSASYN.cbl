      ******************************************************************
      * CICSASYN - deterministic test double for the CICS Asynchronous
      * API verbs used by CRECUST's credit check: RUN TRANSID(..) and
      * FETCH ANY(..).
      *
      * OFF-MAINFRAME SIMPLIFICATION
      * ----------------------------
      * On CICS, RUN TRANSID starts a child transaction that runs the
      * credit-agency program (CRDTAGY1-5) concurrently; the parent later
      * collects each reply with FETCH ANY. That concurrency cannot exist
      * in a single GnuCOBOL run unit, so it is emulated SYNCHRONOUSLY and
      * DETERMINISTICALLY:
      *
      *   RUN   - "runs the agency" inline. The parent has already PUT the
      *           request into container CIPx (x = A..I, one per child) on
      *           the channel. RUN GETs that container (via the existing
      *           CICSCONT double), overlays the SCRIPTED credit score for
      *           that child into the record's credit-score field, PUTs it
      *           back, and enqueues the child's (token, channel) so a
      *           later FETCH can return it. A generated child token is
      *           returned in CHILD. A child scripted "no reply" (see
      *           SETNORPL) is not enqueued, modelling an agency that did
      *           not answer in time.
      *   FETCH - pops the next enqueued child (token + channel, completion
      *           status NORMAL). When the queue is exhausted it returns
      *           RESP=NOTFND / RESP2=1, which is the "no more replies"
      *           terminator CRECUST checks for before averaging the
      *           scores of the children that did respond.
      *
      * Because RUN overlays a fixed, test-supplied score and FETCH replays
      * children in a fixed order, the aggregated credit score CRECUST
      * computes is fully deterministic and assertable.
      *
      * The credit-score field sits at offset 249 (1-based), length 3, in
      * the 261-byte customer record shared by the CRECUST COMMAREA and the
      * child-reply container (COMM-CREDIT-SCORE / WS-CHILD-DATA-CREDIT-
      * SCORE), so RUN overlays WS-BUF(249:3).
      *
      * PROGRAM VERBS (via cicsPreprocessor.py), uniform positional
      * signature (operands a verb lacks are passed OMITTED):
      *   CALL 'CICSASYN' USING BY CONTENT 'RUN     '|'FETCH   '
      *        BY REFERENCE transid channel child
      *                     anytkn compstatus abcode resp resp2
      *
      * DRIVER CONTROL OPS (called directly from tests/unit drivers, same
      * signature; slot is passed in transid, score in compstatus):
      *   RESET    - clear scripted scores/replies and the reply queue.
      *   SETSCORE - set the credit score child <transid> will return.
      *   SETNORPL - mark child <transid> as "did not reply".
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. CICSASYN.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
      * Scripted per-child (1-9) state, resident for the run unit.
       01 WS-SCORE-TBL.
          05 WS-SCORE            OCCURS 9 TIMES PIC 999 VALUE 0.
       01 WS-REPLY-TBL.
          05 WS-REPLY            OCCURS 9 TIMES PIC X   VALUE 'Y'.

      * FIFO of children that "replied", in RUN issue order.
       01 WS-Q.
          05 WS-Q-CNT            PIC 9(4) VALUE 0.
          05 WS-Q-POP            PIC 9(4) VALUE 0.
          05 WS-Q-ENT OCCURS 9 TIMES.
             10 WS-Q-TKN         PIC X(16) VALUE SPACES.
             10 WS-Q-CHAN        PIC X(16) VALUE SPACES.

       01 WS-SEQ                 PIC 9(9) VALUE 0.
       01 WS-TKN.
          05 WS-TKN-PFX          PIC X(7) VALUE 'CHILDT-'.
          05 WS-TKN-NUM          PIC 9(9).

       01 WS-LETTERS             PIC X(9) VALUE 'ABCDEFGHI'.
       01 WS-SLOT-N              PIC 9(4) VALUE 0.
       01 WS-SLOT-1D             PIC 9    VALUE 0.
       01 WS-CONT-NAME           PIC X(16) VALUE SPACES.
       01 WS-BUF                 PIC X(261) VALUE SPACES.
       01 WS-BUF-LEN             PIC S9(8) COMP VALUE 261.
       01 WS-SCORE-DISP          PIC 999 VALUE 0.
       01 WS-CC-RESP             PIC S9(8) COMP VALUE 0.
       01 WS-CC-RESP2            PIC S9(8) COMP VALUE 0.
       01 WS-IX                  PIC 9(4) VALUE 0.

       LINKAGE SECTION.
       01 LK-OP                  PIC X(8).
       01 LK-TRANSID             PIC X(4).
       01 LK-CHANNEL             PIC X(16).
       01 LK-CHILD               PIC X(16).
       01 LK-ANYTKN              PIC X(16).
       01 LK-COMPSTAT            PIC S9(8) COMP.
       01 LK-ABCODE              PIC X(4).
       01 LK-RESP                PIC S9(8) COMP.
       01 LK-RESP2               PIC S9(8) COMP.

       PROCEDURE DIVISION USING LK-OP
                                OPTIONAL LK-TRANSID OPTIONAL LK-CHANNEL
                                OPTIONAL LK-CHILD OPTIONAL LK-ANYTKN
                                OPTIONAL LK-COMPSTAT OPTIONAL LK-ABCODE
                                LK-RESP LK-RESP2.
       A010.
           MOVE 0 TO LK-RESP
           MOVE 0 TO LK-RESP2

           EVALUATE LK-OP
              WHEN 'RESET   '
                 PERFORM DO-RESET
              WHEN 'SETSCORE'
                 MOVE FUNCTION NUMVAL(LK-TRANSID) TO WS-SLOT-N
                 IF WS-SLOT-N >= 1 AND WS-SLOT-N <= 9
                    MOVE LK-COMPSTAT TO WS-SCORE(WS-SLOT-N)
                 END-IF
              WHEN 'SETNORPL'
                 MOVE FUNCTION NUMVAL(LK-TRANSID) TO WS-SLOT-N
                 IF WS-SLOT-N >= 1 AND WS-SLOT-N <= 9
                    MOVE 'N' TO WS-REPLY(WS-SLOT-N)
                 END-IF
              WHEN 'RUN     '
                 PERFORM DO-RUN
              WHEN 'FETCH   '
                 PERFORM DO-FETCH
              WHEN OTHER
                 MOVE 16 TO LK-RESP
           END-EVALUATE

           GOBACK.

       DO-RESET.
           PERFORM VARYING WS-IX FROM 1 BY 1 UNTIL WS-IX > 9
              MOVE 0   TO WS-SCORE(WS-IX)
              MOVE 'Y' TO WS-REPLY(WS-IX)
              MOVE SPACES TO WS-Q-TKN(WS-IX)
              MOVE SPACES TO WS-Q-CHAN(WS-IX)
           END-PERFORM
           MOVE 0 TO WS-Q-CNT
           MOVE 0 TO WS-Q-POP
           MOVE 0 TO WS-SEQ.

       DO-RUN.
      *    Child slot number is the last character of the transid (OCRn).
           MOVE LK-TRANSID(4:1) TO WS-SLOT-1D
           MOVE WS-SLOT-1D TO WS-SLOT-N

      *    Hand a freshly generated child token back to the parent.
           ADD 1 TO WS-SEQ
           MOVE WS-SEQ TO WS-TKN-NUM
           IF LK-CHILD IS NOT OMITTED
              MOVE WS-TKN TO LK-CHILD
           END-IF

           IF WS-SLOT-N < 1 OR WS-SLOT-N > 9
              GO TO DO-RUN-EXIT
           END-IF

           IF WS-REPLY(WS-SLOT-N) = 'Y'
      *       Overlay the scripted score into the child's channel
      *       container and enqueue the reply.
              MOVE SPACES TO WS-CONT-NAME
              STRING 'CIP' DELIMITED BY SIZE,
                     WS-LETTERS(WS-SLOT-N:1) DELIMITED BY SIZE
                 INTO WS-CONT-NAME
              END-STRING

              MOVE SPACES TO WS-BUF
              CALL 'CICSCONT' USING BY CONTENT 'GET'
                   BY REFERENCE WS-CONT-NAME LK-CHANNEL WS-BUF
                                WS-BUF-LEN WS-CC-RESP WS-CC-RESP2
              END-CALL

              MOVE WS-SCORE(WS-SLOT-N) TO WS-SCORE-DISP
              MOVE WS-SCORE-DISP TO WS-BUF(249:3)

              CALL 'CICSCONT' USING BY CONTENT 'PUT'
                   BY REFERENCE WS-CONT-NAME LK-CHANNEL WS-BUF
                                WS-BUF-LEN WS-CC-RESP WS-CC-RESP2
              END-CALL

              ADD 1 TO WS-Q-CNT
              MOVE WS-TKN TO WS-Q-TKN(WS-Q-CNT)
              IF LK-CHANNEL IS NOT OMITTED
                 MOVE LK-CHANNEL TO WS-Q-CHAN(WS-Q-CNT)
              END-IF
           END-IF.
       DO-RUN-EXIT.
           EXIT.

       DO-FETCH.
           IF WS-Q-POP < WS-Q-CNT
              ADD 1 TO WS-Q-POP
              IF LK-ANYTKN IS NOT OMITTED
                 MOVE WS-Q-TKN(WS-Q-POP) TO LK-ANYTKN
              END-IF
              IF LK-CHANNEL IS NOT OMITTED
                 MOVE WS-Q-CHAN(WS-Q-POP) TO LK-CHANNEL
              END-IF
              IF LK-COMPSTAT IS NOT OMITTED
                 MOVE 0 TO LK-COMPSTAT
              END-IF
              IF LK-ABCODE IS NOT OMITTED
                 MOVE SPACES TO LK-ABCODE
              END-IF
           ELSE
      *       No more replies: the NOTFND / RESP2=1 terminator CRECUST
      *       treats as "all outstanding children are done".
              MOVE 13 TO LK-RESP
              MOVE 1  TO LK-RESP2
           END-IF.
