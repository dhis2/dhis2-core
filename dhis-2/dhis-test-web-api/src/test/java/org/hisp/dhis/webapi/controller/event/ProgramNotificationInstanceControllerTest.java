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
package org.hisp.dhis.webapi.controller.event;

import static org.hisp.dhis.utils.Assertions.assertStartsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.Sets;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentService;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.EventService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.notification.ProgramNotificationInstance;
import org.hisp.dhis.program.notification.ProgramNotificationInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonIdentifiableObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ProgramNotificationInstanceControllerTest extends DhisControllerConvenienceTest {

  @Autowired private ProgramNotificationInstanceService programNotificationInstanceService;

  @Autowired private EnrollmentService enrollmentService;

  @Autowired private EventService eventService;

  @Autowired private TrackedEntityService teiService;

  @Autowired private IdentifiableObjectManager idObjectManager;
  private Enrollment enrollmentA;

  private Event eventA;
  private ProgramNotificationInstance programNotification;

  @BeforeEach
  void setUp() {
    OrganisationUnit ouA = createOrganisationUnit('A');
    idObjectManager.save(ouA);

    Program prA = createProgram('A', Sets.newHashSet(), ouA);
    idObjectManager.save(prA);
    ProgramStage psA = createProgramStage('A', prA);
    idObjectManager.save(psA);

    TrackedEntity teiA = createTrackedEntity('A', ouA);
    teiService.addTrackedEntity(teiA);

    enrollmentA = createEnrollment(prA, teiA, ouA);
    enrollmentService.addEnrollment(enrollmentA);

    eventA = createEvent(psA, enrollmentA, ouA);
    eventService.addEvent(eventA);

    programNotification = new ProgramNotificationInstance();
    programNotification.setName("notify");
    programNotification.setEnrollment(enrollmentA);
    programNotification.setEvent(eventA);
    programNotificationInstanceService.save(programNotification);
  }

  @Test
  void shouldGetProgramNotificationWhenPassingDeprecatedProgramInstanceParam() {
    JsonList<JsonIdentifiableObject> list =
        GET("/programNotificationInstances?programInstance={uid}", enrollmentA.getUid())
            .content(HttpStatus.OK)
            .getList("instances", JsonIdentifiableObject.class);

    assertEquals(programNotification.getName(), list.get(0).getName());
  }

  @Test
  void shouldGetProgramNotificationWhenPassingEnrollmentParam() {
    JsonList<JsonIdentifiableObject> list =
        GET("/programNotificationInstances?enrollment={uid}", enrollmentA.getUid())
            .content(HttpStatus.OK)
            .getList("instances", JsonIdentifiableObject.class);

    assertEquals(programNotification.getName(), list.get(0).getName());
  }

  @Test
  void shouldFailToGetProgramNotificationWhenPassingEnrollmentAndProgramInstanceParams() {
    assertStartsWith(
        "Only one parameter of 'programInstance' and 'enrollment'",
        GET(
                "/programNotificationInstances?enrollment={uid}&programInstance={uid}",
                enrollmentA.getUid(),
                enrollmentA.getUid())
            .error(HttpStatus.BAD_REQUEST)
            .getMessage());
  }

  @Test
  void shouldGetProgramNotificationWhenPassingDeprecatedProgramStageInstanceParam() {
    JsonList<JsonIdentifiableObject> list =
        GET("/programNotificationInstances?programStageInstance={uid}", eventA.getUid())
            .content(HttpStatus.OK)
            .getList("instances", JsonIdentifiableObject.class);

    assertEquals(programNotification.getName(), list.get(0).getName());
  }

  @Test
  void shouldGetProgramNotificationWhenPassingEventParams() {
    JsonList<JsonIdentifiableObject> list =
        GET("/programNotificationInstances?event={uid}", eventA.getUid())
            .content(HttpStatus.OK)
            .getList("instances", JsonIdentifiableObject.class);

    assertEquals(programNotification.getName(), list.get(0).getName());
  }

  @Test
  void shouldFailToGetProgramNotificationWhenPassingEventAndProgramStageInstanceParams() {
    assertStartsWith(
        "Only one parameter of 'programStageInstance' and 'event'",
        GET(
                "/programNotificationInstances?event={uid}&programStageInstance={uid}",
                eventA.getUid(),
                eventA.getUid())
            .error(HttpStatus.BAD_REQUEST)
            .getMessage());
  }
}
