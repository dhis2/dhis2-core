
-- Create table "mapview_categorydimensions"

create table public.mapview_categorydimensions (
	mapviewid int8 not null,
	categorydimensionid int4 not null,
	sort_order int4 not null,
	constraint mapview_categorydimensions_pkey primary key (mapviewid, sort_order),
	constraint fk_mapview_categorydimensions_categorydimensionid foreign key (categorydimensionid) references categorydimension(categorydimensionid),
	constraint fk_mapview_categorydimensions_mapviewid foreign key (mapviewid) references mapview(mapviewid)
);

-- Create table "mapview_orgunitgroupsetdimensions"

create table public.mapview_orgunitgroupsetdimensions (
	mapviewid int8 not null,
	sort_order int4 not null,
	orgunitgroupsetdimensionid int4 not null,
	constraint mapview_orgunitgroupsetdimensions_pkey primary key (mapviewid, sort_order),
	constraint fk_mapview_dimensions_orgunitgroupsetdimensionid foreign key (orgunitgroupsetdimensionid) references orgunitgroupsetdimension(orgunitgroupsetdimensionid),
	constraint fk_mapview_orgunitgroupsetdimensions_mapviewid foreign key (mapviewid) references mapview(mapviewid)
);

-- Create table "mapview_categoryoptiongroupsetdimensions"

CREATE TABLE public.mapview_categoryoptiongroupsetdimensions (
	mapviewid int8 NOT NULL,
	sort_order int4 NOT NULL,
	categoryoptiongroupsetdimensionid int4 NOT NULL,
	CONSTRAINT mapview_categoryoptiongroupsetdimensions_pkey PRIMARY KEY (mapviewid, sort_order),
	CONSTRAINT fk_mapview_catoptiongroupsetdimensions_mapviewid FOREIGN KEY (mapviewid) REFERENCES mapview(mapviewid),
	CONSTRAINT fk_mapview_dimensions_catoptiongroupsetdimensionid FOREIGN KEY (categoryoptiongroupsetdimensionid) REFERENCES categoryoptiongroupsetdimension(categoryoptiongroupsetdimensionid)
);

-- Create table "mapview_filters"

create table public.mapview_filters (
  mapviewid int8 not null,
  dimension varchar(255) null,
  sort_order int4 not null,
  constraint mapview_filters_pkey primary key (mapviewid,sort_order),
  constraint fk_mapview_filters_mapviewid foreign key (mapviewid) references public.mapview(mapviewid)
);

-- Insert pe as filter for map views with fixed or relative periods in table "mapview_filters"

insert into mapview_filters (mapviewid, dimension, sort_order)
select mv.mapviewid as "mapviewid", 'pe' as "dimension", 0 as "sort_order"
from mapview mv 
where exists (
  select mapviewid 
  from mapview_periods
  where mapviewid = mv.mapviewid
  and periodid is not null)
or mv.relativeperiodsid is not null;
