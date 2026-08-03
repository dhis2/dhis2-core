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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class UserActivityServiceTest extends PostgresIntegrationTestBase {

  @Autowired private UserActivityService userActivityService;

  @Autowired private UserActivityStore userActivityStore;

  @BeforeEach
  void clearDedup() {
    DefaultUserActivityService.clearDedupCache();
  }

  @Test
  void recordActivityUpsertsOncePerDay() {
    userActivityService.recordActivity("alice");
    userActivityService.recordActivity("alice"); // guarded by cache
    userActivityService.recordActivity("bob");

    LocalDate today = LocalDate.now(ZoneId.systemDefault());
    List<DailyActiveUsers> stats = userActivityService.getDailyStatistics(today, today);
    assertEquals(1, stats.size(), "expected one day of stats: " + stats);
    assertEquals(today, stats.get(0).date());
    assertEquals(2, stats.get(0).activeUsers());
  }

  @Test
  void activeUserCountsUseLastActive() {
    userActivityService.recordActivity("alice");
    userActivityService.recordActivity("bob");

    Date oneHourAgo = new Date(System.currentTimeMillis() - 60 * 60 * 1000);
    Date thirtyDaysAgo = new Date(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000);
    List<Integer> counts =
        userActivityService.getActiveUsersCounts(List.of(oneHourAgo, thirtyDaysAgo));
    assertEquals(2, counts.get(0), "both users active within the hour");
    assertEquals(2, counts.get(1), "both users active within 30 days");
  }

  @Test
  void cleanupRollsUpAndDeletesOldRows() {
    LocalDate oldDay = LocalDate.now(ZoneId.systemDefault()).minusDays(400);
    Date oldTs = Date.from(oldDay.atStartOfDay(ZoneId.systemDefault()).toInstant());
    userActivityStore.upsertActivity("olduser1", oldDay, oldTs);
    userActivityStore.upsertActivity("olduser2", oldDay, oldTs);
    userActivityService.recordActivity("recent");

    userActivityService.cleanup(365, JobProgress.noop());

    List<DailyActiveUsers> oldStats = userActivityService.getDailyStatistics(oldDay, oldDay);
    assertEquals(1, oldStats.size(), "rollup should preserve the old day: " + oldStats);
    assertEquals(2, oldStats.get(0).activeUsers());

    // Raw rows for the old day are gone: counts by lastactive window covering the old day
    // must now come out of rollup only, so a lastactive-based count sees zero old users.
    Date beforeOldDay =
        Date.from(oldDay.minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    List<Integer> counts = userActivityService.getActiveUsersCounts(List.of(beforeOldDay));
    assertEquals(1, counts.get(0), "only the recent raw row should remain");

    LocalDate today = LocalDate.now(ZoneId.systemDefault());
    List<DailyActiveUsers> todayStats = userActivityService.getDailyStatistics(today, today);
    assertTrue(
        todayStats.stream().anyMatch(s -> s.activeUsers() >= 1),
        "recent activity still readable: " + todayStats);
  }
}
