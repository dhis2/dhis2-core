-- insert a new entry with empty object value
insert into keyjsonvalue (keyjsonvalueid, uid, code, created, lastupdated, namespace, namespacekey, encrypted_value, encrypted, lastupdatedby, userid, publicaccess, jbvalue)
    (select nextval('hibernate_sequence'), generate_uid(), null, now(), now(), 'sms-config', 'config', null, false, u.userinfoid, u.userinfoid, null, '{}'::jsonb from userinfo u where u.username = 'admin');
-- copy the value from settings to the new entry (if it exists)
update keyjsonvalue set jbvalue = COALESCE((select s.value from systemsetting s where s.name = 'keySmsSetting'), '{}')::jsonb where namespace = 'sms-config' and namespacekey = 'config';
-- delete the setting
delete from systemsetting where name = 'keySmsSetting';