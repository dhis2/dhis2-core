-- keyConfig is no longer a setting
delete from systemsetting s where s.name = 'keyConfig';