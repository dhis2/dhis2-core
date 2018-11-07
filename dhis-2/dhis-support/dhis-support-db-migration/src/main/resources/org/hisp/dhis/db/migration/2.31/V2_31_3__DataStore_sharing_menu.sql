--Creating tables keyjsonvalueusergroupaccesses and keyjsonvalueuseraccesses if not already created

CREATE TABLE IF NOT EXISTS keyjsonvalueusergroupaccesses (
    keyjsonvalueid integer NOT NULL,
    usergroupaccessid integer NOT NULL
);

CREATE TABLE IF NOT EXISTS keyjsonvalueuseraccesses (
    keyjsonvalueid integer NOT NULL,
    useraccessid integer NOT NULL
);


--Droping existing foreign key constraints in keyjsonvalueuseraccesses
ALTER TABLE keyjsonvalueuseraccesses 
DROP CONSTRAINT IF EXISTS keyjsonvalueuseraccesses_pkey,
DROP CONSTRAINT IF EXISTS fk_keyjsonvalue_useraccessid;

--Adding foreign key constraints for keyjsonvalueuseraccesses
ALTER TABLE keyjsonvalueuseraccesses
ADD CONSTRAINT keyjsonvalueuseraccesses_pkey PRIMARY KEY (keyjsonvalueid, useraccessid),
ADD CONSTRAINT fk_keyjsonvalue_useraccessid FOREIGN KEY (useraccessid) REFERENCES useraccess(useraccessid);

--Droping existing foreign key constraints in keyjsonvalueusergroupaccesses
ALTER TABLE keyjsonvalueusergroupaccesses 
DROP CONSTRAINT IF EXISTS keyjsonvalueusergroupaccesses_pkey,
DROP CONSTRAINT IF EXISTS fk_keyjsonvalue_usergroupaccessid;

--Adding foreign key constraints for keyjsonvalueusergroupaccesses
ALTER TABLE keyjsonvalueusergroupaccesses
ADD CONSTRAINT keyjsonvalueusergroupaccesses_pkey PRIMARY KEY (keyjsonvalueid, usergroupaccessid),
ADD CONSTRAINT fk_keyjsonvalue_usergroupaccessid FOREIGN KEY (usergroupaccessid) REFERENCES usergroupaccess(usergroupaccessid);

--Adding columns userid and publicaccess into keyjsonvalue if not already present
ALTER TABLE keyjsonvalue 
ADD COLUMN IF NOT EXISTS userid integer,
ADD COLUMN IF NOT EXISTS publicaccess character varying(8);

--Droping existing foreign key constraints to make the script idempotent
ALTER TABLE keyjsonvalue 
DROP CONSTRAINT IF EXISTS fk_keyjsonvalue_userid;

--Adding foreign key constraings for keyjsonvalue
ALTER TABLE keyjsonvalue
ADD CONSTRAINT fk_keyjsonvalue_userid FOREIGN KEY (userid) REFERENCES userinfo(userinfoid);


--Updating all existing datastore items to have public read write access.
UPDATE keyjsonvalue SET publicaccess='rw------' WHERE publicaccess IS NULL;