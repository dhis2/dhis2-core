/*
 * Copyright (c) 2004-2025, University of Oslo
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

import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;
import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.test.utils.Assertions.assertStartsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.List;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.TestSetup;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TrackerProgramServiceTest extends PostgresIntegrationTestBase {
  @Autowired private TestSetup testSetup;
  @Autowired private TrackerProgramService trackerProgramService;
  @Autowired private IdentifiableObjectManager manager;

  private User regularUser;
  private TrackedEntityType trackedEntityType;

  @BeforeAll
  void setUp() throws IOException {
    testSetup.importMetadata();
    regularUser = userService.getUser("Z7870757a75");
    injectSecurityContextUser(regularUser);
    trackedEntityType = manager.get(TrackedEntityType.class, "Fa7NY4PW6DL");
    assertNotNull(trackedEntityType);
  }

  @Test
  void shouldReturnAllAccessiblePrograms() {
    List<String> accessiblePrograms =
        List.of(
            "BFcipDERJnf",
            "shPjYNifvMK",
            "pcxIanBWlSY",
            "UWRnoyBjvqi",
            "YlUmbgnKWkd",
            "SeeUNWLQmZk",
            "sLngICFQjvH",
            "TsngICFQjvP");

    assertContainsOnly(
        accessiblePrograms, getUids(trackerProgramService.getAccessibleTrackerPrograms()));
  }

  @Test
  void shouldReturnAllAccessibleProgramsByTrackedEntityType() {
    List<String> accessiblePrograms = List.of("TsngICFQjvP");

    assertEquals(
        accessiblePrograms,
        getUids(trackerProgramService.getAccessibleTrackerPrograms(trackedEntityType)));
  }

  @Test
  void shouldReturnEmptyListWhenNoProgramAccessible() {
    makeProgramInaccessible("TsngICFQjvP");

    assertIsEmpty(trackerProgramService.getAccessibleTrackerPrograms(trackedEntityType));
  }

  @Test
  void shouldReturnSingleProgramWhenTrackerProgramAccessible()
      throws ForbiddenException, BadRequestException {
    assertEquals(
        "TsngICFQjvP", trackerProgramService.getTrackerProgram(UID.of("TsngICFQjvP")).getUid());
  }

  @Test
  void shouldFailWhenRequestingSingleProgramThatDoesNotExist() {
    UID madeUpUid = UID.generate();

    Exception exception =
        assertThrows(
            BadRequestException.class, () -> trackerProgramService.getTrackerProgram(madeUpUid));
    assertEquals("Provided program, " + madeUpUid + ", does not exist.", exception.getMessage());
  }

  @Test
  void shouldFailWhenRequestingSingleProgramThatIsNotATrackerProgram() {
    UID eventProgramUid = UID.of("BFcipDERJne");

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () -> trackerProgramService.getTrackerProgram(eventProgramUid));
    assertEquals(
        "Provided program, " + eventProgramUid.getValue() + ", is not a tracker program.",
        exception.getMessage());
  }

  @Test
  void shouldFailWhenRequestingSingleProgramThatIsNotAccessible() {
    makeProgramInaccessible("TsngICFQjvP");

    Exception exception =
        assertThrows(
            ForbiddenException.class,
            () -> trackerProgramService.getTrackerProgram(UID.of("TsngICFQjvP")));
    assertStartsWith(
        "Current user doesn't have access to the provided program", exception.getMessage());
  }

  private void makeProgramInaccessible(String uid) {
    User admin = getAdminUser();
    injectSecurityContextUser(admin);

    Program program = manager.get(Program.class, uid);
    assertNotNull(program);
    program.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
    manager.save(program);

    injectSecurityContextUser(regularUser);
  }
}
