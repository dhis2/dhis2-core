
-- Create mapview filters table

create table public.mapview_filters (
  mapviewid int8 not null,
  dimension varchar(255) null,
  sort_order int4 not null,
  constraint mapview_filters_pkey primary key (mapviewid,sort_order),
  constraint fk_mapview_filters_mapviewid foreign key (mapviewid) references public.mapview(mapviewid)
);

-- Insert pe as filter for map views with fixed or relative periods

insert into mapview_filters (mapviewid, dimension, sort_order)
select mv.mapviewid as "mapviewid", 'pe' as "dimension", 0 as "sort_order"
from mapview mv 
where exists (
  select mapviewid 
  from mapview_periods
  where mapviewid = mv.mapviewid
  and periodid is not null)
or mv.relativeperiodsid is not null;
