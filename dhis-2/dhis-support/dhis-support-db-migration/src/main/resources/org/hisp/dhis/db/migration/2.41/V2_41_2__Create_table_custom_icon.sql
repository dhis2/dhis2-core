CREATE TABLE IF NOT EXISTS customicon
(
    customiconid   int8 GENERATED ALWAYS AS IDENTITY,
    "key"          varchar(100) NOT NULL,
    fileresourceid int8         NOT NULL,
    description    text         NULL,
    keywords       text[]       NULL,
    created        TIMESTAMP    NULL DEFAULT CURRENT_TIMESTAMP,
    createdby      bigint       NOT NULL,
    CONSTRAINT customicon_pkey PRIMARY KEY (customiconid),
    CONSTRAINT customicon_ukey UNIQUE ("key"),
    CONSTRAINT customicon_fileresource_ukey UNIQUE (fileresourceid)
);

ALTER TABLE customicon
    DROP CONSTRAINT IF EXISTS fk_customicon_file_resource;
ALTER TABLE customicon
    ADD CONSTRAINT fk_customicon_file_resource FOREIGN KEY (fileresourceid) REFERENCES fileresource (fileresourceid) ON DELETE CASCADE;
ALTER TABLE customicon
    DROP CONSTRAINT IF EXISTS fk_createdby_userid;
ALTER TABLE customicon
    ADD CONSTRAINT fk_createdby_userid FOREIGN KEY (createdby) REFERENCES userinfo (userinfoid);