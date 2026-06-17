# Normalization Mapping: DB2/VSAM → PostgreSQL

## Before vs. After Architecture

```
╔══════════════════════════════════════════════════════════════════════════════╗
║                        BEFORE (DB2 + VSAM)                                  ║
╠══════════════════════════════════════════════════════════════════════════════╣
║                                                                              ║
║  ┌─────── DB2 ────────────────────────────────────────────────────────┐     ║
║  │                                                                     │     ║
║  │  ACCOUNT table          PROCTRAN table         CONTROL table        │     ║
║  │  ┌───────────────┐      ┌───────────────┐      ┌─────────────┐    │     ║
║  │  │ EYECATCHER    │      │ EYECATCHER    │      │ NAME (PK)   │    │     ║
║  │  │ CUST_NUMBER   │      │ SORTCODE      │      │ VALUE_NUM   │    │     ║
║  │  │ SORTCODE (PK) │      │ NUMBER        │      │ VALUE_STR   │    │     ║
║  │  │ NUMBER (PK)   │      │ DATE          │      └─────────────┘    │     ║
║  │  │ TYPE          │      │ TIME          │                          │     ║
║  │  │ INTEREST_RATE │      │ REF           │      NO PK on PROCTRAN! │     ║
║  │  │ OPENED        │      │ TYPE          │      No FK anywhere!     │     ║
║  │  │ OVERDRAFT_LIM │      │ DESC          │                          │     ║
║  │  │ LAST_STMT     │      │ AMOUNT        │      CONTROL used as    │     ║
║  │  │ NEXT_STMT     │      └───────────────┘      poor-man's SEQUENCE│     ║
║  │  │ AVAIL_BAL     │         No PK!                                  │     ║
║  │  │ ACTUAL_BAL    │         No FK to ACCOUNT!                       │     ║
║  │  └───────────────┘                                                  │     ║
║  │     No FK to CUSTOMER!                                              │     ║
║  └─────────────────────────────────────────────────────────────────────┘     ║
║                                                                              ║
║  ┌─────── VSAM ───────────────────────────────────────────────────────┐     ║
║  │                                                                     │     ║
║  │  CUSTOMER file (KSDS)                                               │     ║
║  │  ┌──────────────────────┐                                           │     ║
║  │  │ EYECATCHER           │     Fixed-length 249-byte records         │     ║
║  │  │ SORTCODE + CUSTNO    │◄─── Composite 16-byte key                 │     ║
║  │  │ NAME (60 chars)      │     (NAME could be structured but isn't)  │     ║
║  │  │ ADDRESS (160 chars)  │     (ADDRESS could be structured)         │     ║
║  │  │ DOB (DDMMYYYY)       │     Date as 8-digit number                │     ║
║  │  │ CREDIT_SCORE (999)   │                                           │     ║
║  │  │ CS_REVIEW_DATE       │                                           │     ║
║  │  └──────────────────────┘                                           │     ║
║  │                                                                     │     ║
║  │  CUSTOMER-CONTROL (special record in same file)                     │     ║
║  │  ┌──────────────────────┐                                           │     ║
║  │  │ Key: 000000+999...9  │     Used as customer # sequence counter   │     ║
║  │  │ NUM_CUSTOMERS        │                                           │     ║
║  │  │ LAST_CUST_NUMBER     │                                           │     ║
║  │  └──────────────────────┘                                           │     ║
║  └─────────────────────────────────────────────────────────────────────┘     ║
║                                                                              ║
║  PROBLEMS:                                                                   ║
║  • No referential integrity (all FKs enforced in application code)           ║
║  • Race condition in Java CONTROL counter (no locking)                       ║
║  • PROCTRAN has no PK (rows not uniquely identifiable)                       ║
║  • Two storage engines (DB2 + VSAM) for one application                      ║
║  • EYECATCHER columns waste space and complicate queries                     ║
║  • Date/time split across two columns in PROCTRAN                            ║
║  • Magic 3-letter codes with no lookup table                                 ║
║  • CONTROL_VALUE_STR is a dead column (always SPACES)                        ║
╚══════════════════════════════════════════════════════════════════════════════╝


                          ║ MIGRATION ║
                          ║           ║
                          ▼           ▼


╔══════════════════════════════════════════════════════════════════════════════╗
║                     AFTER (Normalized PostgreSQL)                            ║
╠══════════════════════════════════════════════════════════════════════════════╣
║                                                                              ║
║  ┌─── Reference Tables ───────────────────────────────────────────────┐     ║
║  │                                                                     │     ║
║  │  branch            accountType          transactionType             │     ║
║  │  ┌──────────┐     ┌─────────────┐      ┌───────────────┐          │     ║
║  │  │sortCode  │     │ code (PK)   │      │ code (PK)     │          │     ║
║  │  │branchName│     │ description │      │ description   │          │     ║
║  │  └──────────┘     │ allowDebit  │      │ category      │          │     ║
║  │       ▲            └─────────────┘      └───────────────┘          │     ║
║  │       │                   ▲                     ▲                   │     ║
║  └───────┼───────────────────┼─────────────────────┼───────────────────┘     ║
║          │                   │                     │                         ║
║  ┌───────┼───── Core Entity Tables ────────────────┼───────────────────┐     ║
║  │       │                   │                     │                   │     ║
║  │  ┌────┴──────────┐  ┌────┴──────────────┐  ┌───┴──────────────┐   │     ║
║  │  │  customer     │  │    account         │  │   transaction    │   │     ║
║  │  ├───────────────┤  ├───────────────────┤  ├──────────────────┤   │     ║
║  │  │ id (PK)       │  │ id (PK)           │  │ id (PK)          │   │     ║
║  │  │ sortCode (FK) │  │ sortCode (FK)     │  │ sortCode (FK)    │   │     ║
║  │  │ customerNum   │  │ accountNumber     │  │ accountNumber    │   │     ║
║  │  │ customerName  │  │ customerNumber FK─┼──│ timestamp        │   │     ║
║  │  │ address       │  │ accountType (FK)  │  │ reference        │   │     ║
║  │  │ dateOfBirth   │  │ interestRate      │  │ transType (FK)   │   │     ║
║  │  │ creditScore   │  │ overdraftLimit    │  │ description      │   │     ║
║  │  │ reviewDate    │  │ availableBalance  │  │ amount           │   │     ║
║  │  │ createdAt     │  │ actualBalance     │  │ createdAt        │   │     ║
║  │  │ updatedAt     │  │ createdAt         │  └──────────────────┘   │     ║
║  │  └───────────────┘  │ updatedAt         │                         │     ║
║  │         ▲            └───────────────────┘                         │     ║
║  │         │                    ▲                                     │     ║
║  │         │ ON DELETE CASCADE  │ account.accountNumber               │     ║
║  │         └────────────────────┘ → transaction.accountNumber         │     ║
║  └────────────────────────────────────────────────────────────────────┘     ║
║                                                                              ║
║  ┌─── Sequences (replace CONTROL table) ──────────────────────────────┐     ║
║  │  accountNumberSeq   → nextval() for new account numbers            │     ║
║  │  customerNumberSeq  → nextval() for new customer numbers           │     ║
║  └────────────────────────────────────────────────────────────────────┘     ║
║                                                                              ║
║  ┌─── Views (replace manually-maintained counters) ───────────────────┐     ║
║  │  accountCountByBranch → SELECT COUNT(*) GROUP BY sortCode          │     ║
║  └────────────────────────────────────────────────────────────────────┘     ║
║                                                                              ║
║  IMPROVEMENTS:                                                               ║
║  ✓ Full referential integrity with FK constraints                            ║
║  ✓ Atomic sequences (no race conditions)                                     ║
║  ✓ Every table has a PK                                                      ║
║  ✓ Single storage engine                                                     ║
║  ✓ No dead columns (EYECATCHER, CONTROL_VALUE_STR removed)                   ║
║  ✓ Proper TIMESTAMP type for transaction date/time                           ║
║  ✓ Lookup tables for codes (self-documenting, extensible)                    ║
║  ✓ Cascade deletes at DB level (replaces fragile application logic)          ║
║  ✓ Audit timestamps (createdAt/updatedAt) on all entities                    ║
╚══════════════════════════════════════════════════════════════════════════════╝
```

