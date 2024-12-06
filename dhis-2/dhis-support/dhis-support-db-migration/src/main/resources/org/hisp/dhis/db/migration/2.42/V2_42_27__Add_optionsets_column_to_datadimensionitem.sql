
-- DHIS2-18370 - Visualization API: Support saving and loading "optionSet" in "items"

-- Add new json column for array of option sets.
alter table datadimensionitem add column if not exists optionsetitem jsonb;
alter table datadimensionitem add column if not exists options jsonb default '[]';
alter table datadimensionitem add column if not exists optionitem jsonb;
alter table datadimensionitem add column if not exists aggregation character varying(40);
