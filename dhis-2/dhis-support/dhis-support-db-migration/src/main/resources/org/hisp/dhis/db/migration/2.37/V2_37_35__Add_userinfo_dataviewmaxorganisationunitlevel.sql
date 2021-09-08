
alter table "userinfo" add column "datavieworgunitlevelid" int8 null;
alter table "userinfo" add constraint "fk_userinfo_datavieworgunitlevelid" foreign key ("orgunitlevelid") references orgunitlevel("orgunitlevelid");
