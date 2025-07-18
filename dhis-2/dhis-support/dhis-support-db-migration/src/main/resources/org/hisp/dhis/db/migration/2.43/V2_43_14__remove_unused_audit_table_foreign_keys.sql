alter table if exists trackedentityattributevalueaudit
    drop constraint if exists fk_attributevalueaudit_trackedentityinstanceid;
alter table if exists trackedentitydatavalueaudit
    drop constraint if exists fk_entityinstancedatavalueaudit_programstageinstanceid;
