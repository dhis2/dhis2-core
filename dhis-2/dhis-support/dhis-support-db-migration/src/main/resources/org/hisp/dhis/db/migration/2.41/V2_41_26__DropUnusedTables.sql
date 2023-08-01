-- drop unused table. Some of them have been caught in the Sierra Leone Db so they might not exists in other instances
drop table if exists enrollment_messageconversation;
drop table if exists event_messageconversation;
drop table if exists programstage_programindicators;
drop table if exists programinstance_outboundsms;
drop table if exists trackedentityinstancereminder;
drop table if exists trackedentityattributegroup;
drop table if exists trackedentitymobilesetting;
drop table if exists programvalidation;