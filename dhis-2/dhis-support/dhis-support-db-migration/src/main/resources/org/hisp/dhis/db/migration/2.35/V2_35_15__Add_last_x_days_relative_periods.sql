
-- Adds last 30, 60, 90, 180 days columns to "relativeperiods" table

alter table "relativeperiods" add column if not exists "last30days" boolean;
update "relativeperiods" set "last30days" = false where "last30days" is null;
alter table "relativeperiods" alter column "last30days" set not null;

alter table "relativeperiods" add column if not exists "last60days" boolean;
update "relativeperiods" set "last60days" = false where "last60days" is null;
alter table "relativeperiods" alter column "last60days" set not null;

alter table "relativeperiods" add column if not exists "last90days" boolean;
update "relativeperiods" set "last90days" = false where "last90days" is null;
alter table "relativeperiods" alter column "last90days" set not null;

alter table "relativeperiods" add column if not exists "last180days" boolean;
update "relativeperiods" set "last180days" = false where "last180days" is null;
alter table "relativeperiods" alter column "last180days" set not null;
