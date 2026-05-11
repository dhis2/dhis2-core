-- Extend the singleevent occurreddate index (V2_43_50) with eventid as
-- tie-breaker for deterministic pagination on the default order.
drop index if exists in_singleevent_programstageid_occurreddate;
create index in_singleevent_programstageid_occurreddate
    on singleevent (programstageid, occurreddate, eventid);
