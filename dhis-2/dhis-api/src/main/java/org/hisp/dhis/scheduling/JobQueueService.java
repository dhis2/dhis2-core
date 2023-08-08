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
package org.hisp.dhis.scheduling;

import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.NotFoundException;

/**
 * Job Queue API
 *
 * <p>Job Queues are named sequences of {@link JobConfiguration}s triggered by a CRON expression.
 * They take the place of a job in the scheduler list.
 *
 * @author Jan Bernitt
 */
public interface JobQueueService {
  Set<String> getQueueNames();

  /**
   * Returns the jobs in queue sequence order.
   *
   * @param name name of the queue
   * @return all jobs that are part of the queue in execution order
   * @throws NotFoundException when no such queue exists
   */
  List<JobConfiguration> getQueue(@Nonnull String name) throws NotFoundException;

  /**
   * Create a new queue sequence.
   *
   * @param name name of the queue
   * @param cronExpression trigger CRON expression to start the queue sequence
   * @param sequence UIDs of the {@link JobConfiguration}s to run
   */
  void createQueue(
      @Nonnull String name, @Nonnull String cronExpression, @Nonnull List<String> sequence)
      throws NotFoundException, ConflictException;

  /**
   * Update a queue sequence.
   *
   * @param name name of the queue
   * @param newName potentially a new name for the queue, can be null or the same as name
   * @param newCronExpression trigger CRON expression to start the queue sequence
   * @param newSequence UIDs of the {@link JobConfiguration}s to run
   */
  void updateQueue(
      @Nonnull String name,
      @CheckForNull String newName,
      @Nonnull String newCronExpression,
      @Nonnull List<String> newSequence)
      throws NotFoundException, ConflictException;

  /**
   * Deletes a queue sequence.
   *
   * <p>When a sequence is disassembled the {@link JobConfiguration}s that were part of it get
   * reset. This means their {@link JobConfiguration#getQueueName()}, {@link
   * JobConfiguration#getQueuePosition()} and their {@link JobConfiguration#getCronExpression()} are
   * cleared. Also, they all get disabled by setting {@link JobConfiguration#isEnabled()} to {@code
   * false}.
   *
   * @param name name of the queue
   */
  void deleteQueue(@Nonnull String name) throws NotFoundException;
}
