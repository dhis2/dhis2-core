-- We are resetting the state of this new column.
-- This column was introduced in 2.36, but as we had changes in the JSONB object, we need to reset it.
UPDATE visualization SET outlieranalysis = NULL;
