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
package org.hisp.dhis.system.notification;

import static java.time.Duration.ofSeconds;
import static org.hisp.dhis.scheduling.JobType.ANALYTICS_TABLE;
import static org.hisp.dhis.scheduling.JobType.DATAVALUE_IMPORT;
import static org.hisp.dhis.scheduling.JobType.METADATA_IMPORT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.testcontainers.shaded.org.awaitility.Awaitility.waitAtMost;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.Test;
import org.mockito.quality.Strictness;

/**
 * @author Lars Helge Overland
 */
class NotifierTest extends DhisConvenienceTest {

  private final Notifier notifier =
      new DefaultNotifier(
          new InMemoryNotifierStore(),
          new ObjectMapper(),
          settingsProvider(),
          System::currentTimeMillis);

  private final User user = makeUser("A");

  private final JobConfiguration dataValueImportJobConfig;

  private final JobConfiguration analyticsTableJobConfig;

  private final JobConfiguration metadataImportJobConfig;

  private final JobConfiguration dataValueImportSecondJobConfig;

  private final JobConfiguration dataValueImportThirdJobConfig;

  private final JobConfiguration dataValueImportFourthConfig;

  private static SystemSettingsProvider settingsProvider() {
    SystemSettingsProvider settingsProvider =
        mock(SystemSettingsProvider.class, withSettings().strictness(Strictness.LENIENT));
    when(settingsProvider.getSystemSetting(SettingKey.NOTIFIER_LOG_LEVEL, "DEBUG"))
        .thenReturn("DEBUG");
    when(settingsProvider.getIntSetting(SettingKey.NOTIFIER_MAX_MESSAGES_PER_JOB)).thenReturn(500);
    when(settingsProvider.getIntSetting(SettingKey.NOTIFIER_MAX_AGE_DAYS)).thenReturn(7);
    when(settingsProvider.getIntSetting(SettingKey.NOTIFIER_MAX_JOBS_PER_TYPE)).thenReturn(500);
    when(settingsProvider.getBoolSetting(SettingKey.NOTIFIER_GIST_OVERVIEW)).thenReturn(true);
    when(settingsProvider.getSystemSetting(SettingKey.NOTIFIER_CLEAN_AFTER_IDLE_TIME, Long.class))
        .thenReturn(60_000L);
    return settingsProvider;
  }

  public NotifierTest() {
    dataValueImportJobConfig = new JobConfiguration(null, DATAVALUE_IMPORT, user.getUid(), false);
    dataValueImportJobConfig.setUid("dvi15678901");
    analyticsTableJobConfig = new JobConfiguration(null, ANALYTICS_TABLE, user.getUid(), false);
    analyticsTableJobConfig.setUid("at145678901");
    metadataImportJobConfig = new JobConfiguration(null, METADATA_IMPORT, user.getUid(), false);
    metadataImportJobConfig.setUid("mdi15678901");
    dataValueImportSecondJobConfig =
        new JobConfiguration(null, DATAVALUE_IMPORT, user.getUid(), false);
    dataValueImportSecondJobConfig.setUid("dvi25678901");
    dataValueImportThirdJobConfig =
        new JobConfiguration(null, DATAVALUE_IMPORT, user.getUid(), false);
    dataValueImportThirdJobConfig.setUid("dvi35678901");
    dataValueImportFourthConfig =
        new JobConfiguration(null, DATAVALUE_IMPORT, user.getUid(), false);
    dataValueImportFourthConfig.setUid("dvi45678901");
    JobConfiguration dataValueImportFifthConfig =
        new JobConfiguration(null, DATAVALUE_IMPORT, user.getUid(), false);
    dataValueImportFifthConfig.setUid("dvi55678901");
  }

