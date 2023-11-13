alter table customicon add column if not exists "created" timestamp;
alter table customicon add column if not exists "lastupdated" timestamp;
alter table customicon add column if not exists "custom" BOOLEAN;