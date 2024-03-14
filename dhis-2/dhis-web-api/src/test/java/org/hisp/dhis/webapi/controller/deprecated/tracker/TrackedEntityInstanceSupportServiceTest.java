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
package org.hisp.dhis.webapi.controller.deprecated.tracker;

import static java.util.Collections.emptyList;
import static org.hisp.dhis.dxf2.deprecated.tracker.TrackedEntityInstanceParams.FALSE;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrackedEntityInstanceSupportServiceTest {

  @Mock private TrackedEntityInstanceService trackedEntityInstanceService;

  @Mock private UserService userService;

  @Mock private ProgramService programService;

  @Mock private TrackerAccessManager trackerAccessManager;

  @Mock private TrackedEntityService instanceService;

  @Mock private TrackedEntityTypeService trackedEntityTypeService;

  @Mock private TrackedEntityService teiService;

  @Mock private TrackedEntityAttributeService trackedEntityAttributeService;

  @InjectMocks private TrackedEntityInstanceSupportService trackedEntityInstanceSupportService;

  private TrackedEntity entity;

  private Program program;

  private User user;

  @BeforeEach
  public void setUpTest() throws Exception {
    entity = new TrackedEntity();
    program = new Program("A");
    program.setUid("A");
    user = new User();
  }

  @Test
  void shouldValidateOwnershipWhenProgramProvided() {
    when(trackedEntityInstanceService.getTrackedEntityInstance(entity.getUid(), user, FALSE))
        .thenReturn(new TrackedEntityInstance());
    when(userService.getUserByUsername(user.getUsername())).thenReturn(user);
    when(programService.getProgram(program.getUid())).thenReturn(program);

    trackedEntityInstanceSupportService.getTrackedEntityInstance(
        entity.getUid(), program.getUid(), emptyList());
    verify(trackedEntityInstanceService, times(1))
        .getTrackedEntityInstance(entity.getUid(), user, FALSE);
  }

  @Test
  void shouldValidateRegisteringOrgUnitWhenProgramNotProvided() {
    when(trackedEntityInstanceService.getTrackedEntityInstance(entity.getUid(), FALSE))
        .thenReturn(new TrackedEntityInstance());
    when(userService.getUserByUsername(user.getUsername())).thenReturn(user);

    trackedEntityInstanceSupportService.getTrackedEntityInstance(
        entity.getUid(), null, emptyList());
    verify(trackedEntityInstanceService, times(1)).getTrackedEntityInstance(entity.getUid(), FALSE);
  }
}
