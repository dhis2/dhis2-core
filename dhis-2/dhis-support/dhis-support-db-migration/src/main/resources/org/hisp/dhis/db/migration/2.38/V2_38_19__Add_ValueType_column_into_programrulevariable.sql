-- This script relates to the issue https://jira.dhis2.org/browse/TECH-820
-- Adds a new column "ValueType" into ProgramRuleVariable table


alter table programrulevariable add column if not exists "valuetype" character varying(50);
UPDATE programrulevariable prv SET valuetype = de.valuetype FROM dataelement de WHERE prv.dataelementid = de.dataelementid;
UPDATE programrulevariable prv SET valuetype = attr.valuetype FROM trackedentityattribute attr WHERE prv.trackedentityattributeid = attr.trackedentityattributeid;
UPDATE programrulevariable prv SET valuetype = 'TEXT' WHERE sourcetype = 'CALCULATED_VALUE';
UPDATE programrulevariable prv SET valuetype = 'TEXT' WHERE valuetype IS NULL;
alter table programrulevariable alter column valuetype set not null;

