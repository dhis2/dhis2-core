/* add not null */
update optionvalue set code = uid where code is null;
alter table optionvalue alter column code set not null;

alter table optionvalue drop constraint if exists optionvalue_unique_optionsetid_and_code;
alter table optionvalue add constraint optionvalue_unique_optionsetid_and_code unique (optionsetid, code);