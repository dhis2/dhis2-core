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
package org.hisp.dhis.webapi.controller.tracker.export.trackedentity;

import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasNoMember;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Set;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerIntegrationTest;
import org.hisp.dhis.webapi.controller.tracker.JsonTrackedEntityChangeLog;
import org.hisp.dhis.webapi.controller.tracker.JsonTrackedEntityChangeLog.JsonTrackedEntityAttribute;
import org.hisp.dhis.webapi.controller.tracker.JsonUser;
import org.hisp.dhis.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TrackedEntitiesExportControllerPostgresTest extends DhisControllerIntegrationTest {
  @Autowired private IdentifiableObjectManager manager;

  private OrganisationUnit orgUnit;

  private Program program;

  private ProgramStage programStage;

  private TrackedEntityType trackedEntityType;

  private TrackedEntity trackedEntity;

  private TrackedEntityAttribute firstNameTEA;

  private TrackedEntityAttribute surnameTEA;

  private User owner;

  private User user;

  @BeforeEach
  void setUp() {
    owner = makeUser("owner");
    orgUnit = createOrganisationUnit('A');
    orgUnit.getSharing().setOwner(owner);
    manager.save(orgUnit, false);

    owner.addOrganisationUnit(orgUnit);
    owner.setTeiSearchOrganisationUnits(Set.of(orgUnit));

    user = createUserWithId("tester", CodeGenerator.generateUid());
    user.addOrganisationUnit(orgUnit);
    user.setTeiSearchOrganisationUnits(Set.of(orgUnit));
    this.userService.updateUser(user);

    program = createProgram('A');
    program.addOrganisationUnit(orgUnit);
    program.getSharing().setOwner(owner);
    program.getSharing().addUserAccess(userAccess());
    manager.save(program, false);

    programStage = createProgramStage('A', program);
    programStage.getSharing().setOwner(owner);
    programStage.getSharing().addUserAccess(userAccess());
    manager.save(programStage, false);

    trackedEntityType = createTrackedEntityType('A');
    manager.save(trackedEntityType, false);

    program.setTrackedEntityType(trackedEntityType);
    manager.save(program, false);

    firstNameTEA = createTrackedEntityAttribute('A');
    manager.save(firstNameTEA, false);
    TrackedEntityAttributeValue firstNameAttributeValue =
        createTrackedEntityAttributeValue('A', trackedEntity, firstNameTEA);

    surnameTEA = createTrackedEntityAttribute('B');
    manager.save(surnameTEA, false);
    TrackedEntityAttributeValue surnameAttributeValue =
        createTrackedEntityAttributeValue('B', trackedEntity, surnameTEA);

    trackedEntity = createTrackedEntity(orgUnit);
    trackedEntity.setTrackedEntityType(trackedEntityType);
    trackedEntity.setTrackedEntityAttributeValues(
        Set.of(firstNameAttributeValue, surnameAttributeValue));
    manager.save(trackedEntity);

    injectSecurityContextUser(user);

    JsonWebMessage importResponse =
        POST(
                "/tracker?async=false&importStrategy=UPDATE",
                createJson(trackedEntity, firstNameTEA, surnameTEA))
            .content(HttpStatus.OK)
            .as(JsonWebMessage.class);
    assertEquals(HttpStatus.OK.toString(), importResponse.getStatus());
  }

  @Test
  void shouldGetTrackedEntityAttributeChangeLogWhenValueUpdatedAndThenDeleted() {
    // TODO Do an e2e test instead?
    JsonList<JsonTrackedEntityChangeLog> changeLogs =
        GET(
                "/tracker/trackedEntities/{id}/changeLogs?program={programUid}",
                trackedEntity.getUid(),
                program.getUid())
            .content(HttpStatus.OK)
            .getList("changeLogs", JsonTrackedEntityChangeLog.class);

    JsonTrackedEntityChangeLog changeLog = changeLogs.get(0);
    JsonUser createdBy = changeLog.getCreatedBy();
    JsonTrackedEntityAttribute attributeChange =
        changeLog.getChange().getTrackedEntityAttributeChange();
    UserDetails currentUser = CurrentUserUtil.getCurrentUserDetails();

    assertAll(
        () -> assertEquals(currentUser.getUid(), createdBy.getUid()),
        () -> assertEquals(currentUser.getUsername(), createdBy.getUsername()),
        () -> assertEquals(currentUser.getFirstName(), createdBy.getFirstName()),
        () -> assertEquals(currentUser.getSurname(), createdBy.getSurname()),
        () -> assertEquals("DELETE", changeLog.getType()),
        () -> assertEquals(firstNameTEA.getUid(), attributeChange.getTrackedEntityAttribute()),
        () -> assertEquals("value 3", attributeChange.getPreviousValue()),
        () -> assertHasNoMember(attributeChange, "currentValue"));
  }

  @Test
  void
      shouldGetChangeLogPagerWithNextAttributeWhenMultipleAttributesImportedAndFirstPageRequested() {
    fail();
  }

  @Test
  void
      shouldGetChangeLogPagerWithNextAndPreviousAttributesWhenMultipleAttributesImportedAndSecondPageRequested() {
    fail();
  }

  @Test
  void
      shouldGetChangeLogPagerWithPreviousAttributeWhenMultipleAttributesImportedAndLastPageRequested() {
    fail();
  }

  @Test
  void
      shouldGetChangeLogPagerWithoutPreviousNorNextAttributeWhenMultipleAttributesImportedAndAllAttributesFitInOnePage() {
    fail();
  }

  private UserAccess userAccess() {
    UserAccess a = new UserAccess();
    a.setUser(user);
    a.setAccess(AccessStringHelper.FULL);
    return a;
  }

  private String createJson(
      TrackedEntity trackedEntity,
      TrackedEntityAttribute firstNameTEA,
      TrackedEntityAttribute surnameTEA) {
    return """
        {
          "trackedEntities": [
          {
            "trackedEntity": "%s",
            "trackedEntityType": "%s",
            "createdAt": "2017-01-26T13:48:13.343",
            "createdAtClient": "2017-01-26T13:48:13.343",
            "updatedAt": "2017-01-26T13:48:13.343",
            "orgUnit": "%s",
            "inactive": false,
            "deleted": false,
            "potentialDuplicate": false,
            "attributes": [
            {
              "attribute": "%s",
              "code": "MMD_PER_NAM",
              "displayName": "First name",
              "createdAt": "2017-01-26T13:48:13.343",
              "updatedAt": "2017-01-26T13:48:13.343",
              "valueType": "TEXT",
              "value": "Marie"
            },
            {
              "attribute": "%s",
              "displayName": "Last name",
              "createdAt": "2017-01-26T13:48:13.343",
              "updatedAt": "2017-01-26T13:48:13.343",
              "valueType": "TEXT",
              "value": "James"
            }]
          }]
        }
      """
        .formatted(
            trackedEntity.getUid(),
            trackedEntity.getTrackedEntityType().getUid(),
            orgUnit.getUid(),
            firstNameTEA.getUid(),
            surnameTEA.getUid());
  }
}
