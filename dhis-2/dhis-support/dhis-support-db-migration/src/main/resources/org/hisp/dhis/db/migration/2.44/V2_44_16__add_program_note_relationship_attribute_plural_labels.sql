-- https://dhis2.atlassian.net/browse/DHIS2-21738
-- Add plural labels for Note, Relationship and Tracked Entity Attribute to Program

alter table program add column if not exists "noteslabel" text;
alter table program add column if not exists "relationshipslabel" text;
alter table program add column if not exists "trackedentityattributeslabel" text;
