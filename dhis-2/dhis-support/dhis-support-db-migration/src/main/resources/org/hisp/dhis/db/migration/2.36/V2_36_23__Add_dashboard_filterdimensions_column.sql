-- Add columns restrictfilters and allowedfilters
-- to the dashboard table. See Feature DHIS2-7620.

alter table dashboard add column if not exists restrictfilters boolean;
update dashboard set restrictfilters = false where restrictfilters is null;
alter table dashboard alter column restrictfilters set not null;

alter table dashboard add column if not exists allowedfilters jsonb;
