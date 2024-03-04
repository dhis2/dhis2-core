

-- icon table creation

CREATE TABLE IF NOT EXISTS icon
(
    iconid              bigint         NOT NULL,
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
    CONSTRAINT icon_pkey PRIMARY KEY (iconid),
    CONSTRAINT icon_ukey UNIQUE (iconkey),
    CONSTRAINT icon_fileresource_ukey UNIQUE (fileresourceid)
);


-- icon table constraints

ALTER TABLE icon DROP CONSTRAINT IF EXISTS fk_icon_fileresource;
ALTER TABLE icon ADD CONSTRAINT fk_icon_fileresource FOREIGN KEY (fileresourceid) REFERENCES fileresource (fileresourceid) ON DELETE CASCADE;