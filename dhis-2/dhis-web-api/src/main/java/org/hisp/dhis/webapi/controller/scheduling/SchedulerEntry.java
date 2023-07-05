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
package org.hisp.dhis.webapi.controller.scheduling;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;
import java.util.List;
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

  @JsonProperty JobStatus status;

  @JsonProperty boolean enabled;

  @JsonProperty boolean configurable;

  @JsonProperty List<SchedulerEntryJob> sequence;

  static SchedulerEntry of(JobConfiguration config) {
    return new SchedulerEntry(
        config.getName(),
        config.getJobType().name(),
        config.getCronExpression(),
        config.getDelay(),
        config.getNextExecutionTime(),
        config.getJobStatus(),
        config.isEnabled(),
        config.getJobType().isConfigurable(),
        List.of(SchedulerEntryJob.of(config)));
  }

  static SchedulerEntry of(List<JobConfiguration> jobs) {
    List<JobConfiguration> queue =
        jobs.stream().sorted(comparing(JobConfiguration::getQueuePosition)).collect(toList());
    JobConfiguration trigger = queue.get(0);
    if (!trigger.isUsedInQueue()) {
      return of(trigger);
    }
    JobStatus queueStatus =
        queue.stream()
            .map(JobConfiguration::getJobStatus)
            .filter(status -> status == JobStatus.RUNNING)
            .findAny()
            .orElse(trigger.getJobStatus());
    return new SchedulerEntry(
        trigger.getQueueName(),
        "Sequence",
        trigger.getCronExpression(),
        trigger.getDelay(),
        trigger.getNextExecutionTime(),
        queueStatus,
        trigger.isEnabled(),
        true,
        queue.stream()
            .sorted(comparing(JobConfiguration::getQueuePosition))
            .map(SchedulerEntryJob::of)
            .collect(toList()));
  }
}
