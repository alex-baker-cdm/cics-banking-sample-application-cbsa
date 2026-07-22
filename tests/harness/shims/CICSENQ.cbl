      ******************************************************************
      * CICSENQ - test double for EXEC CICS ENQ / DEQ.
      *
      * CRECUST serialises allocation of the CUSTOMER named counter with
      * ENQ ... / DEQ ... around the control-record update. Off-CICS the
      * harness runs a single task in one run unit, so there is nothing to
      * serialise against: a no-op that always reports NORMAL is a faithful
      * emulation of the successful-acquire / successful-release path.
      *
      * Called (via cicsPreprocessor.py) as:
      *   CALL 'CICSENQ' USING BY CONTENT  'ENQ'|'DEQ'
      *        BY REFERENCE resource|OMITTED resp|OMITTED resp2|OMITTED
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. CICSENQ.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 WS-RESP-SINK            PIC S9(8) COMP VALUE 0.
       01 WS-RESP2-SINK           PIC S9(8) COMP VALUE 0.

       LINKAGE SECTION.
       01 LK-OP                   PIC X(3).
       01 LK-RESOURCE             PIC X(16).
       01 LK-RESP                 PIC S9(8) COMP.
       01 LK-RESP2                PIC S9(8) COMP.

       PROCEDURE DIVISION USING LK-OP OPTIONAL LK-RESOURCE
                                OPTIONAL LK-RESP OPTIONAL LK-RESP2.
       A010.
           IF LK-RESP IS OMITTED
              SET ADDRESS OF LK-RESP TO ADDRESS OF WS-RESP-SINK
           END-IF
           IF LK-RESP2 IS OMITTED
              SET ADDRESS OF LK-RESP2 TO ADDRESS OF WS-RESP2-SINK
           END-IF
           MOVE 0 TO LK-RESP
           MOVE 0 TO LK-RESP2
           GOBACK.
