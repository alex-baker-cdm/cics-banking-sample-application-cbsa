-- ============================================================================
-- CBSA PostgreSQL Schema Validation Tests
-- Run after 001 + 002 to verify schema integrity
-- ============================================================================

SET search_path TO cbsa, public;

-- ============================================================================
-- TEST: Table existence and record counts
-- ============================================================================

DO $$
DECLARE
    v_count INTEGER;
BEGIN
    -- Verify all core tables exist with data
    SELECT count(*) INTO v_count FROM cbsa.branch;
    ASSERT v_count >= 1, 'branch table should have at least 1 row';

    SELECT count(*) INTO v_count FROM cbsa.customer;
    ASSERT v_count >= 1, 'customer table should have data';

    SELECT count(*) INTO v_count FROM cbsa.account;
    ASSERT v_count >= 1, 'account table should have data';

    SELECT count(*) INTO v_count FROM cbsa.transaction;
    ASSERT v_count >= 1, 'transaction table should have data';

    SELECT count(*) INTO v_count FROM cbsa.accountType;
    ASSERT v_count = 5, 'accountType should have exactly 5 rows';

    SELECT count(*) INTO v_count FROM cbsa.transactionType;
    ASSERT v_count = 18, 'transactionType should have exactly 18 rows';

    RAISE NOTICE 'PASS: All tables exist and contain expected data';
END $$;

-- ============================================================================
-- TEST: Referential integrity
-- ============================================================================

DO $$
DECLARE
    v_count INTEGER;
BEGIN
    -- Every account references a valid customer
    SELECT count(*) INTO v_count
    FROM cbsa.account a
    WHERE NOT EXISTS (
        SELECT 1 FROM cbsa.customer c
        WHERE c.sortCode = a.sortCode AND c.customerNumber = a.customerNumber
    );
    ASSERT v_count = 0, 'All accounts should reference valid customers';

    -- Every account references a valid branch
    SELECT count(*) INTO v_count
    FROM cbsa.account a
    WHERE NOT EXISTS (
        SELECT 1 FROM cbsa.branch b WHERE b.sortCode = a.sortCode
    );
    ASSERT v_count = 0, 'All accounts should reference valid branches';

    -- Every non-system transaction references a valid branch
    SELECT count(*) INTO v_count
    FROM cbsa.transaction t
    WHERE NOT EXISTS (
        SELECT 1 FROM cbsa.branch b WHERE b.sortCode = t.sortCode
    );
    ASSERT v_count = 0, 'All transactions should reference valid branches';

    RAISE NOTICE 'PASS: Referential integrity verified';
END $$;

-- ============================================================================
-- TEST: Sequences produce valid unique numbers
-- ============================================================================

DO $$
DECLARE
    v_num1 BIGINT;
    v_num2 BIGINT;
BEGIN
    v_num1 := nextval('cbsa.accountNumberSeq');
    v_num2 := nextval('cbsa.accountNumberSeq');
    ASSERT v_num2 = v_num1 + 1, 'accountNumberSeq should increment by 1';

    v_num1 := nextval('cbsa.customerNumberSeq');
    v_num2 := nextval('cbsa.customerNumberSeq');
    ASSERT v_num2 = v_num1 + 1, 'customerNumberSeq should increment by 1';

    RAISE NOTICE 'PASS: Sequences produce valid incrementing numbers';
END $$;

-- ============================================================================
-- TEST: Business rule constraints
-- ============================================================================

