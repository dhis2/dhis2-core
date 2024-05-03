-- https://dhis2.atlassian.net/browse/DHIS2-16236
-- Implement configurable labels for Program and Program Stage

alter table program add column if not exists "enrollmentlabel" text;
alter table program add column if not exists "followuplabel" text;
alter table program add column if not exists "orgunitlabel" text;
alter table program add column if not exists "relationshiplabel" text;
alter table program add column if not exists "notelabel" text;
alter table program add column if not exists "trackedentityattributelabel" text;
alter table programstage add column if not exists "programstagelabel" text;
alter table programstage add column if not exists "eventlabel" text;
