/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.system.notification;

import static java.lang.System.currentTimeMillis;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.stream.Collectors.toList;
import static org.hisp.dhis.notification.NotificationLevel.LOOP;
import static org.hisp.dhis.scheduling.JobType.ANALYTICS_TABLE;
import static org.hisp.dhis.scheduling.JobType.DATA_INTEGRITY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.testcontainers.shaded.org.awaitility.Awaitility.waitAtMost;

import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.quality.Strictness;

/**
 * Tests for the {@link Notifier} API that should run both for the {@link InMemoryNotifierStore} and
 * the {@link RedisNotifierStore}.
 *
 * @author Jan Bernitt
 */
abstract class NotifierStoreTest {

  protected Notifier notifier;
  protected LongSupplier clock = System::currentTimeMillis;
  protected SystemSettingsProvider settingsProvider =
      mock(SystemSettingsProvider.class, withSettings().strictness(Strictness.LENIENT));

  /**
   * Expected to init the {@link #notifier} field with a new {@link Notifier} instance using the
   * provided {@link SystemSettingsProvider}.
   *
   * @param settings the settings to use
   */
  abstract void setUpNotifier(SystemSettingsProvider settings);

  @BeforeEach
  void setUp() {
    // as default
    when(settingsProvider.getSystemSetting(SettingKey.NOTIFIER_LOG_LEVEL, "DEBUG"))
        .thenReturn("DEBUG");
    when(settingsProvider.getIntSetting(SettingKey.NOTIFIER_MAX_MESSAGES_PER_JOB)).thenReturn(500);
    when(settingsProvider.getIntSetting(SettingKey.NOTIFIER_MAX_AGE_DAYS)).thenReturn(7);
    when(settingsProvider.getIntSetting(SettingKey.NOTIFIER_MAX_JOBS_PER_TYPE)).thenReturn(500);
    when(settingsProvider.getBoolSetting(SettingKey.NOTIFIER_GIST_OVERVIEW)).thenReturn(true);
    when(settingsProvider.getSystemSetting(SettingKey.NOTIFIER_CLEAN_AFTER_IDLE_TIME, Long.class))
        .thenReturn(60_000L);
    setUpNotifier(settingsProvider);
  }

  /**
   * When messages are added they have to make it though a sync queue to become visible in the
   * store, therefor one has to wait a bit before making asserts on the store content.
   */
  private void awaitIdle() {
    waitAtMost(ofSeconds(5)).pollInterval(ofMillis(25)).until(notifier::isIdle);
  }

  @Test
  void testNotify() {
    JobConfiguration job1 = job(1, DATA_INTEGRITY);
    notifier.notify(job1, "msg1");
    notifier.notify(job1, "msg2");
    notifier.notify(job1, "msg3");
    awaitIdle();
    assertEquals(3, notifier.getNotificationsByJobId(DATA_INTEGRITY, job1.getUid()).size());
  }

  @Test
  void testGetNotifications() {
    JobConfiguration job1 = job(1, DATA_INTEGRITY);
    notifier.notify(job1, "msg11");
    JobConfiguration job2 = job(2, DATA_INTEGRITY);
    notifier.notify(job2, "msg21");
    notifier.notify(job2, "msg22");
    JobConfiguration job3 = job(3, ANALYTICS_TABLE);
    notifier.notify(job3, "msg31");
    notifier.notify(job3, "msg32");
    notifier.notify(job3, "msg33");

    awaitIdle();

    Map<JobType, Map<String, Deque<Notification>>> notifications = notifier.getNotifications(false);
    assertEquals(2, notifications.size());
    Map<String, Deque<Notification>> dataIntegrity = notifications.get(DATA_INTEGRITY);
    assertEquals(Set.of(job1.getUid(), job2.getUid()), dataIntegrity.keySet());
    assertMessages(List.of("msg11"), dataIntegrity.get(job1.getUid()));
    assertMessages(List.of("msg22", "msg21"), dataIntegrity.get(job2.getUid()));
    Map<String, Deque<Notification>> analyticsTable = notifications.get(ANALYTICS_TABLE);
    assertEquals(Set.of(job3.getUid()), analyticsTable.keySet());
    assertMessages(List.of("msg33", "msg32", "msg31"), analyticsTable.get(job3.getUid()));

    notifications = notifier.getNotifications(true);
    assertMessages(
        List.of("msg33", "msg31"), notifications.get(ANALYTICS_TABLE).get(job3.getUid()));
  }

