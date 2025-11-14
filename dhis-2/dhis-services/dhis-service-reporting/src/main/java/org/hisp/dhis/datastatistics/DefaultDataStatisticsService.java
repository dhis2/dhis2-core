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
package org.hisp.dhis.datastatistics;

import static org.hisp.dhis.scheduling.JobProgress.FailurePolicy.SKIP_STAGE;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.SortOrder;
import org.hisp.dhis.common.Dhis2Info;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.datasummary.DataSummary;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.eventvisualization.EventVisualizationStore;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.SingleEvent;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.statistics.StatisticsProvider;
import org.hisp.dhis.system.SystemInfo.SystemInfoForDataStats;
import org.hisp.dhis.system.SystemService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserInvitationStatus;
import org.hisp.dhis.user.UserQueryParams;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.visualization.Visualization;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Yrjan A. F. Fraschetti
 * @author Julie Hill Roa
 */
@RequiredArgsConstructor
@Service("org.hisp.dhis.datastatistics.DataStatisticsService")
@Transactional
public class DefaultDataStatisticsService implements DataStatisticsService {
  private final DataStatisticsStore dataStatisticsStore;

  private final DataStatisticsEventStore dataStatisticsEventStore;

  private final UserService userService;

  private final IdentifiableObjectManager idObjectManager;

  private final DataValueService dataValueService;

  private final StatisticsProvider statisticsProvider;

  private final EventVisualizationStore eventVisualizationStore;

  private final SystemService systemService;

  private static final ZoneId SERVER_ZONE = ZoneId.systemDefault();

  // -------------------------------------------------------------------------
  // DataStatisticsService implementation
  // -------------------------------------------------------------------------

  @Override
  public int addEvent(DataStatisticsEvent event) {
    dataStatisticsEventStore.save(event);

    return event.getId();
  }

  @Override
  public List<AggregatedStatistics> getReports(
      Date startDate, Date endDate, EventInterval eventInterval) {
    return dataStatisticsStore.getSnapshotsInInterval(eventInterval, startDate, endDate);
  }

  /**
   * Generates a snapshot of data statistics for a given day.
   *
   * @param day The date for which the snapshot is generated.
   * @param progress The JobProgress object used to track the progress of the operation.
   * @return A DataStatistics object containing the aggregated statistics for the specified day.
   */
  private DataStatistics getDataStatisticsSnapshot(Date day, JobProgress progress) {
    Date startDate = getStartDate(day);
    int daysElapsedSince = daysElapsedSince(startDate);

    Integer errorValue = null;
    progress.startingStage("Counting maps", SKIP_STAGE);
    Integer savedMaps =
        progress.runStage(
            errorValue,
            () -> idObjectManager.getCountByCreated(org.hisp.dhis.mapping.Map.class, startDate));
    progress.startingStage("Counting visualisations", SKIP_STAGE);
    Integer savedVisualizations =
        progress.runStage(
            errorValue, () -> idObjectManager.getCountByCreated(Visualization.class, startDate));
    progress.startingStage("Counting event reports", SKIP_STAGE);
    Integer savedEventReports =
        progress.runStage(errorValue, () -> eventVisualizationStore.countReportsCreated(startDate));
    progress.startingStage("Counting event charts", SKIP_STAGE);
    Integer savedEventCharts =
        progress.runStage(errorValue, () -> eventVisualizationStore.countChartsCreated(startDate));
    progress.startingStage("Counting event visualisations", SKIP_STAGE);
    Integer savedEventVisualizations =
        progress.runStage(
            errorValue, () -> eventVisualizationStore.countEventVisualizationsCreated(startDate));
    progress.startingStage("Counting dashboards", SKIP_STAGE);
    Integer savedDashboards =
        progress.runStage(
            errorValue, () -> idObjectManager.getCountByCreated(Dashboard.class, startDate));
    progress.startingStage("Counting indicators", SKIP_STAGE);
    Integer savedIndicators =
        progress.runStage(
            errorValue, () -> idObjectManager.getCountByCreated(Indicator.class, startDate));
    progress.startingStage("Counting data values", SKIP_STAGE);
    Integer savedDataValues =
        progress.runStage(errorValue, () -> dataValueService.getDataValueCount(daysElapsedSince));
    progress.startingStage("Counting users", SKIP_STAGE);
    Integer users = progress.runStage(errorValue, () -> idObjectManager.getCount(User.class));
    progress.startingStage("Counting views", SKIP_STAGE);
    Map<DataStatisticsEventType, Long> eventCountMap =
        progress.runStage(
            Map.of(), () -> dataStatisticsEventStore.getDataStatisticsEventCount(startDate, day));

    return new DataStatistics(
        getOrZero(eventCountMap, DataStatisticsEventType.MAP_VIEW),
        getOrZero(eventCountMap, DataStatisticsEventType.VISUALIZATION_VIEW),
        getOrZero(eventCountMap, DataStatisticsEventType.EVENT_REPORT_VIEW),
        getOrZero(eventCountMap, DataStatisticsEventType.EVENT_CHART_VIEW),
        getOrZero(eventCountMap, DataStatisticsEventType.EVENT_VISUALIZATION_VIEW),
        getOrZero(eventCountMap, DataStatisticsEventType.DASHBOARD_VIEW),
        getOrZero(eventCountMap, DataStatisticsEventType.PASSIVE_DASHBOARD_VIEW),
        getOrZero(eventCountMap, DataStatisticsEventType.DATA_SET_REPORT_VIEW),
        getOrZero(eventCountMap, DataStatisticsEventType.TOTAL_VIEW),
        savedMaps,
        savedVisualizations,
        savedEventReports,
        savedEventCharts,
        savedEventVisualizations,
        savedDashboards,
        savedIndicators,
        savedDataValues,
        (int) getOrZero(eventCountMap, DataStatisticsEventType.ACTIVE_USERS),
        users);
  }

