-- Migration script to add the field min characters to search in the trackedentityattribute table
ALTER TABLE trackedentityattribute
  ADD COLUMN IF NOT EXISTS mincharacterstosearch integer NOT NULL DEFAULT 0;
