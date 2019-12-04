-- This script adds all new tables required by Visualization.
-- See Feature DHIS2-7946

-- It also updates:
-- 1) the table interpretation to add a new foreign key (visualizationid);
-- 2) the table report to add a new foreign key (visualizationid);
-- 3) the table dashboarditem to add a new foreign key (visualizationid).


CREATE TABLE IF NOT EXISTS visualization
(
  visualizationid bigint NOT NULL,
  uid character varying(11) NOT NULL,
  name character varying(255) NOT NULL,
  type character varying(255) NOT NULL,
  code character varying(50),
  title character varying(255),
  subtitle character varying(255),
  description text,
  created timestamp without time zone,
  startdate timestamp without time zone,
  enddate timestamp without time zone,
  categorycomboid integer,
  sortorder integer,
  toplimit integer,
  userid bigint,
  userorgunittype character varying(20),
  publicaccess character varying(8),
  displaydensity character varying(255),
  fontsize character varying(255),
  relativeperiodsid integer,
  numberformatting character varying(255),
  digitgroupseparator character varying(255),
  legendsetid bigint,
  legenddisplaystyle character varying(40),
  legenddisplaystrategy character varying(40),
  aggregationtype character varying(255),
  regressiontype character varying(40),
  targetlinevalue double precision,
  targetlinelabel character varying(255),
  rangeaxislabel character varying(255),
  rangeaxismaxvalue double precision,
  rangeaxissteps integer,
  rangeaxisdecimals integer,
  rangeaxisminvalue double precision,
  domainaxislabel character varying(255),
  baselinevalue double precision,
  baselinelabel character varying(255),
  numbertype character varying(40),
  measurecriteria character varying(255),
  colorsetid bigint,
  hideemptyrowitems character varying(40),
  percentstackedvalues boolean,
  nospacebetweencolumns boolean,
  regression boolean,
  externalaccess boolean,
  userorganisationunit boolean,
  userorganisationunitchildren boolean,
  userorganisationunitgrandchildren boolean,
  paramreportingperiod boolean,
  paramorganisationunit boolean,
  paramparentorganisationunit boolean,
  paramgrandparentorganisationunit boolean,
  rowtotals boolean,
  coltotals boolean,
  subtotals boolean,
  cumulative boolean,
  rowsubtotals boolean,
  colsubtotals boolean,
  completedonly boolean,
  skiprounding boolean,
  showdimensionlabels boolean,
  hidetitle boolean,
  hidesubtitle boolean,
  hidelegend boolean,
  hideemptycolumns boolean,
  hideemptyrows boolean,
  showhierarchy boolean,
  showdata boolean,
  lastupdatedby bigint,
  lastupdated timestamp without time zone,
  favorites jsonb,
  subscribers jsonb,
  translations jsonb,
  CONSTRAINT visualization_pkey PRIMARY KEY (visualizationid),
  CONSTRAINT fk_visualization_lastupdateby FOREIGN KEY (lastupdatedby)
      REFERENCES userinfo (userinfoid) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_visualization_relativeperiodsid FOREIGN KEY (relativeperiodsid)
      REFERENCES relativeperiods (relativeperiodsid) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_visualization_categorycombo FOREIGN KEY (categorycomboid)
      REFERENCES categorycombo (categorycomboid) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_visualization_legendsetid FOREIGN KEY (legendsetid)
      REFERENCES maplegendset (maplegendsetid) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_visualization_userid FOREIGN KEY (userid)
      REFERENCES userinfo (userinfoid) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT visualization_code_key UNIQUE (code),
  CONSTRAINT visualization_relativeperiodsid_key UNIQUE (relativeperiodsid),
  CONSTRAINT visualization_uid_key UNIQUE (uid),
  CONSTRAINT fk_visualization_colorsetid FOREIGN KEY (colorsetid)
      REFERENCES colorset (colorsetid) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
ALTER TABLE visualization
  OWNER TO dhis;


-- Table: visualization_categorydimensions

CREATE TABLE IF NOT EXISTS visualization_categorydimensions
(
  visualizationid bigint NOT NULL,
  categorydimensionid integer NOT NULL,
  sort_order integer NOT NULL,
  CONSTRAINT visualization_categorydimensions_pkey PRIMARY KEY (visualizationid, sort_order),
  CONSTRAINT fk_visualization_categorydimensions_categorydimensionid FOREIGN KEY (categorydimensionid)
      REFERENCES categorydimension (categorydimensionid) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_visualization_categorydimensions_visualizationid FOREIGN KEY (visualizationid)
      REFERENCES visualization (visualizationid) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
ALTER TABLE visualization_categorydimensions
  OWNER TO dhis;


-- Table: visualization_categoryoptiongroupsetdimensions

CREATE TABLE IF NOT EXISTS visualization_categoryoptiongroupsetdimensions
(
  visualizationid bigint NOT NULL,
  sort_order integer NOT NULL,
  categoryoptiongroupsetdimensionid integer NOT NULL,
  CONSTRAINT visualization_categoryoptiongroupsetdimensions_pkey PRIMARY KEY (visualizationid, sort_order),
  CONSTRAINT fk_visualization_catoptiongroupsetdimensions_visualizationid FOREIGN KEY (visualizationid)
      REFERENCES visualization (visualizationid) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_visualization_dimensions_catoptiongroupsetdimensionid FOREIGN KEY (categoryoptiongroupsetdimensionid)
      REFERENCES categoryoptiongroupsetdimension (categoryoptiongroupsetdimensionid) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
ALTER TABLE visualization_categoryoptiongroupsetdimensions
  OWNER TO dhis;


-- Table: visualization_columns

CREATE TABLE IF NOT EXISTS visualization_columns
(
  visualizationid bigint NOT NULL,
  dimension character varying(255),
  sort_order integer NOT NULL,
  CONSTRAINT visualization_columns_pkey PRIMARY KEY (visualizationid, sort_order),
  CONSTRAINT fk_visualization_columns_visualizationid FOREIGN KEY (visualizationid)
      REFERENCES visualization (visualizationid) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
ALTER TABLE visualization_columns
  OWNER TO dhis;


-- Table: visualization_datadimensionitems

CREATE TABLE IF NOT EXISTS visualization_datadimensionitems
(
  visualizationid bigint NOT NULL,
  datadimensionitemid integer NOT NULL,
  sort_order integer NOT NULL,
  CONSTRAINT visualization_datadimensionitems_pkey PRIMARY KEY (visualizationid, sort_order),
  CONSTRAINT fk_visualization_datadimensionitems_datadimensionitemid FOREIGN KEY (datadimensionitemid)
      REFERENCES datadimensionitem (datadimensionitemid) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_visualization_datadimensionitems_visualizationid FOREIGN KEY (visualizationid)
      REFERENCES visualization (visualizationid) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
ALTER TABLE visualization_datadimensionitems
  OWNER TO dhis;


-- Table: visualization_dataelementgroupsetdimensions

CREATE TABLE IF NOT EXISTS visualization_dataelementgroupsetdimensions
(
  visualizationid bigint NOT NULL,
  sort_order integer NOT NULL,
  dataelementgroupsetdimensionid integer NOT NULL,
  CONSTRAINT visualization_dataelementgroupsetdimensions_pkey PRIMARY KEY (visualizationid, sort_order),
  CONSTRAINT fk_visualization_dataelementgroupsetdimensions_visualizationid FOREIGN KEY (visualizationid)
      REFERENCES visualization (visualizationid) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_visualization_dimensions_dataelementgroupsetdimensionid FOREIGN KEY (dataelementgroupsetdimensionid)
      REFERENCES dataelementgroupsetdimension (dataelementgroupsetdimensionid) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
ALTER TABLE visualization_dataelementgroupsetdimensions
  OWNER TO dhis;


-- Table: visualization_filters

CREATE TABLE IF NOT EXISTS visualization_filters
(
  visualizationid bigint NOT NULL,
  dimension character varying(255),
  sort_order integer NOT NULL,
  CONSTRAINT visualization_filters_pkey PRIMARY KEY (visualizationid, sort_order),
  CONSTRAINT fk_visualization_filters_visualizationid FOREIGN KEY (visualizationid)
      REFERENCES visualization (visualizationid) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
ALTER TABLE visualization_filters
  OWNER TO dhis;


-- Table: visualization_itemorgunitgroups

CREATE TABLE IF NOT EXISTS visualization_itemorgunitgroups
(
  visualizationid bigint NOT NULL,
  orgunitgroupid bigint NOT NULL,
  sort_order integer NOT NULL,
  CONSTRAINT visualization_itemorgunitgroups_pkey PRIMARY KEY (visualizationid, sort_order),
  CONSTRAINT fk_visualization_itemorgunitgroups_orgunitgroupid FOREIGN KEY (orgunitgroupid)
      REFERENCES orgunitgroup (orgunitgroupid) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_visualization_itemorgunitunitgroups_visualizationid FOREIGN KEY (visualizationid)
      REFERENCES visualization (visualizationid) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
ALTER TABLE visualization_itemorgunitgroups
  OWNER TO dhis;


-- Table: visualization_organisationunits

CREATE TABLE IF NOT EXISTS visualization_organisationunits
(
  visualizationid bigint NOT NULL,
  organisationunitid bigint NOT NULL,
  sort_order integer NOT NULL,
  CONSTRAINT visualization_organisationunits_pkey PRIMARY KEY (visualizationid, sort_order),
  CONSTRAINT fk_visualization_organisationunits_visualizationid FOREIGN KEY (visualizationid)
      REFERENCES visualization (visualizationid) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fkvisualization_organisationunits_organisationunitid FOREIGN KEY (organisationunitid)
      REFERENCES organisationunit (organisationunitid) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_visualization_organisationunits_organisationunitid FOREIGN KEY (organisationunitid)
      REFERENCES organisationunit (organisationunitid) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
ALTER TABLE visualization_organisationunits
  OWNER TO dhis;


-- Table: visualization_orgunitgroupsetdimensions

CREATE TABLE IF NOT EXISTS visualization_orgunitgroupsetdimensions
(
  visualizationid bigint NOT NULL,
  sort_order integer NOT NULL,
  orgunitgroupsetdimensionid integer NOT NULL,
  CONSTRAINT visualization_orgunitgroupsetdimensions_pkey PRIMARY KEY (visualizationid, sort_order),
  CONSTRAINT fk_visualization_dimensions_orgunitgroupsetdimensionid FOREIGN KEY (orgunitgroupsetdimensionid)
      REFERENCES orgunitgroupsetdimension (orgunitgroupsetdimensionid) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_visualization_orgunitgroupsetdimensions_visualizationid FOREIGN KEY (visualizationid)
      REFERENCES visualization (visualizationid) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
ALTER TABLE visualization_orgunitgroupsetdimensions
  OWNER TO dhis;


-- Table: visualization_orgunitlevels

CREATE TABLE IF NOT EXISTS visualization_orgunitlevels
(
  visualizationid bigint NOT NULL,
  orgunitlevel integer,
  sort_order integer NOT NULL,
  CONSTRAINT visualization_orgunitlevels_pkey PRIMARY KEY (visualizationid, sort_order),
  CONSTRAINT fk_visualization_orgunitlevels_visualizationid FOREIGN KEY (visualizationid)
      REFERENCES visualization (visualizationid) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
ALTER TABLE visualization_orgunitlevels
  OWNER TO dhis;


-- Table: visualization_periods

CREATE TABLE IF NOT EXISTS visualization_periods
(
  visualizationid bigint NOT NULL,
  periodid bigint NOT NULL,
  sort_order integer NOT NULL,
  CONSTRAINT visualization_periods_pkey PRIMARY KEY (visualizationid, sort_order),
  CONSTRAINT fk_visualization_periods_periodid FOREIGN KEY (periodid)
      REFERENCES period (periodid) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_visualization_periods_visualizationid FOREIGN KEY (visualizationid)
      REFERENCES visualization (visualizationid) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
ALTER TABLE visualization_periods
  OWNER TO dhis;


-- Table: visualization_rows

CREATE TABLE IF NOT EXISTS visualization_rows
(
  visualizationid bigint NOT NULL,
  dimension character varying(255),
  sort_order integer NOT NULL,
  CONSTRAINT visualization_rows_pkey PRIMARY KEY (visualizationid, sort_order),
  CONSTRAINT fk_visualization_rows_visualizationid FOREIGN KEY (visualizationid)
      REFERENCES visualization (visualizationid) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
ALTER TABLE visualization_rows
  OWNER TO dhis;


-- Table: visualization_useraccesses

CREATE TABLE IF NOT EXISTS visualization_useraccesses
(
  visualizationid bigint NOT NULL,
  useraccessid integer NOT NULL,
  CONSTRAINT visualization_useraccesses_pkey PRIMARY KEY (visualizationid, useraccessid),
  CONSTRAINT fk_visualization_useraccesses_visualizationid FOREIGN KEY (visualizationid)
      REFERENCES visualization (visualizationid) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_visualization_useraccesses_useraccessid FOREIGN KEY (useraccessid)
      REFERENCES useraccess (useraccessid) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
ALTER TABLE visualization_useraccesses
  OWNER TO dhis;


-- Table: visualization_usergroupaccesses

CREATE TABLE IF NOT EXISTS visualization_usergroupaccesses
(
  visualizationid bigint NOT NULL,
  usergroupaccessid integer NOT NULL,
  CONSTRAINT visualization_usergroupaccesses_pkey PRIMARY KEY (visualizationid, usergroupaccessid),
  CONSTRAINT fk_visualization_usergroupaccesses_visualizationid FOREIGN KEY (visualizationid)
      REFERENCES visualization (visualizationid) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_visualization_usergroupaccesses_usergroupaccessid FOREIGN KEY (usergroupaccessid)
      REFERENCES usergroupaccess (usergroupaccessid) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT visualization_usergroupaccesses_usergroupaccessid_key UNIQUE (usergroupaccessid)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE visualization_usergroupaccesses
  OWNER TO dhis;


-- Table: visualization_yearlyseries

CREATE TABLE IF NOT EXISTS visualization_yearlyseries
(
  visualizationid bigint NOT NULL,
  sort_order integer NOT NULL,
  yearlyseries character varying(255),
  CONSTRAINT visualization_yearlyseries_pkey PRIMARY KEY (visualizationid, sort_order),
  CONSTRAINT fk_visualization_yearlyseries_visualizationid FOREIGN KEY (visualizationid)
      REFERENCES visualization (visualizationid) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
ALTER TABLE visualization_yearlyseries
  OWNER TO dhis;


-- Table: axis

CREATE TABLE axis
(
  axisid bigint NOT NULL,
  dimensionalitem character varying(255) NOT NULL,
  axis integer NOT NULL,
  CONSTRAINT axis_pkey PRIMARY KEY (axisid)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE axis
  OWNER TO dhis;


-- Table: visualization_axis

CREATE TABLE visualization_axis
(
  visualizationid bigint NOT NULL,
  sort_order integer NOT NULL,
  axisid bigint NOT NULL,
  CONSTRAINT visualization_axis_pkey PRIMARY KEY (visualizationid, sort_order),
  CONSTRAINT fk_visualization_axis_visualizationid FOREIGN KEY (visualizationid)
      REFERENCES visualization (visualizationid) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_visualization_axis_axisid FOREIGN KEY (axisid)
      REFERENCES axis (axisid) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
ALTER TABLE visualization_axis
  OWNER TO dhis;


-- Table: interpretation

ALTER TABLE interpretation
   ADD COLUMN IF NOT EXISTS visualizationid bigint;

ALTER TABLE interpretation
  ADD CONSTRAINT fk_interpretation_visualizationid FOREIGN KEY (visualizationid) REFERENCES visualization (visualizationid) ON UPDATE NO ACTION ON DELETE NO ACTION;


-- Table: report

ALTER TABLE report
   ADD COLUMN IF NOT EXISTS visualizationid bigint;

ALTER TABLE report
  ADD CONSTRAINT fk_report_visualizationid FOREIGN KEY (visualizationid) REFERENCES visualization (visualizationid) ON UPDATE NO ACTION ON DELETE NO ACTION;


-- Table: dashboarditem

ALTER TABLE dashboarditem
  ADD COLUMN IF NOT EXISTS visualizationid bigint;

ALTER TABLE dashboarditem
  ADD CONSTRAINT fk_dashboarditem_visualizationid FOREIGN KEY (visualizationid) REFERENCES visualization (visualizationid) ON UPDATE NO ACTION ON DELETE NO ACTION;
