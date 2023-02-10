CREATE TABLE if not exists programstageworkinglist (
    programstageworkinglistid integer NOT NULL,
    uid character varying(11) NOT NULL,
    code character varying(50),
    created timestamp without time zone NOT NULL,
    createdby integer,
    lastupdated timestamp without time zone NOT NULL,
    lastupdatedby integer,
    name character varying(230) NOT NULL,
    description character varying(255),
    programid integer NOT NULL,
    programstageid integer NOT NULL,
    programStageQueryCriteria jsonb default '[]'::jsonb,
    translations jsonb default '[]'::jsonb,
    sharing jsonb default '[]'::jsonb,
    userid integer,
    constraint fk_programid
        foreign key(programid)
        references program(programid)
        on delete cascade,
    constraint fk_programstageid
        foreign key(programstageid)
        references programstage(programstageid)
        on delete cascade
);