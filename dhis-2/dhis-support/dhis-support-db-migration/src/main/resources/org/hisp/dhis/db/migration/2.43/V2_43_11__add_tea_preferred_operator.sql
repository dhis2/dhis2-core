-- Migration script to add the field preferred operator in the trackedentityattribute table
alter table trackedentityattribute
    add column preferredsearchoperator varchar(20) default null;