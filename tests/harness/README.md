# CBSA COBOL isolated-test harness

This harness lets individual CBSA COBOL programs be **unit-tested in isolation
on Linux** with [GnuCOBOL](https://gnucobol.sourceforge.io/) (`cobc`) — no z/OS,
no CICS region, no Db2. It replaces the mainframe services a program depends on
with small, deterministic, inspectable COBOL *test doubles* (stubs), and it
rewrites the `EXEC CICS` verbs into ordinary `CALL`s to those stubs.

The original `src/base/cobol_src/*.cbl` and `src/base/cobol_copy/*.cpy` sources
are **never modified** — the harness consumes them as-is, so the exact same
sources still build for the mainframe.

## Quick start

```bash
# From the repository root:
tests/run.sh
# or:
make -C tests test
```

Requires `cobc` on the PATH (`sudo apt-get install -y gnucobol`). The script
compiles the shims, preprocesses + compiles each program under test, then
compiles and runs each unit test, exiting non-zero if any test fails.

## Layout

```
tests/
  run.sh                     # build + run everything (CI entry point)
  Makefile                   # `make test` / `make clean` wrappers
  harness/
    cicsPreprocessor.py      # EXEC CICS / EXEC SQL  ->  CALL translator
    copy/DFHEIBLK.cpy        # EIB shim copybook (injected into WORKING-STORAGE)
    copy/SQLCA.cpy           # SQL communications area (replaces EXEC SQL INCLUDE SQLCA)
    copy/HOSTACCT.cpy        # driver-side view of a Db2 ACCOUNT row (fixtures)
    shims/                   # the CICS/Db2 test doubles (one module per verb group)
    README.md                # this file
  unit/                      # one test driver per program under test
  build/                     # generated (git-ignored): gensrc, modules, bins
```

## How the shim works

GnuCOBOL does not understand IBM's `EXEC CICS` / `EXEC SQL` statements, and the
programs reference EIB fields (`EIBTASKN`, `EIBRESP`, …) that CICS would supply.
The harness bridges the gap in two lightweight steps.

### 1. Source translation — `cicsPreprocessor.py`

A fixed-format-aware preprocessor reads a `.cbl` and emits a translated copy
(to `build/gensrc/`) that GnuCOBOL can compile. It:

* comments out compiler-directing lines (`CBL …`, `PROCESS …`);
* replaces `DFHRESP(name)` with its numeric value, **padded to the same width**
  so no columns shift (only `NORMAL` = 0 is needed today; extend `DFHRESP_MAP`);
* injects `COPY DFHEIBLK.` into `WORKING-STORAGE` and a `CALL 'CICSINIT'` at the
  top of the `PROCEDURE DIVISION` **only when the program references the EIB**,
  so the EIB fields resolve and are populated deterministically before use;
* rewrites each `EXEC CICS <verb> … END-EXEC` block into a `CALL` to a stub
  module, preserving the trailing period when present;
* rewrites `EXEC SQL … END-EXEC` blocks against the Db2 ACCOUNT double
  (`DB2ACC`) — see the *EXEC SQL / Db2* section below;
* injects `USING DFHCOMMAREA` into a `PROCEDURE DIVISION` header that lacks a
  `USING` clause but whose LINKAGE declares `01 DFHCOMMAREA` (CICS would pass
  it implicitly; off-CICS the driver passes it on the `CALL`).

`EXEC CICS RETURN` is translated to `GOBACK` (it ends the CICS task, so control
must not fall through). Whole `EXEC` blocks are replaced with freshly generated
lines that stay within column 72; in-place edits are width-preserving.

### 2. Test doubles — `harness/shims/`

Each stub is a tiny, ordinary COBOL module compiled with `cobc -m`. Because
GnuCOBOL keeps a called module resident for the life of the process, stateful
stubs (the container store) are shared between the program under test and the
test driver.

| Stub       | Stubs verb(s)                       | Behaviour under test |
|------------|-------------------------------------|----------------------|
| `CICSINIT` | (EIB init, injected)                | Sets `EIBRESP/EIBRESP2=0`, `EIBTRNID=spaces`, and `EIBTASKN` from `CBSA_TEST_TASKN` (default 1) — makes the RNG seed injectable. |
| `CICSCONT` | `GET CONTAINER`, `PUT CONTAINER`    | Process-resident container store keyed by name; `resp=0` on hit, `resp=1` (CONTAINERERR) on a GET miss. |
| `CICSDLAY` | `DELAY`                             | No-op by default (`resp=0`); set `CBSA_TEST_DELAY_MODE=real` to actually sleep. |
| `CICSLINK` | `LINK PROGRAM(..) COMMAREA(..)`     | Records the LINK; for `PROGRAM('INQCUST ')` returns a **valid customer** (`INQCUST-INQ-SUCCESS='Y'`) so account programs that validate a customer can proceed. Force not-found with `CBSA_TEST_INQCUST_SUCCESS=N`. |
| `CICSASGN` | `ASSIGN APPLID/PROGRAM/ABCODE(..)`  | Returns canned values (error-path only). |
| `CICSABND` | `ABEND ABCODE(..)`                  | Records the abend code (error-path only). |
| `CICSTIME` | `ASKTIME ABSTIME(..)`               | Returns a fixed ABSTIME (error-path only). |
| `CICSFTIM` | `FORMATTIME`                        | Returns a fixed date/time (error-path only). |
| `CICSSYNC` | `SYNCPOINT [ROLLBACK]`              | No-op; reports `RESP=NORMAL` (error-path only). |
| `DB2ACC`   | `EXEC SQL` against `ACCOUNT`        | In-memory ACCOUNT table (see below): keyed SELECT, "last account" SELECT, cursor OPEN/FETCH/CLOSE, UPDATE; driver seeds rows and scripts `SQLCODE`. |

**CICS verbs stubbed so far:** `DELAY`, `GET CONTAINER`, `PUT CONTAINER`,
`RETURN`, `LINK`, `ASSIGN` (APPLID/PROGRAM/ABCODE), `ABEND`, `ASKTIME`,
`FORMATTIME`, `HANDLE ABEND`, `SYNCPOINT`. Other verbs (`ADDRESS`, `GETMAIN`,
`RETRIEVE`, …) are **not** yet stubbed. Add them as needed (below).

### EXEC SQL / Db2 ACCOUNT double — `DB2ACC`

`DB2ACC` is a process-resident in-memory `ACCOUNT` table (the same shared-state
pattern as `CICSCONT`). The preprocessor rewrites the `EXEC SQL` statements the
account programs use into `CALL 'DB2ACC'` with a fixed signature:

```
CALL 'DB2ACC' USING BY CONTENT  <op>
     BY REFERENCE sortcode accno custno row sqlcode
```

`row` is the program's `HOST-ACCOUNT-ROW` group (12 ACCOUNT columns); `sqlcode`
is the `SQLCODE` from the `SQLCA` copybook. `INCLUDE SQLCA` is rewritten to
`COPY SQLCA.`; `INCLUDE <table>` (e.g. `ACCDB2`) is dropped (declarative only).

**SQL subset covered** (exactly what UPDACC / INQACC / INQACCCU use):

| Statement                                              | `op`     |
|--------------------------------------------------------|----------|
| `SELECT … INTO … WHERE sortcode AND accno`             | `SELKEY` |
| `SELECT … ORDER BY ACCOUNT_NUMBER DESC FETCH FIRST 1`  | `SELMAX` |
| `UPDATE ACCOUNT SET type/rate/overdraft WHERE key`     | `UPDATE` |
| `DECLARE CURSOR` + `OPEN` (keyed by sortcode+accno)    | `OPENA`  |
| `DECLARE CURSOR` + `OPEN` (keyed by custno+sortcode)   | `OPENC`  |
| `FETCH FROM cursor INTO …` / `CLOSE cursor`            | `FETCH` / `CLOSE` |

`SQLCODE` follows Db2 conventions: `0` found / `+100` not-found / `<0` error.
Cursor `OPEN` mode is inferred from the `DECLARE CURSOR` WHERE columns.

**Driver-facing operations** (seed/inspect the table from a test):

| `op`     | Effect |
|----------|--------|
| `CLEAR`  | Empty the table, reset cursor + scripted-error state. |
| `INSERT` | Append `row` as a fixture row. |
| `SETSQL` | Force `sqlcode` on the **next** data op (drives error paths). |
| `SELKEY` | Re-SELECT a row for assertions. |

Include `copy/HOSTACCT.cpy` under an `01` in the driver to build/inspect rows;
it is byte-identical to `HOST-ACCOUNT-ROW`. Account/date fixture conventions:
keys are fixed-width numeric strings (sort code `"987654"`, account `"00000001"`,
customer `"0000000001"`), dates are `"YYYY-MM-DD"`. See `tests/unit/updaccTest.cbl`,
`inqaccTest.cbl`, `inqacccuTest.cbl`.

### Determinism knobs (environment variables)

| Variable                | Default | Effect |
|-------------------------|---------|--------|
| `CBSA_TEST_TASKN`       | `1`     | Value placed in `EIBTASKN`; seeds `FUNCTION RANDOM`. |
| `CBSA_TEST_DELAY_MODE`  | `stub`  | `real` makes `CICSDLAY` actually sleep. |
| `CBSA_TEST_INQCUST_SUCCESS` | `Y` | `N` makes the `CICSLINK` INQCUST stub return customer-not-found. |
| `COBC`                  | `cobc`  | Override the compiler binary. |

## Adding a new program under test

1. Add its base name (= `PROGRAM-ID`) to `programsUnderTest` in `run.sh`.
2. Write `tests/unit/<name>Test.cbl`: a COBOL main that builds the input
   (COMMAREA and/or seeds a container via `CICSCONT`), `CALL`s the program,
   inspects the outputs, and sets `RETURN-CODE` non-zero on any failed
   assertion (see the existing drivers for the pattern).
3. Run `tests/run.sh`. If it fails to translate, the program probably uses an
   `EXEC CICS`/`EXEC SQL` verb not yet handled — add it (below).

## Adding a new CICS verb stub

1. **Preprocessor:** add a branch in `_translate_block()` (CICS) or
   `_translate_sql_block()` (SQL) in `cicsPreprocessor.py` that maps the verb's
   operands to a `CALL`. Pass data items `BY REFERENCE` and literals `BY CONTENT`.
2. **Stub module:** add `harness/shims/<MODULE>.cbl` (a `PROGRAM-ID` whose name
   matches the module file). Keep behaviour deterministic and inspectable.
3. For a new `DFHRESP(...)` condition, add it to `DFHRESP_MAP`.
4. Re-run `tests/run.sh`.

Naming convention: shell/Python/config identifiers use **camelCase**
(`cicsPreprocessor.py`, `run.sh` variables); COBOL modules keep the repo's
UPPERCASE `PROGRAM-ID` convention.
