/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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

import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.datastatistics.AggregatedStatistics;
import org.hisp.dhis.datastatistics.DataStatistics;
import org.hisp.dhis.datastatistics.DataStatisticsStore;
import org.hisp.dhis.datastatistics.EventInterval;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.util.DateUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
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
      SessionFactory sessionFactory,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      CurrentUserService currentUserService,
      AclService aclService) {
    super(
        sessionFactory,
        jdbcTemplate,
        publisher,
        DataStatistics.class,
        currentUserService,
        aclService,
        false);
  }

  // -------------------------------------------------------------------------
  // DataStatisticsStore implementation
  // -------------------------------------------------------------------------

  @Override
  public List<AggregatedStatistics> getSnapshotsInInterval(
      EventInterval eventInterval, Date startDate, Date endDate) {
    final String sql = getQuery(eventInterval, startDate, endDate);

    log.debug("Get snapshots SQL: " + sql);

    return jdbcTemplate.query(
        sql,
        (resultSet, i) -> {
          AggregatedStatistics ads = new AggregatedStatistics();

          ads.setYear(resultSet.getInt("yr"));

          if (eventInterval == EventInterval.DAY) {
            ads.setDay(resultSet.getInt("day"));
            ads.setMonth(resultSet.getInt("mnt"));
          } else if (eventInterval == EventInterval.WEEK) {
            ads.setWeek(resultSet.getInt("week"));
          } else if (eventInterval == EventInterval.MONTH) {
            ads.setMonth(resultSet.getInt("mnt"));
          }

          ads.setMapViews(resultSet.getInt("mapViews"));
          ads.setVisualizationViews(resultSet.getInt("visualizationViews"));
          ads.setEventReportViews(resultSet.getInt("eventReportViews"));
          ads.setEventChartViews(resultSet.getInt("eventChartViews"));
          ads.setEventVisualizationViews(resultSet.getInt("eventVisualizationViews"));
          ads.setDashboardViews(resultSet.getInt("dashboardViews"));
          ads.setPassiveDashboardViews(resultSet.getInt("passiveDashboardViews"));
          ads.setDataSetReportViews(resultSet.getInt("dataSetReportViews"));
          ads.setTotalViews(resultSet.getInt("totalViews"));
          ads.setAverageViews(resultSet.getInt("averageViews"));
          ads.setAverageMapViews(resultSet.getInt("averageMapViews"));
          ads.setAverageVisualizationViews(resultSet.getInt("averageVisualizationViews"));
          ads.setAverageEventReportViews(resultSet.getInt("averageEventReportViews"));
          ads.setAverageEventChartViews(resultSet.getInt("averageEventChartViews"));
          ads.setAverageEventVisualizationViews(resultSet.getInt("averageEventVisualizationViews"));
          ads.setAverageDashboardViews(resultSet.getInt("averageDashboardViews"));
          ads.setAveragePassiveDashboardViews(resultSet.getInt("averagePassiveDashboardViews"));
          ads.setSavedMaps(resultSet.getInt("savedMaps"));
          ads.setSavedVisualizations(resultSet.getInt("savedVisualizations"));
          ads.setSavedEventReports(resultSet.getInt("savedEventReports"));
          ads.setSavedEventCharts(resultSet.getInt("savedEventCharts"));
          ads.setSavedEventVisualizations(resultSet.getInt("savedEventVisualizations"));
          ads.setSavedDashboards(resultSet.getInt("savedDashboards"));
          ads.setSavedIndicators(resultSet.getInt("savedIndicators"));
          ads.setSavedDataValues(resultSet.getInt("savedDataValues"));
          ads.setActiveUsers(resultSet.getInt("activeUsers"));
          ads.setUsers(resultSet.getInt("users"));

          return ads;
        });
  }

  private String getQuery(EventInterval eventInterval, Date startDate, Date endDate) {
    String sql;

    if (eventInterval == EventInterval.DAY) {
      sql = getDaySql(startDate, endDate);
    } else if (eventInterval == EventInterval.WEEK) {
      sql = getWeekSql(startDate, endDate);
    } else if (eventInterval == EventInterval.MONTH) {
      sql = getMonthSql(startDate, endDate);
    } else if (eventInterval == EventInterval.YEAR) {
      sql = getYearSql(startDate, endDate);
    } else {
      sql = getDaySql(startDate, endDate);
    }

    return sql;
  }

  /**
   * Creating a SQL for retrieving aggregated data with group by YEAR.
   *
   * @param start start date
   * @param end end date
   * @return SQL string
   */
  private String getYearSql(Date start, Date end) {
    return "select extract(year from created) as yr, "
        + getCommonSql(start, end)
        + "group by yr order by yr;";
  }

  /**
   * Creating a SQL for retrieving aggregated data with group by YEAR, MONTH.
   *
   * @param start start date
   * @param end end date
   * @return SQL string
   */
  private String getMonthSql(Date start, Date end) {
    return "select extract(year from created) as yr, "
        + "extract(month from created) as mnt, "
        + getCommonSql(start, end)
        + "group by yr, mnt order by yr, mnt;";
  }

  /**
   * Creating a SQL for retrieving aggregated data with group by YEAR, WEEK. Ignoring week 53.
   *
   * @param start start date
   * @param end end date
   * @return SQL string
   */
  private String getWeekSql(Date start, Date end) {
    return "select extract(year from created) as yr, "
        + "extract(week from created) as week, "
        + getCommonSql(start, end)
        + "and extract(week from created) < 53 "
        + "group by yr, week order by yr, week;";
  }

  /**
   * Creating a SQL for retrieving aggregated data with group by YEAR, DAY.
   *
   * @param start start date
   * @param end end date
   * @return SQL string
   */
  private String getDaySql(Date start, Date end) {
    return "select extract(year from created) as yr, "
        + "extract(month from created) as mnt,"
        + "extract(day from created) as day, "
        + getCommonSql(start, end)
        + "group by yr, mnt, day order by yr, mnt, day;";
  }

  /**
   * Part of SQL witch is always the same in the different intervals YEAR, MONTH, WEEK and DAY.
   *
   * @param start start date
   * @param end end date
   * @return SQL string
   */
  private String getCommonSql(Date start, Date end) {
    return "cast(round(cast(sum(mapviews) as numeric),0) as int) as mapViews,"
        + "cast(round(cast(sum(visualizationviews) as numeric),0) as int) as visualizationViews,"
        + "cast(round(cast(sum(eventreportviews) as numeric),0) as int) as eventReportViews, "
        + "cast(round(cast(sum(eventchartviews) as numeric),0) as int) as eventChartViews,"
        + "cast(round(cast(sum(eventvisualizationviews) as numeric),0) as int) as eventVisualizationViews,"
        + "cast(round(cast(sum(dashboardviews) as numeric),0) as int) as dashboardViews, "
        + "cast(round(cast(sum(passivedashboardviews) as numeric),0) as int) as passiveDashboardViews, "
        + "cast(round(cast(sum(datasetreportviews) as numeric),0) as int) as dataSetReportViews, "
        + "max(active_users) as activeUsers,"
        + "coalesce(sum(totalviews)/nullif(max(active_users), 0), 0) as averageViews,"
        + "coalesce(sum(mapviews)/nullif(max(active_users), 0), 0) as averageMapViews, "
        + "coalesce(sum(visualizationviews)/nullif(max(active_users), 0), 0) as averageVisualizationViews, "
        + "coalesce(sum(eventreportviews)/nullif(max(active_users), 0), 0) as averageEventReportViews, "
        + "coalesce(sum(eventchartviews)/nullif(max(active_users), 0), 0) as averageEventChartViews, "
        + "coalesce(sum(eventvisualizationviews)/nullif(max(active_users), 0), 0) as averageEventVisualizationViews, "
        + "coalesce(sum(dashboardviews)/nullif(max(active_users), 0), 0) as averageDashboardViews, "
        + "coalesce(sum(passivedashboardviews)/nullif(max(active_users), 0), 0) as averagePassiveDashboardViews, "
        + "cast(round(cast(sum(totalviews) as numeric),0) as int) as totalViews,"
        + "cast(round(cast(sum(maps) as numeric),0) as int) as savedMaps,"
        + "cast(round(cast(sum(visualizations) as numeric),0) as int) as savedVisualizations,"
        + "cast(round(cast(sum(eventreports) as numeric),0) as int) as savedEventReports,"
        + "cast(round(cast(sum(eventcharts) as numeric),0) as int) as savedEventCharts,"
        + "cast(round(cast(sum(eventvisualizations) as numeric),0) as int) as savedEventVisualizations,"
        + "cast(round(cast(sum(dashboards) as numeric),0) as int) as savedDashboards, "
        + "cast(round(cast(sum(indicators) as numeric),0) as int) as savedIndicators,"
        + "cast(round(cast(sum(datavalues) as numeric),0) as int) as savedDataValues,"
        + "max(users) as users from datastatistics "
        + "where created >= '"
        + DateUtils.getLongDateString(start)
        + "' "
        + "and created <= '"
        + DateUtils.getLongDateString(end)
        + "' ";
  }
}