## Data Flow: Account Creation (Before vs. After)

### Before (DB2 + VSAM + Application Locking)
```
┌──────────┐     ┌───────────────────────────────────────────────────────────┐
│  Client  │────▶│                     CICS Region                           │
└──────────┘     │                                                           │
                 │  1. ENQ RESOURCE('CBSAACCT987654')     ◄── manual lock    │
                 │  2. SELECT FROM CONTROL                                   │
                 │     WHERE NAME='987654-ACCOUNT-LAST'                      │
                 │  3. INCREMENT value in application     ◄── app logic      │
                 │  4. UPDATE CONTROL SET VALUE=new_val                      │
                 │  5. SELECT FROM CONTROL                                   │
                 │     WHERE NAME='987654-ACCOUNT-COUNT'                     │
                 │  6. INCREMENT count in application                        │
                 │  7. UPDATE CONTROL SET VALUE=new_count                    │
                 │  8. LINK TO INQCUST (verify customer)  ◄── VSAM read     │
                 │  9. INSERT INTO ACCOUNT (...)                             │
                 │ 10. INSERT INTO PROCTRAN (...)                            │
                 │ 11. DEQ RESOURCE('CBSAACCT987654')     ◄── unlock        │
                 └───────────────────────────────────────────────────────────┘
                         11 steps, 2 data stores, manual locking
```

