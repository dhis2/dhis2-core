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
package org.hisp.dhis.webapi.controller.tracker.export.event;

import static org.hisp.dhis.security.Authorities.ALL;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.Sets;
import java.util.Date;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerIntegrationTest;
import org.hisp.dhis.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class EventsExportControllerPostgresTest extends DhisControllerIntegrationTest {
  private static final String DATA_ELEMENT_VALUE = "value";
  private static final String NEW_DATA_ELEMENT_VALUE = "new value";

  @Autowired private IdentifiableObjectManager manager;
  private User user;
  private Program program;
  private ProgramStage programStage;
  private OrganisationUnit orgUnit;
  private User owner;
  private TrackedEntityType trackedEntityType;
  private EventDataValue dataValue;

  @BeforeEach
  void setUp() {
    owner = makeUser("owner");
    orgUnit = createOrganisationUnit("a");
    orgUnit.setUid("ZiMBqH865GV");
    manager.save(orgUnit);

    user = createAndAddUser("username", orgUnit, ALL.name());

    trackedEntityType = trackedEntityTypeAccessible();

    program = createProgram('A');
    program.getOrganisationUnits().add(orgUnit);
    program.setUid("q04UBOqq3rp");
    program.setTrackedEntityType(trackedEntityType);
    manager.save(program);

    DataElement de = createDataElement('A', ValueType.TEXT, AggregationType.NONE);
    de.getSharing().setOwner(owner);
    manager.save(de, false);

    programStage = createProgramStage('A', program);
    programStage.setUid("pSllsjpfLH2");
    program.getProgramStages().add(programStage);
    ProgramStageDataElement programStageDataElement =
        createProgramStageDataElement(programStage, de, 1, false);
    manager.save(programStageDataElement);
    programStage.setProgramStageDataElements(Sets.newHashSet(programStageDataElement));
    manager.save(programStage);

    dataValue = new EventDataValue();
    dataValue.setDataElement(de.getUid());
    dataValue.setStoredBy("user");
    dataValue.setValue(DATA_ELEMENT_VALUE);
  }

  @Test
  void shouldGetEventChangeLogWhenDataValueUpdated() {
    Event event = event(enrollment(trackedEntity()));
    event.getEventDataValues().add(dataValue);
    manager.update(event);

    String json =
        """
      {
        "events": [
          {
            "event": "%s",
            "status": "COMPLETED",
            "program": "%s",
            "programStage": "%s",
            "enrollment": "%s",
            "trackedEntity": "%s",
            "orgUnit": "%s",
            "occurredAt": "2023-01-10",
            "scheduledAt": "2023-01-10",
            "storedBy": "tracker",
            "followUp": false,
            "deleted": false,
            "createdAt": "2018-01-20T10:44:03.222",
            "createdAtClient": "2017-01-20T10:44:03.222",
            "updatedAt": "2018-01-20T10:44:33.777",
            "completedBy": "tracker",
            "completedAt": "2023-01-20",
            "notes": [],
            "followup": false,
            "geometry": null,
            "dataValues": [
              {
                "dataElement": "%s",
                "value": "%s"
              }
            ]
          }
        ]
      }}
        """
            .formatted(
                event.getUid(),
                program.getUid(),
                programStage.getUid(),
                event.getEnrollment().getUid(),
                event.getEnrollment().getTrackedEntity().getUid(),
                event.getOrganisationUnit().getUid(),
                event.getEventDataValues().iterator().next().getDataElement(),
                NEW_DATA_ELEMENT_VALUE);

    JsonWebMessage importResponse =
        POST("/tracker?async=false&importStrategy=UPDATE", json)
            .content(HttpStatus.OK)
            .as(JsonWebMessage.class);
    assertEquals(HttpStatus.OK.toString(), importResponse.getStatus());

    JsonWebMessage changeLogResponse =
        GET("/tracker/events/{id}/changeLog", event.getUid())
            .content(HttpStatus.OK)
            .as(JsonWebMessage.class);

    JsonObject changeLogObject = changeLogResponse.asList(JsonList.class).get(0).asObject();
    JsonObject updatedByValue = changeLogObject.get("updatedBy").asObject();
    JsonObject dataValueObject =
        changeLogObject.get("change").asObject().get("dataValue").asObject();
    UserDetails currentUser = CurrentUserUtil.getCurrentUserDetails();

    assertAll(
        () -> {
          assertEquals(currentUser.getUid(), updatedByValue.getString("uid").string());
          assertEquals(currentUser.getUsername(), updatedByValue.getString("username").string());
          assertEquals(currentUser.getFirstName(), updatedByValue.getString("firstName").string());
          assertEquals(currentUser.getSurname(), updatedByValue.getString("surname").string());

          assertEquals(
              event.getEventDataValues().iterator().next().getDataElement(),
              dataValueObject.getString("dataElement").string());
          assertEquals(DATA_ELEMENT_VALUE, dataValueObject.getString("previousValue").string());
          assertEquals(NEW_DATA_ELEMENT_VALUE, dataValueObject.getString("currentValue").string());
        });
  }

  private TrackedEntity trackedEntity() {
    TrackedEntity te = trackedEntity(orgUnit);
    manager.save(te, false);
    return te;
  }

  private TrackedEntity trackedEntity(OrganisationUnit orgUnit) {
    return trackedEntity(orgUnit, trackedEntityType);
  }

  private TrackedEntity trackedEntity(
      OrganisationUnit orgUnit, TrackedEntityType trackedEntityType) {
    TrackedEntity te = createTrackedEntity(orgUnit);
    te.setTrackedEntityType(trackedEntityType);
    te.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
    te.getSharing().setOwner(owner);
    return te;
  }

  private TrackedEntityType trackedEntityTypeAccessible() {
    TrackedEntityType type = trackedEntityType('A');
    type.getSharing().addUserAccess(userAccess());
    manager.save(type, false);
    return type;
  }

  private TrackedEntityType trackedEntityType(char uniqueChar) {
    TrackedEntityType type = createTrackedEntityType(uniqueChar);
    type.getSharing().setOwner(owner);
    type.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
    return type;
  }

  private UserAccess userAccess() {
    UserAccess a = new UserAccess();
    a.setUser(user);
    a.setAccess(AccessStringHelper.FULL);
    return a;
  }

  private Enrollment enrollment(TrackedEntity te) {
    Enrollment enrollment = new Enrollment(program, te, te.getOrganisationUnit());
    enrollment.setAutoFields();
    enrollment.setEnrollmentDate(new Date());
    enrollment.setOccurredDate(new Date());
    enrollment.setStatus(ProgramStatus.COMPLETED);
    manager.save(enrollment);
    return enrollment;
  }

  private Event event(Enrollment enrollment) {
    Event event = new Event(enrollment, programStage, enrollment.getOrganisationUnit());
    event.setAutoFields();
    manager.save(event);
    return event;
  }
}
