alter table dataelementcategory
    add column if not exists shortname character varying(50);
alter table dataelementgroupset
    add column if not exists shortname character varying(50);
alter table orgunitgroupset
    add column if not exists shortname character varying(50);
