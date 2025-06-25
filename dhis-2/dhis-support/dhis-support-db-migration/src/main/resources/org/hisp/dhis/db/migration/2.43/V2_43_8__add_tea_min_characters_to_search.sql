-- Migration script to add the field min characters to search in the trackedentityattribute table
ALTER TABLE trackedentityattribute
    ADD COLUMN mincharacterstosearch INTEGER NOT NULL DEFAULT 0;