  @Test
  void testGetNotificationsByJobId() {
    JobConfiguration job1 = job(1, DATA_INTEGRITY);
    notifier.notify(job1, "msg11");
    JobConfiguration job2 = job(2, DATA_INTEGRITY);
    notifier.notify(job2, "msg21");
    notifier.notify(job2, "msg22");
    JobConfiguration job3 = job(3, ANALYTICS_TABLE);
    notifier.notify(job3, "msg31");
    notifier.notify(job3, "msg32");
    notifier.notify(job3, "msg33");

    awaitIdle();

    assertMessages(
        List.of("msg11"), notifier.getNotificationsByJobId(DATA_INTEGRITY, job1.getUid()));
    assertMessages(
        List.of("msg22", "msg21"), notifier.getNotificationsByJobId(DATA_INTEGRITY, job2.getUid()));
    assertMessages(List.of(), notifier.getNotificationsByJobId(DATA_INTEGRITY, job3.getUid()));
    assertMessages(
        List.of("msg33", "msg32", "msg31"),
        notifier.getNotificationsByJobId(ANALYTICS_TABLE, job3.getUid()));
  }

  @Test
  void testGetNotificationsByJobType() {
    JobConfiguration job1 = job(1, DATA_INTEGRITY);
    notifier.notify(job1, "msg11");
    JobConfiguration job2 = job(2, DATA_INTEGRITY);
    notifier.notify(job2, "msg21");
    notifier.notify(job2, "msg22");
    JobConfiguration job3 = job(3, ANALYTICS_TABLE);
    notifier.notify(job3, "msg31");
    notifier.notify(job3, "msg32");
    notifier.notify(job3, "msg33");

    awaitIdle();

    Map<String, Deque<Notification>> notifications =
        notifier.getNotificationsByJobType(DATA_INTEGRITY, false);
    assertEquals(2, notifications.size());
    assertMessages(List.of("msg11"), notifications.get(job1.getUid()));
    assertMessages(List.of("msg22", "msg21"), notifications.get(job2.getUid()));

    notifications = notifier.getNotificationsByJobType(DATA_INTEGRITY, true);
    assertMessages(List.of("msg11"), notifications.get(job1.getUid()));
    assertMessages(List.of("msg22", "msg21"), notifications.get(job2.getUid()));

    notifications = notifier.getNotificationsByJobType(ANALYTICS_TABLE, false);
    assertEquals(1, notifications.size());
    assertMessages(List.of("msg33", "msg32", "msg31"), notifications.get(job3.getUid()));

    notifications = notifier.getNotificationsByJobType(ANALYTICS_TABLE, true);
    assertEquals(1, notifications.size());
    assertMessages(List.of("msg33", "msg31"), notifications.get(job3.getUid()));
  }

  @Test
  void testClear() {
    JobConfiguration job1 = job(1, DATA_INTEGRITY);
    notifier.notify(job1, "msg11");
    JobConfiguration job2 = job(2, DATA_INTEGRITY);
    notifier.notify(job2, "msg21");
    notifier.notify(job2, "msg22");
    JobConfiguration job3 = job(3, ANALYTICS_TABLE);
    notifier.notify(job3, "msg31");
    notifier.notify(job3, "msg32");
    notifier.notify(job3, "msg33");

    awaitIdle();

    assertEquals(2, notifier.getNotifications(false).size());
    notifier.clear();
    assertEquals(0, notifier.getNotifications(false).size());
    assertEquals(0, notifier.getNotificationsByJobType(DATA_INTEGRITY, false).size());
    assertEquals(0, notifier.getNotificationsByJobType(ANALYTICS_TABLE, false).size());
    assertEquals(0, notifier.getNotificationsByJobId(DATA_INTEGRITY, job1.getUid()).size());
    assertEquals(0, notifier.getNotificationsByJobId(DATA_INTEGRITY, job2.getUid()).size());
    assertEquals(0, notifier.getNotificationsByJobId(ANALYTICS_TABLE, job3.getUid()).size());
  }

