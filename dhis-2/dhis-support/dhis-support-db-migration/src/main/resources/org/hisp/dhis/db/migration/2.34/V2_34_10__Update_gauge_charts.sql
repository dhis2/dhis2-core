-- This script upgrades the Gauge charts so that:

-- all series != 'dx' are on filter
-- data (dx) dimension is set on series
-- all category (rows) are set to empty


-- Move series != 'dx' to the chart_filter table

insert into chart_filters(chartid, filter, sort_order)
select c.chartid as chartid, c.series as filter, coalesce(((
  select max(sort_order)
  from chart_filters cf
  where cf.chartid = c.chartid) + 1), 0) as sort_order
from chart c
where c.type = 'GAUGE'
and c.series <> 'dx';

-- Set 'dx' into series

update chart set series = 'dx'
where type = 'GAUGE';

-- Set all 'category' to empty

update chart set category = ''
where type = 'GAUGE';
