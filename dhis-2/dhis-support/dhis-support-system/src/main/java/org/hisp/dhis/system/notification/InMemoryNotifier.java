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

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Date;
import java.util.Deque;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;

/**
 * @author Lars Helge Overland
 */
@Slf4j
public class InMemoryNotifier implements Notifier {
  public static final int MAX_POOL_TYPE_SIZE = 500;

  private final NotificationMap notificationMap = new NotificationMap(MAX_POOL_TYPE_SIZE);

  @Override
  public Notifier notify(
      JobConfiguration id,
      NotificationLevel level,
      String message,
      boolean completed,
      NotificationDataType dataType,
      JsonNode data) {
    if (id != null && !level.isOff()) {
      Notification notification =
          new Notification(level, id.getJobType(), new Date(), message, completed, dataType, data);

      if (id.isInMemoryJob() && !StringUtils.isEmpty(id.getUid())) {
        notification.setUid(id.getUid());
      }

      notificationMap.add(id, notification);

      NotificationLoggerUtil.log(log, level, message);
    }

    return this;
  }

  @Override
  public Map<JobType, Map<String, Deque<Notification>>> getNotifications() {
    return notificationMap.getNotifications();
  }

  @Override
  public Deque<Notification> getNotificationsByJobId(JobType jobType, String jobId) {
    return notificationMap.getNotificationsByJobId(jobType, jobId);
  }

  @Override
  public Map<String, Deque<Notification>> getNotificationsByJobType(JobType jobType) {
    return notificationMap.getNotificationsWithType(jobType);
  }

  @Override
  public Notifier clear(JobConfiguration id) {
    if (id != null) {
      notificationMap.clear(id);
    }

    return this;
  }

  @Override
  public <T> Notifier addJobSummary(
      JobConfiguration id, NotificationLevel level, T jobSummary, Class<T> jobSummaryType) {
    if (id != null && !(level != null && level.isOff())) {
      notificationMap.addSummary(id, jobSummary);
    }

    return this;
  }

  @Override
  public Map<String, Object> getJobSummariesForJobType(JobType jobType) {
    return notificationMap.getJobSummariesForJobType(jobType);
  }

  @Override
  public Object getJobSummaryByJobId(JobType jobType, String jobId) {
    return notificationMap.getSummary(jobType, jobId);
  }
}
