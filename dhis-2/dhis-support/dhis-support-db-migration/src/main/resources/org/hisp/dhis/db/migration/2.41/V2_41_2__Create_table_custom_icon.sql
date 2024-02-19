

-- customicon table creation

CREATE TABLE IF NOT EXISTS customicon
(
    id                int8 GENERATED ALWAYS AS IDENTITY,
    uid                character varying(11) NOT NULL,
    code                character varying(50),
    iconkey           varchar(100) NOT NULL,
    fileresourceid       int8         NOT NULL,
    description          text         NULL,
    created             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lastupdated         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    createdby           bigint       NOT NULL,
    lastupdatedby       bigint       NOT NULL,
    CONSTRAINT customicon_pkey PRIMARY KEY (id),
    CONSTRAINT customicon_ukey UNIQUE (iconkey),
    CONSTRAINT customicon_fileresource_ukey UNIQUE (fileresourceid)
    );


-- customicon table constraints

ALTER TABLE customicon
DROP CONSTRAINT IF EXISTS fk_customicon_fileresource;
ALTER TABLE customicon
    ADD CONSTRAINT fk_customicon_fileresource FOREIGN KEY (fileresourceid) REFERENCES fileresource (fileresourceid) ON DELETE CASCADE;
ALTER TABLE customicon
DROP CONSTRAINT IF EXISTS fk_createdby_userid;
ALTER TABLE customicon
    ADD CONSTRAINT fk_createdby_userid FOREIGN KEY (createdby) REFERENCES userinfo (userinfoid);
ALTER TABLE customicon
    ADD CONSTRAINT fk_lastupdateby_userid FOREIGN KEY (lastupdatedby) REFERENCES userinfo (userinfoid);



-- customicon_keywords table creation

CREATE TABLE customicon_keywords (

                                     customiconid int not null,
                                     keywords character varying(255)
);



-- customicon_keywords table constraints

ALTER TABLE ONLY customicon_keywords
    ADD CONSTRAINT fk_custom_icon_keywords FOREIGN KEY (customiconid) REFERENCES customicon(id);
