
-- Add eventhook table

create table if not exists eventhook (
    eventhookid int8 not null,
    uid varchar(11) null,
    code varchar(100) null,
    disabled bool NOT NULL,
    created timestamp null,
    userid int8 null,
    lastupdated timestamp null,
    lastupdatedby int8 null,
    description text null,
    translations jsonb default '[]'::jsonb,
    attributevalues jsonb default '{}'::jsonb,
    sharing jsonb default '{}'::jsonb,
    name varchar(230) not null,
    source jsonb default '{}'::jsonb,
    targets jsonb default '[]'::jsonb,
    constraint eventhook_uid_key unique (uid),
    constraint eventhook_code_key unique (code),
    constraint eventhook_name_key unique (name),
    constraint eventhook_pkey primary key (eventhookid)
);

-- Add eventhook foreign keys

alter table eventhook add constraint fk_eventhook_userid_userinfoid
    foreign key (userid) references userinfo(userinfoid);
alter table eventhook add constraint fk_eventhook_lastupdateby_userinfoid
    foreign key (lastupdatedby) references userinfo(userinfoid);
