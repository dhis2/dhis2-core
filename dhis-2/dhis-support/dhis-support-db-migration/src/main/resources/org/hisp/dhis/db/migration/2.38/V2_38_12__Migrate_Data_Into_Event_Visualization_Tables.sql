-- This script relates to the issue https://jira.dhis2.org/browse/TECH-703
-- It migrates all necessary data from Event Chart and Event Report tables
-- into the new structure of Event Visualization tables.


-- Migrate eventchart table into eventvisualization table.
INSERT INTO eventvisualization
(
    -- merged
    eventvisualizationid,

    -- common
    uid,
    code,
    created,
    lastupdated,
    name,
    relativeperiodsid,
    userorganisationunit,
    userorganisationunitchildren,
    userorganisationunitgrandchildren,
    externalaccess,
    userid,
    publicaccess,
    programid,
    programstageid,
    startdate,
    enddate,
    sortorder,
    toplimit,
    outputtype,
    dataelementvaluedimensionid,
    attributevaluedimensionid,
    aggregationtype,
    collapsedatadimensions,
    hidenadata,
    completedonly,
    description,
    title,
    lastupdatedby,
    subtitle,
    hidetitle,
    hidesubtitle,
    programstatus,
    eventstatus,
    favorites,
    subscribers,
    timefield,
    translations,
    orgunitfield,
    userorgunittype,
    sharing,
    attributevalues,
    datatype,

    -- merged
    type,

    -- eventchart specifics
    showdata,
    rangeaxismaxvalue,
    rangeaxisminvalue,
    rangeaxissteps,
    rangeaxisdecimals,
    domainaxislabel,
    rangeaxislabel,
    hidelegend,
    targetlinevalue,
    targetlinelabel,
    baselinevalue,
    baselinelabel,
    regressiontype,
    hideemptyrowitems,
    percentstackedvalues,
    cumulativevalues,
    nospacebetweencolumns)
SELECT eventchartid,

       -- common
       uid,
       code,
       created,
       lastupdated,
       name,
       relativeperiodsid,
       userorganisationunit,
       userorganisationunitchildren,
       userorganisationunitgrandchildren,
       externalaccess,
       userid,
       publicaccess,
       programid,
       programstageid,
       startdate,
       enddate,
       sortorder,
       NULL,
       outputtype,
       dataelementvaluedimensionid,
       attributevaluedimensionid,
       aggregationtype,
       collapsedatadimensions,
       hidenadata,
       completedonly,
       description,
       title,
       lastupdatedby,
       subtitle,
       hidetitle,
       hidesubtitle,
       programstatus,
       eventstatus,
       favorites,
       subscribers,
       timefield,
       translations,
       orgunitfield,
       userorgunittype,
       sharing,
       attributevalues,
       NULL,

       -- merged
       type,

       -- eventchart specifics
       showdata,
       rangeaxismaxvalue,
       rangeaxisminvalue,
       rangeaxissteps,
       rangeaxisdecimals,
       domainaxislabel,
       rangeaxislabel,
       hidelegend,
       targetlinevalue,
       targetlinelabel,
       baselinevalue,
       baselinelabel,
       regressiontype,
       hideemptyrowitems,
       percentstackedvalues,
       cumulativevalues,
       nospacebetweencolumns
FROM eventchart;


-- Migrate eventreport table into eventvisualization table.
INSERT INTO eventvisualization
(
    -- merged
    eventvisualizationid,

    -- common
    uid,
    code,
    created,
    lastupdated,
    name,
    relativeperiodsid,
    userorganisationunit,
    userorganisationunitchildren,
    userorganisationunitgrandchildren,
    externalaccess,
    userid,
    publicaccess,
    programid,
    programstageid,
    startdate,
    enddate,
    sortorder,
    toplimit,
    outputtype,
    dataelementvaluedimensionid,
    attributevaluedimensionid,
    aggregationtype,
    collapsedatadimensions,
    hidenadata,
    completedonly,
    description,
    title,
    lastupdatedby,
    subtitle,
    hidetitle,
    hidesubtitle,
    programstatus,
    eventstatus,
    favorites,
    subscribers,
    timefield,
    translations,
    orgunitfield,
    userorgunittype,
    sharing,
    attributevalues,
    datatype,

    -- merged
    type,

    -- eventreport specifics
    hideemptyrows,
    digitgroupseparator,
    displaydensity,
    fontsize,
    showhierarchy,
    rowtotals,
    coltotals,
    showdimensionlabels,
    rowsubtotals,
    colsubtotals)
