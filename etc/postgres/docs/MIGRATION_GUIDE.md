# DB2 to PostgreSQL Migration Guide

## Overview

This document describes the migration of the CBSA banking application's data layer from IBM DB2 (+ VSAM files) to PostgreSQL. The migration normalizes the schema, eliminates legacy patterns, and leverages PostgreSQL-native features for improved correctness and performance.

## Source Schema (DB2 + VSAM)

### DB2 Tables
| Table | Purpose | Key |
|-------|---------|-----|
| `IBMUSER.ACCOUNT` | Bank accounts | `(SORTCODE, ACCOUNT_NUMBER)` UNIQUE |
| `IBMUSER.PROCTRAN` | Transaction audit trail | **No PK** |
| `IBMUSER.CONTROL` | Sequence counters | `(CONTROL_NAME)` UNIQUE |

### VSAM Files
| File | Purpose | Key |
|------|---------|-----|
| `CUSTOMER` | Customer records | `(SORTCODE + CUSTOMER_NUMBER)` KSDS |
| `CUSTOMER-CONTROL` | Customer counter | Special record in CUSTOMER file |

## Target Schema (PostgreSQL)

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         cbsa (PostgreSQL Schema)                         │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────┐    ┌──────────────┐    ┌───────────────────┐          │
│  │   branch    │◄───│   customer   │◄───│     account       │          │
│  ├─────────────┤    ├──────────────┤    ├───────────────────┤          │
│  │ sortCode PK │    │ id PK        │    │ id PK             │          │
│  │ branchName  │    │ sortCode FK  │    │ sortCode FK       │          │
│  └─────────────┘    │ customerNum  │    │ accountNumber     │          │
│        ▲            │ customerName │    │ customerNumber FK │          │
│        │            │ address      │    │ accountType FK────┼──┐       │
│        │            │ dateOfBirth  │    │ interestRate      │  │       │
│        │            │ creditScore  │    │ overdraftLimit    │  │       │
│        │            │ reviewDate   │    │ availableBalance  │  │       │
│        │            │ createdAt    │    │ actualBalance     │  │       │
│        │            │ updatedAt    │    │ createdAt         │  │       │
│        │            └──────────────┘    │ updatedAt         │  │       │
│        │                                └───────────────────┘  │       │
│        │                                        ▲              │       │
│        │                                        │              │       │
│        │            ┌───────────────────┐       │    ┌─────────┴─────┐ │
│        └────────────│   transaction     │       │    │  accountType  │ │
│                     ├───────────────────┤       │    ├───────────────┤ │
│                     │ id PK             │       │    │ code PK       │ │
│                     │ sortCode FK       │       │    │ description   │ │
│                     │ accountNumber FK──┼───────┘    │ allowDebit    │ │
│                     │ timestamp         │            └───────────────┘ │
│                     │ reference         │                              │
│                     │ transactionType FK┼───────┐                      │
│                     │ description       │       │   ┌────────────────┐ │
│                     │ amount            │       └──►│transactionType │ │
│                     │ createdAt         │           ├────────────────┤ │
│                     └───────────────────┘           │ code PK        │ │
│                                                     │ description    │ │
│  SEQUENCES:                                         │ category       │ │
│  ┌──────────────────────┐                           └────────────────┘ │
│  │ accountNumberSeq     │  (replaces CONTROL table)                    │
│  │ customerNumberSeq    │  (replaces VSAM Named Counter)               │
│  └──────────────────────┘                                              │
│                                                                         │
│  VIEWS:                                                                 │
│  ┌──────────────────────────────────────────┐                          │
│  │ accountCountByBranch (replaces CONTROL)  │                          │
│  └──────────────────────────────────────────┘                          │
└─────────────────────────────────────────────────────────────────────────┘
```

## Normalization Changes

### 1. CONTROL Table → PostgreSQL Sequences

**Before (DB2):**
```
CONTROL table with rows like:
  '987654-ACCOUNT-LAST'  → CONTROL_VALUE_NUM = 15
  '987654-ACCOUNT-COUNT' → CONTROL_VALUE_NUM = 15
