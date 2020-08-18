-- Add column 'openperiodsaftercoenddate' to 'dataset'

ALTER TABLE dataset
  ADD COLUMN IF NOT EXISTS openperiodsaftercoenddate integer default 0;
