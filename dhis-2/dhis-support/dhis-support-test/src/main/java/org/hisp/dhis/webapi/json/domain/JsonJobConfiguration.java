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
package org.hisp.dhis.webapi.json.domain;

import java.time.Instant;
import org.hisp.dhis.jsontree.Expected;
import org.hisp.dhis.jsontree.JsonNumber;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.scheduling.JobStatus;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.SchedulingType;

/**
 * Web API equivalent of a {@link org.hisp.dhis.scheduling.JobConfiguration}.
 *
 * @author Jan Bernitt
 */
public interface JsonJobConfiguration extends JsonIdentifiableObject {

  @Expected
  default JobType getJobType() {
    return getString("jobType").parsed(JobType::valueOf);
  }

  @Expected
  default SchedulingType getSchedulingType() {
    return getString("schedulingType").parsed(SchedulingType::valueOf);
  }

  default String getCronExpression() {
    return getString("cronExpression").string();
  }

  default Integer getDelay() {
    JsonNumber delay = getNumber("delay");
    return delay.isUndefined() ? null : delay.intValue();
  }

  default JsonObject getJobParameters() {
    return getObject("jobParameters");
  }

  default boolean isEnabled() {
    return getBoolean("enabled").booleanValue();
  }

  default JobStatus getJobStatus() {
    return getString("jobStatus").parsed(JobStatus::valueOf);
  }

  default JobStatus getLastExecutedStatus() {
    return getString("lastExecutedStatus").parsed(JobStatus::valueOf);
  }

  default Instant getLastExecuted() {
    return getString("lastExecuted").parsed(Instant::parse);
  }

  default Instant getLastFinished() {
    return getString("lastFinished").parsed(Instant::parse);
  }

  default Instant getLastAlive() {
    return getString("lastAlive").parsed(Instant::parse);
  }

  default String getExecutedBy() {
    return getString("executedBy").string();
  }

  default String getQueueName() {
    return getString("queueName").string();
  }

  default Integer getQueuePosition() {
    JsonNumber queuePosition = getNumber("queuePosition");
    return queuePosition.isUndefined() ? null : queuePosition.intValue();
  }
}
