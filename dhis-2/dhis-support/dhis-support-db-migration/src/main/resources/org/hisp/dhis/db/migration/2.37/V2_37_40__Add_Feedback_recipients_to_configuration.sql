alter table "configuration" add column if not exists "systemupdatenotificationrecipientsid" int8 null;
alter table only "configuration"
    add constraint fk_configuration_systemupdatenotification_recipients foreign key (systemupdatenotificationrecipientsid) references usergroup(usergroupid);