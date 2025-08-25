-- Migration script to add the field preferred operator in the trackedentityattribute table
alter table trackedentityattribute
    add column if not exists preferredsearchoperator varchar(20) default null;