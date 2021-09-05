
alter table "programstagedataelement" add column if not exists "skipanalytics" boolean;
update "programstagedataelement" set "skipanalytics" = false where "skipanalytics" is null;
alter table "programstagedataelement" alter column "skipanalytics" set not null;