```
Pattern: `SELECT → increment in app → UPDATE` (race condition in Java path!)

**After (PostgreSQL):**
```sql
CREATE SEQUENCE cbsa.accountNumberSeq;
-- Usage: SELECT nextval('cbsa.accountNumberSeq');
```
Benefits:
- ✓ Atomic, lock-free counter increments
- ✓ Eliminates race condition bug in Java path (no explicit locking was used)
- ✓ ACCOUNT-COUNT is now a computed view instead of a manually-maintained counter

### 2. EYECATCHER Columns → Removed

**Before:** Every row had `ACCOUNT_EYECATCHER='ACCT'` / `PROCTRAN_EYECATCHER='PRTR'`
- Used as a DB2 soft-delete/record-type discriminator
- Every query included `WHERE EYECATCHER LIKE 'ACCT'`

**After:** Removed entirely. Table structure provides type safety. If soft-delete is ever needed, a `deletedAt TIMESTAMP` column is cleaner.

### 3. PROCTRAN_DATE + PROCTRAN_TIME → Single TIMESTAMP

**Before:**
```
PROCTRAN_DATE  CHAR(8)   -- 'YYYYMMDD' stored as DATE in DB2
PROCTRAN_TIME  CHAR(6)   -- 'HHMMSS'
```
Queries had to: `ORDER BY PROCTRAN_DATE ASC, PROCTRAN_TIME ASC`

**After:**
```sql
transactionTimestamp TIMESTAMP NOT NULL DEFAULT NOW()
```
Benefits:
- ✓ Single indexed column for temporal queries
- ✓ Native timezone support if needed later
- ✓ Simpler ORDER BY and range queries

### 4. ACCOUNT_TYPE → Reference Table

**Before:** `ACCOUNT_TYPE CHAR(8)` with validation only in application code (COBOL + Java)

**After:** FK to `accountType` table with:
- Enforced at DB level via FK constraint
- `allowDebit` flag captures the business rule that MORTGAGE/LOAN accounts cannot be debited via payment facility
- Easy to add new account types without code changes

### 5. PROCTRAN_TYPE → Reference Table

**Before:** 18 magic 3-letter codes scattered across COBOL and Java constants

**After:** FK to `transactionType` table with:
- Human-readable descriptions
- Category grouping (CHEQUE, CREDIT, DEBIT, TRANSFER, etc.)
- Enables reporting/analytics queries by category

### 6. Branch/SortCode → Branch Table

**Before:** `SORTCODE` denormalized as a CHAR(6) in every row of every table

**After:** `branch` table with sortCode as PK, referenced by FK from all entity tables
- Enables branch metadata (name, address, etc.)
- Single source of truth for valid branch codes

### 7. CUSTOMER (VSAM → PostgreSQL)

**Before:** VSAM KSDS file with fixed-length record, composite key

**After:** PostgreSQL table with:
- Surrogate BIGINT PK (for efficient joins and API references)
- Unique constraint on (sortCode, customerNumber) preserving original semantics
- TEXT search index on customerName for the name search API
- Proper DATE type for date of birth (was PIC 9(8) DDMMYYYY)

### 8. Foreign Key Relationships (Application → Database)

**Before:** All referential integrity enforced in application code:
- CREACC links to INQCUST to verify customer exists
- DELCUS links to INQACCCU, then DELACC for each account (cascade in code)
- PROCTRAN has no formal link to ACCOUNT

**After:** Proper FK constraints with database-level enforcement:
- `account.customerNumber → customer.customerNumber` with `ON DELETE CASCADE`
- `transaction.accountNumber → account.accountNumber`
- Referential integrity guaranteed even if application has bugs

## Column Mapping Reference

### ACCOUNT → cbsa.account

| DB2 Column | PostgreSQL Column | Type Change | Notes |
|-----------|-------------------|-------------|-------|
| ACCOUNT_EYECATCHER | *(removed)* | — | Always 'ACCT'; redundant |
| ACCOUNT_CUSTOMER_NUMBER | customerNumber | CHAR(10) → VARCHAR(10) | FK to customer |
| ACCOUNT_SORTCODE | sortCode | CHAR(6) | FK to branch |
| ACCOUNT_NUMBER | accountNumber | CHAR(8) → VARCHAR(8) | Part of unique constraint |
| ACCOUNT_TYPE | accountType | CHAR(8) → VARCHAR(8) | FK to accountType |
| ACCOUNT_INTEREST_RATE | interestRate | DECIMAL(6,2) → NUMERIC(6,2) | Equivalent |
| ACCOUNT_OPENED | opened | DATE | Same |
| ACCOUNT_OVERDRAFT_LIMIT | overdraftLimit | INTEGER | CHECK >= 0 added |
| ACCOUNT_LAST_STATEMENT | lastStatement | DATE | Nullable now |
| ACCOUNT_NEXT_STATEMENT | nextStatement | DATE | Nullable now |
| ACCOUNT_AVAILABLE_BALANCE | availableBalance | DECIMAL(12,2) → NUMERIC(12,2) | Equivalent |
| ACCOUNT_ACTUAL_BALANCE | actualBalance | DECIMAL(12,2) → NUMERIC(12,2) | Equivalent |
| *(new)* | id | BIGINT IDENTITY | Surrogate PK |
| *(new)* | createdAt | TIMESTAMP | Audit field |
| *(new)* | updatedAt | TIMESTAMP | Auto-updated |

### PROCTRAN → cbsa.transaction

| DB2 Column | PostgreSQL Column | Type Change | Notes |
|-----------|-------------------|-------------|-------|
| PROCTRAN_EYECATCHER | *(removed)* | — | Always 'PRTR'; redundant |
| PROCTRAN_SORTCODE | sortCode | CHAR(6) | FK to branch |
| PROCTRAN_NUMBER | accountNumber | CHAR(8) → VARCHAR(8) | Renamed for clarity |
| PROCTRAN_DATE + PROCTRAN_TIME | transactionTimestamp | DATE+CHAR(6) → TIMESTAMP | Combined |
| PROCTRAN_REF | reference | CHAR(12) → VARCHAR(12) | CICS task number |
| PROCTRAN_TYPE | transactionType | CHAR(3) | FK to transactionType |
| PROCTRAN_DESC | description | CHAR(40) → VARCHAR(40) | Variable length |
| PROCTRAN_AMOUNT | amount | DECIMAL(12,2) → NUMERIC(12,2) | Equivalent |
| *(new)* | id | BIGINT IDENTITY | Surrogate PK |
| *(new)* | createdAt | TIMESTAMP | Audit field |

### CUSTOMER (VSAM) → cbsa.customer

| VSAM Field | PostgreSQL Column | Type Change | Notes |
|-----------|-------------------|-------------|-------|
| CUSTOMER-EYECATCHER | *(removed)* | — | Always 'CUST'; redundant |
| CUSTOMER-SORTCODE | sortCode | PIC 9(6) → CHAR(6) | FK to branch |
| CUSTOMER-NUMBER | customerNumber | PIC 9(10) → VARCHAR(10) | Part of unique |
| CUSTOMER-NAME | customerName | PIC X(60) → VARCHAR(60) | Trimmed |
| CUSTOMER-ADDRESS | customerAddress | PIC X(160) → VARCHAR(160) | Trimmed |
| CUSTOMER-DATE-OF-BIRTH | dateOfBirth | PIC 9(8) → DATE | DDMMYYYY → DATE |
| CUSTOMER-CREDIT-SCORE | creditScore | PIC 999 → SMALLINT | CHECK 0-999 |
| CUSTOMER-CS-REVIEW-DATE | creditScoreReviewDate | PIC 9(8) → DATE | DDMMYYYY → DATE |
| *(new)* | id | BIGINT IDENTITY | Surrogate PK |
| *(new)* | createdAt | TIMESTAMP | Audit field |
| *(new)* | updatedAt | TIMESTAMP | Auto-updated |

### CONTROL → *(eliminated)*

| DB2 Row | PostgreSQL Replacement | Notes |
|---------|----------------------|-------|
| `<sortcode>-ACCOUNT-LAST` | `cbsa.accountNumberSeq` | Atomic sequence |
| `<sortcode>-ACCOUNT-COUNT` | `cbsa.accountCountByBranch` view | Computed, not stored |
| *(VSAM customer counter)* | `cbsa.customerNumberSeq` | Atomic sequence |

## Business Rules Preserved

| Rule | Source | PostgreSQL Implementation |
|------|--------|--------------------------|
| Max 10 accounts per customer | CREACC.cbl:347 | Application-level (trigger possible) |
| Valid account types: ISA, MORTGAGE, SAVING, CURRENT, LOAN | CREACC.cbl:1209 | FK to accountType table |
| MORTGAGE/LOAN cannot be debited via payment | DBCRFUN.cbl:330 | accountType.allowDebit flag |
| Insufficient funds check | DBCRFUN.cbl:341 | Application-level check |
| Transfer amount must be positive | XFRFUN.cbl:289 | Application-level check |
| Cannot transfer to same account | XFRFUN.cbl:316 | Application-level check |
| New accounts start with balance 0.00 | Account.java:742 | DEFAULT 0.00 on balance columns |
| Overdraft limit non-negative | Inferred | CHECK (overdraftLimit >= 0) |
| Credit score range 0-999 | CRDTAGY1-5.cbl | CHECK (creditScore BETWEEN 0 AND 999) |
| Customer cascade delete | DELCUS.cbl | ON DELETE CASCADE FK constraint |

## Running the Schema

### Prerequisites
- PostgreSQL 14+ (for GENERATED ALWAYS AS IDENTITY)
- `psql` CLI

### Quick Start
```bash
# Start PostgreSQL (Docker)
docker run -d --name cbsa-postgres \
  -e POSTGRES_DB=cbsa \
  -e POSTGRES_USER=cbsa_admin \
  -e POSTGRES_PASSWORD=cbsa_dev_password \
  -p 5432:5432 \
  postgres:16

# Apply schema
psql -h localhost -U cbsa_admin -d cbsa -f etc/postgres/schema/001_create_schema.sql

# Load seed data
psql -h localhost -U cbsa_admin -d cbsa -f etc/postgres/schema/002_seed_data.sql

# Verify
psql -h localhost -U cbsa_admin -d cbsa -c "SELECT * FROM cbsa.accountCountByBranch;"
```
