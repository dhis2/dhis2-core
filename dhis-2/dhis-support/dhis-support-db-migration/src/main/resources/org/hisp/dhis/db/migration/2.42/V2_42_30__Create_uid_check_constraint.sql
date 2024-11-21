CREATE DOMAIN uid_type AS VARCHAR(11)
    CHECK (
        VALUE ~ '^[A-Za-z]' AND    -- First character must be a letter
        VALUE ~ '^[A-Za-z0-9]{11}$' -- All 11 characters must be alphanumeric
        );

create or replace function check_uid(uid varchar) returns boolean
    language plpgsql
as
$$
begin
     return uid ~ '^[A-Za-z]' and uid ~ '^[A-Za-z0-9]{11}$';
end;
$$;

alter table potentialduplicate add constraint potentialduplicate_check_uid CHECK (check_uid(uid));
alter table potentialduplicate add constraint potentialduplicate_check_original CHECK (check_uid(original));
alter table potentialduplicate add constraint potentialduplicate_check_duplicate CHECK (check_uid(duplicate));