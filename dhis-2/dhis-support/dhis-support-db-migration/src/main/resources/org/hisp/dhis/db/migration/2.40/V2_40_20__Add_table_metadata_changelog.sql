create table if not exists metadatachangelog (
    metadatachangelogid int not null,
    uid varchar(11) NOT NULL,
    code varchar(50) unique,
    created timestamp not null,
    lastupdated timestamp,
    createdby int8 not null,
    name varchar(250) unique not null,
    type varchar(50),
    version varchar(10) not null,
    dhis2version varchar(10) not null,
    dhis2build varchar(10) not null,
    locale varchar(10) not null,
    importfile int8,
    lastupdatedby int8,

    constraint metadatachangelogid primary key (metadatachangelogid),
    constraint fk_metadatachangelog_fileresourceid foreign key (importfile) references fileresource (fileresourceid),
    constraint fk_metadatachangelog_createdby_userinfoid FOREIGN KEY (createdby) REFERENCES userinfo(userinfoid),
    CONSTRAINT fk_metadatachangelog_lastupdateby_userinfoid FOREIGN KEY (lastupdatedby) REFERENCES userinfo(userinfoid)
    );





