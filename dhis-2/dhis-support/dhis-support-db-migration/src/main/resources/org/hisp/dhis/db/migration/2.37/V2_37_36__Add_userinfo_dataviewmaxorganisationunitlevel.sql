
alter table "userinfo" add column "dataviewmaxorgunitlevelid" int8 null;
alter table "userinfo" add constraint "fk_userinfo_dataviewmaxorgunitlevelid_orgunitlevelid" 
	foreign key ("dataviewmaxorgunitlevelid") references orgunitlevel("orgunitlevelid");
