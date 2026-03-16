-- Migration related to DHIS2-20837.

do
$$
declare
    max_retries int := 10;
    attempt int := 0;
begin
  if not exists (
    select 1 from periodtype where name = 'WeeklyFriday'
  ) then
    loop
      begin
        insert into periodtype (periodtypeid, name)
        values (nextval('hibernate_sequence'), 'WeeklyFriday');
        exit; -- success, leave the loop
      exception
        when unique_violation then
          attempt := attempt + 1;
          if attempt >= max_retries then
            raise exception 'failed to insert WeeklyFriday after % attempts due to duplicate key', max_retries;
          end if;
          raise info 'duplicate key on attempt %, retrying...', attempt;
          -- loop continues, nextval will get a new value
      end;
    end loop;
  else
    raise info '%', 'WeeklyFriday already exists';
  end if;
end;
$$ language plpgsql;

insert into configuration_dataoutputperiodtype (periodtypeid, configurationid)
select p.periodtypeid, c.configurationid from periodtype p, configuration c where p.name = 'WeeklyFriday';
