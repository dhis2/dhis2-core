-- This script responsible for migrating required data to Visualization.
-- See Feature DHIS2-7946

-- It migrates:
-- 1) copy data from chart table into visualization;
-- 2) copy data from reporttable table into visualization;
-- 3) copy reporttable column into the visualizationid column in the dashboarditems table;
-- 4) copy chartid column into the visualizationid column in the dashboarditems table;
-- 5) copy the reporttableid column into the visualizationid column in the report table;
-- 6) copy data from series table into axis table;
-- 7) copy data from chart_seriesitems table into visualization_axis table;
-- 8) set default values for NULL (Java primitive) columns.
-- 9) update the column visualizationid of the dashboarditem table, so it receives all the chartid's.
-- 10) update the column visualizationid of the dashboarditem table, so it receives all the reporttableid's.
-- 11) update the column visualizationid of the report table, so it receives all the reporttableid's.


-- Set any possible empty or null NAME to an empty space ' ', as name is mandatory.
UPDATE chart SET name = ' ' WHERE COALESCE(name, '') = '' OR name IS NULL;

-- Migrate all chart table into the visualization table.
INSERT INTO visualization
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
FROM chart;

-- Migrate all data from chart_categorydimensions into visualization_categorydimensions.
INSERT INTO visualization_categorydimensions
(
  visualizationid,
  categorydimensionid,
  sort_order
)
SELECT
  chartid,
  categorydimensionid,
  sort_order
FROM chart_categorydimensions;

-- Migrate all data from chart_categorydimensions into visualization_categorydimensions.
INSERT INTO visualization_categoryoptiongroupsetdimensions
(
  visualizationid,
  categoryoptiongroupsetdimensionid,
  sort_order
)
SELECT
  chart,
  categoryoptiongroupsetdimensionid,
  sort_order
FROM chart_categoryoptiongroupsetdimensions;

-- Migrate all data from chart_datadimensionitems into visualization_datadimensionitems.
INSERT INTO visualization_datadimensionitems
(
  visualizationid,
  datadimensionitemid,
  sort_order
)
SELECT
  chartid,
  datadimensionitemid,
  sort_order
FROM chart_datadimensionitems;

-- Migrate all data from chart_dataelementgroupsetdimensions into visualization_dataelementgroupsetdimensions.
INSERT INTO visualization_dataelementgroupsetdimensions
(
  visualizationid,
  dataelementgroupsetdimensionid,
  sort_order
)
SELECT
  chartid,
  dataelementgroupsetdimensionid,
  sort_order
FROM chart_dataelementgroupsetdimensions;

-- Migrate all data from chart_filters into visualization_filters.
INSERT INTO visualization_filters
(
  visualizationid,
  dimension,
  sort_order
)
SELECT
  chartid,
  filter,
  sort_order
FROM chart_filters;

-- Migrate all data from chart_itemorgunitgroups into visualization_itemorgunitgroups.
INSERT INTO visualization_itemorgunitgroups
(
  visualizationid,
  orgunitgroupid,
  sort_order
)
SELECT
  chartid,
  orgunitgroupid,
  sort_order
FROM chart_itemorgunitgroups;

-- Migrate all data from chart_organisationunits into visualization_organisationunits.
INSERT INTO visualization_organisationunits
(
  visualizationid,
  organisationunitid,
  sort_order
)
SELECT
  chartid,
  organisationunitid,
  sort_order
FROM chart_organisationunits;

-- Migrate all data from chart_orgunitgroupsetdimensions into visualization_orgunitgroupsetdimensions.
INSERT INTO visualization_orgunitgroupsetdimensions
(
  visualizationid,
  orgunitgroupsetdimensionid,
  sort_order
)
SELECT
  chartid,
  orgunitgroupsetdimensionid,
  sort_order
FROM chart_orgunitgroupsetdimensions;

-- Migrate all data from chart_orgunitlevels into visualization_orgunitlevels.
INSERT INTO visualization_orgunitlevels
(
  visualizationid,
  orgunitlevel,
  sort_order
)
SELECT
  chartid,
  orgunitlevel,
  sort_order
FROM chart_orgunitlevels;

-- Migrate all data from chart_periods into visualization_periods.
INSERT INTO visualization_periods
(
  visualizationid,
  periodid,
  sort_order
)
SELECT
  chartid,
  periodid,
  sort_order
FROM chart_periods;

-- Migrate all data from chart_yearlyseries into visualization_yearlyseries.
INSERT INTO visualization_yearlyseries
(
  visualizationid,
  yearlyseries,
  sort_order
)
SELECT
  chartid,
  yearlyseries,
  sort_order
FROM chart_yearlyseries;

-- Migrate all data from chartuseraccesses into visualization_useraccesses.
INSERT INTO visualization_useraccesses
(
  visualizationid,
  useraccessid
)
SELECT
  chartid,
  useraccessid
FROM chartuseraccesses;

