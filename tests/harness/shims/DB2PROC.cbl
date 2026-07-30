      ******************************************************************
      * DB2PROC - test double for the Db2 PROCTRAN transaction-audit
      * table (EXEC SQL INSERT INTO PROCTRAN).
      *
      * A process-resident, in-memory PROCTRAN table, sibling to DB2ACC.
      * The money-movement / delete programs (DELACC, DBCRFUN, XFRFUN)
      * write an audit row to PROCTRAN after they change ACCOUNT; this
      * double captures those rows so a driver can assert them, and lets a
      * driver script a failed INSERT to drive the abend paths.
      *
      * cicsPreprocessor.py rewrites EXEC SQL INSERT INTO PROCTRAN into a
      * CALL to this module with a fixed positional signature:
      *
      *   CALL 'DB2PROC' USING BY CONTENT  <op>
      *        BY REFERENCE row SQLCA
      *
      *   The final operand is the caller's whole SQLCA group so the
      *   double can set SQLCODE (a scripted INSERT failure) and, when a
      *   driver reads rows back, return a count via SQLERRD(1).
      *
      *   op (8 chars, space padded):
      *     CLEAR   - empty the table + reset error state          (driver)
      *     SETSQL  - force SQLCODE on the NEXT insert; read from
      *               the caller's SQLCA                           (driver)
      *     COUNT   - return the number of rows in SQLERRD(1)       (driver)
      *     GETLAST - copy the most recently inserted row into row  (driver)
      *     GETFRST - copy the first inserted row into row          (driver)
      *     INSERT  - append 'row' as a PROCTRAN audit row
      *
      *   'row' has the fixed 9-column PROCTRAN layout (see below); it is
      *   the program's HOST-PROCTRAN-ROW group (tests/harness/copy/
      *   HOSTPROC.cpy is the byte-identical driver view). SQLCODE:
      *     0    statement ok / row returned
      *     +100 no rows to return (GETLAST/GETFRST on empty table)
      *     <0   scripted error (via SETSQL) to drive PROCTRAN-write
      *          failure / abend paths
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. DB2PROC.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 WS-STATE.
          05 WS-ROW-COUNT          PIC 9(4)  VALUE 0.
          05 WS-FORCE-FLAG         PIC X     VALUE 'N'.
          05 WS-FORCE-SQLCODE      PIC S9(9) COMP-5 VALUE 0.
       01 WS-TABLE.
          05 WS-ROW OCCURS 100 TIMES.
             10 WS-P-EYE           PIC X(4).
             10 WS-P-SORTCODE      PIC X(6).
             10 WS-P-ACCNO         PIC X(8).
             10 WS-P-DATE          PIC X(10).
             10 WS-P-TIME          PIC X(6).
             10 WS-P-REF           PIC X(12).
             10 WS-P-TYPE          PIC X(3).
             10 WS-P-DESC          PIC X(40).
             10 WS-P-AMOUNT        PIC S9(10)V99 COMP-3.

       LINKAGE SECTION.
       01 LK-OP                    PIC X(8).
       01 LK-ROW.
          05 LK-P-EYE              PIC X(4).
          05 LK-P-SORTCODE         PIC X(6).
          05 LK-P-ACCNO            PIC X(8).
          05 LK-P-DATE             PIC X(10).
          05 LK-P-TIME             PIC X(6).
          05 LK-P-REF              PIC X(12).
          05 LK-P-TYPE             PIC X(3).
          05 LK-P-DESC             PIC X(40).
          05 LK-P-AMOUNT           PIC S9(10)V99 COMP-3.
       COPY SQLCA.

       PROCEDURE DIVISION USING LK-OP LK-ROW SQLCA.
       MAIN-A.
           EVALUATE LK-OP
              WHEN 'CLEAR   '
                 PERFORM DO-CLEAR
              WHEN 'SETSQL  '
                 PERFORM DO-SETSQL
              WHEN 'COUNT   '
                 PERFORM DO-COUNT
              WHEN 'GETLAST '
                 PERFORM DO-GETLAST
              WHEN 'GETFRST '
                 PERFORM DO-GETFRST
              WHEN 'INSERT  '
                 PERFORM DO-INSERT
              WHEN OTHER
                 MOVE -900 TO SQLCODE
           END-EVALUATE
           GOBACK.

       DO-CLEAR.
           MOVE 0   TO WS-ROW-COUNT
           MOVE 'N' TO WS-FORCE-FLAG
           MOVE 0   TO WS-FORCE-SQLCODE
           MOVE 0   TO SQLCODE.

       DO-SETSQL.
           MOVE SQLCODE TO WS-FORCE-SQLCODE
           MOVE 'Y' TO WS-FORCE-FLAG.

       DO-COUNT.
           MOVE WS-ROW-COUNT TO SQLERRD(1)
           MOVE 0 TO SQLCODE.

       DO-INSERT.
      *    A scripted failure (SETSQL) leaves the table unchanged, just
      *    like a real INSERT that the database rejected.
           IF WS-FORCE-FLAG = 'Y'
              MOVE WS-FORCE-SQLCODE TO SQLCODE
              MOVE 'N' TO WS-FORCE-FLAG
           ELSE
              IF WS-ROW-COUNT >= 100
                 MOVE -901 TO SQLCODE
              ELSE
                 ADD 1 TO WS-ROW-COUNT
                 MOVE LK-ROW TO WS-ROW(WS-ROW-COUNT)
                 MOVE 0 TO SQLCODE
              END-IF
           END-IF.

       DO-GETLAST.
           IF WS-ROW-COUNT = 0
              MOVE +100 TO SQLCODE
           ELSE
              MOVE WS-ROW(WS-ROW-COUNT) TO LK-ROW
              MOVE 0 TO SQLCODE
           END-IF.

       DO-GETFRST.
           IF WS-ROW-COUNT = 0
              MOVE +100 TO SQLCODE
           ELSE
              MOVE WS-ROW(1) TO LK-ROW
              MOVE 0 TO SQLCODE
           END-IF.
