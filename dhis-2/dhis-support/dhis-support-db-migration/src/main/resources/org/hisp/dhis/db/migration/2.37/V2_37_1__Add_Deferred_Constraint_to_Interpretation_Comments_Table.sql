-- Checking unique constraint only in the end of the transaction.
-- DHIS2-8256: Deleting interpretation comments does not always work

ALTER TABLE interpretation_comments DROP CONSTRAINT interpretation_comments_interpretationcommentid_key;

ALTER TABLE public.interpretation_comments ADD CONSTRAINT interpretation_comments_interpretationcommentid_key UNIQUE (interpretationcommentid) DEFERRABLE INITIALLY DEFERRED;
