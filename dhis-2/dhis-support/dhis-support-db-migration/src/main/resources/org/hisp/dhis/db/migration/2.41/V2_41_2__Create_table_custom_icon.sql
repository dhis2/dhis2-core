CREATE TABLE IF NOT EXISTS customicon (
    customiconid int8 NOT NULL,
    "key" varchar(100) NOT NULL,
    fileresourceid int8 NOT NULL,
    description text NULL,
    created TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    createdby bigint NOT NULL,
    CONSTRAINT customicon_pkey PRIMARY KEY (customiconid),
    CONSTRAINT customicon_ukey UNIQUE ("key"),
    CONSTRAINT customicon_fileresource_ukey UNIQUE (fileresourceid)
);

CREATE TABLE IF NOT EXISTS customiconkeywords (
    customiconid int8 NOT NULL,
    keyword VARCHAR(255) NULL,
    CONSTRAINT keyword_pkey PRIMARY KEY (customiconid,keyword)
);

ALTER TABLE customicon ADD CONSTRAINT fk_customicon_file_resource FOREIGN KEY (fileresourceid) REFERENCES fileresource(fileresourceid) ON DELETE CASCADE;
ALTER TABLE customicon ADD CONSTRAINT fk_createdby_userid FOREIGN KEY (createdby) REFERENCES userinfo(userinfoid);
ALTER TABLE customiconkeywords ADD CONSTRAINT fk_keyword_customiconid FOREIGN KEY (customiconid) REFERENCES customicon(customiconid) ON DELETE CASCADE;