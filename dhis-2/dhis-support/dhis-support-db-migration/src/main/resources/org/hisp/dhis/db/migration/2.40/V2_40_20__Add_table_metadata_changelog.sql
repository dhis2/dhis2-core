create table if not exists metadatachangelog (
    metadatachangelogid int not null,
    code varchar(50),
    created timestamp not null,
    createdby bigint,
    name varchar(250) not null,
    type varchar(50),
    version varchar(10) not null,
    dhisversion varchar(10) not null,
    locale varchar(10) not null,
    success boolean,
    importfile integer,

    constraint metadatachangelogid primary key (metadatachangelogid),
    constraint fk_metadatachangelog_fileresourceid foreign key (importfile) references fileresource (fileresourceid);
    constraint metadatachangelog_code_key unique (code),
    constraint fk_metadatachangelog_createdby_userinfoid FOREIGN KEY (createdby) REFERENCES userinfo(userinfoid)
    );





