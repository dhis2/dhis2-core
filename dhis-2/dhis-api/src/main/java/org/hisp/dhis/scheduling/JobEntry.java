/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.scheduling;

import static java.util.Objects.requireNonNull;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.UID;

/**
 * A transient, read-only version of a {@link JobConfiguration} reduced to the properties relevant
 * within the scheduling and job related processes.
 *
 * @author Jan Bernitt
 */
public record JobEntry(
    // properties that never change
    @Nonnull UID id,
    @Nonnull JobType type,
    @Nonnull SchedulingType schedulingType,
    // properties that can change
    @Nonnull String name,
    @Nonnull JobStatus status,
    @CheckForNull UID executedBy,
    @CheckForNull String cronExpression,
    @CheckForNull Integer delay,
    @CheckForNull Date lastExecuted,
    @CheckForNull Date lastFinished,
    @CheckForNull Date lastAlive,
    @CheckForNull String queueName,
    @CheckForNull Integer queuePosition,
    @CheckForNull JobParameters parameters) {

  public JobEntry {
    // fail fast when nullness restrictions are not met
    requireNonNull(id);
    requireNonNull(type);
    requireNonNull(name);
    requireNonNull(schedulingType);
    requireNonNull(status);
  }

  public JobEntry(UID id, JobType type) {
    this(id, type, null);
  }

  public JobEntry(UID id, JobType type, JobParameters parameters) {
    this(
        id,
        type,
        SchedulingType.ONCE_ASAP,
        id.getValue(),
        JobStatus.NOT_STARTED,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        parameters);
  }

  public boolean isUsedInQueue() {
    return queueName != null;
  }

  public boolean isDueBetween(
      @Nonnull Instant now, @Nonnull Instant then, @Nonnull Duration maxCronDelay) {
    Instant dueTime = toTrigger().nextExecutionTime(now, maxCronDelay);
    return dueTime != null && dueTime.isBefore(then);
  }

  public JobKey toKey() {
    return new JobKey(id, type);
  }

  public JobTrigger toTrigger() {
    return new JobTrigger(schedulingType, lastExecuted, cronExpression, delay);
  }
}
