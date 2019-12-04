
-- This script migrates:
-- 1) copy data from chart table into visualization;
-- 2) copy data from reporttable table into visualization;
-- 3) copy reporttable column into the visualizationid column in the dashboarditems table;
-- 4) copy chartid column into the visualizationid column in the dashboarditems table;
-- 5) copy the reporttableid column into the visualizationid column in the report table;
-- 6) copy data from series table into axis table;
-- 7) copy data from chart_seriesitems table into visualization_axis table;
-- 8) set default values for NULL (Java primitive) columns.


-- Set any possible empty or null NAME to an empty space ' ', as name is mandatory.
UPDATE public.chart SET name = ' ' WHERE COALESCE(name, '') = '' OR name IS NULL;

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

-- TODO: Maikel, DOUBLE CHECK THIS ONE!
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


-- Set any possible empty or null NAME to an empty space ' '.
UPDATE public.reporttable SET name = ' ' WHERE COALESCE(name, '') = '' OR name IS NULL;

-- Migrate all reporttable table into the visualization table.
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
  categorycomboid,
  sortorder,
  toplimit,
  userid,
  userorgunittype,
  publicaccess,
  displaydensity,
  fontsize,
  relativeperiodsid,
  numberformatting,
  digitgroupseparator,
  legendsetid,
  legenddisplaystyle,
  legenddisplaystrategy,
  aggregationtype,
  numbertype,
  measurecriteria,
  cumulative,
  regression,
  externalaccess,
  userorganisationunit,
  userorganisationunitchildren,
  userorganisationunitgrandchildren,
  paramreportingperiod,
  paramorganisationunit,
  paramparentorganisationunit,
  paramgrandparentorganisationunit,
  rowtotals,
  coltotals,
  subtotals,
  rowsubtotals,
  colsubtotals,
  completedonly,
  skiprounding,
  showdimensionlabels,
  hidetitle,
  hidesubtitle,
  hideemptycolumns,
  hideemptyrows,
  showhierarchy,
  lastupdatedby,
  lastupdated,
  favorites,
  subscribers,
  translations
)
SELECT
  reporttableid,
  uid,
  name,
  'PIVOT_TABLE',
  code,
  title,
  subtitle,
  description,
  created,
  startdate,
  enddate,
  categorycomboid,
  sortorder,
  toplimit,
  userid,
  userorgunittype,
  publicaccess,
  displaydensity,
  fontsize,
  relativeperiodsid,
  numberformatting,
  digitgroupseparator,
  legendsetid,
  legenddisplaystyle,
  legenddisplaystrategy,
  aggregationtype,
  numbertype,
  measurecriteria,
  cumulative,
  regression,
  externalaccess,
  userorganisationunit,
  userorganisationunitchildren,
  userorganisationunitgrandchildren,
  paramreportingmonth,
  paramorganisationunit,
  paramparentorganisationunit,
  paramgrandparentorganisationunit,
  rowtotals,
  coltotals,
  subtotals,
  rowsubtotals,
  colsubtotals,
  completedonly,
  skiprounding,
  showdimensionlabels,
  hidetitle,
  hidesubtitle,
  hideemptycolumns,
  hideemptyrows,
  showhierarchy,
  lastupdatedby,
  lastupdated,
  favorites,
  subscribers,
  translations
FROM public.reporttable;

-- Migrate all data from reporttable_categorydimensions into visualization_categorydimensions.
INSERT INTO public.visualization_categorydimensions
(
  visualizationid,
  categorydimensionid,
  sort_order
)
SELECT
  reporttableid,
  categorydimensionid,
  sort_order
FROM public.reporttable_categorydimensions;

-- Migrate all data from reporttable_categoryoptiongroupsetdimensions into visualization_categoryoptiongroupsetdimensions.
INSERT INTO public.visualization_categoryoptiongroupsetdimensions
(
  visualizationid,
  categoryoptiongroupsetdimensionid,
  sort_order
)
SELECT
  reporttableid,
  categoryoptiongroupsetdimensionid,
  sort_order
FROM public.reporttable_categoryoptiongroupsetdimensions;

