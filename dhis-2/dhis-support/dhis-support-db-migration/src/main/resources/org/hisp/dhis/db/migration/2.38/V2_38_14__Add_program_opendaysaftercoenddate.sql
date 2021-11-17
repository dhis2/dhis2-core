-- Add column 'opendaysaftercoenddate' to 'program'

ALTER TABLE program
  ADD COLUMN IF NOT EXISTS opendaysaftercoenddate integer default 0;
