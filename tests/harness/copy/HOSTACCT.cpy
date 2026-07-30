      ******************************************************************
      * HOSTACCT.cpy - test-driver view of one Db2 ACCOUNT row.
      *
      * Byte-for-byte identical to the HOST-ACCOUNT-ROW group that the
      * account programs pass to DB2ACC, so a driver can build fixture
      * rows (INSERT) and inspect rows (SELKEY) through the same shim
      * calling convention. Include under a caller-provided 01, e.g.
      *     01 FIX-ROW.
      *        COPY HOSTACCT.
      ******************************************************************
          03 FIX-EYE                    PIC X(4).
          03 FIX-CUSTNO                 PIC X(10).
          03 FIX-SORTCODE               PIC X(6).
          03 FIX-ACCNO                  PIC X(8).
          03 FIX-TYPE                   PIC X(8).
          03 FIX-RATE                   PIC S9(4)V99 COMP-3.
          03 FIX-OPENED                 PIC X(10).
          03 FIX-OVERDRAFT              PIC S9(9) COMP.
          03 FIX-LASTSTMT               PIC X(10).
          03 FIX-NEXTSTMT               PIC X(10).
          03 FIX-AVAILBAL               PIC S9(10)V99 COMP-3.
          03 FIX-ACTUALBAL              PIC S9(10)V99 COMP-3.
