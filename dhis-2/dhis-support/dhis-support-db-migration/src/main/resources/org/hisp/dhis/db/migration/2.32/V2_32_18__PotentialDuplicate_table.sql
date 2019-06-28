create sequence potentialduplicatesequence;

create table potentialduplicate (
  potentialduplicateid bigint not null primary key,
  uid character varying(11) not null unique,
  created timestamp without time zone not null,
  lastUpdated timestamp without time zone not null,
  lastUpdatedBy bigint not null,
  teiA character varying(11) not null,
  teiB character varying(11),
  status character varying(255) not null
);

alter table potentialduplicate
  add constraint potentialduplicate_teia_teib unique (teiA, teiB);

alter table potentialduplicate
  add constraint potentialduplicate_lastupdatedby_user foreign key (lastUpdatedBy) references users(userid);