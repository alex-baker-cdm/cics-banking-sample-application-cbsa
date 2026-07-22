      ******************************************************************
      * DB2ACC - test double for the Db2 ACCOUNT table (EXEC SQL).
      *
      * A process-resident, in-memory ACCOUNT table. Because GnuCOBOL
      * keeps a called module loaded for the life of the run unit, the
      * same table is shared between the test driver (which seeds fixture
      * rows) and the program under test (which SELECTs / UPDATEs / opens
      * a cursor over it). This mirrors the container-store pattern used
      * by CICSCONT.
      *
      * cicsPreprocessor.py rewrites the EXEC SQL statements used by
      * UPDACC / INQACC / INQACCCU into CALLs to this module with a fixed
      * signature:
      *
      *   CALL 'DB2ACC' USING BY CONTENT  <op>
      *        BY REFERENCE sortcode accno custno row sqlcode
      *
      *   op (8 chars, space padded):
      *     CLEAR   - empty the table + reset cursor/error state (driver)
      *     INSERT  - append 'row' as a fixture row               (driver)
      *     SETSQL  - force 'sqlcode' on the NEXT data op          (driver)
      *     SELKEY  - SELECT .. INTO row WHERE sortcode + accno
      *     SELMAX  - SELECT the highest accno row for sortcode
      *               (ORDER BY ACCOUNT_NUMBER DESC FETCH FIRST 1)
      *     OPENA   - OPEN cursor filtered by sortcode + accno
      *     OPENC   - OPEN cursor filtered by custno + sortcode
      *     FETCH   - FETCH next cursor row INTO row
      *     CLOSE   - CLOSE cursor
      *     UPDATE  - UPDATE type/rate/overdraft WHERE sortcode + accno
      *
      *   'row' has the fixed 12-column ACCOUNT layout (see below); it is
      *   the program's HOST-ACCOUNT-ROW group. sqlcode mirrors the SQLCA:
      *     0    row found / statement ok
      *     +100 not found / no (more) rows
      *     <0   scripted error (via SETSQL) to drive error paths
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. DB2ACC.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 WS-STATE.
          05 WS-ROW-COUNT          PIC 9(4)  VALUE 0.
          05 WS-FORCE-FLAG         PIC X     VALUE 'N'.
          05 WS-FORCE-SQLCODE      PIC S9(9) COMP-5 VALUE 0.
          05 WS-CURSOR-OPEN        PIC X     VALUE 'N'.
          05 WS-CURSOR-POS         PIC 9(4)  VALUE 0.
          05 WS-CURSOR-CNT         PIC 9(4)  VALUE 0.
          05 WS-CURSOR-IDX         OCCURS 100 TIMES PIC 9(4).
       01 WS-TABLE.
          05 WS-ROW OCCURS 100 TIMES.
             10 WS-R-EYE           PIC X(4).
             10 WS-R-CUSTNO        PIC X(10).
             10 WS-R-SORTCODE      PIC X(6).
             10 WS-R-ACCNO         PIC X(8).
             10 WS-R-TYPE          PIC X(8).
             10 WS-R-RATE          PIC S9(4)V99 COMP-3.
             10 WS-R-OPENED        PIC X(10).
             10 WS-R-OVERDRAFT     PIC S9(9) COMP.
             10 WS-R-LASTSTMT      PIC X(10).
             10 WS-R-NEXTSTMT      PIC X(10).
             10 WS-R-AVAILBAL      PIC S9(10)V99 COMP-3.
             10 WS-R-ACTUALBAL     PIC S9(10)V99 COMP-3.
       01 WS-I                     PIC 9(4)  VALUE 0.
       01 WS-BEST                  PIC 9(4)  VALUE 0.
       01 WS-FOUND                 PIC X     VALUE 'N'.

       LINKAGE SECTION.
       01 LK-OP                    PIC X(8).
       01 LK-SORTCODE              PIC X(6).
       01 LK-ACCNO                 PIC X(8).
       01 LK-CUSTNO                PIC X(10).
       01 LK-ROW.
          05 LK-R-EYE              PIC X(4).
          05 LK-R-CUSTNO           PIC X(10).
          05 LK-R-SORTCODE         PIC X(6).
          05 LK-R-ACCNO            PIC X(8).
          05 LK-R-TYPE             PIC X(8).
          05 LK-R-RATE             PIC S9(4)V99 COMP-3.
          05 LK-R-OPENED           PIC X(10).
          05 LK-R-OVERDRAFT        PIC S9(9) COMP.
          05 LK-R-LASTSTMT         PIC X(10).
          05 LK-R-NEXTSTMT         PIC X(10).
          05 LK-R-AVAILBAL         PIC S9(10)V99 COMP-3.
          05 LK-R-ACTUALBAL        PIC S9(10)V99 COMP-3.
       01 LK-SQLCODE               PIC S9(9) COMP-5.

       PROCEDURE DIVISION USING LK-OP LK-SORTCODE LK-ACCNO
                                LK-CUSTNO LK-ROW LK-SQLCODE.
       MAIN-A.
           EVALUATE LK-OP
              WHEN 'CLEAR   '
                 PERFORM DO-CLEAR
              WHEN 'INSERT  '
                 PERFORM DO-INSERT
              WHEN 'SETSQL  '
                 PERFORM DO-SETSQL
              WHEN OTHER
      *          Data ops honour a scripted SQLCODE (one-shot).
                 IF WS-FORCE-FLAG = 'Y'
                    MOVE WS-FORCE-SQLCODE TO LK-SQLCODE
                    MOVE 'N' TO WS-FORCE-FLAG
                 ELSE
                    PERFORM DISPATCH-DATA-OP
                 END-IF
           END-EVALUATE
           GOBACK.

       DISPATCH-DATA-OP.
           EVALUATE LK-OP
              WHEN 'SELKEY  '
                 PERFORM DO-SELKEY
              WHEN 'SELMAX  '
                 PERFORM DO-SELMAX
              WHEN 'UPDATE  '
                 PERFORM DO-UPDATE
              WHEN 'OPENA   '
                 PERFORM DO-OPEN-A
              WHEN 'OPENC   '
                 PERFORM DO-OPEN-C
              WHEN 'FETCH   '
                 PERFORM DO-FETCH
              WHEN 'CLOSE   '
                 PERFORM DO-CLOSE
              WHEN OTHER
                 MOVE -900 TO LK-SQLCODE
           END-EVALUATE.

       DO-CLEAR.
           MOVE 0   TO WS-ROW-COUNT
           MOVE 'N' TO WS-FORCE-FLAG
           MOVE 0   TO WS-FORCE-SQLCODE
           MOVE 'N' TO WS-CURSOR-OPEN
           MOVE 0   TO WS-CURSOR-POS
           MOVE 0   TO WS-CURSOR-CNT
           MOVE 0   TO LK-SQLCODE.

       DO-INSERT.
           IF WS-ROW-COUNT >= 100
              MOVE -901 TO LK-SQLCODE
           ELSE
              ADD 1 TO WS-ROW-COUNT
              MOVE LK-ROW TO WS-ROW(WS-ROW-COUNT)
              MOVE 0 TO LK-SQLCODE
           END-IF.

       DO-SETSQL.
           MOVE LK-SQLCODE TO WS-FORCE-SQLCODE
           MOVE 'Y' TO WS-FORCE-FLAG.

       DO-SELKEY.
           MOVE 'N' TO WS-FOUND
           PERFORM VARYING WS-I FROM 1 BY 1
              UNTIL WS-I > WS-ROW-COUNT OR WS-FOUND = 'Y'
              IF WS-R-SORTCODE(WS-I) = LK-SORTCODE
                 AND WS-R-ACCNO(WS-I) = LK-ACCNO
                 MOVE WS-ROW(WS-I) TO LK-ROW
                 MOVE 'Y' TO WS-FOUND
              END-IF
           END-PERFORM
           IF WS-FOUND = 'Y'
              MOVE 0 TO LK-SQLCODE
           ELSE
              MOVE +100 TO LK-SQLCODE
           END-IF.

       DO-SELMAX.
           MOVE 'N' TO WS-FOUND
           MOVE 0 TO WS-BEST
           PERFORM VARYING WS-I FROM 1 BY 1 UNTIL WS-I > WS-ROW-COUNT
              IF WS-R-SORTCODE(WS-I) = LK-SORTCODE
                 IF WS-FOUND = 'N'
                    OR WS-R-ACCNO(WS-I) > WS-R-ACCNO(WS-BEST)
                    MOVE WS-I TO WS-BEST
                    MOVE 'Y' TO WS-FOUND
                 END-IF
              END-IF
           END-PERFORM
           IF WS-FOUND = 'Y'
              MOVE WS-ROW(WS-BEST) TO LK-ROW
              MOVE 0 TO LK-SQLCODE
           ELSE
              MOVE +100 TO LK-SQLCODE
           END-IF.

       DO-UPDATE.
           MOVE 'N' TO WS-FOUND
           PERFORM VARYING WS-I FROM 1 BY 1
              UNTIL WS-I > WS-ROW-COUNT OR WS-FOUND = 'Y'
              IF WS-R-SORTCODE(WS-I) = LK-SORTCODE
                 AND WS-R-ACCNO(WS-I) = LK-ACCNO
                 MOVE LK-R-TYPE      TO WS-R-TYPE(WS-I)
                 MOVE LK-R-RATE      TO WS-R-RATE(WS-I)
                 MOVE LK-R-OVERDRAFT TO WS-R-OVERDRAFT(WS-I)
                 MOVE 'Y' TO WS-FOUND
              END-IF
           END-PERFORM
           IF WS-FOUND = 'Y'
              MOVE 0 TO LK-SQLCODE
           ELSE
              MOVE +100 TO LK-SQLCODE
           END-IF.

       DO-OPEN-A.
           MOVE 0 TO WS-CURSOR-CNT
           PERFORM VARYING WS-I FROM 1 BY 1 UNTIL WS-I > WS-ROW-COUNT
              IF WS-R-SORTCODE(WS-I) = LK-SORTCODE
                 AND WS-R-ACCNO(WS-I) = LK-ACCNO
                 ADD 1 TO WS-CURSOR-CNT
                 MOVE WS-I TO WS-CURSOR-IDX(WS-CURSOR-CNT)
              END-IF
           END-PERFORM
           MOVE 'Y' TO WS-CURSOR-OPEN
           MOVE 0 TO WS-CURSOR-POS
           MOVE 0 TO LK-SQLCODE.

       DO-OPEN-C.
           MOVE 0 TO WS-CURSOR-CNT
           PERFORM VARYING WS-I FROM 1 BY 1 UNTIL WS-I > WS-ROW-COUNT
              IF WS-R-CUSTNO(WS-I) = LK-CUSTNO
                 AND WS-R-SORTCODE(WS-I) = LK-SORTCODE
                 ADD 1 TO WS-CURSOR-CNT
                 MOVE WS-I TO WS-CURSOR-IDX(WS-CURSOR-CNT)
              END-IF
           END-PERFORM
           MOVE 'Y' TO WS-CURSOR-OPEN
           MOVE 0 TO WS-CURSOR-POS
           MOVE 0 TO LK-SQLCODE.

       DO-FETCH.
           IF WS-CURSOR-OPEN NOT = 'Y'
              MOVE -502 TO LK-SQLCODE
           ELSE
              ADD 1 TO WS-CURSOR-POS
              IF WS-CURSOR-POS > WS-CURSOR-CNT
                 MOVE +100 TO LK-SQLCODE
              ELSE
                 MOVE WS-ROW(WS-CURSOR-IDX(WS-CURSOR-POS)) TO LK-ROW
                 MOVE 0 TO LK-SQLCODE
              END-IF
           END-IF.

       DO-CLOSE.
           MOVE 'N' TO WS-CURSOR-OPEN
           MOVE 0 TO WS-CURSOR-POS
           MOVE 0 TO WS-CURSOR-CNT
           MOVE 0 TO LK-SQLCODE.
