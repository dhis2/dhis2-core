-- Implement configurable plural labels for Program and Program Stage

alter table program add column if not exists "enrollmentslabel" text;