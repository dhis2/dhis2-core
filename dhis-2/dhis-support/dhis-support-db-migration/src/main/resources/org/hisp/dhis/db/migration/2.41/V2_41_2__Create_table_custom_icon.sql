

-- customicon table creation

CREATE TABLE IF NOT EXISTS customicon
(
    id                  bigint         NOT NULL,
    uid                 character      varying(11) NOT NULL,
    code                character      varying(50),
    iconkey             varchar(100)   NOT NULL,
    fileresourceid      bigint         NULL,
    description         text           NOT NULL,
    keywords            jsonb          NULL,
    created             TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lastupdated         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    createdby           bigint         NULL,
    lastupdatedby       bigint         NULL,
    custom              Boolean        NOT NULL,
    CONSTRAINT customicon_pkey PRIMARY KEY (id),
    CONSTRAINT customicon_ukey UNIQUE (iconkey),
    CONSTRAINT customicon_fileresource_ukey UNIQUE (fileresourceid)
);


-- customicon table constraints

ALTER TABLE customicon DROP CONSTRAINT IF EXISTS fk_customicon_fileresource;
ALTER TABLE customicon ADD CONSTRAINT fk_customicon_fileresource FOREIGN KEY (fileresourceid) REFERENCES fileresource (fileresourceid) ON DELETE CASCADE;