-- This script relates to the task https://jira.dhis2.org/browse/DHIS2-11418
CREATE TABLE IF NOT EXISTS api_token
(
    apiTokenId    bigint                      not null primary key,
    uid           character varying(11)       not null unique,
    code          character varying(50) unique,
    created       timestamp without time zone not null,
    lastUpdated   timestamp without time zone not null,
    lastUpdatedBy bigint                      not null,
    createdBy     bigint                      not null,
    version       int4                        not null,
    type          character varying(50)       NOT NULL,
    expire        bigint                      not null,
    key           character varying(128)      not null,
    attributes    JSONB default '{}'::JSONB,
    sharing       JSONB default '{}'::JSONB
);
