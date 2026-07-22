      ******************************************************************
      * DFHEIBLK.cpy - test-harness EXEC Interface Block (EIB) shim.
      *
      * The real CICS translator injects DFHEIBLK into the LINKAGE
      * SECTION. Off-CICS we only need the handful of EIB fields the
      * Layer-0 programs actually reference. cicsPreprocessor.py inserts
      * this COPY into WORKING-STORAGE and calls CICSINIT to populate it.
      ******************************************************************
       01 DFHEIB-SHIM.
          05 EIBTASKN   PIC S9(7) COMP-3.
          05 EIBRESP    PIC S9(8) COMP.
          05 EIBRESP2   PIC S9(8) COMP.
          05 EIBTRNID   PIC X(4).
