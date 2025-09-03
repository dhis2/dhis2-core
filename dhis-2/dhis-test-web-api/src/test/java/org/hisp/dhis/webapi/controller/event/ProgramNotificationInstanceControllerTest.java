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

import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.utils.Assertions.assertStartsWith;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasNoMember;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.Sets;
import java.util.List;
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
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwnerService;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.controller.tracker.JsonPage;
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

  @Autowired private TrackedEntityProgramOwnerService trackedEntityProgramOwnerService;

  private Enrollment enrollment;
  private Event event;
  private ProgramNotificationInstance enrollmentNotification1;
  private ProgramNotificationInstance enrollmentNotification2;
  private ProgramNotificationInstance eventNotification;

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
    enrollment = createEnrollment(prA, teiA, ouA);
    enrollmentService.addEnrollment(enrollment);
    trackedEntityProgramOwnerService.createTrackedEntityProgramOwner(teiA, prA, ouA);

    enrollmentNotification1 = new ProgramNotificationInstance();
    enrollmentNotification1.setName("enrollment A notification 1");
    enrollmentNotification1.setEnrollment(enrollment);
    programNotificationInstanceService.save(enrollmentNotification1);

    enrollmentNotification2 = new ProgramNotificationInstance();
    enrollmentNotification2.setName("enrollment A notification 2");
    enrollmentNotification2.setEnrollment(enrollment);
    programNotificationInstanceService.save(enrollmentNotification2);

    event = createEvent(psA, enrollment, ouA);
    eventService.addEvent(event);
    eventNotification = new ProgramNotificationInstance();
    eventNotification.setName("event A notification");
    eventNotification.setEvent(event);
    programNotificationInstanceService.save(eventNotification);
  }

  @Test
  void shouldGetProgramNotificationWhenPassingDeprecatedProgramInstanceParam() {
    JsonList<JsonIdentifiableObject> list =
        GET("/programNotificationInstances?programInstance={uid}", enrollment.getUid())
            .content(HttpStatus.OK)
            .getList("programNotificationInstances", JsonIdentifiableObject.class);

    assertContainsOnly(
        List.of(enrollmentNotification1.getName(), enrollmentNotification2.getName()),
        list.toList(JsonIdentifiableObject::getName));
  }

  @Test
  void shouldFailToGetProgramNotificationWhenPassingEnrollmentAndProgramInstanceParams() {
    assertStartsWith(
        "Only one parameter of 'programInstance' and 'enrollment'",
        GET(
                "/programNotificationInstances?enrollment={uid}&programInstance={uid}",
                enrollment.getUid(),
                enrollment.getUid())
            .error(HttpStatus.BAD_REQUEST)
            .getMessage());
  }

  @Test
  void shouldGetProgramNotificationWhenPassingDeprecatedProgramStageInstanceParam() {
    JsonList<JsonIdentifiableObject> list =
        GET("/programNotificationInstances?programStageInstance={uid}", event.getUid())
            .content(HttpStatus.OK)
            .getList("programNotificationInstances", JsonIdentifiableObject.class);

    assertEquals(eventNotification.getName(), list.get(0).getName());
  }

  @Test
  void shouldGetProgramNotificationWhenPassingEventParams() {
    JsonList<JsonIdentifiableObject> list =
        GET("/programNotificationInstances?event={uid}", event.getUid())
            .content(HttpStatus.OK)
            .getList("programNotificationInstances", JsonIdentifiableObject.class);

    assertEquals(eventNotification.getName(), list.get(0).getName());
  }

  @Test
  void shouldFailToGetProgramNotificationWhenPassingEventAndProgramStageInstanceParams() {
    assertStartsWith(
        "Only one parameter of 'programStageInstance' and 'event'",
        GET(
                "/programNotificationInstances?event={uid}&programStageInstance={uid}",
                event.getUid(),
                event.getUid())
            .error(HttpStatus.BAD_REQUEST)
            .getMessage());
  }

  @Test
  void shouldGetPaginatedItemsWithDefaults() {
    JsonPage page =
        GET("/programNotificationInstances?enrollment={uid}", enrollment.getUid())
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    JsonList<JsonIdentifiableObject> list =
        page.getList("programNotificationInstances", JsonIdentifiableObject.class);
    assertContainsOnly(
        List.of(enrollmentNotification1.getName(), enrollmentNotification2.getName()),
        list.toList(JsonIdentifiableObject::getName));

    assertEquals(1, page.getPager().getPage());
    assertEquals(50, page.getPager().getPageSize());
    assertEquals(2, page.getPager().getTotal());
    assertEquals(1, page.getPager().getPageCount());

    // assert deprecated fields
    assertEquals(1, page.getPage());
    assertEquals(50, page.getPageSize());
    assertEquals(2, page.getTotal());
    assertEquals(1, page.getPageCount());
  }

  @Test
  void shouldGetPaginatedItemsWithNonDefaults() {
    JsonPage page =
        GET("/programNotificationInstances?enrollment={uid}&page=2&pageSize=1", enrollment.getUid())
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    JsonList<JsonIdentifiableObject> list =
        page.getList("programNotificationInstances", JsonIdentifiableObject.class);
    assertEquals(
        1,
        list.size(),
        () -> String.format("mismatch in number of expected notification(s), got %s", list));

    assertEquals(2, page.getPager().getPage());
    assertEquals(1, page.getPager().getPageSize());
    assertEquals(2, page.getPager().getTotal());
    assertEquals(2, page.getPager().getPageCount());

    // assert deprecated fields
    assertEquals(2, page.getPage());
    assertEquals(1, page.getPageSize());
    assertEquals(2, page.getTotal());
    assertEquals(2, page.getPageCount());
  }

  @Test
  void shouldGetPaginatedItemsWithPagingSetToTrue() {
    JsonPage page =
        GET("/programNotificationInstances?enrollment={uid}&paging=true", enrollment.getUid())
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    JsonList<JsonIdentifiableObject> list =
        page.getList("programNotificationInstances", JsonIdentifiableObject.class);
    assertContainsOnly(
        List.of(enrollmentNotification1.getName(), enrollmentNotification2.getName()),
        list.toList(JsonIdentifiableObject::getName));

    assertEquals(1, page.getPager().getPage());
    assertEquals(50, page.getPager().getPageSize());
    assertEquals(2, page.getPager().getTotal());
    assertEquals(1, page.getPager().getPageCount());

    // assert deprecated fields
    assertEquals(1, page.getPage());
    assertEquals(50, page.getPageSize());
    assertEquals(2, page.getTotal());
    assertEquals(1, page.getPageCount());
  }

  @Test
  void shouldGetNonPaginatedItemsWithSkipPaging() {
    JsonPage page =
        GET("/programNotificationInstances?enrollment={uid}&skipPaging=true", enrollment.getUid())
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    JsonList<JsonIdentifiableObject> list =
        page.getList("programNotificationInstances", JsonIdentifiableObject.class);
    assertContainsOnly(
        List.of(enrollmentNotification1.getName(), enrollmentNotification2.getName()),
        list.toList(JsonIdentifiableObject::getName));
    assertHasNoMember(page, "pager");

    // assert deprecated fields
    assertHasNoMember(page, "page");
    assertHasNoMember(page, "pageSize");
    assertHasNoMember(page, "total");
    assertHasNoMember(page, "pageCount");
  }

  @Test
  void shouldGetNonPaginatedItemsWithPagingSetToFalse() {
    JsonPage page =
        GET("/programNotificationInstances?enrollment={uid}&paging=false", enrollment.getUid())
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    JsonList<JsonIdentifiableObject> list =
        page.getList("programNotificationInstances", JsonIdentifiableObject.class);
    assertContainsOnly(
        List.of(enrollmentNotification1.getName(), enrollmentNotification2.getName()),
        list.toList(JsonIdentifiableObject::getName));
    assertHasNoMember(page, "pager");

    // assert deprecated fields
    assertHasNoMember(page, "page");
    assertHasNoMember(page, "pageSize");
    assertHasNoMember(page, "total");
    assertHasNoMember(page, "pageCount");
  }

  @Test
  void shouldFailWhenSkipPagingAndPagingAreFalse() {
    String message =
        GET(
                "/programNotificationInstances?enrollment={uid}&paging=false&skipPaging=false",
                enrollment.getUid())
            .content(HttpStatus.BAD_REQUEST)
            .getString("message")
            .string();

    assertStartsWith("Paging can either be enabled or disabled", message);
  }

  @Test
  void shouldFailWhenSkipPagingAndPagingAreTrue() {
    String message =
        GET(
                "/programNotificationInstances?enrollment={uid}&paging=true&skipPaging=true",
                enrollment.getUid())
            .content(HttpStatus.BAD_REQUEST)
            .getString("message")
            .string();

    assertStartsWith("Paging can either be enabled or disabled", message);
  }
}
