create table if not exists programtempowner (
    programtempownerid BIGINT NOT NULL,
    programid BIGINT,
    trackedentityinstanceid BIGINT,
    validtill timestamp without time zone,
    userid BIGINT,
    reason character varying(50000)
);

alter table programtempowner 
	drop constraint if exists programtempowner_pkey,
	drop constraint if exists fk_programtempowner_programid,
	drop constraint if exists fk_programtempowner_trackedentityinstanceid,
	drop constraint if exists fk_programtempowner_userid;
 

alter table programtempowner 
	add constraint programtempowner_pkey PRIMARY KEY (programtempownerid),
	add constraint fk_programtempowner_programid FOREIGN KEY (programid) REFERENCES program(programid),
	add constraint fk_programtempowner_trackedentityinstanceid FOREIGN KEY (trackedentityinstanceid) REFERENCES trackedentityinstance(trackedentityinstanceid),
	add constraint fk_programtempowner_userid FOREIGN KEY (userid) REFERENCES users(userid);

create index in_programtempowner_validtill on programtempowner (extract(epoch FROM validtill));
    
alter table programownershiphistory add column if not exists organisationunitid BIGINT;
alter table programownershiphistory drop constraint if exists fk_programownershiphistory_organisationunitid;
alter table programownershiphistory add constraint fk_programownershiphistory_organisationunitid FOREIGN KEY (organisationunitid) REFERENCES organisationunit(organisationunitid);

