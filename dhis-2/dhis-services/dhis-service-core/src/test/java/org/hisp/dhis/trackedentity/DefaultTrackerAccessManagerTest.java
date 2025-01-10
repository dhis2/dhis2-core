/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.trackedentity;

import static org.hisp.dhis.DhisConvenienceTest.createOrganisationUnit;
import static org.hisp.dhis.DhisConvenienceTest.createProgram;
import static org.hisp.dhis.common.AccessLevel.CLOSED;
import static org.hisp.dhis.common.AccessLevel.OPEN;
import static org.hisp.dhis.common.AccessLevel.PROTECTED;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultTrackerAccessManagerTest {

  @Mock private OrganisationUnitService organisationUnitService;

  @InjectMocks private DefaultTrackerAccessManager trackerAccessManager;

  private Program program;

  private OrganisationUnit orgUnit;

  private User user;

  @BeforeEach
  void before() {
    program = createProgram('A');
    orgUnit = createOrganisationUnit('A');
    user = new User();
  }

  @Test
  void shouldHaveAccessWhenProgramOpenAndSearchAccessAvailable() {
    program.setAccessLevel(OPEN);

    when(organisationUnitService.isInUserSearchHierarchy(user, orgUnit)).thenReturn(true);

    assertTrue(
        trackerAccessManager.canAccess(user, program, orgUnit),
        "User should have access to open program");
  }

  @Test
  void shouldNotHaveAccessWhenProgramOpenAndSearchAccessNotAvailable() {
    program.setAccessLevel(OPEN);

    when(organisationUnitService.isInUserSearchHierarchy(user, orgUnit)).thenReturn(false);

    assertFalse(
        trackerAccessManager.canAccess(user, program, orgUnit),
        "User should not have access to open program");
  }

  @Test
  void shouldHaveAccessWhenProgramNullAndSearchAccessAvailable() {
    when(organisationUnitService.isInUserSearchHierarchy(user, orgUnit)).thenReturn(true);

    assertTrue(
        trackerAccessManager.canAccess(user, null, orgUnit),
        "User should have access to unspecified program");
  }

  @Test
  void shouldNotHaveAccessWhenProgramNullAndSearchAccessNotAvailable() {
    when(organisationUnitService.isInUserSearchHierarchy(user, orgUnit)).thenReturn(false);

    assertFalse(
        trackerAccessManager.canAccess(user, null, orgUnit),
        "User should not have access to unspecified program");
  }

  @Test
  void shouldHaveAccessWhenProgramClosedAndCaptureAccessAvailable() {
    program.setAccessLevel(CLOSED);

    when(organisationUnitService.isInUserHierarchy(user, orgUnit)).thenReturn(true);

    assertTrue(
        trackerAccessManager.canAccess(user, program, orgUnit),
        "User should have access to closed program");
  }

  @Test
  void shouldNotHaveAccessWhenProgramClosedAndCaptureAccessNotAvailable() {
    program.setAccessLevel(CLOSED);

    when(organisationUnitService.isInUserHierarchy(user, orgUnit)).thenReturn(false);

    assertFalse(
        trackerAccessManager.canAccess(user, program, orgUnit),
        "User should not have access to closed program");
  }

  @Test
  void shouldHaveAccessWhenProgramProtectedAndCaptureAccessAvailable() {
    program.setAccessLevel(PROTECTED);

    when(organisationUnitService.isInUserHierarchy(user, orgUnit)).thenReturn(true);

    assertTrue(
        trackerAccessManager.canAccess(user, program, orgUnit),
        "User should have access to protected program");
  }

  @Test
  void shouldNotHaveAccessWhenProgramProtectedAndCaptureAccessNotAvailable() {
    program.setAccessLevel(PROTECTED);

    assertFalse(
        trackerAccessManager.canAccess(user, program, orgUnit),
        "User should not have access to protected program");
  }
}
