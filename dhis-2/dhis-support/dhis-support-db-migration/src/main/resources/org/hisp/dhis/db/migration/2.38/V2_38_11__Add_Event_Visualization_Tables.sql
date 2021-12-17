-- This script relates to the issue https://jira.dhis2.org/browse/TECH-703
-- It creates all tables needed to support Event Visualizations and update
-- all necessary existing tables.

-- eventvisualization table
CREATE TABLE IF NOT EXISTS eventvisualization
(
    -- merged
    eventvisualizationid              int8         NOT NULL,

    -- common
    uid                               varchar(11)  NULL,
    code                              varchar(50)  NULL,
    created                           timestamp    NULL,
    lastupdated                       timestamp    NULL,
    name                              varchar(230) NOT NULL,
    relativeperiodsid                 int4         NULL,
    userorganisationunit              bool         NULL,
    userorganisationunitchildren      bool         NULL,
    userorganisationunitgrandchildren bool         NULL,
    externalaccess                    bool         NULL,
    userid                            int8         NULL,
    publicaccess                      varchar(8)   NULL,
    programid                         int8         NOT NULL,
    programstageid                    int8         NULL,
    startdate                         timestamp    NULL,
    enddate                           timestamp    NULL,
    sortorder                         int4         NULL,
    toplimit                          int4         NULL,
    outputtype                        varchar(30)  NULL,
    dataelementvaluedimensionid       int8         NULL,
    attributevaluedimensionid         int8         NULL,
    aggregationtype                   varchar(30)  NULL,
    collapsedatadimensions            bool         NULL,
    hidenadata                        bool         NULL,
    completedonly                     bool         NULL,
    description                       text         NULL,
    title                             varchar(255) NULL,
    lastupdatedby                     int8         NULL,
    subtitle                          varchar(255) NULL,
    hidetitle                         bool         NULL,
    hidesubtitle                      bool         NULL,
    programstatus                     varchar(40)  NULL,
    eventstatus                       varchar(40)  NULL,
    favorites                         jsonb        NULL,
    subscribers                       jsonb        NULL,
    timefield                         varchar(255) NULL,
    translations                      jsonb        NULL,
    orgunitfield                      varchar(255) NULL,
    userorgunittype                   varchar(20)  NULL,
    sharing                           jsonb        NULL DEFAULT '{}'::jsonb,
    attributevalues                   jsonb        NULL DEFAULT '{}'::jsonb,

    -- merged
    type                              varchar(255) NOT NULL,

    -- eventchart specifics
    showdata                          bool         NULL,
    rangeaxismaxvalue                 float8       NULL,
    rangeaxisminvalue                 float8       NULL,
    rangeaxissteps                    int4         NULL,
    rangeaxisdecimals                 int4         NULL,
    domainaxislabel                   varchar(255) NULL,
    rangeaxislabel                    varchar(255) NULL,
    hidelegend                        bool         NULL,
    targetlinevalue                   float8       NULL,
    targetlinelabel                   varchar(255) NULL,
    baselinevalue                     float8       NULL,
    baselinelabel                     varchar(255) NULL,
    regressiontype                    varchar(40)  NULL, -- can be null now
    hideemptyrowitems                 varchar(40)  NULL, -- can be null now
    percentstackedvalues              bool         NULL,
    cumulativevalues                  bool         NULL,
    nospacebetweencolumns             bool         NULL,

    -- eventreport specifics
    datatype                          varchar(230) NULL,
    hideemptyrows                     bool         NULL,
    digitgroupseparator               varchar(255) NULL,
    displaydensity                    varchar(255) NULL,
    fontsize                          varchar(255) NULL,
    showhierarchy                     bool         NULL,
    rowtotals                         bool         NULL,
    coltotals                         bool         NULL,
    showdimensionlabels               bool         NULL,
    rowsubtotals                      bool         NULL,
    colsubtotals                      bool         NULL,
    legacy                            bool         NULL,

    -- CONTRAINTS
    CONSTRAINT eventvisualization_pkey PRIMARY KEY (eventvisualizationid),
    CONSTRAINT fk_evisualization_attributevaluedimensionid FOREIGN KEY (attributevaluedimensionid) REFERENCES trackedentityattribute (trackedentityattributeid),
    CONSTRAINT fk_evisualization_dataelementvaluedimensionid FOREIGN KEY (dataelementvaluedimensionid) REFERENCES dataelement (dataelementid),
    CONSTRAINT fk_evisualization_programid FOREIGN KEY (programid) REFERENCES "program" (programid),
    CONSTRAINT fk_evisualization_programstageid FOREIGN KEY (programstageid) REFERENCES programstage (programstageid),
    CONSTRAINT fk_evisualization_relativeperiodsid FOREIGN KEY (relativeperiodsid) REFERENCES relativeperiods (relativeperiodsid),
    CONSTRAINT fk_evisualization_userid FOREIGN KEY (userid) REFERENCES userinfo (userinfoid),
    CONSTRAINT fk_evisualization_lastupdateby_userid FOREIGN KEY (lastupdatedby) REFERENCES userinfo (userinfoid),
    CONSTRAINT fk_evisualization_report_relativeperiodsid FOREIGN KEY (relativeperiodsid) REFERENCES relativeperiods (relativeperiodsid)
);


