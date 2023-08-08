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
package org.hisp.dhis.fileresource;

import static java.util.stream.Collectors.toList;
import static org.hisp.dhis.scheduling.JobProgress.FailurePolicy.SKIP_ITEM_OUTLIER;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.DeleteNotAllowedException;
import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.springframework.stereotype.Component;

/**
 * Deletes any orphaned FileResources. Queries for non-assigned or failed-upload FileResources and
 * deletes them from the database and/or file store.
 *
 * @author Halvdan Hoem Grelland
 */
@Slf4j
@AllArgsConstructor
@Component
public class FileResourceCleanUpJob implements Job {
  private final FileResourceService fileResourceService;

  private final SystemSettingManager systemSettingManager;

  private final FileResourceContentStore fileResourceContentStore;

  @Override
  public JobType getJobType() {
    return JobType.FILE_RESOURCE_CLEANUP;
  }

  @Override
  public void execute(JobConfiguration jobConfiguration, JobProgress progress) {
    progress.startingProcess("Clean-up file resources");
    FileResourceRetentionStrategy retentionStrategy =
        systemSettingManager.getSystemSetting(
            SettingKey.FILE_RESOURCE_RETENTION_STRATEGY, FileResourceRetentionStrategy.class);

    List<Entry<String, String>> deletedOrphans = new ArrayList<>();
    List<Entry<String, String>> deletedAuditFiles = new ArrayList<>();

    // Delete expired FRs
    if (!FileResourceRetentionStrategy.FOREVER.equals(retentionStrategy)) {
      List<FileResource> expired = fileResourceService.getExpiredFileResources(retentionStrategy);
      progress.startingStage("Deleting expired file resources", expired.size(), SKIP_ITEM_OUTLIER);
      progress.runStage(
          expired,
          FileResourceCleanUpJob::toIdentifier,
          fr -> {
            if (safeDelete(fr)) {
              deletedAuditFiles.add(new SimpleEntry<>(fr.getName(), fr.getUid()));
            }
          });
    }

    // Delete failed uploads
    List<FileResource> orphanedFileResources =
        fileResourceService.getOrphanedFileResources().stream()
            .filter(fr -> !isFileStored(fr))
            .collect(toList());
    progress.startingStage(
        "Deleting failed uploads", orphanedFileResources.size(), SKIP_ITEM_OUTLIER);
    progress.runStage(
        orphanedFileResources,
        FileResourceCleanUpJob::toIdentifier,
        fr -> {
          if (safeDelete(fr)) {
            deletedOrphans.add(new SimpleEntry<>(fr.getName(), fr.getUid()));
          }
        });

    if (!deletedOrphans.isEmpty()) {
      log.info(
          String.format(
              "Deleted %d orphaned FileResources: %s",
              deletedOrphans.size(), prettyPrint(deletedOrphans)));
    }

    if (!deletedAuditFiles.isEmpty()) {
      log.info(
          String.format(
              "Deleted %d expired FileResource audits: %s",
              deletedAuditFiles.size(), prettyPrint(deletedAuditFiles)));
    }
    progress.completedProcess(null);
  }

  private static String toIdentifier(FileResource fr) {
    return fr.getUid() + ":" + fr.getName();
  }

  private String prettyPrint(List<Entry<String, String>> list) {
    if (list.isEmpty()) {
      return "";
    }

    StringBuilder sb = new StringBuilder("[ ");

    list.forEach(
        pair -> sb.append(pair.getKey()).append(" , uid: ").append(pair.getValue()).append(", "));

    sb.deleteCharAt(sb.lastIndexOf(",")).append("]");

    return sb.toString();
  }

  private boolean isFileStored(FileResource fileResource) {
    return fileResourceContentStore.fileResourceContentExists(fileResource.getStorageKey());
  }

  /**
   * Attempts to delete a fileresource. Fixes the isAssigned status if it turns out to be referenced
   * by something else
   *
   * @param fileResource the fileresource to delete
   * @return true if the delete was successful
   */
  private boolean safeDelete(FileResource fileResource) {
    try {
      fileResourceService.deleteFileResource(fileResource);
      return true;
    } catch (DeleteNotAllowedException e) {
      fileResource.setAssigned(true);
      fileResourceService.updateFileResource(fileResource);
    }

    return false;
  }
}
