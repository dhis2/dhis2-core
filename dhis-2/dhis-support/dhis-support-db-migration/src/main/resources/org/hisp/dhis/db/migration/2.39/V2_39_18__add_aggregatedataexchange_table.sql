
-- This table superseeds the 'analyticsdataexchange' table added in migration 'V2_39_12'

drop table if exists analyticsdataexchange;

-- Add aggregatedataexchange table

drop table if exists aggregatedataexchange;

create table aggregatedataexchange (
    aggregatedataexchangeid int8 not null,
    uid varchar(11) null,
    code varchar(100) null,
    created timestamp null,
    userid int8 null,
    lastupdated timestamp null,
    lastupdatedby int8 null,
    translations jsonb null,
    sharing jsonb null default '{}'::jsonb,
    attributevalues jsonb null default '{}'::jsonb,
    name varchar(230) not null,
    "source" jsonb not null,
    "target" jsonb not null,
    constraint aggregatedataexchange_uid_key unique (uid),
    constraint aggregatedataexchange_code_key unique (code),
    constraint aggregatedataexchange_name_key unique (name),
    constraint aggregatedataexchange_pkey primary key (aggregatedataexchangeid)
);

-- Add aggregatedataexchange foreign keys

alter table aggregatedataexchange add constraint fk_aggregatedataexchange_userid_userinfoid 
    foreign key (userid) references userinfo(userinfoid);
alter table aggregatedataexchange add constraint fk_aggregatedataexchange_lastupdateby_userinfoid 
    foreign key (lastupdatedby) references userinfo(userinfoid);
