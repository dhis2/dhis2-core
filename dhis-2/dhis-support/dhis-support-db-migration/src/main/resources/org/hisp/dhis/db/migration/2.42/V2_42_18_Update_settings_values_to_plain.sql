-- at some point in the past settings were made into JSON values stored in a text column
-- to allow storing complex values
-- now that all complex value settings were eliminated settings can be stored plain
-- in essence this quotes around strings are stripped
update systemsetting set value = substring(value, 2, length(value) - 2) where value ~ '^".*"$';
-- for good measure we also clear any row that is left for "empty" complex values
delete from systemsetting where value = '[]' or value = '{}';
-- and lastly a cleanup
update systemsetting set value = null where value = 'null' or value = '""';
delete from systemsetting where (value is null) and (translations is null or translations = '{}');