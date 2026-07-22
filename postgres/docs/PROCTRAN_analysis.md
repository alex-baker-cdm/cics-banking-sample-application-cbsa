# PROCTRAN Table — Access-Pattern & Migration Analysis

Repository: `alex-baker-cdm/cics-banking-sample-application-cbsa` (IBM CICS Banking Sample App / CBSA)

**PROCTRAN** = "Processed Transactions" — an **append-only journal / audit log** of every
posted banking event (debits, credits, transfers, and account/customer create/delete audit
records). Every business program that changes money or the account/customer population writes
exactly one PROCTRAN row per event. **No program ever UPDATEs or DELETEs a PROCTRAN row** —
even account/customer deletions are recorded as *new* PROCTRAN insert records.

---

## 1. DDL — Definitive Table Definition

Source of truth: `etc/install/base/db2jcl/INSTDB2.jcl` (lines 70–87), duplicated in
`etc/install/base/db2jcl/CRETB02.jcl` (with schema `IBMUSER.`).

```sql
CREATE STOGROUP  PROCTRAN VOLUMES('*',...) VCAT DSNV12DP;
CREATE TABLESPACE PROCTRAN IN CBSA USING STOGROUP PROCTRAN;

CREATE TABLE PROCTRAN (
    PROCTRAN_EYECATCHER   CHAR(4),            -- always 'PRTR' (see eyecatcher below)
    PROCTRAN_SORTCODE     CHAR(6)  NOT NULL,  -- bank sort code (branch id)
    PROCTRAN_NUMBER       CHAR(8)  NOT NULL,  -- account number
    PROCTRAN_DATE         DATE,               -- posting date
    PROCTRAN_TIME         CHAR(6),            -- HHMMSS as text
    PROCTRAN_REF          CHAR(12),           -- reference = CICS task number (EIBTASKN)
    PROCTRAN_TYPE         CHAR(3),            -- transaction type code (see §4)
    PROCTRAN_DESC         CHAR(40),           -- free-form / structured description (overloaded)
    PROCTRAN_AMOUNT       DECIMAL(12, 2)      -- signed monetary amount
)
IN CBSA.PROCTRAN NOT VOLATILE CARDINALITY AUDIT NONE DATA CAPTURE NONE;
```

### Keys / Indexes — **NONE**
- **There is NO primary key, NO unique index, and NO index of any kind on PROCTRAN.**
  In `INSTDB2.jcl`, `ACCOUNT` gets `ACCTINDX` (UNIQUE on SORTCODE+NUMBER) and `ACCTCUST`, and
  `CONTROL` gets `CONTINDX` (UNIQUE on CONTROL_NAME). The standalone index jobs `CREI101`
  (ACCTINDX), `CREI201` (ACCTCUST) and `CREI301` (CONTINDX) exist — but **there is no
  `CREIx` job for PROCTRAN**. It is a pure heap table.
- Only `PROCTRAN_SORTCODE` and `PROCTRAN_NUMBER` are `NOT NULL`; all other columns are nullable.

### COBOL host-variable declaration vs. installed DDL — **type mismatch**
`src/base/cobol_copy/PROCDB2.cpy` (`EXEC SQL DECLARE PROCTRAN TABLE`) declares
`PROCTRAN_DATE CHAR(8)` and `PROCTRAN_AMOUNT DECIMAL(12,2)`, whereas the installed table
(`INSTDB2.jcl`) uses `PROCTRAN_DATE DATE`. The record copybook `PROCTRAN.cpy`
stores the date as `PIC 9(8)` (YYYYMMDD). So the same column is modelled three ways
(DB2 `DATE`, SQL-declare `CHAR(8)`, COBOL `9(8)`). Migration must pick one (recommend real `DATE`).

### Record copybook (`src/base/cobol_copy/PROCTRAN.cpy`)
Defines the in-memory layout and the many `REDEFINES` of the 40-char DESC field:
- `PROC-TRAN-EYE-CATCHER PIC X(4)` — 88 `PROC-TRAN-VALID VALUE 'PRTR'`.
  Redefined as `PROC-TRAN-LOGICAL-DELETE-FLAG` with 88 `PROC-TRAN-LOGICALLY-DELETED VALUE X'FF'`
  (a logical-delete convention carried over from the VSAM design — **not actually used** for
  PROCTRAN since rows are never deleted).
- `PROC-TRAN-ID` = `PROC-TRAN-SORT-CODE PIC 9(6)` + `PROC-TRAN-NUMBER PIC 9(8)`.
- `PROC-TRAN-DATE 9(8)` (+ YYYY/MM/DD redefine), `PROC-TRAN-TIME 9(6)` (+ HH/MM/SS redefine).
- `PROC-TRAN-REF 9(12)`, `PROC-TRAN-TYPE X(3)`, `PROC-TRAN-DESC X(40)`, `PROC-TRAN-AMOUNT S9(10)V99`.