-- eventvisualization_attributedimensions table
CREATE TABLE IF NOT EXISTS eventvisualization_attributedimensions
(
    eventvisualizationid              int8 NOT NULL,
    trackedentityattributedimensionid int4 NOT NULL,
    sort_order                        int4 NOT NULL,

    -- CONTRAINTS
    CONSTRAINT eventvisualization_attributedimensions_pkey PRIMARY KEY (eventvisualizationid, sort_order),
    CONSTRAINT fk_evisualization_attributedimensions_attributedimensionid FOREIGN KEY (trackedentityattributedimensionid) REFERENCES trackedentityattributedimension (trackedentityattributedimensionid),
    CONSTRAINT fk_evisualization_attributedimensions_evisualizationid FOREIGN KEY (eventvisualizationid) REFERENCES eventvisualization (eventvisualizationid)
);


-- eventvisualization_categorydimensions table
CREATE TABLE IF NOT EXISTS eventvisualization_categorydimensions
(
    eventvisualizationid int8 NOT NULL,
    sort_order           int4 NOT NULL,
    categorydimensionid  int4 NOT NULL,

    -- CONTRAINTS
    CONSTRAINT eventvisualization_categorydimensions_pkey PRIMARY KEY (eventvisualizationid, sort_order),
    CONSTRAINT fk_evisualization_categorydimensions_categorydimensionid FOREIGN KEY (categorydimensionid) REFERENCES categorydimension (categorydimensionid),
    CONSTRAINT fk_evisualization_categorydimensions_evisualizationid FOREIGN KEY (eventvisualizationid) REFERENCES eventvisualization (eventvisualizationid)
);


-- eventvisualization_categoryoptiongroupsetdimensions table
CREATE TABLE IF NOT EXISTS eventvisualization_categoryoptiongroupsetdimensions
(
    eventvisualizationid              int8 NOT NULL,
    sort_order                        int4 NOT NULL,
    categoryoptiongroupsetdimensionid int4 NOT NULL,

    -- CONTRAINTS
    CONSTRAINT eventvisualization_categoryoptiongroupsetdimensions_pkey PRIMARY KEY (eventvisualizationid, sort_order),
    CONSTRAINT fk_evisualization_catoptiongroupsetdimensions_evisualizationid FOREIGN KEY (eventvisualizationid) REFERENCES eventvisualization (eventvisualizationid),
    CONSTRAINT fk_evisualization_dimensions_catoptiongroupsetdimensionid FOREIGN KEY (categoryoptiongroupsetdimensionid) REFERENCES categoryoptiongroupsetdimension (categoryoptiongroupsetdimensionid)
);


