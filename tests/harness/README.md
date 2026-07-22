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
| `CICSVSAM` | file control: `READ`, `READ UPDATE`, `REWRITE`, `WRITE`, `STARTBR`, `READNEXT`, `READPREV`, `ENDBR` | Process-resident, keyed in-memory VSAM **KSDS** double. See "VSAM (KSDS) data layer" below. |
| `DB2ACC`   | `EXEC SQL` against `ACCOUNT`        | In-memory ACCOUNT table (see below): keyed SELECT, "last account" SELECT, cursor OPEN/FETCH/CLOSE, UPDATE; driver seeds rows and scripts `SQLCODE`. |
| `CICSBMS`  | `SEND MAP`, `RECEIVE MAP`, `SEND TEXT`, `SEND CONTROL` | Process-resident 3270 screen buffer. `RECEIVE MAP` copies the driver-preloaded input image into the program; `SEND MAP`/`SEND TEXT` capture the output image for the driver to read back. See "BMS presentation layer" below. |
| `CICSAID`  | (EIB AID/COMMAREA init, injected)   | Resident holder for the injectable `EIBAID` (which key was pressed) and `EIBCALEN`; the driver `SET`s them, `CICSINIT` reads them into the EIB at program entry. |
| `CICSRETN` | `RETURN TRANSID(..) [COMMAREA(..)]` | Records the pseudo-conversational hand-off (next transid + saved COMMAREA) so a driver can assert which transaction the screen returned to. |
| `CICSBIF`  | `BIF DEEDIT FIELD(..)`              | Strips non-digits from a field, right-justifies and zero-fills (as real CICS `DEEDIT`). |
| `CICSINQA` | `INQUIRE ASSOCIATION(..)`           | Returns canned origin data (applid/userid/facility/network/type) for the screen's COMMAREA. |
| `LINKREC`  | (behind `CICSLINK`)                 | Capture/scripting store for the single business `LINK` a screen makes: the driver `SCRIPT`s the linked program's reply COMMAREA and `GET`s back the COMMAREA the screen passed in. See "BMS presentation layer" below. |

**CICS verbs stubbed so far:** `DELAY`, `GET CONTAINER`, `PUT CONTAINER`,
`RETURN`, `LINK`, `ASSIGN` (APPLID/PROGRAM/ABCODE), `ABEND`, `ASKTIME`,
`FORMATTIME`, `HANDLE ABEND` (disabled to a no-op), `SYNCPOINT`, and the VSAM
file-control verbs `READ`, `READ UPDATE`, `REWRITE`, `WRITE`, `STARTBR`,
`READNEXT`, `READPREV`, `ENDBR`. **`EXEC SQL`** against `ACCOUNT` is also
supported (see the Db2 section below). The **BMS / 3270 presentation** verbs
`SEND MAP`, `RECEIVE MAP`, `SEND TEXT`, `SEND CONTROL`, `RETURN TRANSID(..)
COMMAREA(..)`, `BIF DEEDIT`, and `INQUIRE ASSOCIATION` are supported too (see
the BMS section below). Other verbs (`ADDRESS`, `GETMAIN`, `RETRIEVE`, `XCTL`,
…) are **not** yet stubbed — add them as needed (below).

### VSAM (KSDS) data layer — `CICSVSAM`

