alter table program_attribute_group alter column shortname set not null;
alter table program_attribute_group drop constraint if exists program_attribute_group_unique_shortname;
alter table program_attribute_group add constraint program_attribute_group_unique_shortname unique (shortname);

alter table dataset alter column shortname set not null;
alter table dataset drop constraint if exists dataset_unique_shortname;
alter table dataset add constraint dataset_unique_shortname unique (shortname);

alter table orgunitgroup alter column shortname set not null;
alter table orgunitgroup drop constraint if exists orgunitgroup_unique_shortname;
alter table orgunitgroup add constraint orgunitgroup_unique_shortname unique (shortname);

alter table dataelementcategoryoption alter column shortname set not null;
alter table dataelementcategoryoption drop constraint if exists dataelementcategoryoption_unique_shortname;
alter table dataelementcategoryoption add constraint dataelementcategoryoption_unique_shortname unique (shortname);

alter table constant alter column shortname set not null;
alter table constant drop constraint if exists constant_unique_shortname;
alter table constant add constraint constant_unique_shortname unique (shortname);

alter table dataelementgroup alter column shortname set not null;
alter table dataelementgroup drop constraint if exists dataelementgroup_unique_shortname;
alter table dataelementgroup add constraint dataelementgroup_unique_shortname unique (shortname);
