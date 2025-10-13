/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.dxf2.metadata.jobs;

import static org.hisp.dhis.test.TestBase.injectSecurityContextNoSettings;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import org.hisp.dhis.dxf2.metadata.sync.MetadataSyncPreProcessor;
import org.hisp.dhis.dxf2.metadata.version.MetadataVersionDelegate;
import org.hisp.dhis.metadata.version.MetadataVersion;
import org.hisp.dhis.metadata.version.MetadataVersionService;
import org.hisp.dhis.metadata.version.VersionType;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobProgress.Status;
import org.hisp.dhis.scheduling.JobStatus;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.RecordingJobProgress;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.user.SystemUser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MetadataSyncJobTest {

  @Mock private SystemSettingsService settingsService;
  @Mock private MetadataVersionService metadataVersionService;
  @Mock private MetadataVersionDelegate metadataVersionDelegate;

  @BeforeAll
  static void setUp() {
    SystemUser user = new SystemUser();
    injectSecurityContextNoSettings(user);
  }

  @Test
  @DisplayName(
      "Job progress status should be SUCCESS after metadata sync pre-processor setup completes")
  void preprocessSetupTest() {
    // given
    JobConfiguration config = new JobConfiguration();
    config.setJobType(JobType.META_DATA_SYNC);
    config.setLastExecutedStatus(JobStatus.RUNNING);

    RecordingJobProgress jobProgress = new RecordingJobProgress(config.toKey());

    MetadataSyncPreProcessor preProcessor =
        new MetadataSyncPreProcessor(settingsService, null, null, null, null);

    // when
    preProcessor.setUp(null, jobProgress);

    // then
    assertEquals(1, jobProgress.getProgress().getSequence().size());
    assertEquals(Status.SUCCESS, jobProgress.getProgress().getSequence().getFirst().getStatus());
  }

  @Test
  @DisplayName(
      "Job progress status should be SUCCESS after metadata sync pre-processor handle current metadata version completes")
  void handleCurrentMetadataVersionTest() {
    // given
    JobConfiguration config = new JobConfiguration();
    config.setJobType(JobType.META_DATA_SYNC);
    config.setLastExecutedStatus(JobStatus.RUNNING);

    RecordingJobProgress jobProgress = new RecordingJobProgress(config.toKey());

    MetadataSyncPreProcessor preProcessor =
        new MetadataSyncPreProcessor(
            settingsService, metadataVersionService, metadataVersionDelegate, null, null);

    MetadataVersion mdVersion = new MetadataVersion("test", VersionType.BEST_EFFORT);
    when(metadataVersionService.getCurrentVersion()).thenReturn(mdVersion);

    // when
    preProcessor.handleCurrentMetadataVersion(null, jobProgress);

    // then
    assertEquals(1, jobProgress.getProgress().getSequence().size());
    assertEquals(Status.SUCCESS, jobProgress.getProgress().getSequence().getFirst().getStatus());
  }

  @Test
  @DisplayName(
      "Job progress status should be SUCCESS after metadata sync pre-processor handle metadata versions completes")
  void handleMetadataVersionsTest() {
    // given
    JobConfiguration config = new JobConfiguration();
    config.setJobType(JobType.META_DATA_SYNC);
    config.setLastExecutedStatus(JobStatus.RUNNING);

    RecordingJobProgress jobProgress = new RecordingJobProgress(config.toKey());

    MetadataSyncPreProcessor preProcessor =
        new MetadataSyncPreProcessor(
            settingsService, metadataVersionService, metadataVersionDelegate, null, null);

    MetadataVersion mdVersion = new MetadataVersion("test", VersionType.BEST_EFFORT);
    when(metadataVersionDelegate.getMetaDataDifference(mdVersion)).thenReturn(List.of());

    // when
    preProcessor.handleMetadataVersionsList(null, mdVersion, jobProgress);

    // then
    assertEquals(1, jobProgress.getProgress().getSequence().size());
    assertEquals(Status.SUCCESS, jobProgress.getProgress().getSequence().getFirst().getStatus());
  }
}