  @Override
  public long saveDataStatistics(DataStatistics dataStatistics) {
    dataStatisticsStore.save(dataStatistics);

    return dataStatistics.getId();
  }

  @Override
  public long saveDataStatisticsSnapshot(JobProgress progress) {
    return saveDataStatistics(getDataStatisticsSnapshot(new Date(), progress));
  }

  @Override
  public List<FavoriteStatistics> getTopFavorites(
      DataStatisticsEventType eventType, int pageSize, SortOrder sortOrder, String username) {
    return dataStatisticsEventStore.getFavoritesData(eventType, pageSize, sortOrder, username);
  }

  @Override
  public FavoriteStatistics getFavoriteStatistics(String uid) {
    return dataStatisticsEventStore.getFavoriteStatistics(uid);
  }

  /**
   * Generates a summary of system statistics including object counts, active users, user
   * invitations, data value counts, tracker event counts, single event counts, enrollment counts,
   * and system information.
   *
   * @return A DataSummary object containing the system statistics summary.
   */
  @Override
  public DataSummary getSystemStatisticsSummary() {
    DataSummary statistics = new DataSummary();

    // Database objects
    Map<String, Long> objectCounts = new HashMap<>();
    statisticsProvider
        .getObjectCounts()
        .forEach((object, count) -> objectCounts.put(object.getValue(), count));

    statistics.setObjectCounts(objectCounts);

    // Active users
    Map<Integer, Integer> activeUsers =
        Map.ofEntries(
            Map.entry(0, userService.getActiveUsersCount(hoursAgo(1))),
            Map.entry(1, userService.getActiveUsersCount(startOfToday())),
            Map.entry(2, userService.getActiveUsersCount(daysAgo(2))),
            Map.entry(7, userService.getActiveUsersCount(daysAgo(7))),
            Map.entry(30, userService.getActiveUsersCount(daysAgo(30))));
    statistics.setActiveUsers(activeUsers);

    // User invitations
    Map<String, Integer> userInvitations = new HashMap<>();

    UserQueryParams inviteAll = new UserQueryParams();
    inviteAll.setInvitationStatus(UserInvitationStatus.ALL);
    userInvitations.put(UserInvitationStatus.ALL.getValue(), userService.getUserCount(inviteAll));

    UserQueryParams inviteExpired = new UserQueryParams();
    inviteExpired.setInvitationStatus(UserInvitationStatus.EXPIRED);
    userInvitations.put(
        UserInvitationStatus.EXPIRED.getValue(), userService.getUserCount(inviteExpired));

    statistics.setUserInvitations(userInvitations);

    Map<Integer, Integer> dataValueCount =
        Map.ofEntries(
            Map.entry(0, dataValueService.getDataValueCountLastUpdatedAfter(hoursAgo(1), true)),
            Map.entry(1, dataValueService.getDataValueCountLastUpdatedAfter(daysAgo(1), true)),
            Map.entry(7, dataValueService.getDataValueCountLastUpdatedAfter(daysAgo(7), true)),
            Map.entry(30, dataValueService.getDataValueCountLastUpdatedAfter(daysAgo(30), true)));
    statistics.setDataValueCount(dataValueCount);

    Map<Integer, Long> trackerEventCount =
        Map.ofEntries(
            Map.entry(
                0, (long) idObjectManager.getCountByLastUpdated(TrackerEvent.class, hoursAgo(1))),
            Map.entry(
                1, (long) idObjectManager.getCountByLastUpdated(TrackerEvent.class, daysAgo(1))),
            Map.entry(
                7, (long) idObjectManager.getCountByLastUpdated(TrackerEvent.class, daysAgo(7))),
            Map.entry(
                30, (long) idObjectManager.getCountByLastUpdated(TrackerEvent.class, daysAgo(30))));
    statistics.setTrackerEventCount(trackerEventCount);

    Map<Integer, Long> singleEventCount =
        Map.ofEntries(
            Map.entry(
                0, (long) idObjectManager.getCountByLastUpdated(SingleEvent.class, hoursAgo(1))),
            Map.entry(
                1, (long) idObjectManager.getCountByLastUpdated(SingleEvent.class, daysAgo(1))),
            Map.entry(
                7, (long) idObjectManager.getCountByLastUpdated(SingleEvent.class, daysAgo(7))),
            Map.entry(
                30, (long) idObjectManager.getCountByLastUpdated(SingleEvent.class, daysAgo(30))));
    statistics.setSingleEventCount(Map.copyOf(singleEventCount));

    Map<Integer, Long> eventCount = new HashMap<>(trackerEventCount);
    singleEventCount.forEach((k, v) -> eventCount.merge(k, v, Long::sum));
    statistics.setEventCount(Map.copyOf(eventCount));

    Map<Integer, Long> enrollmentCount =
        Map.ofEntries(
            Map.entry(
                0, (long) idObjectManager.getCountByLastUpdated(Enrollment.class, hoursAgo(1))),
            Map.entry(
                1, (long) idObjectManager.getCountByLastUpdated(Enrollment.class, daysAgo(1))),
            Map.entry(
                7, (long) idObjectManager.getCountByLastUpdated(Enrollment.class, daysAgo(7))),
            Map.entry(
                30, (long) idObjectManager.getCountByLastUpdated(Enrollment.class, daysAgo(30))));
    statistics.setEnrollmentCount(Map.copyOf(enrollmentCount));

    statistics.setSystem(getDhis2Info());

    return statistics;
  }

