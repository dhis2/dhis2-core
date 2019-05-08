
-- Pie charts upgrade

-- Moves 'series' to 'filter', then 'category' to 'series' and sets 'category' 
-- to null for pie charts

-- Move 'series' to become a 'filter' when 'series' and 'category' are present

insert into chart_filters(chartid, filter, sort_order)
select c.chartid as chartid, c.series as filter, coalesce(((
  select max(sort_order) 
  from chart_filters cf
  where cf.chartid = c.chartid) + 1), 0) as sort_order
from chart c 
where c.type = 'PIE'
and c.category is not null
and c.series is not null;

-- Set 'series' to null temporarily

update chart set series = null
where type = 'PIE'
and category is not null
and series is not null;

-- Move 'category' to 'series' when 'category' is present

update chart set series = category
where type = 'PIE'
and category is not null;

-- Set 'category' to null

update chart set category = null
where type = 'PIE'
and category is not null;

