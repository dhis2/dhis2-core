-- drop unused table. Some of them have been caught in the Sierra Leone Db so they might not exists in other instances
drop table if exists enrollment_messageconversation;
drop table if exists event_messageconversation;
drop table if exists programstage_programindicators;
drop table if exists programinstance_outboundsms;
drop table if exists trackedentityinstancereminder;
drop table if exists trackedentityattributegroup;
-- in Sierra Leone Db trackedentityattribute has a foreign key to trackedentitymobilesetting
do $$
declare

dyn_sql text;
stat text;

begin
dyn_sql :=
'select ''ALTER TABLE '' || tc.table_name || '' DROP CONSTRAINT '' || tc.constraint_name
from
	information_schema.table_constraints as tc
join information_schema.key_column_usage as kcu
on
	tc.constraint_name = kcu.constraint_name
	and tc.table_schema = kcu.table_schema
join information_schema.constraint_column_usage as ccu
on
	ccu.constraint_name = tc.constraint_name
	and ccu.table_schema = tc.table_schema
where
	tc.constraint_type = ''FOREIGN KEY''
	and tc.table_name = ''trackedentityattribute''
	and ccu.table_name = ''trackedentitymobilesetting''';

execute dyn_sql into stat;

if stat != '' then
execute stat;
end if;

end $$;

drop table if exists trackedentitymobilesetting;
drop table if exists programvalidation;