-- Migrate all data from reporttable_datadimensionitems into visualization_datadimensionitems.
INSERT INTO public.visualization_datadimensionitems
(
  visualizationid,
  datadimensionitemid,
  sort_order
)
SELECT
  reporttableid,
  datadimensionitemid,
  sort_order
FROM public.reporttable_datadimensionitems;

-- Migrate all data from reporttable_dataelementgroupsetdimensions into visualization_dataelementgroupsetdimensions.
INSERT INTO public.visualization_dataelementgroupsetdimensions
(
  visualizationid,
  dataelementgroupsetdimensionid,
  sort_order
)
SELECT
  reporttableid,
  dataelementgroupsetdimensionid,
  sort_order
FROM public.reporttable_dataelementgroupsetdimensions;

-- Migrate all data from reporttable_filters into visualization_filters.
INSERT INTO public.visualization_filters
(
  visualizationid,
  dimension,
  sort_order
)
SELECT
  reporttableid,
  dimension,
  sort_order
FROM public.reporttable_filters;

-- Migrate all data from reporttable_itemorgunitgroups into visualization_itemorgunitgroups.
INSERT INTO public.visualization_itemorgunitgroups
(
  visualizationid,
  orgunitgroupid,
  sort_order
)
SELECT
  reporttableid,
  orgunitgroupid,
  sort_order
FROM public.reporttable_itemorgunitgroups;

-- Migrate all data from reporttable_organisationunits into visualization_organisationunits.
INSERT INTO public.visualization_organisationunits
(
  visualizationid,
  organisationunitid,
  sort_order
)
SELECT
  reporttableid,
  organisationunitid,
  sort_order
FROM public.reporttable_organisationunits;

-- Migrate all data from reporttable_orgunitgroupsetdimensions into visualization_orgunitgroupsetdimensions.
INSERT INTO public.visualization_orgunitgroupsetdimensions
(
  visualizationid,
  orgunitgroupsetdimensionid,
  sort_order
)
SELECT
  reporttableid,
  orgunitgroupsetdimensionid,
  sort_order
FROM public.reporttable_orgunitgroupsetdimensions;

-- Migrate all data from reporttable_orgunitlevels into visualization_orgunitlevels.
INSERT INTO public.visualization_orgunitlevels
(
  visualizationid,
  orgunitlevel,
  sort_order
)
SELECT
  reporttableid,
  orgunitlevel,
  sort_order
FROM public.reporttable_orgunitlevels;

-- Migrate all data from reporttable_periods into visualization_periods.
INSERT INTO public.visualization_periods
(
  visualizationid,
  periodid,
  sort_order
)
SELECT
  reporttableid,
  periodid,
  sort_order
FROM public.reporttable_periods;

-- Migrate all data from reporttableuseraccesses into visualization_useraccesses.
INSERT INTO public.visualization_useraccesses
(
  visualizationid,
  useraccessid
)
SELECT
  reporttableid,
  useraccessid
FROM public.reporttableuseraccesses;

-- Migrate all data from reporttableusergroupaccesses into visualization_usergroupaccesses.
INSERT INTO public.visualization_usergroupaccesses
(
  visualizationid,
  usergroupaccessid
)
SELECT
  reporttableid,
  usergroupaccessid
FROM public.reporttableusergroupaccesses;

-- Migrate the reporttable_columns table into visualization_columns.
INSERT INTO public.visualization_columns
(
  visualizationid,
  dimension,
  sort_order
)
SELECT
  reporttableid,
  dimension,
  sort_order
FROM public.reporttable_columns;

-- Migrate the reporttable_rows table into visualization_rows.
INSERT INTO public.visualization_rows
(
  visualizationid,
  dimension,
  sort_order
)
SELECT
  reporttableid,
  dimension,
  sort_order
FROM public.reporttable_rows;

-- Migrate the series table into the axis table.
INSERT INTO public.axis
(
  axisid,
  dimensionalitem,
  axis
)
SELECT
  seriesid,
  series,
  axis
FROM public.series;

