
-- Add a new column "eventstatus" to the mapview table.

alter table mapview add column if not exists eventstatus VARCHAR(50);
