      ******************************************************************
      * CICSASGN - test double for EXEC CICS ASSIGN.
      *
      * Returns canned, inspectable values for the ASSIGN fields the
      * programs request (APPLID, PROGRAM, ABCODE). Only reached on an
      * error path (CRDTAGY1, INQCUST abend handler).
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. CICSASGN.

       DATA DIVISION.
       LINKAGE SECTION.
       01 LK-FIELD               PIC X(8).
       01 LK-RECEIVER            PIC X(8).

       PROCEDURE DIVISION USING LK-FIELD LK-RECEIVER.
       A010.
           EVALUATE LK-FIELD
              WHEN "APPLID  "
                 MOVE "CBSATEST" TO LK-RECEIVER
              WHEN "PROGRAM "
                 MOVE "TESTPGM " TO LK-RECEIVER
              WHEN "ABCODE  "
      *          ABCODE receivers are 4 bytes; write only 4 to be safe.
                 MOVE "TEST"     TO LK-RECEIVER(1:4)
              WHEN OTHER
                 MOVE SPACES     TO LK-RECEIVER
           END-EVALUATE
           GOBACK.
