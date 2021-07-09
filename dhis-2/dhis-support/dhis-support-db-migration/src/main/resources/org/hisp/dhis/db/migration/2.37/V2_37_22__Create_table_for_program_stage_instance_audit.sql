CREATE TABLE programstageinstanceaudit (
    programstageinstanceauditid bigint NOT NULL,
    programstageinstance character varying(255),
    created timestamp without time zone,
    modifiedby character varying(255),
    audittype character varying(100) NOT NULL
);