-- Migration related to DHIS2-20837.

-- Migrate required data into the new table.
do
$$
declare
    stmt TEXT;
begin
  -- Check if configuration table has row.
  if not exists (
    select 1
    from periodtype where name = 'WeeklyFriday';
    )
  then
    insert into periodtype (periodtypeid, name) values (nextval('hibernate_sequence'), 'WeeklyFriday');
  else
    RAISE INFO '%','WeeklyFriday already exists';
  end if;

insert into configuration_dataoutputperiodtype (periodtypeid, configurationid) select p.periodtypeid, c.configurationid from periodtype p, configuration c where p.name = 'WeeklyFriday';
