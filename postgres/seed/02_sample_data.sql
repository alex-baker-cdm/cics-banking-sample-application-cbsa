-- =============================================================================
-- Small sample dataset for local verification
-- =============================================================================
SET search_path TO cbsa;

INSERT INTO branch (sort_code, branch_name) VALUES
    ('987654', 'CBSA Demo Branch')
ON CONFLICT DO NOTHING;

-- Two customers (numbers drawn from the customer sequence).
INSERT INTO customer (sort_code, customer_number, title, given_name, family_name,
                      address_street, address_town, address_postcode, date_of_birth, credit_score)
VALUES
    ('987654', LPAD(nextval('customer_number_seq')::text, 10, '0'),
     'Mr', 'Alan', 'Turing', '1 Church St', 'Wilmslow', 'SK9 1AA', DATE '1912-06-23', 850),
    ('987654', LPAD(nextval('customer_number_seq')::text, 10, '0'),
     'Ms', 'Ada', 'Lovelace', '12 Byron Rd', 'London', 'NW1 2DB', DATE '1815-12-10', 900);

-- Two accounts for customer #1, one for customer #2.
INSERT INTO account (sort_code, account_number, customer_id, account_type_code,
                     interest_rate, overdraft_limit, available_balance, actual_balance)
SELECT '987654', LPAD(nextval('account_number_seq')::text, 8, '0'),
       c.customer_id, 'CURRENT ', 0.00, 500.00, 1000.00, 1000.00
FROM customer c WHERE c.family_name = 'Turing';

INSERT INTO account (sort_code, account_number, customer_id, account_type_code,
                     interest_rate, overdraft_limit, available_balance, actual_balance)
SELECT '987654', LPAD(nextval('account_number_seq')::text, 8, '0'),
       c.customer_id, 'SAVING  ', 1.50, 0.00, 250.00, 250.00
FROM customer c WHERE c.family_name = 'Turing';

INSERT INTO account (sort_code, account_number, customer_id, account_type_code,
                     interest_rate, overdraft_limit, available_balance, actual_balance)
SELECT '987654', LPAD(nextval('account_number_seq')::text, 8, '0'),
       c.customer_id, 'CURRENT ', 0.00, 0.00, 3000.00, 3000.00
FROM customer c WHERE c.family_name = 'Lovelace';

-- A credit and a debit on Turing's current account.
INSERT INTO account_transaction (account_id, type_code, amount, description)
SELECT a.account_id, 'CRE', 200.00, 'Salary'
FROM account a JOIN customer c ON c.customer_id = a.customer_id
WHERE c.family_name = 'Turing' AND a.account_type_code = 'CURRENT ';

INSERT INTO account_transaction (account_id, type_code, amount, description)
SELECT a.account_id, 'DEB', 50.00, 'Groceries'
FROM account a JOIN customer c ON c.customer_id = a.customer_id
WHERE c.family_name = 'Turing' AND a.account_type_code = 'CURRENT ';
