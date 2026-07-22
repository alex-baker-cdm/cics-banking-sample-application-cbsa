      ******************************************************************
      * DB2CTRL - test double for the Db2 CONTROL table (EXEC SQL).
      *
      * A process-resident, in-memory CONTROL table, sibling to DB2ACC /
      * DB2PROC. CREACC allocates the next account number from the
      * CONTROL "named-counter" rows (<sortcode>-ACCOUNT-LAST and
      * <sortcode>-ACCOUNT-COUNT): it SELECTs a row by CONTROL_NAME,
      * increments the numeric value, and UPDATEs it back. This double
      * serves those SELECT / UPDATE statements and lets a driver seed the
      * starting counters and read them back afterwards.
      *
      * cicsPreprocessor.py rewrites the CONTROL EXEC SQL statements into a
      * CALL to this module with a fixed positional signature:
      *
      *   CALL 'DB2CTRL' USING BY CONTENT  <op>
      *        BY REFERENCE row SQLCA
      *
      *   'row' is the program's HOST-CONTROL-ROW group (CONTROL_NAME,
      *   CONTROL_VALUE_NUM, CONTROL_VALUE_STR). tests/harness/copy/
      *   HOSTCTRL.cpy is a byte-identical driver-side view for seeding /
      *   inspecting rows. The final operand is the caller's whole SQLCA
      *   group so the double can set SQLCODE.
      *
      *   op (8 chars, space padded):
      *     CLEAR   - empty the table + reset error state          (driver)
      *     SEED    - insert/replace a CONTROL row keyed on
      *               CONTROL_NAME, from 'row'                      (driver)
      *     GETVAL  - copy the row whose CONTROL_NAME matches 'row'
      *               back into 'row' (readback for assertions)    (driver)
      *     SETSQL  - force SQLCODE on the NEXT data op; read from
      *               the caller's SQLCA                            (driver)
      *     SELKEY  - SELECT .. INTO row WHERE CONTROL_NAME
      *     UPDATE  - UPDATE CONTROL_VALUE_NUM WHERE CONTROL_NAME
      *
      *   SQLCODE mirrors Db2:
      *     0    row found / statement ok
      *     +100 not found
      *     <0   scripted error (via SETSQL) to drive error paths
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. DB2CTRL.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 WS-STATE.
          05 WS-ROW-COUNT          PIC 9(4)  VALUE 0.
          05 WS-FORCE-FLAG         PIC X     VALUE 'N'.
          05 WS-FORCE-SQLCODE      PIC S9(9) COMP-5 VALUE 0.
       01 WS-TABLE.
          05 WS-ROW OCCURS 50 TIMES.
             10 WS-C-NAME          PIC X(32).
             10 WS-C-VALNUM        PIC S9(9) COMP.
             10 WS-C-VALSTR        PIC X(40).
       01 WS-I                     PIC 9(4)  VALUE 0.
       01 WS-FOUND                 PIC X     VALUE 'N'.

       LINKAGE SECTION.
       01 LK-OP                    PIC X(8).
       01 LK-ROW.
          05 LK-C-NAME             PIC X(32).
          05 LK-C-VALNUM           PIC S9(9) COMP.
          05 LK-C-VALSTR           PIC X(40).
       COPY SQLCA.

       PROCEDURE DIVISION USING LK-OP LK-ROW SQLCA.
       MAIN-A.
           EVALUATE LK-OP
              WHEN 'CLEAR   '
                 PERFORM DO-CLEAR
              WHEN 'SEED    '
                 PERFORM DO-SEED
              WHEN 'GETVAL  '
                 PERFORM DO-GETVAL
              WHEN 'SETSQL  '
                 PERFORM DO-SETSQL
              WHEN OTHER
      *          Data ops honour a scripted SQLCODE (one-shot).
                 IF WS-FORCE-FLAG = 'Y'
                    MOVE WS-FORCE-SQLCODE TO SQLCODE
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
              WHEN 'UPDATE  '
                 PERFORM DO-UPDATE
              WHEN OTHER
                 MOVE -900 TO SQLCODE
           END-EVALUATE.

       DO-CLEAR.
           MOVE 0   TO WS-ROW-COUNT
           MOVE 'N' TO WS-FORCE-FLAG
           MOVE 0   TO WS-FORCE-SQLCODE
           MOVE 0   TO SQLCODE.

      *    Insert or replace a CONTROL row keyed on CONTROL_NAME.
       DO-SEED.
           PERFORM FIND-BY-NAME
           IF WS-FOUND = 'Y'
              MOVE LK-ROW TO WS-ROW(WS-I)
           ELSE
              IF WS-ROW-COUNT >= 50
                 MOVE -901 TO SQLCODE
              ELSE
                 ADD 1 TO WS-ROW-COUNT
                 MOVE LK-ROW TO WS-ROW(WS-ROW-COUNT)
                 MOVE 0 TO SQLCODE
              END-IF
           END-IF.

       DO-SETSQL.
           MOVE SQLCODE TO WS-FORCE-SQLCODE
           MOVE 'Y' TO WS-FORCE-FLAG.

       DO-GETVAL.
           PERFORM FIND-BY-NAME
           IF WS-FOUND = 'Y'
              MOVE WS-ROW(WS-I) TO LK-ROW
              MOVE 0 TO SQLCODE
           ELSE
              MOVE +100 TO SQLCODE
           END-IF.

       DO-SELKEY.
           PERFORM FIND-BY-NAME
           IF WS-FOUND = 'Y'
              MOVE WS-ROW(WS-I) TO LK-ROW
              MOVE 0 TO SQLCODE
           ELSE
              MOVE +100 TO SQLCODE
           END-IF.

      *    UPDATE CONTROL SET CONTROL_VALUE_NUM = :hv WHERE CONTROL_NAME.
       DO-UPDATE.
           PERFORM FIND-BY-NAME
           IF WS-FOUND = 'Y'
              MOVE LK-C-VALNUM TO WS-C-VALNUM(WS-I)
              MOVE 0 TO SQLCODE
           ELSE
              MOVE +100 TO SQLCODE
           END-IF.

      *    Locate the row whose CONTROL_NAME matches LK-C-NAME.
       FIND-BY-NAME.
           MOVE 'N' TO WS-FOUND
           PERFORM VARYING WS-I FROM 1 BY 1
              UNTIL WS-I > WS-ROW-COUNT OR WS-FOUND = 'Y'
              IF WS-C-NAME(WS-I) = LK-C-NAME
                 MOVE 'Y' TO WS-FOUND
              END-IF
           END-PERFORM
           IF WS-FOUND = 'Y'
              SUBTRACT 1 FROM WS-I
           END-IF.
