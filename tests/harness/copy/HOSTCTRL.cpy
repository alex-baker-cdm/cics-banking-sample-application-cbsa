      ******************************************************************
      * HOSTCTRL.cpy - test-driver view of one Db2 CONTROL row.
      *
      * Byte-for-byte identical to the HOST-CONTROL-ROW group that CREACC
      * passes to DB2CTRL, so a driver can seed the named-counter rows
      * (SEED) and read them back (GETVAL) through the same shim calling
      * convention. Include under a caller-provided 01, e.g.
      *     01 FIX-CTRL.
      *        COPY HOSTCTRL.
      ******************************************************************
          03 FIXC-NAME                 PIC X(32).
          03 FIXC-VALNUM               PIC S9(9) COMP.
          03 FIXC-VALSTR               PIC X(40).
