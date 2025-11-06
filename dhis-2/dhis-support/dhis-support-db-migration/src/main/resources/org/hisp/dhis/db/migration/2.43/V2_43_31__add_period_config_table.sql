create table if not exists configuration_dataoutputperiodtype (
    configurationid int4 not null,
    periodtypeid int4 not null
);

alter table configuration_dataoutputperiodtype add constraint fk_configuration_dataoutputperiodtype_periodtypeid
    foreign key (periodtypeid) references periodtype(periodtypeid);

alter table configuration_dataoutputperiodtype add constraint fk_configuration_dataoutputperiodtype_configurationid
    foreign key (configurationid) references configuration(configurationid);

