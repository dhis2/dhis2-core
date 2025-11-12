ALTER TABLE datastatistics DROP COLUMN IF EXISTS indicatorviews;

ALTER TABLE datastatistics
  ALTER COLUMN mapviews                TYPE bigint USING round(mapviews)::bigint,
  ALTER COLUMN visualizationviews      TYPE bigint USING round(visualizationviews)::bigint,
  ALTER COLUMN eventreportviews        TYPE bigint USING round(eventreportviews)::bigint,
  ALTER COLUMN eventchartviews         TYPE bigint USING round(eventchartviews)::bigint,
  ALTER COLUMN eventvisualizationviews TYPE bigint USING round(eventvisualizationviews)::bigint,
  ALTER COLUMN dashboardviews          TYPE bigint USING round(dashboardviews)::bigint,
  ALTER COLUMN passivedashboardviews   TYPE bigint USING round(passivedashboardviews)::bigint,
  ALTER COLUMN datasetreportviews      TYPE bigint USING round(datasetreportviews)::bigint,
  ALTER COLUMN totalviews              TYPE bigint USING round(totalviews)::bigint,
  ALTER COLUMN active_users            TYPE bigint USING round(active_users)::bigint,
  ALTER COLUMN users                   TYPE bigint USING round(users)::bigint;


ALTER TABLE datastatistics
  ALTER COLUMN maps                    TYPE bigint USING round(maps)::bigint,
  ALTER COLUMN visualizations          TYPE bigint USING round(visualizations)::bigint,
  ALTER COLUMN eventreports            TYPE bigint USING round(eventreports)::bigint,
  ALTER COLUMN eventcharts             TYPE bigint USING round(eventcharts)::bigint,
  ALTER COLUMN eventvisualizations     TYPE bigint USING round(eventvisualizations)::bigint,
  ALTER COLUMN dashboards              TYPE bigint USING round(dashboards)::bigint,
  ALTER COLUMN indicators              TYPE bigint USING round(indicators)::bigint,
  ALTER COLUMN datavalues              TYPE bigint USING round(datavalues)::bigint;