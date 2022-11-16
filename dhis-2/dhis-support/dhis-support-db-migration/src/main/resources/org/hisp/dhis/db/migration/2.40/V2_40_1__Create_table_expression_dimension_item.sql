CREATE TABLE IF NOT EXISTS expressiondimensionitem
(
    expressiondimensionitemid bigint NOT NULL,
    code character varying(50),
    name character varying(230) NOT NULL,
    shortname character varying(50),
    formname character varying(230),
    expression text,
    description text,
    missingvaluestrategy character varying(255) NOT NULL,
    slidingwindow boolean,
    lastupdated timestamp without time zone,
    uid character varying(11),
    created timestamp without time zone,
    userid bigint,
    publicaccess character varying(8),
    lastupdatedby bigint,
    translations jsonb DEFAULT '[]'::jsonb,
    attributevalues jsonb DEFAULT '{}'::jsonb,
    sharing jsonb DEFAULT '{}'::jsonb,
    CONSTRAINT expressiondimensionitem_pkey PRIMARY KEY (expressiondimensionitemid),
    CONSTRAINT expressiondimensionitem_code_key UNIQUE (code),
    CONSTRAINT expressiondimensionitem_uid_key UNIQUE (uid),
    CONSTRAINT expressiondimensionitem_shortname_key UNIQUE (shortname),
    CONSTRAINT fk_expressiondimensionitem_userid FOREIGN KEY (userid)
    REFERENCES public.userinfo (userinfoid) MATCH SIMPLE
                          ON UPDATE NO ACTION
                          ON DELETE NO ACTION,
    CONSTRAINT fk_lastupdateby_userid FOREIGN KEY (lastupdatedby)
    REFERENCES public.userinfo (userinfoid) MATCH SIMPLE
                          ON UPDATE NO ACTION
                          ON DELETE NO ACTION
    );

ALTER TABLE IF EXISTS expressiondimensionitem
    OWNER to postgres;
