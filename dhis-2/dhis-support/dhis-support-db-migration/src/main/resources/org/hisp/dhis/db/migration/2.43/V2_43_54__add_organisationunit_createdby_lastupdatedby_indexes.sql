-- Indexes on organisationunit.userid (createdBy) and organisationunit.lastupdatedby.
-- These columns are checked by the existsByUser veto during user deletion.
-- Without indexes, the OR query does a sequential scan on the full table (51ms on large datasets).
-- Two separate indexes allow PostgreSQL to use a bitmap-OR plan.

CREATE INDEX IF NOT EXISTS in_organisationunit_userid
    ON organisationunit (userid);

CREATE INDEX IF NOT EXISTS in_organisationunit_lastupdatedby
    ON organisationunit (lastupdatedby);
