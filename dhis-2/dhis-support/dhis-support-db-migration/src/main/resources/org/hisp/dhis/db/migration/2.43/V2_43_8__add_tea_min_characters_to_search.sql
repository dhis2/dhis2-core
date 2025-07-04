-- Migration script to add the field min characters to search in the trackedentityattribute table
alter table trackedentityattribute
    add column mincharacterstosearch integer not null default 0;