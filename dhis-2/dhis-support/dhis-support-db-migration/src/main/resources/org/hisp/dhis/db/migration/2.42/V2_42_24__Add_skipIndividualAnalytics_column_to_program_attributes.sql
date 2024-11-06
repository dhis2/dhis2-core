
-- Step 1: Add the new column with a default value of false
ALTER TABLE program_attributes
    ADD COLUMN IF NOT EXISTS skipIndividualAnalytics boolean DEFAULT FALSE;

-- Step 2: Update existing rows to ensure they all have the value false
UPDATE program_attributes
SET skipIndividualAnalytics = FALSE;
