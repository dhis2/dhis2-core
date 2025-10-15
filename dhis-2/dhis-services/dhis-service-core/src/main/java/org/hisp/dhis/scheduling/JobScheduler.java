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
package org.hisp.dhis.scheduling;

import static java.lang.System.currentTimeMillis;
import static java.util.stream.Collectors.groupingBy;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.user.SystemUser;
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

  /**
   * Remember if the scheduling loop has been started. This is so during tests the "execute now" can
   * manually issue the run based on the scheduler not being active.
   */
  private final AtomicBoolean scheduling = new AtomicBoolean();

  private final JobService jobService;
  private final JobSchedulerLoopService service;
  private final SystemSettingsService settingsProvider;
  private final ExecutorService workers = Executors.newCachedThreadPool();
  private final Map<JobType, Queue<UID>> continuousJobsByType = new ConcurrentHashMap<>();

  public void start() {
    long loopTimeMs = LOOP_SECONDS * 1000L;
    long alignment = loopTimeMs - (currentTimeMillis() % loopTimeMs);
    Executors.newSingleThreadScheduledExecutor()
        .scheduleAtFixedRate(this, alignment, loopTimeMs, TimeUnit.MILLISECONDS);
    scheduling.set(true);
  }

  @Override
  public boolean isScheduling() {
    return scheduling.get();
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
        Map<JobType, List<JobEntry>> readyByType =
            service.getDueJobConfigurations(LOOP_SECONDS).stream()
                .collect(groupingBy(JobEntry::type));
        // only attempt to start one per type per loop invocation
        readyByType.forEach((type, jobs) -> runIfDue(now, type, jobs));
        if (!readyByType.containsKey(JobType.HOUSEKEEPING)) {
          createHousekeepingJob();
        }
      }
    } catch (Exception ex) {
      log.error("Exceptions thrown in scheduler loop", ex);
      // this needs to be caught otherwise the scheduling would end
    }
  }

  private void createHousekeepingJob() {
    try {
      service.createHousekeepingJob(new SystemUser());
    } catch (Exception ex) {
      log.error("Unable to create house-keeping job: " + ex.getMessage(), ex);
    }
  }

  @Override
  public void runIfDue(JobEntry job) {
    runIfDue(Instant.now().truncatedTo(ChronoUnit.SECONDS), job.type(), List.of(job));
  }

  private void runIfDue(Instant now, JobType type, List<JobEntry> jobs) {
    if (!type.isUsingContinuousExecution()) {
      runIfDue(now, jobs.get(0));
      return;
    }
    Queue<UID> jobIds = continuousJobsByType.get(type);
    boolean spawnWorker = false;
    if (jobIds == null) {
      Queue<UID> localQueue = new ConcurrentLinkedQueue<>();
      Queue<UID> sharedQueue = continuousJobsByType.putIfAbsent(type, localQueue);
      spawnWorker = sharedQueue == null; // no previous queue => this thread put the queue
      jobIds = continuousJobsByType.get(type);
    }
    // add those IDs to the queue that are not yet in it
    jobs.stream().map(JobEntry::id).forEach(jobIds::add);

    if (spawnWorker) {
      // we want to prevent starting more than one worker per job type
      // but if this does happen it is no issue as both will be pulling
      // from the same queue
      workers.submit(() -> runContinuous(type));
    }
  }

  private void runContinuous(JobType type) {
    try {
      Queue<UID> jobIds = continuousJobsByType.get(type);
      UID jobId = jobIds.poll();
      while (jobId != null) {
        JobEntry config = service.getJobConfiguration(jobId);
        if (config != null && (config.status() == JobStatus.SCHEDULED)) {
          Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
          Instant dueTime = dueTime(now, config);
          runDueJob(config, dueTime);
        }
        jobId = jobIds.poll();
      }
    } finally {
      // need to be done so that we never have a queue without a worker by accident
      continuousJobsByType.remove(type);
    }
  }

  private void runIfDue(Instant now, JobEntry config) {
    Instant dueTime = dueTime(now, config);
    if (dueTime != null) {
      workers.submit(() -> runDueJob(config, dueTime));
    }
  }

  private Instant dueTime(Instant now, JobEntry config) {
    Duration maxCronDelay =
        Duration.ofHours(settingsProvider.getCurrentSettings().getJobsMaxCronDelayHours());
    Instant dueTime = config.toTrigger().nextExecutionTime(now, maxCronDelay);
    return dueTime != null && !dueTime.isAfter(now) ? dueTime : null;
  }

  @Override
  public void runDueJob(JobEntry config) {
    runDueJob(config, Instant.now().truncatedTo(ChronoUnit.SECONDS));
  }

  /** This is executed on a worker thread. The start time is the desired time to run. */
  private void runDueJob(JobEntry config, Instant start) {
    UID jobId = config.id();
    if (!service.tryRun(jobId)) {
      log.debug(
          String.format(
              "Could not start job %s although it should run %s",
              jobId, start.atZone(ZoneId.systemDefault())));
      return;
    }
    log.debug("Running job %s");
    JobProgress progress = null;
    try {
      settingsProvider.clearCurrentSettings(); // ensure working with recent settings
      AtomicLong lastAlive = new AtomicLong(0L);
      progress = service.startRun(jobId, config.executedBy(), () -> alive(jobId, lastAlive));

      jobService.getJob(config.type()).execute(config, progress);

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
      if (progress != null) progress.failedProcess(ex);
      service.finishRunFail(jobId, ex);
    } finally {
      if (service.finishRunSuccess(jobId) && config.isUsedInQueue()) {
        JobEntry next = service.getNextInQueue(config.queueName(), config.queuePosition());
        if (next != null)
          runDueJob(next, start); // this is a tail recursion but job queues are not very long
      }
    }
  }

  /** The observing has to be outside the service as it will need a DB transaction. */
  private void alive(UID jobId, AtomicLong lastAssured) {
    long now = currentTimeMillis();
    if (now - lastAssured.get() > 10_000) {
      lastAssured.set(now);
      service.updateAsRunning(jobId);
    }
  }
}
