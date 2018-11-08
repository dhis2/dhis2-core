--Drop tables optionusergroupaccesses and optionuseraccesses if not already created
DROP TABLE IF  EXISTS optionusergroupaccesses ;
DROP TABLE IF  EXISTS optionuseraccesses ;

--Drop columns userid and publicaccess from option if present
ALTER TABLE optionvalue DROP COLUMN IF  EXISTS userid;
ALTER TABLE optionvalue DROP COLUMN IF  EXISTS publicaccess;

--Drop foreign key constraints
ALTER TABLE optionvalue DROP CONSTRAINT IF EXISTS fk_option_userid;



