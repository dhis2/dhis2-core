-- Review cumulative_values x cumulative

-- Set any possible empty or null NAME to an empty space ' '.
UPDATE public.reporttable SET name = ' ' WHERE COALESCE(name, '') = '' OR name IS NULL;

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