### After (PostgreSQL with Sequences)
```
┌──────────┐     ┌───────────────────────────────────────────────────────────┐
│  Client  │────▶│                   Application Server                      │
└──────────┘     │                                                           │
                 │  BEGIN;                                                    │
                 │  1. SELECT nextval('accountNumberSeq') ◄── atomic, no lock│
                 │  2. INSERT INTO account (...)          ◄── FK validates   │
                 │  3. INSERT INTO transaction (...)       customer exists    │
                 │  COMMIT;                                                   │
                 └───────────────────────────────────────────────────────────┘
                         3 steps, 1 data store, no manual locking
```

## Transaction Type Normalization

```
┌─────────────────────────────────────────────────────────────────────┐
│                   transactionType Reference Table                     │
├───────┬──────────────────────────────────┬──────────────────────────┤
│ Code  │ Description                      │ Category                 │
├───────┼──────────────────────────────────┼──────────────────────────┤
│  CHA  │ Cheque Acknowledged              │ CHEQUE                   │
│  CHF  │ Cheque Failure                   │ CHEQUE                   │
│  CHI  │ Cheque Paid In                   │ CHEQUE                   │
│  CHO  │ Cheque Paid Out                  │ CHEQUE                   │
├───────┼──────────────────────────────────┼──────────────────────────┤
│  CRE  │ Credit                           │ CREDIT                   │
│  PCR  │ Payment Credit                   │ CREDIT                   │
├───────┼──────────────────────────────────┼──────────────────────────┤
│  DEB  │ Debit                            │ DEBIT                    │
│  PDR  │ Payment Debit                    │ DEBIT                    │
├───────┼──────────────────────────────────┼──────────────────────────┤
│  TFR  │ Transfer                         │ TRANSFER                 │
├───────┼──────────────────────────────────┼──────────────────────────┤
│  OCA  │ Branch Create Account            │ ACCOUNT_LIFECYCLE        │
│  ICA  │ Web Create Account               │ ACCOUNT_LIFECYCLE        │
│  ODA  │ Branch Delete Account            │ ACCOUNT_LIFECYCLE        │
│  IDA  │ Web Delete Account               │ ACCOUNT_LIFECYCLE        │
├───────┼──────────────────────────────────┼──────────────────────────┤
│  OCC  │ Branch Create Customer           │ CUSTOMER_LIFECYCLE       │
│  ICC  │ Web Create Customer              │ CUSTOMER_LIFECYCLE       │
│  ODC  │ Branch Delete Customer           │ CUSTOMER_LIFECYCLE       │
│  IDC  │ Web Delete Customer              │ CUSTOMER_LIFECYCLE       │
├───────┼──────────────────────────────────┼──────────────────────────┤
│  OCS  │ Create Standing Order/DD         │ STANDING_ORDER           │
└───────┴──────────────────────────────────┴──────────────────────────┘
```
