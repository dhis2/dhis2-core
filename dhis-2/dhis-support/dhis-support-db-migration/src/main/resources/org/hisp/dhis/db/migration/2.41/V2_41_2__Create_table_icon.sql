

-- icon table creation

CREATE TABLE IF NOT EXISTS icon
(
    iconkey             varchar(100)   NOT NULL,
    fileresourceid      bigint         NOT NULL,
    description         text           NULL,
    keywords            jsonb          NULL,
    created             TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lastupdated         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    createdby           bigint         NULL,
    custom              boolean        NOT NULL,
    CONSTRAINT icon_pkey PRIMARY KEY (iconkey),
    CONSTRAINT icon_ukey UNIQUE (iconkey),
    CONSTRAINT icon_fileresource_ukey UNIQUE (fileresourceid)
);


-- icon table constraints

ALTER TABLE icon DROP CONSTRAINT IF EXISTS fk_icon_fileresource;
ALTER TABLE icon ADD CONSTRAINT fk_icon_fileresource FOREIGN KEY (fileresourceid) REFERENCES fileresource (fileresourceid) ON DELETE CASCADE;

ALTER TABLE icon DROP CONSTRAINT IF EXISTS fk_createdby_userid;
ALTER TABLE icon ADD CONSTRAINT fk_createdby_userid FOREIGN KEY (createdby) REFERENCES userinfo (userinfoid);