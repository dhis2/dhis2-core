-- Interim workaround for a defect in the platform-perf DB dump.
--
-- DHIS2 assigns primary keys for most tables (userinfo, organisationunit, usergroup, ...) from a
-- single shared sequence, `hibernate_sequence`. The platform-perf dump was bulk-seeded with ~250k
-- users and ~250k org units at high ids, but ships `hibernate_sequence` set to ~965. As a result
-- every insert reuses an id that already exists and fails with e.g.
--   ERROR: duplicate key value violates unique constraint "userinfo_pkey"
--   Detail: Key (userinfoid)=(973) already exists.
-- which makes every write operation (user create/replicate/delete) return HTTP 409. The users
-- performance test is write-heavy, so it cannot run against the dump until the sequence is advanced
-- past the seeded ids.
--
-- This advances `hibernate_sequence` to a value comfortably above any seeded id. It is FORWARD-ONLY
-- (GREATEST with the current value), so it is a harmless no-op on correctly generated dumps
-- (sierra-leone, hmis) and never moves a sequence backwards (which would reintroduce collisions).
--
-- This is a stopgap applied at DB-image build time. The proper fix is to regenerate the
-- platform-perf dump with the sequence set correctly; remove this script once that lands.
SELECT setval(
    'hibernate_sequence',
    GREATEST(100000000::bigint, (SELECT last_value FROM hibernate_sequence)),
    true);