-- Migrate the chart_seriesitems table into the visualization_axis table.
INSERT INTO public.visualization_axis
(
  visualizationid,
  sort_order,
  axisid
)
SELECT
  chartid,
  sort_order,
  seriesid
FROM public.chart_seriesitems;

-- Add boolean defaults for null columns to avoid Hibernate parse errors.
UPDATE public.visualization SET percentstackedvalues = FALSE WHERE percentstackedvalues is NULL;
UPDATE public.visualization SET nospacebetweencolumns = FALSE WHERE nospacebetweencolumns is NULL;
UPDATE public.visualization SET regression = FALSE WHERE regression is NULL;
UPDATE public.visualization SET externalaccess = FALSE WHERE externalaccess is NULL;
UPDATE public.visualization SET userorganisationunit = FALSE WHERE userorganisationunit is NULL;
UPDATE public.visualization SET userorganisationunitchildren = FALSE WHERE userorganisationunitchildren is NULL;
UPDATE public.visualization SET userorganisationunitgrandchildren = FALSE WHERE userorganisationunitgrandchildren is NULL;
UPDATE public.visualization SET paramreportingperiod = FALSE WHERE paramreportingperiod is NULL;
UPDATE public.visualization SET paramorganisationunit = FALSE WHERE paramorganisationunit is NULL;
UPDATE public.visualization SET paramparentorganisationunit = FALSE WHERE paramparentorganisationunit is NULL;
UPDATE public.visualization SET paramgrandparentorganisationunit = FALSE WHERE paramgrandparentorganisationunit is NULL;
UPDATE public.visualization SET rowtotals = FALSE WHERE rowtotals is NULL;
UPDATE public.visualization SET coltotals = FALSE WHERE coltotals is NULL;
UPDATE public.visualization SET subtotals = FALSE WHERE subtotals is NULL;
UPDATE public.visualization SET cumulative = FALSE WHERE cumulative is NULL;
UPDATE public.visualization SET rowsubtotals = FALSE WHERE rowsubtotals is NULL;
UPDATE public.visualization SET colsubtotals = FALSE WHERE colsubtotals is NULL;
UPDATE public.visualization SET completedonly = FALSE WHERE completedonly is NULL;
UPDATE public.visualization SET skiprounding = FALSE WHERE skiprounding is NULL;
UPDATE public.visualization SET showdimensionlabels = FALSE WHERE showdimensionlabels is NULL;
UPDATE public.visualization SET hidetitle = FALSE WHERE hidetitle is NULL;
UPDATE public.visualization SET hidesubtitle = FALSE WHERE hidesubtitle is NULL;
UPDATE public.visualization SET hidelegend = FALSE WHERE hidelegend is NULL;
UPDATE public.visualization SET hideemptycolumns = FALSE WHERE hideemptycolumns is NULL;
UPDATE public.visualization SET hideemptyrows = FALSE WHERE hideemptyrows is NULL;
UPDATE public.visualization SET showhierarchy = FALSE WHERE showhierarchy is NULL;
UPDATE public.visualization SET showdata = FALSE WHERE showdata is NULL;


-- Add int defaults for null columns to avoid Hibernate parse errors.
UPDATE public.visualization SET toplimit = 0 WHERE toplimit is NULL;
UPDATE public.visualization SET rangeaxissteps = 0 WHERE rangeaxissteps is NULL;
UPDATE public.visualization SET rangeaxisdecimals = 0 WHERE rangeaxisdecimals is NULL;


-- Add default public access for null columns.
UPDATE public.visualization SET publicaccess = '--------' WHERE COALESCE(publicaccess, '') = '' OR publicaccess IS NULL;


-- Update the column visualizationid of the public.dashboarditem table
-- so it receives all the chartid's.
UPDATE public.dashboarditem SET visualizationid = chartid
WHERE chartid IS NOT NULL;


-- Update the column visualizationid of the public.dashboarditem table
-- so it receives all the reporttableid's.
UPDATE public.dashboarditem SET visualizationid = reporttable
WHERE reporttable IS NOT NULL;


-- Update the column visualizationid of the public.report table
-- so it receives all the reporttableid's.
UPDATE public.report SET visualizationid = reporttableid;
