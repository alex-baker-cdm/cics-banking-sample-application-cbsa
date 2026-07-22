      ******************************************************************
      * HOSTPROC.cpy - test-driver view of one Db2 PROCTRAN row.
      *
      * Byte-for-byte identical to the HOST-PROCTRAN-ROW group that the
      * money-movement / delete programs pass to DB2PROC, so a driver can
      * inspect the audit rows those programs wrote (GETLAST / GETFRST)
      * through the same shim calling convention. Include under a
      * caller-provided 01, e.g.
      *     01 FIX-PROC.
      *        COPY HOSTPROC.
      ******************************************************************
          03 FIXP-EYE                   PIC X(4).
          03 FIXP-SORTCODE              PIC X(6).
          03 FIXP-ACCNO                 PIC X(8).
          03 FIXP-DATE                  PIC X(10).
          03 FIXP-TIME                  PIC X(6).
          03 FIXP-REF                   PIC X(12).
          03 FIXP-TYPE                  PIC X(3).
          03 FIXP-DESC                  PIC X(40).
          03 FIXP-AMOUNT                PIC S9(10)V99 COMP-3.
