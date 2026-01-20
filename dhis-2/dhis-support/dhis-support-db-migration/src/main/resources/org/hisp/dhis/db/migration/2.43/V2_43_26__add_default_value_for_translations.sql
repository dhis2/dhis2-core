update dataelement set translations = '[]'::jsonb where translations is null;
alter table dataelement alter column translations set default '[]'::jsonb;

update categorycombo set translations = '[]'::jsonb where translations is null;
alter table categorycombo alter column translations set default '[]'::jsonb;

update attribute set translations = '[]'::jsonb where translations is null;
alter table attribute alter column translations set default '[]'::jsonb;

update categoryoptioncombo set translations = '[]'::jsonb where translations is null;
alter table categoryoptioncombo alter column translations set default '[]'::jsonb;

update categoryoptiongroupset set translations = '[]'::jsonb where translations is null;
alter table categoryoptiongroupset alter column translations set default '[]'::jsonb;

update categoryoptiongroup set translations = '[]'::jsonb where translations is null;
alter table categoryoptiongroup alter column translations set default '[]'::jsonb;

update categoryoption set translations = '[]'::jsonb where translations is null;
alter table categoryoption alter column translations set default '[]'::jsonb;

update constant set translations = '[]'::jsonb where translations is null;
alter table constant alter column translations set default '[]'::jsonb;

update dashboard set translations = '[]'::jsonb where translations is null;
alter table dashboard alter column translations set default '[]'::jsonb;

update dataapprovallevel set translations = '[]'::jsonb where translations is null;
alter table dataapprovallevel alter column translations set default '[]'::jsonb;

update dataapprovalworkflow set translations = '[]'::jsonb where translations is null;
alter table dataapprovalworkflow alter column translations set default '[]'::jsonb;

update category set translations = '[]'::jsonb where translations is null;
alter table category alter column translations set default '[]'::jsonb;

update dataelementgroupset set translations = '[]'::jsonb where translations is null;
alter table dataelementgroupset alter column translations set default '[]'::jsonb;

update dataelementgroup set translations = '[]'::jsonb where translations is null;
alter table dataelementgroup alter column translations set default '[]'::jsonb;

update dataentryform set translations = '[]'::jsonb where translations is null;
alter table dataentryform alter column translations set default '[]'::jsonb;

update dataset set translations = '[]'::jsonb where translations is null;
alter table dataset alter column translations set default '[]'::jsonb;

update document set translations = '[]'::jsonb where translations is null;
alter table document alter column translations set default '[]'::jsonb;

update eventchart set translations = '[]'::jsonb where translations is null;
alter table eventchart alter column translations set default '[]'::jsonb;

update eventreport set translations = '[]'::jsonb where translations is null;
alter table eventreport alter column translations set default '[]'::jsonb;

update indicatorgroupset set translations = '[]'::jsonb where translations is null;
alter table indicatorgroupset alter column translations set default '[]'::jsonb;

update indicator set translations = '[]'::jsonb where translations is null;
alter table indicator alter column translations set default '[]'::jsonb;

update indicatortype set translations = '[]'::jsonb where translations is null;
alter table indicatortype alter column translations set default '[]'::jsonb;

update maplegendset set translations = '[]'::jsonb where translations is null;
alter table maplegendset alter column translations set default '[]'::jsonb;

update maplegend set translations = '[]'::jsonb where translations is null;
alter table maplegend alter column translations set default '[]'::jsonb;

update map set translations = '[]'::jsonb where translations is null;
alter table map alter column translations set default '[]'::jsonb;

update mapview set translations = '[]'::jsonb where translations is null;
alter table mapview alter column translations set default '[]'::jsonb;

update optiongroupset set translations = '[]'::jsonb where translations is null;
alter table optiongroupset alter column translations set default '[]'::jsonb;

update optiongroup set translations = '[]'::jsonb where translations is null;
alter table optiongroup alter column translations set default '[]'::jsonb;

update optionset set translations = '[]'::jsonb where translations is null;
alter table optionset alter column translations set default '[]'::jsonb;

update optionvalue set translations = '[]'::jsonb where translations is null;
alter table optionvalue alter column translations set default '[]'::jsonb;

update organisationunit set translations = '[]'::jsonb where translations is null;
alter table organisationunit alter column translations set default '[]'::jsonb;

update orgunitgroupset set translations = '[]'::jsonb where translations is null;
alter table orgunitgroupset alter column translations set default '[]'::jsonb;

update orgunitgroup set translations = '[]'::jsonb where translations is null;
alter table orgunitgroup alter column translations set default '[]'::jsonb;

update orgunitlevel set translations = '[]'::jsonb where translations is null;
alter table orgunitlevel alter column translations set default '[]'::jsonb;

update predictorgroup set translations = '[]'::jsonb where translations is null;
alter table predictorgroup alter column translations set default '[]'::jsonb;

update programindicatorgroup set translations = '[]'::jsonb where translations is null;
alter table programindicatorgroup alter column translations set default '[]'::jsonb;

update programindicator set translations = '[]'::jsonb where translations is null;
alter table programindicator alter column translations set default '[]'::jsonb;

update programmessage set translations = '[]'::jsonb where translations is null;
alter table programmessage alter column translations set default '[]'::jsonb;

update programrule set translations = '[]'::jsonb where translations is null;
alter table programrule alter column translations set default '[]'::jsonb;

update programsection set translations = '[]'::jsonb where translations is null;
alter table programsection alter column translations set default '[]'::jsonb;

update programstagesection set translations = '[]'::jsonb where translations is null;
alter table programstagesection alter column translations set default '[]'::jsonb;

update programstage set translations = '[]'::jsonb where translations is null;
alter table programstage alter column translations set default '[]'::jsonb;

update program set translations = '[]'::jsonb where translations is null;
alter table program alter column translations set default '[]'::jsonb;

update relationshiptype set translations = '[]'::jsonb where translations is null;
alter table relationshiptype alter column translations set default '[]'::jsonb;

update report set translations = '[]'::jsonb where translations is null;
alter table report alter column translations set default '[]'::jsonb;

update trackedentityattribute set translations = '[]'::jsonb where translations is null;
alter table trackedentityattribute alter column translations set default '[]'::jsonb;

update trackedentitytype set translations = '[]'::jsonb where translations is null;
alter table trackedentitytype alter column translations set default '[]'::jsonb;

update usergroup set translations = '[]'::jsonb where translations is null;
alter table usergroup alter column translations set default '[]'::jsonb;

update userrole set translations = '[]'::jsonb where translations is null;
alter table userrole alter column translations set default '[]'::jsonb;

update validationrule set translations = '[]'::jsonb where translations is null;
alter table validationrule alter column translations set default '[]'::jsonb;

update indicatorgroup set translations = '[]'::jsonb where translations is null;
alter table indicatorgroup alter column translations set default '[]'::jsonb;

update validationrulegroup set translations = '[]'::jsonb where translations is null;
alter table validationrulegroup alter column translations set default '[]'::jsonb;