SELECT eventreportid,

       -- common
       uid,
       code,
       created,
       lastupdated,
       name,
       relativeperiodsid,
       userorganisationunit,
       userorganisationunitchildren,
       userorganisationunitgrandchildren,
       externalaccess,
       userid,
       publicaccess,
       programid,
       programstageid,
       startdate,
       enddate,
       sortorder,
       toplimit,
       outputtype,
       dataelementvaluedimensionid,
       attributevaluedimensionid,
       aggregationtype,
       collapsedatadimensions,
       hidenadata,
       completedonly,
       description,
       title,
       lastupdatedby,
       subtitle,
       hidetitle,
       hidesubtitle,
       programstatus,
       eventstatus,
       favorites,
       subscribers,
       timefield,
       translations,
       orgunitfield,
       userorgunittype,
       sharing,
       attributevalues,
       datatype,

       -- merged
       datatype,

       -- eventreport specifics
       hideemptyrows,
       digitgroupseparator,
       displaydensity,
       fontsize,
       showhierarchy,
       rowtotals,
       coltotals,
       showdimensionlabels,
       rowsubtotals,
       colsubtotals
FROM eventreport;


-- Update type for EVENTS and AGGREGATED_VALUES in eventvisualization table
UPDATE eventvisualization
SET type = 'LINE_LIST'
WHERE datatype = 'EVENTS';
UPDATE eventvisualization
SET type = 'PIVOT_TABLE'
WHERE datatype = 'AGGREGATED_VALUES';


-- Migrate data from eventchart_attributedimensions into eventvisualization_attributedimensions.
INSERT INTO eventvisualization_attributedimensions
(eventvisualizationid,
 trackedentityattributedimensionid,
 sort_order)
SELECT eventchartid,
       trackedentityattributedimensionid,
       sort_order
FROM eventchart_attributedimensions;


-- Migrate data from eventreport_attributedimensions into eventvisualization_attributedimensions.
INSERT INTO eventvisualization_attributedimensions
(eventvisualizationid,
 trackedentityattributedimensionid,
 sort_order)
SELECT eventreportid,
       trackedentityattributedimensionid,
       sort_order
FROM eventreport_attributedimensions;


-- Migrate data from eventchart_categorydimensions into eventvisualization_categorydimensions.
INSERT INTO eventvisualization_categorydimensions
(eventvisualizationid,
 categorydimensionid,
 sort_order)
SELECT eventchartid,
       categorydimensionid,
       sort_order
FROM eventchart_categorydimensions;


-- Migrate data from eventreport_categorydimensions into eventvisualization_categorydimensions.
INSERT INTO eventvisualization_categorydimensions
(eventvisualizationid,
 categorydimensionid,
 sort_order)
SELECT eventreportid,
       categorydimensionid,
       sort_order
FROM eventreport_categorydimensions;


-- Migrate data from eventchart_categoryoptiongroupsetdimensions into eventvisualization_categoryoptiongroupsetdimensions.
INSERT INTO eventvisualization_categoryoptiongroupsetdimensions
(eventvisualizationid,
 categoryoptiongroupsetdimensionid,
 sort_order)
SELECT eventchartid,
       categoryoptiongroupsetdimensionid,
       sort_order
FROM eventchart_categoryoptiongroupsetdimensions;


-- Migrate data from eventreport_categoryoptiongroupsetdimensions into eventvisualization_categoryoptiongroupsetdimensions.
INSERT INTO eventvisualization_categoryoptiongroupsetdimensions
(eventvisualizationid,
 categoryoptiongroupsetdimensionid,
 sort_order)
SELECT eventreportid,
       categoryoptiongroupsetdimensionid,
       sort_order
FROM eventreport_categoryoptiongroupsetdimensions;


-- Migrate data from eventchart_columns into eventvisualization_columns.
INSERT INTO eventvisualization_columns
(eventvisualizationid,
 dimension,
 sort_order)
