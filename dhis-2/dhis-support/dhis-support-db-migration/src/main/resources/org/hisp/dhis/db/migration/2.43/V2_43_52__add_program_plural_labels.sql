-- Implement configurable plural labels for Program and Program Stage

alter table program add column if not exists "enrollmentslabel" text;
alter table program add column if not exists "programstageslabel" text;
alter table program add column if not exists "eventslabel" text;
alter table programstage add column if not exists "eventslabel" text;
alter table trackedentitytype add column if not exists "trackedentitytypeslabel" text;