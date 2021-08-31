-- DHIS2-11047
-- This script will remove all tables related to Chart and ReportTable.
-- Those entities are being entirely removed from the codebase.
-- Some statistics data needs are being move into visualization.

-- Drop ReportTable and Chart columns from dashboarditem table.
ALTER TABLE dashboarditem
    DROP COLUMN IF EXISTS chartid;

ALTER TABLE dashboarditem
    DROP COLUMN IF EXISTS reporttable;

ALTER TABLE report
    DROP COLUMN IF EXISTS reporttableid;

-- Drop all Design tables
DROP TABLE IF EXISTS designreporttables;
DROP TABLE IF EXISTS designcharts;
DROP TABLE IF EXISTS design;

-- Drop all Chart tables
DROP TABLE IF EXISTS chart_yearlyseries;
DROP TABLE IF EXISTS chart_seriesitems;
DROP TABLE IF EXISTS chart_periods;
DROP TABLE IF EXISTS chart_orgunitlevels;
DROP TABLE IF EXISTS chart_orgunitgroupsetdimensions;
DROP TABLE IF EXISTS chart_organisationunits;
DROP TABLE IF EXISTS chart_itemorgunitgroups;
DROP TABLE IF EXISTS chart_filters;
DROP TABLE IF EXISTS chart_dataelementgroupsetdimensions;
DROP TABLE IF EXISTS chart_datadimensionitems;
DROP TABLE IF EXISTS chart_categoryoptiongroupsetdimensions;
DROP TABLE IF EXISTS chart_categorydimensions;
DROP TABLE IF EXISTS chart;

-- Drop all ReportTable tables
DROP TABLE IF EXISTS reporttable;
DROP TABLE IF EXISTS reporttable_categorydimensions;
DROP TABLE IF EXISTS reporttable_categoryoptiongroupsetdimensions;
DROP TABLE IF EXISTS reporttable_columns;
DROP TABLE IF EXISTS reporttable_datadimensionitems;
DROP TABLE IF EXISTS reporttable_dataelementgroupsetdimensions;
DROP TABLE IF EXISTS reporttable_filters;
DROP TABLE IF EXISTS reporttable_itemorgunitgroups;
DROP TABLE IF EXISTS reporttable_organisationunits;
DROP TABLE IF EXISTS reporttable_orgunitgroupsetdimensions;
DROP TABLE IF EXISTS reporttable_orgunitlevels;
DROP TABLE IF EXISTS reporttable_periods;
DROP TABLE IF EXISTS reporttable_rows;

UPDATE datastatisticsevent
SET eventtype = 'VISUALIZATION_VIEW'
WHERE eventtype = 'REPORT_TABLE_VIEW' OR eventtype = 'CHART_VIEW';

UPDATE datastatistics
SET visualizationviews = reporttableviews + chartviews;

ALTER TABLE datastatistics
    DROP COLUMN IF EXISTS reporttableviews;

ALTER TABLE datastatistics
    DROP COLUMN IF EXISTS chartviews;

UPDATE datastatistics
SET visualizations = reporttables + charts;

ALTER TABLE datastatistics
    DROP COLUMN IF EXISTS reporttables;

ALTER TABLE datastatistics
    DROP COLUMN IF EXISTS charts;
