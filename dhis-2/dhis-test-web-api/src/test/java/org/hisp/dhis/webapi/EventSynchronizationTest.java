/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

import org.hisp.dhis.dxf2.sync.EventSynchronization;
import org.hisp.dhis.dxf2.sync.SyncUtils;
import org.hisp.dhis.dxf2.synch.AvailabilityStatus;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.SchedulingManager;
import org.hisp.dhis.scheduling.parameters.EventProgramsDataSynchronizationJobParameters;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.CurrentUserUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

class EventSynchronizationTest extends DhisControllerIntegrationTest {

  @Autowired SchedulingManager synchronousSchedulingManager;
  @Autowired EventSynchronization eventSynchronization;
  @Autowired SystemSettingManager systemSettingManager;

  /**
   * This test only checks the initial setup of an {@link EventSynchronization}. Basically checking
   * that a valid {@link org.hisp.dhis.user.User} is logged in to perform the sync and performing an
   * initial DB call to check if any events need syncing (in this case it's 0). It's not possible to
   * have a reliable, automated test, fully checking an {@link EventSynchronization}, as 2 DHIS2
   * instances are required.
   */
  @Test
  @DisplayName(
      "Running an Event Sync job completes successfully when there is no current user logged in")
  void eventSyncJobTest() {
    // given
    systemSettingManager.saveSystemSetting(SettingKey.REMOTE_INSTANCE_USERNAME, "admin");
    EventProgramsDataSynchronizationJobParameters params =
        new EventProgramsDataSynchronizationJobParameters();

    JobConfiguration config =
        new JobConfiguration("event-sync", JobType.EVENT_PROGRAMS_DATA_SYNC, null, true);
    config.setJobParameters(params);

    AvailabilityStatus remoteServerIsAvailable =
        new AvailabilityStatus(true, "Authentication was successful", HttpStatus.OK);

    // ensure that no user is logged in to mirror an async job starting (pre v41 behaviour)
    injectSecurityContext(null);
    assertNull(CurrentUserUtil.getCurrentUsername());

    // mock remote server availability
    try (MockedStatic<SyncUtils> syncUtils = mockStatic(SyncUtils.class)) {
      syncUtils
          .when(() -> SyncUtils.testServerAvailability(any(), any()))
          .thenReturn(remoteServerIsAvailable);

      // when
      boolean synchronizationSuccessful =
          assertDoesNotThrow(() -> synchronousSchedulingManager.executeNow(config));

      // then
      assertTrue(synchronizationSuccessful);
    }
  }
}
