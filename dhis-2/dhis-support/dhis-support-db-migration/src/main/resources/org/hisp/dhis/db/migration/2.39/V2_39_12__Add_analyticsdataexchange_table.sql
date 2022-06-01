
-- public.analyticsdataexchange table

drop table if exists public.analyticsdataexchange;

create table public.analyticsdataexchange (
    analyticsdataexchangeid int8 not null,
    uid varchar(11) null,
    code varchar(100) null,
    created timestamp null,
    userid int8 null,
    lastupdated timestamp null,
    lastupdatedby int8 null,
    translations jsonb null,
    sharing jsonb null default '{}'::jsonb,
    name varchar(230) not null,
    "source" jsonb not null,
    "target" jsonb not null,
    constraint analyticsdataexchange_uid_key unique (uid),
    constraint analyticsdataexchange_code_key unique (code),
    constraint analyticsdataexchange_name_key unique (name),
    constraint analyticsdataexchange_pkey primary key (analyticsdataexchangeid)
);

-- public.analyticsdataexchange foreign keys

alter table public.analyticsdataexchange add constraint fk_analyticsdataexchange_userid_userinfoid 
    foreign key (userid) references public.userinfo(userinfoid);
alter table public.analyticsdataexchange add constraint fk_analyticsdataexchange_lastupdateby_userinfoid 
    foreign key (lastupdatedby) references public.userinfo(userinfoid);
