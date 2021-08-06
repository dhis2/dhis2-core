-- This script relates to the task https://jira.dhis2.org/browse/DHIS2-11418
CREATE TABLE api_token
(
    apitokenid    BIGINT       NOT NULL,
    uid           VARCHAR(11)  NOT NULL,
    code          CHARACTER varying(50),
    version       int4         NOT NULL,
    type          int4         NOT NULL,
    expire        BIGINT       NOT NULL,
    key           VARCHAR(128) NOT NULL,
    attributes    JSONB default '{}'::JSONB,
    created       TIMESTAMP    NOT NULL,
    createdBy     BIGINT       NOT NULL,
    lastUpdated   TIMESTAMP    NOT NULL,
    lastUpdatedBy BIGINT       NOT NULL,
    sharing       JSONB default '{}'::JSONB
);