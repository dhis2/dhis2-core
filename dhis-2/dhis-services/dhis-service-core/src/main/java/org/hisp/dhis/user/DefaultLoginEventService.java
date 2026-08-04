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
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.SimpleCacheBuilder;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.scheduling.JobProgress;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link LoginEventService}. Dedup is a per-JVM in-memory cache keyed by {@code
 * username|authType} with a 15 minute write TTL, so PAT/basic-auth request storms only produce one
 * raw row per user per auth type per window. In a multi-node cluster each node has its own cache
 * (acceptable for statistics; worst case is one extra row per node per window).
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
@Service
public class DefaultLoginEventService implements LoginEventService {

  /** Keep raw events this many days before rolling them into the daily aggregate. */
  public static final int DEFAULT_RETENTION_DAYS = 365;

  private static final Cache<Boolean> RECENT_LOGIN_CACHE =
      new SimpleCacheBuilder<Boolean>()
          .forRegion("loginEventDedup")
          .expireAfterWrite(15, TimeUnit.MINUTES)
          .withInitialCapacity(10_000)
          .withMaximumSize(100_000)
          .build();

  private final LoginEventStore loginEventStore;

  private final Set<String> excludedUsernames;

  public DefaultLoginEventService(
      LoginEventStore loginEventStore, DhisConfigurationProvider config) {
    this.loginEventStore = loginEventStore;
    this.excludedUsernames =
        parseExcludedUsernames(
            config.getProperty(ConfigurationKey.SYSTEM_USER_STATS_EXCLUDED_USERS));
  }

  /**
   * Parses the comma-separated {@code system.user_stats.excluded_users} value, typically monitoring
   * or integration service accounts that would otherwise inflate the statistics.
   */
  static Set<String> parseExcludedUsernames(String value) {
    if (value == null || value.isBlank()) {
      return Set.of();
    }
    return Arrays.stream(value.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toUnmodifiableSet());
  }

  @Override
  @Transactional
  public void recordLogin(String username, LoginAuthType authType) {
    if (username == null
        || username.isBlank()
        || authType == null
        || excludedUsernames.contains(username)) {
      return;
    }
    String key = username + '|' + authType.name();
    if (RECENT_LOGIN_CACHE.getIfPresent(key).isPresent()) {
      return;
    }
    try {
      loginEventStore.save(new LoginEvent(username, new Date(), authType));
      RECENT_LOGIN_CACHE.put(key, Boolean.TRUE);
    } catch (Exception e) {
      // Never break authentication because of a stats write.
      log.warn("Failed to record login event for user '{}': {}", username, e.getMessage());
    }
  }

  /** Clears the write-side dedup cache. Intended for tests only. */
  public static void clearDedupCache() {
    RECENT_LOGIN_CACHE.invalidateAll();
  }

  @Override
  @Transactional(readOnly = true)
  public List<DailyLoginStatistics> getDailyStatistics(LocalDate startDate, LocalDate endDate) {
    if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
      return List.of();
    }
    // endDate is inclusive for the caller; SQL windows are half-open [start, end).
    Date start = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    Date end = Date.from(endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    return loginEventStore.getDailyStatistics(start, end);
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

    progress.startingStage("Rolling up login events older than " + days + " days", SKIP_STAGE);
    int rolledUp = progress.runStage(0, () -> loginEventStore.rollupOlderThan(olderThan));
    progress.startingStage("Deleting rolled-up login events", SKIP_STAGE);
    int deleted = progress.runStage(0, () -> loginEventStore.deleteOlderThan(olderThan));
    log.info(
        "Login event cleanup: rolled up {} day(s), deleted {} raw row(s) older than {}",
        rolledUp,
        deleted,
        olderThan);
  }
}
