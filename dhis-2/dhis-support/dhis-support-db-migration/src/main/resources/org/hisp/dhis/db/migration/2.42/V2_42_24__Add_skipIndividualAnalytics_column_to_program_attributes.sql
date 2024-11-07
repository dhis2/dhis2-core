
-- Add the new column with a default value of false
ALTER TABLE program_attributes
    ADD COLUMN IF NOT EXISTS skipIndividualAnalytics boolean DEFAULT FALSE;