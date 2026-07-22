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
| `CICSSYNC` | `SYNCPOINT [ROLLBACK]`              | No-op that always reports NORMAL (error-path only). |
| `CICSVSAM` | file control: `READ`, `READ UPDATE`, `REWRITE`, `WRITE`, `STARTBR`, `READNEXT`, `READPREV`, `ENDBR` | Process-resident, keyed in-memory VSAM **KSDS** double serving **multiple named files** (e.g. `CUSTOMER` + `ABNDFILE`). See "VSAM (KSDS) data layer" below. |
| `DB2ACC`   | `EXEC SQL` against `ACCOUNT`        | In-memory ACCOUNT table (see below): keyed SELECT, "last account" SELECT, cursor OPEN/FETCH/CLOSE, UPDATE; driver seeds rows and scripts `SQLCODE`. |

**CICS verbs stubbed so far:** `DELAY`, `GET CONTAINER`, `PUT CONTAINER`,
`RETURN`, `LINK`, `ASSIGN` (APPLID/PROGRAM/ABCODE), `ABEND`, `ASKTIME`,
`FORMATTIME`, `HANDLE ABEND` (disabled to a no-op), `SYNCPOINT`, and the VSAM
file-control verbs `READ`, `READ UPDATE`, `REWRITE`, `WRITE`, `STARTBR`,
`READNEXT`, `READPREV`, `ENDBR`. **`EXEC SQL`** against `ACCOUNT` is also
supported (see the Db2 section below). Other verbs (`ADDRESS`, `GETMAIN`,
`RETRIEVE`, …) are **not** yet stubbed — add them as needed (below).

### VSAM (KSDS) data layer — `CICSVSAM`

