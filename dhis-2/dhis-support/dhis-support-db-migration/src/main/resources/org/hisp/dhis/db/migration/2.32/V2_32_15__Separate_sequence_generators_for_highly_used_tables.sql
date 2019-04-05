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

create sequence if not exists programinstance_sequence;
select setval('programinstance_sequence', max(programinstanceid)) FROM programinstance;

create sequence if not exists deletedobject_sequence;
select setval('deletedobject_sequence', max(deletedobjectid)) FROM deletedobject;

create sequence if not exists reservedvalue_sequence;
select setval('reservedvalue_sequence', max(reservedvalueid)) FROM reservedvalue;

create sequence if not exists usermessage_sequence;
select setval('usermessage_sequence', max(usermessageid)) FROM usermessage;

create sequence if not exists messageconversation_sequence;
select setval('messageconversation_sequence', max(messageconversationid)) FROM messageconversation;

create sequence if not exists message_sequence;
select setval('message_sequence', max(messageid)) FROM message;