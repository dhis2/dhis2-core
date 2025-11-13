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
package org.hisp.dhis.system.notification;

import static java.time.Duration.ofSeconds;
import static org.hisp.dhis.scheduling.JobType.ANALYTICS_TABLE;
import static org.hisp.dhis.scheduling.JobType.DATAVALUE_IMPORT;
import static org.hisp.dhis.scheduling.JobType.METADATA_IMPORT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testcontainers.shaded.org.awaitility.Awaitility.waitAtMost;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.hisp.dhis.jsontree.Json;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsService;
import org.junit.jupiter.api.Test;

/**
 * @author Lars Helge Overland
 */
class NotifierTest {

  private final Notifier notifier;

  private final JobConfiguration analyticsTable;
  private final JobConfiguration metadataImport;

  private final JobConfiguration dataImport1;
  private final JobConfiguration dataImport2;
  private final JobConfiguration dataImport3;
  private final JobConfiguration dataImport4;

  public NotifierTest() {
    analyticsTable = new JobConfiguration(null, ANALYTICS_TABLE);
    metadataImport = new JobConfiguration(null, METADATA_IMPORT);
    dataImport1 = new JobConfiguration(null, DATAVALUE_IMPORT);
    dataImport2 = new JobConfiguration(null, DATAVALUE_IMPORT);
    dataImport3 = new JobConfiguration(null, DATAVALUE_IMPORT);
    dataImport4 = new JobConfiguration(null, DATAVALUE_IMPORT);
    SystemSettingsService settingsService = mock(SystemSettingsService.class);
    when(settingsService.getCurrentSettings()).thenReturn(SystemSettings.of(Map.of()));
    this.notifier =
        new DefaultNotifier(
            new InMemoryNotifierStore(),
            new ObjectMapper(),
            settingsService,
            System::currentTimeMillis);
  }

  @Test
  void testGetNotifications() {
    notifier.notify(dataImport1, "Import started");
    notifier.notify(dataImport1, "Import working");
    notifier.notify(dataImport1, "Import done");
    notifier.notify(analyticsTable, "Process started");
    notifier.notify(analyticsTable, "Process done");
    awaitIdle();
    assertNotNull(notifier.getNotifications(false));
    assertEquals(3, getNotificationsCount(DATAVALUE_IMPORT, dataImport1.getUid()));
    assertEquals(2, getNotificationsCount(ANALYTICS_TABLE, analyticsTable.getUid()));
    assertEquals(0, getNotificationsCount(METADATA_IMPORT, metadataImport.getUid()));

    notifier.clear(dataImport1.toKey());
    notifier.clear(analyticsTable.toKey());
    notifier.notify(dataImport1, "Import started");
    notifier.notify(dataImport1, "Import working");
    notifier.notify(dataImport1, "Import done");
    notifier.notify(analyticsTable, "Process started");
    notifier.notify(analyticsTable, "Process done");
    awaitIdle();
    assertEquals(3, getNotificationsCount(DATAVALUE_IMPORT, dataImport1.getUid()));
    assertEquals(2, getNotificationsCount(ANALYTICS_TABLE, analyticsTable.getUid()));
    notifier.clear(dataImport1.toKey());
    assertEquals(0, getNotificationsCount(DATAVALUE_IMPORT, dataImport1.getUid()));
    assertEquals(2, getNotificationsCount(ANALYTICS_TABLE, analyticsTable.getUid()));
    notifier.clear(analyticsTable.toKey());
    assertEquals(0, getNotificationsCount(DATAVALUE_IMPORT, dataImport1.getUid()));
    assertEquals(0, getNotificationsCount(ANALYTICS_TABLE, analyticsTable.getUid()));

    notifier.notify(dataImport2, "Process done");
    notifier.notify(dataImport1, "Import started");
    notifier.notify(dataImport1, "Import working");
    notifier.notify(dataImport1, "Import in progress");
    notifier.notify(dataImport1, "Import done");
    notifier.notify(analyticsTable, "Process started");
    notifier.notify(analyticsTable, "Process done");
    awaitIdle();
    Deque<Notification> notifications =
        getNotifications(DATAVALUE_IMPORT).get(dataImport1.getUid());
    assertNotNull(notifications);
    assertEquals(4, notifications.size());

    notifier.notify(dataImport3, "Completed1");
    awaitIdle();
    Map<String, Deque<Notification>> byJobId = getNotifications(DATAVALUE_IMPORT);
    assertNotNull(byJobId);
    assertEquals(3, byJobId.size());
    assertEquals(4, byJobId.get(dataImport1.getUid()).size());
    assertEquals(1, byJobId.get(dataImport2.getUid()).size());
    assertEquals(1, byJobId.get(dataImport3.getUid()).size());
    assertEquals("Completed1", byJobId.get(dataImport3.getUid()).getFirst().getMessage());

    notifier.notify(dataImport4, "Completed2");
    awaitIdle();
    byJobId = getNotifications(DATAVALUE_IMPORT);
    assertNotNull(byJobId);
    assertEquals(4, byJobId.get(dataImport1.getUid()).size());
    assertEquals(1, byJobId.get(dataImport2.getUid()).size());
    assertEquals(1, byJobId.get(dataImport3.getUid()).size());
    assertEquals(1, byJobId.get(dataImport4.getUid()).size());
    assertEquals("Completed2", byJobId.get(dataImport4.getUid()).getFirst().getMessage());
  }

