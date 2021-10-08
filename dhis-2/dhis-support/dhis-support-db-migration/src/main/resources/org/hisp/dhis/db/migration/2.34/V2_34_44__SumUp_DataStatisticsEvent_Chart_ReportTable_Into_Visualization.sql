-- This will sum-up CHART_VIEW and REPORT_TABLE_VIEW into VISUALIZATION_VIEW.
-- See JIRA DHIS2-11693

-- Populate the visualizationviews based on existing metrics for report table and charts.
UPDATE datastatistics
SET visualizationviews = reporttableviews + chartviews WHERE visualizationviews IS NULL;

UPDATE datastatistics
SET visualizations = reporttables + charts WHERE visualizations IS NULL;
