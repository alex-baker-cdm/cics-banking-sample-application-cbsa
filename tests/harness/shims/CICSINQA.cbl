      ******************************************************************
      * CICSINQA - test double for EXEC CICS INQUIRE ASSOCIATION(token)
      * with the origin-data (OD*) receivers BNK1CRA requests.
      *
      * Returns canned, deterministic origin data so the credit/debit
      * screen can populate the ORIGIN block it passes on to DBCRFUN.
      *
      *   CALL 'CICSINQA' USING BY REFERENCE applid userid facilName
      *                                      networkId facilType
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. CICSINQA.

       DATA DIVISION.
       LINKAGE SECTION.
       01 LK-APPLID              PIC X(8).
       01 LK-USERID              PIC X(8).
       01 LK-FACILNAME           PIC X(8).
       01 LK-NETWORKID           PIC X(8).
       01 LK-FACILTYPE           PIC S9(8) COMP.

       PROCEDURE DIVISION USING LK-APPLID LK-USERID LK-FACILNAME
                                LK-NETWORKID LK-FACILTYPE.
       A010.
           MOVE 'CBSATEST' TO LK-APPLID
           MOVE 'TESTUSER' TO LK-USERID
           MOVE 'TESTTERM' TO LK-FACILNAME
           MOVE 'TESTNETW' TO LK-NETWORKID
           MOVE 1          TO LK-FACILTYPE
           GOBACK.