  @Test
  void testClear_byType() {
    JobConfiguration job1 = job(1, DATA_INTEGRITY);
    notifier.notify(job1, "msg11");
    JobConfiguration job2 = job(2, DATA_INTEGRITY);
    notifier.notify(job2, "msg21");
    notifier.notify(job2, "msg22");
    JobConfiguration job3 = job(3, ANALYTICS_TABLE);
    notifier.notify(job3, "msg31");
    notifier.notify(job3, "msg32");
    notifier.notify(job3, "msg33");

    awaitIdle();

    assertEquals(3, notifier.getNotificationsByJobId(ANALYTICS_TABLE, job3.getUid()).size());
    notifier.clear(ANALYTICS_TABLE);
    assertEquals(0, notifier.getNotificationsByJobId(ANALYTICS_TABLE, job3.getUid()).size());

    assertEquals(2, notifier.getNotificationsByJobType(DATA_INTEGRITY, true).size());
    notifier.clear(DATA_INTEGRITY);
    assertEquals(0, notifier.getNotificationsByJobType(DATA_INTEGRITY, true).size());
  }

  @Test
  void testClear_byJob() {
    JobConfiguration job1 = job(1, DATA_INTEGRITY);
    notifier.notify(job1, "msg11");
    JobConfiguration job2 = job(2, DATA_INTEGRITY);
    notifier.notify(job2, "msg21");
    notifier.notify(job2, "msg22");

    awaitIdle();

    notifier.clear(DATA_INTEGRITY, UID.of(job2.getUid()));
    assertEquals(1, notifier.getNotificationsByJobType(DATA_INTEGRITY, false).size());
    assertEquals(0, notifier.getNotificationsByJobId(DATA_INTEGRITY, job2.getUid()).size());
  }

  @Test
  void testCapMaxAge() {
    long t0 = currentTimeMillis();
    AtomicLong manualClock = new AtomicLong(t0);
    clock = manualClock::get;
    setUpNotifier(settingsProvider); // update to use clock

    manualClock.set(t0 - DAYS.toMillis(5));
    JobConfiguration job1 = job(1, DATA_INTEGRITY);
    notifier.notify(job1, "msg11");

    manualClock.set(t0 - HOURS.toMillis(55));
    JobConfiguration job2 = job(2, DATA_INTEGRITY);
    notifier.notify(job2, "msg21");
    notifier.notify(job2, "msg22");

    manualClock.set(t0 - DAYS.toMillis(1));
    JobConfiguration job3 = job(3, ANALYTICS_TABLE);
    notifier.notify(job3, "msg31");
    notifier.notify(job3, "msg32");
    notifier.notify(job3, "msg33");

    awaitIdle();

    manualClock.set(currentTimeMillis()); // must be actual now
    notifier.capMaxAge(3);
    assertEquals(0, notifier.getNotificationsByJobId(DATA_INTEGRITY, job1.getUid()).size());
    assertEquals(2, notifier.getNotificationsByJobId(DATA_INTEGRITY, job2.getUid()).size());
    assertEquals(3, notifier.getNotificationsByJobId(ANALYTICS_TABLE, job3.getUid()).size());
    notifier.capMaxAge(2, DATA_INTEGRITY);
    assertEquals(0, notifier.getNotificationsByJobType(DATA_INTEGRITY, false).size());
    assertEquals(3, notifier.getNotificationsByJobId(ANALYTICS_TABLE, job3.getUid()).size());
    assertEquals(1, notifier.getNotificationsByJobType(ANALYTICS_TABLE, false).size());
    notifier.capMaxAge(2);
    assertEquals(3, notifier.getNotificationsByJobId(ANALYTICS_TABLE, job3.getUid()).size());
  }

  @Test
  void testCapMaxCount() {
    JobConfiguration job1 = job(1, DATA_INTEGRITY);
    notifier.notify(job1, "msg11");
    JobConfiguration job2 = job(2, DATA_INTEGRITY);
    notifier.notify(job2, "msg21");
    notifier.notify(job2, "msg22");
    JobConfiguration job3 = job(3, DATA_INTEGRITY);
    notifier.notify(job3, "msg31");
    notifier.notify(job3, "msg32");
    notifier.notify(job3, "msg33");
    JobConfiguration job4 = job(4, ANALYTICS_TABLE);
    notifier.notify(job4, "msg41");
    notifier.notify(job4, "msg42");
    notifier.notify(job4, "msg43");
    notifier.notify(job4, "msg44");

    awaitIdle();

    notifier.capMaxCount(2);
    assertEquals(2, notifier.getNotificationsByJobType(DATA_INTEGRITY, true).size());
    assertEquals(1, notifier.getNotificationsByJobType(ANALYTICS_TABLE, false).size());
    assertEquals(
        Set.of(job2.getUid(), job3.getUid()),
        notifier.getNotificationsByJobType(DATA_INTEGRITY, true).keySet());
  }

