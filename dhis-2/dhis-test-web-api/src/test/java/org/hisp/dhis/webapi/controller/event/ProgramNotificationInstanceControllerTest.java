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
package org.hisp.dhis.webapi.controller.event;

import static org.hisp.dhis.security.Authorities.ALL;
import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertHasNoMember;
import static org.hisp.dhis.webapi.controller.tracker.JsonAssertions.assertPagerLink;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.program.notification.ProgramNotificationInstance;
import org.hisp.dhis.program.notification.ProgramNotificationInstanceService;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonIdentifiableObject;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.acl.TrackedEntityProgramOwnerService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.tracker.JsonPage;
import org.hisp.dhis.webapi.controller.tracker.JsonPage.JsonPager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProgramNotificationInstanceControllerTest extends PostgresControllerIntegrationTestBase {

  @Autowired private ProgramNotificationInstanceService programNotificationInstanceService;

  @Autowired private TrackedEntityProgramOwnerService trackedEntityProgramOwnerService;

  private Enrollment enrollment;

  private TrackerEvent event;

  private ProgramNotificationInstance enrollmentNotification1;

  private ProgramNotificationInstance enrollmentNotification2;

  private ProgramNotificationInstance eventNotification;

  @BeforeEach
  void setUp() {
    OrganisationUnit orgUnit = createOrganisationUnit('A');
    manager.save(orgUnit);

    User user = createAndAddUser("tester", orgUnit, ALL.name());
    user.setTeiSearchOrganisationUnits(Set.of(orgUnit));
    this.userService.updateUser(user);

    Program prA = createProgram('A', Set.of(), orgUnit);
    manager.save(prA);
    ProgramStage psA = createProgramStage('A', prA);
    manager.save(psA);
    TrackedEntityType trackedEntityType = createTrackedEntityType('O');
    manager.save(trackedEntityType);
    TrackedEntity trackedEntityA = createTrackedEntity('A', orgUnit, trackedEntityType);
    manager.save(trackedEntityA);
    enrollment = createEnrollment(prA, trackedEntityA, orgUnit);
    manager.save(enrollment);
    trackedEntityProgramOwnerService.createTrackedEntityProgramOwner(trackedEntityA, prA, orgUnit);

    enrollmentNotification1 = new ProgramNotificationInstance();
    enrollmentNotification1.setName("enrollment A notification 1");
    enrollmentNotification1.setEnrollment(enrollment);
    programNotificationInstanceService.save(enrollmentNotification1);

    enrollmentNotification2 = new ProgramNotificationInstance();
    enrollmentNotification2.setName("enrollment A notification 2");
    enrollmentNotification2.setEnrollment(enrollment);
    programNotificationInstanceService.save(enrollmentNotification2);

    event = createEvent(psA, enrollment, orgUnit);
    manager.save(event);
    eventNotification = new ProgramNotificationInstance();
    eventNotification.setName("event A notification");
    eventNotification.setEvent(event);
    programNotificationInstanceService.save(eventNotification);
    manager.flush();

    switchContextToUser(user);
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

    JsonPager pager = page.getPager();
    assertEquals(1, pager.getPage());
    assertEquals(50, pager.getPageSize());
    assertHasNoMember(pager, "total", "pageCount");
  }

  @Test
  void shouldGetFirstPage() {
    JsonPage page =
        GET(
                "/programNotificationInstances?enrollment={uid}&page=1&pageSize=1&totalPages=false",
                enrollment.getUid())
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    JsonList<JsonIdentifiableObject> list =
        page.getList("programNotificationInstances", JsonIdentifiableObject.class);
    assertEquals(
        1,
        list.size(),
        () -> String.format("mismatch in number of expected notification(s), got %s", list));

    JsonPager pager = page.getPager();
    assertEquals(1, pager.getPage());
    assertEquals(1, page.getPager().getPageSize());
    assertHasNoMember(pager, "total", "pageCount", "prevPage");
    assertPagerLink(
        pager.getNextPage(),
        2,
        1,
        String.format(
            "http://localhost/api/programNotificationInstances?enrollment=%s",
            enrollment.getUid()));
  }

  @Test
  void shouldGetLastPage() {
    JsonPage page =
        GET(
                "/programNotificationInstances?enrollment={uid}&page=2&pageSize=1&totalPages=true",
                enrollment.getUid())
            .content(HttpStatus.OK)
            .asA(JsonPage.class);

    JsonList<JsonIdentifiableObject> list =
        page.getList("programNotificationInstances", JsonIdentifiableObject.class);
    assertEquals(
        1,
        list.size(),
        () -> String.format("mismatch in number of expected notification(s), got %s", list));

    JsonPager pager = page.getPager();
    assertEquals(2, pager.getPage());
    assertEquals(1, page.getPager().getPageSize());
    assertEquals(2, page.getPager().getTotal());
    assertEquals(2, page.getPager().getPageCount());
    assertPagerLink(
        pager.getPrevPage(),
        1,
        1,
        String.format(
            "http://localhost/api/programNotificationInstances?enrollment=%s",
            enrollment.getUid()));
    assertHasNoMember(pager, "nextPage");
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

    JsonPager pager = page.getPager();
    assertEquals(1, pager.getPage());
    assertEquals(50, page.getPager().getPageSize());
    assertHasNoMember(pager, "total", "pageCount");
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
  }
}