---

## 2. Access Inventory — Every Reference in the Codebase

`PROCTRAN` is referenced in **27 files**. Only these perform actual SQL against the table:

### Writers (COBOL `EXEC SQL INSERT` — one INSERT statement each, identical column list)
| Program | Business op | TYPE code(s) written | DESC content | AMOUNT | Key/predicate |
|---|---|---|---|---|---|
| `DBCRFUN.cbl` | Debit / Credit at counter, and payment debit/credit | `DEB`, `CRE`, `PDR`, `PCR` | `'COUNTER WTHDRW'`, `'COUNTER RECVED'`, or payment origin text | `COMM-AMT` | INSERT (no predicate) |
| `XFRFUN.cbl` | Transfer funds between two local accounts | `TFR` | `PROC-TRAN-DESC-XFR`: `'TRANSFER'`+ target SORTCODE(6)+ACCOUNT(8) | `COMM-AMT` | INSERT — writes **two** rows (debit-side + credit-side) |
| `CREACC.cbl` | Open account | `'OCA'` (hard-coded) | CUSTNO(10)+ACCTYPE(8)+LAST-STMT(8)+NEXT-STMT(8)+spaces | `0` | INSERT |
| `CRECUST.cbl` | Create customer | `'OCC'` (hard-coded) | SORTCODE(6)+CUSTNO(10)+NAME(14)+DOB(10) | `0` | INSERT |
| `DELACC.cbl` | Close/delete account | `'ODA'` (via `PROC-TY-BRANCH-DELETE-ACCOUNT`) | built from `PROC-TRAN-DESC-DELACC` (custno, acctype, last/next stmt dates) | account actual balance | INSERT |
| `DELCUS.cbl` | Delete customer | `'ODC'` (hard-coded) | SORTCODE(6)+CUSTNO(10)+NAME(14)+DOB(10) | `0` | INSERT |

Notes on writers:
- `PROCTRAN_REF` in **all** COBOL writers = `WS-EIBTASKN12` (the CICS task number, `EIBTASKN`),
  **not** a global sequence. It is only unique within a CICS run, not a durable surrogate key.
- `PROCTRAN_SORTCODE`/`PROCTRAN_NUMBER` come from the account being posted.
- `DELACC`/`XFRFUN` build the row in the `PROCTRAN-AREA` copybook structure then move whole
  fields to host variables; the others populate host variables field-by-field.
- `ABNDPROC.cbl` and `UPDCUST.cbl` mention PROCTRAN only in comments/working-storage
  (`UPDCUST` explicitly documents that customer field updates do **not** write PROCTRAN);
  no SQL against the table.

### Readers / Writers (Java, z/OS Connect / Liberty web layer)
`src/webui/src/main/java/com/ibm/cics/cip/bankliberty/web/db2/ProcessedTransaction.java`:
- **INSERT** (`SQL_INSERT`, constant, one statement reused by all write* methods):
  `INSERT INTO PROCTRAN (...9 cols...) VALUES (?,?,?,?,?,?,?,?,?)`.
  Methods: `writeDebit` (TYPE `DEB`), `writeCredit` (`CRE`), `writeTransfer` (`TFR`),
  `writeCreateCustomer` (`ICC`), `writeDeleteCustomer` (`IDC`), `writeCreateAccount` (`ICA`),
  `writeDeleteAccount` (`IDA`). EYECATCHER always `PROC_TRAN_VALID` = `'PRTR'`.
- **SELECT (paged read)** — the only read path:
  ```sql
  SELECT * FROM (
     SELECT p.*, row_number() over() AS rn
     FROM PROCTRAN AS p
     WHERE PROCTRAN_SORTCODE LIKE ?
     ORDER BY PROCTRAN_DATE ASC, PROCTRAN_TIME ASC
  ) AS col WHERE rn BETWEEN ? AND ?
  ```
  Predicate: `PROCTRAN_SORTCODE LIKE ?`; ordered by `PROCTRAN_DATE, PROCTRAN_TIME`; paginated
  by `offset+1 .. offset+limit`. Reads all 9 columns. This full-table scan + sort + window
  function is exactly the query that would benefit from an index (see migration notes).
- After reading, the type code drives DESC parsing: `processTransferRecord` (`TFR`),
  `processCreateDeleteAccountRecord` (create/delete account types), and
  `processCreateDeleteCustomerRecord` (create/delete customer types) slice the 40-char
  `PROCTRAN_DESC` by fixed offsets (e.g. custno = chars 6–16, name 16–30, DOB 30–40).

