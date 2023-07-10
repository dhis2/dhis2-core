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

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableSet;
import static java.util.Comparator.comparing;
import static org.hisp.dhis.scheduling.JobStatus.COMPLETED;
import static org.hisp.dhis.scheduling.JobStatus.DISABLED;
import static org.hisp.dhis.scheduling.JobStatus.RUNNING;

import java.util.Collection;
import java.util.Date;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.eventhook.EventUtils;
import org.hisp.dhis.scheduling.JobProgress.Process;
import org.hisp.dhis.system.util.Clock;
import org.slf4j.MDC;

/**
 * Base for synchronous or asynchronous {@link SchedulingManager} implementation that keeps track of
 * running/executing jobs and makes sure only one per {@link JobType} can run at the same time.
 *
 * @author Jan Bernitt
 */
@Slf4j
@AllArgsConstructor
public abstract class AbstractSchedulingManager implements SchedulingManager {
  private final SchedulingManagerSupport support;

  /**
   * Set of currently running jobs. We use a map to use CAS operation {@link Map#putIfAbsent(Object,
   * Object)} to "atomically" start or abort execution.
   */
  private final Map<JobType, ControlledJobProgress> runningLocally = new ConcurrentHashMap<>();

  private final Map<JobType, ControlledJobProgress> completedLocally = new ConcurrentHashMap<>();

  private final Cache<Deque<Process>> runningRemotely;

  private final Cache<Deque<Process>> completedRemotely;

  private final Cache<Boolean> cancelledRemotely;

  protected AbstractSchedulingManager(SchedulingManagerSupport support) {
    this.support = support;

    this.runningRemotely = support.getCacheProvider().createRunningJobsInfoCache();
    this.completedRemotely = support.getCacheProvider().createCompletedJobsInfoCache();
    this.cancelledRemotely = support.getCacheProvider().createJobCancelRequestedCache();
  }

  @PostConstruct
  public void init() {
    support.getLeaderManager().setSchedulingManager(this);
  }

  protected final void clusterHeartbeat() {
    for (Entry<JobType, ControlledJobProgress> info : runningLocally.entrySet()) {
      String key = info.getKey().name();
      if (cancelledRemotely.getIfPresent(key).isPresent()) {
        cancel(info.getKey());
        cancelledRemotely.invalidate(key);
      } else {
        // heart beat update so the entry does not expire
        runningRemotely.put(key, info.getValue().getProcesses());
      }
    }
    // always use most recent local in the cache
    for (Entry<JobType, ControlledJobProgress> localInfo : completedLocally.entrySet()) {
      String key = localInfo.getKey().name();
      Optional<Deque<Process>> remoteInfo = completedRemotely.getIfPresent(key);
      Date now = new Date();
      if (remoteInfo.isEmpty()
          || Process.startedTime(remoteInfo.get(), now)
              .before(Process.startedTime(localInfo.getValue().getProcesses(), now))) {
        completedRemotely.put(key, localInfo.getValue().getProcesses());
      }
    }
  }

  @Override
  public boolean isRunning(JobType type) {
    return isRunningLocally(type) || isRunningRemotely(type);
  }

  final boolean isRunningLocally(JobType type) {
    return runningLocally.containsKey(type);
  }

  final boolean isRunningRemotely(JobType type) {
    return runningRemotely.getIfPresent(type.name()).isPresent();
  }

  @Override
  public final Collection<JobType> getRunningTypes() {
    return unmodifiableUnionOf(runningRemotely, runningLocally);
  }

  @Override
  public final Collection<JobType> getCompletedTypes() {
    return unmodifiableUnionOf(completedRemotely, completedLocally);
  }

  @Override
  public final Collection<Process> getRunningProgress(JobType type) {
    ControlledJobProgress local = runningLocally.get(type);
    if (local != null) {
      return unmodifiableCollection(local.getProcesses());
    }
    var remoteInfo = runningRemotely.getIfPresent(type.name());
    return remoteInfo.isEmpty() ? emptyList() : unmodifiableCollection(remoteInfo.get());
  }

