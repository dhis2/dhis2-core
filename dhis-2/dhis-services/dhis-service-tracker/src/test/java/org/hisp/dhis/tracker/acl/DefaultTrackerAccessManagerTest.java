/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.tracker.acl;

import static org.hisp.dhis.common.AccessLevel.CLOSED;
import static org.hisp.dhis.common.AccessLevel.OPEN;
import static org.hisp.dhis.common.AccessLevel.PROTECTED;
import static org.hisp.dhis.test.TestBase.createOrganisationUnit;
import static org.hisp.dhis.test.TestBase.createProgram;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultTrackerAccessManagerTest {

  @InjectMocks private DefaultTrackerAccessManager trackerAccessManager;

  private Program program;

  private OrganisationUnit orgUnit;

  private User user;

  @BeforeEach
  public void before() {
    program = createProgram('A');
    orgUnit = createOrganisationUnit('A');
    user = new User();
  }

  @Test
  void shouldHaveAccessWhenProgramOpenAndSearchAccessAvailable() {
    program.setAccessLevel(OPEN);
    user.setTeiSearchOrganisationUnits(Set.of(orgUnit));

    assertTrue(
        trackerAccessManager.canAccess(UserDetails.fromUser(user), program, orgUnit),
        "User should have access to open program");
  }

  @Test
  void shouldNotHaveAccessWhenProgramOpenAndSearchAccessNotAvailable() {
    program.setAccessLevel(OPEN);

    assertFalse(
        trackerAccessManager.canAccess(UserDetails.fromUser(user), program, orgUnit),
        "User should not have access to open program");
  }

  @Test
  void shouldHaveAccessWhenProgramNullAndSearchAccessAvailable() {
    user.setTeiSearchOrganisationUnits(Set.of(orgUnit));

    assertTrue(
        trackerAccessManager.canAccess(UserDetails.fromUser(user), null, orgUnit),
        "User should have access to unspecified program");
  }

  @Test
  void shouldNotHaveAccessWhenProgramNullAndSearchAccessNotAvailable() {
    assertFalse(
        trackerAccessManager.canAccess(UserDetails.fromUser(user), null, orgUnit),
        "User should not have access to unspecified program");
  }

  @Test
  void shouldHaveAccessWhenProgramClosedAndCaptureAccessAvailable() {
    program.setAccessLevel(CLOSED);
    user.setOrganisationUnits(Set.of(orgUnit));

    assertTrue(
        trackerAccessManager.canAccess(UserDetails.fromUser(user), program, orgUnit),
        "User should have access to closed program");
  }

  @Test
  void shouldNotHaveAccessWhenProgramClosedAndCaptureAccessNotAvailable() {
    program.setAccessLevel(CLOSED);

    assertFalse(
        trackerAccessManager.canAccess(UserDetails.fromUser(user), program, orgUnit),
        "User should not have access to closed program");
  }

  @Test
  void shouldHaveAccessWhenProgramProtectedAndCaptureAccessAvailable() {
    program.setAccessLevel(PROTECTED);
    user.setOrganisationUnits(Set.of(orgUnit));

    assertTrue(
        trackerAccessManager.canAccess(UserDetails.fromUser(user), program, orgUnit),
        "User should have access to protected program");
  }

  @Test
  void shouldNotHaveAccessWhenProgramProtectedAndCaptureAccessNotAvailable() {
    program.setAccessLevel(PROTECTED);

    assertFalse(
        trackerAccessManager.canAccess(UserDetails.fromUser(user), program, orgUnit),
        "User should not have access to protected program");
  }
}
