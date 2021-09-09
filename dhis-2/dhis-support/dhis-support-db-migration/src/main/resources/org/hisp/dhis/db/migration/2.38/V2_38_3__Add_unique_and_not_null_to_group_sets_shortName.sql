alter table indicatorgroupset alter column shortname set not null;
alter table indicatorgroupset drop constraint if exists indicatorgroupset_unique_shortname;
alter table indicatorgroupset add constraint indicatorgroupset_unique_shortname unique (shortname);

alter table categoryoptiongroupset alter column shortname set not null;
alter table categoryoptiongroupset drop constraint if exists categoryoptiongroupset_unique_shortname;
alter table categoryoptiongroupset add constraint categoryoptiongroupset_unique_shortname unique (shortname);
