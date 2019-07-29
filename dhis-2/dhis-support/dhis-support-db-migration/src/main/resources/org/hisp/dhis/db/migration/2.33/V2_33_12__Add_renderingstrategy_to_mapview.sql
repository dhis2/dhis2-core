
alter table mapview add column renderingstrategy varchar(50);
update mapview set renderingstrategy = 'SINGLE' where renderingstrategy is null;
alter table mapview alter column renderingstrategy set not null;
