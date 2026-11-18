-- Validation result ids came from the shared hibernate_sequence. Persisting validation results
-- upserts with INSERT ... ON CONFLICT, which takes an id for every attempted row, so each
-- violation found again used up a hibernate_sequence value. Give the table its own sequence as
-- column default, starting after any id already taken.
create sequence if not exists validationresult_sequence;

select setval('validationresult_sequence', greatest(
    (select last_value from validationresult_sequence),
    coalesce((select max(validationresultid) from validationresult), 1)));

alter sequence validationresult_sequence owned by validationresult.validationresultid;

alter table validationresult
    alter column validationresultid set default nextval('validationresult_sequence');
