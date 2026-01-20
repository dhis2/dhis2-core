-- Adds a description field to the usergroup table
alter table usergroup add column if not exists description varchar(255);
