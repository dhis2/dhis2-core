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

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * The {@link NoopJobProgress} can be used as a {@link JobProgress} instance when no actual flow
 * control and tracking is wanted. For example in test context or in manual runs of operations that
 * would generally support the tracking though the {@link JobProgress} abstraction.
 *
 * @author Jan Bernitt
 */
class NoopJobProgress implements JobProgress {
  public static final JobProgress INSTANCE = new NoopJobProgress();

  private NoopJobProgress() {
    // hide
  }

  @Override
  public void startingProcess(String description, Object... args) {
    // as the name said we do nothing
  }

  @Override
  public void completedProcess(String summary, Object... args) {
    // as the name said we do nothing
  }

  @Override
  public void failedProcess(@CheckForNull String error, Object... args) {
    // as the name said we do nothing
  }

  @Override
  public void startingStage(
      @Nonnull String description, int workItems, @Nonnull FailurePolicy onFailure) {
    // as the name said we do nothing
  }

  @Override
  public void completedStage(String summary, Object... args) {
    // as the name said we do nothing
  }

  @Override
  public void failedStage(@Nonnull String error, Object... args) {
    // as the name said we do nothing
  }

  @Override
  public void startingWorkItem(@Nonnull String description, @Nonnull FailurePolicy onFailure) {
    // as the name said we do nothing
  }

  @Override
  public void completedWorkItem(String summary, Object... args) {
    // as the name said we do nothing
  }

  @Override
  public void failedWorkItem(@Nonnull String error, Object... args) {
    // as the name said we do nothing
  }
}
