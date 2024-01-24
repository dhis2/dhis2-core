-- https://dhis2.atlassian.net/browse/DHIS2-16236
-- Implement configurable labels for Program and Program Stage

alter table program add column if not exists "enrollmentLabel" text;
alter table program add column if not exists "followUpLabel" text;
alter table program add column if not exists "orgUnitLabel" text;
alter table program add column if not exists "relationshipLabel" text;
alter table program add column if not exists "noteLabel" text;
alter table program add column if not exists "trackedEntityAttributeLabel" text;
alter table programstage add column if not exists "programStageLabel" text;
alter table programstage add column if not exists "eventLabel" text;
