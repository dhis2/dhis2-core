update section set translations = '[]'::jsonb where translations is null;
alter table section alter column translations set default '[]'::jsonb;
alter table section alter column translations set not null;

-- All of those tables already have default value set in V2_43_26 migration
-- We are just adding not null constraint in this migration
alter table dataelement alter column translations set not null;
alter table categorycombo alter column translations set not null;
alter table attribute alter column translations set not null;
alter table categoryoptioncombo alter column translations set not null;
alter table categoryoptiongroupset alter column translations set not null;
alter table categoryoptiongroup alter column translations set not null;
alter table categoryoption alter column translations set not null;
alter table constant alter column translations set not null;
alter table dashboard alter column translations set not null;
alter table dataapprovallevel alter column translations set not null;
alter table dataapprovalworkflow alter column translations set not null;
alter table category alter column translations set not null;
alter table dataelementgroupset alter column translations set not null;
alter table dataelementgroup alter column translations set not null;
alter table dataentryform alter column translations set not null;
alter table dataset alter column translations set not null;
alter table document alter column translations set not null;
alter table eventchart alter column translations set not null;
alter table eventreport alter column translations set not null;
alter table indicatorgroupset alter column translations set not null;
alter table indicator alter column translations set not null;
alter table indicatortype alter column translations set not null;
alter table maplegendset alter column translations set not null;
alter table maplegend alter column translations set not null;
alter table map alter column translations set not null;
alter table mapview alter column translations set not null;
alter table optiongroupset alter column translations set not null;
alter table optiongroup alter column translations set not null;
alter table optionset alter column translations set not null;
alter table optionvalue alter column translations set not null;
alter table organisationunit alter column translations set not null;
alter table orgunitgroupset alter column translations set not null;

