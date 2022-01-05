update sqlview set type='VIEW' where type is null;
update sqlview set cachestrategy='RESPECT_SYSTEM_SETTING' where cachestrategy is null;

alter table sqlview alter COLUMN type set not NULL;
alter table sqlview alter COLUMN cachestrategy set not NULL;
