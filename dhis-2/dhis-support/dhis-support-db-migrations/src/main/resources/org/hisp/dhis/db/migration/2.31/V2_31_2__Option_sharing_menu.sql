--Creating tables optionusergroupaccesses and optionuseraccesses if not already created

CREATE TABLE IF NOT EXISTS optionusergroupaccesses (
    optionvalueid integer NOT NULL,
    usergroupaccessid integer NOT NULL
);

CREATE TABLE IF NOT EXISTS optionuseraccesses (
    optionvalueid integer NOT NULL,
    useraccessid integer NOT NULL
);


--Droping existing foreign key constraints in optionuseraccesses
ALTER TABLE optionuseraccesses 
DROP CONSTRAINT IF EXISTS optionuseraccesses_pkey,
DROP CONSTRAINT IF EXISTS fk_option_useraccessid;

--Adding foreign key constraints for optionuseraccesses
ALTER TABLE optionuseraccesses
ADD CONSTRAINT optionuseraccesses_pkey PRIMARY KEY (optionvalueid, useraccessid),
ADD CONSTRAINT fk_option_useraccessid FOREIGN KEY (useraccessid) REFERENCES useraccess(useraccessid);

--Droping existing foreign key constraints in optionusergroupaccesses
ALTER TABLE optionusergroupaccesses 
DROP CONSTRAINT IF EXISTS optionusergroupaccesses_pkey,
DROP CONSTRAINT IF EXISTS fk_option_usergroupaccessid;

--Adding foreign key constraints for optionusergroupaccesses
ALTER TABLE optionusergroupaccesses
ADD CONSTRAINT optionusergroupaccesses_pkey PRIMARY KEY (optionvalueid, usergroupaccessid),
ADD CONSTRAINT fk_option_usergroupaccessid FOREIGN KEY (usergroupaccessid) REFERENCES usergroupaccess(usergroupaccessid);

--Adding columns userid and publicaccess into option if not already present
ALTER TABLE optionvalue 
ADD COLUMN IF NOT EXISTS userid integer,
ADD COLUMN IF NOT EXISTS publicaccess character varying(8);

--Droping existing foreign key constraints to make the script idempotent
ALTER TABLE optionvalue 
DROP CONSTRAINT IF EXISTS fk_option_userid;

--Adding foreign key constraints for option
ALTER TABLE optionvalue
ADD CONSTRAINT fk_option_userid FOREIGN KEY (userid) REFERENCES userinfo(userinfoid);

