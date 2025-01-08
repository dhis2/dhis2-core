-- Support program indicator disaggregation

CREATE TABLE IF NOT EXISTS program_categorymapping
(
    categorymappingid       bigint          NOT NULL,
    programid               bigint          NOT NULL,
    categoryid              bigint          NOT NULL,
    mappingName             varchar(126)    NOT NULL,
    uid                     varchar(11)     NOT NULL,
    code                    varchar(50),
    created                 timestamp       NOT NULL,
    lastupdated             timestamp       NOT NULL,
    lastupdatedby           bigint,
    CONSTRAINT program_categorymapping_pkey PRIMARY KEY (categorymappingid),
    CONSTRAINT fk_categorymapping_programid FOREIGN KEY (programid)
        REFERENCES program(programid),
    CONSTRAINT fk_categorymapping_categoryid FOREIGN KEY (categoryid)
        REFERENCES category(categoryid),
    CONSTRAINT categorymapping_unique_key UNIQUE (programid, categoryid, mappingName),
    CONSTRAINT categorymapping_uid_key UNIQUE (uid),
    CONSTRAINT categorymapping_code_key UNIQUE (code),
    CONSTRAINT fk_lastupdateby_userid FOREIGN KEY (lastupdatedby) REFERENCES userinfo(userinfoid)
);

CREATE TABLE IF NOT EXISTS program_categoryoptionmapping
(
    optionmappingid         bigint          NOT NULL,
    categorymappingid       bigint          NOT NULL,
    categoryoptionid        bigint          NOT NULL,
    filter                  text            NOT NULL,
    CONSTRAINT program_categoryoptionmapping_pkey PRIMARY KEY (optionmappingid),
    CONSTRAINT fk_optionmapping_categorymappingid FOREIGN KEY (categorymappingid)
        REFERENCES program_categorymapping(categorymappingid),
    CONSTRAINT fk_optionmapping_categoryoptionid FOREIGN KEY (categoryoptionid)
        REFERENCES categoryoption(categoryoptionid),
    CONSTRAINT categoryoptionmapping_ukey UNIQUE (categorymappingid, categoryoptionid)
    );

ALTER TABLE programindicator ADD COLUMN IF NOT EXISTS aggregateexportdataelement varchar(255);
ALTER TABLE programindicator ADD COLUMN IF NOT EXISTS categorycomboid bigint;
ALTER TABLE programindicator ADD COLUMN IF NOT EXISTS attributecomboid bigint;
ALTER TABLE programindicator DROP CONSTRAINT IF EXISTS fk_programindicator_categorycomboid;
ALTER TABLE programindicator DROP CONSTRAINT IF EXISTS fk_programindicator_attributecomboid;
ALTER TABLE programindicator
    ADD CONSTRAINT fk_programindicator_categorycomboid FOREIGN KEY (categorycomboid)
        REFERENCES categorycombo(categorycomboid);
ALTER TABLE programindicator
    ADD CONSTRAINT fk_programindicator_attributecomboid FOREIGN KEY (attributecomboid)
        REFERENCES categorycombo(categorycomboid);

UPDATE programindicator
SET categorycomboid = (SELECT categorycomboid FROM categorycombo WHERE name = 'default' LIMIT 1)
WHERE categorycomboid IS NULL;

UPDATE programindicator
SET attributecomboid = (SELECT categorycomboid FROM categorycombo WHERE name = 'default' LIMIT 1)
WHERE attributecomboid IS NULL;

ALTER TABLE programindicator ALTER COLUMN categorycomboid SET NOT NULL;
ALTER TABLE programindicator ALTER COLUMN attributecomboid SET NOT NULL;

CREATE TABLE IF NOT EXISTS programindicator_categorymapping
(
    programindicatorid      bigint          NOT NULL,
    categoryid              bigint          NOT NULL,
    categorymappingid       bigint          NOT NULL,
    CONSTRAINT programindicator_categorymapping_pkey PRIMARY KEY (programindicatorid, categoryid),
    CONSTRAINT fk_pi_categorymapping_programindicatorid FOREIGN KEY (programindicatorid)
        REFERENCES programindicator(programindicatorid),
    CONSTRAINT fk_pi_categorymapping_categoryid FOREIGN KEY (categoryid)
        REFERENCES category(categoryid),
    CONSTRAINT fk_pi_categorymapping_categorymappingid FOREIGN KEY (categorymappingid)
        REFERENCES program_categorymapping(categorymappingid)
);
