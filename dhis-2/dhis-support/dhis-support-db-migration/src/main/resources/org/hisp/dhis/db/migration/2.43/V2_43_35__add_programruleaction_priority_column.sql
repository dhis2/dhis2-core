-- Migration related to DHIS2-20498.
-- Add priority column to programruleaction
ALTER TABLE programruleaction ADD COLUMN IF NOT EXISTS priority INTEGER;
