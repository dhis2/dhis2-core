alter table "configuration" add column if not exists "systemupdatenotificationrecipientsid" int8 null;
alter table "configuration" drop constraint if exists fk_configuration_systemupdatenotification_recipients;
alter table only "configuration"
    add constraint fk_configuration_systemupdatenotification_recipients foreign key (systemupdatenotificationrecipientsid) references usergroup(usergroupid);
