SELECT
  ous."uidlevel1",
  ous."uidlevel2",
  ous."uidlevel3",
  ous."uidlevel4",
  ous."uidlevel5",
  ous."uidlevel6",
  ous."uidlevel7",
  ougs."pwWdBd3Bhvk",
  dps."daily",
  dps."weekly",
  dps."monthly",
  dps."bimonthly",
  dps."quarterly",
  dps."sixmonthly",
  dps."sixmonthlyapril",
  dps."yearly",
  dps."financialapril",
  dps."financialjuly",
  dps."financialoct",
  (SELECT CASE WHEN
    value
    =
    'true'
    THEN 1
          WHEN
            value
            =
            'false'
            THEN 0
          ELSE NULL END
   FROM
     trackedentitydatavalue
   WHERE
     programstageinstanceid
     =
     psi.programstageinstanceid
     AND
     dataelementid
     =
     7601)                                             AS "zeklwDmwrDr",
  (SELECT value
   FROM
     trackedentitydatavalue
   WHERE
     programstageinstanceid
     =
     psi.programstageinstanceid
     AND
     dataelementid
     =
     7575)                                             AS "CtipqDTVQ08",
  (SELECT CASE WHEN
    value
    =
    'true'
    THEN 1
          WHEN
            value
            =
            'false'
            THEN 0
          ELSE NULL END
   FROM
     trackedentitydatavalue
   WHERE
     programstageinstanceid
     =
     psi.programstageinstanceid
     AND
     dataelementid
     =
     7688)                                             AS "nNW3uYDDRd5",
  (SELECT cast(
      value
      AS
      TIMESTAMP)
   FROM
     trackedentitydatavalue
   WHERE
     programstageinstanceid
     =
     psi.programstageinstanceid
     AND
     dataelementid
     =
     6900
     AND
     value
     ~*
     '^\d{4}-\d{2}-\d{2}(\s|T)?(\d{2}:\d{2}:\d{2})?$') AS "MvFK3GuedO4",
  (SELECT cast(
      value
      AS
      DOUBLE PRECISION)
   FROM
     trackedentitydatavalue
   WHERE
     programstageinstanceid
     =
     psi.programstageinstanceid
     AND
     dataelementid
     =
     7590
     AND
     value
     ~*
     '^(-?[0-9]+)(\.[0-9]+)?$')                        AS "EmpntizyQIo",
  (SELECT CASE WHEN
    value
    =
    'true'
    THEN 1
          WHEN
            value
            =
            'false'
            THEN 0
          ELSE NULL END
   FROM
     trackedentitydatavalue
   WHERE
     programstageinstanceid
     =
     psi.programstageinstanceid
     AND
     dataelementid
     =
     7605)                                             AS "E3oRMAB6I4Y",
  (SELECT CASE WHEN
    value
    =
    'true'
    THEN 1
          WHEN
            value
            =
            'false'
            THEN 0
          ELSE NULL END
   FROM
     trackedentitydatavalue
   WHERE
     programstageinstanceid
     =
     psi.programstageinstanceid
     AND
     dataelementid
     =
     7596)                                             AS "VS9XRSdil9U",
  (SELECT cast(
      value
      AS
      DOUBLE PRECISION)
   FROM
     trackedentitydatavalue
   WHERE
     programstageinstanceid
     =
     psi.programstageinstanceid
     AND
     dataelementid
     =
     7592
     AND
     value
     ~*
     '^(-?[0-9]+)(\.[0-9]+)?$')                        AS "BbnfBg4vnRw",
  (SELECT value
   FROM
     trackedentitydatavalue
   WHERE
     programstageinstanceid
     =
     psi.programstageinstanceid
     AND
     dataelementid
     =
     7330)                                             AS "msTA9ZES9LB",
  (SELECT CASE WHEN
    value
    =
    'true'
    THEN 1
          WHEN
            value
            =
            'false'
            THEN 0
          ELSE NULL END
   FROM
     trackedentitydatavalue
   WHERE
     programstageinstanceid
     =
     psi.programstageinstanceid
     AND
     dataelementid
     =
     7606)                                             AS "MxbhCkHjjEs",
  (SELECT cast(
      value
      AS
      DOUBLE PRECISION)
   FROM
     trackedentitydatavalue
   WHERE
     programstageinstanceid
     =
     psi.programstageinstanceid
     AND
     dataelementid
     =
     7580
     AND
     value
     ~*
     '^(-?[0-9]+)(\.[0-9]+)?$')                        AS "qpaUEXEZ8Sr",
  (SELECT CASE WHEN
    value
    =
    'true'
    THEN 1
          WHEN
            value
            =
            'false'
            THEN 0
          ELSE NULL END
   FROM
     trackedentitydatavalue
   WHERE
     programstageinstanceid
     =
     psi.programstageinstanceid
     AND
     dataelementid
     =
     7609)                                             AS "wQwHzY1X5J3",
  (SELECT value
   FROM
     trackedentitydatavalue
   WHERE
     programstageinstanceid
     =
     psi.programstageinstanceid
     AND
     dataelementid
     =
     7577)                                             AS "YuKSmFcFQTO",
  (SELECT value
   FROM
     trackedentitydatavalue
   WHERE
     programstageinstanceid
     =
     psi.programstageinstanceid
     AND
     dataelementid
     =
     6893)                                             AS "pTVjK1rtjhf",
  (SELECT value
   FROM
     trackedentitydatavalue
   WHERE
     programstageinstanceid
     =
     psi.programstageinstanceid
     AND
     dataelementid
     =
     6906)                                             AS "Gsu1e1s04Jc",
  (SELECT cast(
      value
      AS
      BIGINT)
   FROM
     trackedentitydatavalue
   WHERE
     programstageinstanceid
     =
     psi.programstageinstanceid
     AND
     dataelementid
     =
     7034
     AND
     value
     ~*
     '^(-?[0-9]+)(\.[0-9]+)?$')                        AS "xoW9Ppf2Ol2",
  (SELECT CASE WHEN
    value
    =
    'true'
    THEN 1
          WHEN
            value
            =
            'false'
            THEN 0
          ELSE NULL END
   FROM
     trackedentitydatavalue
   WHERE
     programstageinstanceid
     =
     psi.programstageinstanceid
     AND
     dataelementid
     =
     7604)                                             AS "Cz8edRvdDc1",
  (SELECT CASE WHEN
    value
    =
    'true'
    THEN 1
          WHEN
            value
            =
            'false'
            THEN 0
          ELSE NULL END
   FROM
     trackedentitydatavalue
   WHERE
     programstageinstanceid
     =
     psi.programstageinstanceid
     AND
     dataelementid
     =
     7687)                                             AS "wmtwSSDlOi1",
  (SELECT cast(
      value
      AS
      DOUBLE PRECISION)
   FROM
     trackedentitydatavalue
   WHERE
     programstageinstanceid
     =
     psi.programstageinstanceid
     AND
     dataelementid
     =
     942712
     AND
     value
     ~*
     '^(-?[0-9]+)(\.[0-9]+)?$')                        AS "UTe1wkF3V1M",
  (SELECT cast(
      value
      AS
      DOUBLE PRECISION)
   FROM
     trackedentitydatavalue
   WHERE
     programstageinstanceid
     =
     psi.programstageinstanceid
     AND
     dataelementid
     =
     7573
     AND
     value
     ~*
     '^(-?[0-9]+)(\.[0-9]+)?$')                        AS "c1X4WGMlOiA",
  (SELECT CASE WHEN
    value
    =
    'true'
    THEN 1
          WHEN
            value
            =
            'false'
            THEN 0
          ELSE NULL END
   FROM
     trackedentitydatavalue
   WHERE
     programstageinstanceid
     =
     psi.programstageinstanceid
     AND
     dataelementid
     =
     7602)                                             AS "qIdlM7gECDs",
  (SELECT value
   FROM
     trackedentitydatavalue
   WHERE
     programstageinstanceid
     =
     psi.programstageinstanceid
     AND
     dataelementid
     =
     6961)                                             AS "uQMWKZUJBAy",
  (SELECT value
   FROM
     trackedentitydatavalue
   WHERE
     programstageinstanceid
     =
     psi.programstageinstanceid
     AND
     dataelementid
     =
     7591)                                             AS "rlX9tCrFUBd",
  (SELECT cast(
      value
      AS
      BIGINT)
   FROM
     trackedentitydatavalue
   WHERE
     programstageinstanceid
     =
     psi.programstageinstanceid
     AND
     dataelementid
     =
     7593
     AND
     value
     ~*
     '^(-?[0-9]+)(\.[0-9]+)?$')                        AS "Aivz2rZECCq",
  (SELECT value
   FROM
     trackedentitydatavalue
   WHERE
     programstageinstanceid
     =
     psi.programstageinstanceid
     AND
     dataelementid
     =
     7355)                                             AS "SddwwjFP44H",
  (SELECT CASE WHEN
    value
    =
    'true'
    THEN 1
          WHEN
            value
            =
            'false'
            THEN 0
          ELSE NULL END
   FROM
     trackedentitydatavalue
   WHERE
     programstageinstanceid
     =
     psi.programstageinstanceid
     AND
     dataelementid
     =
     7603)                                             AS "QotKBacqaXF",
  (SELECT CASE WHEN
    value
    =
    'true'
    THEN 1
          WHEN
            value
            =
            'false'
            THEN 0
          ELSE NULL END
   FROM
     trackedentitydatavalue
   WHERE
     programstageinstanceid
     =
     psi.programstageinstanceid
     AND
     dataelementid
     =
     7610)                                             AS "joIX8dTK2AO",
  (SELECT CASE WHEN
    value
    =
    'true'
    THEN 1
          WHEN
            value
            =
            'false'
            THEN 0
          ELSE NULL END
   FROM
     trackedentitydatavalue
   WHERE
     programstageinstanceid
     =
     psi.programstageinstanceid
     AND
     dataelementid
     =
     7599)                                             AS "RsYZ110xIJU",
  (SELECT value
   FROM
     trackedentitydatavalue
   WHERE
     programstageinstanceid
     =
     psi.programstageinstanceid
     AND
     dataelementid
     =
     6901)                                             AS "q0VFViKlSxR",
  (SELECT value
   FROM
     trackedentitydatavalue
   WHERE
     programstageinstanceid
     =
     psi.programstageinstanceid
     AND
     dataelementid
     =
     7310)                                             AS "NQYNJdLN8kW",
  (SELECT value
   FROM
     trackedentitydatavalue
   WHERE
     programstageinstanceid
     =
     psi.programstageinstanceid
     AND
     dataelementid
     =
     6894)                                             AS "RXs32L7F6k4",
  (SELECT CASE WHEN
    value
    =
    'true'
    THEN 1
          WHEN
            value
            =
            'false'
            THEN 0
          ELSE NULL END
   FROM
     trackedentitydatavalue
   WHERE
     programstageinstanceid
     =
     psi.programstageinstanceid
     AND
     dataelementid
     =
     7598)                                             AS "Dc5ZSduiU6N",
  (SELECT CASE WHEN
    value
    =
    'true'
    THEN 1
          WHEN
            value
            =
            'false'
            THEN 0
          ELSE NULL END
   FROM
     trackedentitydatavalue
   WHERE
     programstageinstanceid
     =
     psi.programstageinstanceid
     AND
     dataelementid
     =
     7685)                                             AS "GFOf0g1NVUE",
  (SELECT CASE WHEN
    value
    =
    'true'
    THEN 1
          WHEN
            value
            =
            'false'
            THEN 0
          ELSE NULL END
   FROM
     trackedentitydatavalue
   WHERE
     programstageinstanceid
     =
     psi.programstageinstanceid
     AND
     dataelementid
     =
     7595)                                             AS "HuITij8x3k5",
  (SELECT value
   FROM
     trackedentitydatavalue
   WHERE
     programstageinstanceid
     =
     psi.programstageinstanceid
     AND
     dataelementid
     =
     7576)                                             AS "x48LJJJ2qeR",
  (SELECT CASE WHEN
    value
    =
    'true'
    THEN 1
          WHEN
            value
            =
            'false'
            THEN 0
          ELSE NULL END
   FROM
     trackedentitydatavalue
   WHERE
     programstageinstanceid
     =
     psi.programstageinstanceid
     AND
     dataelementid
     =
     7611)                                             AS "I7eMlDQwlez",
  (SELECT value
   FROM
     trackedentitydatavalue
   WHERE
     programstageinstanceid
     =
     psi.programstageinstanceid
     AND
     dataelementid
     =
     6905)                                             AS "Z37L2JIf8FF",
  (SELECT value
   FROM
     trackedentitydatavalue
   WHERE
     programstageinstanceid
     =
     psi.programstageinstanceid
     AND
     dataelementid
     =
     6895)                                             AS "G3xVc5kvrfv",
  (SELECT value
   FROM
     trackedentitydatavalue
   WHERE
     programstageinstanceid
     =
     psi.programstageinstanceid
     AND
     dataelementid
     =
     7589)                                             AS "SxMQ9O5yDrp",
  (SELECT CASE WHEN
    value
    =
    'true'
    THEN 1
          WHEN
            value
            =
            'false'
            THEN 0
          ELSE NULL END
   FROM
     trackedentitydatavalue
   WHERE
     programstageinstanceid
     =
     psi.programstageinstanceid
     AND
     dataelementid
     =
     6913)                                             AS "eUHkkkwvCUC",
  (SELECT cast(
      value
      AS
      BIGINT)
   FROM
     trackedentitydatavalue
   WHERE
     programstageinstanceid
     =
     psi.programstageinstanceid
     AND
     dataelementid
     =
     7035
     AND
     value
     ~*
     '^(-?[0-9]+)(\.[0-9]+)?$')                        AS "IqL1dGOWrrB",
  (SELECT cast(
      value
      AS
      DOUBLE PRECISION)
   FROM
     trackedentitydatavalue
   WHERE
     programstageinstanceid
     =
     psi.programstageinstanceid
     AND
     dataelementid
     =
     7579
     AND
     value
     ~*
     '^(-?[0-9]+)(\.[0-9]+)?$')                        AS "ilQvDiovHGV",
  (SELECT cast(
      value
      AS
      TIMESTAMP)
   FROM
     trackedentitydatavalue
   WHERE
     programstageinstanceid
     =
     psi.programstageinstanceid
     AND
     dataelementid
     =
     6896)                                             AS "J4yiijg3a7w",
  (SELECT CASE WHEN
    value
    =
    'true'
    THEN 1
          WHEN
            value
            =
            'false'
            THEN 0
          ELSE NULL END
   FROM
     trackedentitydatavalue
   WHERE
     programstageinstanceid
     =
     psi.programstageinstanceid
     AND
     dataelementid
     =
     7686)                                             AS "hqIz7MkU7Et",
  (SELECT cast(
      value
      AS
      TIMESTAMP)
   FROM
     trackedentitydatavalue
   WHERE
     programstageinstanceid
     =
     psi.programstageinstanceid
     AND
     dataelementid
     =
     6903
     AND
     value
     ~*
     '^\d{4}-\d{2}-\d{2}(\s|T)?(\d{2}:\d{2}:\d{2})?$') AS "SU0Cl3VsvpX",
  (SELECT cast(
      value
      AS
      DOUBLE PRECISION)
   FROM
     trackedentitydatavalue
   WHERE
     programstageinstanceid
     =
     psi.programstageinstanceid
     AND
     dataelementid
     =
     7578
     AND
     value
     ~*
     '^(-?[0-9]+)(\.[0-9]+)?$')                        AS "hQV8NZrOBDj",
  (SELECT value
   FROM
     trackedentitydatavalue
   WHERE
     programstageinstanceid
     =
     psi.programstageinstanceid
     AND
     dataelementid
     =
     6899)                                             AS "yYMd2MDYJca",
  (SELECT value
   FROM
     trackedentitydatavalue
   WHERE
     programstageinstanceid
     =
     psi.programstageinstanceid
     AND
     dataelementid
     =
     7574)                                             AS "LUvrSEVJuln",
  (SELECT value
   FROM
     trackedentitydatavalue
   WHERE
     programstageinstanceid
     =
     psi.programstageinstanceid
     AND
     dataelementid
     =
     6907)                                             AS "D3rLWgTla8e",
  psi.uid,
  pi.uid,
  ps.uid,
  ao.uid,
  pi.enrollmentdate,
  pi.incidentdate,
  psi.executiondate,
  psi.duedate,
  psi.completeddate,
  pi.status,
  psi.status,
  psi.longitude,
  psi.latitude,
  ou.uid,
  ou.name,
  ou.code,
  (SELECT ST_SetSRID(
      ST_MakePoint(
          psi.longitude,
          psi.latitude),
      4326))                                           AS geom
