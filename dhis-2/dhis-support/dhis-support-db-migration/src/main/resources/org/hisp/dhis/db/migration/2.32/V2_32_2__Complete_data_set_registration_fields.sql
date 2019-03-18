
alter table completedatasetregistration
add column if not exists lastupdatedby character varying(255);

alter table completedatasetregistration
add column if not exists lastupdated timestamp;

alter table completedatasetregistration
add column if not exists completed boolean;

update completedatasetregistration set completed = true where completed is null;

alter table completedatasetregistration alter column completed set not null;
