package org.hisp.dhis.scheduling;

import static java.lang.String.format;
import static org.hisp.dhis.eventhook.EventUtils.schedulerCompleted;
import static org.hisp.dhis.eventhook.EventUtils.schedulerFailed;
import static org.hisp.dhis.eventhook.EventUtils.schedulerStart;

import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.eventhook.EventHookPublisher;
import org.hisp.dhis.leader.election.LeaderManager;
import org.hisp.dhis.message.MessageService;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultJobConfigurationRunner implements JobConfigurationRunner {

  private final LeaderManager leaderManager;
  private final JobConfigurationStore jobConfigurationStore;
  private final EventHookPublisher events;
  private final MessageService messages;

  @Override
  public boolean tryBecomeLeader() {
    return false;
  }

  @Override
  public void assureAsLeader() {

  }

  @Override
  public boolean tryRun(@Nonnull String uid) {
    if (!jobConfigurationStore.tryRun(uid)) return false;
    JobConfiguration job = jobConfigurationStore.getByUid(uid);
    if (job == null) return false;
    doSafely("start", "MDC.put", () -> MDC.put("sessionId", getSessionId(job)));
    doSafely("start", "publishEvent", () -> events.publishEvent(schedulerStart(job)));
    return true;
  }

  private static String getSessionId(JobConfiguration job) {
    return job.getUid() != null ? "UID:" + job.getUid() : "TYPE:" + job.getJobType().name();
  }

  @Override
  public JobProgress prepareRun(@Nonnull String id) {
    return null;
  }

  @Override
  public void assureAsRunning(@Nonnull String id) {
    jobConfigurationStore.assureRunning(id);
  }

  @Override
  public void completeRun(@Nonnull String id) {
    if (jobConfigurationStore.tryStop(id, JobStatus.COMPLETED)) {
      JobConfiguration job = jobConfigurationStore.getByUid(id);
      if (job == null) return;
      doSafely("complete", "publishEvent", () -> events.publishEvent(schedulerCompleted(job)));
      //TODO
    }
  }

  @Override
  public void failRun(@Nonnull String id, @Nonnull Exception ex) {
    if (jobConfigurationStore.tryStop(id, JobStatus.FAILED)) {
      JobConfiguration job = jobConfigurationStore.getByUid(id);
      if (job == null) return;
      String message = String.format("Job failed: '%s'", job.getName());
      log.error(message, ex);
      log.error(DebugUtils.getStackTrace(ex));
      doSafely("fail", "MDC.remove", () -> MDC.remove("sessionId"));
      doSafely("fail", "publishEvent", () -> events.publishEvent(schedulerFailed(job)));
      doSafely("fail", "sendSystemErrorNotification", () -> messages.sendSystemErrorNotification(message, ex));
      //TODO
    }
  }

  @Override
  public void cancelRun(@Nonnull String id) {
    if (jobConfigurationStore.tryStop(id, JobStatus.STOPPED)) {
      JobConfiguration job = jobConfigurationStore.getByUid(id);
      if (job == null) return;
      doSafely("cancel", "MDC.remove", () -> MDC.remove("sessionId"));
      doSafely("cancel", "publishEvent", () -> events.publishEvent(schedulerFailed(job)));
      //TODO
    }
  }

  private void doSafely(String phase, String step, Runnable action) {
    try {
      action.run();
    } catch (Exception ex) {
      log.error(format("Exception while running job %s post action: %s", phase, step), ex);
    }
  }
}
