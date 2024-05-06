/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.tracker.validation;

import static org.hisp.dhis.tracker.Assertions.assertHasOnlyErrors;
import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.hisp.dhis.tracker.validation.Users.USER_11;
import static org.hisp.dhis.tracker.validation.Users.USER_12;
import static org.hisp.dhis.tracker.validation.ValidationCode.E4020;

import java.io.IOException;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerImportService;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.report.ImportReport;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RelationshipSecurityImportValidationTest extends TrackerTest {

  @Autowired protected ProgramInstanceService enrollmentService;

  @Autowired private TrackerImportService trackerImportService;

  @Autowired protected UserService _userService;

  @Override
  protected void initTest() throws IOException {
    userService = _userService;
    setUpMetadata("tracker/tracker_basic_metadata.json");
    injectAdminUser();
    assertNoErrors(
        trackerImportService.importTracker(fromJson("tracker/validations/te_relationship.json")));
    manager.flush();
  }

  @Test
  void shouldCreateWhenUserHasAccessToRelationshipTypeAndWriteAccessToBidirectionalRelationship()
      throws IOException {
    TrackerImportParams params = fromJson("tracker/validations/relationship_siblings.json");
    params.setUserId(USER_11);

    ImportReport importReport = trackerImportService.importTracker(params);
    assertNoErrors(importReport);
  }

  @Test
  void
      shouldFailToCreateWhenUserHasNoAccessToRelationshipTypeAndWriteAccessToBidirectionalRelationship()
          throws IOException {
    TrackerImportParams params = fromJson("tracker/validations/relationship_siblings.json");
    params.setUserId(USER_11);

    RelationshipType relationshipType = manager.get(RelationshipType.class, "xLmPUYJX8Ks");
    relationshipType.getSharing().getUsers().remove(USER_11);
    manager.update(relationshipType);

    ImportReport importReport = trackerImportService.importTracker(params);

    assertHasOnlyErrors(importReport, E4020);
  }

  @Test
  void
      shouldFailToCreateWhenUserHasAccessToRelationshipTypeAndNoWriteAccessToBidirectionalRelationshipFrom()
          throws IOException {
    TrackerImportParams params = fromJson("tracker/validations/relationship_siblings.json");
    params.setUserId(USER_11);

    Program program = manager.get(Program.class, "E8o1E9tAppy");
    program.getSharing().getUsers().get(USER_11).setAccess("r-r-----");
    manager.update(program);

    ImportReport importReport = trackerImportService.importTracker(params);

    assertHasOnlyErrors(importReport, E4020);
  }

  @Test
  void
      shouldFailToCreateWhenUserHasAccessToRelationshipTypeAndNoWriteAccessToBidirectionalBidirectionalRelationshipTo()
          throws IOException {
    TrackerImportParams params = fromJson("tracker/validations/relationship_siblings.json");
    params.setUserId(USER_11);

    Program program = manager.get(Program.class, "YYY1E9tAbbW");
    program.getSharing().getUsers().get(USER_11).setAccess("r-r-----");
    manager.update(program);

    ImportReport importReport = trackerImportService.importTracker(params);

    assertHasOnlyErrors(importReport, E4020);
  }

  @Test
  void shouldDeleteWhenUserHasAccessToRelationshipTypeAndWriteAccessToBidirectionalRelationship()
      throws IOException {
    TrackerImportParams params = fromJson("tracker/validations/relationship_siblings.json");
    params.setUserId(USER_11);

    ImportReport importReport = trackerImportService.importTracker(params);
    assertNoErrors(importReport);

    params.setImportStrategy(TrackerImportStrategy.DELETE);
    importReport = trackerImportService.importTracker(params);
    assertNoErrors(importReport);
  }

  @Test
  void
      shouldFailToDeleteWhenUserHasNoAccessToRelationshipTypeAndWriteAccessToBidirectionalRelationship()
          throws IOException {
    TrackerImportParams params = fromJson("tracker/validations/relationship_siblings.json");

    params.setUserId(USER_11);

    ImportReport importReport = trackerImportService.importTracker(params);
    assertNoErrors(importReport);

    RelationshipType relationshipType = manager.get(RelationshipType.class, "xLmPUYJX8Ks");
    relationshipType.getSharing().getUsers().remove(USER_11);
    manager.update(relationshipType);

    params.setImportStrategy(TrackerImportStrategy.DELETE);
    importReport = trackerImportService.importTracker(params);

    assertHasOnlyErrors(importReport, E4020);
  }

  @Test
  void
      shouldFailToDeleteWhenUserHasAccessToRelationshipTypeAndNoWriteAccessToBidirectionalRelationshipFrom()
          throws IOException {
    TrackerImportParams params = fromJson("tracker/validations/relationship_siblings.json");

    params.setUserId(USER_11);

    ImportReport importReport = trackerImportService.importTracker(params);
    assertNoErrors(importReport);

    Program program = manager.get(Program.class, "E8o1E9tAppy");
    program.getSharing().getUsers().get(USER_11).setAccess("r-r-----");
    manager.update(program);

    params.setImportStrategy(TrackerImportStrategy.DELETE);
    importReport = trackerImportService.importTracker(params);

    assertHasOnlyErrors(importReport, E4020);
  }

  @Test
  void
      shouldFailToDeleteWhenUserHasAccessToRelationshipTypeAndNoWriteAccessToBidirectionalRelationshipTo()
          throws IOException {
    TrackerImportParams params = fromJson("tracker/validations/relationship_siblings.json");

    params.setUserId(USER_11);

    ImportReport importReport = trackerImportService.importTracker(params);
    assertNoErrors(importReport);

    Program program = manager.get(Program.class, "YYY1E9tAbbW");
    program.getSharing().getUsers().get(USER_11).setAccess("r-r-----");
    manager.update(program);

    params.setImportStrategy(TrackerImportStrategy.DELETE);
    importReport = trackerImportService.importTracker(params);

    assertHasOnlyErrors(importReport, E4020);
  }

  @Test
  void shouldCreateWhenUserHasAccessToRelationshipTypeAndWriteAccessToRelationship()
      throws IOException {
    TrackerImportParams params = fromJson("tracker/validations/relationship_parent_child.json");

    params.setUserId(USER_12);

    ImportReport importReport = trackerImportService.importTracker(params);
    assertNoErrors(importReport);
  }

  @Test
  void shouldFailToCreateWhenUserHasNoAccessToRelationshipTypeAndWriteAccessToRelationship()
      throws IOException {
    TrackerImportParams params = fromJson("tracker/validations/relationship_parent_child.json");

    params.setUserId(USER_12);

    RelationshipType relationshipType = manager.get(RelationshipType.class, "TV9oB9LT3sh");
    relationshipType.getSharing().getUsers().remove(USER_12);
    manager.update(relationshipType);

    ImportReport importReport = trackerImportService.importTracker(params);

    assertHasOnlyErrors(importReport, E4020);
  }

  @Test
  void shouldFailToCreateWhenUserHasAccessToRelationshipTypeAndNoWriteAccessToRelationshipFrom()
      throws IOException {
    TrackerImportParams params = fromJson("tracker/validations/relationship_parent_child.json");

    params.setUserId(USER_12);

    Program program = manager.get(Program.class, "E8o1E9tAppy");
    program.getSharing().getUsers().get(USER_12).setAccess("r-r-----");
    manager.update(program);

    ImportReport importReport = trackerImportService.importTracker(params);

    assertHasOnlyErrors(importReport, E4020);
  }

  @Test
  void shouldFailToCreateWhenUserHasAccessToRelationshipTypeAndNoWriteAccessToRelationshipTo()
      throws IOException {
    TrackerImportParams params = fromJson("tracker/validations/relationship_parent_child.json");

    params.setUserId(USER_12);

    Program program = manager.get(Program.class, "YYY1E9tAbbW");
    program.getSharing().getUsers().remove(USER_12);
    manager.update(program);

    ImportReport importReport = trackerImportService.importTracker(params);

    assertHasOnlyErrors(importReport, E4020);
  }

  @Test
  void shouldDeleteWhenUserHasAccessToRelationshipTypeAndWriteAccessToRelationship()
      throws IOException {
    TrackerImportParams params = fromJson("tracker/validations/relationship_parent_child.json");

    params.setUserId(USER_12);

    ImportReport importReport = trackerImportService.importTracker(params);
    assertNoErrors(importReport);

    Program program = manager.get(Program.class, "YYY1E9tAbbW");
    program.getSharing().getUsers().remove(USER_12);
    manager.update(program);

    params.setImportStrategy(TrackerImportStrategy.DELETE);
    importReport = trackerImportService.importTracker(params);
    assertNoErrors(importReport);
  }

  @Test
  void shouldFailToDeleteWhenUserHasNoAccessToRelationshipTypeAndWriteAccessToRelationship()
      throws IOException {
    TrackerImportParams params = fromJson("tracker/validations/relationship_parent_child.json");

    params.setUserId(USER_12);

    ImportReport importReport = trackerImportService.importTracker(params);
    assertNoErrors(importReport);

    RelationshipType relationshipType = manager.get(RelationshipType.class, "TV9oB9LT3sh");
    relationshipType.getSharing().getUsers().remove(USER_12);
    manager.update(relationshipType);

    params.setImportStrategy(TrackerImportStrategy.DELETE);
    importReport = trackerImportService.importTracker(params);

    assertHasOnlyErrors(importReport, E4020);
  }

  @Test
  void shouldFailToDeleteWhenUserHasAccessToRelationshipTypeAndNoWriteAccessToRelationshipFrom()
      throws IOException {
    TrackerImportParams params = fromJson("tracker/validations/relationship_parent_child.json");

    params.setUserId(USER_12);

    ImportReport importReport = trackerImportService.importTracker(params);
    assertNoErrors(importReport);

    Program program = manager.get(Program.class, "E8o1E9tAppy");
    program.getSharing().getUsers().get(USER_12).setAccess("r-r-----");
    manager.update(program);

    params.setImportStrategy(TrackerImportStrategy.DELETE);
    importReport = trackerImportService.importTracker(params);

    assertHasOnlyErrors(importReport, E4020);
  }
}
