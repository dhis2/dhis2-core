-- chart_seriesitems is not migrated -- ASK ABOUT IT TO THE UI TEAM

-- Set any possible empty or null NAME to an empty space ' '.
UPDATE public.chart SET name = ' ' WHERE COALESCE(name, '') = '' OR name IS NULL;

-- Set any possible empty or null TYPE to a default 'COLUMN'.
UPDATE public.chart SET type = 'COLUMN' WHERE COALESCE(type, '') = '' OR type IS NULL;

-- Migrate all chart table into the visualization table.
INSERT INTO public.visualization
(
  visualizationid,
  uid,
  name,
  type,
  code,
  title,
  subtitle,
  description,
  created,
  startdate,
  enddate,
  sortorder,
  userid,
  userorgunittype,
  publicaccess,
  relativeperiodsid,
  legendsetid,
  legenddisplaystrategy,
  aggregationtype,
  regressiontype,
  targetlinevalue,
  targetlinelabel,
  rangeaxislabel,
  rangeaxismaxvalue,
  rangeaxissteps,
  rangeaxisdecimals,
  rangeaxisminvalue,
  domainaxislabel,
  baselinevalue,
  baselinelabel,
  colorsetid,
  hideemptyrowitems,
  percentstackedvalues,
  cumulative,
  nospacebetweencolumns,
  externalaccess,
  userorganisationunit,
  userorganisationunitchildren,
  userorganisationunitgrandchildren,
  completedonly,
  hidetitle,
  hidesubtitle,
  hidelegend,
  showdata,
  lastupdatedby,
  lastupdated,
  favorites,
  subscribers,
  translations
)
SELECT
  chartid,
  uid,
  name,
  type,
  code,
  title,
  subtitle,
  description,
  created,
  startdate,
  enddate,
  sortorder,
  userid,
  userorgunittype,
  publicaccess,
  relativeperiodsid,
  legendsetid,
  legenddisplaystrategy,
  aggregationtype,
  regressiontype,
  targetlinevalue,
  targetlinelabel,
  rangeaxislabel,
  rangeaxismaxvalue,
  rangeaxissteps,
  rangeaxisdecimals,
  rangeaxisminvalue,
  domainaxislabel,
  baselinevalue,
  baselinelabel,
  colorsetid,
  hideemptyrowitems,
  percentstackedvalues,
  cumulativevalues,
  nospacebetweencolumns,
  externalaccess,
  userorganisationunit,
  userorganisationunitchildren,
  userorganisationunitgrandchildren,
  completedonly,
  hidetitle,
  hidesubtitle,
  hidelegend,
  showdata,
  lastupdatedby,
  lastupdated,
  favorites,
  subscribers,
  translations
FROM public.chart;

-- Migrate all data from chart_categorydimensions into visualization_categorydimensions.
INSERT INTO public.visualization_categorydimensions
(
  visualizationid,
  categorydimensionid,
  sort_order
)
SELECT
  chartid,
  categorydimensionid,
  sort_order
FROM public.chart_categorydimensions;

-- Migrate all data from chart_categorydimensions into visualization_categorydimensions.
INSERT INTO public.visualization_categoryoptiongroupsetdimensions
(
  visualizationid,
  categoryoptiongroupsetdimensionid,
  sort_order
)
SELECT
  chart,
  categoryoptiongroupsetdimensionid,
  sort_order
FROM public.chart_categoryoptiongroupsetdimensions;

-- Migrate all data from chart_datadimensionitems into visualization_datadimensionitems.
INSERT INTO public.visualization_datadimensionitems
(
  visualizationid,
  datadimensionitemid,
  sort_order
)
SELECT
  chartid,
  datadimensionitemid,
  sort_order
FROM public.chart_datadimensionitems;

-- Migrate all data from chart_dataelementgroupsetdimensions into visualization_dataelementgroupsetdimensions.
INSERT INTO public.visualization_dataelementgroupsetdimensions
(
  visualizationid,
  dataelementgroupsetdimensionid,
  sort_order
)
SELECT
  chartid,
  dataelementgroupsetdimensionid,
  sort_order
FROM public.chart_dataelementgroupsetdimensions;

-- Migrate all data from chart_filters into visualization_filters.
INSERT INTO public.visualization_filters
(
  visualizationid,
  dimension,
  sort_order
)
SELECT
  chartid,
  filter,
  sort_order
FROM public.chart_filters;

-- Migrate all data from chart_itemorgunitgroups into visualization_itemorgunitgroups.
INSERT INTO public.visualization_itemorgunitgroups
(
  visualizationid,
  orgunitgroupid,
  sort_order
)
SELECT
  chartid,
  orgunitgroupid,
  sort_order
FROM public.chart_itemorgunitgroups;

-- Migrate all data from chart_organisationunits into visualization_organisationunits.
INSERT INTO public.visualization_organisationunits
(
  visualizationid,
  organisationunitid,
  sort_order
)
SELECT
  chartid,
  organisationunitid,
  sort_order
FROM public.chart_organisationunits;

-- Migrate all data from chart_orgunitgroupsetdimensions into visualization_orgunitgroupsetdimensions.
INSERT INTO public.visualization_orgunitgroupsetdimensions
(
  visualizationid,
  orgunitgroupsetdimensionid,
  sort_order
)
SELECT
  chartid,
  orgunitgroupsetdimensionid,
  sort_order
FROM public.chart_orgunitgroupsetdimensions;

-- Migrate all data from chart_orgunitlevels into visualization_orgunitlevels.
INSERT INTO public.visualization_orgunitlevels
(
  visualizationid,
  orgunitlevel,
  sort_order
)
SELECT
  chartid,
  orgunitlevel,
  sort_order
FROM public.chart_orgunitlevels;

-- Migrate all data from chart_periods into visualization_periods.
INSERT INTO public.visualization_periods
(
  visualizationid,
  periodid,
  sort_order
)
SELECT
  chartid,
  periodid,
  sort_order
FROM public.chart_periods;

-- Migrate all data from chart_yearlyseries into visualization_yearlyseries.
INSERT INTO public.visualization_yearlyseries
(
  visualizationid,
  yearlyseries,
  sort_order
)
SELECT
  chartid,
  yearlyseries,
  sort_order
FROM public.chart_yearlyseries;

-- Migrate all data from chartuseraccesses into visualization_useraccesses.
INSERT INTO public.visualization_useraccesses
(
  visualizationid,
  useraccessid
)
SELECT
  chartid,
  useraccessid
FROM public.chartuseraccesses;

-- Migrate all data from chartusergroupaccesses into visualization_usergroupaccesses.
INSERT INTO public.visualization_usergroupaccesses
(
  visualizationid,
  usergroupaccessid
)
SELECT
  chartid,
  usergroupaccessid
FROM public.chartusergroupaccesses;

-- DOUBLE CHECK THIS ONE!
-- Migrate the 'series' column from the chart table into visualization_columns.
INSERT INTO public.visualization_columns
(
  visualizationid,
  dimension,
  sort_order
)
SELECT
  chartid,
  series,
  sortorder
FROM public.chart;

-- DOUBLE CHECK THIS ONE!
-- Migrate the 'category' column from the chart table into visualization_rows.
INSERT INTO public.visualization_rows
(
  visualizationid,
  dimension,
  sort_order
)
SELECT
  chartid,
  category,
  sortorder
FROM public.chart;