FROM
  programstageinstance psi INNER JOIN
  programinstance pi
    ON
      psi.programinstanceid
      =
      pi.programinstanceid
  INNER JOIN
  programstage ps
    ON
      psi.programstageid
      =
      ps.programstageid
  INNER JOIN
  program pr
    ON
      pi.programid
      =
      pr.programid
  INNER JOIN
  categoryoptioncombo ao
    ON
      psi.attributeoptioncomboid
      =
      ao.categoryoptioncomboid
  LEFT JOIN
  trackedentityinstance tei
    ON
      pi.trackedentityinstanceid
      =
      tei.trackedentityinstanceid
  INNER JOIN
  organisationunit ou
    ON
      psi.organisationunitid
      =
      ou.organisationunitid
  LEFT JOIN
  _orgunitstructure ous
    ON
      psi.organisationunitid
      =
      ous.organisationunitid
  LEFT JOIN
  _organisationunitgroupsetstructure ougs
    ON
      psi.organisationunitid
      =
      ougs.organisationunitid
  INNER JOIN
  _categorystructure acs
    ON
      psi.attributeoptioncomboid
      =
      acs.categoryoptioncomboid
  LEFT JOIN
  _dateperiodstructure dps
    ON
      cast(
          psi.executiondate
          AS
          DATE)
      =
      dps.dateperiod
WHERE
  psi.executiondate
  >=
  '2015-01-01'
  AND
  psi.executiondate
  <=
  '2015-12-31'
  AND
  pr.programid
  =
  5
  AND
  psi.organisationunitid
  IS
  NOT
  NULL
  AND
  psi.executiondate
  IS
  NOT
  NULL
  AND
  psi.deleted
  IS
  FALSE