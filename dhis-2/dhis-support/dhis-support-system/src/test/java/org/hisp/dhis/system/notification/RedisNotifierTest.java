/*
 * Copyright (c) 2004-2023, University of Oslo
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.scheduling.JobType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.BoundZSetOperations;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * @author David Mackessy
 */
@ExtendWith(MockitoExtension.class)
class RedisNotifierTest extends DhisConvenienceTest {

  @Mock private RedisTemplate<String, String> redisTemplate;

  @Mock private BoundZSetOperations<String, String> boundZSetOperations;

  @InjectMocks private RedisNotifier notifier;

  /**
   * When Redis is enabled, the APP UI relies on specific ordered data to know when generating
   * Resource/Analytics tables has completed. The APP UI only knows the job is complete once it sees
   * that the first element in the returned data is marked as 'completed'. This test confirms that
   * the returned data always has the latest {@link Notification} as the first element.
   */
  @Test
  void getNotificationsByJobIdTest_OrderedByTime() {
    notifier = new RedisNotifier(redisTemplate, new ObjectMapper());
    JobType jobType = JobType.ANALYTICS_TABLE;
    String jobId = "job1d1";
    Set<String> dataFromRedis = new HashSet<>();

    // 3 Notifications, each with a different 'time' value
    // the 'completed' Notification has the latest time
    String notificationMiddle =
        "{\"uid\":\"ju8WSUHJKHO\",\"level\": \"INFO\",\"category\":\"ANALYTICS_TABLE\",\"time\":\"2023-07-05T10:16:33.554\",\"message\":\"1 Analytics tables updated\",\"completed\":false}";
    String notificationLatest =
        "{\"uid\":\"zM8zxPLTKaY\",\"level\":\"INFO\",\"category\":\"ANALYTICS_TABLE\",\"time\":\"2023-07-05T10:16:33.555\",\"message\":\"2 Drop SQL views\",\"completed\":true}";
    String notificationEarliest =
        "{\"uid\":\"aM8zxPLTKaY\",\"level\":\"INFO\",\"category\":\"ANALYTICS_TABLE\",\"time\":\"2023-07-05T10:16:33.553\",\"message\":\"3 Drop SQL views\",\"completed\":false}";

    // add notifications unordered
    dataFromRedis.add(notificationMiddle);
    dataFromRedis.add(notificationEarliest);
    dataFromRedis.add(notificationLatest);

    when(redisTemplate.boundZSetOps(any())).thenReturn(boundZSetOperations);
    when(redisTemplate.boundZSetOps(any()).range(0, -1)).thenReturn(dataFromRedis);

    // Notifications should be returned in an ordered Queue from this call
    Deque<Notification> result = notifier.getNotificationsByJobId(jobType, jobId);

    assertFalse(result.isEmpty());
    // check first Notification is completed
    Notification peek = result.peek();
    assertTrue(peek.isCompleted());

    // confirm the ordering of each Notification
    Notification latestNotification = result.removeFirst();
    Notification middleNotification = result.removeFirst();
    Notification earliestNotification = result.removeFirst();
    assertTrue(latestNotification.getTime().after(middleNotification.getTime()));
    assertTrue(latestNotification.getTime().after(earliestNotification.getTime()));
    assertTrue(middleNotification.getTime().after(earliestNotification.getTime()));
  }
}
