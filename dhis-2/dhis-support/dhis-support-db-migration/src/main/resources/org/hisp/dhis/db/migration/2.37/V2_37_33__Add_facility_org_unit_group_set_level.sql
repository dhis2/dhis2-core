
alter table "configuration" add column if not exists "facilityorgunitgroupset" int8 null;
alter table "configuration" add constraint "fk_configuration_facilityorgunitgroupset" 
	foreign key ("facilityorgunitgroupset") references orgunitgroupset(orgunitgroupsetid);

alter table "configuration" add column if not exists "facilityorgunitlevel" int8 null;
alter table "configuration" add constraint "fk_configuration_facilityorgunitlevel" 
	foreign key ("facilityorgunitlevel") references orgunitlevel(orgunitlevelid);
