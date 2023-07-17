package org.hisp.dhis.scheduling;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The service abstraction with all support functions the {@link DefaultSchedulingManager} needs.
 * This is used to abstract way other services in the system as well as all DB access.
 *
 * @author Jan Bernitt
 */
public interface JobConfigurationRunner {

  /**
   * @return true, if node is or become the leader, else false
   */
  boolean tryBecomeLeader();

  /**
   * Called to stay being the leader. Being the leader times out if not renewed this way.
   * By renewing in a regular interval a leader stays the leader until he is unable to renew.
   */
  void assureAsLeader();

  /**
   * A successful update means the DB was updated and the state flipped from {@link
   * JobStatus#SCHEDULED} to {@link JobStatus#RUNNING}. This also sets the {@link
   * JobConfiguration#getLastExecuted()} to "now" and the {@link
   * JobConfiguration#getLastExecutedStatus()} to {@link JobStatus#RUNNING}.
   *
   * @param uid of the job to switch to a {@link JobStatus#RUNNING} state
   * @return true, if update was successful and the execution should begin, otherwise false
   */
  boolean tryRun(@Nonnull String uid);

  /**
   * Called when a run of the provided job is about to be processed.
   * @param id the job that will be executed
   * @return the progress tracker to use
   */
  JobProgress prepareRun(@Nonnull String id);

  /**
   * Heartbeat signal while processing a job so observers can identify a hanging or stale run. In
   * such a case the methods gets no longer called which stops updating the alive timestamp in the
   * database for the provided job configuration. If the alive timestamp is too old the job is
   * considered hanging or stale.
   *
   * <p>There is no exact timeout as the update to this is done cooperative as part of the
   * processing. Some steps in the processing may take seconds or few minutes making it not send a
   * heartbeat during that longer step time.
   *
   * @param id of the job being executed at the moment
   */
  void assureAsRunning(@Nonnull String id);

  /**
   * Called on a successful completion of the job process.
   *
   * <p>Switches back the status from {@link JobStatus#RUNNING} to {@link JobStatus#SCHEDULED} and
   * records the {@link JobConfiguration#getLastRuntimeExecution()} as the time between the {@link
   * JobConfiguration#getLastExecuted()} and "now".
   *
   * <p>The {@link JobConfiguration#getLastExecutedStatus()} is set to {@link JobStatus#COMPLETED}.
   *
   * <p>Disabled status is preserved. This means if the {@link JobConfiguration#isEnabled()} is
   * false the status is updated to {@link JobStatus#DISABLED} when this method is called.
   *
   * <p>Idempotent: If the status is not {@link JobStatus#RUNNING} when called the call has no effect.
   *
   * @param id the job that finished running
   */
  void completeRun(@Nonnull String id);

  /**
   * Adjusts a job after it failed before completion. The {@link JobConfiguration#getJobStatus()} is
   * changed from {@link JobStatus#RUNNING} to {@link JobStatus#SCHEDULED}.
   *
   * <p>Records the {@link JobConfiguration#getLastRuntimeExecution()} as the time between the
   * {@link JobConfiguration#getLastExecuted()} and "now" and updates the {@link
   * JobConfiguration#getLastExecutedStatus()} to {@link JobStatus#FAILED}.
   *
   * <p>Disabled status is preserved. This means if the {@link JobConfiguration#isEnabled()} is
   * false the status is updated to {@link JobStatus#DISABLED} when this method is called.
   *
   * <p>Idempotent: If the status is not {@link JobStatus#RUNNING} when called the call has no effect.
   *
   * @param id the job that failed before it could complete
   */
  void failRun(@Nonnull String id, @Nonnull Exception ex);

  /**
   * Adjusts a job after it has been cancelled. The {@link JobConfiguration#getJobStatus()} is
   * changed from {@link JobStatus#RUNNING} to {@link JobStatus#SCHEDULED}.
   *
   * <p>Records the {@link JobConfiguration#getLastRuntimeExecution()} as the time between the
   * {@link JobConfiguration#getLastExecuted()} and "now" and updates the {@link
   * JobConfiguration#getLastExecutedStatus()} to {@link JobStatus#STOPPED}.
   *
   * <p>Disabled status is preserved. This means if the {@link JobConfiguration#isEnabled()} is
   * false the status is updated to {@link JobStatus#DISABLED} when this method is called.
   *
   * <p>Idempotent: If the status is not {@link JobStatus#RUNNING} when called the call has no effect.
   *
   * @param id the job that got cancelled
   */
  void cancelRun(@Nonnull String id);
}
