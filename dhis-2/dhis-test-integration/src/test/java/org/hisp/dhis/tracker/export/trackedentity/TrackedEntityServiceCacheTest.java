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
package org.hisp.dhis.tracker.export.trackedentity;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.test.utils.Assertions;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.tracker.Page;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.TestSetup;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

// Do not use the annotation @ActiveProfiles, this is an exception as for now it's the only way to
// use a cache in our integration tests
@ActiveProfiles("cache-test")
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TrackedEntityServiceCacheTest extends PostgresIntegrationTestBase {
  @Autowired private TestSetup testSetup;
  @Autowired private IdentifiableObjectManager manager;
  @Autowired private TrackedEntityService trackedEntityService;
  @Autowired private ProgramService programService;

  private User regularUser;
  private Program programA;
  private String trackedEntityA;
  private String trackedEntityB;
  private String trackedEntityC;
  private PageParams defaultPageParams;

  @BeforeAll
  void setUp() throws IOException, BadRequestException, ForbiddenException, NotFoundException {
    testSetup.importMetadata();
    User importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);
    testSetup.importTrackerData();
    regularUser = userService.getUser("FIgVWzUCkpw");
    programA = manager.get(Program.class, "BFcipDERJnf");

    trackedEntityA = "dUE514NMOlo";
    trackedEntityB = "QS6w44flWAf";
    trackedEntityC = "H8732208127";
    String trackedEntityD = "mHWCacsGYYn";

    defaultPageParams = PageParams.of(1, 10, false);

    injectAdminIntoSecurityContext();
    makeProgramTheOnlyOneAccessible(programA);
    TrackedEntityOperationParams operationParams =
        createTrackedEntitiesOperationParams(trackedEntityD);
    injectSecurityContextUser(regularUser);

    List<String> trackedEntities =
        extractUIDs(trackedEntityService.findTrackedEntities(operationParams, defaultPageParams));
    Assertions.assertContainsOnly(List.of(trackedEntityD), trackedEntities);
  }

  @Test
  void shouldFindTrackedEntityWhenOwnerAlreadyInCache()
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityOperationParams operationParams = createProgramOperationParams();
    injectSecurityContextUser(regularUser);

    List<String> trackedEntities =
        extractUIDs(trackedEntityService.findTrackedEntities(operationParams, defaultPageParams));
    Assertions.assertContainsOnly(List.of(trackedEntityA, trackedEntityB), trackedEntities);

    trackedEntities =
        extractUIDs(trackedEntityService.findTrackedEntities(operationParams, defaultPageParams));
    Assertions.assertContainsOnly(List.of(trackedEntityA, trackedEntityB), trackedEntities);
  }

  @Test
  void shouldFindTrackedEntityWhenAnotherOwnerAlreadyInCache()
      throws ForbiddenException, BadRequestException, NotFoundException {
    User user = userService.getUser("o1HMTIzBGo7");
    injectSecurityContextUser(user);

    List<String> trackedEntities =
        extractUIDs(
            trackedEntityService.findTrackedEntities(
                createTrackedEntitiesOperationParams(trackedEntityC), defaultPageParams));

    Assertions.assertContainsOnly(List.of(trackedEntityC), trackedEntities);
  }

  private TrackedEntityOperationParams createProgramOperationParams() {
    return TrackedEntityOperationParams.builder()
        .program(programA)
        .trackedEntities(UID.of(trackedEntityA, trackedEntityB))
        .build();
  }

  private TrackedEntityOperationParams createTrackedEntitiesOperationParams(String trackedEntity) {
    return TrackedEntityOperationParams.builder()
        .trackedEntities(Set.of(UID.of(trackedEntity)))
        .build();
  }

  private void makeProgramTheOnlyOneAccessible(Program program) {
    programService.getAllPrograms().stream()
        .filter(p -> !p.getUid().equalsIgnoreCase(program.getUid()))
        .forEach(
            p -> {
              p.setSharing(Sharing.builder().publicAccess(AccessStringHelper.DEFAULT).build());
              manager.update(p);
            });
  }

  private List<String> extractUIDs(Page<TrackedEntity> trackedEntities) {
    return trackedEntities.getItems().stream().map(TrackedEntity::getUid).toList();
  }
}
