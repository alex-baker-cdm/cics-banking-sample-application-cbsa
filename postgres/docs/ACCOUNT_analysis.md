# CBSA Db2 Table `ACCOUNT` — Exhaustive Access-Pattern Analysis

Repo: `alex-baker-cdm/cics-banking-sample-application-cbsa`
Scope: every access to the Db2 **ACCOUNT** table across COBOL, JCL/DDL, and Java (Liberty webui + z/OS Connect).

---

## 1. DDL — columns, types, nullability, keys, indexes

Authoritative CREATE is in `etc/install/base/db2jcl/INSTDB2.jcl` (also split into `CRETB01.jcl` for the table and `CREI101.jcl` / `CREI201.jcl` for the indexes). The COBOL `EXEC SQL DECLARE` copybook is `src/base/cobol_copy/ACCDB2.cpy`.

```sql
CREATE TABLE ACCOUNT (
    ACCOUNT_EYECATCHER          CHAR(4),
    ACCOUNT_CUSTOMER_NUMBER     CHAR(10),
    ACCOUNT_SORTCODE            CHAR(6)  NOT NULL,
    ACCOUNT_NUMBER              CHAR(8)  NOT NULL,
    ACCOUNT_TYPE                CHAR(8),
    ACCOUNT_INTEREST_RATE       DECIMAL(6,2),      -- ACCDB2.cpy declares DECIMAL(4,2)  <-- MISMATCH
    ACCOUNT_OPENED              DATE,
    ACCOUNT_OVERDRAFT_LIMIT     INTEGER,
    ACCOUNT_LAST_STATEMENT      DATE,
    ACCOUNT_NEXT_STATEMENT      DATE,
    ACCOUNT_AVAILABLE_BALANCE   DECIMAL(12,2),     -- ACCDB2.cpy declares DECIMAL(10,2) <-- MISMATCH
    ACCOUNT_ACTUAL_BALANCE      DECIMAL(12,2)      -- ACCDB2.cpy declares DECIMAL(10,2) <-- MISMATCH
) IN CBSA.ACCOUNT NOT VOLATILE;

CREATE UNIQUE INDEX ACCTINDX ON ACCOUNT(ACCOUNT_SORTCODE, ACCOUNT_NUMBER);            -- de-facto PRIMARY KEY
CREATE INDEX        ACCTCUST ON ACCOUNT(ACCOUNT_SORTCODE, ACCOUNT_CUSTOMER_NUMBER);  -- secondary access path
```

- **No PRIMARY KEY, no FOREIGN KEY, no CHECK** is declared. The only `NOT NULL` columns are `ACCOUNT_SORTCODE` and `ACCOUNT_NUMBER`.
- **De-facto primary key** = unique index `ACCTINDX (ACCOUNT_SORTCODE, ACCOUNT_NUMBER)`.
- **Secondary index** `ACCTCUST (ACCOUNT_SORTCODE, ACCOUNT_CUSTOMER_NUMBER)` supports "all accounts for a customer" lookups (used by INQACCCU / DELCUS).
- The COBOL app-record copybook `ACCOUNT.cpy` stores the same fields but with different physical shapes: dates as `PIC 9(8)` (DDMMYYYY) and balances as `PIC S9(10)V99` — see migration notes.

---

## 2. Every access pattern

### 2a. COBOL programs with direct `EXEC SQL` against ACCOUNT

