alter table potentialduplicate drop constraint if exists potentialduplicate_lastupdatedby_user;
alter table potentialduplicate add column if not exists createdbyusername varchar(255);
alter table potentialduplicate add column if not exists lastupdatebyusername varchar(255);
update potentialduplicate set createdbyusername = u.username from potentialduplicate pd inner join users u on u.userid = pd.lastupdatedby where u.userid = potentialduplicate.lastupdatedby ;
update potentialduplicate set lastupdatebyusername = u.username from potentialduplicate pd inner join users u on u.userid = pd.lastupdatedby where u.userid = potentialduplicate.lastupdatedby ;
alter table potentialduplicate alter column createdbyusername set not null;
alter table potentialduplicate alter column lastupdatebyusername set not null;
alter table potentialduplicate drop column if exists lastupdatedby;