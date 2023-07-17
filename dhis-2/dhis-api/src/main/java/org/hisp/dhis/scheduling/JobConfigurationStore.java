package org.hisp.dhis.scheduling;

import java.util.List;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.GenericDimensionalObjectStore;

/**
 * @author Jan Bernitt
 */
public interface JobConfigurationStore extends GenericDimensionalObjectStore<JobConfiguration> {

  boolean tryStop(@Nonnull String uid, JobStatus lastExecutedStatus);

  /**
   * @return all job configurations that could potentially be started based on their cron expression or delay time
   */
  List<JobConfigurationTrigger> getAllTriggers();

  /**
   * A successful update means the DB was updated and the state flipped from {@link
   * JobStatus#SCHEDULED} to {@link JobStatus#RUNNING}.
   * <p>
   * If the update is successful the {@link JobConfiguration#getLastExecuted()} is also updated to "now".
   *
   * @param uid of the job to switch to a {@link JobStatus#RUNNING} state
   * @return true, if update was successful, otherwise false
   */
  boolean tryRun(@Nonnull String uid);

  boolean assureRunning(@Nonnull String uid);
}
