# Db2 Table `CONTROL` — Exhaustive Access-Pattern Analysis (CBSA)

Repository: `alex-baker-cdm/cics-banking-sample-application-cbsa`
Scope: every access to the Db2 table **CONTROL** across COBOL, JCL/DDL and Java (z/OS Connect / Liberty webui).

---

## 1. DDL — exact definition

Source of truth: `etc/install/base/db2jcl/INSTDB2.jcl` (lines 89–103), duplicated per-object in
`CRESG03.jcl` (STOGROUP), `CRETS03.jcl` (TABLESPACE), `CRETB03.jcl` (TABLE), `CREI301.jcl` (INDEX).
The in-program `DECLARE TABLE` is `src/base/cobol_copy/CONTDB2.cpy`.

```sql
CREATE STOGROUP CONTROL VOLUMES('*','*','*','*','*') VCAT DSNV12DP;
CREATE TABLESPACE CONTROL IN CBSA USING STOGROUP CONTROL;

CREATE TABLE CONTROL (
    CONTROL_NAME       CHAR(32),      -- key/name of the counter or value
    CONTROL_VALUE_NUM  INTEGER,       -- numeric value (counter / next number)
    CONTROL_VALUE_STR  CHAR(40)       -- optional string value (unused in practice)
)
IN CBSA.CONTROL NOT VOLATILE CARDINALITY AUDIT NONE DATA CAPTURE NONE;

CREATE UNIQUE INDEX CONTINDX ON CONTROL(CONTROL_NAME) USING STOGROUP CONTROL;
```

`CONTDB2.cpy` DECLARE adds nullability nuance:

```
CONTROL_NAME       CHAR(32) NOT NULL,
CONTROL_VALUE_NUM  INTEGER,          -- nullable
CONTROL_VALUE_STR  CHAR(40)          -- nullable
```

| Column | Type | Nullable | Notes |
|---|---|---|---|
| `CONTROL_NAME` | CHAR(32) | NOT NULL (per DECLARE) | Logical primary key. Fixed 32-char, space-padded. Composite semantics encoded as a string: `"<sortcode>-<KEY>"`. |
| `CONTROL_VALUE_NUM` | INTEGER | Yes | The actual counter / last-used value. |
| `CONTROL_VALUE_STR` | CHAR(40) | Yes | Space-padded; always written as SPACES by every writer. Effectively dead. |

**Keys / indexes**
- No `PRIMARY KEY` declared. Uniqueness is enforced only by **`CONTINDX` = UNIQUE INDEX on `CONTROL_NAME`** — this is the de-facto primary key.
- No foreign keys. No other indexes.

---

## 2. Every accessor of the CONTROL table

There are exactly **three** code artifacts that touch the Db2 `CONTROL` table. All predicates are equality on the unique key `CONTROL_NAME`; there are no range scans, joins or cursors.

### 2.1 `src/base/cobol_src/CREACC.cbl` — Open Account (online/CICS)
Host vars (`HOST-CONTROL-ROW`, lines 110–113): `HV-CONTROL-NAME PIC X(32)`, `HV-CONTROL-VALUE-NUM PIC S9(9) COMP`, `HV-CONTROL-VALUE-STR PIC X(40)`.

Section `FIND-NEXT-ACCOUNT` does two SELECT+UPDATE pairs:

1. **`<sortcode>-ACCOUNT-LAST`** (lines 437–533) — generate the next account number.
   - `SELECT CONTROL_NAME, CONTROL_VALUE_NUM, CONTROL_VALUE_STR INTO ... FROM CONTROL WHERE CONTROL_NAME = :HV-CONTROL-NAME` (singleton SELECT). `CONTROL_NAME` built by `STRING REQUIRED-SORT-CODE '-' 'ACCOUNT-LAST'`.
   - `+1`, used as the new account number; then `UPDATE CONTROL SET CONTROL_VALUE_NUM = :HV WHERE CONTROL_NAME = :HV`.
   - On any non-zero SQLCODE → CICS ABEND `HNCS` (fatal). No `FOR UPDATE`/row lock between SELECT and UPDATE.
2. **`<sortcode>-ACCOUNT-COUNT`** (lines 604–698) — increment the count of accounts for the sort code. Same SELECT then `UPDATE ... SET CONTROL_VALUE_NUM = value+1`.

Reads: `CONTROL_VALUE_NUM` (both rows). Writes: `CONTROL_VALUE_NUM` (both rows). `CONTROL_VALUE_STR` read but ignored.
Business operation: **open a new bank account** (account-number allocation + running account count).