| Program | Operation(s) | WHERE / key predicate | Columns | Business operation |
|---|---|---|---|---|
| **CREACC.cbl** | `INSERT INTO ACCOUNT (all 12 cols)` | — (insert) | writes all columns | **Open account.** First validates the customer exists and counts existing accounts (LINK `INQACCCU`); rejects if customer has >9 accounts. Generates the new `ACCOUNT_NUMBER` from the CONTROL table (`<sortcode>-ACCOUNT-LAST`, guarded by CICS `ENQ`/`DEQ`), bumps `<sortcode>-ACCOUNT-COUNT`, then inserts. Also writes a PROCTRAN row. |
| **DELACC.cbl** | `SELECT (all 12 cols)` then `DELETE` | `ACCOUNT_NUMBER = :n AND ACCOUNT_SORTCODE = :s` | reads all, then deletes row | **Close/delete account.** Reads the row (to return it / write history), deletes by full composite key, decrements `<sortcode>-ACCOUNT-COUNT` in CONTROL, writes a PROCTRAN "account deleted" record. |
| **UPDACC.cbl** | `SELECT (all 12 cols)` then `UPDATE` | `ACCOUNT_SORTCODE = :s AND ACCOUNT_NUMBER = :n` | reads all; **updates only** `ACCOUNT_TYPE, ACCOUNT_INTEREST_RATE, ACCOUNT_OVERDRAFT_LIMIT` | **Amend account attributes** (type / interest rate / overdraft). Balances and keys are deliberately NOT updated. |
| **INQACC.cbl** | (a) cursor `DECLARE ACC-CURSOR ... FOR FETCH ONLY` + `FETCH`; (b) singleton `SELECT ... ORDER BY ACCOUNT_NUMBER DESC FETCH FIRST 1 ROWS ONLY` | (a) `ACCOUNT_SORTCODE = :s AND ACCOUNT_NUMBER = :n`; (b) `ACCOUNT_SORTCODE = :s` | reads all 12 cols | **Account enquiry (single account).** Path (a) reads one account by full key. Path (b) is the **magic account number `99999999`** case = "return the highest existing account number for this sort code". |
| **INQACCCU.cbl** | cursor `DECLARE ACC-CURSOR ... FOR FETCH ONLY` + `FETCH` (loop) | `ACCOUNT_CUSTOMER_NUMBER = :c AND ACCOUNT_SORTCODE = :s` | reads all 12 cols | **List all accounts for a customer** (uses `ACCTCUST` index). Also used by CREACC to count a customer's accounts and to confirm the customer exists. |
| **DBCRFUN.cbl** | `SELECT (all 12 cols)` then `UPDATE (all cols set)` | both: `ACCOUNT_SORTCODE = :s AND ACCOUNT_NUMBER = :n` | reads all; writes all (effectively updates the two balances) | **Debit / Credit (single account).** Reads account; if `COMM-AMT < 0` and it would push `ACCOUNT_AVAILABLE_BALANCE` below the overdraft limit **and** `COMM-FACILTYPE = 496`, rejects with fail code `3` (insufficient funds). Otherwise adds amount to available + actual balance, updates, writes PROCTRAN. Fail codes: `1`/`2` not found, `3` insufficient funds, `4` account-type/loan rule. |
| **XFRFUN.cbl** | 2× `SELECT (all 12 cols)` + 2× `UPDATE (all cols set)` | each: `ACCOUNT_SORTCODE = :s AND ACCOUNT_NUMBER = :n` | reads all for both accounts; writes both balances | **Transfer funds between two accounts.** Reads "from" and "to" accounts, debits one / credits the other (with the same overdraft/insufficient-funds guard as DBCRFUN), updates both rows, writes PROCTRAN transfer records. |
| **BANKDATA.cbl** | bulk `INSERT INTO ACCOUNT (all 12 cols)`; `DELETE FROM ACCOUNT WHERE ACCOUNT_SORTCODE = :s` | delete: `ACCOUNT_SORTCODE = :s` | insert all; delete whole sort code | **Test-data generator / purge.** Bulk-loads randomized accounts; the DELETE wipes every account for a sort code (note: NOT keyed on number). |

### 2b. Orchestration / presentation COBOL programs (no direct SQL — they `EXEC CICS LINK`)

| Program | Links to | Business screen / flow |
|---|---|---|
| **BNK1CAC.cbl** | `CREACC` | BMS "Create Account" screen |
| **BNK1DAC.cbl** | `INQACC` then `DELACC` | BMS "Display / Delete Account" screen |
| **BNK1UAC.cbl** | `INQACC` then `UPDACC` | BMS "Update Account" screen |
| **BNK1CRA.cbl** | `DBCRFUN` | BMS "Credit / Debit" screen |
| **BNK1TFN.cbl** | `XFRFUN` | BMS "Transfer Funds" screen |
| **DELCUS.cbl** | `INQACCCU` (+ `DELACC` per account) | Delete customer → **cascade delete of all their accounts** |
| **CREACC.cbl** | `INQACCCU` | Customer-existence / account-count validation before open |

### 2c. Java layer (Liberty `webui` + z/OS Connect) — JDBC to the same table

`src/webui/src/main/java/com/ibm/cics/cip/bankliberty/web/db2/Account.java` mirrors the COBOL access paths via prepared statements (all include an `ACCOUNT_EYECATCHER LIKE 'ACCT'` filter and use `LIKE` on key columns because of CHAR padding):

