-- =============================================================================
-- Schema verification tests (fail loudly via ASSERT)
-- =============================================================================
SET search_path TO cbsa;

DO $$
DECLARE
    v_turing   BIGINT;
    v_cur_acc  BIGINT;
    v_sav_acc  BIGINT;
    v_lov_acc  BIGINT;
    v_txn      BIGINT;
    v_count    BIGINT;
BEGIN
    SELECT customer_id INTO v_turing FROM customer WHERE family_name = 'Turing';

    ---------------------------------------------------------------------------
    -- 1. FK enforcement: account with unknown customer must fail.
    ---------------------------------------------------------------------------
    BEGIN
        INSERT INTO account (sort_code, account_number, customer_id, account_type_code)
        VALUES ('987654', '00099999', 999999, 'CURRENT ');
        RAISE EXCEPTION 'TEST FAILED: FK to customer was not enforced';
    EXCEPTION WHEN foreign_key_violation THEN
        RAISE NOTICE 'OK  1 FK customer enforced';
    END;

    ---------------------------------------------------------------------------
    -- 2. FK to branch enforced.
    ---------------------------------------------------------------------------
    BEGIN
        INSERT INTO account (sort_code, account_number, customer_id, account_type_code)
        VALUES ('000000', '00088888', v_turing, 'CURRENT ');
        RAISE EXCEPTION 'TEST FAILED: FK to branch was not enforced';
    EXCEPTION WHEN foreign_key_violation THEN
        RAISE NOTICE 'OK  2 FK branch enforced';
    END;

    ---------------------------------------------------------------------------
    -- 3. Natural-key uniqueness (sort_code, account_number).
    ---------------------------------------------------------------------------
    SELECT account_id, account_number INTO v_cur_acc, v_sav_acc
    FROM account WHERE customer_id = v_turing AND account_type_code = 'CURRENT ';
    BEGIN
        INSERT INTO account (sort_code, account_number, customer_id, account_type_code)
        SELECT sort_code, account_number, v_turing, 'SAVING  '
        FROM account WHERE account_id = v_cur_acc;
        RAISE EXCEPTION 'TEST FAILED: duplicate (sort_code, account_number) allowed';
    EXCEPTION WHEN unique_violation THEN
        RAISE NOTICE 'OK  3 natural key uniqueness enforced';
    END;

    ---------------------------------------------------------------------------
    -- 4. Overdraft CHECK: available_balance >= -overdraft_limit.
    ---------------------------------------------------------------------------
    BEGIN
        UPDATE account SET available_balance = -600.00
        WHERE account_id = v_cur_acc;   -- overdraft_limit is 500
        RAISE EXCEPTION 'TEST FAILED: overdraft CHECK not enforced';
    EXCEPTION WHEN check_violation THEN
        RAISE NOTICE 'OK  4 overdraft CHECK enforced';
    END;

    ---------------------------------------------------------------------------
    -- 5. credit_score domain CHECK.
    ---------------------------------------------------------------------------
    BEGIN
        UPDATE customer SET credit_score = 1500 WHERE customer_id = v_turing;
        RAISE EXCEPTION 'TEST FAILED: credit_score CHECK not enforced';
    EXCEPTION WHEN check_violation THEN
        RAISE NOTICE 'OK  5 credit_score CHECK enforced';
    END;

    ---------------------------------------------------------------------------
    -- 6. Transfer: two journal rows + transfer_detail counterparty resolves.
    ---------------------------------------------------------------------------
    SELECT account_id INTO v_lov_acc FROM account a
      JOIN customer c ON c.customer_id = a.customer_id WHERE c.family_name = 'Lovelace';

    INSERT INTO account_transaction (account_id, type_code, amount, description)
        VALUES (v_cur_acc, 'TFR', -100.00, 'Transfer to Lovelace') RETURNING transaction_id INTO v_txn;
    INSERT INTO transfer_detail (transaction_id, counterparty_sort_code, counterparty_account_number, counterparty_account_id)
        SELECT v_txn, a.sort_code, a.account_number, a.account_id FROM account a WHERE a.account_id = v_lov_acc;

    SELECT count(*) INTO v_count FROM transaction_detail
        WHERE transaction_id = v_txn AND counterparty_account_number IS NOT NULL;
    ASSERT v_count = 1, 'TEST FAILED: transfer_detail did not join through transaction_detail';
    RAISE NOTICE 'OK  6 transfer counterparty normalized + joined';

    ---------------------------------------------------------------------------
    -- 7. Derived count view replaces CONTROL ACCOUNT-COUNT and stays accurate.
    ---------------------------------------------------------------------------
    SELECT account_count INTO v_count FROM branch_account_count WHERE sort_code = '987654';
    ASSERT v_count = (SELECT count(*) FROM account WHERE sort_code = '987654'),
        'TEST FAILED: branch_account_count view mismatch';
    RAISE NOTICE 'OK  7 derived branch_account_count accurate (=%).', v_count;

    ---------------------------------------------------------------------------
    -- 8. Lifecycle vs financial categorization present.
    ---------------------------------------------------------------------------
    ASSERT (SELECT count(*) FROM transaction_type WHERE category = 'FINANCIAL') = 9
        AND (SELECT count(*) FROM transaction_type WHERE category = 'LIFECYCLE') = 9,
        'TEST FAILED: transaction_type seed incomplete';
    RAISE NOTICE 'OK  8 transaction_type lookup seeded (9 financial / 9 lifecycle)';

    RAISE NOTICE '=== ALL SCHEMA TESTS PASSED ===';
END $$;
