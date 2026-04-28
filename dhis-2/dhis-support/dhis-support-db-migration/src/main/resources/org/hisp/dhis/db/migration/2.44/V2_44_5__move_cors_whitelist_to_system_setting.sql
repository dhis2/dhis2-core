-- Move CORS whitelist persistence from the legacy singleton Configuration entity
-- to the in-memory system settings snapshot, then remove the legacy table.
-- Idempotent: the to_regclass guard skips work on a replay where the legacy table
-- has already been dropped, and the not-exists check skips re-insertion when
-- 'corsWhitelist' is already present in systemsetting.
do $$
begin
  if to_regclass('configuration_corswhitelist') is not null then
    with migrated_cors_whitelist as (
      select string_agg(cors_entry, ',' order by cors_entry) as value
      from (
        select distinct trim(corswhitelist) as cors_entry
        from configuration_corswhitelist
        where corswhitelist is not null
          and trim(corswhitelist) <> ''
      ) entries
    )
    insert into systemsetting(systemsettingid, name, value)
    select nextval('hibernate_sequence'), 'corsWhitelist', value
    from migrated_cors_whitelist
    where value is not null
      and not exists (
        select 1
        from systemsetting
        where name = 'corsWhitelist'
      );
  end if;
end $$;

drop table if exists configuration_corswhitelist cascade;
