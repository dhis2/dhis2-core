/* add not null */
update optionvalue set code = uid where code is null;
alter table optionvalue alter column code set not null;

/* Only alter table if code is effectively unique for each set */
create function make_option_value_code_unique_in_set() returns void as
$$
begin
    perform 1 from optionvalue group by optionsetid, code having count(*) > 1;
    if not found then
        alter table optionvalue drop constraint if exists optionvalue_unique_optionsetid_and_code;
        alter table optionvalue add constraint optionvalue_unique_optionsetid_and_code unique (optionsetid, code);
    end if;
end;
$$ language plpgsql;

/* run the update*/
select make_option_value_code_unique_in_set();

/* clean up the update function */
drop function if exists make_option_value_code_unique_in_set();

