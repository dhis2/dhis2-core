-- This script is responsible for adding new jsonb columns into dashboard table.
-- See Feature DHIS2-11607.

-- Add new JSONB columns("layout" and "itemconfig") into dashboard table;
alter table dashboard add column if not exists "layout" jsonb;
alter table dashboard add column if not exists "itemconfig" jsonb;
