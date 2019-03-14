create sequence if not exists datavalueaudit_sequence;
select setval('datavalueaudit_sequence', max(datavalueauditid)) FROM datavalueaudit;

create sequence if not exists programstageinstance_sequence;
select setval('programstageinstance_sequence', max(programstageinstanceid)) FROM programstageinstance;

create sequence if not exists trackedentitydatavalueaudit_sequence;
select setval('trackedentitydatavalueaudit_sequence', max(trackedentitydatavalueauditid)) FROM trackedentitydatavalueaudit;

create sequence if not exists trackedentityinstance_sequence;
select setval('trackedentityinstance_sequence', max(trackedentityinstanceid)) FROM trackedentityinstance;

create sequence if not exists trackedentityinstanceaudit_sequence;
select setval('trackedentityinstanceaudit_sequence', max(trackedentityinstanceauditid)) FROM trackedentityinstanceaudit;