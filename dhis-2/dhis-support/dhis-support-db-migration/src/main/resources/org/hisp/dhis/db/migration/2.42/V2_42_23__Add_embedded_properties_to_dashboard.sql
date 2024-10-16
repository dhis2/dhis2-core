
-- Add various embedded dashboard property columns to dashboard table

alter table "dashboard" add column if not exists "embeddedprovider" varchar(50);
alter table "dashboard" add column if not exists "embeddedid" text;
alter table "dashboard" add column if not exists "embeddedoptions" jsonb;
