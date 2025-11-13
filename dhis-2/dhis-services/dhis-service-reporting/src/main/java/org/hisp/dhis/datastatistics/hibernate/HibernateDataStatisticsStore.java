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

    final String sql = getQuery(eventInterval);

    log.debug("Get snapshots SQL: {}", sql);

    PreparedStatementSetter pss =
        ps -> {
          ps.setTimestamp(1, new java.sql.Timestamp(startDate.getTime()));
          ps.setTimestamp(2, new java.sql.Timestamp(endDate.getTime()));
        };

    return jdbcTemplate.query(
        sql,
        pss,
        (rs, i) -> {

          // period fields are nullable
          Integer year = null;
          Integer month = null;
          Integer week = null;
          Integer day = null;

          switch (eventInterval) {
            case YEAR -> year = getNullableInt(rs, "yr");
            case MONTH -> {
              year = getNullableInt(rs, "yr");
              month = getNullableInt(rs, "mnt");
            }
            case WEEK -> {
              year = getNullableInt(rs, "isoyear");
              week = getNullableInt(rs, "wk");
            }
            case DAY -> {
              year = getNullableInt(rs, "yr");
              month = getNullableInt(rs, "mnt");
              day = getNullableInt(rs, "day");
            }
          }

          return new AggregatedStatistics(
              year,
              month,
              week,
              day,

              // counts (BIGINT → Long)
              rs.getLong("mapViews"),
              rs.getLong("visualizationViews"),
              rs.getLong("eventReportViews"),
              rs.getLong("eventChartViews"),
              rs.getLong("eventVisualizationViews"),
              rs.getLong("dashboardViews"),
              rs.getLong("passiveDashboardViews"),
              rs.getLong("dataSetReportViews"),
              rs.getLong("totalViews"),
              rs.getDouble("averageViews"),
              rs.getDouble("averageMapViews"),
              rs.getDouble("averageVisualizationViews"),
              rs.getDouble("averageEventReportViews"),
              rs.getDouble("averageEventChartViews"),
              rs.getDouble("averageEventVisualizationViews"),
              rs.getDouble("averageDashboardViews"),
              rs.getDouble("averagePassiveDashboardViews"),
              rs.getLong("savedMaps"),
              rs.getLong("savedVisualizations"),
              rs.getLong("savedEventReports"),
              rs.getLong("savedEventCharts"),
              rs.getLong("savedEventVisualizations"),
              rs.getLong("savedDashboards"),
              rs.getLong("savedIndicators"),
              rs.getLong("savedDataValues"),

              // users (BIGINT → Long)
              rs.getLong("activeUsers"),
              rs.getLong("users"));
        });
  }

  private static String byYearSql() {
    return """
        WITH filtered AS (
            SELECT *
            FROM datastatistics
            WHERE created >= ? AND created <= ?
        ),
        bucketed AS (
            SELECT date_trunc('year', created) AS period, *
            FROM filtered
        )
        SELECT period,
               EXTRACT(YEAR FROM period)::int AS yr,
               %s
        FROM bucketed
        GROUP BY period, yr
        ORDER BY period
        """
        .formatted(COMMON_SELECT_LIST);
  }

  private static String byMonthSql() {
    return """
        WITH filtered AS (
            SELECT *
            FROM datastatistics
            WHERE created >= ? AND created <= ?
        ),
        bucketed AS (
            SELECT date_trunc('month', created) AS period, *
            FROM filtered
        )
        SELECT period,
               EXTRACT(YEAR  FROM period)::int AS yr,
               EXTRACT(MONTH FROM period)::int AS mnt,
               %s
        FROM bucketed
        GROUP BY period, yr, mnt
        ORDER BY period
        """
        .formatted(COMMON_SELECT_LIST);
  }

  private static String byWeekSql() {
    return """
        WITH filtered AS (
            SELECT *
            FROM datastatistics
            WHERE created >= ? AND created <= ?
        ),
        bucketed AS (
            SELECT date_trunc('week', created) AS period, *
            FROM filtered
        )
        SELECT period,
               EXTRACT(ISOYEAR FROM period)::int AS isoyear,
               EXTRACT(WEEK    FROM period)::int AS wk,
               %s
        FROM bucketed
        GROUP BY period, isoyear, wk
        ORDER BY period
        """
        .formatted(COMMON_SELECT_LIST);
  }

  private static String byDaySql() {
    return """
        WITH filtered AS (
            SELECT *
            FROM datastatistics
            WHERE created >= ? AND created <= ?
        ),
        bucketed AS (
            SELECT date_trunc('day', created) AS period, *
            FROM filtered
        )
        SELECT period,
               EXTRACT(YEAR  FROM period)::int AS yr,
               EXTRACT(MONTH FROM period)::int AS mnt,
               EXTRACT(DAY   FROM period)::int AS day,
               %s
        FROM bucketed
        GROUP BY period, yr, mnt, day
        ORDER BY period
        """
        .formatted(COMMON_SELECT_LIST);
  }

  private String getQuery(EventInterval interval) {
    return switch (interval) {
      case WEEK -> byWeekSql();
      case MONTH -> byMonthSql();
      case YEAR -> byYearSql();
      default -> byDaySql();
    };
  }

  static final String COMMON_SELECT_LIST =
      """
        COALESCE(SUM(mapviews), 0)::bigint                AS mapViews,
        COALESCE(SUM(visualizationviews), 0)::bigint      AS visualizationViews,
        COALESCE(SUM(eventreportviews), 0)::bigint        AS eventReportViews,
        COALESCE(SUM(eventchartviews), 0)::bigint         AS eventChartViews,
        COALESCE(SUM(eventvisualizationviews), 0)::bigint AS eventVisualizationViews,
        COALESCE(SUM(dashboardviews), 0)::bigint          AS dashboardViews,
        COALESCE(SUM(passivedashboardviews), 0)::bigint   AS passiveDashboardViews,
        COALESCE(SUM(datasetreportviews), 0)::bigint      AS dataSetReportViews,
        COALESCE(MAX(active_users), 0)                    AS activeUsers,
        COALESCE(SUM(totalviews)::double precision              / NULLIF(MAX(active_users), 0), 0) AS averageViews,
        COALESCE(SUM(mapviews)::double precision                / NULLIF(MAX(active_users), 0), 0) AS averageMapViews,
        COALESCE(SUM(visualizationviews)::double precision      / NULLIF(MAX(active_users), 0), 0) AS averageVisualizationViews,
        COALESCE(SUM(eventreportviews)::double precision        / NULLIF(MAX(active_users), 0), 0) AS averageEventReportViews,
        COALESCE(SUM(eventchartviews)::double precision         / NULLIF(MAX(active_users), 0), 0) AS averageEventChartViews,
        COALESCE(SUM(eventvisualizationviews)::double precision / NULLIF(MAX(active_users), 0), 0) AS averageEventVisualizationViews,
        COALESCE(SUM(dashboardviews)::double precision          / NULLIF(MAX(active_users), 0), 0) AS averageDashboardViews,
        COALESCE(SUM(passivedashboardviews)::double precision   / NULLIF(MAX(active_users), 0), 0) AS averagePassiveDashboardViews,
        COALESCE(SUM(totalviews),0)::bigint              AS totalViews,
        COALESCE(SUM(maps),0)::bigint                    AS savedMaps,
        COALESCE(SUM(visualizations),0)::bigint          AS savedVisualizations,
        COALESCE(SUM(eventreports),0)::bigint            AS savedEventReports,
        COALESCE(SUM(eventcharts),0)::bigint             AS savedEventCharts,
        COALESCE(SUM(eventvisualizations),0)::bigint     AS savedEventVisualizations,
        COALESCE(SUM(dashboards),0)::bigint              AS savedDashboards,
        COALESCE(SUM(indicators),0)::bigint              AS savedIndicators,
        COALESCE(SUM(datavalues),0)::bigint              AS savedDataValues,
        COALESCE(MAX(users),0)::bigint                           AS users
        """;

  private static Integer getNullableInt(ResultSet rs, String column) throws SQLException {
    int value = rs.getInt(column);
    return rs.wasNull() ? null : value;
  }
}