  @Test
  void testGetNotifications() {
    notifier.notify(dataValueImportJobConfig, "Import started");
    notifier.notify(dataValueImportJobConfig, "Import working");
    notifier.notify(dataValueImportJobConfig, "Import done");
    notifier.notify(analyticsTableJobConfig, "Process started");
    notifier.notify(analyticsTableJobConfig, "Process done");

    awaitIdle();

    Map<JobType, Map<String, Deque<Notification>>> notificationsMap =
        notifier.getNotifications(false);
    assertNotNull(notificationsMap);
    assertEquals(
        3,
        notifier
            .getNotificationsByJobId(
                dataValueImportJobConfig.getJobType(), dataValueImportJobConfig.getUid())
            .size());
    assertEquals(
        2,
        notifier
            .getNotificationsByJobId(
                analyticsTableJobConfig.getJobType(), analyticsTableJobConfig.getUid())
            .size());
    assertEquals(
        0,
        notifier
            .getNotificationsByJobId(
                metadataImportJobConfig.getJobType(), metadataImportJobConfig.getUid())
            .size());
    notifier.clear(dataValueImportJobConfig);
    notifier.clear(analyticsTableJobConfig);
    notifier.notify(dataValueImportJobConfig, "Import started");
    notifier.notify(dataValueImportJobConfig, "Import working");
    notifier.notify(dataValueImportJobConfig, "Import done");
    notifier.notify(analyticsTableJobConfig, "Process started");
    notifier.notify(analyticsTableJobConfig, "Process done");

    awaitIdle();

    assertEquals(
        3,
        notifier
            .getNotificationsByJobId(
                dataValueImportJobConfig.getJobType(), dataValueImportJobConfig.getUid())
            .size());
    assertEquals(
        2,
        notifier
            .getNotificationsByJobId(
                analyticsTableJobConfig.getJobType(), analyticsTableJobConfig.getUid())
            .size());
    notifier.clear(dataValueImportJobConfig);
    assertEquals(
        0,
        notifier
            .getNotificationsByJobId(
                dataValueImportJobConfig.getJobType(), dataValueImportJobConfig.getUid())
            .size());
    assertEquals(
        2,
        notifier
            .getNotificationsByJobId(
                analyticsTableJobConfig.getJobType(), analyticsTableJobConfig.getUid())
            .size());
    notifier.clear(analyticsTableJobConfig);
    assertEquals(
        0,
        notifier
            .getNotificationsByJobId(
                dataValueImportJobConfig.getJobType(), dataValueImportJobConfig.getUid())
            .size());
    assertEquals(
        0,
        notifier
            .getNotificationsByJobId(
                analyticsTableJobConfig.getJobType(), analyticsTableJobConfig.getUid())
            .size());
    notifier.notify(dataValueImportSecondJobConfig, "Process done");
    notifier.notify(dataValueImportJobConfig, "Import started");
    notifier.notify(dataValueImportJobConfig, "Import working");
    notifier.notify(dataValueImportJobConfig, "Import in progress");
    notifier.notify(dataValueImportJobConfig, "Import done");
    notifier.notify(analyticsTableJobConfig, "Process started");
    notifier.notify(analyticsTableJobConfig, "Process done");

    awaitIdle();

    Deque<Notification> notifications =
        notifier
            .getNotificationsByJobType(DATAVALUE_IMPORT, false)
            .get(dataValueImportJobConfig.getUid());
    assertNotNull(notifications);
    assertEquals(4, notifications.size());

    notifier.notify(dataValueImportThirdJobConfig, "Completed1");

    awaitIdle();

    Map<String, Deque<Notification>> notificationsByJobType =
        notifier.getNotificationsByJobType(DATAVALUE_IMPORT, false);

    assertNotNull(notificationsByJobType);
    assertEquals(3, notificationsByJobType.size());
    assertEquals(4, notificationsByJobType.get(dataValueImportJobConfig.getUid()).size());
    assertEquals(1, notificationsByJobType.get(dataValueImportSecondJobConfig.getUid()).size());
    assertEquals(1, notificationsByJobType.get(dataValueImportThirdJobConfig.getUid()).size());
    assertEquals(
        "Completed1",
        notificationsByJobType.get(dataValueImportThirdJobConfig.getUid()).getFirst().getMessage());

    notifier.notify(dataValueImportFourthConfig, "Completed2");

    awaitIdle();

    notificationsByJobType = notifier.getNotificationsByJobType(DATAVALUE_IMPORT, false);
    assertNotNull(notificationsByJobType);
    assertEquals(4, notificationsByJobType.get(dataValueImportJobConfig.getUid()).size());
    assertEquals(1, notificationsByJobType.get(dataValueImportSecondJobConfig.getUid()).size());
    assertEquals(1, notificationsByJobType.get(dataValueImportThirdJobConfig.getUid()).size());
    assertEquals(1, notificationsByJobType.get(dataValueImportFourthConfig.getUid()).size());
    assertEquals(
        "Completed2",
        notificationsByJobType.get(dataValueImportFourthConfig.getUid()).getFirst().getMessage());
  }

