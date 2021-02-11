alter table dataelementcategory alter column shortname set not null;
alter table dataelementcategory drop constraint if exists dataelementcategory_unique_shortname;
alter table dataelementcategory add constraint dataelementcategory_unique_shortname unique (shortname);

alter table dataelementgroupset alter column shortname set not null;
alter table dataelementgroupset drop constraint if exists dataelementgroupset_unique_shortname;
alter table dataelementgroupset add constraint dataelementgroupset_unique_shortname unique (shortname);

alter table orgunitgroupset alter column shortname set not null;
alter table orgunitgroupset drop constraint if exists orgunitgroupset_unique_shortname;
alter table orgunitgroupset add constraint orgunitgroupset_unique_shortname unique (shortname);
