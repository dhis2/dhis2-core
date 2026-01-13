-- Migration related to DHIS2-20379.


-- Create new table + constraints.
create table if not exists configuration_dataoutputperiodtype (
    configurationid int4 not null,
    periodtypeid int4 not null
);

alter table configuration_dataoutputperiodtype add constraint fk_configuration_dataoutputperiodtype_periodtypeid
    foreign key (periodtypeid) references periodtype(periodtypeid);

alter table configuration_dataoutputperiodtype add constraint fk_configuration_dataoutputperiodtype_configurationid
    foreign key (configurationid) references configuration(configurationid);

alter table configuration_dataoutputperiodtype add constraint uk_configuration_dataoutputperiodtype unique (configurationid, periodtypeid);

-- Migrate required data into the new table.
do
$$
declare
    stmt TEXT;
begin
  -- Check if configuration table has row.
  if exists (
    select 1
    from configuration
    )
  then
    insert into configuration_dataoutputperiodtype (periodtypeid, configurationid) select p.periodtypeid, c.configurationid from periodtype p, configuration c where p.name != 'TwoYearly';
  else
    RAISE INFO '%','Configuration table has no rows';
  end if;
end;
$$ language plpgsql;