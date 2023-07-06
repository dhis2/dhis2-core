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

import static org.hisp.dhis.scheduling.JobType.DATAVALUE_IMPORT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.Deque;
import java.util.Map;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.junit.jupiter.api.Test;

class NotificationMapTest {
  private final NotificationMap notifications = new NotificationMap(50);

  @Test
  void testFirstSummaryToBeCreatedIsTheFirstOneToBeRemoved() {
    final int maxSize = 50;
    // Fill the map with jobs
    JobConfiguration config = new JobConfiguration(null, DATAVALUE_IMPORT, "userId", false);
    addSummaryEntries(config, maxSize);
    // Add one more
    config.setUid(String.valueOf(maxSize));
    notifications.addSummary(config, maxSize);
    // Check that oldest job is not in the map anymore
    assertFalse(notifications.getJobSummariesForJobType(config.getJobType()).containsKey("0"));
    // Add one more
    config.setUid(String.valueOf(maxSize + 1));
    notifications.addSummary(config, maxSize + 1);
    // Check that oldest job is not in the map anymore
    assertFalse(notifications.getJobSummariesForJobType(config.getJobType()).containsKey("1"));
  }

  @Test
  void testFirstNotificationToBeCreatedIsTheFirstOneToBeRemoved() {
    final int maxSize = 50;
    // Fill the map with jobs
    JobConfiguration config = new JobConfiguration(null, DATAVALUE_IMPORT, "userId", false);
    config.setUid("1");
    addNotificationEntries(config, maxSize);

    Map<String, Deque<Notification>> typeNotification =
        notifications.getNotificationsWithType(config.getJobType());
    Deque<Notification> jobNotifications = typeNotification.get(config.getUid());
    assertNotNull(jobNotifications);
    assertEquals(maxSize, jobNotifications.size());
    // Add one more
    notifications.add(config, newNotification(config, maxSize));
    // Check that oldest job is not in the map anymore
    assertFalse(jobNotifications.stream().anyMatch(n -> "0".equals(n.getMessage())));
    assertTrue(jobNotifications.stream().anyMatch(n -> (maxSize + "").equals(n.getMessage())));
    assertEquals(maxSize, jobNotifications.size());
    // Add one more
    notifications.add(config, newNotification(config, maxSize + 1));
    // Check that oldest job is not in the map anymore
    assertFalse(jobNotifications.stream().anyMatch(n -> "1".equals(n.getMessage())));
    assertTrue(
        jobNotifications.stream().anyMatch(n -> ((maxSize + 1) + "").equals(n.getMessage())));
    assertEquals(maxSize, jobNotifications.size());
  }

  private void addSummaryEntries(JobConfiguration config, int n) {
    for (int i = 0; i < n; i++) {
      config.setUid(String.valueOf(i));
      notifications.addSummary(config, i);
    }
  }

  private void addNotificationEntries(JobConfiguration config, int n) {
    for (int i = 0; i < n; i++) {
      notifications.add(config, newNotification(config, i));
    }
  }

  private Notification newNotification(JobConfiguration config, int no) {
    return new Notification(
        NotificationLevel.INFO, config.getJobType(), new Date(), "" + no, false, null, null);
  }
}