`CICSVSAM` is a process-resident, keyed in-memory KSDS store (same "resident
module = shared state" trick as `CICSCONT`). A test driver seeds fixture
records into it, then calls the program under test, which reads / updates /
browses the very same store. The preprocessor rewrites every file-control verb
into one uniform call:

**Multiple files by name.** The double serves any number of KSDS files side by
side (e.g. `CUSTOMER` and `ABNDFILE`). Every slot is tagged with the file it
belongs to, and each file carries its own *geometry* — the key offset + key
length within the record image, and the record length. The geometry drives key
extraction (`SEED`), key comparison (all ops, only the file's key length is
compared), and how many bytes are copied to/from the caller's buffer, so files
with different record layouts coexist without interfering. `CUSTOMER` geometry
is **built in** as the default (key at cols 5-20, 16-byte key, 259-byte record)
so the existing customer tests need no changes; any other file is registered
once via the `DEFFILE` control op (below) before use.

```
CALL 'CICSVSAM' USING BY CONTENT  op(8) file(8)
     BY REFERENCE ridfld|OMITTED record|OMITTED resp resp2
```

Operands a verb does not carry (e.g. `RIDFLD` on `REWRITE`/`ENDBR`, or the
record on `STARTBR`/`ENDBR`) are passed `OMITTED`; the shim only touches the
ones relevant to each op. `READ ... UPDATE` remembers the key so the following
`REWRITE` targets the right record. Browse ops keep an internal cursor:
`STARTBR` positions at/around the `RIDFLD` (so `STARTBR(HIGH-VALUES)` +
`READPREV` returns the highest-key record), and `READNEXT`/`READPREV` walk the
keys in order, writing the found key back into `RIDFLD` like real VSAM.

RESP codes mirror real CICS/VSAM so program control flow is unchanged:
`NORMAL=0`, `NOTFND=13`, `DUPREC=14`, `ENDFILE=20` (all in `DFHRESP_MAP`).

**Driver-only control ops** (called directly from the test driver, never
emitted by the preprocessor):

| op         | Effect |
|------------|--------|
| `RESET   ` | Empty the data store and clear all cursor/update/forced-RESP state. The **file registry is preserved** (issue `DEFFILE` once, then `RESET` freely between sub-tests). |
| `DEFFILE ` | Register/override a file's geometry. The 12-char `ridfld` operand carries three zoned numbers concatenated — key offset (4) + key length (4) + record length (4), all 1-based. e.g. `"000100120681"` = key at col 1, 12 bytes, 681-byte record (`ABNDFILE`). `CUSTOMER` is pre-registered, so only non-customer files need this. |
| `SEED    ` | Insert/replace a fixture record; the key is read from the record image at the file's key offset/length (cols 5-20 for `CUSTOMER`). Pass the record via the `record` operand and `OMITTED` for `ridfld`. |
| `FORCERSP` | Script the RESP the **next** file verb returns (value passed via the `resp` operand), to drive an arbitrary error path. |

The record image the driver seeds is the `CUSTOMER` copybook layout (259
bytes). See `tests/unit/updcustTest.cbl` and `tests/unit/inqcustTest.cbl` for
the seed / call / assert pattern, including deterministic exercise of the
`INQCUST` random-customer and last-customer browse paths via `CBSA_TEST_TASKN`.

**A second file — `ABNDFILE` (`ABNDPROC`).** `ABNDPROC` `EXEC CICS WRITE`s an
`ABNDINFO` record to the `ABNDFILE` KSDS (12-byte key = `ABND-VSAM-KEY`,
681-byte record). `tests/unit/abndprocTest.cbl` shows how to reuse the same
double for a non-customer file:

```
* register ABNDFILE geometry once: key col 1, len 12, record 681
CALL 'CICSVSAM' USING BY CONTENT 'DEFFILE ' 'ABNDFILE'
     BY REFERENCE ws-geom("000100120681") OMITTED resp resp2
CALL 'CICSVSAM' USING BY CONTENT 'RESET   ' 'ABNDFILE' ...   *> data only
CALL 'ABNDPROC' USING ws-commarea                            *> program WRITEs
* read the written record back for assertions (READ is driver-callable):
CALL 'CICSVSAM' USING BY CONTENT 'READ    ' 'ABNDFILE'
     BY REFERENCE abnd-vsam-key ws-readback resp resp2
```

The error path (`ABNDPROC`'s "unable to write" branch) is exercised by
scripting a non-NORMAL RESP on the WRITE with `FORCERSP` and then asserting no
record was persisted.

**The `CRDTAGY` family (`CRDTAGY1`-`CRDTAGY5`).** The five credit agencies are
near-identical; each reads/writes its own container on channel `CIPCREDCHANN`
(`CIPA`=CRDTAGY1, `CIPB`=CRDTAGY2, `CIPC`=CRDTAGY3, `CIPD`=CRDTAGY4,
`CIPE`=CRDTAGY5) and computes a random 1..999 credit score seeded from
`EIBTASKN`. Beyond the container name the only differences are cosmetic
(whitespace and the program name embedded in `DISPLAY` diagnostics). Each has a
driver (`tests/unit/crdtagy{1..5}Test.cbl`) asserting the score stays in range
and is deterministic under a fixed `CBSA_TEST_TASKN`; the `DELAY` is a no-op.

**Binding `DFHCOMMAREA`:** on the mainframe the CICS translator addresses
`DFHCOMMAREA` automatically, so some programs (e.g. `UPDCUST`) write
`PROCEDURE DIVISION.` with no `USING`. Off-CICS the preprocessor detects a
`01 DFHCOMMAREA` in the LINKAGE SECTION and appends `USING DFHCOMMAREA` to the
`PROCEDURE DIVISION` header so the driver's `CALL ... USING commarea` connects.

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

### Known gap — `BANKDATA` (not yet testable)

`BANKDATA` (the batch seeder for CUSTOMER + ACCOUNT) was attempted for a scoped
test but **could not be run** through this harness without building substantial
new, non-trivial infrastructure. It is deliberately **not** in
`programsUnderTest` — no fake pass. The concrete blockers:

1. **CUSTOMER via native COBOL file I/O, not `EXEC CICS`.** `BANKDATA` uses
   `SELECT ... ASSIGN TO VSAM ORGANIZATION INDEXED` with `OPEN OUTPUT` /
   `WRITE` / `CLOSE` and a `FILE STATUS`. The preprocessor never sees these
   (they aren't `EXEC CICS`), so the `CICSVSAM` double is bypassed entirely —
   verifying CUSTOMER rows would mean reading a real GnuCOBOL ISAM file, not
   the in-memory double the task asks us to reuse.
2. **`EXEC SQL` verbs the harness doesn't translate.** `BANKDATA` uses
   `INSERT INTO ACCOUNT`, `INSERT INTO CONTROL`, `DELETE FROM ACCOUNT`,
   `DELETE FROM CONTROL` and `COMMIT WORK`. `cicsPreprocessor.py` currently
   supports only SELECT/UPDATE/cursor ops and raises on the first `COMMIT WORK`.
3. **`DB2ACC` has no INSERT/DELETE, and a different host-var convention.**
   `BANKDATA`'s ACCOUNT host variables are `HV-ACCOUNT-SORT-CODE` /
   `HV-ACCOUNT-NUMBER` (hyphenated), whereas `DB2ACC`'s calling convention
   expects `HV-ACCOUNT-SORTCODE` / `HV-ACCOUNT-ACC-NO`. The `ACCOUNT` double
   also has no row-insert or row-delete op.
4. **No `CONTROL` table double exists.** `BANKDATA` writes two CONTROL rows
   (`<sortcode>-ACCOUNT-LAST`, `<sortcode>-ACCOUNT-COUNT`); a new shim +
   `HOST-CONTROL-ROW`/`CONTDB2` support would be required.
5. **LE callable services.** `TIMESTAMP` calls `CEEGMT` / `CEEDATM`, which do
   not exist off-z/OS and would need stub modules.

The RNG *is* injectable (`RANDOM-SEED` comes from the PARM), so determinism is
achievable; the blockers above are structural, not about non-determinism. A
faithful `BANKDATA` test therefore needs a batch/native-file + `INSERT`/`DELETE`
Db2 harness that is out of scope for this wave and would risk destabilising the
shared harness the other waves depend on.

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
