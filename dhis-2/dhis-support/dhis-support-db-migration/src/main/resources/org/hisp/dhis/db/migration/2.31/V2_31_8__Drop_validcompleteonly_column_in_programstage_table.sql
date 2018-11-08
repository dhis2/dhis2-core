-- Drops validcompleteonly column from programstage table. It is not needed anymore as we have a validationstrategy column instead.
ALTER TABLE programstage DROP COLUMN IF EXISTS validcompleteonly;