  @Test
  void testGetSummary() {
    notifier.addJobSummary(dataValueImportJobConfig, "somethingid1", String.class);
    notifier.addJobSummary(analyticsTableJobConfig, "somethingid2", String.class);
    notifier.addJobSummary(dataValueImportSecondJobConfig, "somethingid4", String.class);
    notifier.addJobSummary(metadataImportJobConfig, "somethingid3", String.class);

    awaitIdle();

    Map<String, JsonNode> jobSummariesForAnalyticsType =
        notifier.getJobSummariesForJobType(DATAVALUE_IMPORT);
    assertNotNull(jobSummariesForAnalyticsType);
    assertEquals(2, jobSummariesForAnalyticsType.size());
    Map<String, JsonNode> jobSummariesForMetadataImportType =
        notifier.getJobSummariesForJobType(METADATA_IMPORT);
    assertNotNull(jobSummariesForMetadataImportType);
    assertEquals(1, jobSummariesForMetadataImportType.size());
    assertEquals(
        "somethingid3",
        jobSummariesForMetadataImportType.get(metadataImportJobConfig.getUid()).asText());
    JsonNode summary =
        notifier.getJobSummaryByJobId(
            dataValueImportJobConfig.getJobType(), dataValueImportJobConfig.getUid());
    assertNotNull(summary);
    assertEquals("somethingid1", summary.asText(), "True");
    notifier.addJobSummary(dataValueImportThirdJobConfig, "summarry3", String.class);
    jobSummariesForAnalyticsType = notifier.getJobSummariesForJobType(DATAVALUE_IMPORT);
    assertNotNull(jobSummariesForAnalyticsType);
    assertEquals(3, jobSummariesForAnalyticsType.size());
    notifier.addJobSummary(dataValueImportFourthConfig, "summarry4", String.class);
    jobSummariesForAnalyticsType = notifier.getJobSummariesForJobType(DATAVALUE_IMPORT);
    assertNotNull(jobSummariesForAnalyticsType);
    assertEquals(4, jobSummariesForAnalyticsType.size());
  }

  @Test
  void testInsertingNotificationsInSameJobConcurrently() throws InterruptedException {
    ExecutorService e = Executors.newFixedThreadPool(5);
    JobConfiguration jobConfig = new JobConfiguration(null, METADATA_IMPORT, user.getUid(), true);
    notifier.notify(jobConfig, "somethingid");
    IntStream.range(0, 100)
        .forEach(i -> e.execute(() -> notifier.notify(jobConfig, "somethingid" + i)));
    IntStream.range(0, 100)
        .forEach(
            i -> {
              for (Notification notification :
                  notifier.getNotificationsByJobId(METADATA_IMPORT, jobConfig.getUid())) {
                // Iterate over notifications when new notification are added
                assertNotNull(notification.getUid());
              }
            });
    awaitTermination(e);
    awaitIdle();
    assertEquals(
        101,
        notifier.getNotificationsByJobType(METADATA_IMPORT, false).get(jobConfig.getUid()).size());
  }

  private void awaitIdle() {
    waitAtMost(ofSeconds(5)).until(notifier::isIdle);
  }

  public void awaitTermination(ExecutorService threadPool) {
    threadPool.shutdown();
    try {
      if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
        threadPool.shutdownNow();
      }
    } catch (InterruptedException ex) {
      threadPool.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }
}
