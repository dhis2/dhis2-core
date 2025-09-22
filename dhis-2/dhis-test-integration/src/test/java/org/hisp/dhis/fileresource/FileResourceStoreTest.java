/*
 * Copyright (c) 2004-2025, University of Oslo
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobConfigurationStore;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.SchedulingType;
import org.hisp.dhis.test.integration.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class FileResourceStoreTest extends IntegrationTestBase {

  @Autowired FileResourceStore fileResourceStore;
  @Autowired JobConfigurationStore jobConfigurationStore;

  @Test
  @DisplayName(
      "Getting all unassigned file resources with JOB_DATA domain should retrieve expected results")
  void getAllUnassignedWithJobDataTest() {
    // given 5 file resources exist with a mix of domains
    // 3 JOB_DATA (2 unassigned, 1 assigned)
    // 1 DATA_VALUE
    // 1 ICON
    FileResource fr0 =
        new FileResource(
            "file resource 0", "application/json", 100L, "20", FileResourceDomain.JOB_DATA);
    fr0.setAssigned(true);
    FileResource fr1 =
        new FileResource(
            "file resource 1", "application/json", 100L, "20", FileResourceDomain.JOB_DATA);
    fr1.setAssigned(false);
    FileResource fr2 =
        new FileResource(
            "file resource 2", "application/json", 200L, "30", FileResourceDomain.JOB_DATA);
    fr2.setAssigned(false);
    FileResource fr3 =
        new FileResource("file resource 3", "image/svg+xml", 100L, "20", FileResourceDomain.ICON);
    fr3.setAssigned(false);
    FileResource fr4 =
        new FileResource(
            "file resource 4", "application/pdf", 100L, "20", FileResourceDomain.DATA_VALUE);
    fr4.setAssigned(false);

    fileResourceStore.save(fr0);
    fileResourceStore.save(fr1);
    fileResourceStore.save(fr2);
    fileResourceStore.save(fr3);
    fileResourceStore.save(fr4);

    // and job config exists for only 1 of the unassigned file resources
    JobConfiguration config = new JobConfiguration(JobType.METADATA_IMPORT);
    config.setSchedulingType(SchedulingType.ONCE_ASAP);

    // simulate file resource having the same UID as its job, which is how it works for JOB_DATA
    config.setUid(fr1.getUid());
    jobConfigurationStore.save(config);

    entityManager.flush();

    // when retrieving by unassigned and JOB_DATA domain and no job config
    List<FileResource> allUnassignedByDomain =
        fileResourceStore.getAllUnassignedByJobDataDomainWithNoJobConfig();

    // then 1 unassigned file resources (JOB_DATA), with no job config is returned
    assertEquals(1, allUnassignedByDomain.size());
    assertTrue(allUnassignedByDomain.contains(fr2));
  }
}
