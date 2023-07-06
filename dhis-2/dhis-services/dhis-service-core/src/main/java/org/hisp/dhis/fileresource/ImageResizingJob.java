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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.JobType;
import org.springframework.stereotype.Component;

/**
 * Job will fetch all the image FileResources with flag hasMultiple set to false. It will process
 * those image FileResources create three images files for each of them. Once created, images will
 * be stored at EWS and flag hasMultiple is set to true. @Author Zubair Asghar.
 */
@Slf4j
@Component("imageResizingJob")
public class ImageResizingJob implements Job {
  private final FileResourceContentStore fileResourceContentStore;

  private final FileResourceService fileResourceService;

  private final ImageProcessingService imageProcessingService;

  public ImageResizingJob(
      FileResourceContentStore fileResourceContentStore,
      FileResourceService fileResourceService,
      ImageProcessingService imageProcessingService) {
    this.fileResourceContentStore = fileResourceContentStore;
    this.fileResourceService = fileResourceService;
    this.imageProcessingService = imageProcessingService;
  }

  @Override
  public JobType getJobType() {
    return JobType.IMAGE_PROCESSING;
  }

  @Override
  public void execute(JobConfiguration jobConfiguration, JobProgress progress) {
    List<FileResource> fileResources = fileResourceService.getAllUnProcessedImagesFiles();

    File tmpFile = null;

    String storageKey;

    int count = 0;

    for (FileResource fileResource : fileResources) {
      String key = fileResource.getStorageKey();

      tmpFile = new File(UUID.randomUUID().toString());

      if (!fileResourceContentStore.fileResourceContentExists(key)) {
        log.error(
            "The referenced file could not be found for FileResource: " + fileResource.getUid());
        continue;
      }

      try (FileOutputStream fileOutputStream = new FileOutputStream(tmpFile)) {

        fileResourceContentStore.copyContent(key, fileOutputStream);

        Map<ImageFileDimension, File> imageFiles =
            imageProcessingService.createImages(fileResource, tmpFile);

        storageKey = fileResourceContentStore.saveFileResourceContent(fileResource, imageFiles);

        if (storageKey != null) {
          fileResource.setHasMultipleStorageFiles(true);
          fileResourceService.updateFileResource(fileResource);
          count++;
        } else {
          log.error("File upload failed");
        }
      } catch (Exception e) {
        DebugUtils.getStackTrace(e);
      } finally {
        try {
          if (tmpFile != null) {
            Files.deleteIfExists(tmpFile.toPath());
          }
        } catch (IOException ioe) {
          log.warn(
              String.format("Temporary file '%s' could not be deleted.", tmpFile.toPath()), ioe);
        }
      }
    }

    log.info(String.format("Number of FileResources processed: %d", count));
  }
}
