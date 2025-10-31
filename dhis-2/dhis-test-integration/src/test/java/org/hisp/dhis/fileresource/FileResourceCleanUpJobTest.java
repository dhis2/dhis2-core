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

import static org.hisp.dhis.fileresource.FileResourceDomain.DATA_VALUE;
import static org.hisp.dhis.fileresource.FileResourceDomain.DOCUMENT;
import static org.hisp.dhis.fileresource.FileResourceDomain.ICON;
import static org.hisp.dhis.fileresource.FileResourceDomain.JOB_DATA;
import static org.hisp.dhis.fileresource.FileResourceDomain.MESSAGE_ATTACHMENT;
import static org.hisp.dhis.fileresource.FileResourceDomain.ORG_UNIT;
import static org.hisp.dhis.fileresource.FileResourceDomain.PUSH_ANALYSIS;
import static org.hisp.dhis.fileresource.FileResourceDomain.USER_AVATAR;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mockStatic;

import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.joda.time.DateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Kristian Wærstad
 */
class FileResourceCleanUpJobTest extends PostgresIntegrationTestBase {

  @Autowired private FileResourceCleanUpJob cleanUpJob;

  @Autowired private FileResourceStore fileResourceStore;

  @Test
  @DisplayName("Unassigned file resources are deleted")
  void unassignedTest() {
    // mock grace period to simulate that it has passed
    try (MockedStatic<DefaultFileResourceService> mocked =
        mockStatic(DefaultFileResourceService.class)) {
      mocked
          .when(DefaultFileResourceService::getGracePeriod)
          .thenReturn(DateTime.now().plusDays(1));

      // given 2 file resources exists for each domain (1 assigned, 1 unassigned)
      FileResource fr1 = createFileResource('a', ORG_UNIT, false, "fr1".getBytes());
      FileResource fr2 = createFileResource('b', ORG_UNIT, true, "fr2".getBytes());

      FileResource fr3 = createFileResource('c', DOCUMENT, false, "fr3".getBytes());
      FileResource fr4 = createFileResource('d', DOCUMENT, true, "fr4".getBytes());

      FileResource fr5 = createFileResource('e', ICON, false, "fr5".getBytes());
      FileResource fr6 = createFileResource('f', ICON, true, "fr6".getBytes());

      FileResource fr7 = createFileResource('g', USER_AVATAR, false, "fr7".getBytes());
      FileResource fr8 = createFileResource('h', USER_AVATAR, true, "fr8".getBytes());

      FileResource fr9 = createFileResource('i', MESSAGE_ATTACHMENT, false, "fr9".getBytes());
      FileResource fr10 = createFileResource('j', MESSAGE_ATTACHMENT, true, "fr10".getBytes());

      FileResource fr11 = createFileResource('k', PUSH_ANALYSIS, false, "fr11".getBytes());
      FileResource fr12 = createFileResource('l', PUSH_ANALYSIS, true, "fr12".getBytes());

      FileResource fr13 = createFileResource('m', JOB_DATA, false, "fr13".getBytes());
      FileResource fr14 = createFileResource('n', JOB_DATA, true, "fr14".getBytes());

      FileResource fr15 = createFileResource('o', DATA_VALUE, false, "fr15".getBytes());
      FileResource fr16 = createFileResource('p', DATA_VALUE, true, "fr16".getBytes());

      // when the cleanup job runs
      cleanUpJob.execute(null, JobProgress.noop());

      // then unassigned file resources are deleted
      assertNull(fileResourceStore.getByUid(fr1.getUid()));
      assertNull(fileResourceStore.getByUid(fr3.getUid()));
      assertNull(fileResourceStore.getByUid(fr5.getUid()));
      assertNull(fileResourceStore.getByUid(fr7.getUid()));
      assertNull(fileResourceStore.getByUid(fr13.getUid()));
      assertNull(fileResourceStore.getByUid(fr15.getUid()));

      // and assigned file resources still exist
      assertNotNull(fileResourceStore.getByUid(fr2.getUid()));
      assertNotNull(fileResourceStore.getByUid(fr4.getUid()));
      assertNotNull(fileResourceStore.getByUid(fr6.getUid()));
      assertNotNull(fileResourceStore.getByUid(fr8.getUid()));
      assertNotNull(fileResourceStore.getByUid(fr14.getUid()));
      assertNotNull(fileResourceStore.getByUid(fr16.getUid()));

      // and domains not to be deleted (MESSAGE_ATTACHMENT, PUSH_ANALYSIS) still exist whether
      // assigned or not
      assertNotNull(fileResourceStore.getByUid(fr9.getUid()));
      assertNotNull(fileResourceStore.getByUid(fr10.getUid()));
      assertNotNull(fileResourceStore.getByUid(fr11.getUid()));
      assertNotNull(fileResourceStore.getByUid(fr12.getUid()));
    }
  }

  private FileResource createFileResource(
      char c, FileResourceDomain domain, boolean assigned, byte[] content) {
    FileResource fr = createFileResource(c, content);
    fr.setDomain(domain);
    fr.setAssigned(assigned);
    fileResourceStore.save(fr);
    entityManager.flush();
    return fr;
  }
}
