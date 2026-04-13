create unlogged table if not exists usagemetricsconsent (
    usagemetricsconsentid int8 not null,
    dbsystemidentifier character varying not null,
    consent boolean not null,
    created timestamp,
    userid int8,
    lastupdated timestamp,
    lastupdatedby int8,
    constraint usagemetricsconsent_pkey primary key (usagemetricsconsentid),
    constraint onlyonetuple_check check (usagemetricsconsentid = 1)
);