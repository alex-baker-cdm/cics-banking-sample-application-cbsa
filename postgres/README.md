# CBSA → PostgreSQL migration

A normalized (3NF) PostgreSQL target schema for the CICS Banking Sample
Application, migrating off Db2 (`ACCOUNT`, `PROCTRAN`, `CONTROL`) and the VSAM
`CUSTOMER` file.

```
postgres/
├── schema/
│   ├── 01_schema.sql              -- tables, PKs, FKs, CHECKs, indexes
│   └── 02_sequences_and_views.sql -- CONTROL replacement (sequences + views)
├── seed/
│   ├── 01_lookups.sql             -- account_type + transaction_type reference data
│   └── 02_sample_data.sql         -- small demo dataset
├── test/
│   └── 01_tests.sql               -- constraint / behaviour assertions
├── docs/
│   ├── MIGRATION.md               -- normalization walkthrough + column mapping
│   ├── ERD.md                     -- entity-relationship diagram
│   ├── img/                       -- rendered diagrams (+ mermaid sources)
│   └── {ACCOUNT,PROCTRAN,CONTROL}_analysis.md  -- per-table access-pattern analyses
└── run_local.sh                   -- build + seed + test in a throwaway Docker Postgres
```

## Quick start

```bash
./postgres/run_local.sh
```

Requires Docker. Spins up `postgres:16`, applies the schema, seeds data, and runs
the test suite. Expected tail:

```
NOTICE:  === ALL SCHEMA TESTS PASSED ===
```

## Applying to an existing database

```bash
psql "$DATABASE_URL" -f postgres/schema/01_schema.sql
psql "$DATABASE_URL" -f postgres/schema/02_sequences_and_views.sql
psql "$DATABASE_URL" -f postgres/seed/01_lookups.sql
```

Objects are created in a dedicated `cbsa` schema.

See [`docs/MIGRATION.md`](./docs/MIGRATION.md) for the full rationale.