SELECT eventchartid,
       dimension,
       sort_order
FROM eventchart_columns;


-- Migrate data from eventreport_columns into eventvisualization_columns.
INSERT INTO eventvisualization_columns
(eventvisualizationid,
 dimension,
 sort_order)
SELECT eventreportid,
       dimension,
       sort_order
FROM eventreport_columns;


-- Migrate data from eventchart_dataelementdimensions into eventvisualization_dataelementdimensions.
INSERT INTO eventvisualization_dataelementdimensions
(eventvisualizationid,
 trackedentitydataelementdimensionid,
 sort_order)
SELECT eventchartid,
       trackedentitydataelementdimensionid,
       sort_order
FROM eventchart_dataelementdimensions;


-- Migrate data from eventreport_dataelementdimensions into eventvisualization_dataelementdimensions.
INSERT INTO eventvisualization_dataelementdimensions
(eventvisualizationid,
 trackedentitydataelementdimensionid,
 sort_order)
SELECT eventreportid,
       trackedentitydataelementdimensionid,
       sort_order
FROM eventreport_dataelementdimensions;


-- Migrate data from eventchart_filters into eventvisualization_filters.
INSERT INTO eventvisualization_filters
(eventvisualizationid,
 dimension,
 sort_order)
SELECT eventchartid,
       dimension,
       sort_order
FROM eventchart_filters;


-- Migrate data from eventreport_filters into eventvisualization_filters.
INSERT INTO eventvisualization_filters
(eventvisualizationid,
 dimension,
 sort_order)
SELECT eventreportid,
       dimension,
       sort_order
FROM eventreport_filters;


-- Migrate data from eventchart_itemorgunitgroups into eventvisualization_itemorgunitgroups.
INSERT INTO eventvisualization_itemorgunitgroups
(eventvisualizationid,
 orgunitgroupid,
 sort_order)
SELECT eventchartid,
       orgunitgroupid,
       sort_order
FROM eventchart_itemorgunitgroups;


-- Migrate data from eventreport_itemorgunitgroups into eventvisualization_itemorgunitgroups.
INSERT INTO eventvisualization_itemorgunitgroups
(eventvisualizationid,
 orgunitgroupid,
 sort_order)
SELECT eventreportid,
       orgunitgroupid,
       sort_order
FROM eventreport_itemorgunitgroups;


-- Migrate data from eventchart_organisationunits into eventvisualization_organisationunits.
INSERT INTO eventvisualization_organisationunits
(eventvisualizationid,
 organisationunitid,
 sort_order)
SELECT eventchartid,
       organisationunitid,
       sort_order
FROM eventchart_organisationunits;


-- Migrate data from eventreport_organisationunits into eventvisualization_organisationunits.
INSERT INTO eventvisualization_organisationunits
(eventvisualizationid,
 organisationunitid,
 sort_order)
SELECT eventreportid,
       organisationunitid,
       sort_order
FROM eventreport_organisationunits;


-- Migrate data from eventchart_orgunitgroupsetdimensions into eventvisualization_orgunitgroupsetdimensions.
INSERT INTO eventvisualization_orgunitgroupsetdimensions
(eventvisualizationid,
 orgunitgroupsetdimensionid,
 sort_order)
SELECT eventchartid,
       orgunitgroupsetdimensionid,
       sort_order
FROM eventchart_orgunitgroupsetdimensions;


-- Migrate data from eventreport_orgunitgroupsetdimensions into eventvisualization_orgunitgroupsetdimensions.
INSERT INTO eventvisualization_orgunitgroupsetdimensions
(eventvisualizationid,
 orgunitgroupsetdimensionid,
 sort_order)
SELECT eventreportid,
       orgunitgroupsetdimensionid,
       sort_order
FROM eventreport_orgunitgroupsetdimensions;


-- Migrate data from eventchart_orgunitlevels into eventvisualization_orgunitlevels.
INSERT INTO eventvisualization_orgunitlevels
(eventvisualizationid,
 orgunitlevel,
 sort_order)
SELECT eventchartid,
       orgunitlevel,
       sort_order
FROM eventchart_orgunitlevels;