-- eventvisualization_columns table
CREATE TABLE IF NOT EXISTS eventvisualization_columns
(
    eventvisualizationid int8         NOT NULL,
    dimension            varchar(255) NULL,
    sort_order           int4         NOT NULL,
    CONSTRAINT eventvisualization_columns_pkey PRIMARY KEY (eventvisualizationid, sort_order),
    CONSTRAINT fk_evisualization_columns_evisualizationid FOREIGN KEY (eventvisualizationid) REFERENCES eventvisualization (eventvisualizationid)
);


-- eventvisualization_dataelementdimensions table
CREATE TABLE IF NOT EXISTS eventvisualization_dataelementdimensions
(
    eventvisualizationid                int8 NOT NULL,
    trackedentitydataelementdimensionid int4 NOT NULL,
    sort_order                          int4 NOT NULL,
    CONSTRAINT eventvisualization_dataelementdimensions_pkey PRIMARY KEY (eventvisualizationid, sort_order),
    CONSTRAINT fk_evisualization_dataelementdimensions_dataelementdimensionid FOREIGN KEY (trackedentitydataelementdimensionid) REFERENCES trackedentitydataelementdimension (trackedentitydataelementdimensionid),
    CONSTRAINT fk_evisualization_dataelementdimensions_evisualizationid FOREIGN KEY (eventvisualizationid) REFERENCES eventvisualization (eventvisualizationid)
);


-- eventvisualization_filters table
CREATE TABLE IF NOT EXISTS eventvisualization_filters
(
    eventvisualizationid int8         NOT NULL,
    dimension            varchar(255) NULL,
    sort_order           int4         NOT NULL,
    CONSTRAINT eventvisualization_filters_pkey PRIMARY KEY (eventvisualizationid, sort_order),
    CONSTRAINT fk_evisualization_filters_evisualizationid FOREIGN KEY (eventvisualizationid) REFERENCES eventvisualization (eventvisualizationid)
);


-- eventvisualization_itemorgunitgroups table
CREATE TABLE IF NOT EXISTS eventvisualization_itemorgunitgroups
(
    eventvisualizationid int8 NOT NULL,
    orgunitgroupid       int8 NOT NULL,
    sort_order           int4 NOT NULL,
    CONSTRAINT eventvisualization_itemorgunitgroups_pkey PRIMARY KEY (eventvisualizationid, sort_order),
    CONSTRAINT fk_evisualization_itemorgunitgroups_orgunitgroupid FOREIGN KEY (orgunitgroupid) REFERENCES orgunitgroup (orgunitgroupid),
    CONSTRAINT fk_evisualization_itemorgunitunitgroups_evisualizationid FOREIGN KEY (eventvisualizationid) REFERENCES eventvisualization (eventvisualizationid)
);


-- eventvisualization_organisationunits table
CREATE TABLE IF NOT EXISTS eventvisualization_organisationunits
(
    eventvisualizationid int8 NOT NULL,
    organisationunitid   int8 NOT NULL,
    sort_order           int4 NOT NULL,
    CONSTRAINT eventvisualization_organisationunits_pkey PRIMARY KEY (eventvisualizationid, sort_order),
    CONSTRAINT fk_evisualization_organisationunits_evisualizationid FOREIGN KEY (eventvisualizationid) REFERENCES eventvisualization (eventvisualizationid),
    CONSTRAINT fk_evisualization_organisationunits_organisationunitid FOREIGN KEY (organisationunitid) REFERENCES organisationunit (organisationunitid)
);


-- eventvisualization_orgunitgroupsetdimensions table
CREATE TABLE IF NOT EXISTS eventvisualization_orgunitgroupsetdimensions
(
    eventvisualizationid       int8 NOT NULL,
    sort_order                 int4 NOT NULL,
    orgunitgroupsetdimensionid int4 NOT NULL,
    CONSTRAINT eventvisualization_orgunitgroupsetdimensions_pkey PRIMARY KEY (eventvisualizationid, sort_order),
    CONSTRAINT fk_evisualization_dimensions_ogunitgroupsetdimensionid FOREIGN KEY (orgunitgroupsetdimensionid) REFERENCES orgunitgroupsetdimension (orgunitgroupsetdimensionid),
    CONSTRAINT fk_evisualization_orgunitgroupsetdimensions_evisualizationid FOREIGN KEY (eventvisualizationid) REFERENCES eventvisualization (eventvisualizationid)
);


