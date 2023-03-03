CREATE TABLE IF NOT EXISTS metadatapackage (
    metadatapackageid int NOT NULL,
    uid VARCHAR(11) NOT NULL,
    code VARCHAR(50) ,
    created TIMESTAMP NOT NULL,
    lastupdated TIMESTAMP ,
    createdby int8 NOT NULL,
    name VARCHAR(250) NOT NULL,
    type VARCHAR(50),
    version VARCHAR(10) NOT NULL,
    dhis2version VARCHAR(10) NOT NULL,
    dhis2build VARCHAR(10) NOT NULL,
    locale VARCHAR(10) NOT NULL,
    importfile int8,
    lastupdatedby int8,

    CONSTRAINT metadatapackageid PRIMARY KEY (metadatapackageid),
    CONSTRAINT fk_metadatapackage_fileresourceid FOREIGN KEY (importfile) REFERENCES fileresource (fileresourceid),
    CONSTRAINT fk_metadatapackage_createdby_userinfoid FOREIGN KEY (createdby) REFERENCES userinfo(userinfoid),
    CONSTRAINT fk_metadatapackage_lastupdateby_userinfoid FOREIGN KEY (lastupdatedby) REFERENCES userinfo(userinfoid) );





