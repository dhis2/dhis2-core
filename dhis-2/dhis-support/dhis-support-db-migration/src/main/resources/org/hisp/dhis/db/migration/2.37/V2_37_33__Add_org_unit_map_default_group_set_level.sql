
alter table "configuration" add column if not exists "orgunitmapdefaultgroupset" int8 null;
alter table "configuration" add constraint "fk_configuration_orgunitmapdefaultgroupset" 
	foreign key ("orgunitmapdefaultgroupset") references orgunitgroupset(orgunitgroupsetid);

alter table "configuration" add column if not exists "orgunitmapdefaultlevel" int8 null;
alter table "configuration" add constraint "fk_configuration_orgunitmapdefaultlevel" 
	foreign key ("orgunitmapdefaultlevel") references orgunitlevel(orgunitlevelid);