-- Migrate data from eventreport_orgunitlevels into eventvisualization_orgunitlevels.
INSERT INTO eventvisualization_orgunitlevels
(eventvisualizationid,
 orgunitlevel,
 sort_order)
SELECT eventreportid,
       orgunitlevel,
       sort_order
FROM eventreport_orgunitlevels;


-- Migrate data from eventchart_periods into eventvisualization_periods.
INSERT INTO eventvisualization_periods
(eventvisualizationid,
 periodid,
 sort_order)
SELECT eventchartid,
       periodid,
       sort_order
FROM eventchart_periods;


-- Migrate data from eventreport_periods into eventvisualization_periods.
INSERT INTO eventvisualization_periods
(eventvisualizationid,
 periodid,
 sort_order)
SELECT eventreportid,
       periodid,
       sort_order
FROM eventreport_periods;


-- Migrate data from eventchart_programindicatordimensions into eventvisualization_programindicatordimensions.
INSERT INTO eventvisualization_programindicatordimensions
(eventvisualizationid,
 trackedentityprogramindicatordimensionid,
 sort_order)
SELECT eventchartid,
       trackedentityprogramindicatordimensionid,
       sort_order
FROM eventchart_programindicatordimensions;


-- Migrate data from eventreport_programindicatordimensions into eventvisualization_programindicatordimensions.
INSERT INTO eventvisualization_programindicatordimensions
(eventvisualizationid,
 trackedentityprogramindicatordimensionid,
 sort_order)
SELECT eventreportid,
       trackedentityprogramindicatordimensionid,
       sort_order
FROM eventreport_programindicatordimensions;


-- Migrate data from eventchart_rows into eventvisualization_rows.
INSERT INTO eventvisualization_rows
(eventvisualizationid,
 dimension,
 sort_order)
SELECT eventchartid,
       dimension,
       sort_order
FROM eventchart_rows;


-- Migrate data from eventreport_rows into eventvisualization_rows.
INSERT INTO eventvisualization_rows
(eventvisualizationid,
 dimension,
 sort_order)
SELECT eventreportid,
       dimension,
       sort_order
FROM eventreport_rows;


-- Add default value for public access null columns.
UPDATE eventvisualization
SET publicaccess = '--------'
WHERE COALESCE(publicaccess, '') = ''
   OR publicaccess IS NULL;


-- Update the column eventvisualizationid of the dashboarditem table
-- so it receives all the eventchartid's.
UPDATE dashboarditem
SET eventvisualizationid = eventchartid
WHERE eventchartid IS NOT NULL;


-- Update the column visualizationid of the dashboarditem table
-- so it receives all the eventreport's.
UPDATE dashboarditem
SET eventvisualizationid = eventreport
WHERE eventreport IS NOT NULL;


-- Migrating interpretation columns eventreport and eventchart into eventvisualization table
UPDATE interpretation
SET eventvisualizationid = eventreportid
WHERE eventreportid IS NOT NULL;

UPDATE interpretation
SET eventvisualizationid = eventchartid
WHERE eventchartid IS NOT NULL;


-- Populate the eventvisualizationviews based on existing metrics for report table and charts.
UPDATE datastatistics
SET eventvisualizationviews = eventreportviews + eventchartviews
WHERE eventvisualizationviews IS NULL;

UPDATE datastatistics
SET eventvisualizations = eventreports + eventcharts
WHERE eventvisualizations IS NULL;


-- Set all NULL boolean columns to false. Hibernate are primitive booleans.
UPDATE eventvisualization
SET userorganisationunit = false
WHERE userorganisationunit IS NULL;

UPDATE eventvisualization
SET userorganisationunitchildren = false
WHERE userorganisationunitchildren IS NULL;

UPDATE eventvisualization
SET userorganisationunitgrandchildren = false
WHERE userorganisationunitgrandchildren IS NULL;

UPDATE eventvisualization
SET externalaccess = false
WHERE externalaccess IS NULL;

UPDATE eventvisualization
SET collapsedatadimensions = false
WHERE collapsedatadimensions IS NULL;

UPDATE eventvisualization
SET hidenadata = false
WHERE hidenadata IS NULL;

