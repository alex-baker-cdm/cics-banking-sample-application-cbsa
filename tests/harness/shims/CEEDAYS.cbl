      ******************************************************************
      * CEEDAYS - off-mainframe stand-in for the LE date service.
      *
      * CRECUST calls CEEDAYS to convert a validated date of birth
      * (YYYYMMDD) to a Lilian day number, then compares it with today's
      * Lilian value (from CEELOCT) to sanity-check the date. Off CICS/LE
      * we only need a deterministic, monotonic mapping: a larger date must
      * yield a larger number, and any plausible DOB must be smaller than
      * the fixed "today" CEELOCT returns (200000).
      *
      * On success the LE feedback token is set to CEE000 (LOW-VALUES).
      * CRECUST screens out-of-range years (< 1601) before calling, so this
      * double always reports success.
      *
      * Signature (per the CRECUST CALL):
      *   CALL "CEEDAYS" USING date-record date-format lilian-out fc-out
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. CEEDAYS.

       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 WS-DAYS                 PIC S9(9) BINARY VALUE 0.

       LINKAGE SECTION.
       01 LK-DATE.
          05 LK-DATE-LEN          PIC S9(4) BINARY.
          05 LK-DATE-YEAR         PIC 9(4).
          05 LK-DATE-MONTH        PIC 9(2).
          05 LK-DATE-DAY          PIC 9(2).
       01 LK-FORMAT.
          05 LK-FORMAT-LEN        PIC S9(4) BINARY.
          05 LK-FORMAT-TEXT       PIC X(8).
       01 LK-LILLIAN              PIC S9(9) BINARY.
       01 LK-FC                   PIC X(12).

       PROCEDURE DIVISION USING LK-DATE LK-FORMAT LK-LILLIAN LK-FC.
       A010.
           COMPUTE WS-DAYS = ((LK-DATE-YEAR - 1601) * 365)
                             + (LK-DATE-MONTH * 31)
                             + LK-DATE-DAY
           MOVE WS-DAYS TO LK-LILLIAN
           MOVE LOW-VALUES TO LK-FC
           GOBACK.