-- Migrate all data from chartusergroupaccesses into visualization_usergroupaccesses.
INSERT INTO visualization_usergroupaccesses
(
  visualizationid,
  usergroupaccessid
)
SELECT
  chartid,
  usergroupaccessid
FROM chartusergroupaccesses;

-- DOUBLE CHECK THIS ONE!
-- Migrate the 'series' column from the chart table into visualization_columns.
INSERT INTO visualization_columns
(
  visualizationid,
  dimension,
  sort_order
)
SELECT
  chartid,
  series,
  sortorder
FROM chart;

-- TODO: Maikel, DOUBLE CHECK THIS ONE!
-- Migrate the 'category' column from the chart table into visualization_rows.
INSERT INTO visualization_rows
(
  visualizationid,
  dimension,
  sort_order
)
SELECT
  chartid,
  category,
  sortorder
FROM chart;


-- Set any possible empty or null NAME to an empty space ' '.
UPDATE reporttable SET name = ' ' WHERE COALESCE(name, '') = '' OR name IS NULL;

-- Migrate all reporttable table into the visualization table.
INSERT INTO visualization
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
  toplimit,
  userid,
  userorgunittype,
  publicaccess,
  displaydensity,
  fontsize,
  relativeperiodsid,
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
  sortorder,
  toplimit,
  userid,
  userorgunittype,
  publicaccess,
  displaydensity,
  fontsize,
  relativeperiodsid,
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
FROM reporttable;

-- Migrate all data from reporttable_categorydimensions into visualization_categorydimensions.
INSERT INTO visualization_categorydimensions
(
  visualizationid,
  categorydimensionid,
  sort_order
)
SELECT
  reporttableid,
  categorydimensionid,
  sort_order
FROM reporttable_categorydimensions;

-- Migrate all data from reporttable_categoryoptiongroupsetdimensions into visualization_categoryoptiongroupsetdimensions.
INSERT INTO visualization_categoryoptiongroupsetdimensions
(
  visualizationid,
  categoryoptiongroupsetdimensionid,
  sort_order
)
SELECT
  reporttableid,
  categoryoptiongroupsetdimensionid,
  sort_order
FROM reporttable_categoryoptiongroupsetdimensions;

-- Migrate all data from reporttable_datadimensionitems into visualization_datadimensionitems.
INSERT INTO visualization_datadimensionitems
(
  visualizationid,
  datadimensionitemid,
  sort_order
)
SELECT
  reporttableid,
  datadimensionitemid,
  sort_order
FROM reporttable_datadimensionitems;

-- Migrate all data from reporttable_dataelementgroupsetdimensions into visualization_dataelementgroupsetdimensions.
INSERT INTO visualization_dataelementgroupsetdimensions
(
  visualizationid,
  dataelementgroupsetdimensionid,
  sort_order
)
SELECT
  reporttableid,
  dataelementgroupsetdimensionid,
  sort_order
FROM reporttable_dataelementgroupsetdimensions;

-- Migrate all data from reporttable_filters into visualization_filters.
INSERT INTO visualization_filters
(
  visualizationid,
  dimension,
  sort_order
)
SELECT
  reporttableid,
  dimension,
  sort_order
FROM reporttable_filters;

-- Migrate all data from reporttable_itemorgunitgroups into visualization_itemorgunitgroups.
INSERT INTO visualization_itemorgunitgroups
(
  visualizationid,
  orgunitgroupid,
  sort_order
)
SELECT
  reporttableid,
  orgunitgroupid,
  sort_order
FROM reporttable_itemorgunitgroups;

-- Migrate all data from reporttable_organisationunits into visualization_organisationunits.
INSERT INTO visualization_organisationunits
(
  visualizationid,
  organisationunitid,
  sort_order
)
SELECT
  reporttableid,
  organisationunitid,
  sort_order
FROM reporttable_organisationunits;

-- Migrate all data from reporttable_orgunitgroupsetdimensions into visualization_orgunitgroupsetdimensions.
INSERT INTO visualization_orgunitgroupsetdimensions
(
  visualizationid,
  orgunitgroupsetdimensionid,
  sort_order
)
SELECT
  reporttableid,
  orgunitgroupsetdimensionid,
  sort_order
FROM reporttable_orgunitgroupsetdimensions;

-- Migrate all data from reporttable_orgunitlevels into visualization_orgunitlevels.
INSERT INTO visualization_orgunitlevels
(
  visualizationid,
  orgunitlevel,
  sort_order
)
SELECT
  reporttableid,
  orgunitlevel,
  sort_order
FROM reporttable_orgunitlevels;

-- Migrate all data from reporttable_periods into visualization_periods.
INSERT INTO visualization_periods
(
  visualizationid,
  periodid,
  sort_order
)
SELECT
  reporttableid,
  periodid,
  sort_order
FROM reporttable_periods;

-- Migrate all data from reporttableuseraccesses into visualization_useraccesses.
INSERT INTO visualization_useraccesses
(
  visualizationid,
  useraccessid
)
SELECT
  reporttableid,
  useraccessid
