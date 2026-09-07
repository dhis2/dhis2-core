-- Partial index to support DataValueTrimJob efficiently.
-- Only indexes rows where value is null or empty and not yet deleted,
-- which are the candidates for soft-deletion by the trim job.
-- Expected to be tiny in a healthy system (near-zero rows match).
create index if not exists idx_datavalue_null_empty_trim
    on datavalue (dataelementid)
    where deleted = false and (value is null or value = '');
