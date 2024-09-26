-- part 1
-- at some point in the past settings were made into JSON values stored in a text column
-- to allow storing complex values
-- now that all complex value settings were eliminated settings can be stored plain
-- convert JSON escaped as string into a plain string (that may be JSON)
update systemsetting set value = ((value)::json #>> '{}') where value ~ '^".*"$';

-- for good measure we also clear any row that is left for "empty" complex values
delete from systemsetting where value = '[]' or value = '{}';
-- and lastly a cleanup
update systemsetting set value = null where value = 'null' or value = '""';
delete from systemsetting where (value is null) and (translations is null or translations = '{}');

-- part 2
-- move translations to the datastore
--   namespace is hard coded 'settings-translations'
--   name becomes namespacekey
insert into keyjsonvalue (keyjsonvalueid, uid, created, lastupdated, namespace, namespacekey, jbvalue)
    select nextval('hibernate_sequence'), generate_uid(), now(), now(), 'settings-translations', s.name,  s.translations
    from systemsetting s
    where s.translations is not null and s.translations != '{}'
    and not exists (select 1 from keyjsonvalue where namespace = 'settings-translations' and namespacekey = s.name);
-- remove translations from systemsetting table
alter table systemsetting drop column if exists translations;