CREATE TABLE IF NOT EXISTS public.expressiondimensionitem
(
    expressionid bigint NOT NULL,
    name character varying(230) COLLATE pg_catalog."default" NOT NULL,
    code character varying(50) COLLATE pg_catalog."default",
    expression text,
    description text COLLATE pg_catalog."default",
    missingvaluestrategy character varying(255) NOT NULL,
    slidingwindow boolean,
    lastupdated timestamp without time zone,
    uid character varying(11) COLLATE pg_catalog."default",
    created timestamp without time zone,
    userid bigint,
    publicaccess character varying(8) COLLATE pg_catalog."default",
    lastupdatedby bigint,
    translations jsonb DEFAULT '[]'::jsonb,
    attributevalues jsonb DEFAULT '{}'::jsonb,
    sharing jsonb DEFAULT '{}'::jsonb,
    CONSTRAINT expressiondimensionitem_pkey PRIMARY KEY (expressionid),
    CONSTRAINT expressiondimensionitem_code_key UNIQUE (code),
    CONSTRAINT expressiondimensionitem_uid_key UNIQUE (uid),
    CONSTRAINT fk_expressiondimensionitem_userid FOREIGN KEY (userid)
    REFERENCES public.userinfo (userinfoid) MATCH SIMPLE
                          ON UPDATE NO ACTION
                          ON DELETE NO ACTION,
    CONSTRAINT fk_lastupdateby_userid FOREIGN KEY (lastupdatedby)
    REFERENCES public.userinfo (userinfoid) MATCH SIMPLE
                          ON UPDATE NO ACTION
                          ON DELETE NO ACTION
    )

    TABLESPACE pg_default;

ALTER TABLE IF EXISTS public.expressiondimensionitem
    OWNER to postgres;