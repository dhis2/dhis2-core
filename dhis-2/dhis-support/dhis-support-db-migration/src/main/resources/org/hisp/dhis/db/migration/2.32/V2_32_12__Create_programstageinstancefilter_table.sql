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
    eventstatus character varying(50),
    eventcreatedperiod jsonb,
    eventdatavaluefilters jsonb
);

alter table ONLY programstageinstancefilter
add constraint programstageinstancefilter_pkey primary key (programstageinstancefilterid),
add constraint uk_programstageinstancefilter_uid unique (uid),
add constraint fk_lastupdatedby_userid foreign key (lastupdatedby) references userinfo(userinfoid),
add constraint fk_programstageinstancefilter_programid foreign key (programid) references program(programid),
add constraint fk_programstageinstancefilter_programstageid foreign key (programstageid) references programstage(programstageid),
add constraint fk_programstageinstancefilter_organisationunitid foreign key (organisationunitid) references organisationunit(organisationunitid);

