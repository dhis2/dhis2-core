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
package org.hisp.dhis.tracker.imports.validation;

import static org.hisp.dhis.tracker.Assertions.assertHasOnlyErrors;
import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.hisp.dhis.tracker.imports.validation.Users.USER_11;
import static org.hisp.dhis.tracker.imports.validation.Users.USER_12;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E4020;

import java.io.IOException;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.tracker.TestSetup;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RelationshipSecurityImportValidationTest extends PostgresIntegrationTestBase {
  @Autowired private TestSetup testSetup;

  @Autowired private TrackerImportService trackerImportService;

  @Autowired private IdentifiableObjectManager manager;

  private User importUser;

  @BeforeAll
  void setUp() throws IOException {
    testSetup.importMetadata("tracker/tracker_basic_metadata.json");

    importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);

    TrackerImportParams params = TrackerImportParams.builder().build();
    assertNoErrors(
        trackerImportService.importTracker(
            params, testSetup.fromJson("tracker/validations/te_relationship.json")));
    manager.flush();
  }

  @BeforeEach
  void setUpUser() {
    injectSecurityContextUser(importUser);
  }

  @Test
  void shouldCreateWhenUserHasAccessToRelationshipTypeAndWriteAccessToBidirectionalRelationship()
      throws IOException {
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/relationship_siblings.json");
    TrackerImportParams params = new TrackerImportParams();

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);
    assertNoErrors(importReport);
  }

  @Test
  void
      shouldFailToCreateWhenUserHasNoAccessToRelationshipTypeAndWriteAccessToBidirectionalRelationship()
          throws IOException {
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/relationship_siblings.json");
    TrackerImportParams params = new TrackerImportParams();

    RelationshipType relationshipType = manager.get(RelationshipType.class, "xLmPUYJX8Ks");
    relationshipType.getSharing().getUsers().remove(USER_11);
    manager.update(relationshipType);
    injectSecurityContextUser(userService.getUser(USER_11));

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertHasOnlyErrors(importReport, E4020);
  }

  @Test
  void
      shouldFailToCreateWhenUserHasAccessToRelationshipTypeAndNoWriteAccessToBidirectionalRelationshipFrom()
          throws IOException {
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/relationship_siblings.json");
    TrackerImportParams params = new TrackerImportParams();

    Program program = manager.get(Program.class, "E8o1E9tAppy");
    program.getSharing().getUsers().get(USER_11).setAccess("r-r-----");
    manager.update(program);
    injectSecurityContextUser(userService.getUser(USER_11));

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertHasOnlyErrors(importReport, E4020);
  }

  @Test
  void
      shouldFailToCreateWhenUserHasAccessToRelationshipTypeAndNoWriteAccessToBidirectionalBidirectionalRelationshipTo()
          throws IOException {
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/relationship_siblings.json");
    TrackerImportParams params = new TrackerImportParams();

    Program program = manager.get(Program.class, "YYY1E9tAbbW");
    program.getSharing().getUsers().get(USER_11).setAccess("r-r-----");
    manager.update(program);
    injectSecurityContextUser(userService.getUser(USER_11));

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertHasOnlyErrors(importReport, E4020);
  }

  @Test
  void shouldDeleteWhenUserHasAccessToRelationshipTypeAndWriteAccessToBidirectionalRelationship()
      throws IOException {
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/relationship_siblings.json");
    TrackerImportParams params = new TrackerImportParams();

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);
    assertNoErrors(importReport);

    params.setImportStrategy(TrackerImportStrategy.DELETE);
    importReport = trackerImportService.importTracker(params, trackerObjects);
    assertNoErrors(importReport);
  }

  @Test
  void
      shouldFailToDeleteWhenUserHasNoAccessToRelationshipTypeAndWriteAccessToBidirectionalRelationship()
          throws IOException {
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/relationship_siblings.json");
    TrackerImportParams params = new TrackerImportParams();

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);
    assertNoErrors(importReport);

    RelationshipType relationshipType = manager.get(RelationshipType.class, "xLmPUYJX8Ks");
    relationshipType.getSharing().getUsers().remove(USER_11);
    manager.update(relationshipType);
    injectSecurityContextUser(userService.getUser(USER_11));

    params.setImportStrategy(TrackerImportStrategy.DELETE);
    importReport = trackerImportService.importTracker(params, trackerObjects);

    assertHasOnlyErrors(importReport, E4020);
  }

  @Test
  void
      shouldFailToDeleteWhenUserHasAccessToRelationshipTypeAndNoWriteAccessToBidirectionalRelationshipFrom()
          throws IOException {
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/relationship_siblings.json");
    TrackerImportParams params = new TrackerImportParams();

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);
    assertNoErrors(importReport);

    Program program = manager.get(Program.class, "E8o1E9tAppy");
    program.getSharing().getUsers().get(USER_11).setAccess("r-r-----");
    manager.update(program);
    injectSecurityContextUser(userService.getUser(USER_12));

    params.setImportStrategy(TrackerImportStrategy.DELETE);
    importReport = trackerImportService.importTracker(params, trackerObjects);

    assertHasOnlyErrors(importReport, E4020);
  }

  @Test
  void
      shouldFailToDeleteWhenUserHasAccessToRelationshipTypeAndNoWriteAccessToBidirectionalRelationshipTo()
          throws IOException {
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/relationship_siblings.json");
    TrackerImportParams params = new TrackerImportParams();

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);
    assertNoErrors(importReport);

    Program program = manager.get(Program.class, "YYY1E9tAbbW");
    program.getSharing().getUsers().get(USER_11).setAccess("r-r-----");
    manager.update(program);
    injectSecurityContextUser(userService.getUser(USER_11));

    params.setImportStrategy(TrackerImportStrategy.DELETE);
    importReport = trackerImportService.importTracker(params, trackerObjects);

    assertHasOnlyErrors(importReport, E4020);
  }

  @Test
  void shouldCreateWhenUserHasAccessToRelationshipTypeAndWriteAccessToRelationship()
      throws IOException {
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/relationship_parent_child.json");
    TrackerImportParams params = new TrackerImportParams();
    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);
    assertNoErrors(importReport);
  }

  @Test
  void shouldFailToCreateWhenUserHasNoAccessToRelationshipTypeAndWriteAccessToRelationship()
      throws IOException {
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/relationship_parent_child.json");
    TrackerImportParams params = new TrackerImportParams();
    RelationshipType relationshipType = manager.get(RelationshipType.class, "TV9oB9LT3sh");
    relationshipType.getSharing().getUsers().remove(USER_12);
    manager.update(relationshipType);
    injectSecurityContextUser(userService.getUser(USER_12));

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertHasOnlyErrors(importReport, E4020);
  }

  @Test
  void shouldFailToCreateWhenUserHasAccessToRelationshipTypeAndNoWriteAccessToRelationshipFrom()
      throws IOException {
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/relationship_parent_child.json");
    TrackerImportParams params = new TrackerImportParams();
    Program program = manager.get(Program.class, "E8o1E9tAppy");
    program.getSharing().getUsers().get(USER_12).setAccess("r-r-----");
    manager.update(program);
    injectSecurityContextUser(userService.getUser(USER_12));

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertHasOnlyErrors(importReport, E4020);
  }

  @Test
  void shouldFailToCreateWhenUserHasAccessToRelationshipTypeAndNoWriteAccessToRelationshipTo()
      throws IOException {
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/relationship_parent_child.json");
    TrackerImportParams params = new TrackerImportParams();
    Program program = manager.get(Program.class, "YYY1E9tAbbW");
    program.getSharing().getUsers().remove(USER_12);
    manager.update(program);
    injectSecurityContextUser(userService.getUser(USER_12));

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertHasOnlyErrors(importReport, E4020);
  }

  @Test
  void shouldDeleteWhenUserHasAccessToRelationshipTypeAndWriteAccessToRelationship()
      throws IOException {
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/relationship_parent_child.json");
    TrackerImportParams params = new TrackerImportParams();
    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);
    assertNoErrors(importReport);

    Program program = manager.get(Program.class, "YYY1E9tAbbW");
    program.getSharing().getUsers().remove(USER_12);
    manager.update(program);

    params.setImportStrategy(TrackerImportStrategy.DELETE);
    importReport = trackerImportService.importTracker(params, trackerObjects);
    assertNoErrors(importReport);
  }

  @Test
  void shouldFailToDeleteWhenUserHasNoAccessToRelationshipTypeAndWriteAccessToRelationship()
      throws IOException {
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/relationship_parent_child.json");
    TrackerImportParams params = new TrackerImportParams();
    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);
    assertNoErrors(importReport);

    RelationshipType relationshipType = manager.get(RelationshipType.class, "TV9oB9LT3sh");
    relationshipType.getSharing().getUsers().remove(USER_12);
    manager.update(relationshipType);
    injectSecurityContextUser(userService.getUser(USER_12));

    params.setImportStrategy(TrackerImportStrategy.DELETE);
    importReport = trackerImportService.importTracker(params, trackerObjects);

    assertHasOnlyErrors(importReport, E4020);
  }

  @Test
  void shouldFailToDeleteWhenUserHasAccessToRelationshipTypeAndNoWriteAccessToRelationshipFrom()
      throws IOException {
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/relationship_parent_child.json");
    TrackerImportParams params = new TrackerImportParams();
    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);
    assertNoErrors(importReport);

    Program program = manager.get(Program.class, "E8o1E9tAppy");
    program.getSharing().getUsers().get(USER_12).setAccess("r-r-----");
    manager.update(program);
    injectSecurityContextUser(userService.getUser(USER_12));

    params.setImportStrategy(TrackerImportStrategy.DELETE);
    importReport = trackerImportService.importTracker(params, trackerObjects);

    assertHasOnlyErrors(importReport, E4020);
  }
}
