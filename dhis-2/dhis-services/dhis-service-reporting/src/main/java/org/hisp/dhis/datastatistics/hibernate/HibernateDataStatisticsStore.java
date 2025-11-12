/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.datastatistics.hibernate;

import jakarta.persistence.EntityManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.datastatistics.AggregatedStatistics;
import org.hisp.dhis.datastatistics.DataStatistics;
import org.hisp.dhis.datastatistics.DataStatisticsStore;
import org.hisp.dhis.datastatistics.EventInterval;
import org.hisp.dhis.security.acl.AclService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.stereotype.Repository;

/**
 * @author Yrjan A. F. Fraschetti
 * @author Julie Hill Roa
 */
@Slf4j
@Repository("org.hisp.dhis.datastatistics.DataStatisticsStore")
public class HibernateDataStatisticsStore extends HibernateIdentifiableObjectStore<DataStatistics>
    implements DataStatisticsStore {

  public HibernateDataStatisticsStore(
      EntityManager entityManager,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      AclService aclService) {
    super(entityManager, jdbcTemplate, publisher, DataStatistics.class, aclService, false);
  }

  // -------------------------------------------------------------------------
  // DataStatisticsStore implementation
  // -------------------------------------------------------------------------

  @Override
  public List<AggregatedStatistics> getSnapshotsInInterval(
      EventInterval eventInterval, Date startDate, Date endDate) {

    final String sql = getQuery(eventInterval); // uses the CTE with ? ? placeholders

    log.debug("Get snapshots SQL: {}", sql);

    PreparedStatementSetter pss =
        ps -> {
          ps.setTimestamp(1, new java.sql.Timestamp(startDate.getTime())); // inclusive
          ps.setTimestamp(2, new java.sql.Timestamp(endDate.getTime())); // exclusive
        };

    return jdbcTemplate.query(
        sql,
        pss,
        (rs, i) -> {
          AggregatedStatistics ads = new AggregatedStatistics();

          if (hasColumn(rs, "yr")) ads.setYear(rs.getInt("yr"));
          switch (eventInterval) {
            case DAY -> {
              ads.setMonth(rs.getInt("mnt"));
              ads.setDay(rs.getInt("day"));
            }
            case WEEK -> {
              ads.setYear(rs.getInt("isoyear"));
              ads.setWeek(rs.getInt("wk"));
            }
            case MONTH -> ads.setMonth(rs.getInt("mnt"));
            default -> {
              /* YEAR already handled via yr */
            }
          }

          // counts (BIGINT) -> long
          ads.setMapViews(rs.getLong("mapViews"));
          ads.setVisualizationViews(rs.getLong("visualizationViews"));
          ads.setEventReportViews(rs.getLong("eventReportViews"));
          ads.setEventChartViews(rs.getLong("eventChartViews"));
          ads.setEventVisualizationViews(rs.getLong("eventVisualizationViews"));
          ads.setDashboardViews(rs.getLong("dashboardViews"));
          ads.setPassiveDashboardViews(rs.getLong("passiveDashboardViews"));
          ads.setDataSetReportViews(rs.getLong("dataSetReportViews"));
          ads.setTotalViews(rs.getLong("totalViews"));
          ads.setSavedMaps(rs.getLong("savedMaps"));
          ads.setSavedVisualizations(rs.getLong("savedVisualizations"));
          ads.setSavedEventReports(rs.getLong("savedEventReports"));
          ads.setSavedEventCharts(rs.getLong("savedEventCharts"));
          ads.setSavedEventVisualizations(rs.getLong("savedEventVisualizations"));
          ads.setSavedDashboards(rs.getLong("savedDashboards"));
          ads.setSavedIndicators(rs.getLong("savedIndicators"));
          ads.setSavedDataValues(rs.getLong("savedDataValues"));

          // averages (double precision)
          ads.setAverageViews(rs.getDouble("averageViews"));
          ads.setAverageMapViews(rs.getDouble("averageMapViews"));
          ads.setAverageVisualizationViews(rs.getDouble("averageVisualizationViews"));
          ads.setAverageEventReportViews(rs.getDouble("averageEventReportViews"));
          ads.setAverageEventChartViews(rs.getDouble("averageEventChartViews"));
          ads.setAverageEventVisualizationViews(rs.getDouble("averageEventVisualizationViews"));
          ads.setAverageDashboardViews(rs.getDouble("averageDashboardViews"));
          ads.setAveragePassiveDashboardViews(rs.getDouble("averagePassiveDashboardViews"));

          // users
          ads.setActiveUsers(rs.getLong("activeUsers"));
          ads.setUsers(rs.getLong("users"));

          return ads;
        });
  }

  static final String BASE_CTE =
"""
WITH filtered AS (
  SELECT *
  FROM datastatistics
  WHERE created >= ? AND created <= ?
)
""";

  private static String byYearSql() {
    return BASE_CTE
        + "SELECT\n"
        + "  date_trunc('year', created) AS period,\n"
        + "  EXTRACT(YEAR FROM date_trunc('year', created))::int AS yr,\n"
        + commonSelectList()
        + "\n"
        + "FROM filtered\n"
        + "GROUP BY period, yr\n"
        + "ORDER BY period";
  }

  private static String byMonthSql() {
    return BASE_CTE
        + ", bucketed AS (\n"
        + "  SELECT date_trunc('month', created) AS period, *\n"
        + "  FROM filtered\n"
        + ")\n"
        + "SELECT period,\n"
        + "       EXTRACT(YEAR  FROM period)::int AS yr,\n"
        + "       EXTRACT(MONTH FROM period)::int AS mnt,\n"
        + commonSelectList()
        + "\n"
        + "FROM bucketed\n"
        + "GROUP BY period, yr, mnt\n"
        + "ORDER BY period";
  }

  private static String byWeekSql() {
    return BASE_CTE
        + ", bucketed AS (\n"
        + "  SELECT date_trunc('week', created) AS period, *\n"
        + "  FROM filtered\n"
        + ")\n"
        + "SELECT period,\n"
        + "       EXTRACT(ISOYEAR FROM period)::int AS isoyear,\n"
        + "       EXTRACT(WEEK    FROM period)::int AS wk,\n"
        + commonSelectList()
        + "\n"
        + "FROM bucketed\n"
        + "GROUP BY period, isoyear, wk\n"
        + "ORDER BY period";
  }

  private static String byDaySql() {
    return BASE_CTE
        + ", bucketed AS (\n"
        + "  SELECT date_trunc('day', created) AS period, *\n"
        + "  FROM filtered\n"
        + ")\n"
        + "SELECT period,\n"
        + "       EXTRACT(YEAR  FROM period)::int  AS yr,\n"
        + "       EXTRACT(MONTH FROM period)::int  AS mnt,\n"
        + "       EXTRACT(DAY   FROM period)::int  AS day,\n"
        + commonSelectList()
        + "\n"
        + "FROM bucketed\n"
        + "GROUP BY period, yr, mnt, day\n"
        + "ORDER BY period";
  }

  private String getQuery(EventInterval interval) {
    return switch (interval) {
      case WEEK -> byWeekSql();
      case MONTH -> byMonthSql();
      case YEAR -> byYearSql();
      default -> byDaySql();
    };
  }

  private static String commonSelectList() {
    return String.join(
        ", ",
        "SUM(mapviews)::bigint                AS mapViews",
        "SUM(visualizationviews)::bigint      AS visualizationViews",
        "SUM(eventreportviews)::bigint        AS eventReportViews",
        "SUM(eventchartviews)::bigint         AS eventChartViews",
        "SUM(eventvisualizationviews)::bigint AS eventVisualizationViews",
        "SUM(dashboardviews)::bigint          AS dashboardViews",
        "SUM(passivedashboardviews)::bigint   AS passiveDashboardViews",
        "SUM(datasetreportviews)::bigint      AS dataSetReportViews",
        "MAX(active_users)                    AS activeUsers",
        "COALESCE(SUM(totalviews)::double precision              / NULLIF(MAX(active_users), 0), 0) AS averageViews",
        "COALESCE(SUM(mapviews)::double precision                / NULLIF(MAX(active_users), 0), 0) AS averageMapViews",
        "COALESCE(SUM(visualizationviews)::double precision      / NULLIF(MAX(active_users), 0), 0) AS averageVisualizationViews",
        "COALESCE(SUM(eventreportviews)::double precision        / NULLIF(MAX(active_users), 0), 0) AS averageEventReportViews",
        "COALESCE(SUM(eventchartviews)::double precision         / NULLIF(MAX(active_users), 0), 0) AS averageEventChartViews",
        "COALESCE(SUM(eventvisualizationviews)::double precision / NULLIF(MAX(active_users), 0), 0) AS averageEventVisualizationViews",
        "COALESCE(SUM(dashboardviews)::double precision          / NULLIF(MAX(active_users), 0), 0) AS averageDashboardViews",
        "COALESCE(SUM(passivedashboardviews)::double precision   / NULLIF(MAX(active_users), 0), 0) AS averagePassiveDashboardViews",
        "SUM(totalviews)::bigint              AS totalViews",
        "SUM(maps)::bigint                    AS savedMaps",
        "SUM(visualizations)::bigint          AS savedVisualizations",
        "SUM(eventreports)::bigint            AS savedEventReports",
        "SUM(eventcharts)::bigint             AS savedEventCharts",
        "SUM(eventvisualizations)::bigint     AS savedEventVisualizations",
        "SUM(dashboards)::bigint              AS savedDashboards",
        "SUM(indicators)::bigint              AS savedIndicators",
        "SUM(datavalues)::bigint              AS savedDataValues",
        "MAX(users)                           AS users");
  }

  private static boolean hasColumn(ResultSet rs, String name) {
    try {
      rs.findColumn(name);
      return true;
    } catch (SQLException e) {
      return false;
    }
  }
}
