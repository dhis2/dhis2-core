drop table if exists metadataproposal;
create table metadataproposal (
    proposalid bigint not null primary key,
    uid character varying(11) not null unique,
    type character varying(6) not null,
    status character varying(12) not null,
    target character varying(30) not null,
    targetUid character varying(11),
    created timestamp without time zone not null,
    createdby bigint not null,
    change jsonb not null,
    comment character varying(255),
    reason character varying(1024),
    finalised timestamp without time zone,
    finalisedby bigint
);

alter table metadataproposal
    add constraint fk_createdby_userid foreign key (createdby) references users(userid);
alter table metadataproposal
    add constraint fk_finalisedby_userid foreign key (finalisedby) references users(userid);
