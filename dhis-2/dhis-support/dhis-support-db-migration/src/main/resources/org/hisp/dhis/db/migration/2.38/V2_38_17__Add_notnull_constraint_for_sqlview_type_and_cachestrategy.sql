update sqlview set type='VIEW' where type is null;
update sqlview set cachestrategy='RESPECT_SYSTEM_SETTING' where cachestrategy is null;

alter table sqlview alter column type set not null;
alter table sqlview alter column cachestrategy set not null;