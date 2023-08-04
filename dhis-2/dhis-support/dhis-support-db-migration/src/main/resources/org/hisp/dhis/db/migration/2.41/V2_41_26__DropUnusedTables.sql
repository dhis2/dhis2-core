-- drop unused table. Only reference in the metadata existed with an sql foreign key to enrollment or event
drop table if exists enrollment_messageconversation;
drop table if exists event_messageconversation;