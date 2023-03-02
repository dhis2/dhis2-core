create table if not exists metadatachangelog (
    metadatachangelogid bigint not null,
    uid varchar(11) NOT NULL,
    code varchar(50) unique,
    created timestamp not null,
    lastupdated timestamp,
    createdby bigint not null,
    name varchar(250) unique not null ,
    type varchar(50),
    version varchar(10) not null,
    dhisversion varchar(10) not null,
    locale varchar(10) not null,
    importfile bigint,

    constraint metadatachangelogid primary key (metadatachangelogid),
    constraint fk_metadatachangelog_fileresourceid foreign key (importfile) references fileresource (fileresourceid),
    constraint fk_metadatachangelog_createdby_userinfoid FOREIGN KEY (createdby) REFERENCES userinfo(userinfoid)
    );





