# CBSA PostgreSQL — Entity Relationship Diagram

The normalized target schema (schema `cbsa`).

![CBSA normalized ERD](./img/erd.png)

Rendered by GitHub from Mermaid below (source in [`img/erd.mmd`](./img/erd.mmd)):

```mermaid
erDiagram
    branch ||--o{ customer : "has"
    branch ||--o{ account : "has"
    customer ||--o{ account : "owns"
    account_type ||--o{ account : "classifies"
    account ||--o{ account_transaction : "journals"
    transaction_type ||--o{ account_transaction : "typed by"
    account_transaction ||--o| transfer_detail : "counterparty"
    account ||--o{ transfer_detail : "target"

    branch {
        char6 sort_code PK
        text  branch_name
    }
    customer {
        bigint customer_id PK
        char6  sort_code FK
        char10 customer_number
        text   title
        text   given_name
        text   family_name
        text   address_street
        text   address_town
        text   address_postcode
        date   date_of_birth
        smallint credit_score
    }
    account_type {
        char8 account_type_code PK
        text  description
    }
    account {
        bigint account_id PK
        char6  sort_code FK
        char8  account_number
        bigint customer_id FK
        char8  account_type_code FK
        numeric interest_rate
        date    opened_date
        numeric overdraft_limit
        numeric available_balance
        numeric actual_balance
    }
    transaction_type {
        char3 type_code PK
        text  description
        text  category
    }
    account_transaction {
        bigint transaction_id PK
        bigint account_id FK
        timestamp occurred_at
        char3  type_code FK
        numeric amount
        text   reference
        text   description
    }
    transfer_detail {
        bigint transaction_id PK
        char6  counterparty_sort_code
        char8  counterparty_account_number
        bigint counterparty_account_id FK
    }
```

## Source → target overview

```
   Db2 / VSAM (CBSA)                         PostgreSQL (normalized, 3NF)
   ─────────────────                         ────────────────────────────
   CUSTOMER (VSAM KSDS) ───────┐             branch          (NEW, from sort code)
   ACCOUNT  (Db2)  ────────┐   ├──────────►  customer        (name/address split)
   PROCTRAN (Db2)  ──────┐ │   │             account_type    (NEW lookup)
   CONTROL  (Db2)  ─┐    │ │   └──────────►  account
                    │    │ └──────────────►  account_transaction
   sort code (dup   │    └────────────────►  transfer_detail (NEW, from DESC)
   in all 4)  ──────┘                        transaction_type(NEW lookup)
                                             account_number_seq / customer_number_seq (replace CONTROL)
                                             branch_account_count (VIEW, replaces CONTROL count)
```
