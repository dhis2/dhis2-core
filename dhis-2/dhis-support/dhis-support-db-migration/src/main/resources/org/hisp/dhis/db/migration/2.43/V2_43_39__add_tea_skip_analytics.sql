-- Add the column to skip analytics column with a default value of false
alter table trackedentityattribute
    add column if not exists skipAnalytics bool not null default false;