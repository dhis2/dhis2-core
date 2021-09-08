alter table indicatorgroupset
    add column if not exists shortname character varying(50);
alter table categoryoptiongroupset
    add column if not exists shortname character varying(50);
