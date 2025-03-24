drop index if exists "program_rule_program";
drop index if exists "program_rule_variable_program";

CREATE INDEX program_rule_program ON programrule USING btree (programid);
CREATE INDEX program_rule_variable_program ON programrulevariable USING btree (programid);