### 2.2 `src/webui/.../web/db2/Account.java` — Open Account (web / z/OS Connect / REST)
Method `createAccount` (lines ~725–800). JDBC equivalent of CREACC's account-number step, but **only** the `ACCOUNT-LAST` counter (it does **not** maintain `ACCOUNT-COUNT`).
- `SELECT * from CONTROL where CONTROL_NAME = ?` with `? = "<paddedSortCode>-ACCOUNT-LAST"` (sort code zero-padded to 6 via `padSortCode`).
- Reads `CONTROL_VALUE_NUM` (`rs.getLong`), `+1`, pads to 8 digits (`padAccountNumber`) → account number.
- `UPDATE CONTROL SET CONTROL_VALUE_NUM = ? WHERE CONTROL_NAME = ?`.
- Then inserts the new ACCOUNT row.
Note (line 736): a first `controlString`/`sqlControl` is built with the *unpadded* `sortcode.toString()` and then immediately overwritten with the padded form (dead assignment / latent bug).
Business operation: **open a new bank account via the modern web/REST channel**.

### 2.3 `src/base/cobol_src/BANKDATA.cbl` — Bulk data generator / initializer
Host vars identical to CREACC (lines 337–340); pulls `CONTDB2` DECLARE (line 333).

- **INSERT** two rows after generating the sample population (lines 605–662):
  - `<sortcode>-ACCOUNT-LAST`  ← `CONTROL_VALUE_NUM = LAST-ACCOUNT-NUMBER`
  - `<sortcode>-ACCOUNT-COUNT` ← `CONTROL_VALUE_NUM = NUMBER-OF-ACCOUNTS`
  - `CONTROL_VALUE_STR` set to SPACES in both.
- **DELETE** the same two rows during teardown/reload (lines 1256–1259 and 1322–1325): `DELETE FROM CONTROL WHERE CONTROL_NAME = :HV-CONTROL-NAME` for `ACCOUNT-LAST` then `ACCOUNT-COUNT`. Tolerates SQLCODE 0 and +100 (not-found); anything else aborts.

Business operation: **seed / reset the environment** (create the initial CONTROL counters, or drop them on regeneration).

---

## 3. Enumerated `CONTROL_NAME` values actually used

Only **two logical keys** exist, both per sort code (so N rows for N sort codes; the demo uses a single sort code):

| `CONTROL_NAME` pattern | `CONTROL_VALUE_NUM` meaning | Written by | Read by |
|---|---|---|---|
| `"<6-digit sortcode>-ACCOUNT-LAST"` | Last/highest account number issued for the sort code (next = value+1) | BANKDATA (INSERT/DELETE), CREACC (UPDATE), Account.java (UPDATE) | CREACC, Account.java |
| `"<6-digit sortcode>-ACCOUNT-COUNT"` | Number of accounts currently held for the sort code | BANKDATA (INSERT/DELETE), CREACC (UPDATE) | CREACC |

Example literal: `000001-ACCOUNT-LAST`.
`CONTROL_VALUE_STR` is **never** meaningfully populated — always SPACES.

**Not stored in the Db2 CONTROL table (important):** customer numbering and counts. Those live in the **VSAM CUSTOMER KSDS control record** (eyecatcher `CTRL`, key = sortcode + `9999999999`), see `CUSTCTRL.cpy` / `CustomerControl.java` (`NUMBER-OF-CUSTOMERS`, `LAST-CUSTOMER-NUMBER`). CRECUST allocates customer numbers via a **CICS Named Counter** + that VSAM control record, not via the CONTROL table. The `ACCTCTRL.cpy` group (`NUMBER-OF-ACCOUNTS`, `LAST-ACCOUNT-NUMBER`, eyecatcher `CTRL`) is the analogous VSAM-style layout but is only referenced structurally (e.g. declared in DELACC) — it is **not** the Db2 CONTROL table.

**Orphan copybook:** `src/base/cobol_copy/CONTROLI.cpy` defines `CONTROL-CUSTOMER-COUNT/LAST` and `CONTROL-ACCOUNT-COUNT/LAST` (packed decimal) but is **not included by any program** — a vestige of an intended richer/typed control schema that was never used.

---

## 4. Keys & foreign-key-like relationships

- **Logical PK / unique key:** `CONTROL_NAME` (via `CONTINDX`).
- **Embedded composite key:** `CONTROL_NAME` concatenates `SORTCODE` (6) + `-` + counter kind. The sort code is duplicated as a string prefix in every row rather than being a real column — a denormalization.
- **Soft relationships (not enforced by any DB constraint, only by COBOL/Java convention):**
  - `CONTROL."<sc>-ACCOUNT-LAST"` seeds `ACCOUNT.ACCOUNT_NUMBER` (+ `ACCOUNT.ACCOUNT_SORTCODE`) at account creation.
  - `CONTROL."<sc>-ACCOUNT-COUNT"` should equal `COUNT(*) FROM ACCOUNT WHERE ACCOUNT_SORTCODE = <sc>` but is not transactionally guaranteed.
