drop type if exists metatdata_proposal_type;
create type metatdata_proposal_type as enum (
    'ADD',
    'UPDATE',
    'REMOVE'
);

drop table if exists metadataproposal;
create table metadataproposal (
    proposalid bigint not null primary key,
    uid character varying(11) not null unique,
    type character varying(20) not null,
    target metatdata_proposal_type not null,
    targetUid character varying(11),
    created timestamp without time zone not null,
    createdby bigint not null,
    change jsonb not null,
    comment character varying(255)
);

alter table metadataproposal
    add constraint fk_createdby_userid foreign key (createdby) references users(userid);
