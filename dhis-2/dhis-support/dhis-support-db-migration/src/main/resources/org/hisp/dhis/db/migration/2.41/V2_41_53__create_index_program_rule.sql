drop index if exists "program_rule_program";
drop index if exists "program_rule_variable_program";

create index program_rule_program on programrule using btree (programid);
create index program_rule_variable_program on programrulevariable using btree (programid);