| SQL | Predicate | Purpose |
|---|---|---|
| `SELECT * from ACCOUNT where ... ACCOUNT_NUMBER like ? and ACCOUNT_SORTCODE like ?` | number + sortcode | read one account |
| `SELECT * from ACCOUNT where ... ACCOUNT_SORTCODE like ? order by ACCOUNT_NUMBER DESC` | sortcode | magic account `99999999` = highest number |
| `SELECT * from ACCOUNT where ... ACCOUNT_CUSTOMER_NUMBER like ? and ACCOUNT_SORTCODE like ? ORDER BY ACCOUNT_NUMBER` | customer + sortcode | list accounts for a customer |
| `SELECT * from ACCOUNT where ... ACCOUNT_SORTCODE like ? ORDER BY ACCOUNT_NUMBER` | sortcode | list all accounts for a branch |
| `SELECT COUNT(*) as ACCOUNT_COUNT from ACCOUNT where ... ACCOUNT_SORTCODE like ?` | sortcode | count accounts |
| `INSERT INTO ACCOUNT (...) VALUES ('ACCT',?,...,0.00,0.00)` + CONTROL read/update | — | open account (balances forced to 0.00) |
| `DELETE from ACCOUNT where ... ACCOUNT_NUMBER like ? and ACCOUNT_SORTCODE like ?` | number + sortcode | delete account |
| `UPDATE ACCOUNT SET (all fields) WHERE ACCOUNT_NUMBER like ? AND ACCOUNT_SORTCODE like ?` | number + sortcode | full update |
| `UPDATE ACCOUNT SET ACCOUNT_TYPE, ACCOUNT_INTEREST_RATE, ACCOUNT_OVERDRAFT_LIMIT WHERE ...` | number + sortcode | "safe" attribute update (matches UPDACC) |
| `UPDATE ACCOUNT SET ACCOUNT_ACTUAL_BALANCE, ACCOUNT_AVAILABLE_BALANCE WHERE ...` | number + sortcode | balance update (debit/credit) |

Related Java files: `api/json/AccountsResource.java` (REST endpoints), `api/json/CounterResource.java` (CONTROL counters), `webui/data_access/Account.java` + `AccountList.java` (REST clients), `datainterfaces/NewAccountNumber.java`. The z/OS Connect Spring interface `Z-OS-Connect-Customer-Services-Interface/.../WebController.java` and `InqAccczJson.java` call the same operations over REST.

---

## 3. Keys and relationships

- **Primary key (logical):** composite `(ACCOUNT_SORTCODE, ACCOUNT_NUMBER)` — enforced only by the unique index `ACCTINDX`.
- **Secondary access path:** `(ACCOUNT_SORTCODE, ACCOUNT_CUSTOMER_NUMBER)` via index `ACCTCUST`.
- **FK-like → CUSTOMER (VSAM KSDS, key SORTCODE+NUMBER):** `ACCOUNT_CUSTOMER_NUMBER` (+ `ACCOUNT_SORTCODE`) references the customer. Not a DB constraint — enforced in COBOL (CREACC validates the customer exists before opening an account; DELCUS cascades deletes).
- **FK-like ← PROCTRAN:** `PROCTRAN (PROCTRAN_SORTCODE, PROCTRAN_NUMBER)` references an account; every balance-changing program (CREACC, DELACC, DBCRFUN, XFRFUN) writes a PROCTRAN audit row.
- **CONTROL (generic key/value store):** rows `<sortcode>-ACCOUNT-LAST` (last-issued account number) and `<sortcode>-ACCOUNT-COUNT` (number of accounts) drive account-number generation and counts; `CONTROL_NAME` is the unique key (`CONTINDX`).

---

## 4. Data-quality / modelling issues for a Postgres migration

