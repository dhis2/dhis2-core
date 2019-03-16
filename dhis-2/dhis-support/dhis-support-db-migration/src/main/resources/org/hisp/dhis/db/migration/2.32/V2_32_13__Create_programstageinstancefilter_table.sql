--Create table programstageinstancefilter
create table programstageinstancefilter (
    programstageinstancefilterid bigint not NULL,
    uid character varying(11) not NULL,
    created timestamp without time zone not NULL,
    lastupdated timestamp without time zone not NULL,
    lastupdatedby bigint,
    name character varying(230) not NULL,
    description character varying(255),
    programid bigint,
    programstageid bigint,
    organisationunitid bigint,
    eventquerycriteria jsonb,
	userid bigint,
    publicaccess character varying(8)
);

--Create table programstageinstancefilterusergroupaccesses
create table if not exists programstageinstancefilterusergroupaccesses (
    programstageinstancefilterid bigint not null,
    usergroupaccessid bigint not null
);

--Create table programstageinstancefilteruseraccesses
create table if not exists programstageinstancefilteruseraccesses (
    programstageinstancefilterid bigint not null,
    useraccessid bigint not null
);

--Adding constraints for programstageinstancefilteruseraccesses
alter table programstageinstancefilteruseraccesses
add constraint programstageinstancefilteruseraccesses_pkey primary key (programstageinstancefilterid, useraccessid),
add constraint fk_programstageinstancefilter_programstageinstancefilterid foreign key (programstageinstancefilterid) references programstageinstancefilter(programstageinstancefilterid),
add constraint fk_programstageinstancefilter_useraccessid foreign key (useraccessid) references useraccess(useraccessid);

--Adding constraints for programstageinstancefilterusergroupaccesses
alter table programstageinstancefilterusergroupaccesses
add constraint programstageinstancefilterusergroupaccesses_pkey primary key (programstageinstancefilterid, usergroupaccessid),
add constraint fk_programstageinstancefilter_programstageinstancefilterid foreign key (programstageinstancefilterid) references programstageinstancefilter(programstageinstancefilterid),
add constraint fk_programstageinstancefilter_usergroupaccessid foreign key (usergroupaccessid) references usergroupaccess(usergroupaccessid);

--Adding constraints for programstageinstancefilter
alter table programstageinstancefilter
add constraint programstageinstancefilter_pkey primary key (programstageinstancefilterid),
add constraint uk_programstageinstancefilter_uid unique (uid),
add constraint fk_programstageinstancefilter_userid foreign key (userid) references userinfo(userinfoid);
add constraint fk_lastupdatedby_userid foreign key (lastupdatedby) references userinfo(userinfoid),
add constraint fk_programstageinstancefilter_programid foreign key (programid) references program(programid),
add constraint fk_programstageinstancefilter_programstageid foreign key (programstageid) references programstage(programstageid),
add constraint fk_programstageinstancefilter_organisationunitid foreign key (organisationunitid) references organisationunit(organisationunitid);

