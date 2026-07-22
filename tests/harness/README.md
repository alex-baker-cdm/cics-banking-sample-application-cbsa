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

Everything is compiled with `-fperform-osvs` (`dialectOpts` in `run.sh`) so
GnuCOBOL uses IBM OS/VS `PERFORM` semantics: a `GO TO` that branches back out
of a still-active `PERFORM` — as `XFRFUN`'s `-911` deadlock-retry loop does
(`GO TO UPDATE-ACCOUNT-DB2` from inside `PERFORM UPDATE-ACCOUNT-DB2-TO`) —
unwinds cleanly when the perform's exit point is reached, instead of GnuCOBOL
replaying the abandoned range.

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
    copy/HOSTPROC.cpy        # driver-side view of a Db2 PROCTRAN row (fixtures)
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
| `CICSLINK` | `LINK PROGRAM(..) COMMAREA(..)`     | Generalized LINK double. `PROGRAM('INQCUST ')` returns a **valid customer** (`INQCUST-INQ-SUCCESS='Y'`); force not-found with `CBSA_TEST_INQCUST_SUCCESS=N` or the `LNKINQOK` driver op. `PROGRAM('INQACCCU')` returns the customer's **account count** in `NUMBER-OF-ACCOUNTS` — `CBSA_TEST_INQACCCU_COUNT` (default = scripted-list count) / `CBSA_TEST_INQACCCU_SUCCESS` (default Y) drive the max-accounts rule, and a driver-scripted account list (`LNKADDAC`) fills the ODO account array. `PROGRAM('DELACC  ')` captures each account key passed so a driver can assert the cascade. Every other program (a screen's single business LINK — `DBCRFUN`/`CREACC`/`XFRFUN` — plus `ABNDPROC`) is forwarded to the `LINKREC` store. See "Generalized LINK — `CICSLINK`" below. |
| `CICSASYN` | `RUN TRANSID(..) CHANNEL(..) CHILD(..)`, `FETCH ANY(..)` | Deterministic **synchronous** stand-in for CICS asynchronous child transactions (CRECUST's credit checks). See "Async credit-check emulation — `CICSASYN`" below. |
| `CEEDAYS`  | LE `CEEDAYS` (date → Lilian days)   | Deterministic date-to-days stand-in (LE service unavailable off-z/OS). |
| `CEELOCT`  | LE `CEELOCT` (current local time)   | Returns a fixed Lilian/Gregorian date/time for deterministic review-date stamping. |
| `CICSASGN` | `ASSIGN APPLID/PROGRAM/ABCODE(..)`  | Returns canned values (error-path only). |
| `CICSABND` | `ABEND ABCODE(..)`                  | **Records the last abend code** in process-resident state so a driver can assert which failure path fired. The preprocessor emits `CALL 'CICSABND' … 'ABEND' abcode` **followed by `GOBACK`** so control cannot run past a translated ABEND. See "CICS abend capture" below. |
| `CICSTIME` | `ASKTIME ABSTIME(..)`               | Returns a fixed ABSTIME (error-path only). |
| `CICSFTIM` | `FORMATTIME`                        | Returns a fixed date/time (error-path only). |
| `CICSENQ`  | `ENQ RESOURCE(..)`, `DEQ RESOURCE(..)` | No-op that always reports NORMAL. A unit test runs single-process, so the serialised resource (e.g. CREACC's account named-counter) is always uncontended. See "ENQ/DEQ" below. |
| `CICSSYNC` | `SYNCPOINT [ROLLBACK]`              | No-op that always reports NORMAL. NOTE: it does **not** undo `DB2ACC`/`DB2PROC` row changes, so a rolled-back program leaves its in-memory writes in place — drive rollback/abort paths so the abort happens **before** any table write (see `xfrfunTest` TO-not-found case). |
| `CICSVSAM` | file control: `READ`, `READ UPDATE`, `REWRITE`, `WRITE`, `STARTBR`, `READNEXT`, `READPREV`, `ENDBR` | Process-resident, keyed in-memory VSAM **KSDS** double serving **multiple named files** (e.g. `CUSTOMER` + `ABNDFILE`). See "VSAM (KSDS) data layer" below. |
| `DB2ACC`   | `EXEC SQL` against `ACCOUNT`        | In-memory ACCOUNT table (see below): keyed SELECT, "last account" SELECT, cursor OPEN/FETCH/CLOSE, UPDATE, DELETE; driver seeds rows and scripts `SQLCODE`/`SQLERRD(3)`. |
| `DB2PROC`  | `EXEC SQL INSERT INTO PROCTRAN`     | In-memory PROCTRAN audit table (see below): stores inserted rows; driver reads them back to assert the audit trail and scripts the INSERT `SQLCODE`. |
| `DB2CTRL`  | `EXEC SQL` against `CONTROL`         | In-memory CONTROL named-counter table (see below): keyed SELECT + UPDATE of `CONTROL_VALUE_NUM`; driver seeds counter rows and reads them back. Used by CREACC to allocate the next account number. |
| `CICSBMS`  | `SEND MAP`, `RECEIVE MAP`, `SEND TEXT`, `SEND CONTROL` | Process-resident 3270 screen buffer. `RECEIVE MAP` copies the driver-preloaded input image into the program; `SEND MAP`/`SEND TEXT` capture the output image for the driver to read back. See "BMS presentation layer" below. |
| `CICSAID`  | (EIB AID/COMMAREA init, injected)   | Resident holder for the injectable `EIBAID` (which key was pressed) and `EIBCALEN`; the driver `SET`s them, `CICSINIT` reads them into the EIB at program entry. |
| `CICSRETN` | `RETURN TRANSID(..) [COMMAREA(..)]` | Records the pseudo-conversational hand-off (next transid + saved COMMAREA) so a driver can assert which transaction the screen returned to. |
| `CICSBIF`  | `BIF DEEDIT FIELD(..)`              | Strips non-digits from a field, right-justifies and zero-fills (as real CICS `DEEDIT`). |
| `CICSINQA` | `INQUIRE ASSOCIATION(..)`           | Returns canned origin data (applid/userid/facility/network/type) for the screen's COMMAREA. |
| `LINKREC`  | (behind `CICSLINK`)                 | Capture/scripting store for the single business `LINK` a screen makes: the driver `SCRIPT`s the linked program's reply COMMAREA and `GET`s back the COMMAREA the screen passed in. See "BMS presentation layer" below. |

**CICS verbs stubbed so far:** `DELAY`, `GET CONTAINER`, `PUT CONTAINER`,
`RETURN`, `LINK`, `ASSIGN` (APPLID/PROGRAM/ABCODE), `ABEND`, `ASKTIME`,
`FORMATTIME`, `HANDLE ABEND` (disabled to a no-op), `SYNCPOINT`, `ENQ`, `DEQ`,
`RUN TRANSID ... CHANNEL ... CHILD`, `FETCH ANY`, and the VSAM file-control
verbs `READ`, `READ UPDATE`, `REWRITE`, `WRITE`, `DELETE`, `STARTBR`,
`READNEXT`, `READPREV`, `ENDBR`. **`EXEC SQL`** against `ACCOUNT`
(SELECT / UPDATE / DELETE / cursor / INSERT), `CONTROL` (keyed SELECT / UPDATE),
and `INSERT INTO PROCTRAN` are also supported (see the Db2 sections below). The
**BMS / 3270 presentation** verbs `SEND MAP`, `RECEIVE MAP`, `SEND TEXT`,
`SEND CONTROL`, `RETURN TRANSID(..) COMMAREA(..)`, `BIF DEEDIT`, and
`INQUIRE ASSOCIATION` are supported too (see the BMS section below). Other verbs
(`ADDRESS`, `GETMAIN`, `RETRIEVE`, `XCTL`, …) are **not** yet stubbed — add them
as needed (below).

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
     BY REFERENCE sortcode accno custno HOST-ACCOUNT-ROW SQLCA
```

`HOST-ACCOUNT-ROW` is the program's row group (12 ACCOUNT columns); the whole
`SQLCA` group is passed (not just `SQLCODE`) so the double can also report
`SQLERRD(3)` — the reason token XFRFUN inspects for its deadlock retry.
`INCLUDE SQLCA` is rewritten to `COPY SQLCA.`; `INCLUDE <table>` (e.g. `ACCDB2`)
is dropped (declarative only).

**SQL subset covered** (what UPDACC / INQACC / INQACCCU / DELACC / DBCRFUN /
XFRFUN use):

| Statement                                              | `op`     |
|--------------------------------------------------------|----------|
| `SELECT … INTO … WHERE sortcode AND accno`             | `SELKEY` |
| `SELECT … ORDER BY ACCOUNT_NUMBER DESC FETCH FIRST 1`  | `SELMAX` |
| `UPDATE ACCOUNT SET <all columns> WHERE key`           | `UPDATE` |
| `DELETE FROM ACCOUNT WHERE sortcode AND accno`         | `DELKEY` |
| `DECLARE CURSOR` + `OPEN` (keyed by sortcode+accno)    | `OPENA`  |
| `DECLARE CURSOR` + `OPEN` (keyed by custno+sortcode)   | `OPENC`  |
| `FETCH FROM cursor INTO …` / `CLOSE cursor`            | `FETCH` / `CLOSE` |

`SQLCODE` follows Db2 conventions: `0` found / `+100` not-found / `<0` error.
`UPDATE` replaces the **whole** matched row from the host variables (real SQL
UPDATE sets every listed column), so the money-movement programs' balance
writes persist and can be re-`SELKEY`ed. `DELKEY` removes the matched row
(`+100` if none). Cursor `OPEN` mode is inferred from the `DECLARE CURSOR`
WHERE columns.

**Driver-facing operations** (seed/inspect the table from a test):

| `op`     | Effect |
|----------|--------|
| `CLEAR`  | Empty the table, reset cursor + scripted-error state. |
| `INSERT` | Append `HOST-ACCOUNT-ROW` as a fixture row. |
| `SETSQL` | Force the caller's `SQLCODE` **and** `SQLERRD(3)` on the **next** data op (drives error / deadlock paths). |
| `SELKEY` | Re-SELECT a row for assertions. |

To script a Db2 deadlock for XFRFUN's retry loop, move the reason values into
the driver's SQLCA before `SETSQL` — `SQLCODE = -911` and
`SQLERRD(3) = 13172872` — then call the program; the forced pair is applied to
the next `SELKEY`/`UPDATE` only (one-shot). Normal data ops zero `SQLERRD(3)`.

Include `copy/HOSTACCT.cpy` under an `01` in the driver to build/inspect rows;
it is byte-identical to `HOST-ACCOUNT-ROW`. Account/date fixture conventions:
keys are fixed-width numeric strings (sort code `"987654"`, account `"00000001"`,
customer `"0000000001"`), dates are `"YYYY-MM-DD"`. See `tests/unit/updaccTest.cbl`,
`inqaccTest.cbl`, `inqacccuTest.cbl`, `delaccTest.cbl`.

### EXEC SQL / Db2 PROCTRAN audit double — `DB2PROC`

`DB2PROC` is a sibling in-memory table for the **PROCTRAN** processed-transaction
audit log. The account-mutating programs (`DELACC`, `DBCRFUN`, `XFRFUN`) write
one audit row per business event; `DB2PROC` records those rows so a driver can
assert the audit trail. The preprocessor maps `INSERT INTO PROCTRAN` to:

```
CALL 'DB2PROC' USING BY CONTENT  <op>
     BY REFERENCE HOST-PROCTRAN-ROW SQLCA
```

`HOST-PROCTRAN-ROW` is the program's PROCTRAN host group (eyecatcher, sortcode,
account, date, time, ref, type, description, amount). `copy/HOSTPROC.cpy` is a
byte-identical driver-side view for building/inspecting rows.

| `op`      | Effect |
|-----------|--------|
| `INSERT`  | Append `HOST-PROCTRAN-ROW` (unless a scripted error is pending). |
| `CLEAR`   | Empty the table and clear scripted-error state (driver). |
| `SETSQL`  | Force `SQLCODE` on the next `INSERT`; a failed insert leaves the table unchanged (driver). |
| `COUNT`   | Return the current row count in `SQLERRD(1)` (driver). |
| `GETLAST` | Copy the most-recently inserted row into `HOST-PROCTRAN-ROW` (driver). |
| `GETFRST` | Copy the first inserted row into `HOST-PROCTRAN-ROW` (driver). |

Typical assertion pattern: `CLEAR` both tables, seed ACCOUNT rows, call the
program, then `COUNT` + `GETLAST` on `DB2PROC` to check the audit row's `type`
(`ODA` account-delete, `CRE`/`DEB` credit/debit, `TFR` transfer) and `amount`.
See `tests/unit/delaccTest.cbl`, `dbcrfunTest.cbl`, `xfrfunTest.cbl`.

### EXEC SQL / Db2 CONTROL named-counter double — `DB2CTRL`

`DB2CTRL` is a sibling in-memory table for the **CONTROL** table, which holds
CBSA's named-counter rows keyed on `CONTROL_NAME`. `CREACC` allocates the next
account number from two rows — `<sortcode>-ACCOUNT-LAST` (the last account
number issued) and `<sortcode>-ACCOUNT-COUNT` (how many accounts exist) — by
`SELECT`ing a row, incrementing `CONTROL_VALUE_NUM`, and `UPDATE`ing it back
(bracketed by `ENQ`/`DEQ`). The preprocessor maps those `CONTROL` statements to:

```
CALL 'DB2CTRL' USING BY CONTENT  <op>
     BY REFERENCE HOST-CONTROL-ROW SQLCA
```

`HOST-CONTROL-ROW` is the program's three-field CONTROL host group
(`CONTROL_NAME` X(32), `CONTROL_VALUE_NUM` S9(9) COMP, `CONTROL_VALUE_STR`
X(40)). `copy/HOSTCTRL.cpy` is a byte-identical driver-side view for seeding
and inspecting rows.

| `op`     | Effect |
|----------|--------|
| `SELKEY` | `SELECT .. INTO row WHERE CONTROL_NAME` — `SQLCODE` 0 (found) / +100 (not found). |
| `UPDATE` | `UPDATE CONTROL SET CONTROL_VALUE_NUM WHERE CONTROL_NAME` — 0 / +100. |
| `SEED`   | Insert/replace a CONTROL row keyed on `CONTROL_NAME`, from `row` (driver). |
| `GETVAL` | Copy the row matching `row`'s `CONTROL_NAME` back into `row` for read-back (driver). |
| `CLEAR`  | Empty the table and clear scripted-error state (driver). |
| `SETSQL` | Force `SQLCODE` on the next `SELKEY`/`UPDATE` (driver). |

Typical pattern: `CLEAR`, then `SEED` the counter rows (e.g. `ACCOUNT-LAST`=10,
`ACCOUNT-COUNT`=5), call the program, then `GETVAL` each row to assert the
counters advanced. See `tests/unit/creaccTest.cbl`.

### ENQ / DEQ serialisation — `CICSENQ`

On the mainframe `CREACC` brackets its named-counter update with
`EXEC CICS ENQ RESOURCE(..)` / `DEQ RESOURCE(..)` so concurrent tasks serialise.
A unit test runs single-process, so the resource is always uncontended: the
preprocessor routes both verbs to `CICSENQ`, a no-op that always reports NORMAL
(`RESP`/`RESP2` = 0). The signature carries the verb name for diagnostics:

```
CALL 'CICSENQ' USING BY CONTENT  <'ENQ'|'DEQ'>
     BY REFERENCE resp|OMITTED resp2|OMITTED
```

### CICS abend capture — `CICSABND`

Failure paths in these programs `EXEC CICS ABEND ABCODE(xxxx)` instead of
returning. The preprocessor translates that to a `CALL 'CICSABND'` that stores
the code, immediately followed by `GOBACK` so nothing runs past the abend. The
code is held in process-resident state, and the driver reads it with these
control ops (signature `USING op abcode flag`):

| `op`    | Effect |
|---------|--------|
| `READ`  | Return the last abend `abcode` (`X(4)`) and a `flag` (`Y` if an abend fired since the last reset). |
| `RESET` | Clear the stored code and set `flag` to `N` (call before the program to isolate the assertion). |

So a driver asserts a failure path by `RESET`-ing, calling the program, then
`READ`-ing and comparing the code — e.g. `SAME` (transfer to same account),
`FROM`/`TO  ` (account update failed), `HROL` (rollback failed), `WPCD`
(PROCTRAN write failed), `HWPT` (DELACC PROCTRAN write failed). See
`tests/unit/xfrfunTest.cbl` for the `SAME` capture.

### Generalized LINK — `CICSLINK`

The customer orchestrators `LINK` to three sub-programs. `CICSLINK` emulates all
three from one module (dispatched on the `PROGRAM` name it is passed) and lets a
driver both **script** the results and **inspect** what was passed:

- **`INQCUST`** — validates a customer exists. Returns success by default;
  a driver forces not-found either with `CBSA_TEST_INQCUST_SUCCESS=N` or the
  `LNKINQOK` control op (so a single run can flip it per sub-test).
- **`INQACCCU`** — enumerates a customer's accounts. The double fills the
  program's ODO account array (`NUMBER-OF-ACCOUNTS` + `ACCOUNT-DETAILS`) from a
  driver-scripted list of account numbers (each carrying the fixed sort code
  `987654`).
- **`DELACC`** — deletes one account. The double **captures the account number**
  from each COMMAREA it is handed, in call order, so the driver can assert the
  cascade fired once per account with the right keys.

Driver-only control ops (signature `USING op ctrl`, where `ctrl` is a small
group `{inqOk PIC X, nAcc PIC 9(4), idx PIC 9(4), accNo PIC 9(8)}`):

| op         | Effect |
|------------|--------|
| `LNKRESET` | Clear the scripted account list, the DELACC captures, and INQCUST state. |
| `LNKADDAC` | Append `accNo` to the scripted `INQACCCU` account list. |
| `LNKINQOK` | Set INQCUST success to `inqOk` (`Y`/`N`). |
| `LNKGETCN` | Return the DELACC capture count in `nAcc`. |
| `LNKGETAC` | Return the captured account number at 1-based `idx` in `accNo`. |

See `tests/unit/delcusTest.cbl`: it scripts three accounts, calls `DELCUS`, then
asserts `LNKGETCN`=3 and each `LNKGETAC` matches the scripted key in order, that
the CUSTOMER record is gone, and that one `ODC` PROCTRAN row was written. The
customer-not-found edge (`LNKINQOK` with `N`) asserts **nothing** was deleted
and no audit row written.

### Async credit-check emulation — `CICSASYN`

CRECUST runs its five credit-agency checks (`CRDTAGY1`-`5`) **asynchronously**:
for each child it `PUT`s a container on channel `CIPCREDCHANN`, then
`EXEC CICS RUN TRANSID(OCRn) CHANNEL(..) CHILD(token)` to start the child
transaction, and later `EXEC CICS FETCH ANY(..)` in a loop to gather the
completed children and average their scores.

**Off-mainframe simplification (documented).** Genuine asynchronous child tasks
cannot be reproduced in a single GnuCOBOL run unit, so `CICSASYN` emulates the
whole thing **synchronously and deterministically**:

- On `RUN`, it takes the child container that CRECUST just `PUT` on the channel
  (via `CICSCONT`), **overlays a fixed scripted credit score** into the child
  record (offset 249, `PIC 999`), puts it back, and queues a completion token.
- On `FETCH ANY`, it returns the queued children **in issue order** with
  `COMPSTATUS = DFHVALUE(NORMAL)`; when the queue drains it returns `NOTFND`
  with `RESP2=1`, ending CRECUST's fetch loop exactly as real CICS would.

Because the scores are driver-supplied, the aggregate CRECUST computes is fully
deterministic. Driver-only control ops (signature matches the emitted
`RUN`/`FETCH` call, `op` in the first operand):

| op         | Effect |
|------------|--------|
| `RESET`    | Clear scripted scores and the completion queue. |
| `SETSCORE` | Script the score a given child (1..5, via the transid slot) returns. |
| `SETNORPL` | Suppress a child's reply (it never appears in `FETCH`), to drive the "fewer replies than started" path. |

`tests/unit/crecustTest.cbl` scripts scores `100/200/300/400/500` and asserts
CRECUST stores their average `300`, that the next customer number is allocated
from the seeded CUSTOMER **control record** (`READ UPDATE`+`REWRITE`, so it is
incremented `100`→`101` and count `5`→`6`), that the new CUSTOMER record is
written, and that one `OCC` PROCTRAN row is produced. The edge test feeds an
invalid date of birth (year `1500`) and asserts `COMM-SUCCESS='N'`, fail code
`O`, and that **no** CUSTOMER record / audit row / control-record change
resulted.

### Locking + LE date services — off-mainframe simplifications

- **`CICSENQ` (`ENQ`/`DEQ`).** CRECUST serializes named-counter allocation with
  `ENQ`/`DEQ`. Off-CICS everything runs in one run unit, so the double is a
  no-op returning `resp=0` — the allocation logic is unchanged, just
  uncontended.
- **`CEEDAYS` / `CEELOCT` (Language Environment).** CRECUST calls these LE
  callable services to compute the credit-score review date. They do not exist
  off-z/OS, so deterministic stand-ins return fixed values (`CEELOCT` a fixed
  "today", `CEEDAYS` a monotonic day count), keeping the review-date stamping
  deterministic. A test-only copy of `CEEIGZCT` (the LE feedback-code copybook)
  lives under `tests/harness/copy/` because it is not in the product copy tree;
  the include path is `-I tests/harness/copy -I src/base/cobol_copy`, so
  test-only copybooks resolve first without touching the product ones.

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
| `CBSA_TEST_INQACCCU_COUNT`  | `1` | Account count the `CICSLINK` INQACCCU stub returns (drive the max-accounts rule; set `10` for at-limit). |
| `CBSA_TEST_INQACCCU_SUCCESS`| `Y` | `N` makes the `CICSLINK` INQACCCU stub report failure (`COMM-SUCCESS='N'`). |
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