FROM reporttableuseraccesses;

-- Migrate all data from reporttableusergroupaccesses into visualization_usergroupaccesses.
INSERT INTO visualization_usergroupaccesses
(
  visualizationid,
  usergroupaccessid
)
SELECT
  reporttableid,
  usergroupaccessid
FROM reporttableusergroupaccesses;

-- Migrate the reporttable_columns table into visualization_columns.
INSERT INTO visualization_columns
(
  visualizationid,
  dimension,
  sort_order
)
SELECT
  reporttableid,
  dimension,
  sort_order
FROM reporttable_columns;

-- Migrate the reporttable_rows table into visualization_rows.
INSERT INTO visualization_rows
(
  visualizationid,
  dimension,
  sort_order
)
SELECT
  reporttableid,
  dimension,
  sort_order
FROM reporttable_rows;

-- Migrate the series table into the axis table.
INSERT INTO axis
(
  axisid,
  dimensionalitem,
  axis
)
SELECT
  seriesid,
  series,
  axis
FROM series;

-- Migrate the chart_seriesitems table into the visualization_axis table.
INSERT INTO visualization_axis
(
  visualizationid,
  sort_order,
  axisid
)
SELECT
  chartid,
  sort_order,
  seriesid
FROM chart_seriesitems;

-- Add boolean defaults for null columns to avoid Hibernate parse errors.
UPDATE visualization SET percentstackedvalues = FALSE WHERE percentstackedvalues is NULL;
UPDATE visualization SET nospacebetweencolumns = FALSE WHERE nospacebetweencolumns is NULL;
UPDATE visualization SET regression = FALSE WHERE regression is NULL;
UPDATE visualization SET externalaccess = FALSE WHERE externalaccess is NULL;
UPDATE visualization SET userorganisationunit = FALSE WHERE userorganisationunit is NULL;
UPDATE visualization SET userorganisationunitchildren = FALSE WHERE userorganisationunitchildren is NULL;
UPDATE visualization SET userorganisationunitgrandchildren = FALSE WHERE userorganisationunitgrandchildren is NULL;
UPDATE visualization SET paramreportingperiod = FALSE WHERE paramreportingperiod is NULL;
UPDATE visualization SET paramorganisationunit = FALSE WHERE paramorganisationunit is NULL;
UPDATE visualization SET paramparentorganisationunit = FALSE WHERE paramparentorganisationunit is NULL;
UPDATE visualization SET paramgrandparentorganisationunit = FALSE WHERE paramgrandparentorganisationunit is NULL;
UPDATE visualization SET rowtotals = FALSE WHERE rowtotals is NULL;
UPDATE visualization SET coltotals = FALSE WHERE coltotals is NULL;
UPDATE visualization SET cumulative = FALSE WHERE cumulative is NULL;
UPDATE visualization SET rowsubtotals = FALSE WHERE rowsubtotals is NULL;
UPDATE visualization SET colsubtotals = FALSE WHERE colsubtotals is NULL;
UPDATE visualization SET completedonly = FALSE WHERE completedonly is NULL;
UPDATE visualization SET skiprounding = FALSE WHERE skiprounding is NULL;
UPDATE visualization SET showdimensionlabels = FALSE WHERE showdimensionlabels is NULL;
UPDATE visualization SET hidetitle = FALSE WHERE hidetitle is NULL;
UPDATE visualization SET hidesubtitle = FALSE WHERE hidesubtitle is NULL;
UPDATE visualization SET hidelegend = FALSE WHERE hidelegend is NULL;
UPDATE visualization SET hideemptycolumns = FALSE WHERE hideemptycolumns is NULL;
UPDATE visualization SET hideemptyrows = FALSE WHERE hideemptyrows is NULL;
UPDATE visualization SET showhierarchy = FALSE WHERE showhierarchy is NULL;
UPDATE visualization SET showdata = FALSE WHERE showdata is NULL;


-- Add int defaults for null columns to avoid Hibernate parse errors.
UPDATE visualization SET toplimit = 0 WHERE toplimit is NULL;
UPDATE visualization SET rangeaxissteps = 0 WHERE rangeaxissteps is NULL;
UPDATE visualization SET rangeaxisdecimals = 0 WHERE rangeaxisdecimals is NULL;


-- Add default public access for null columns.
UPDATE visualization SET publicaccess = '--------' WHERE COALESCE(publicaccess, '') = '' OR publicaccess IS NULL;


-- Update the column visualizationid of the dashboarditem table
-- so it receives all the chartid's.
UPDATE dashboarditem SET visualizationid = chartid
WHERE chartid IS NOT NULL;


-- Update the column visualizationid of the dashboarditem table
-- so it receives all the reporttableid's.
UPDATE dashboarditem SET visualizationid = reporttable
WHERE reporttable IS NOT NULL;


-- Update the column visualizationid of the report table
-- so it receives all the reporttableid's.
UPDATE report SET visualizationid = reporttableid;