`CICSVSAM` is a process-resident, keyed in-memory KSDS store (same "resident
module = shared state" trick as `CICSCONT`). A test driver seeds fixture
records into it, then calls the program under test, which reads / updates /
browses the very same store. The preprocessor rewrites every file-control verb
into one uniform call:

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
| `RESET   ` | Empty the store and clear all cursor/update state. |
| `SEED    ` | Insert/replace a fixture record; the key is read from the record image (cols 5-20 = `CUSTOMER-KEY`). Pass the 259-byte record via the `record` operand and `OMITTED` for `ridfld`. |
| `FORCERSP` | Script the RESP the **next** file verb returns (value passed via the `resp` operand), to drive an arbitrary error path. |

The record image the driver seeds is the `CUSTOMER` copybook layout (259
bytes). See `tests/unit/updcustTest.cbl` and `tests/unit/inqcustTest.cbl` for
the seed / call / assert pattern, including deterministic exercise of the
`INQCUST` random-customer and last-customer browse paths via `CBSA_TEST_TASKN`.

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

### BMS presentation layer — `CICSBMS` / `CICSAID` / `CICSRETN` / `LINKREC`

The CBSA screen programs (`BNKMENU`, `BNK1CRA`, `BNK1CAC`, `BNK1TFN`, …) are
**pseudo-conversational 3270** programs: on each turn they `RECEIVE MAP` the
operator's input, branch on the AID (which key was pressed) and `EIBCALEN`
(first-time-in vs. a saved COMMAREA), do their work — usually a single business
`LINK` — then `SEND MAP` an output screen and `RETURN TRANSID(..)` to hand off
to the next transaction. The harness models one such turn end-to-end.

**Symbolic map copybooks.** BMS symbolic maps are normally generated from the
`.bms` mapset at build time. The original `.bms` sources are never modified;
instead `harness/generateSymbolicMaps.py` parses each mapset and emits an
equivalent symbolic-map copybook into `harness/copy/`:

```bash
# reads the mapsets, writes tests/harness/copy/{BNK1MAI,BNK1CDM,BNK1CAM,BNK1TFM}.cpy
python3 tests/harness/generateSymbolicMaps.py \
    src/base/bms_src/BNK1MAI.bms src/base/bms_src/BNK1CDM.bms \
    src/base/bms_src/BNK1CAM.bms src/base/bms_src/BNK1TFM.bms \
    --outDir tests/harness/copy
```

Each field `NAME` yields the standard BMS trio `NAMEL` (input length, `S9(4)
COMP`), `NAMEF`/`NAMEA` (flag/attribute) and `NAMEI` (input value); the output
map `…O` fields redefine the same storage. A program `COPY`s the map exactly as
on z/OS; a driver `COPY`s the same member to preload input and read output.

**One turn, driven from the test:**

```
CICSAID  SET   -> set EIBAID (PF key) + EIBCALEN         (driver)
CICSBMS  PUTIN -> preload the operator's input map image (driver)
LINKREC  SCRIPT-> script the business program's reply     (driver, optional)
CALL <SCREEN> USING commarea                              (invoke the program)
CICSBMS  GETOUT-> read back the output map image          (driver)
CICSRETN GET   -> read the transid it handed off to       (driver)
LINKREC  GET   -> read the COMMAREA the screen passed in  (driver)
```

At program entry `CICSINIT` seeds `EIBAID`/`EIBCALEN` from the resident
`CICSAID` holder (defaults: ENTER, length 0). `RECEIVE MAP` returns the image
the driver `PUTIN`, and every `SEND MAP`/`SEND TEXT` overwrites the buffer the
driver reads with `GETOUT`.

**Business `LINK` capture/scripting.** `CICSLINK` forwards every non-`INQCUST`
`LINK` to the resident `LINKREC` store: it captures the inbound COMMAREA and, if
the driver scripted a reply for that program, copies the reply back over the
COMMAREA. This is how a screen test asserts *"the right COMMAREA was passed to
`DBCRFUN`/`CREACC`/`XFRFUN`"* and injects the business program's response.
The presentation COMMAREA layouts are program-private WORKING-STORAGE records
(not copybooks), so each driver declares a byte-compatible mirror to build the
reply and inspect the capture. Because `LINK PROGRAM('X')` names are fixed
8-byte fields, the preprocessor space-pads short program/transid literals so the
captured name is exact (`'CREACC'` → `'CREACC  '`).

**Driver-only control ops** (never emitted by the preprocessor):

| Module     | op        | Effect |
|------------|-----------|--------|
| `CICSAID`  | `SET`     | Set `EIBAID` (an AID from `DFHAID`) and `EIBCALEN`. |
| `CICSBMS`  | `PUTIN`   | Preload the input map image `RECEIVE MAP` will return. |
| `CICSBMS`  | `GETOUT`  | Read back the last `SEND MAP`/`SEND TEXT` output image. |
| `CICSBMS`  | `RESET`   | Clear the screen buffer. |
| `CICSRETN` | `GET`     | Read the recorded next-transid + saved COMMAREA. |
| `CICSRETN` | `RESET`   | Clear the recorded hand-off. |
| `LINKREC`  | `SCRIPT`  | Register the reply COMMAREA for a linked program. |
| `LINKREC`  | `GET`     | Read the captured program name, length and COMMAREA. |
| `LINKREC`  | `RESET`   | Clear captured + scripted state. |

`DFHAID` (harness copy in `harness/copy/`) provides distinct one-byte AID values
so a driver's `CICSAID SET DFHENTER` matches the program's `EIBAID = DFHENTER`.
See `tests/unit/{bnkmenu,bnk1cra,bnk1cac,bnk1tfn}Test.cbl` for the full pattern,
including `bnk1tfnTest.cbl` which **pins the fail-code-3 fall-through** in
`BNK1TFN` (the `WHEN '3'` branch lacks the `GO TO GCD999` the other fail codes
have, so its message is immediately overwritten) as a regression test without
touching production source.

**Follow-ups (not yet covered):** the multi-turn screens `BNK1DCS`, `BNK1DAC`
and `BNK1UAC` thread a state flag through the COMMAREA across an Enter→PF-key
sequence; testing them needs the driver to invoke the screen twice, feeding the
first turn's returned COMMAREA (via `CICSRETN GET`) back into the second. `XCTL`
is also not yet stubbed (the CBSA menu uses `RETURN TRANSID`, not `XCTL`).

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
