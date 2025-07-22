-- Migration script to add the field trigram indexable in the trackedentityattribute table
alter table trackedentityattribute
    add column if not exists trigramindexable bool not null default false;