UPDATE eventvisualization
SET completedonly = false
WHERE completedonly IS NULL;

UPDATE eventvisualization
SET hidetitle = false
WHERE hidetitle IS NULL;

UPDATE eventvisualization
SET hidesubtitle = false
WHERE hidesubtitle IS NULL;

UPDATE eventvisualization
SET showdata = false
WHERE showdata IS NULL;

UPDATE eventvisualization
SET hidelegend = false
WHERE hidelegend IS NULL;

UPDATE eventvisualization
SET percentstackedvalues = false
WHERE percentstackedvalues IS NULL;

UPDATE eventvisualization
SET cumulativevalues = false
WHERE cumulativevalues IS NULL;

UPDATE eventvisualization
SET nospacebetweencolumns = false
WHERE nospacebetweencolumns IS NULL;

UPDATE eventvisualization
SET hideemptyrows = false
WHERE hideemptyrows IS NULL;

UPDATE eventvisualization
SET showhierarchy = false
WHERE showhierarchy IS NULL;

UPDATE eventvisualization
SET rowtotals = false
WHERE rowtotals IS NULL;

UPDATE eventvisualization
SET coltotals = false
WHERE coltotals IS NULL;

UPDATE eventvisualization
SET showdimensionlabels = false
WHERE showdimensionlabels IS NULL;

UPDATE eventvisualization
SET rowsubtotals = false
WHERE rowsubtotals IS NULL;

UPDATE eventvisualization
SET colsubtotals = false
WHERE colsubtotals IS NULL;

UPDATE eventvisualization
SET legacy = true
WHERE legacy IS NULL;

-- Set all NULL boolean columns to 0. Hibernate are primitive int's.
UPDATE eventvisualization
SET toplimit = 0
WHERE toplimit IS NULL;


-- Update constraints in dashboard item table
ALTER TABLE dashboarditem DROP CONSTRAINT IF EXISTS fk_dashboarditem_eventchartid;
ALTER TABLE dashboarditem DROP CONSTRAINT IF EXISTS fk_dashboarditem_eventreportid;
ALTER TABLE dashboarditem ADD CONSTRAINT fk_dashboarditem_eventchartid FOREIGN KEY (eventchartid) REFERENCES eventvisualization(eventvisualizationid);
ALTER TABLE dashboarditem ADD CONSTRAINT fk_dashboarditem_eventreportid FOREIGN KEY (eventreport) REFERENCES eventvisualization(eventvisualizationid);


-- Update constraints in interpretation table
ALTER TABLE interpretation DROP CONSTRAINT IF EXISTS fk_interpretation_eventreportid;
ALTER TABLE interpretation DROP CONSTRAINT IF EXISTS fk_interpretation_eventchartid;
ALTER TABLE interpretation ADD CONSTRAINT fk_interpretation_eventreportid FOREIGN KEY (eventchartid) REFERENCES eventvisualization(eventvisualizationid);
ALTER TABLE interpretation ADD CONSTRAINT fk_interpretation_eventchartid FOREIGN KEY (eventreportid) REFERENCES eventvisualization(eventvisualizationid);


-- Grant authorities for all users that can see Event Reports and Event Charts
INSERT INTO userroleauthorities (userroleid, authority)
SELECT userroleid, 'F_EVENT_VISUALIZATION_EXTERNAL'
FROM userroleauthorities WHERE authority = 'F_EVENTREPORT_EXTERNAL';

INSERT INTO userroleauthorities (userroleid, authority)
SELECT userroleid, 'F_EVENT_VISUALIZATION_PUBLIC_ADD'
FROM userroleauthorities WHERE authority = 'F_EVENTREPORT_PUBLIC_ADD';

INSERT INTO userroleauthorities (userroleid, authority)
SELECT userroleid, 'F_EVENT_VISUALIZATION_EXTERNAL'
FROM userroleauthorities WHERE authority = 'F_EVENTCHART_EXTERNAL';

INSERT INTO userroleauthorities (userroleid, authority)
SELECT userroleid, 'F_EVENT_VISUALIZATION_PUBLIC_ADD'
FROM userroleauthorities WHERE authority = 'F_EVENTCHART_PUBLIC_ADD';
