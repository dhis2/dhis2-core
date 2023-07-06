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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.hisp.dhis.util.DateUtils.getMediumDateString;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.common.AsyncTaskExecutor;
import org.hisp.dhis.leader.election.LeaderManager;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.system.notification.Notifier;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

/**
 * A {@link SchedulingManager} that runs {@link #schedule(JobConfiguration)} and {@link
 * #executeNow(JobConfiguration)} asynchronously.
 *
 * <p>Whether a job can run is solely determined by {@link #isRunning(JobType)} which makes sure
 * only one asynchronous task can run at a time.
 *
 * <p>The {@link DefaultSchedulingManager} manages its private state with the sole goal of being
 * able to cancel asynchronously running tasks.
 *
 * @author Henning Håkonsen (original implementation)
 * @author Jan Bernitt (refactored)
 */
@Slf4j
@Service("org.hisp.dhis.scheduling.SchedulingManager")
public class DefaultSchedulingManager extends AbstractSchedulingManager {
  private static final int DEFAULT_INITIAL_DELAY_S = 10;

  /**
   * When a job is scheduled using an executor that will then invoke the job at specific times in
   * the future a {@link Future} is added to the map which can be used to stop the job to continue
   * to run in the future. It is essentially removed from the executor.
   *
   * <p>The key is the UID of the {@link JobConfiguration}. Should the UID be {@code null} the
   * {@link JobConfiguration#getName()} is used as key.
   *
   * <p>This means any {@link JobConfiguration} only aborts itself from further runs in the future.
   * Other configurations for the same {@link JobType} might exist and continue to run if there have
   * been scheduled to do so.
   */
  private final Map<String, Future<?>> removeFromScheduleByIdOrName = new ConcurrentHashMap<>();

  private final TaskScheduler jobScheduler;

  private final AsyncTaskExecutor taskExecutor;

  public DefaultSchedulingManager(
      JobService jobService,
      JobConfigurationService jobConfigurationService,
      MessageService messageService,
      Notifier notifier,
      LeaderManager leaderManager,
      @Qualifier("taskScheduler") TaskScheduler jobScheduler,
      AsyncTaskExecutor taskExecutor,
      CacheProvider cacheProvider) {
    super(
        jobService,
        jobConfigurationService,
        messageService,
        leaderManager,
        notifier,
        cacheProvider);
    checkNotNull(jobConfigurationService);
    checkNotNull(messageService);
    checkNotNull(leaderManager);
    checkNotNull(jobScheduler);
    checkNotNull(taskExecutor);
    checkNotNull(jobService);

    this.jobScheduler = jobScheduler;
    this.taskExecutor = taskExecutor;

    jobScheduler.scheduleWithFixedDelay(this::clusterHeartbeat, Duration.ofSeconds(30));
  }

  @Override
  public boolean isScheduled(JobConfiguration conf) {
    Future<?> removeFromSchedule = removeFromScheduleByIdOrName.get(getKey(conf));
    return removeFromSchedule != null && !removeFromSchedule.isDone();
  }

  @Override
  public void schedule(JobConfiguration conf) {
    boolean isCronTriggered = conf.getJobType().getSchedulingType() == SchedulingType.CRON;
    if (isCronTriggered && isNullOrEmpty(conf.getCronExpression())) {
      log.warn(
          "Job {} of type {} cannot be scheduled as it has no CRON expression.",
          conf.getUid(),
          conf.getJobType());
      return;
    }
    log.info("Scheduling job: {}", conf);

    if (scheduleTask(
        conf,
        task ->
            isCronTriggered
                ? scheduleCronBased(conf, task)
                : scheduleFixedDelayBased(conf, task))) {
      log.info("Scheduled job: {}", conf);
    }
  }

  @Override
  public void scheduleWithStartTime(JobConfiguration conf, Date startTime) {
    if (scheduleTask(conf, task -> scheduleTimeBased(startTime, task))) {
      log.info("Scheduled job: {} with start time: {}", conf, getMediumDateString(startTime));
    }
  }

  private boolean scheduleTask(JobConfiguration conf, Function<Runnable, Future<?>> schedule) {
    String jobId = conf.getUid();
    if (isNullOrEmpty(jobId) && isNullOrEmpty(conf.getName())) {
      log.warn("Job of type {} cannot be scheduled as it has no UID or name", conf.getJobType());
      return false;
    }
    Runnable task = conf.isInMemoryJob() ? () -> execute(conf) : () -> execute(jobId);
    Future<?> removeFromSchedule = schedule.apply(task);
    String key = getKey(conf);
    Future<?> removeScheduledBefore = removeFromScheduleByIdOrName.put(key, removeFromSchedule);
    removeFromSchedule(conf, key, removeScheduledBefore);
    log.info("Job {} of type {} has been added to the schedule", key, conf.getJobType());
    return true;
  }

  private Future<?> scheduleFixedDelayBased(JobConfiguration conf, Runnable task) {
    return jobScheduler.scheduleWithFixedDelay(
        task,
        Instant.now().plusSeconds(DEFAULT_INITIAL_DELAY_S),
        Duration.of(conf.getDelay(), ChronoUnit.SECONDS));
  }

  private Future<?> scheduleCronBased(JobConfiguration conf, Runnable task) {
    return jobScheduler.schedule(task, new CronTrigger(conf.getCronExpression()));
  }

  private Future<?> scheduleTimeBased(Date startTime, Runnable task) {
    return jobScheduler.schedule(task, startTime);
  }

  @Override
  public boolean executeNow(JobConfiguration conf) {
    if (conf == null || conf.getJobType() == null || isRunning(conf.getJobType())) {
      return false;
    }
    log.info("Scheduler initiated execution of job: {}", conf);
    Runnable task = () -> execute(conf);
    taskExecutor.executeTaskWithCancelation(task);
    return true;
  }

  @Override
  public void stop(JobConfiguration conf) {
    String key = getKey(conf);
    if (!isNullOrEmpty(key)) {
      removeFromSchedule(conf, key, removeFromScheduleByIdOrName.remove(key));
    }
  }

  private void removeFromSchedule(JobConfiguration conf, String key, Future<?> removeFromSchedule) {
    JobType type = conf.getJobType();
    if (removeFromSchedule == null) {
      log.debug("Job '{}' of type '{}' was not scheduled before.", key, type);
      return;
    }
    if (removeFromSchedule.isDone()) {
      log.info(
          "Removing job '{}' of type '{}' from schedule had no effect as no further runs were scheduled.",
          key,
          type);
      return;
    }
    boolean accepted = removeFromSchedule.cancel(false);
    log.info("Removed job '{}' of type: '{}' from schedule: '{}'", key, type, accepted);
  }

  private static String getKey(JobConfiguration conf) {
    return isNullOrEmpty(conf.getUid()) ? conf.getName() : conf.getUid();
  }
}
