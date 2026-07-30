      ******************************************************************
      * CICSAID - resident holder for the injectable EIBAID / EIBCALEN.
      *
      * A 3270 pseudo-conversational program branches on the attention
      * identifier (EIBAID: which key the operator pressed) and on
      * EIBCALEN (0 => first time in, otherwise a COMMAREA was passed).
      * Off-CICS a test driver sets these before invoking the program:
      *
      *     CALL 'CICSAID' USING 'SET' aidByte calenValue
      *
      * and CICSINIT reads them back with the 'GET' op when it populates
      * the EIB shim at program entry. The module is compiled with
      * ``cobc -m`` and stays resident, so the value the driver SETs
      * survives until the program under test is CALLed.
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. CICSAID.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
      * Default: ENTER pressed, no COMMAREA (first-time-in).
       01 WS-AID                  PIC X       VALUE QUOTE.
       01 WS-CALEN                PIC S9(4) COMP VALUE 0.

       LINKAGE SECTION.
       01 LK-OP                   PIC X(3).
       01 LK-AID                  PIC X.
       01 LK-CALEN                PIC S9(4) COMP.

       PROCEDURE DIVISION USING LK-OP LK-AID LK-CALEN.
       A010.
           EVALUATE LK-OP
              WHEN 'SET'
                 MOVE LK-AID   TO WS-AID
                 MOVE LK-CALEN TO WS-CALEN
              WHEN 'GET'
                 MOVE WS-AID   TO LK-AID
                 MOVE WS-CALEN TO LK-CALEN
              WHEN OTHER
                 CONTINUE
           END-EVALUATE
           GOBACK.
