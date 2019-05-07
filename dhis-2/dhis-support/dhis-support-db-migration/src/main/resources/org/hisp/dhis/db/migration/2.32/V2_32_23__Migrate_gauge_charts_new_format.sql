
-- Gauge charts upgrade

-- Move 'category' to become a 'filter' when 'category' is present

insert into chart_filters(chartid, filter, sort_order)
select c.chartid as chartid, c.category as filter, coalesce(((
  select max(sort_order) 
  from chart_filters cf
  where cf.chartid = c.chartid) + 1), 0) as sort_order
from chart c 
where c.type = 'GAUGE'
and c.category is not null;

-- Set 'category' to null

update chart set category = null
where type = 'GAUGE'
and category is not null;

