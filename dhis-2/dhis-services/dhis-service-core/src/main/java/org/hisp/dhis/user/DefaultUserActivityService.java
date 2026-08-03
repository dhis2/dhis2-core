/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.user;

import static org.hisp.dhis.scheduling.JobProgress.FailurePolicy.SKIP_STAGE;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.SimpleCacheBuilder;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.scheduling.JobProgress;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link UserActivityService}. The write guard is a per-JVM cache keyed by username with a
 * 10 minute write TTL, so an active user costs at most one small upsert per 10 minutes (and the
 * {@code lastactive} column is at most 10 minutes stale). In a multi-node cluster each node keeps
 * its own guard; the upsert is idempotent so extra writes are harmless.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
@Service
public class DefaultUserActivityService implements UserActivityService {

  /** Keep raw per-user-per-day rows this many days before rolling up. */
  public static final int DEFAULT_RETENTION_DAYS = 365;

  private static final Cache<Boolean> RECENT_ACTIVITY_CACHE =
      new SimpleCacheBuilder<Boolean>()
          .forRegion("userActivityDedup")
          .expireAfterWrite(10, TimeUnit.MINUTES)
          .withInitialCapacity(10_000)
          .withMaximumSize(100_000)
          .build();

  private final UserActivityStore userActivityStore;

  private final Set<String> excludedUsernames;

  public DefaultUserActivityService(
      UserActivityStore userActivityStore, DhisConfigurationProvider config) {
    this.userActivityStore = userActivityStore;
    this.excludedUsernames =
        DefaultLoginEventService.parseExcludedUsernames(
            config.getProperty(ConfigurationKey.SYSTEM_USER_STATS_EXCLUDED_USERS));
  }

  @Override
  @Transactional
  public void recordActivity(String username) {
    if (username == null || username.isBlank() || excludedUsernames.contains(username)) {
      return;
    }
    if (RECENT_ACTIVITY_CACHE.getIfPresent(username).isPresent()) {
      return;
    }
    try {
      Date now = new Date();
      userActivityStore.upsertActivity(username, LocalDate.now(ZoneId.systemDefault()), now);
      RECENT_ACTIVITY_CACHE.put(username, Boolean.TRUE);
    } catch (Exception e) {
      // Never break a request because of a stats write.
      log.warn("Failed to record user activity for '{}': {}", username, e.getMessage());
    }
  }

  /** Clears the write-side guard cache. Intended for tests only. */
  public static void clearDedupCache() {
    RECENT_ACTIVITY_CACHE.invalidateAll();
  }

  @Override
  @Transactional(readOnly = true)
  public List<Integer> getActiveUsersCounts(List<Date> sinceDates) {
    return userActivityStore.getActiveUserCounts(sinceDates);
  }

  @Override
  @Transactional(readOnly = true)
  public List<DailyActiveUsers> getDailyStatistics(LocalDate startDate, LocalDate endDate) {
    if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
      return List.of();
    }
    Date start = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    Date end = Date.from(endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    return userActivityStore.getDailyStatistics(start, end);
  }

  @Override
  @Transactional
  public void cleanup(int retentionDays, JobProgress progress) {
    int days = retentionDays > 0 ? retentionDays : DEFAULT_RETENTION_DAYS;
    Date olderThan =
        Date.from(
            LocalDate.now(ZoneId.systemDefault())
                .minusDays(days)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant());

    progress.startingStage("Rolling up user activity older than " + days + " days", SKIP_STAGE);
    int rolledUp = progress.runStage(0, () -> userActivityStore.rollupOlderThan(olderThan));
    progress.startingStage("Deleting rolled-up user activity", SKIP_STAGE);
    int deleted = progress.runStage(0, () -> userActivityStore.deleteOlderThan(olderThan));
    log.info(
        "User activity cleanup: rolled up {} day(s), deleted {} raw row(s) older than {}",
        rolledUp,
        deleted,
        olderThan);
  }
}
