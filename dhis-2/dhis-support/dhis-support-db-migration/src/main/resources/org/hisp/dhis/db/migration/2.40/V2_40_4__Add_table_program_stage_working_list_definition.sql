CREATE TABLE if not exists programstageworkinglistdefinition (
    programstageworkinglistdefinitionid integer NOT NULL,
    uid character varying(11) NOT NULL,
    code character varying(50),
    created timestamp without time zone NOT NULL,
    lastupdated timestamp without time zone NOT NULL,
    lastupdatedby integer,
    name character varying(230) NOT NULL,
    description character varying(255),
    sortorder integer,
    style jsonb,
    programid integer NOT NULL,
    programstageid integer NOT NULL,
    programStageQueryCriteria jsonb,
    translations jsonb,
    sharing jsonb,
    userid integer
);