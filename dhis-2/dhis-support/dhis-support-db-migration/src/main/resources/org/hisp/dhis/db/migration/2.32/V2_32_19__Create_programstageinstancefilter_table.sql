--Create table programstageinstancefilter
create table programstageinstancefilter (
programstageinstancefilterid bigint not null,
uid character varying(11) not null,
created timestamp without time zone not null,
lastupdated timestamp without time zone not null,
lastupdatedby bigint,
name character varying(230) not null,
description character varying(255),
program character varying(11) not null,
programstage character varying(11),
eventquerycriteria jsonb,
userid bigint,
publicaccess character varying(8)
);

--Create table programstageinstancefilterusergroupaccesses
create table if not exists programstageinstancefilterusergroupaccesses (
programstageinstancefilterid bigint not null,
usergroupaccessid integer not null
);

--Create table programstageinstancefilteruseraccesses
create table if not exists programstageinstancefilteruseraccesses (
programstageinstancefilterid bigint not null,
useraccessid integer not null
);

--Adding constraints for programstageinstancefilter
alter table programstageinstancefilter
add constraint programstageinstancefilter_pkey primary key (programstageinstancefilterid),
add constraint uk_programstageinstancefilter_uid unique (uid),
add constraint fk_programstageinstancefilter_userid foreign key (userid) references userinfo(userinfoid),
add constraint fk_lastupdatedby_userid foreign key (lastupdatedby) references userinfo(userinfoid);

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

