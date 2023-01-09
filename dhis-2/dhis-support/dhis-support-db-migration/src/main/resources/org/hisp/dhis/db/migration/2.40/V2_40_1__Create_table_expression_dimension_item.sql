create table if not exists expressiondimensionitem
(
    expressiondimensionitemid bigint not null,
    code character varying(50),
    name character varying(230) not null,
    shortname character varying(50),
    formname character varying(230),
    expression text,
    description text,
    missingvaluestrategy character varying(255) not null,
    slidingwindow boolean,
    lastupdated timestamp without time zone,
    uid character varying(11),
    created timestamp without time zone,
    userid bigint,
    publicaccess character varying(8),
    lastupdatedby bigint,
    translations jsonb default '[]'::jsonb,
    attributevalues jsonb default '{}'::jsonb,
    sharing jsonb default '{}'::jsonb,
    constraint expressiondimensionitem_pkey primary key (expressiondimensionitemid),
    constraint expressiondimensionitem_code_key unique (code),
    constraint expressiondimensionitem_uid_key unique (uid),
    constraint fk_expressiondimensionitem_userid foreign key (userid)
    references userinfo (userinfoid) match simple
                          on update no action
                          on delete no action,
    constraint fk_lastupdateby_userid foreign key (lastupdatedby)
    references userinfo (userinfoid) match simple
                          on update no action
                          on delete no action
    );