  @Override
  public final Collection<Process> getCompletedProgress(JobType type) {
    ControlledJobProgress local = completedLocally.get(type);
    Collection<Process> localInfo =
        local == null ? emptyList() : unmodifiableCollection(local.getProcesses());
    var remoteInfo = completedRemotely.getIfPresent(type.name());
    if (remoteInfo.isEmpty()) {
      return localInfo;
    }
    Date now = new Date();
    return localInfo.isEmpty()
            || Process.startedTime(remoteInfo.get(), now).after(Process.startedTime(localInfo, now))
        ? unmodifiableCollection(remoteInfo.get())
        : localInfo;
  }

  @Override
  public final void cancel(JobType type) {
    ControlledJobProgress local = runningLocally.get(type);
    if (local != null) {
      local.requestCancellation();
    } else {
      // no matter which node received the cancel this is shared
      cancelledRemotely.put(type.name(), true);
    }
  }

  /**
   * Runs a job queue sequence.
   *
   * @param name name of the queue to run
   * @param startPosition first position in the queue to run
   * @return true, if the entire sequence ran successful, otherwise false
   */
  protected final boolean executeQueue(String name, int startPosition) {
    int position = startPosition;
    while (true) {
      // OBS! intentionally the sequence is reloaded every time to allow for changes of the sequence
      //      while it is running
      List<JobConfiguration> sequence =
          support.getJobConfigurationService().getAllJobConfigurations().stream()
              .filter(c -> name.equals(c.getQueueName()))
              .sorted(comparing(JobConfiguration::getQueuePosition))
              .collect(Collectors.toList());
      if (position >= sequence.size()) {
        return true; // queue done
      }
      JobConfiguration config = sequence.get(position++);
      if (!execute(config) || config.getLastExecutedStatus() != JobStatus.COMPLETED) {
        return false; // stop queue due to error
      }
    }
  }

  /**
   * Run a job potentially in the future.
   *
   * <p>This is used for scheduled jobs to reload the job right before it is run so the most recent
   * configuration is used.
   *
   * @param jobId ID of the job to run
   * @return true when the job ran successful, otherwise false
   */
  protected final boolean execute(String jobId) {
    return execute(support.getJobConfigurationService().getJobConfigurationByUid(jobId));
  }

  /**
   * @param configuration the job to run now
   * @return true when the job ran successful, otherwise false
   */
  protected final boolean execute(JobConfiguration configuration) {
    if (!configuration.isEnabled()) {
      return false;
    }
    JobType type = configuration.getJobType();
    if (configuration.isLeaderOnlyJob() && !support.getLeaderManager().isLeader()) {
      whenLeaderOnlyOnNonLeader(configuration);
      return false;
    }
    ControlledJobProgress progress = createJobProgress(configuration);
    // OBS: we cannot use computeIfAbsent since we have no way of
    // atomically create and find out if there was a value before
    // so we need to pay the price of creating the progress up front
    if (runningLocally.putIfAbsent(type, progress) != null) {
      whenAlreadyRunning(configuration);
      return false;
    }
    if (!runningRemotely.putIfAbsent(type.name(), progress.getProcesses())) {
      runningLocally.remove(type);
      whenAlreadyRunning(configuration);
      return false;
    }

    Clock clock = new Clock().startClock();
    try {
      log.debug(String.format("Job started: '%s'", configuration.getName()));
      configuration.setJobStatus(JobStatus.RUNNING);
      if (!configuration.isInMemoryJob()) {
        support.getJobConfigurationService().updateJobConfiguration(configuration);
      }
      // in memory effect only: mark running (dirty)
      configuration.setLastExecutedStatus(JobStatus.RUNNING);

      String identifier =
          configuration.getUid() != null
              ? "UID:" + configuration.getUid()
              : "TYPE:" + configuration.getJobType().name();
      MDC.put("sessionId", identifier);
      // run the actual job
      support.getEventHookPublisher().publishEvent(EventUtils.schedulerStart(configuration));

      try {
        support.getAuthenticationService().obtainAuthentication(configuration.getExecutedBy());
        support.getJobService().getJob(type).execute(configuration, progress);
      } finally {
        support.getAuthenticationService().clearAuthentication();
      }

      Process process = progress.getProcesses().peekLast();
      if (process != null && process.getStatus() == JobProgress.Status.RUNNING) {
        // automatically complete processes that were not cleanly
        // complected by calling completedProcess at the end of the job
        progress.completedProcess("(process completed implicitly)");
      }
      boolean wasSuccessfulRun =
          !progress.isCancellationRequested()
              && progress.getProcesses().stream()
                  .allMatch(p -> p.getStatus() == JobProgress.Status.SUCCESS);
      if (configuration.getLastExecutedStatus() == RUNNING) {
        JobStatus errorStatus =
            progress.isCancellationRequested() ? JobStatus.STOPPED : JobStatus.FAILED;
        configuration.setLastExecutedStatus(wasSuccessfulRun ? JobStatus.COMPLETED : errorStatus);
      }

      if (wasSuccessfulRun) {
        support.getEventHookPublisher().publishEvent(EventUtils.schedulerCompleted(configuration));
      } else {
        support.getEventHookPublisher().publishEvent(EventUtils.schedulerFailed(configuration));
      }

      return wasSuccessfulRun;
    } catch (Exception ex) {
      progress.failedProcess(ex);
      whenRunThrewException(configuration, ex, progress);
      support.getEventHookPublisher().publishEvent(EventUtils.schedulerFailed(configuration));
      return false;
    } finally {
      completedLocally.put(type, runningLocally.remove(type));
      runningRemotely.invalidate(type.name());
      whenRunIsDone(configuration, clock);
      MDC.remove("sessionId");
    }
  }