1. **Eyecatcher filler** `ACCOUNT_EYECATCHER CHAR(4)` = constant `'ACCT'`. Pure mainframe storage marker. Drop it, or replace with a `CHECK`/discriminator; note the Java layer relies on `LIKE 'ACCT'` filtering.
2. **No declared PK / NOT NULL on business columns.** `ACCOUNT_CUSTOMER_NUMBER`, `ACCOUNT_TYPE`, balances, dates are all nullable. In Postgres declare `PRIMARY KEY (account_sortcode, account_number)` and appropriate `NOT NULL`.
3. **DECIMAL precision/scale mismatch** between the DDL and the COBOL DECLARE/host vars: table = `DECIMAL(12,2)` balances / `DECIMAL(6,2)` interest; `ACCDB2.cpy` = `DECIMAL(10,2)` / `DECIMAL(4,2)`; COBOL working storage = `PIC S9(10)V99`. Pick one target (e.g. `numeric(12,2)` balances, `numeric(6,2)` interest) and reconcile — risk of silent truncation/overflow.
4. **Fixed CHAR padding.** All keys are space-padded `CHAR(n)`; the Java code compensates with `LIKE`. Migrate to trimmed `varchar`/`char` and normalize comparisons, or you'll reproduce padding bugs.
5. **Odd date storage.** Db2 columns are `DATE`, but the COBOL app record (`ACCOUNT.cpy`) stores dates as `PIC 9(8)` `DDMMYYYY` and host-variable strings as `X(10)` `DD.MM.YYYY`. Normalize to real `DATE` and centralize format conversion.
6. **`ACCOUNT_OVERDRAFT_LIMIT INTEGER`** (whole currency units) sits alongside 2-dp balances — inconsistent money representation.
7. **Magic sentinel value `99999999`.** Not a real row — it's a request token meaning "give me the highest account number for this sort code" (INQACC path (b), Java `sql9999`). Handle in the query/service layer; never load it as data.
8. **Sort-code duplication / denormalization.** `SORTCODE` is repeated on ACCOUNT, CUSTOMER, PROCTRAN and every CONTROL key. In a relational model consider a `branch` table (or a single-branch assumption) rather than propagating sortcode everywhere.
9. **CONTROL as a generic key/value store** (`name`, `value_num`, `value_str`) used both for sequence generation and row counts. Replace with a real Postgres `SEQUENCE` per branch for account numbers and a live `COUNT(*)`/materialized counter instead of a hand-maintained value.
10. **Race-prone number generation.** Account numbers come from CICS `ENQ`/`DEQ` + read-increment-write of CONTROL. In Postgres use a `SEQUENCE` or a serializable transaction to avoid duplicate keys.

---

## 5. Referential-integrity / business rules enforced in COBOL (candidates for DB constraints)

- **Customer must exist** before an account is opened — CREACC links INQACCCU/INQCUST. → FK `account.(sortcode, customer_number)` → `customer`.
- **Max 10 accounts per customer** — CREACC rejects when existing count `> 9`. → trigger / CHECK / app rule.
- **Delete-customer cascades to accounts** — DELCUS enumerates accounts (INQACCCU) and deletes each (DELACC). → `ON DELETE CASCADE`.
- **Overdraft / insufficient-funds guard** — DBCRFUN & XFRFUN reject a debit that pushes available balance below the (negative) overdraft limit when `COMM-FACILTYPE = 496`. → CHECK `available_balance >= -overdraft_limit` (note: only enforced for one facility type today).
- **Immutable keys on amend** — UPDACC only updates type/interest/overdraft, never keys or balances. → keep keys immutable; restrict which columns amend paths may write.
- **Every balance change is audited** — CREACC/DELACC/DBCRFUN/XFRFUN write PROCTRAN. → FK PROCTRAN → ACCOUNT, and enforce audit at the service/trigger layer.
- **Composite key uniqueness** — `(sortcode, number)` unique (ACCTINDX). → PRIMARY KEY.

---

## Appendix — key source locations
- DDL: `etc/install/base/db2jcl/INSTDB2.jcl`, `CRETB01.jcl`, `CREI101.jcl`, `CREI201.jcl`
- COBOL Db2 host-var copybook: `src/base/cobol_copy/ACCDB2.cpy`; app record: `src/base/cobol_copy/ACCOUNT.cpy`; PROCTRAN/CONTROL copybooks: `PROCTRAN.cpy`, `CONTDB2.cpy`/`CONTROLI.cpy`
- COBOL programs: `src/base/cobol_src/{CREACC,DELACC,UPDACC,INQACC,INQACCCU,DBCRFUN,XFRFUN,BANKDATA}.cbl` (+ BNK1* / DELCUS orchestration)
- Java: `src/webui/src/main/java/com/ibm/cics/cip/bankliberty/web/db2/Account.java` and `api/json/AccountsResource.java`
