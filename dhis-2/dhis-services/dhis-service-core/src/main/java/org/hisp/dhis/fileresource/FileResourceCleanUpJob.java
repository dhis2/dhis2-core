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
package org.hisp.dhis.fileresource;

import static org.hisp.dhis.fileresource.FileResourceDomain.DOCUMENT;
import static org.hisp.dhis.fileresource.FileResourceDomain.ICON;
import static org.hisp.dhis.fileresource.FileResourceDomain.ORG_UNIT;
import static org.hisp.dhis.fileresource.FileResourceDomain.USER_AVATAR;
import static org.hisp.dhis.scheduling.JobProgress.FailurePolicy.SKIP_ITEM_OUTLIER;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.DeleteNotAllowedException;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.fileresource.events.FileDeletedEvent;
import org.hisp.dhis.icon.Icon;
import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.JobEntry;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.springframework.stereotype.Component;

/**
 * The {@link FileResourceCleanUpJob} is responsible for cleaning up unassigned {@link
 * FileResource}s. Each {@link FileResourceDomain} flow e.g. create {@link DataValue}, delete {@link
 * Icon} etc. is responsible for setting and unsetting the {@code assigned} value of a {@link
 * FileResource}. The {@link FileResourceCleanUpJob} will delete {@link FileResource} entries in the
 * {@code fileresource} table. When those deletes occur, they trigger {@link FileDeletedEvent}s
 * which then delete the actual file from the file system. <br>
 * There are 3 targeted delete operations, as each have slightly different criteria for deletion:
 *
 * <ol>
 *   <li>{@link FileResource} domains for {@code DATA_VALUE} will be deleted when:
 *       <ul>
 *         <li>unassigned
 *         <li>{@link DefaultFileResourceService#getGracePeriod()} has passed
 *         <li>{@link FileResourceRetentionStrategy} has passed
 *       </ul>
 *   <li>{@link FileResource} domains for {@code JOB_DATA} will be deleted when:
 *       <ul>
 *         <li>unassigned
 *         <li>No {@link FileResource} UID in the {@code jobconfiguration} table for {@code
 *             ONCE_ASAP} jobs
 *       </ul>
 *   <li>{@link FileResource} domains for {@code domainsToDeleteWhenUnassigned} will be deleted
 *       when:
 *       <ul>
 *         <li>unassigned
 *         <li>{@link DefaultFileResourceService#getGracePeriod()} has passed
 *       </ul>
 * </ol>
 *
 * <br>
 * The job currently excludes {@code PUSH_ANALYSIS} & {@code MESSAGE_ATTACHMENT} domains. Product
 * need to review these. As such, these files are not currently cleaned up.<br>
 * This job runs nightly (see {@link JobType for details}.<br>
 * Reviewed and updated for 2.43
 *
 * @author davidmackessy
 */
@Slf4j
@AllArgsConstructor
@Component
public class FileResourceCleanUpJob implements Job {
  private final FileResourceService fileResourceService;
  private final SystemSettingsProvider settingsProvider;

  // PUSH_ANALYSIS & MESSAGE_ATTACHMENT currently omitted
  private final Set<FileResourceDomain> domainsToDeleteWhenUnassigned =
      Set.of(DOCUMENT, ICON, ORG_UNIT, USER_AVATAR);

  @Override
  public JobType getJobType() {
    return JobType.FILE_RESOURCE_CLEANUP;
  }

  @Override
  public void execute(JobEntry jobConfiguration, JobProgress progress) {
    progress.startingProcess("Clean-up file resources");
    FileResourceRetentionStrategy retentionStrategy =
        settingsProvider.getCurrentSettings().getFileResourceRetentionStrategy();

    List<UnassignedFileResource> deletedFileResources = new ArrayList<>();

    // DV FRs
    if (!FileResourceRetentionStrategy.FOREVER.equals(retentionStrategy)) {
      List<FileResource> dvUnassigned =
          fileResourceService.getExpiredDataValueFileResources(retentionStrategy);
      deleteFrs(
          progress,
          "Deleting unassigned DataValue file resources",
          dvUnassigned,
          deletedFileResources);
    }

    // Job Data FRs
    List<FileResource> jobDataUnassigned =
        fileResourceService.getAllUnassignedByJobDataDomainWithNoJobConfig();
    deleteFrs(
        progress,
        "Deleting unassigned JOB_DATA file resources associated with deleted ONCE_ASAP jobs",
        jobDataUnassigned,
        deletedFileResources);

    // Remaining applicable FRs to be deleted
    List<FileResource> remainingUnassigned =
        fileResourceService.getExpiredFileResources(domainsToDeleteWhenUnassigned);
    deleteFrs(
        progress,
        "Deleting unassigned file resources for domains %s"
            .formatted(
                domainsToDeleteWhenUnassigned.stream()
                    .map(Enum::name)
                    .collect(Collectors.joining())),
        remainingUnassigned,
        deletedFileResources);

    log.info(
        "Deleted {} unassigned FileResources {}",
        deletedFileResources.size(),
        deletedFileResources.isEmpty() ? "" : deletedFileResources);

    progress.completedProcess("File resource clean-up complete");
  }

  private void deleteFrs(
      JobProgress progress,
      String message,
      List<FileResource> fileResources,
      List<UnassignedFileResource> deletedFileResources) {
    progress.startingStage(message, fileResources.size(), SKIP_ITEM_OUTLIER);
    progress.runStage(
        fileResources,
        FileResourceCleanUpJob::toIdentifier,
        fr -> {
          if (safeDelete(fr)) {
            deletedFileResources.add(
                new UnassignedFileResource(fr.getUid(), fr.getName(), fr.getDomain().name()));
          }
        });
    progress.completedStage(null);
  }

  private static String toIdentifier(FileResource fr) {
    return fr.getUid() + ":" + fr.getName();
  }

  private record UnassignedFileResource(String uid, String name, String domain) {
    @Nonnull
    @Override
    public String toString() {
      return """
          {
            uid=%s,
            name=%s,
            domain=%s
          }"""
          .formatted(uid, name, domain);
    }
  }

  /**
   * Attempts to delete a {@link FileResource}. Fixes the {@code assigned} status if it turns out to
   * be referenced by something else
   *
   * @param fileResource the {@link FileResource} to delete
   * @return true if deletion was successful
   */
  private boolean safeDelete(FileResource fileResource) {
    try {
      fileResourceService.deleteFileResource(fileResource);
      return true;
    } catch (DeleteNotAllowedException e) {
      log.error(
          "Could not delete file resource: {}, setting back as assigned. Error: {}",
          fileResource.getUid(),
          e.getMessage());
      fileResource.setAssigned(true);
      fileResourceService.updateFileResource(fileResource);
    }
    return false;
  }
}
