-- Migration related to https://dhis2.atlassian.net/browse/DHIS2-20950
-- The keyStyle setting was only used in mobile and is superseded by the new keyCustomColorMobile setting
insert into systemsetting(systemsettingid, name, value)
select nextval('hibernate_sequence'), 'keyCustomColorMobile', mappings.new_value
from (values
    ('light_blue/light_blue.css', '#007DEB'),
    ('green/green.css', '#218C51'),
    ('india/india.css', '#EA5911'),
    ('myanmar/myanmar.css', '#8C2121')) as mappings(old_value, new_value)
inner join systemsetting s
on s.value = mappings.old_value
where s.name = 'keyStyle';

delete from systemsetting where name = 'keyStyle';
