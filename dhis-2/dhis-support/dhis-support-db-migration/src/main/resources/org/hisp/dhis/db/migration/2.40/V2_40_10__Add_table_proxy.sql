-- Add proxy table

DROP TABLE IF EXISTS proxy;

CREATE TABLE proxy (
	proxyid int8 NOT NULL,
	uid varchar(11) NOT NULL,
	code varchar(50) NULL,
	created timestamp NOT NULL,
	lastupdated timestamp NOT NULL,
	lastupdatedby int8 NULL,
	name varchar(230) NOT NULL,
	description text NULL,
	url text NOT NULL,
	enabled bool NOT NULL,
	auth jsonb NULL DEFAULT '{}'::jsonb,
	headers jsonb NULL DEFAULT '{}'::jsonb,
	userid int8 NULL,
	translations jsonb  DEFAULT '[]'::jsonb,
	sharing jsonb NULL DEFAULT '{}'::jsonb,
	attributevalues jsonb NULL,
	CONSTRAINT proxy_pkey PRIMARY KEY (proxyid),
	CONSTRAINT proxy_uid_key UNIQUE (uid),
	CONSTRAINT proxy_code_key UNIQUE (code),
	CONSTRAINT proxy_name_key UNIQUE (name),
	CONSTRAINT fk_proxy_lastupdateby_userinfoid FOREIGN KEY (lastupdatedby) REFERENCES userinfo(userinfoid),
	CONSTRAINT fk_proxy_userid_userinfoid FOREIGN KEY (userid) REFERENCES userinfo(userinfoid)
);
