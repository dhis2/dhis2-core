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
package org.hisp.dhis.scheduling;

import static java.lang.System.currentTimeMillis;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Implementation of the main scheduling loop executed every 20 seconds.
 *
 * <p>In the loop it is determined if a job should trigger and if so it is executed on a worker
 * thread.
 *
 * @author Jan Bernitt
 * @since 2.41
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobScheduler implements Runnable, JobRunner {

  /**
   * <b>Why 20 seconds loops?</b> Goal is a smooth execution of jobs with minute precision for the
   * trigger moment.
   *
   * <ul>
   *   <li>Multiples of 20 seconds align with minutes
   *   <li>Multiples of 1.5 times the time (30sec) align with minutes (leader election)
   *   <li>Happens at least twice a minute
   *   <li>Leaves as much time as possible otherwise to get through the work of triggering jobs
   * </ul>
   */
  private static final int LOOP_SECONDS = 20;

  /**
   * The TTL time for the leader value must be longer than the {@link #LOOP_SECONDS} so that a
   * leader can maintain itself as a leader by assuring being alive faster than the value times out
   * from the set TTL.
   */
  private static final int TTL_SECONDS = LOOP_SECONDS * 3 / 2;

  private final JobService jobService;
  private final JobSchedulerLoopService service;
  private final ExecutorService workers = Executors.newCachedThreadPool();

  public void start() {
    long loopTimeMs = LOOP_SECONDS * 1000L;
    long alignment = currentTimeMillis() % loopTimeMs;
    Executors.newSingleThreadScheduledExecutor()
        .scheduleAtFixedRate(this, alignment, loopTimeMs, TimeUnit.MILLISECONDS);
  }

  /**
   * The main scheduling loop executed every 20 seconds (see {@link #start()}).
   *
   * <p>If this node is the leader it tries to run jobs that might be due to run.
   */
  @Override
  public void run() {
    try {
      Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS).plusSeconds(1);
      if (service.tryBecomeLeader(TTL_SECONDS)) {
        service.assureAsLeader(TTL_SECONDS);
        service.getDueJobConfigurations(LOOP_SECONDS).forEach(c -> runIfDue(now, c));
      }
    } catch (Exception ex) {
      log.error("Exceptions thrown in scheduler loop", ex);
      // this needs to be caught otherwise the scheduling would end
    }
  }

  private void runIfDue(Instant now, JobConfiguration config) {
    Instant dueTime = config.nextExecutionTime(now);
    if (dueTime != null && !dueTime.isAfter(now)) {
      workers.submit(() -> runDueJob(config, dueTime));
    }
  }

  @Override
  public void runDueJob(JobConfiguration config) {
    runDueJob(config, Instant.now().truncatedTo(ChronoUnit.SECONDS));
  }

  /** This is executed on a worker thread. The start time is the desired time to run. */
  private void runDueJob(JobConfiguration config, Instant start) {
    String jobId = config.getUid();
    if (!service.tryRun(jobId)) {
      log.debug(
          String.format(
              "Could not start job %s although it should run %s",
              jobId, start.atZone(ZoneId.systemDefault())));
      return;
    }
    JobProgress progress = null;
    try {
      AtomicLong lastAlive = new AtomicLong(currentTimeMillis());
      progress = service.startRun(jobId, config.getExecutedBy(), () -> alive(jobId, lastAlive));

      jobService.getJob(config.getJobType()).execute(config, progress);

      if (progress.isCancelled() && !progress.isAborted()) {
        service.finishRunCancel(jobId);
      } else if (!progress.isSuccessful()) {
        service.finishRunFail(jobId, null);
      }
    } catch (CancellationException ex) {
      if (progress != null && progress.isAborted()) {
        service.finishRunFail(jobId, ex);
      } else {
        service.finishRunCancel(jobId);
      }
    } catch (Exception ex) {
      service.finishRunFail(jobId, ex);
    } finally {
      if (service.finishRunSuccess(jobId) && config.isUsedInQueue()) {
        JobConfiguration next =
            service.getNextInQueue(config.getQueueName(), config.getQueuePosition());
        if (next != null)
          runDueJob(next, start); // this is a tail recursion but job queues are not very long
      }
    }
  }

  /** The observing has to be outside the service as it will need a DB transaction. */
  private void alive(String jobId, AtomicLong lastAssured) {
    long now = currentTimeMillis();
    if (now - lastAssured.get() > 10_000) {
      lastAssured.set(now);
      service.updateAsRunning(jobId);
    }
  }
}
