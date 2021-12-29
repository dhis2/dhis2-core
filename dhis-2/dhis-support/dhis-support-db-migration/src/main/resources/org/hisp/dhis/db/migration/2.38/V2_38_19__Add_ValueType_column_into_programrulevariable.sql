-- This script relates to the issue https://jira.dhis2.org/browse/TECH-820
-- Adds a new column "ValueType" into ProgramRuleVariable table


alter table programrulevariable add column if not exists "valuetype" character varying(50);
update table programrulevariable set valuetype = 'TEXT' where valuetype is null;

