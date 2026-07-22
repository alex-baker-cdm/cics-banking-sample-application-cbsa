      ******************************************************************
      * CEEIGZCT - off-mainframe stand-in for the LE-supplied copybook of
      * the same name. CRECUST COPYs it inside its Language Environment
      * feedback code (FC), immediately under 02 CONDITION-TOKEN-VALUE, to
      * introduce the CEE000 ("no condition / success") condition name.
      *
      * The real LE copybook is not shipped with the CBSA sources, so the
      * harness provides an equivalent under tests/harness/copy (which is
      * ahead of src/base/cobol_copy on the COPY path). CEE000 is true when
      * the whole condition token is LOW-VALUES, exactly as the LE feedback
      * protocol defines it, and the CEEDAYS / CEELOCT doubles set that on
      * success.
      ******************************************************************
              88  CEE000                          VALUE LOW-VALUES.
