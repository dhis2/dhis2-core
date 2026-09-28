-- Data values in eventdatavalues no longer store createdByUserInfo when it is the event's
-- createdbyuserinfo, or lastUpdatedByUserInfo when it is the data value's createdByUserInfo. A
-- missing user is now derived from these instead of meaning unknown. Some versions stored data
-- values without users on events that have one (2.35.1 when adding values to an existing event,
-- 2.36.0 to 2.36.6 through /api/tracker). Mark these users as unknown with an empty object, so
-- that they are not attributed to the event's creator. Rerunning it changes nothing.
-- DHIS2-22181

do $$
declare
    t text;
begin
    foreach t in array array['trackerevent', 'singleevent'] loop
        execute format($sql$
            update %1$I ev
            set eventdatavalues = (
                select jsonb_object_agg(
                    dv.key,
                    dv.value
                    || case
                        when not dv.value ? 'createdByUserInfo'
                            and ev.createdbyuserinfo is not null
                            then '{"createdByUserInfo": {}}'::jsonb
                        else '{}'::jsonb
                    end
                    || case
                        when not dv.value ? 'lastUpdatedByUserInfo'
                            and (dv.value ? 'createdByUserInfo' or ev.createdbyuserinfo is not null)
                            then '{"lastUpdatedByUserInfo": {}}'::jsonb
                        else '{}'::jsonb
                    end)
                from jsonb_each(ev.eventdatavalues) dv)
            where (ev.createdbyuserinfo is not null
                    and jsonb_path_exists(ev.eventdatavalues,
                        '$.* ? (!exists(@.createdByUserInfo) || !exists(@.lastUpdatedByUserInfo))'))
                or (ev.createdbyuserinfo is null
                    and jsonb_path_exists(ev.eventdatavalues,
                        '$.* ? (exists(@.createdByUserInfo) && !exists(@.lastUpdatedByUserInfo))'))
            $sql$, t);
    end loop;
end;
$$;
