      ******************************************************************
      * CICSLINK - test double for EXEC CICS LINK PROGRAM(..) COMMAREA(..)
      *
      * A single, process-resident double that recognises each LINKed
      * program the CBSA orchestrators call and, for the driver-scriptable
      * ones, lets a test set up inputs and inspect what was passed. The
      * concrete COMMAREA layout for each program is overlaid onto the
      * passed area with SET ADDRESS OF <view> (so no single fixed layout
      * has to satisfy every caller).
      *
      * RECOGNISED PROGRAMS
      *   INQCUST   validates a customer. Returns "found" by default; a
      *             test forces not-found with CBSA_TEST_INQCUST_SUCCESS=N
      *             (preserved) or the LNKINQOK driver op.
      *   INQACCCU  (LINKed by DELCUS) returns the SCRIPTED list of the
      *             customer's accounts - count into NUMBER-OF-ACCOUNTS and
      *             each account number into COMM-ACCNO(i).
      *   DELACC    (LINKed by DELCUS, once per account) CAPTURES the
      *             account number passed in DELACC-COMM-ACCNO so a test
      *             can assert exactly which accounts were deleted, in
      *             order. The delete itself is a no-op here.
      *   any other program (e.g. ABNDPROC on error paths) is just recorded
      *             with a DISPLAY, as before.
      *
      * DRIVER CONTROL OPS (LK-PROGRAM carries the op; a CTRL block is
      * passed as the COMMAREA):
      *   LNKRESET  clear scripted accounts, captures, and INQCUST flag.
      *   LNKADDAC  append CTRL-ACCNO to the scripted INQACCCU account list.
      *   LNKINQOK  set the INQCUST success flag to CTRL-INQOK ('Y'/'N').
      *   LNKGETCN  return the DELACC capture count in CTRL-NACC.
      *   LNKGETAC  return the CTRL-IDX'th captured account in CTRL-ACCNO.
      *
      * Called (via cicsPreprocessor.py) as:
      *   CALL 'CICSLINK' USING BY CONTENT program BY REFERENCE commarea
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. CICSLINK.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 WS-ENV-SUCCESS          PIC X(4) VALUE SPACES.
       01 WS-INQOK                PIC X    VALUE 'Y'.
       01 WS-I                    PIC 9(4) VALUE 0.

      * Scripted INQACCCU account list (resident for the run unit).
       01 WS-SCR.
          05 WS-SCR-CNT           PIC 9(4) VALUE 0.
          05 WS-SCR-ACC OCCURS 20 TIMES PIC 9(8) VALUE 0.

      * Captured DELACC account numbers, in LINK order.
       01 WS-CAP.
          05 WS-CAP-CNT           PIC 9(4) VALUE 0.
          05 WS-CAP-ACC OCCURS 20 TIMES PIC 9(8) VALUE 0.

       LINKAGE SECTION.
       01 LK-PROGRAM              PIC X(8).
       01 LK-COMMAREA             PIC X(4096).

      * Program-specific overlays of the passed COMMAREA.
       01 INQCUST-VIEW.
          COPY INQCUST.
       01 INQACCCU-VIEW.
          COPY INQACCCU.
       01 DELACC-VIEW.
          03 DA-EYE               PIC X(4).
          03 DA-CUSTNO            PIC X(10).
          03 DA-SCODE             PIC X(6).
          03 DA-ACCNO             PIC 9(8).

      * Driver control block overlay.
       01 CTRL-VIEW.
          03 CTRL-INQOK           PIC X.
          03 CTRL-NACC            PIC 9(4).
          03 CTRL-IDX             PIC 9(4).
          03 CTRL-ACCNO           PIC 9(8).

       PROCEDURE DIVISION USING LK-PROGRAM LK-COMMAREA.
       A010.
           EVALUATE LK-PROGRAM
              WHEN 'INQCUST '
                 PERFORM DO-INQCUST
              WHEN 'INQACCCU'
                 PERFORM DO-INQACCCU
              WHEN 'DELACC  '
                 PERFORM DO-DELACC
              WHEN 'LNKRESET'
                 PERFORM DO-LNKRESET
              WHEN 'LNKADDAC'
                 PERFORM DO-LNKADDAC
              WHEN 'LNKINQOK'
                 PERFORM DO-LNKINQOK
              WHEN 'LNKGETCN'
                 PERFORM DO-LNKGETCN
              WHEN 'LNKGETAC'
                 PERFORM DO-LNKGETAC
              WHEN OTHER
                 DISPLAY "CICSLINK (stub): LINK to " LK-PROGRAM
           END-EVALUATE
           GOBACK.

       DO-INQCUST.
           SET ADDRESS OF INQCUST-VIEW TO ADDRESS OF LK-COMMAREA
           MOVE SPACES TO WS-ENV-SUCCESS
           ACCEPT WS-ENV-SUCCESS FROM ENVIRONMENT
              "CBSA_TEST_INQCUST_SUCCESS"
           END-ACCEPT
           IF WS-INQOK = 'N'
              OR WS-ENV-SUCCESS = 'N' OR WS-ENV-SUCCESS = 'n'
              MOVE 'N' TO INQCUST-INQ-SUCCESS
              MOVE '1' TO INQCUST-INQ-FAIL-CD
           ELSE
              MOVE 'Y' TO INQCUST-INQ-SUCCESS
              MOVE '0' TO INQCUST-INQ-FAIL-CD
           END-IF.

       DO-INQACCCU.
           SET ADDRESS OF INQACCCU-VIEW TO ADDRESS OF LK-COMMAREA
           MOVE WS-SCR-CNT TO NUMBER-OF-ACCOUNTS
           MOVE 'Y' TO COMM-SUCCESS OF INQACCCU-VIEW
           MOVE 'Y' TO CUSTOMER-FOUND OF INQACCCU-VIEW
           MOVE ' ' TO COMM-FAIL-CODE OF INQACCCU-VIEW
           PERFORM VARYING WS-I FROM 1 BY 1 UNTIL WS-I > WS-SCR-CNT
              MOVE 'ACCT'                TO COMM-EYE(WS-I)
              MOVE CUSTOMER-NUMBER       TO COMM-CUSTNO(WS-I)
              MOVE WS-SCR-ACC(WS-I)      TO COMM-ACCNO(WS-I)
           END-PERFORM.

       DO-DELACC.
           SET ADDRESS OF DELACC-VIEW TO ADDRESS OF LK-COMMAREA
           ADD 1 TO WS-CAP-CNT
           MOVE DA-ACCNO TO WS-CAP-ACC(WS-CAP-CNT).

       DO-LNKRESET.
           MOVE 'Y' TO WS-INQOK
           MOVE 0 TO WS-SCR-CNT
           MOVE 0 TO WS-CAP-CNT.

       DO-LNKADDAC.
           SET ADDRESS OF CTRL-VIEW TO ADDRESS OF LK-COMMAREA
           ADD 1 TO WS-SCR-CNT
           MOVE CTRL-ACCNO TO WS-SCR-ACC(WS-SCR-CNT).

       DO-LNKINQOK.
           SET ADDRESS OF CTRL-VIEW TO ADDRESS OF LK-COMMAREA
           MOVE CTRL-INQOK TO WS-INQOK.

       DO-LNKGETCN.
           SET ADDRESS OF CTRL-VIEW TO ADDRESS OF LK-COMMAREA
           MOVE WS-CAP-CNT TO CTRL-NACC.

       DO-LNKGETAC.
           SET ADDRESS OF CTRL-VIEW TO ADDRESS OF LK-COMMAREA
           IF CTRL-IDX >= 1 AND CTRL-IDX <= WS-CAP-CNT
              MOVE WS-CAP-ACC(CTRL-IDX) TO CTRL-ACCNO
           ELSE
              MOVE 0 TO CTRL-ACCNO
           END-IF.
