/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.webapi.controller.scheduling;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.hisp.dhis.scheduling.JobConfiguration.maxDelayedExecutionTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.annotation.CheckForNull;
import lombok.Value;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobStatus;

@Value
class SchedulerEntry {
  @JsonProperty String name;

  @JsonProperty String type;

  @JsonProperty String cronExpression;

  @JsonProperty Integer delay;

  @JsonProperty Date nextExecutionTime;

  /**
   * The end of the window for the current {@link #nextExecutionTime}. This means if execution is
   * missed on the intended time this will be the latest time an attempt is made for that occurrence
   * before the next time will be the occurrence after that.
   */
  @JsonProperty Date maxDelayedExecutionTime;

  /**
   * The number of seconds until the next execution will happen. This is purely for convenience to
   * spare the client to fetch the server time to compute this value as the client can not use the
   * client local time to compute it from {@link #nextExecutionTime}
   */
  @JsonProperty Long secondsToNextExecutionTime;

  /**
   * The number of seconds until the end of the execution window. This is purely for convenience to
   * * spare the client to fetch the server time to compute this value as the client can not use the
   * * client local time to compute it from {@link #maxDelayedExecutionTime}
   */
  @JsonProperty Long secondsToMaxDelayedExecutionTime;

  @JsonProperty JobStatus status;

  @JsonProperty boolean enabled;

  @JsonProperty boolean configurable;

  @JsonProperty List<SchedulerEntryJob> sequence;

  static SchedulerEntry of(JobConfiguration config, Duration maxCronDelay) {
    Instant nextExecutionTime = config.nextExecutionTime(Instant.now(), maxCronDelay);
    Instant maxDelayedExecutionTime =
        maxDelayedExecutionTime(config, maxCronDelay, nextExecutionTime);
    return new SchedulerEntry(
        config.getName(),
        config.getJobType().name(),
        config.getCronExpression(),
        config.getDelay(),
        dateOf(nextExecutionTime),
        dateOf(maxDelayedExecutionTime),
        secondsUntil(nextExecutionTime),
        secondsUntil(maxDelayedExecutionTime),
        config.getJobStatus(),
        config.isEnabled(),
        config.getJobType().isUserDefined(),
        List.of(SchedulerEntryJob.of(config)));
  }

  static SchedulerEntry of(List<JobConfiguration> jobs, Duration maxCronDelay) {
    List<JobConfiguration> queue =
        jobs.stream().sorted(comparing(JobConfiguration::getQueuePosition)).collect(toList());
    JobConfiguration trigger = queue.get(0);
    if (!trigger.isUsedInQueue()) {
      return of(trigger, maxCronDelay);
    }
    JobStatus queueStatus =
        queue.stream()
            .map(JobConfiguration::getJobStatus)
            .filter(status -> status == JobStatus.RUNNING)
            .findAny()
            .orElse(trigger.getJobStatus());
    Instant nextExecutionTime = trigger.nextExecutionTime(Instant.now(), maxCronDelay);
    Instant maxDelayedExecutionTime =
        maxDelayedExecutionTime(trigger, maxCronDelay, nextExecutionTime);
    return new SchedulerEntry(
        trigger.getQueueName(),
        "Sequence",
        trigger.getCronExpression(),
        trigger.getDelay(),
        dateOf(nextExecutionTime),
        dateOf(maxDelayedExecutionTime),
        secondsUntil(nextExecutionTime),
        secondsUntil(maxDelayedExecutionTime),
        queueStatus,
        trigger.isEnabled(),
        true,
        queue.stream()
            .sorted(comparing(JobConfiguration::getQueuePosition))
            .map(SchedulerEntryJob::of)
            .collect(toList()));
  }

  private static Date dateOf(Instant instant) {
    return instant == null ? null : Date.from(instant);
  }

  @CheckForNull
  public static Long secondsUntil(Instant instant) {
    if (instant == null) return null;
    return Duration.between(Instant.now(), instant).getSeconds();
  }
}