-- eventvisualization_orgunitlevels table
CREATE TABLE IF NOT EXISTS eventvisualization_orgunitlevels
(
    eventvisualizationid int8 NOT NULL,
    orgunitlevel         int4 NULL,
    sort_order           int4 NOT NULL,
    CONSTRAINT eventvisualization_orgunitlevels_pkey PRIMARY KEY (eventvisualizationid, sort_order),
    CONSTRAINT fk_evisualization_orgunitlevels_evisualizationid FOREIGN KEY (eventvisualizationid) REFERENCES eventvisualization (eventvisualizationid)
);


-- eventvisualization_periods table
CREATE TABLE IF NOT EXISTS eventvisualization_periods
(
    eventvisualizationid int8 NOT NULL,
    periodid             int8 NOT NULL,
    sort_order           int4 NOT NULL,
    CONSTRAINT eventvisualization_periods_pkey PRIMARY KEY (eventvisualizationid, sort_order),
    CONSTRAINT fk_evisualization_periods_evisualizationid FOREIGN KEY (eventvisualizationid) REFERENCES eventvisualization (eventvisualizationid),
    CONSTRAINT fk_evisualization_periods_periodid FOREIGN KEY (periodid) REFERENCES "period" (periodid)
);


-- eventvisualization_programindicatordimensions table
CREATE TABLE IF NOT EXISTS eventvisualization_programindicatordimensions
(
    eventvisualizationid                     int8 NOT NULL,
    trackedentityprogramindicatordimensionid int4 NOT NULL,
    sort_order                               int4 NOT NULL,
    CONSTRAINT eventvisualization_programindicatordimensions_pkey PRIMARY KEY (eventvisualizationid, sort_order),
    CONSTRAINT fk_evisualization_prindicatordimensions_prindicatordimensionid FOREIGN KEY (trackedentityprogramindicatordimensionid) REFERENCES trackedentityprogramindicatordimension (trackedentityprogramindicatordimensionid),
    CONSTRAINT fk_evisualization_programindicatordimensions_evisualizationid FOREIGN KEY (eventvisualizationid) REFERENCES eventvisualization (eventvisualizationid),
    CONSTRAINT fk_evisualization_programindicatordim_programindicatordimid FOREIGN KEY (trackedentityprogramindicatordimensionid) REFERENCES trackedentityprogramindicatordimension (trackedentityprogramindicatordimensionid)
);


-- eventvisualization_rows table
CREATE TABLE IF NOT EXISTS eventvisualization_rows
(
    eventvisualizationid int8         NOT NULL,
    dimension            varchar(255) NULL,
    sort_order           int4         NOT NULL,
    CONSTRAINT eventvisualization_rows_pkey PRIMARY KEY (eventvisualizationid, sort_order),
    CONSTRAINT fk_evisualization_rows_evisualizationid FOREIGN KEY (eventvisualizationid) REFERENCES eventvisualization (eventvisualizationid)
);


-- interpretation table
ALTER TABLE interpretation
    ADD COLUMN IF NOT EXISTS eventvisualizationid bigint;

ALTER TABLE interpretation
    ADD CONSTRAINT fk_interpretation_evisualizationid FOREIGN KEY (eventvisualizationid) REFERENCES eventvisualization (eventvisualizationid);


-- dashboarditem table
ALTER TABLE dashboarditem
    ADD COLUMN IF NOT EXISTS eventvisualizationid bigint;

ALTER TABLE dashboarditem
    ADD CONSTRAINT fk_dashboarditem_evisualizationid FOREIGN KEY (eventvisualizationid) REFERENCES eventvisualization (eventvisualizationid);


-- statistics table
ALTER TABLE datastatistics
    ADD COLUMN IF NOT EXISTS eventvisualizationviews double precision;

ALTER TABLE datastatistics
    ADD COLUMN IF NOT EXISTS eventvisualizations double precision;
