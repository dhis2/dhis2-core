-- Add the column to skip individual analytics column with a default value of false
alter table trackedentityattribute
    add column if not exists skipIndividualAnalytics bool not null default false;