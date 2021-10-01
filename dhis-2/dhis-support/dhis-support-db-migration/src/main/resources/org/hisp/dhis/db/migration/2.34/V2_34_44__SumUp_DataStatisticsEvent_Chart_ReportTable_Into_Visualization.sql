-- This duplicates all rows that contains CHART_VIEW and REPORT_TABLE_VIEW.
-- They will be duplicated using VISUALIZATION_VIEW as the event type.
-- Used the default Hibernate sequence (native).
-- See JIRA DHIS2-11693

-- Populate the visualizationviews based on existing metrics for report table and charts.
UPDATE datastatistics
SET visualizationviews = reporttableviews + chartviews;

UPDATE datastatistics
SET visualizations = reporttables + charts;
