# Entity Relationship Diagram (New PostgreSQL Schema)

```mermaid
erDiagram
    branch ||--o{ customer : "has"
    branch ||--o{ account : "has"
    branch ||--o{ transaction : "has"
    customer ||--o{ account : "owns (max 10)"
    account ||--o{ transaction : "generates"
    accountType ||--o{ account : "classifies"
    transactionType ||--o{ transaction : "classifies"

    branch {
        char6 sortCode PK
        varchar100 branchName
        timestamp createdAt
        timestamp updatedAt
    }

    customer {
        bigint id PK
        char6 sortCode FK
        varchar10 customerNumber UK
        varchar60 customerName
        varchar160 customerAddress
        date dateOfBirth
        smallint creditScore
        date creditScoreReviewDate
        timestamp createdAt
        timestamp updatedAt
    }

    account {
        bigint id PK
        char6 sortCode FK
        varchar8 accountNumber UK
        varchar10 customerNumber FK
        varchar8 accountType FK
        numeric6_2 interestRate
        date opened
        integer overdraftLimit
        date lastStatement
        date nextStatement
        numeric12_2 availableBalance
        numeric12_2 actualBalance
        timestamp createdAt
        timestamp updatedAt
    }

    transaction {
        bigint id PK
        char6 sortCode FK
        varchar8 accountNumber FK
        timestamp transactionTimestamp
        varchar12 reference
        char3 transactionType FK
        varchar40 description
        numeric12_2 amount
        timestamp createdAt
    }

    accountType {
        varchar8 code PK
        varchar50 description
        boolean allowDebit
        timestamp createdAt
    }

    transactionType {
        char3 code PK
        varchar60 description
        varchar20 category
        timestamp createdAt
    }
```

## Cardinality Rules

| Relationship | Cardinality | Business Rule |
|---|---|---|
| branch → customer | 1:N | A branch can have many customers |
| branch → account | 1:N | A branch can have many accounts |
| customer → account | 1:N (max 10) | A customer can have up to 10 accounts |
| account → transaction | 1:N | An account has many transactions (append-only) |
| accountType → account | 1:N | Each account has exactly one type |
| transactionType → transaction | 1:N | Each transaction has exactly one type |

## Index Strategy

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         INDEX COVERAGE MAP                                │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  customer table:                                                         │
│  ┌──────────────────────────────────────────────────────────────┐       │
│  │ PK:    id                                                     │       │
│  │ UQ:    (sortCode, customerNumber)      ← original VSAM key   │       │
│  │ IDX:   customerName (pattern_ops)      ← name search API     │       │
│  └──────────────────────────────────────────────────────────────┘       │
│                                                                          │
│  account table:                                                          │
│  ┌──────────────────────────────────────────────────────────────┐       │
│  │ PK:    id                                                     │       │
│  │ UQ:    (sortCode, accountNumber)       ← original DB2 PK     │       │
│  │ IDX:   (sortCode, customerNumber)      ← customer lookup     │       │
│  │ IDX:   (sortCode, actualBalance)       ← balance queries     │       │
│  └──────────────────────────────────────────────────────────────┘       │
│                                                                          │
│  transaction table:                                                      │
│  ┌──────────────────────────────────────────────────────────────┐       │
│  │ PK:    id                                                     │       │
│  │ IDX:   (sortCode, transactionTimestamp) ← main query pattern │       │
│  │ IDX:   (sortCode, accountNumber)        ← account history    │       │
│  │ IDX:   transactionType                  ← type filtering     │       │
│  └──────────────────────────────────────────────────────────────┘       │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```