  private void awaitIdle() {
    waitAtMost(ofSeconds(5)).until(notifier::isIdle);
  }

  private Map<String, Deque<Notification>> getNotifications(JobType type) {
    return notifier.getNotificationsByJobType(type, false);
  }

  private int getNotificationsCount(JobType type, String uid) {
    return notifier.getNotificationsByJobId(type, uid).size();
  }

  @Test
  void testGetSummary() {
    notifier.addJobSummary(dataImport1, "somethingid1", String.class);
    notifier.addJobSummary(analyticsTable, "somethingid2", String.class);
    notifier.addJobSummary(dataImport2, "somethingid4", String.class);
    notifier.addJobSummary(metadataImport, "somethingid3", String.class);
    Map<String, JsonValue> jobSummariesForAnalyticsType =
        notifier.getJobSummariesForJobType(DATAVALUE_IMPORT);
    assertNotNull(jobSummariesForAnalyticsType);
    assertEquals(2, jobSummariesForAnalyticsType.size());
    Map<String, JsonValue> jobSummariesForMetadataImportType =
        notifier.getJobSummariesForJobType(METADATA_IMPORT);
    assertNotNull(jobSummariesForMetadataImportType);
    assertEquals(1, jobSummariesForMetadataImportType.size());
    assertEquals(
        Json.of("somethingid3"), jobSummariesForMetadataImportType.get(metadataImport.getUid()));
    Object summary = notifier.getJobSummaryByJobId(DATAVALUE_IMPORT, dataImport1.getUid());
    assertNotNull(summary);
    assertEquals(Json.of("somethingid1"), summary, "True");
    notifier.addJobSummary(dataImport3, "summarry3", String.class);
    jobSummariesForAnalyticsType = notifier.getJobSummariesForJobType(DATAVALUE_IMPORT);
    assertNotNull(jobSummariesForAnalyticsType);
    assertEquals(3, jobSummariesForAnalyticsType.size());
    notifier.addJobSummary(dataImport4, "summarry4", String.class);
    jobSummariesForAnalyticsType = notifier.getJobSummariesForJobType(DATAVALUE_IMPORT);
    assertNotNull(jobSummariesForAnalyticsType);
    assertEquals(4, jobSummariesForAnalyticsType.size());
  }

  @Test
  void testInsertingNotificationsInSameJobConcurrently() {
    ExecutorService e = Executors.newFixedThreadPool(5);
    JobConfiguration jobConfig = new JobConfiguration(null, METADATA_IMPORT);
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
    assertEquals(101, notifier.getNotificationsByJobId(METADATA_IMPORT, jobConfig.getUid()).size());
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