  private ControlledJobProgress createJobProgress(JobConfiguration configuration) {
    JobProgress tracker =
        configuration.getJobType().isUsingNotifications()
            ? new NotifierJobProgress(support.getNotifier(), configuration)
            : NoopJobProgress.INSTANCE;
    return new ControlledJobProgress(support.getMessageService(), configuration, tracker, true);
  }

  private void whenRunIsDone(JobConfiguration configuration, Clock clock) {
    String duration = clock.time();
    if (configuration.getLastExecutedStatus() == COMPLETED) {
      log.debug(
          "Job executed successfully: '{}'. Time used: '{}'", configuration.getName(), duration);
    }
    configuration.setJobStatus(JobStatus.SCHEDULED);
    configuration.setLastExecuted(new Date(clock.getStartTime()));
    configuration.setLastRuntimeExecution(duration);

    if (configuration.isInMemoryJob()) {
      return;
    }
    JobConfiguration persistentConfiguration =
        support.getJobConfigurationService().getJobConfigurationByUid(configuration.getUid());

    if (persistentConfiguration != null) {
      if (persistentConfiguration.getJobStatus() == DISABLED
          || !persistentConfiguration.isEnabled()) {
        // keep disabled status change from DB
        configuration.setJobStatus(DISABLED);
        configuration.setEnabled(false);
      }
      // only if it still exists: update
      support.getJobConfigurationService().updateJobConfiguration(configuration);
    }
  }

  private void whenRunThrewException(
      JobConfiguration configuration, Exception ex, ControlledJobProgress progress) {
    String message = String.format("Job failed: '%s'", configuration.getName());
    log.error(message, ex);
    log.error(DebugUtils.getStackTrace(ex));
    if (!configuration.isInMemoryJob()) {
      support.getMessageService().sendSystemErrorNotification(message, ex);
    }
    configuration.setLastExecutedStatus(
        progress.isCancellationRequested() ? JobStatus.STOPPED : JobStatus.FAILED);
    if (ex instanceof InterruptedException) {
      Thread.currentThread().interrupt();
    }
  }

  private void whenLeaderOnlyOnNonLeader(JobConfiguration configuration) {
    log.debug(
        "Not a leader, skipping job of type: '{}' and name: '{}'",
        configuration.getJobType(),
        configuration.getName());
  }

  private void whenAlreadyRunning(JobConfiguration configuration) {
    String message =
        String.format(
            "Job aborted: '%s', job of type: '%s' is already running",
            configuration.getName(), configuration.getJobType());
    log.error(message);
    if (!configuration.isInMemoryJob()) {
      support
          .getMessageService()
          .sendSystemErrorNotification(message, new RuntimeException(message));
    }
  }

  private static Set<JobType> unmodifiableUnionOf(
      Cache<Deque<Process>> cluster, Map<JobType, ControlledJobProgress> local) {
    EnumSet<JobType> all = EnumSet.noneOf(JobType.class);
    cluster.keys().forEach(key -> all.add(JobType.valueOf(key)));
    all.addAll(local.keySet());
    return unmodifiableSet(all);
  }
}
