      ******************************************************************
      * CICSENQ - test double for EXEC CICS ENQ / DEQ.
      *
      * On the mainframe CREACC brackets its allocation of the next
      * account number (the CONTROL named-counter row) with ENQ .. DEQ so
      * concurrent tasks serialise. A unit test runs single-process, so
      * the resource is always uncontended: this double is a no-op that
      * always reports NORMAL (RESP/RESP2 = 0). The op ('ENQ' / 'DEQ') is
      * accepted for symmetry / diagnostics but not otherwise acted on.
      *
      * cicsPreprocessor.py routes both verbs here with the signature:
      *   CALL 'CICSENQ' USING BY CONTENT op(8)
      *        BY REFERENCE resp|OMITTED resp2|OMITTED
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. CICSENQ.

       DATA DIVISION.
       LINKAGE SECTION.
       01 LK-OP                   PIC X(8).
       01 LK-RESP                 PIC S9(8) COMP.
       01 LK-RESP2                PIC S9(8) COMP.

       PROCEDURE DIVISION USING LK-OP
                                OPTIONAL LK-RESP OPTIONAL LK-RESP2.
       A010.
           IF LK-RESP IS NOT OMITTED
              MOVE 0 TO LK-RESP
           END-IF
           IF LK-RESP2 IS NOT OMITTED
              MOVE 0 TO LK-RESP2
           END-IF
           GOBACK.
