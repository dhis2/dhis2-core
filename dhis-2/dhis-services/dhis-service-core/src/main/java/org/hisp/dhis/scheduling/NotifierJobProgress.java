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
package org.hisp.dhis.scheduling;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.dhis.system.notification.NotificationDataType;
import org.hisp.dhis.system.notification.NotificationLevel;
import org.hisp.dhis.system.notification.Notifier;

/**
 * A {@link JobProgress} implementation that forwards the tracking to a {@link Notifier}. It has no
 * flow control and should be wrapped in a {@link RecordingJobProgress} for that purpose.
 *
 * @see RecordingJobProgress
 */
@RequiredArgsConstructor
public class NotifierJobProgress implements JobProgress {

  private final Notifier notifier;
  private final JobKey job;
  private final JobParameters params;
  private final NotificationLevel level;
  private final AtomicBoolean hasCleared = new AtomicBoolean();

  private int stageItems;
  private int stageItem;

  private boolean isLoggedLoop() {
    return NotificationLevel.LOOP.ordinal() >= level.ordinal();
  }

  private boolean isLoggedInfo() {
    return NotificationLevel.INFO.ordinal() >= level.ordinal();
  }

  private boolean isLoggedError() {
    return NotificationLevel.ERROR.ordinal() >= level.ordinal();
  }

  @Override
  public void startingProcess(String description, Object... args) {
    String message =
        isNotEmpty(description) ? format(description, args) : job.type() + " process started";
    if (hasCleared.compareAndSet(false, true)) {
      notifier.clear(job);
    }
    // Note: intentionally no log level check - always log first
    notifier.notify(
        job,
        NotificationLevel.INFO,
        isLoggedInfo() ? message : "",
        false,
        NotificationDataType.PARAMETERS,
        getJobParameterData());
  }

  @Override
  public void completedProcess(String summary, Object... args) {
    // Note: intentionally no log level check - always log last
    notifier.notify(job, isLoggedInfo() ? format(summary, args) : "", true);
  }

  @Override
  public void failedProcess(@CheckForNull String error, Object... args) {
    // Note: intentionally no log level check - always log last
    notifier.notify(job, NotificationLevel.ERROR, format(error, args), true);
  }

  @Override
  public void startingStage(
      @Nonnull String description, int workItems, @Nonnull FailurePolicy onFailure) {
    stageItems = workItems;
    stageItem = 0;
    if (isLoggedInfo() && isNotEmpty(description)) {
      notifier.notify(job, description);
    }
  }

  @Override
  public void completedStage(String summary, Object... args) {
    if (isLoggedInfo() && isNotEmpty(summary)) {
      notifier.notify(job, format(summary, args));
    }
  }

  @Override
  public void failedStage(@Nonnull String error, Object... args) {
    if (isLoggedError() && isNotEmpty(error)) {
      notifier.notify(job, NotificationLevel.ERROR, format(error, args), false);
    }
  }

  @Override
  public void startingWorkItem(@Nonnull String description, @Nonnull FailurePolicy onFailure) {
    if (isLoggedLoop() && isNotEmpty(description)) {
      String nOf = "[" + (stageItems > 0 ? stageItem + "/" + stageItems : "" + stageItem) + "] ";
      notifier.notify(job, NotificationLevel.LOOP, nOf + description, false);
    }
    stageItem++;
  }

  @Override
  public void completedWorkItem(String summary, Object... args) {
    if (isLoggedLoop() && isNotEmpty(summary)) {
      String nOf = "[" + (stageItems > 0 ? stageItem + "/" + stageItems : "" + stageItem) + "] ";
      notifier.notify(job, NotificationLevel.LOOP, nOf + format(summary, args), false);
    }
  }

  @Override
  public void failedWorkItem(@Nonnull String error, Object... args) {
    if (isLoggedError() && isNotEmpty(error)) {
      notifier.notify(job, NotificationLevel.ERROR, format(error, args), false);
    }
  }

  private JsonValue getJobParameterData() {
    if (params == null) return null;
    try {
      // TODO
      return JsonValue.of(new ObjectMapper().writeValueAsString(params));
    } catch (Exception ex) {
      return null;
    }
  }
}
