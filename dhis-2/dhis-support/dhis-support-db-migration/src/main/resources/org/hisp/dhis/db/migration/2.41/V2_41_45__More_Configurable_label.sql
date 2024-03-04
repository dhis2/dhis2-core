-- https://dhis2.atlassian.net/browse/DHIS2-16236
-- Implement configurable labels for a Program when there is no need to configure or relate to a program stage

alter table program add column if not exists "programstagelabel" text;
alter table program add column if not exists "eventlabel" text;