DO $$
BEGIN
    -- Invalid account type should be rejected
    BEGIN
        INSERT INTO cbsa.account (sortCode, accountNumber, customerNumber, accountType, opened)
        VALUES ('987654', '99999999', '0000000001', 'BADTYPE', CURRENT_DATE);
        ASSERT FALSE, 'Should have rejected invalid account type';
    EXCEPTION WHEN foreign_key_violation THEN
        NULL; -- Expected
    END;

    -- Credit score > 999 should be rejected
    BEGIN
        INSERT INTO cbsa.customer (sortCode, customerNumber, customerName, customerAddress, dateOfBirth, creditScore)
        VALUES ('987654', '9999999999', 'Test', 'Addr', '1990-01-01', 1500);
        ASSERT FALSE, 'Should have rejected credit score > 999';
    EXCEPTION WHEN check_violation THEN
        NULL; -- Expected
    END;

    -- Negative overdraft limit should be rejected
    BEGIN
        INSERT INTO cbsa.account (sortCode, accountNumber, customerNumber, accountType, opened, overdraftLimit)
        VALUES ('987654', '99999999', '0000000001', 'CURRENT', CURRENT_DATE, -100);
        ASSERT FALSE, 'Should have rejected negative overdraft limit';
    EXCEPTION WHEN check_violation THEN
        NULL; -- Expected
    END;

    RAISE NOTICE 'PASS: Business rule constraints enforced correctly';
END $$;

-- ============================================================================
-- TEST: Cascade delete behavior
-- ============================================================================

DO $$
DECLARE
    v_acct_count INTEGER;
BEGIN
    -- Insert a test customer with accounts
    INSERT INTO cbsa.customer (sortCode, customerNumber, customerName, customerAddress, dateOfBirth, creditScore)
    VALUES ('987654', '8888888888', 'Cascade Test', '1 Test St', '1985-01-01', 500);

    INSERT INTO cbsa.account (sortCode, accountNumber, customerNumber, accountType, opened)
    VALUES ('987654', '88888801', '8888888888', 'CURRENT', CURRENT_DATE);
    INSERT INTO cbsa.account (sortCode, accountNumber, customerNumber, accountType, opened)
    VALUES ('987654', '88888802', '8888888888', 'SAVING', CURRENT_DATE);

    -- Verify accounts exist
    SELECT count(*) INTO v_acct_count FROM cbsa.account WHERE customerNumber = '8888888888';
    ASSERT v_acct_count = 2, 'Test customer should have 2 accounts';

    -- Delete customer — accounts should cascade delete
    DELETE FROM cbsa.customer WHERE customerNumber = '8888888888';

    SELECT count(*) INTO v_acct_count FROM cbsa.account WHERE customerNumber = '8888888888';
    ASSERT v_acct_count = 0, 'Accounts should be cascade deleted when customer is deleted';

    RAISE NOTICE 'PASS: Cascade delete works correctly';
END $$;

-- ============================================================================
-- TEST: View correctness
-- ============================================================================

DO $$
DECLARE
    v_view_count BIGINT;
    v_actual_count BIGINT;
BEGIN
    SELECT accountCount INTO v_view_count FROM cbsa.accountCountByBranch WHERE sortCode = '987654';
    SELECT count(*) INTO v_actual_count FROM cbsa.account WHERE sortCode = '987654';
    ASSERT v_view_count = v_actual_count, 'accountCountByBranch view should match actual count';

    RAISE NOTICE 'PASS: Views return correct data';
END $$;

-- ============================================================================
-- TEST: Trigger fires on update
-- ============================================================================

DO $$
DECLARE
    v_created TIMESTAMP;
    v_updated TIMESTAMP;
BEGIN
    SELECT createdAt, updatedAt INTO v_created, v_updated
    FROM cbsa.customer WHERE customerNumber = '0000000001';

    -- Update a field
    UPDATE cbsa.customer SET customerName = 'Prof Beti Sheringham' WHERE customerNumber = '0000000001';

    SELECT updatedAt INTO v_updated FROM cbsa.customer WHERE customerNumber = '0000000001';
    ASSERT v_updated > v_created, 'updatedAt should be later than createdAt after update';

    RAISE NOTICE 'PASS: Update trigger fires correctly';
END $$;

-- ============================================================================
-- SUMMARY
-- ============================================================================
DO $$
BEGIN
    RAISE NOTICE '';
    RAISE NOTICE '============================================';
    RAISE NOTICE 'ALL VALIDATION TESTS PASSED';
    RAISE NOTICE '============================================';
END $$;
