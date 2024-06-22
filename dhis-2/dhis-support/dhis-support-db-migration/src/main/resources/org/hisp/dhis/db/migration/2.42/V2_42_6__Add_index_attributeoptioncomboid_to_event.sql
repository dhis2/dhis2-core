
-- Uses function 'dhis2_create_index_if_not_exists'
-- Adds a btree index on column 'attributeoptioncomboid' on table 'event'

select dhis2_create_index_if_not_exists(
  'in_event_attributeoptioncomboid',
  'create index in_event_attributeoptioncomboid on event using btree(attributeoptioncomboid)'
);
