create table if not exists programstageworkinglist (
    programstageworkinglistid int8 not null,
    uid varchar(11) not null,
    code varchar(50),
    created timestamp not null,
    createdby int8,
    lastupdated timestamp not null,
    lastupdatedby int8,
    name varchar(230) not null,
    description varchar(255),
    programid int8 not null,
    programstageid int8 not null,
    programstagequerycriteria jsonb default '[]'::jsonb,
    translations jsonb default '[]'::jsonb,
    sharing jsonb default '[]'::jsonb,
    userid int8,
    constraint programstageworkinglist_uid_key unique (uid),
    constraint programstageworkinglist_code_key unique (code),
    constraint programstageworkinglist_pkey primary key (programstageworkinglistid),
    constraint fk_programstageworkinglist_programid
        foreign key(programid)
        references program(programid)
        on delete cascade,
    constraint fk_programstageworkinglist_programstageid
        foreign key(programstageid)
        references programstage(programstageid)
        on delete cascade
);
