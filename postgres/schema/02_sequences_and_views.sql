-- =============================================================================
-- CONTROL table replacement + derived views
-- =============================================================================
-- The Db2 CONTROL table was a generic key/value counter store holding only:
--   '<sortcode>-ACCOUNT-LAST'  -> next account number   (read-modify-write, race-prone)
--   '<sortcode>-ACCOUNT-COUNT' -> account count         (drifted: not decremented on delete,
--                                                         not maintained by the web path)
-- We replace LAST with native SEQUENCEs (atomic) and derive COUNT via a view.
-- Customer numbering (CBSA: VSAM control record + CICS named counter) also
-- becomes a sequence here.
-- =============================================================================

SET search_path TO cbsa;

-- Next account / customer numbers. CBSA numbers are per sort code; with a single
-- demo sort code a global sequence is sufficient. For true multi-branch numbering
-- use one sequence per branch (see postgres/docs/MIGRATION.md).
CREATE SEQUENCE IF NOT EXISTS account_number_seq  AS BIGINT START 1 MINVALUE 1 MAXVALUE 99999999;
CREATE SEQUENCE IF NOT EXISTS customer_number_seq AS BIGINT START 1 MINVALUE 1 MAXVALUE 9999999999;

-- Derived account count per branch (replaces the drift-prone ACCOUNT-COUNT row).
CREATE OR REPLACE VIEW branch_account_count AS
    SELECT b.sort_code,
           COUNT(a.account_id) AS account_count
    FROM branch b
    LEFT JOIN account a ON a.sort_code = b.sort_code
    GROUP BY b.sort_code;

COMMENT ON VIEW branch_account_count IS 'Replaces CONTROL "<sortcode>-ACCOUNT-COUNT"; always accurate because it is derived.';

-- Convenience read model mirroring the CBSA "list transactions for an account"
-- query, with the transfer counterparty joined back in.
CREATE OR REPLACE VIEW transaction_detail AS
    SELECT t.transaction_id,
           a.sort_code,
           a.account_number,
           t.occurred_at,
           t.type_code,
           tt.description AS type_description,
           tt.category,
           t.amount,
           t.reference,
           t.description,
           td.counterparty_sort_code,
           td.counterparty_account_number
    FROM account_transaction t
    JOIN account a            ON a.account_id = t.account_id
    JOIN transaction_type tt  ON tt.type_code = t.type_code
    LEFT JOIN transfer_detail td ON td.transaction_id = t.transaction_id;
