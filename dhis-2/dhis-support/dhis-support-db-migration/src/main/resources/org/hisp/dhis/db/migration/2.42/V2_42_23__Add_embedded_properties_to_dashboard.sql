
-- Add various embedded dashboard property columns to dashboard table

alter table "dashboard" add column if not exists "embeddedprovider" varchar(50);
update "dashboard" set "embeddedprovider" = 'NATIVE' where "embeddedprovider" is null;
alter table "dashboard" alter column "embeddedprovider" set not null;

alter table "dashboard" add column if not exists "embeddedid" text;

alter table "dashboard" add column if not exists "embeddedoptions" jsonb;
