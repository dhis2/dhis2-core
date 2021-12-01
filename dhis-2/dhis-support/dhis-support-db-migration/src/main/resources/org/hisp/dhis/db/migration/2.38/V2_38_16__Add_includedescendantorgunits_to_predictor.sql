-- For legacy predictors, include descendants for backwards compatibility
ALTER TABLE predictor ADD COLUMN IF NOT EXISTS organisationUnitDescendants VARCHAR(100) DEFAULT 'DESCENDANTS';

-- For future predictors, selected only is a better default
ALTER TABLE predictor ALTER COLUMN organisationUnitDescendants SET DEFAULT 'SELECTED';
