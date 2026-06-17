-- ============================================================================
-- CBSA PostgreSQL Schema Migration
-- Migrates from DB2 + VSAM to a normalized PostgreSQL schema
-- ============================================================================

-- Create schema namespace
CREATE SCHEMA IF NOT EXISTS cbsa;
SET search_path TO cbsa, public;

-- ============================================================================
-- REFERENCE / LOOKUP TABLES
-- ============================================================================

-- Branch table: normalizes the sort_code that was previously denormalized
-- across every row in every table
CREATE TABLE cbsa.branch (
    sortCode        CHAR(6) PRIMARY KEY,
    branchName      VARCHAR(100),
    createdAt       TIMESTAMP NOT NULL DEFAULT NOW(),
    updatedAt       TIMESTAMP NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE cbsa.branch IS 'Bank branch lookup. Replaces denormalized ACCOUNT_SORTCODE/PROCTRAN_SORTCODE/CUSTOMER_SORTCODE.';

-- Account type reference table: enforces the 5 valid types as a proper lookup
CREATE TABLE cbsa.accountType (
    code            VARCHAR(8) PRIMARY KEY,
    description     VARCHAR(50) NOT NULL,
    allowDebit      BOOLEAN NOT NULL DEFAULT TRUE,
    createdAt       TIMESTAMP NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE cbsa.accountType IS 'Normalized from ACCOUNT_TYPE CHAR(8). Business rule: MORTGAGE and LOAN cannot be debited via payment facility.';

INSERT INTO cbsa.accountType (code, description, allowDebit) VALUES
    ('ISA',      'Individual Savings Account', TRUE),
    ('MORTGAGE', 'Mortgage Account',           FALSE),
    ('SAVING',   'Savings Account',            TRUE),
    ('CURRENT',  'Current Account',            TRUE),
    ('LOAN',     'Loan Account',               FALSE);

-- Transaction type reference table: normalizes the 18 PROCTRAN_TYPE codes
CREATE TABLE cbsa.transactionType (
    code            CHAR(3) PRIMARY KEY,
    description     VARCHAR(60) NOT NULL,
    category        VARCHAR(20) NOT NULL,
    createdAt       TIMESTAMP NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE cbsa.transactionType IS 'Normalized from PROCTRAN_TYPE CHAR(3). 18 distinct transaction type codes.';

INSERT INTO cbsa.transactionType (code, description, category) VALUES
    ('CHA', 'Cheque Acknowledged',             'CHEQUE'),
    ('CHF', 'Cheque Failure',                  'CHEQUE'),
    ('CHI', 'Cheque Paid In',                  'CHEQUE'),
    ('CHO', 'Cheque Paid Out',                 'CHEQUE'),
    ('CRE', 'Credit',                          'CREDIT'),
    ('DEB', 'Debit',                           'DEBIT'),
    ('ICA', 'Web Create Account',              'ACCOUNT_LIFECYCLE'),
    ('ICC', 'Web Create Customer',             'CUSTOMER_LIFECYCLE'),
    ('IDA', 'Web Delete Account',              'ACCOUNT_LIFECYCLE'),
    ('IDC', 'Web Delete Customer',             'CUSTOMER_LIFECYCLE'),
    ('OCA', 'Branch Create Account',           'ACCOUNT_LIFECYCLE'),
    ('OCC', 'Branch Create Customer',          'CUSTOMER_LIFECYCLE'),
    ('ODA', 'Branch Delete Account',           'ACCOUNT_LIFECYCLE'),
    ('ODC', 'Branch Delete Customer',          'CUSTOMER_LIFECYCLE'),
    ('OCS', 'Create Standing Order/Direct Debit', 'STANDING_ORDER'),
    ('PCR', 'Payment Credit',                  'CREDIT'),
    ('PDR', 'Payment Debit',                   'DEBIT'),
    ('TFR', 'Transfer',                        'TRANSFER');

-- ============================================================================
-- SEQUENCES (replace DB2 CONTROL table and VSAM CUSTOMER-CONTROL)
-- ============================================================================

-- Replaces CONTROL row '<sortcode>-ACCOUNT-LAST' (read-increment-update pattern)
-- PostgreSQL sequences are atomic and lock-free — eliminates the race condition
-- bug identified in the Java path
CREATE SEQUENCE cbsa.accountNumberSeq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    CACHE 10;

COMMENT ON SEQUENCE cbsa.accountNumberSeq IS 'Replaces DB2 CONTROL table ACCOUNT-LAST counter. Atomic, no locking needed.';

-- Replaces VSAM CUSTOMER-CONTROL Named Counter Server pattern
CREATE SEQUENCE cbsa.customerNumberSeq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    CACHE 10;

COMMENT ON SEQUENCE cbsa.customerNumberSeq IS 'Replaces VSAM Named Counter Server (HBNK-CUST-NO). Atomic, no locking needed.';

-- ============================================================================
-- CORE ENTITY TABLES
-- ============================================================================

-- CUSTOMER table: migrated from VSAM KSDS to PostgreSQL
-- Normalization: structured name/address fields (commented fields in COBOL copybook
-- are now first-class columns)
CREATE TABLE cbsa.customer (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sortCode            CHAR(6) NOT NULL REFERENCES cbsa.branch(sortCode),
    customerNumber      VARCHAR(10) NOT NULL,
    customerName        VARCHAR(60) NOT NULL,
    customerAddress     VARCHAR(160) NOT NULL DEFAULT '',
    dateOfBirth         DATE NOT NULL,
    creditScore         SMALLINT NOT NULL DEFAULT 0
                        CHECK (creditScore BETWEEN 0 AND 999),
    creditScoreReviewDate DATE,
    createdAt           TIMESTAMP NOT NULL DEFAULT NOW(),
    updatedAt           TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Composite unique constraint preserving original VSAM key semantics
    CONSTRAINT uq_customer_sortcode_number UNIQUE (sortCode, customerNumber)
);

COMMENT ON TABLE cbsa.customer IS 'Migrated from VSAM KSDS. Original key: CUSTOMER-SORTCODE(6) + CUSTOMER-NUMBER(10).';

CREATE INDEX idx_customer_name ON cbsa.customer (customerName varchar_pattern_ops);
COMMENT ON INDEX idx_customer_name IS 'Supports substring name search (getCustomersByNameExternal).';

-- ACCOUNT table: migrated from DB2 IBMUSER.ACCOUNT
-- Normalization:
--   - Removed EYECATCHER column (always 'ACCT', replaced by table structure)
--   - ACCOUNT_TYPE now FK to accountType reference table
--   - Proper FK to customer
CREATE TABLE cbsa.account (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sortCode            CHAR(6) NOT NULL REFERENCES cbsa.branch(sortCode),
    accountNumber       VARCHAR(8) NOT NULL,
    customerNumber      VARCHAR(10) NOT NULL,
    accountType         VARCHAR(8) NOT NULL REFERENCES cbsa.accountType(code),
    interestRate        NUMERIC(6, 2) NOT NULL DEFAULT 0.00,
    opened              DATE NOT NULL DEFAULT CURRENT_DATE,
    overdraftLimit      INTEGER NOT NULL DEFAULT 0
                        CHECK (overdraftLimit >= 0),
    lastStatement       DATE,
    nextStatement       DATE,
    availableBalance    NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    actualBalance       NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    createdAt           TIMESTAMP NOT NULL DEFAULT NOW(),
    updatedAt           TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Composite unique constraint preserving original DB2 index semantics
    CONSTRAINT uq_account_sortcode_number UNIQUE (sortCode, accountNumber),
    -- FK to customer (enables ON DELETE CASCADE replacing application-level cascade)
    CONSTRAINT fk_account_customer
        FOREIGN KEY (sortCode, customerNumber)
        REFERENCES cbsa.customer(sortCode, customerNumber)
        ON DELETE CASCADE
);

COMMENT ON TABLE cbsa.account IS 'Migrated from DB2 IBMUSER.ACCOUNT. Original PK: ACCOUNT_SORTCODE + ACCOUNT_NUMBER.';

-- Index for customer lookup (replaces DB2 INDEX ACCTCUST)
CREATE INDEX idx_account_customer ON cbsa.account (sortCode, customerNumber);
COMMENT ON INDEX idx_account_customer IS 'Replaces DB2 INDEX ACCTCUST. Supports getAccounts(customerNumber).';

-- Index for balance range queries
CREATE INDEX idx_account_balance ON cbsa.account (sortCode, actualBalance);
COMMENT ON INDEX idx_account_balance IS 'Supports getAccountsByBalance() queries.';

-- PROCTRAN (Processed Transaction) table: migrated from DB2 IBMUSER.PROCTRAN
-- Normalization:
--   - Removed EYECATCHER column (always 'PRTR')
--   - Combined PROCTRAN_DATE + PROCTRAN_TIME into single TIMESTAMP
--   - PROCTRAN_TYPE now FK to transactionType reference table
--   - Added surrogate PK (original table had no PK — rows not uniquely identifiable)
CREATE TABLE cbsa.transaction (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sortCode            CHAR(6) NOT NULL REFERENCES cbsa.branch(sortCode),
    accountNumber       VARCHAR(8) NOT NULL,
    transactionTimestamp TIMESTAMP NOT NULL DEFAULT NOW(),
    reference           VARCHAR(12) NOT NULL,
    transactionType     CHAR(3) NOT NULL REFERENCES cbsa.transactionType(code),
    description         VARCHAR(40),
    amount              NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    createdAt           TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Note: No hard FK to account because customer-level events (OCC, ODC, ICC, IDC)
    -- use accountNumber='00000000' which has no corresponding account row.
    -- Application-level validation enforces referential integrity for account-level events.
    CONSTRAINT chk_account_number_format
        CHECK (accountNumber ~ '^[0-9]{1,8}$')
);

COMMENT ON TABLE cbsa.transaction IS 'Migrated from DB2 IBMUSER.PROCTRAN. Append-only audit trail. Original had no PK.';

-- Primary query index (replaces the ORDER BY PROCTRAN_DATE, PROCTRAN_TIME pattern)
CREATE INDEX idx_transaction_sortcode_ts ON cbsa.transaction (sortCode, transactionTimestamp);
COMMENT ON INDEX idx_transaction_sortcode_ts IS 'Covers primary getProcessedTransactions() query: filter by sortCode, order by timestamp.';

-- Index for account-level transaction lookup
CREATE INDEX idx_transaction_account ON cbsa.transaction (sortCode, accountNumber);
COMMENT ON INDEX idx_transaction_account IS 'Supports future per-account transaction history queries.';

-- Index for transaction type filtering
CREATE INDEX idx_transaction_type ON cbsa.transaction (transactionType);

-- ============================================================================
-- MATERIALIZED VIEW: Account count per branch (replaces CONTROL ACCOUNT-COUNT)
-- ============================================================================

CREATE OR REPLACE VIEW cbsa.accountCountByBranch AS
SELECT
    sortCode,
    COUNT(*) AS accountCount
FROM cbsa.account
GROUP BY sortCode;

COMMENT ON VIEW cbsa.accountCountByBranch IS 'Replaces CONTROL <sortcode>-ACCOUNT-COUNT rows. Computed dynamically instead of manually maintained.';

-- ============================================================================
-- TRIGGER: Auto-update updatedAt timestamps
-- ============================================================================

CREATE OR REPLACE FUNCTION cbsa.updateTimestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updatedAt = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_customer_updated
    BEFORE UPDATE ON cbsa.customer
    FOR EACH ROW EXECUTE FUNCTION cbsa.updateTimestamp();

CREATE TRIGGER trg_account_updated
    BEFORE UPDATE ON cbsa.account
    FOR EACH ROW EXECUTE FUNCTION cbsa.updateTimestamp();
