-- Add proxy table

drop table if exists proxy;

create table proxy (
    proxyid int8 not null,
    uid varchar(11) null,
    code varchar(100) null,
    created timestamp null,
    userid int8 null,
    lastupdated timestamp null,
    lastupdatedby int8 null,
    description varchar(100) null,
    translations jsonb default '[]'::jsonb,
    attributevalues jsonb default '{}'::jsonb,
    sharing jsonb default '{}'::jsonb,
    name varchar(230) not null,
    url varchar(230) not null,
    contenttype varchar(230) not null,
    headers jsonb default '{}'::jsonb,
    auth jsonb default '{}'::jsonb,
    constraint proxy_uid_key unique (uid),
    constraint proxy_code_key unique (code),
    constraint proxy_name_key unique (name),
    constraint proxy_pkey primary key (proxyid)
);

-- Add proxy foreign keys

alter table proxy add constraint fk_proxy_userid_userinfoid
    foreign key (userid) references userinfo(userinfoid);
alter table proxy add constraint fk_proxy_lastupdateby_userinfoid
    foreign key (lastupdatedby) references userinfo(userinfoid);
