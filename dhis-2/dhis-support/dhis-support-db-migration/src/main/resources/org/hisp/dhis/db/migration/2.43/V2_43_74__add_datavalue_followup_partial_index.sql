-- Partial index on the data values flagged for follow-up. Follow-up analysis
-- (FollowupValueManager, JdbcDataAnalysisStore) filters on followup = true, which
-- matches very few rows, but with no index on it a wide request reads the whole
-- datavalue table. Only flagged rows are indexed, so the index stays a few pages.
-- IF NOT EXISTS lets large installations create it beforehand with
-- CREATE INDEX CONCURRENTLY under the same name. DHIS2-22310.
CREATE INDEX IF NOT EXISTS "in_datavalue_followup" ON "datavalue" ("dataelementid") WHERE "followup" = true;
