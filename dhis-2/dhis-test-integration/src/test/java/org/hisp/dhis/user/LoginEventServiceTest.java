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
class LoginEventServiceTest extends PostgresIntegrationTestBase {

  @Autowired private LoginEventService loginEventService;

  @Autowired private LoginEventStore loginEventStore;

  @org.junit.jupiter.api.BeforeEach
  void clearDedup() {
    DefaultLoginEventService.clearDedupCache();
  }

  @Test
  void recordLoginWritesOneRowAndDedupsWithinWindow() {
    loginEventService.recordLogin("alice", LoginAuthType.FORM);
    loginEventService.recordLogin("alice", LoginAuthType.FORM); // deduped
    loginEventService.recordLogin("alice", LoginAuthType.API_TOKEN); // different type
    loginEventService.recordLogin("bob", LoginAuthType.FORM);

    // HQL sees the flushed rows regardless of JDBC date-window math.
    Long total =
        entityManager
            .createQuery("select count(e) from LoginEvent e", Long.class)
            .getSingleResult();
    assertEquals(3L, total, "dedup should keep FORM once + API_TOKEN + bob FORM");

    LocalDate today = LocalDate.now(ZoneId.systemDefault());
    List<DailyLoginStatistics> stats = loginEventService.getDailyStatistics(today, today);

    assertEquals(1, stats.size(), "expected one day of stats, got: " + stats);
    DailyLoginStatistics day = stats.get(0);
    assertEquals(today, day.date());
    assertEquals(3, day.logins());
    assertEquals(2, day.uniqueUsers());
  }

  @Test
  void cleanupRollsUpAndDeletesOldRows() {
    LocalDate oldDay = LocalDate.now(ZoneId.systemDefault()).minusDays(400);
    Date oldTs = Date.from(oldDay.atStartOfDay(ZoneId.systemDefault()).toInstant());
    loginEventStore.save(new LoginEvent("olduser1", oldTs, LoginAuthType.FORM));
    loginEventStore.save(new LoginEvent("olduser2", oldTs, LoginAuthType.BASIC));
    loginEventService.recordLogin("recent", LoginAuthType.FORM);

    Long before =
        entityManager
            .createQuery("select count(e) from LoginEvent e", Long.class)
            .getSingleResult();
    assertTrue(before >= 3L, "precondition: raw rows present");

    loginEventService.cleanup(365, JobProgress.noop());
    entityManager.clear();

    Long remainingOld =
        entityManager
            .createQuery(
                "select count(e) from LoginEvent e where e.timestamp < :cutoff", Long.class)
            .setParameter(
                "cutoff",
                Date.from(
                    LocalDate.now(ZoneId.systemDefault())
                        .minusDays(365)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()))
            .getSingleResult();
    assertEquals(0L, remainingOld, "old raw rows should be pruned");

    List<DailyLoginStatistics> oldStats = loginEventService.getDailyStatistics(oldDay, oldDay);
    assertEquals(1, oldStats.size(), "rollup should preserve the old day: " + oldStats);
    assertEquals(2, oldStats.get(0).logins());
    assertEquals(2, oldStats.get(0).uniqueUsers());

    LocalDate today = LocalDate.now(ZoneId.systemDefault());
    List<DailyLoginStatistics> todayStats = loginEventService.getDailyStatistics(today, today);
    assertTrue(
        todayStats.stream().anyMatch(s -> s.logins() >= 1),
        "recent raw event should still be readable: " + todayStats);
  }
}
