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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;

/**
 * Contains all fields relevant to compute the next execution time of a {@link Job}.
 *
 * @author Jan Bernitt
 * @param type type of scheduling used
 * @param lastExecuted last time the job ran, null if it never ran before
 * @param cronExpression the CRON expressed used if type is {@link SchedulingType#CRON}, otherwise
 *     null
 * @param delay the delay in seconds if type is {@link SchedulingType#FIXED_DELAY}, otherwise null
 */
public record JobTrigger(
    @Nonnull SchedulingType type,
    @CheckForNull Date lastExecuted,
    @CheckForNull String cronExpression,
    @CheckForNull Integer delay) {

  /**
   * @param now current timestamp, ideally without milliseconds
   * @param maxCronDelay the maximum duration a CRON based job will trigger on the same day after
   *     its intended time during the day. If more time has passed already the execution for that
   *     day is skipped and the next day will be the target
   * @return the next time this job should run based on the {@link #getLastExecuted()} time
   */
  public Instant nextExecutionTime(@Nonnull Instant now, @Nonnull Duration maxCronDelay) {
    return nextExecutionTime(ZoneId.systemDefault(), now, maxCronDelay);
  }

  public Instant nextExecutionTime(
      @Nonnull ZoneId zone, @Nonnull Instant now, @Nonnull Duration maxCronDelay) {
    // for good measure we offset the last time by 1 second
    boolean isFirstExecution = lastExecuted == null;
    Instant since = isFirstExecution ? now : lastExecuted.toInstant().plusSeconds(1);

    return switch (type) {
      case ONCE_ASAP -> nextOnceExecutionTime(since);
      case FIXED_DELAY -> nextDelayExecutionTime(since);
      case CRON ->
          nextCronExecutionTime(
              zone, isFirstExecution ? since.minus(maxCronDelay) : since, now, maxCronDelay);
    };
  }

  private Instant nextCronExecutionTime(
      @Nonnull ZoneId zone, @Nonnull Instant since, Instant now, @Nonnull Duration maxDelay) {
    if (isUndefinedCronExpression(cronExpression)) return null;
    // we use a no offset zone for the context as we want the given since value to be taken as is
    // the zone is not actually used by this is closest to what would be required
    ZoneOffset noOffsetZone = ZoneOffset.UTC;
    SimpleTriggerContext context = new SimpleTriggerContext(Clock.fixed(since, noOffsetZone));
    Date next = new CronTrigger(cronExpression, zone).nextExecutionTime(context);
    if (next == null) return null;
    while (next != null && now.isAfter(next.toInstant().plus(maxDelay))) {
      context =
          new SimpleTriggerContext(Clock.fixed(next.toInstant().plusSeconds(1), noOffsetZone));
      next = new CronTrigger(cronExpression, zone).nextExecutionTime(context);
    }
    return next == null ? null : next.toInstant();
  }

  private Instant nextDelayExecutionTime(@Nonnull Instant since) {
    if (delay == null || delay <= 0) return null;
    // always want to run delay after last start, right away when never started
    return lastExecuted == null
        ? since
        : lastExecuted.toInstant().plusSeconds(delay).truncatedTo(ChronoUnit.SECONDS);
  }

  private Instant nextOnceExecutionTime(@Nonnull Instant since) {
    return since;
  }

  private static boolean isUndefinedCronExpression(String cronExpression) {
    return cronExpression == null
        || cronExpression.isEmpty()
        || cronExpression.equals("* * * * * ?");
  }
}