Supporting Java:
- `datainterfaces/PROCTRAN.java` — constants for the eyecatcher (`PROC_TRAN_VALID='PRTR'`),
  all TYPE codes, and DESC sub-field getters/setters mirroring the copybook REDEFINES.
- `api/json/ProcessedTransactionResource.java` — REST resource; `getProcessedTransactionInternal`
  calls `getProcessedTransactions(sortCode, limit, offset)`; `writeDebitCreditInternal`,
  `writeTransferLocalInternal`, `writeCreate/DeleteAccount/Customer*` wrap the DB2 writers.
- `api/json/AccountsResource.java` — on create-account, transfer-local, debit/credit, and
  delete-account, calls the ProcessedTransactionResource writers; treats a failed PROCTRAN
  write as a hard error (`"Failed to write to PROCTRAN data store"`).
- `api/json/CustomerResource.java` — same pattern for create-customer and delete-customer.

### Non-code references
`src/base/cobol_copy/BANKMAP.cpy` (BMS map fields), `etc/install/base/db2jcl/DROPDB2.jcl`
(DROP TABLE/STOGROUP/TABLESPACE), `DRPTB02/DRPTS02/DRPSG02.jcl`, `DB2BIND.jcl`, and docs
(`README.md`, `CBSA_BMS_User_Guide.md`).

---

## 3. Keys & Relationships (Foreign-Key-Like)

- **Logical identity of a PROCTRAN row is composite:** `PROCTRAN_SORTCODE + PROCTRAN_NUMBER`
  (the account) — but this is **not unique** (an account has many transactions) and is **not
  enforced by any index**. There is no natural single-row key; `REF` (task number) + timestamp
  is the closest thing to a business key and is not guaranteed unique.
- **PROCTRAN → ACCOUNT**: `(PROCTRAN_SORTCODE, PROCTRAN_NUMBER)` references
  `ACCOUNT(ACCOUNT_SORTCODE, ACCOUNT_NUMBER)` (composite). Enforced only in application logic,
  not by a DB2 FK.
- **ACCOUNT → CUSTOMER**: `ACCOUNT(ACCOUNT_SORTCODE, ACCOUNT_CUSTOMER_NUMBER)` references the
  CUSTOMER VSAM KSDS (customer number). PROCTRAN embeds the customer number *inside*
  `PROCTRAN_DESC` for create/delete-customer & create/delete-account records (positional), not
  as a real column.
- **Transfer linkage**: `TFR` rows embed the *counterparty* sort code + account number inside
  `PROCTRAN_DESC` (`PROC-TRAN-DESC-XFR-SORTCODE`/`-ACCOUNT`) rather than as columns.
- **CONTROL table** is a generic key/value store (`CONTROL_NAME`, `CONTROL_VALUE_NUM`,
  `CONTROL_VALUE_STR`) holding counters/last-used numbers; unique index `CONTINDX` on NAME.

---

## 4. PROCTRAN_TYPE Codes (from `PROCTRAN.cpy` 88-levels)

| Code | Meaning | Written by |
|---|---|---|
| `CHA` | Cheque acknowledged | (defined, not written in COBOL sample) |
| `CHF` | Cheque failure | (defined) |
| `CHI` | Cheque paid in | (defined) |
| `CHO` | Cheque paid out | (defined) |
| `CRE` | Credit | DBCRFUN, Java writeCredit |
| `DEB` | Debit | DBCRFUN, Java writeDebit |
| `ICA` | Web (internet) create account | Java writeCreateAccount |
| `ICC` | Web create customer | Java writeCreateCustomer |
| `IDA` | Web delete account | Java writeDeleteAccount |
| `IDC` | Web delete customer | Java writeDeleteCustomer |
| `OCA` | Branch create account | CREACC |
| `OCC` | Branch create customer | CRECUST |
| `ODA` | Branch delete account | DELACC |
| `ODC` | Branch delete customer | DELCUS |
| `OCS` | Create SODD (standing order/direct debit) | (defined) |
| `PCR` | Payment credit | DBCRFUN |
| `PDR` | Payment debit | DBCRFUN |
| `TFR` | Transfer | XFRFUN, Java writeTransfer |

The `O..` (branch/"outside") vs `I..` (internet/web) prefix distinguishes the origin channel;
the COBOL green-screen programs hard-code the `O..` variants, the Liberty web layer writes the
`I..` variants. Both channels also emit `DEB`/`CRE`/`TFR` for money movement.

---

## 5. Data-Quality / Modelling Issues for a Postgres Migration

1. **Eyecatcher column** — `PROCTRAN_EYECATCHER CHAR(4)` is always `'PRTR'` (or `X'FF...'` for
   the never-used logical-delete flag). It's a legacy storage sanity marker with no business
   meaning; drop it in Postgres (or keep as a `CHECK`-constrained constant if provenance matters).