  @Test
  void testGetNotifierMaxMessagesPerJob() {
    when(settingsProvider.getIntSetting(SettingKey.NOTIFIER_MAX_MESSAGES_PER_JOB)).thenReturn(3);
    setUpNotifier(settingsProvider);
    JobConfiguration job1 = job(1, DATA_INTEGRITY);
    notifier.notify(job1, "msg11");
    JobConfiguration job2 = job(2, DATA_INTEGRITY);
    notifier.notify(job2, "msg21");
    notifier.notify(job2, "msg22");
    notifier.notify(job2, "msg23");
    notifier.notify(job2, "msg24");
    JobConfiguration job3 = job(3, ANALYTICS_TABLE);
    notifier.notify(job3, "msg31");
    notifier.notify(job3, "msg32");
    notifier.notify(job3, "msg33");
    notifier.notify(job3, "msg34");
    notifier.notify(job3, "msg35");

    awaitIdle();

    assertMessages(
        List.of("msg11"), notifier.getNotificationsByJobId(DATA_INTEGRITY, job1.getUid()));
    assertMessages(
        List.of("msg24", "msg23", "msg21"),
        notifier.getNotificationsByJobId(DATA_INTEGRITY, job2.getUid()));
    assertMessages(
        List.of("msg35", "msg34", "msg31"),
        notifier.getNotificationsByJobId(ANALYTICS_TABLE, job3.getUid()));
  }

  @Test
  void testNotify_loopLevelMessagesAreReplaced() {
    JobConfiguration job1 = job(1, DATA_INTEGRITY);
    notifier.notify(job1, "msg11");
    JobConfiguration job2 = job(2, DATA_INTEGRITY);
    notifier.notify(job2, "msg21");
    notifier.notify(job2, LOOP, "msg22");
    notifier.notify(job2, LOOP, "msg23");
    notifier.notify(job2, "msg24");
    JobConfiguration job3 = job(3, ANALYTICS_TABLE);
    notifier.notify(job3, "msg31");
    notifier.notify(job3, "msg32");
    notifier.notify(job3, LOOP, "msg33");
    notifier.notify(job3, "msg34");
    notifier.notify(job3, "msg35");

    awaitIdle();

    assertMessages(
        List.of("msg11"), notifier.getNotificationsByJobId(DATA_INTEGRITY, job1.getUid()));
    assertMessages(
        List.of("msg24", "msg21"), notifier.getNotificationsByJobId(DATA_INTEGRITY, job2.getUid()));
    assertMessages(
        List.of("msg35", "msg34", "msg32", "msg31"),
        notifier.getNotificationsByJobId(ANALYTICS_TABLE, job3.getUid()));
  }

  @Test
  void testCleanAfterIdleTime() throws InterruptedException {
    when(settingsProvider.getIntSetting(SettingKey.NOTIFIER_MAX_JOBS_PER_TYPE)).thenReturn(2);
    when(settingsProvider.getSystemSetting(SettingKey.NOTIFIER_CLEAN_AFTER_IDLE_TIME, Long.class))
        .thenReturn(100L);
    setUpNotifier(settingsProvider);
    JobConfiguration job1 = job(1, DATA_INTEGRITY);
    notifier.notify(job1, "msg11");
    JobConfiguration job2 = job(2, DATA_INTEGRITY);
    notifier.notify(job2, "msg21");
    notifier.notify(job2, LOOP, "msg22");
    notifier.notify(job2, LOOP, "msg23");
    notifier.notify(job2, "msg24");
    JobConfiguration job3 = job(3, DATA_INTEGRITY);
    notifier.notify(job3, "msg31");
    notifier.notify(job3, "msg32");
    notifier.notify(job3, LOOP, "msg33");
    notifier.notify(job3, "msg34");
    notifier.notify(job3, "msg35");

    awaitIdle();

    // wait to clean-up kicks in after 100ms
    Thread.sleep(200);

    awaitIdle();

    assertEquals(2, notifier.getNotificationsByJobType(DATA_INTEGRITY, false).size());
  }

  private static JobConfiguration job(int serial, JobType type) {
    return new JobConfiguration("job" + serial, type, null, true);
  }

  private static void assertMessages(List<String> expected, Deque<Notification> actual) {
    assertEquals(expected, actual.stream().map(Notification::getMessage).collect(toList()));
  }
}
