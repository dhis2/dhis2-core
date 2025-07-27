-- Migration script to add defaults to the data value table
-- to allow inserts with less values provided explicitly

-- add defaults to columns that have the same value for all rows affected by an import
ALTER TABLE datavalue
    ALTER COLUMN created SET DEFAULT now(),
    ALTER COLUMN lastupdated SET DEFAULT now(),
    ALTER COLUMN storedby SET DEFAULT current_setting('dhis2.user')::text;