  /*---------------------------------------------------------------------------
     // Supportive methods
     // -------------------------------------------------------------------------
  */

  /**
   * Returns the value associated with the specified DataStatisticsEventType from the map, or zero
   * if the type is not present in the map.
   *
   * @param map The map containing DataStatisticsEventType keys and their associated Long values.
   * @param type The DataStatisticsEventType for which the value is to be retrieved.
   * @return The Long value associated with the specified type, or zero if the type is
   */
  private static long getOrZero(
      Map<DataStatisticsEventType, Long> map, DataStatisticsEventType type) {
    return map.getOrDefault(type, 0L);
  }

  /**
   * Returns the start of today as a Date object. Note that this method uses the server's default
   * time zone to determine the start of the day. Note that this may not necessarily start at
   * exactly 00:00 hours due to time zone differences.
   *
   * @return A Date object representing the start of today.
   */
  private static Date startOfToday() {
    ZonedDateTime startOfDay = LocalDate.now(SERVER_ZONE).atStartOfDay(SERVER_ZONE);
    return Date.from(startOfDay.toInstant());
  }

  /**
   * Returns a Date object representing the date and time a specified number of days ago from the
   * current date and time. Note that this method uses the server's default time zone. Due to time
   * zone differences, the exact number of hours may vary.
   *
   * @param d Number of days ago
   * @return A Date object representing the date and time d days ago.
   */
  private static Date daysAgo(int d) {
    return Date.from(ZonedDateTime.now(SERVER_ZONE).minusDays(d).toInstant());
  }

  /**
   * Returns a Date object representing the date and time a specified number of hours ago from the
   * current date and time. Note that this method uses the server's default time zone.
   *
   * @param h Number of hours ago
   * @return A Date object representing the date and time h hours ago.
   */
  private static Date hoursAgo(int h) {
    return Date.from(ZonedDateTime.now(SERVER_ZONE).minusHours(h).toInstant());
  }

  /**
   * Returns the start date which is one day before the given day.
   *
   * @param day The date for which the start date is to be calculated.
   * @return A Date object representing the start date, which is one day before the given day.
   */
  private Date getStartDate(Date day) {
    return Date.from(day.toInstant().atZone(SERVER_ZONE).minusDays(1).toInstant());
  }

  private int daysElapsedSince(Date startDate) {
    LocalDate start = startDate.toInstant().atZone(SERVER_ZONE).toLocalDate();
    LocalDate today = LocalDate.now(SERVER_ZONE);
    return (int) ChronoUnit.DAYS.between(start, today);
  }

  private Dhis2Info getDhis2Info() {
    SystemInfoForDataStats system = systemService.getSystemInfoForDataStats();

    return new Dhis2Info()
        .setVersion(system.version())
        .setRevision(system.revision())
        .setBuildTime(system.buildTime())
        .setSystemId(system.id())
        .setServerDate(system.serverDate());
  }
}
