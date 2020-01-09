
-- Add "delay" column

alter table jobconfiguration add column "delay" integer;

-- Remove not null constraint on "cronexpression" column

alter table jobconfiguration alter column "cronexpression" drop not null;
