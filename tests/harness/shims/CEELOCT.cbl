      ******************************************************************
      * CEELOCT - off-mainframe stand-in for the LE "current local time"
      * service.
      *
      * CRECUST calls CEELOCT to obtain today's Lilian day number and
      * Gregorian date; it uses the Gregorian year for the customer age
      * check and the Lilian value to confirm the DOB is in the past.
      * A fixed, deterministic "today" keeps the tests reproducible:
      * Lilian 200000 (larger than any DOB CEEDAYS produces) and Gregorian
      * year 2025.
      *
      * On success the LE feedback token is set to CEE000 (LOW-VALUES).
      *
      * Signature (per the CRECUST CALL):
      *   CALL "CEELOCT" USING lilian-out seconds-out gregorian-out fc-out
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. CEELOCT.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 WS-DUMMY                PIC X VALUE SPACE.

       LINKAGE SECTION.
       01 LK-LILLIAN              PIC S9(9) BINARY.
       01 LK-SECONDS              COMP-2.
       01 LK-GREGORIAN.
          05 LK-G-YEAR            PIC 9(4).
          05 LK-G-MONTH           PIC 9(2).
          05 LK-G-DAY             PIC 9(2).
          05 LK-G-HOURS           PIC 9(2).
          05 LK-G-MINUTES         PIC 9(2).
          05 LK-G-SECONDS         PIC 9(2).
          05 LK-G-MILLISECONDS    PIC 9(3).
       01 LK-FC                   PIC X(12).

       PROCEDURE DIVISION USING LK-LILLIAN LK-SECONDS
                                LK-GREGORIAN LK-FC.
       A010.
           MOVE 200000 TO LK-LILLIAN
           MOVE 0      TO LK-SECONDS
           MOVE 2025   TO LK-G-YEAR
           MOVE 01     TO LK-G-MONTH
           MOVE 01     TO LK-G-DAY
           MOVE 12     TO LK-G-HOURS
           MOVE 00     TO LK-G-MINUTES
           MOVE 00     TO LK-G-SECONDS
           MOVE 000    TO LK-G-MILLISECONDS
           MOVE LOW-VALUES TO LK-FC
           GOBACK.
