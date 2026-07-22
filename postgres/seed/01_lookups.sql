-- =============================================================================
-- Reference / lookup data
-- =============================================================================
SET search_path TO cbsa;

-- Account product types (CBSA used free-text ACCOUNT_TYPE CHAR(8)).
INSERT INTO account_type (account_type_code, description) VALUES
    ('CURRENT ', 'Current account'),
    ('SAVING  ', 'Savings account'),
    ('LOAN    ', 'Loan account'),
    ('MORTGAGE', 'Mortgage account'),
    ('ISA     ', 'Individual Savings Account')
ON CONFLICT (account_type_code) DO NOTHING;

-- PROCTRAN_TYPE 3-char codes (src/base/cobol_copy/PROCTRAN.cpy 88-levels).
INSERT INTO transaction_type (type_code, description, category) VALUES
    ('CRE', 'Credit',                         'FINANCIAL'),
    ('DEB', 'Debit',                          'FINANCIAL'),
    ('PCR', 'Payment credit',                 'FINANCIAL'),
    ('PDR', 'Payment debit',                  'FINANCIAL'),
    ('TFR', 'Transfer between accounts',      'FINANCIAL'),
    ('CHA', 'Cheque acknowledged',            'FINANCIAL'),
    ('CHF', 'Cheque failure',                 'FINANCIAL'),
    ('CHI', 'Cheque paid in',                 'FINANCIAL'),
    ('CHO', 'Cheque paid out',                'FINANCIAL'),
    ('OCA', 'Branch: create account',         'LIFECYCLE'),
    ('OCC', 'Branch: create customer',        'LIFECYCLE'),
    ('ODA', 'Branch: delete account',         'LIFECYCLE'),
    ('ODC', 'Branch: delete customer',        'LIFECYCLE'),
    ('OCS', 'Create set-overdraft (SODD)',    'LIFECYCLE'),
    ('ICA', 'Web: create account',            'LIFECYCLE'),
    ('ICC', 'Web: create customer',           'LIFECYCLE'),
    ('IDA', 'Web: delete account',            'LIFECYCLE'),
    ('IDC', 'Web: delete customer',           'LIFECYCLE')
ON CONFLICT (type_code) DO NOTHING;
