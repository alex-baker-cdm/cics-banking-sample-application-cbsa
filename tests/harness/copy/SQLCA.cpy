      ******************************************************************
      * SQLCA.cpy - test-harness SQL Communications Area.
      *
      * The real Db2 precompiler expands "EXEC SQL INCLUDE SQLCA" into
      * the standard SQLCA. Off-Db2, cicsPreprocessor.py rewrites that
      * INCLUDE into "COPY SQLCA." so this layout lands in
      * WORKING-STORAGE. It mirrors the IBM-supplied COBOL SQLCA so the
      * fields the account programs reference (SQLCODE, SQLSTATE,
      * SQLERRML, SQLERRMC, SQLERRD) resolve unchanged. The DB2ACC shim
      * sets SQLCODE (0 found / +100 not-found / negative scripted error).
      ******************************************************************
       01 SQLCA.
          05 SQLCAID                       PIC X(8).
          05 SQLCABC                        PIC S9(9) COMP-5.
          05 SQLCODE                        PIC S9(9) COMP-5.
          05 SQLERRM.
             49 SQLERRML                     PIC S9(4) COMP-5.
             49 SQLERRMC                     PIC X(70).
          05 SQLERRP                        PIC X(8).
          05 SQLERRD OCCURS 6 TIMES          PIC S9(9) COMP-5.
          05 SQLWARN.
             10 SQLWARN0                     PIC X.
             10 SQLWARN1                     PIC X.
             10 SQLWARN2                     PIC X.
             10 SQLWARN3                     PIC X.
             10 SQLWARN4                     PIC X.
             10 SQLWARN5                     PIC X.
             10 SQLWARN6                     PIC X.
             10 SQLWARN7                     PIC X.
          05 SQLEXT.
             10 SQLWARN8                     PIC X.
             10 SQLWARN9                     PIC X.
             10 SQLWARNA                     PIC X.
             10 SQLSTATE                     PIC X(5).
