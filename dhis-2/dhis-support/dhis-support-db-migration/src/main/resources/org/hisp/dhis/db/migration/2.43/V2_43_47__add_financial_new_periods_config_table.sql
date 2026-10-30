-- Migration related to DHIS2-20838.

do
$$
declare
    max_retries int := 10;
    attempt int := 0;
begin
  if not exists (
    select 1 from periodtype where name = 'FinancialFeb'
  ) then
    loop
      begin
        insert into periodtype (periodtypeid, name)
        values (nextval('hibernate_sequence'), 'FinancialFeb');
        exit; -- success, leave the loop
      exception
        when unique_violation then
          attempt := attempt + 1;
          if attempt >= max_retries then
            raise exception 'failed to insert FinancialFeb after % attempts due to duplicate key', max_retries;
          end if;
          raise info 'duplicate key on attempt %, retrying...', attempt;
          -- loop continues, nextval will get a new value
      end;
    end loop;
  else
    raise info '%', 'FinancialFeb already exists';
  end if;

  max_retries := 10;
  attempt := 0;

  if not exists (
    select 1 from periodtype where name = 'FinancialAug'
  ) then
    loop
      begin
        insert into periodtype (periodtypeid, name)
        values (nextval('hibernate_sequence'), 'FinancialAug');
        exit; -- success, leave the loop
      exception
        when unique_violation then
          attempt := attempt + 1;
          if attempt >= max_retries then
            raise exception 'failed to insert FinancialAug after % attempts due to duplicate key', max_retries;
          end if;
          raise info 'duplicate key on attempt %, retrying...', attempt;
          -- loop continues, nextval will get a new value
      end;
    end loop;
  else
    raise info '%', 'FinancialAug already exists';
  end if;
end;
$$ language plpgsql;

insert into configuration_dataoutputperiodtype (periodtypeid, configurationid)
select p.periodtypeid, c.configurationid from periodtype p, configuration c where p.name = 'FinancialFeb';

insert into configuration_dataoutputperiodtype (periodtypeid, configurationid)
select p.periodtypeid, c.configurationid from periodtype p, configuration c where p.name = 'FinancialAug';