2. **Overloaded `PROCTRAN_DESC CHAR(40)`** — this single fixed-width field is a polymorphic
   union parsed positionally by TYPE (7+ different `REDEFINES`: transfer, create/delete account,
   create/delete customer). Java slices it by hard-coded substring offsets. In Postgres this
   should be normalized into typed columns (counterparty_sortcode, counterparty_account,
   customer_number, account_type, statement dates, dob, name) or a `JSONB` payload, rather than
   a padded char blob.
3. **Fixed CHAR padding** — sort code/number/ref stored as zero-padded fixed CHAR (and numeric
   sub-fields stored as text digits). Trailing/leading spaces and zero-fill will need trimming;
   use real `INTEGER`/`NUMERIC`/`TEXT` types.
4. **Date/Time split & inconsistency** — date is `DATE` in DB2 but `CHAR(8)` in the SQL-declare
   copybook and `9(8)` YYYYMMDD in the record; time is a separate `CHAR(6)` HHMMSS string. The
   Java reader recombines DATE + parsed HH/MM/SS into a single timestamp. Migrate to a single
   `TIMESTAMP`/`TIMESTAMPTZ` column.
5. **`DECIMAL(12,2)` vs COBOL `S9(10)V99`** — COBOL allows max ±9,999,999,999.99 (12 digits) —
   consistent, but confirm sign handling; map to `NUMERIC(12,2)`.
6. **Magic values** — TYPE 3-char codes and the `'TRANSFER'`/`'CREATE'`/`'DELETE'` footer flags
   embedded in DESC are magic strings. Encode as an enum / lookup table.
7. **`PROCTRAN_REF` is the CICS task number**, not a durable/unique sequence — do not treat it
   as a global primary key. Add a real surrogate PK (`BIGINT GENERATED ALWAYS AS IDENTITY`).
8. **No key / no index** — append-only heap with a read path that filters `SORTCODE LIKE ?` and
   sorts by DATE,TIME. Add PK + index on `(PROCTRAN_SORTCODE, PROCTRAN_NUMBER)` and on
   `(PROCTRAN_SORTCODE, PROCTRAN_DATE, PROCTRAN_TIME)` to support the pagination query and the
   FK; consider `row_number() OVER (ORDER BY ...)` → `OFFSET/LIMIT` or keyset pagination.
9. **Sort-code duplication** — the branch sort code is repeated on ACCOUNT, PROCTRAN, and inside
   DESC of transfer records; it's effectively a constant for a single-branch install. Normalize.
10. **Nullability** — only SORTCODE/NUMBER are NOT NULL; DATE/TIME/TYPE/AMOUNT are all nullable
    although application always populates them. Tighten to NOT NULL in Postgres.
11. **Logical-delete flag unused** — the `X'FF'` logical-delete redefine of the eyecatcher is
    dead weight for PROCTRAN (rows are never deleted); do not carry it forward.

---

## 6. Referential-Integrity Rules Enforced in COBOL (to model as constraints)

- **Atomicity of balance change + journal write.** `DBCRFUN` (and the other money movers)
  update the `ACCOUNT` row first, then INSERT into PROCTRAN. If the PROCTRAN insert fails
  (`SQLCODE NOT = 0`), the program issues `EXEC CICS SYNCPOINT ROLLBACK` to undo the ACCOUNT
  update, and if the rollback itself fails it abends with message `WTPD010 - COULD NOT ROLL
  BACK, POSSIBLE DATA INTEGRITY ISSUE BETWEEN ACCOUNT AND PROCTRAN`. In Postgres: wrap the
  balance update and journal insert in **one transaction**; a FK from PROCTRAN → ACCOUNT plus
  the transaction guarantees no orphan journal rows.
- **Every posting must reference an existing account.** SORTCODE+NUMBER on PROCTRAN always
  corresponds to a real ACCOUNT row at write time → enforce with a composite **FK**
  `PROCTRAN(SORTCODE, NUMBER) → ACCOUNT(SORTCODE, NUMBER)`. (Caveat: delete-account records are
  written for accounts being removed — decide FK timing / whether to journal before the ACCOUNT
  delete, or relax the FK for `*DA` audit rows.)
- **Transfers post two rows.** `XFRFUN` writes a debit-side and a credit-side PROCTRAN row and
  updates both ACCOUNT balances; both must succeed atomically.
- **Web write failures are fatal to the operation.** The Java resources abort the REST call and
  return `"Failed to write to PROCTRAN data store"` if the journal insert fails — the journal is
  treated as mandatory, reinforcing that PROCTRAN is the system of record for activity.
- **Customer field updates deliberately write nothing** (`UPDCUST`), so absence of a PROCTRAN
  row for a customer edit is intentional, not a bug.