- Contrast with the wider schema (context, not this table): `ACCOUNT.ACCOUNT_CUSTOMER_NUMBER` → CUSTOMER (VSAM); `PROCTRAN` → ACCOUNT via `PROCTRAN_SORTCODE` + `PROCTRAN_NUMBER`. All composite on `SORTCODE + NUMBER`, none enforced as Db2 FKs.

---

## 5. Referential-integrity / business rules enforced in code (should become DB constraints)

1. `ACCOUNT-LAST` must exist before any account is opened — CREACC/Account.java treat a missing row as fatal (ABEND / error). In Postgres this is naturally handled by a **sequence** that always exists.
2. Account number = `ACCOUNT-LAST + 1`, then the counter is bumped — a read-modify-write with **no locking / `FOR UPDATE`** between SELECT and UPDATE (race window under concurrency). A sequence removes this hazard.
3. `ACCOUNT-COUNT` is incremented on create by CREACC but is **not decremented by DELACC** (account deletion does not touch CONTROL) and is **not maintained at all by the Java path** → the counter drifts and is unreliable. Should be a derived `COUNT(*)`/materialized view, not a stored counter.
4. Sort code embedded in `CONTROL_NAME` must match the account's sort code — convention only.

---

## 6. Data-quality / modelling issues for a Postgres migration

- **Generic key/value/counter table (anti-pattern):** `CONTROL` is an EAV-style bag. In Postgres each logical counter should become a dedicated **`SEQUENCE`** (e.g. `account_number_seq` per sort code, or a single sequence) rather than a hand-incremented row.
- **`ACCOUNT-LAST` → sequence.** `ACCOUNT-COUNT` → **drop the stored counter**; compute via `COUNT(*)`/view. This eliminates the drift and the missing-decrement bug.
- **Composite key smуggled into a string.** `CONTROL_NAME = "<sortcode>-<kind>"` should be decomposed into typed columns (`sort_code INT`, `counter_kind`) or, better, dissolved into sequences/derived values.
- **Fixed CHAR padding.** `CONTROL_NAME CHAR(32)` and `CONTROL_VALUE_STR CHAR(40)` are blank-padded fixed width; sort code prefix is zero-padded to 6 in Java (`padSortCode`) but written unpadded/`REQUIRED-SORT-CODE`-formatted in COBOL — **inconsistent padding across channels** can cause key-mismatch. Use `varchar`/`text` and trim; or drop entirely per above.
- **Dead column.** `CONTROL_VALUE_STR` is never populated (always SPACES) — do not carry it forward.
- **Numeric width mismatch.** Column is `INTEGER`; COBOL host var is `S9(9) COMP`; Java reads it as `Long` then narrows to `int` (comment even notes "up to 10 digits" vs 8-digit account numbers) — a latent overflow/narrowing smell. A `bigint` sequence sidesteps it.
- **No PK, uniqueness only via a separately-created UNIQUE INDEX.** Add an explicit `PRIMARY KEY` (or make the whole table obsolete).
- **No FKs / no constraints** linking counters to ACCOUNT; integrity is code-enforced and already inconsistent between the COBOL and Java paths.
- **Sort-code duplication** across every CONTROL row and across ACCOUNT/PROCTRAN — normalize sort code into its own reference table if it must be modelled.
- **Concurrency:** hand-rolled read-increment-write with no row locking; migrate to sequences (atomic) to be correct under load.

---

## 7. Summary

The Db2 `CONTROL` table is a tiny, generic 3-column key/value/counter store whose only real content is two per-sort-code integer counters — `<sortcode>-ACCOUNT-LAST` (next account number) and `<sortcode>-ACCOUNT-COUNT` (account count). It is written by the bulk loader `BANKDATA` (INSERT/DELETE) and by the two "open account" paths, `CREACC.cbl` (SELECT+UPDATE both keys) and the web `Account.java` (SELECT+UPDATE `ACCOUNT-LAST` only). Every access is an equality lookup on the unique `CONTROL_NAME` index; there are no cursors, joins or FKs. Customer numbering is deliberately **not** here (it lives in the VSAM CUSTOMER control record + a CICS named counter), and the `CONTROLI.cpy` copybook hints at a richer design that was never wired up. For Postgres, `ACCOUNT-LAST` should become a `SEQUENCE`, `ACCOUNT-COUNT` should be dropped in favour of a derived count (fixing the delete-doesn't-decrement and web-doesn't-maintain bugs), and the whole EAV table with its stringly-typed composite key, dead `CONTROL_VALUE_STR`, CHAR padding and lock-free read-modify-write should be retired.
