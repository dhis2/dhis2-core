-- For legacy predictors, set includedescendantorgunits to true
ALTER TABLE predictor ADD COLUMN IF NOT EXISTS includedescendantorgunits BOOLEAN DEFAULT true;

-- For future predictors, includedescendantorgunits defaults to false
ALTER TABLE predictor ALTER COLUMN includedescendantorgunits DROP DEFAULT;
