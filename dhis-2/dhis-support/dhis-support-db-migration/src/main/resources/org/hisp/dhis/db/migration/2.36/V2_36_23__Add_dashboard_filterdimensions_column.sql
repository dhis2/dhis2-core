-- Add columns filterdimensionsarerestricted and allowedfilterdimensions
-- to the dashboard table. See Feature DHIS2-7620.

alter table if exists dashboard add column if not exists filterdimensionsarerestricted boolean;
alter table if exists dashboard add column if not exists allowedfilterdimensions jsonb;
