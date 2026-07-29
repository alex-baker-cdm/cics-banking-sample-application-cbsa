-- =============================================================================
-- CBSA -> PostgreSQL : Normalized schema (3NF)
-- =============================================================================
-- Source: IBM CICS Banking Sample Application (CBSA)
--   Db2 tables : ACCOUNT, PROCTRAN, CONTROL   (etc/install/base/db2jcl/INSTDB2.jcl)
--   VSAM KSDS  : CUSTOMER                      (src/base/cobol_copy/CUSTOMER.cpy)
--
-- Design goals (derived from the per-table access-pattern analyses under
-- postgres/docs/*_analysis.md):
--   * Real primary keys, foreign keys and CHECK constraints (CBSA declared NONE;
--     every relationship was enforced only in COBOL/Java).
--   * De-duplicate the branch "sort code" that was copied into every table.
--   * Decompose overloaded / positional fields (CUSTOMER name+address, the
--     polymorphic PROCTRAN_DESC) into typed columns and dependent tables.
--   * Replace the CONTROL key/value counter table with native SEQUENCEs and
--     derived COUNT(*) views (fixes the documented counter-drift + race bugs).
--   * Drop legacy storage artifacts: 'ACCT'/'PRTR'/'CUST' eyecatchers, the
--     X'FF' logical-delete redefine, and the always-blank CONTROL_VALUE_STR.
--   * Merge PROCTRAN_DATE (3 inconsistent encodings) + PROCTRAN_TIME into one
--     TIMESTAMP.
--
-- Naming: snake_case. PostgreSQL folds unquoted identifiers to lower case, so
-- camelCase would force quoting everywhere; snake_case is the portable choice.
-- =============================================================================

BEGIN;

CREATE SCHEMA IF NOT EXISTS cbsa;
SET search_path TO cbsa;

-- -----------------------------------------------------------------------------
-- branch  (NEW) -- extracts the 6-digit sort code duplicated across ACCOUNT,
--                  CUSTOMER, PROCTRAN and smuggled into CONTROL_NAME.
-- -----------------------------------------------------------------------------
CREATE TABLE branch (
    sort_code   CHAR(6)     PRIMARY KEY
                            CHECK (sort_code ~ '^[0-9]{6}$'),
    branch_name TEXT
);

COMMENT ON TABLE  branch            IS 'Bank branch / sort code. Normalizes the sort code that CBSA repeated in every table.';
COMMENT ON COLUMN branch.sort_code  IS 'Six-digit branch sort code (was ACCOUNT_SORTCODE / CUSTOMER_SORTCODE / PROCTRAN_SORTCODE).';

-- -----------------------------------------------------------------------------
-- account_type  (NEW lookup) -- was the free-text ACCOUNT_TYPE CHAR(8).
-- -----------------------------------------------------------------------------
CREATE TABLE account_type (
    account_type_code CHAR(8)  PRIMARY KEY,
    description       TEXT     NOT NULL
);

COMMENT ON TABLE account_type IS 'Lookup for account product types (was free-text ACCOUNT_TYPE).';

-- -----------------------------------------------------------------------------
-- customer  (from VSAM CUSTOMER) -- name & address decomposed into components
--   (the COBOL copybook itself documented these sub-fields as commented-out
--    07-levels: title / given-name / initials / family-name and
--    street / district / town / postcode).
-- -----------------------------------------------------------------------------
CREATE TABLE customer (
    customer_id       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sort_code         CHAR(6)  NOT NULL REFERENCES branch (sort_code),
    customer_number   CHAR(10) NOT NULL
                               CHECK (customer_number ~ '^[0-9]{10}$'),
    title             TEXT,
    given_name        TEXT,
    initials          TEXT,
    family_name       TEXT,
    address_street    TEXT,
    address_district  TEXT,
    address_town      TEXT,
    address_postcode  TEXT,
    date_of_birth     DATE,
    credit_score      SMALLINT CHECK (credit_score BETWEEN 0 AND 999),
    credit_score_reviewed_on DATE,
    CONSTRAINT uq_customer_natural UNIQUE (sort_code, customer_number)
);

COMMENT ON TABLE  customer      IS 'Bank customer (migrated from the VSAM CUSTOMER KSDS). Name/address split into typed columns.';
COMMENT ON COLUMN customer.credit_score IS 'Aggregate credit score (0-999); source scores come from the CRDTAGY1..5 credit-agency programs.';

-- -----------------------------------------------------------------------------
-- account  (from Db2 ACCOUNT)
--   Real FKs to customer and branch; surrogate PK; natural key kept UNIQUE.
-- -----------------------------------------------------------------------------
CREATE TABLE account (
    account_id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sort_code            CHAR(6)  NOT NULL REFERENCES branch (sort_code),
    account_number       CHAR(8)  NOT NULL
                                  CHECK (account_number ~ '^[0-9]{8}$'),
    customer_id          BIGINT   NOT NULL REFERENCES customer (customer_id),
    account_type_code    CHAR(8)  NOT NULL REFERENCES account_type (account_type_code),
    interest_rate        NUMERIC(6,2)  NOT NULL DEFAULT 0,
    opened_date          DATE     NOT NULL DEFAULT CURRENT_DATE,
    overdraft_limit      NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (overdraft_limit >= 0),
    last_statement_date  DATE,
    next_statement_date  DATE,
    available_balance    NUMERIC(12,2) NOT NULL DEFAULT 0,
    actual_balance       NUMERIC(12,2) NOT NULL DEFAULT 0,
    CONSTRAINT uq_account_natural UNIQUE (sort_code, account_number),
    -- Defensive invariant (stricter than legacy CBSA). CBSA did NOT enforce an
    -- overdraft-limit bound: DBCRFUN only rejects a debit when the resulting
    -- balance would go below zero, and only for the payment facility
    -- (COMM-FACILTYPE=496); teller debits and interest/fee postings were
    -- unbounded. Relax or drop this if migrating real data whose balances can
    -- legitimately fall below -overdraft_limit (e.g. LOAN/MORTGAGE debt).
    CONSTRAINT ck_account_overdraft CHECK (available_balance >= -overdraft_limit)
);

-- ACCTCUST secondary index equivalent: list all accounts for a customer.
CREATE INDEX ix_account_customer ON account (customer_id);
CREATE INDEX ix_account_sortcode ON account (sort_code);

COMMENT ON TABLE  account IS 'Bank account (migrated from Db2 ACCOUNT). Composite natural key (sort_code, account_number) kept UNIQUE; surrogate PK added.';
COMMENT ON COLUMN account.overdraft_limit IS 'Widened from CBSA INTEGER (whole units) to NUMERIC(12,2) for consistency with balances.';

-- -----------------------------------------------------------------------------
-- transaction_type  (NEW lookup) -- was the 3-char PROCTRAN_TYPE magic code.
-- -----------------------------------------------------------------------------
CREATE TABLE transaction_type (
    type_code    CHAR(3)  PRIMARY KEY,
    description  TEXT     NOT NULL,
    -- FINANCIAL = moves money; LIFECYCLE = create/delete audit event.
    category     TEXT     NOT NULL CHECK (category IN ('FINANCIAL','LIFECYCLE'))
);

COMMENT ON TABLE transaction_type IS 'Lookup decoding the PROCTRAN_TYPE 3-char codes (DEB/CRE/TFR/OCA/...).';

-- -----------------------------------------------------------------------------
-- account_transaction  (from Db2 PROCTRAN)
--   Append-only journal. DATE+TIME merged into occurred_at; surrogate PK
--   (PROCTRAN_REF was only the CICS task number, not durable/unique).
-- -----------------------------------------------------------------------------
CREATE TABLE account_transaction (
    transaction_id  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    account_id      BIGINT       NOT NULL REFERENCES account (account_id),
    occurred_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    type_code       CHAR(3)      NOT NULL REFERENCES transaction_type (type_code),
    amount          NUMERIC(12,2) NOT NULL DEFAULT 0,
    reference       TEXT,        -- was PROCTRAN_REF (CICS task number)
    description     TEXT,        -- free-text remainder of PROCTRAN_DESC
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Supports the paged read query (WHERE account ORDER BY date,time).
CREATE INDEX ix_txn_account_time ON account_transaction (account_id, occurred_at);

COMMENT ON TABLE  account_transaction IS 'Append-only transaction/audit journal (migrated from Db2 PROCTRAN). Never UPDATEd/DELETEd in CBSA.';
COMMENT ON COLUMN account_transaction.reference IS 'Was PROCTRAN_REF = CICS EIBTASKN; not unique, kept as an opaque reference.';

-- -----------------------------------------------------------------------------
-- transfer_detail  (NEW) -- decomposes the transfer counterparty that CBSA
--   packed positionally into PROCTRAN_DESC for TYPE='TFR'
--   ("TRANSFER" + counterparty sort code + account number).
--   1:0..1 with account_transaction; only present for transfers.
-- -----------------------------------------------------------------------------
CREATE TABLE transfer_detail (
    transaction_id             BIGINT PRIMARY KEY
                                      REFERENCES account_transaction (transaction_id) ON DELETE CASCADE,
    counterparty_sort_code     CHAR(6) NOT NULL,
    counterparty_account_number CHAR(8) NOT NULL,
    counterparty_account_id    BIGINT REFERENCES account (account_id)
);

COMMENT ON TABLE transfer_detail IS 'Transfer counterparty (was embedded positionally in PROCTRAN_DESC for TYPE=TFR).';

COMMIT;
