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
package org.hisp.dhis.tracker.export;

import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;
import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.SlimPager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentOperationParams;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentOperationParams.EnrollmentOperationParamsBuilder;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentService;
import org.hisp.dhis.tracker.export.enrollment.Enrollments;
import org.hisp.dhis.tracker.export.event.EventOperationParams;
import org.hisp.dhis.tracker.export.event.EventOperationParams.EventOperationParamsBuilder;
import org.hisp.dhis.tracker.export.event.EventService;
import org.hisp.dhis.tracker.export.event.Events;
import org.hisp.dhis.tracker.export.relationship.RelationshipOperationParams;
import org.hisp.dhis.tracker.export.relationship.RelationshipOperationParams.RelationshipOperationParamsBuilder;
import org.hisp.dhis.tracker.export.relationship.RelationshipService;
import org.hisp.dhis.tracker.export.relationship.Relationships;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntities;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityOperationParams;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityOperationParams.TrackedEntityOperationParamsBuilder;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityService;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.event.mapper.SortDirection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Tests ordering and pagination of tracker exporters via the service layer. */
class OrderAndPaginationExporterTest extends TrackerTest {

  @Autowired private TrackedEntityService trackedEntityService;

  @Autowired private EnrollmentService enrollmentService;

  @Autowired private EventService eventService;

  @Autowired private RelationshipService relationshipService;

  @Autowired private TrackerImportService trackerImportService;

  @Autowired private IdentifiableObjectManager manager;

  private OrganisationUnit orgUnit;

  private ProgramStage programStage;

  private TrackedEntityType trackedEntityType;

  private EventOperationParamsBuilder eventParamsBuilder;

  private User importUser;

  @Override
  protected void initTest() throws IOException {
    setUpMetadata("tracker/simple_metadata.json");
    importUser = userService.getUser("M5zQapPyTZI");
    assertNoErrors(
        trackerImportService.importTracker(
            fromJson("tracker/event_and_enrollment.json", importUser.getUid())));
    orgUnit = get(OrganisationUnit.class, "h4w96yEMlzO");
    programStage = get(ProgramStage.class, "NpsdDv6kKSO");
    trackedEntityType = get(TrackedEntityType.class, "ja8NY4PW7Xm");

    manager.flush();
  }

  @BeforeEach
  void setUp() {
    // needed as some tests are run using another user (injectSecurityContext) while most tests
    // expect to be run by admin
    injectAdminUser();

    eventParamsBuilder = EventOperationParams.builder();
    eventParamsBuilder.orgUnitMode(SELECTED);
  }

  @Test
  void shouldReturnPaginatedTrackedEntitiesGivenNonDefaultPageSize()
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityOperationParamsBuilder builder =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnit.getUid()))
            .trackedEntityTypeUid(trackedEntityType.getUid())
            .orderBy(UID.of("numericAttr"), SortDirection.ASC);

    TrackedEntityOperationParams params = builder.page(1).pageSize(3).user(importUser).build();

    TrackedEntities firstPage = trackedEntityService.getTrackedEntities(params);

    assertAll(
        "first page",
        // TODO(tracker): fix in TECH-1601. I assume this was recently introduced (only on master)
        // when
        // handleLastPageFlag was copied from the event service which works in conjunction with the
        // event store
        // that fetches pageSize + 1 when totalCount=false. This split of logic between
        // service/store is
        // error prone and hard to follow. We will fix/refactor it for all entities so this is
        // purely a concern
        // of the store.
        () -> assertSlimPager(1, 3, true, firstPage.getPager()),
        () ->
            assertEquals(
                List.of("dUE514NMOlo", "mHWCacsGYYn", "QS6w44flWAf"),
                uids(firstPage.getTrackedEntities())));

    params = builder.page(2).pageSize(3).build();

    TrackedEntities secondPage = trackedEntityService.getTrackedEntities(params);

    assertAll(
        "second (last) page",
        () -> assertSlimPager(2, 3, true, secondPage.getPager()),
        () ->
            assertEquals(
                List.of("QesgJkTyTCk", "guVNoAerxWo"), uids(secondPage.getTrackedEntities())));

    params = builder.page(3).pageSize(3).build();

    assertIsEmpty(getTrackedEntities(params));
  }

  @Test
  void shouldReturnPaginatedTrackedEntitiesGivenNonDefaultPageSizeAndTotalPages()
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityOperationParamsBuilder builder =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnit.getUid()))
            .trackedEntityTypeUid(trackedEntityType.getUid())
            .user(importUser)
            .orderBy(UID.of("numericAttr"), SortDirection.ASC);

    TrackedEntityOperationParams params = builder.page(1).pageSize(3).totalPages(true).build();

    TrackedEntities firstPage = trackedEntityService.getTrackedEntities(params);

    assertAll(
        "first page",
        () -> assertPager(1, 3, 5, firstPage.getPager()),
        () ->
            assertEquals(
                List.of("dUE514NMOlo", "mHWCacsGYYn", "QS6w44flWAf"),
                uids(firstPage.getTrackedEntities())));

    params = builder.page(2).pageSize(3).totalPages(true).build();

    TrackedEntities secondPage = trackedEntityService.getTrackedEntities(params);

    assertAll(
        "second (last) page",
        () -> assertPager(2, 3, 5, secondPage.getPager()),
        () ->
            assertEquals(
                List.of("QesgJkTyTCk", "guVNoAerxWo"), uids(secondPage.getTrackedEntities())));

    params = builder.page(3).pageSize(3).totalPages(true).build();

    assertIsEmpty(getTrackedEntities(params));
  }

  @Test
  void shouldOrderTrackedEntitiesByPrimaryKeyDescByDefault()
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntity QS6w44flWAf = get(TrackedEntity.class, "QS6w44flWAf");
    TrackedEntity dUE514NMOlo = get(TrackedEntity.class, "dUE514NMOlo");
    List<String> expected =
        Stream.of(QS6w44flWAf, dUE514NMOlo)
            .sorted(Comparator.comparing(TrackedEntity::getId).reversed()) // reversed = desc
            .map(TrackedEntity::getUid)
            .toList();

    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnit.getUid()))
            .trackedEntityUids(Set.of("QS6w44flWAf", "dUE514NMOlo"))
            .trackedEntityTypeUid(trackedEntityType.getUid())
            .user(importUser)
            .build();

    List<String> trackedEntities = getTrackedEntities(params);

    assertEquals(expected, trackedEntities);
  }

  @Test
  void shouldOrderTrackedEntitiesByCreatedAsc()
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntity QS6w44flWAf = get(TrackedEntity.class, "QS6w44flWAf");
    TrackedEntity dUE514NMOlo = get(TrackedEntity.class, "dUE514NMOlo");

    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnit.getUid()))
            .trackedEntityUids(Set.of("QS6w44flWAf", "dUE514NMOlo"))
            .trackedEntityTypeUid(trackedEntityType.getUid())
            .user(importUser)
            .orderBy("created", SortDirection.ASC)
            .build();

    List<String> trackedEntities = getTrackedEntities(params);

    boolean isSameCreatedDate = QS6w44flWAf.getCreated().equals(dUE514NMOlo.getCreated());
    if (isSameCreatedDate) {
      // the order is non-deterministic if the created date is the same. we can then only assert
      // the correct TEs are in the result. otherwise the test is flaky
      assertContainsOnly(List.of("QS6w44flWAf", "dUE514NMOlo"), trackedEntities);
    } else {
      List<String> expected =
          Stream.of(QS6w44flWAf, dUE514NMOlo)
              .sorted(Comparator.comparing(TrackedEntity::getCreated)) // asc
              .map(TrackedEntity::getUid)
              .toList();
      assertEquals(expected, trackedEntities);
    }
  }

  @Test
  void shouldOrderTrackedEntitiesByCreatedDesc()
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntity QS6w44flWAf = get(TrackedEntity.class, "QS6w44flWAf");
    TrackedEntity dUE514NMOlo = get(TrackedEntity.class, "dUE514NMOlo");

    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnit.getUid()))
            .trackedEntityUids(Set.of("QS6w44flWAf", "dUE514NMOlo"))
            .trackedEntityTypeUid(trackedEntityType.getUid())
            .user(importUser)
            .orderBy("created", SortDirection.DESC)
            .build();

    List<String> trackedEntities = getTrackedEntities(params);

    boolean isSameCreatedDate = QS6w44flWAf.getCreated().equals(dUE514NMOlo.getCreated());
    if (isSameCreatedDate) {
      // the order is non-deterministic if the created date is the same. we can then only assert
      // the correct TEs are in the result. otherwise the test is flaky
      assertContainsOnly(List.of("QS6w44flWAf", "dUE514NMOlo"), trackedEntities);
    } else {
      List<String> expected =
          Stream.of(QS6w44flWAf, dUE514NMOlo)
              .sorted(Comparator.comparing(TrackedEntity::getCreated).reversed()) // reversed = desc
              .map(TrackedEntity::getUid)
              .toList();
      assertEquals(expected, trackedEntities);
    }
  }

  @Test
  void shouldOrderTrackedEntitiesByCreatedAtClientDesc()
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnit.getUid()))
            .trackedEntityUids(Set.of("QS6w44flWAf", "dUE514NMOlo"))
            .trackedEntityTypeUid(trackedEntityType.getUid())
            .user(importUser)
            .orderBy("createdAtClient", SortDirection.DESC)
            .build();

    List<String> trackedEntities = getTrackedEntities(params);

    assertEquals(List.of("dUE514NMOlo", "QS6w44flWAf"), trackedEntities);
  }

  @Test
  void shouldOrderTrackedEntitiesByCreatedAtClientAsc()
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnit.getUid()))
            .trackedEntityUids(Set.of("QS6w44flWAf", "dUE514NMOlo"))
            .trackedEntityTypeUid(trackedEntityType.getUid())
            .user(importUser)
            .orderBy("createdAtClient", SortDirection.ASC)
            .build();

    List<String> trackedEntities = getTrackedEntities(params);

    assertEquals(List.of("QS6w44flWAf", "dUE514NMOlo"), trackedEntities);
  }

  @Test
  void shouldOrderTrackedEntitiesByEnrolledAtAsc()
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnit.getUid()))
            .trackedEntityUids(Set.of("QS6w44flWAf", "dUE514NMOlo"))
            .trackedEntityTypeUid(trackedEntityType.getUid())
            .user(importUser)
            .orderBy("enrollment.enrollmentDate", SortDirection.ASC)
            .build();

    List<String> trackedEntities = getTrackedEntities(params);

    assertEquals(List.of("QS6w44flWAf", "dUE514NMOlo"), trackedEntities);
  }

  @Test
  void shouldOrderTrackedEntitiesByEnrolledAtDesc()
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnit.getUid()))
            .trackedEntityUids(Set.of("QS6w44flWAf", "dUE514NMOlo"))
            .trackedEntityTypeUid(trackedEntityType.getUid())
            .user(importUser)
            .orderBy("enrollment.enrollmentDate", SortDirection.DESC)
            .build();

    List<String> trackedEntities = getTrackedEntities(params);

    assertEquals(List.of("dUE514NMOlo", "QS6w44flWAf"), trackedEntities);
  }

  @Test
  void shouldOrderTrackedEntitiesByFieldAndAttribute()
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnit.getUid()))
            .trackedEntityUids(Set.of("QS6w44flWAf", "dUE514NMOlo"))
            .trackedEntityTypeUid(trackedEntityType.getUid())
            .user(importUser)
            .orderBy(UID.of("toDelete000"), SortDirection.ASC)
            .orderBy("enrollment.enrollmentDate", SortDirection.ASC)
            .build();

    List<String> trackedEntities = getTrackedEntities(params);

    assertEquals(List.of("QS6w44flWAf", "dUE514NMOlo"), trackedEntities);
  }

  @Test
  void shouldOrderTrackedEntitiesByAttributeAsc()
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnit.getUid()))
            .trackedEntityUids(Set.of("QS6w44flWAf", "dUE514NMOlo"))
            .trackedEntityTypeUid(trackedEntityType.getUid())
            .user(importUser)
            .orderBy(UID.of("toUpdate000"), SortDirection.ASC)
            .build();

    List<String> trackedEntities = getTrackedEntities(params);

    assertEquals(List.of("dUE514NMOlo", "QS6w44flWAf"), trackedEntities);
  }

  @Test
  void shouldOrderTrackedEntitiesByAttributeDesc()
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnit.getUid()))
            .trackedEntityUids(Set.of("QS6w44flWAf", "dUE514NMOlo"))
            .trackedEntityTypeUid(trackedEntityType.getUid())
            .user(importUser)
            .orderBy(UID.of("toUpdate000"), SortDirection.DESC)
            .build();

    List<String> trackedEntities = getTrackedEntities(params);

    assertEquals(List.of("QS6w44flWAf", "dUE514NMOlo"), trackedEntities);
  }

  @Test
  void shouldOrderTrackedEntitiesByAttributeWhenFiltered()
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnit.getUid()))
            .trackedEntityTypeUid(trackedEntityType.getUid())
            .user(importUser)
            .orderBy(UID.of("toUpdate000"), SortDirection.ASC)
            .filters("numericAttr:lt:75")
            .build();

    List<String> trackedEntities = getTrackedEntities(params);

    assertEquals(List.of("dUE514NMOlo", "mHWCacsGYYn"), trackedEntities);
  }

  @Test
  void shouldOrderTrackedEntitiesByAttributeWhenFilteredOnSameAttribute()
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnit.getUid()))
            .trackedEntityTypeUid(trackedEntityType.getUid())
            .user(importUser)
            .orderBy(UID.of("numericAttr"), SortDirection.DESC)
            .filters("numericAttr:lt:75")
            .build();

    List<String> trackedEntities = getTrackedEntities(params);

    assertEquals(List.of("mHWCacsGYYn", "dUE514NMOlo"), trackedEntities);
  }

  @Test
  void shouldOrderTrackedEntitiesByAttributeAndNotFilterOutATrackedEntityWithoutThatAttribute()
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnit.getUid()))
            .trackedEntityTypeUid(trackedEntityType.getUid())
            .trackedEntityUids(
                Set.of(
                    "dUE514NMOlo", "QS6w44flWAf")) // TE QS6w44flWAf without attribute notUpdated0
            .user(importUser)
            .orderBy(UID.of("notUpdated0"), SortDirection.DESC)
            .build();

    List<String> trackedEntities = getTrackedEntities(params);

    // https://www.postgresql.org/docs/current/queries-order.html
    // By default, null values sort as if larger than any non-null value
    // => TE QS6w44flWAf without attribute notUpdated0 will come first when DESC
    assertEquals(List.of("QS6w44flWAf", "dUE514NMOlo"), trackedEntities);
  }

  @Test
  void shouldOrderTrackedEntitiesByMultipleAttributesAsc()
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnit.getUid()))
            .trackedEntityUids(Set.of("QS6w44flWAf", "dUE514NMOlo"))
            .trackedEntityTypeUid(trackedEntityType.getUid())
            .user(importUser)
            .orderBy(UID.of("toDelete000"), SortDirection.DESC)
            .orderBy(UID.of("numericAttr"), SortDirection.ASC)
            .build();

    List<String> trackedEntities = getTrackedEntities(params);

    assertEquals(List.of("dUE514NMOlo", "QS6w44flWAf"), trackedEntities);
  }

  @Test
  void shouldOrderTrackedEntitiesByMultipleAttributesDesc()
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnit.getUid()))
            .trackedEntityUids(Set.of("QS6w44flWAf", "dUE514NMOlo"))
            .trackedEntityTypeUid(trackedEntityType.getUid())
            .user(importUser)
            .orderBy(UID.of("toDelete000"), SortDirection.DESC)
            .orderBy(UID.of("numericAttr"), SortDirection.DESC)
            .build();

    List<String> trackedEntities = getTrackedEntities(params);

    assertEquals(List.of("QS6w44flWAf", "dUE514NMOlo"), trackedEntities);
  }

  @Test
  void shouldReturnPaginatedEnrollmentsGivenNonDefaultPageSize()
      throws ForbiddenException, BadRequestException {
    EnrollmentOperationParamsBuilder builder =
        EnrollmentOperationParams.builder()
            .orgUnitUids(Set.of(orgUnit.getUid()))
            .orderBy("enrollmentDate", SortDirection.ASC);

    EnrollmentOperationParams params = builder.page(1).pageSize(1).build();

    Enrollments firstPage = enrollmentService.getEnrollments(params);

    assertAll(
        "first page",
        () -> assertSlimPager(1, 1, false, firstPage.getPager()),
        () -> assertEquals(List.of("nxP7UnKhomJ"), uids(firstPage.getEnrollments())));

    params = builder.page(2).pageSize(1).build();

    Enrollments secondPage = enrollmentService.getEnrollments(params);

    assertAll(
        "second (last) page",
        () -> assertSlimPager(2, 1, true, secondPage.getPager()),
        () -> assertEquals(List.of("TvctPPhpD8z"), uids(secondPage.getEnrollments())));

    params = builder.page(3).pageSize(1).build();

    assertIsEmpty(getEnrollments(params));
  }

  @Test
  void shouldReturnPaginatedEnrollmentsGivenNonDefaultPageSizeAndTotalPages()
      throws ForbiddenException, BadRequestException {
    EnrollmentOperationParamsBuilder builder =
        EnrollmentOperationParams.builder()
            .orgUnitUids(Set.of(orgUnit.getUid()))
            .orderBy("enrollmentDate", SortDirection.ASC);

    EnrollmentOperationParams params = builder.page(1).pageSize(1).totalPages(true).build();

    Enrollments firstPage = enrollmentService.getEnrollments(params);

    assertAll(
        "first page",
        () -> assertPager(1, 1, 2, firstPage.getPager()),
        () -> assertEquals(List.of("nxP7UnKhomJ"), uids(firstPage.getEnrollments())));

    params = builder.page(2).pageSize(1).totalPages(true).build();

    Enrollments secondPage = enrollmentService.getEnrollments(params);

    assertAll(
        "second (last) page",
        () -> assertPager(2, 1, 2, secondPage.getPager()),
        () -> assertEquals(List.of("TvctPPhpD8z"), uids(secondPage.getEnrollments())));

    params = builder.page(3).pageSize(1).totalPages(true).build();

    assertIsEmpty(getEnrollments(params));
  }

  @Test
  void shouldOrderEnrollmentsByPrimaryKeyDescByDefault()
      throws ForbiddenException, BadRequestException {
    Enrollment nxP7UnKhomJ = get(Enrollment.class, "nxP7UnKhomJ");
    Enrollment TvctPPhpD8z = get(Enrollment.class, "TvctPPhpD8z");
    List<String> expected =
        Stream.of(nxP7UnKhomJ, TvctPPhpD8z)
            .sorted(Comparator.comparing(Enrollment::getId).reversed()) // reversed = desc
            .map(Enrollment::getUid)
            .toList();

    EnrollmentOperationParams params =
        EnrollmentOperationParams.builder().orgUnitUids(Set.of(orgUnit.getUid())).build();

    List<String> enrollments = getEnrollments(params);

    assertEquals(expected, enrollments);
  }

  @Test
  void shouldOrderEnrollmentsByEnrolledAtAsc() throws ForbiddenException, BadRequestException {
    EnrollmentOperationParams params =
        EnrollmentOperationParams.builder()
            .orgUnitUids(Set.of(orgUnit.getUid()))
            .orderBy("enrollmentDate", SortDirection.ASC)
            .build();

    List<String> enrollments = getEnrollments(params);

    assertEquals(List.of("nxP7UnKhomJ", "TvctPPhpD8z"), enrollments);
  }

  @Test
  void shouldOrderEnrollmentsByEnrolledAtDesc() throws ForbiddenException, BadRequestException {
    EnrollmentOperationParams params =
        EnrollmentOperationParams.builder()
            .orgUnitUids(Set.of(orgUnit.getUid()))
            .orderBy("enrollmentDate", SortDirection.DESC)
            .build();

    List<String> enrollments = getEnrollments(params);

    assertEquals(List.of("TvctPPhpD8z", "nxP7UnKhomJ"), enrollments);
  }

  @Test
  void shouldReturnPaginatedEventsWithNotesGivenNonDefaultPageSize()
      throws ForbiddenException, BadRequestException {
    EventOperationParamsBuilder paramsBuilder =
        eventParamsBuilder
            .orgUnitUid(orgUnit.getUid())
            .events(Set.of("pTzf9KYMk72", "D9PbzJY8bJM"))
            .orderBy("executionDate", SortDirection.DESC);

    EventOperationParams params = paramsBuilder.page(1).pageSize(1).build();

    Events firstPage = eventService.getEvents(params);

    assertAll(
        "first page",
        () -> assertSlimPager(1, 1, false, firstPage.getPager()),
        () -> assertEquals(List.of("D9PbzJY8bJM"), eventUids(firstPage)));

    params = paramsBuilder.page(2).pageSize(1).build();

    Events secondPage = eventService.getEvents(params);

    assertAll(
        "second (last) page",
        () -> assertSlimPager(2, 1, true, secondPage.getPager()),
        () -> assertEquals(List.of("pTzf9KYMk72"), eventUids(secondPage)));

    params = paramsBuilder.page(3).pageSize(3).build();

    assertIsEmpty(getEvents(params));
  }

  @Test
  void shouldReturnPaginatedEventsWithTotalPages() throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        eventParamsBuilder
            .orgUnitUid(orgUnit.getUid())
            .programStageUid(programStage.getUid())
            .totalPages(true)
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), events);
  }

  @Test
  void shouldReturnPaginatedPublicEventsWithMultipleCategoryOptionsGivenNonDefaultPageSize()
      throws ForbiddenException, BadRequestException {
    OrganisationUnit orgUnit = get(OrganisationUnit.class, "DiszpKrYNg8");
    Program program = get(Program.class, "iS7eutanDry");

    EventOperationParamsBuilder paramsBuilder =
        eventParamsBuilder
            .orgUnitUid(orgUnit.getUid())
            .programUid(program.getUid())
            .orderBy("executionDate", SortDirection.DESC);

    EventOperationParams params = paramsBuilder.page(1).pageSize(3).build();

    Events firstPage = eventService.getEvents(params);

    assertAll(
        "first page",
        () -> assertSlimPager(1, 3, false, firstPage.getPager()),
        () ->
            assertEquals(
                List.of("ck7DzdxqLqA", "OTmjvJDn0Fu", "kWjSezkXHVp"), eventUids(firstPage)));

    params = paramsBuilder.page(2).pageSize(3).build();

    Events secondPage = eventService.getEvents(params);

    assertAll(
        "second (last) page",
        () -> assertSlimPager(2, 3, true, secondPage.getPager()),
        () ->
            assertEquals(
                List.of("lumVtWwwy0O", "QRYjLTiJTrA", "cadc5eGj0j7"), eventUids(secondPage)));

    params = paramsBuilder.page(3).pageSize(3).build();

    assertIsEmpty(getEvents(params));
  }

  @Test
  void shouldReturnPaginatedEventsWithMultipleCategoryOptionsGivenNonDefaultPageSizeAndTotalPages()
      throws ForbiddenException, BadRequestException {
    OrganisationUnit orgUnit = get(OrganisationUnit.class, "DiszpKrYNg8");
    Program program = get(Program.class, "iS7eutanDry");

    EventOperationParams params =
        eventParamsBuilder
            .orgUnitUid(orgUnit.getUid())
            .programUid(program.getUid())
            .orderBy("executionDate", SortDirection.DESC)
            .page(1)
            .pageSize(2)
            .totalPages(true)
            .build();

    Events events = eventService.getEvents(params);

    assertAll(
        "first page",
        () -> assertPager(1, 2, 6, events.getPager()),
        () -> assertEquals(List.of("ck7DzdxqLqA", "OTmjvJDn0Fu"), eventUids(events)));
  }

  @Test
  void shouldOrderEventsByPrimaryKeyDescByDefault() throws ForbiddenException, BadRequestException {
    Event d9PbzJY8bJM = get(Event.class, "D9PbzJY8bJM");
    Event pTzf9KYMk72 = get(Event.class, "pTzf9KYMk72");
    List<String> expected =
        Stream.of(d9PbzJY8bJM, pTzf9KYMk72)
            .sorted(Comparator.comparing(Event::getId).reversed()) // reversed = desc
            .map(Event::getUid)
            .toList();

    EventOperationParams params = eventParamsBuilder.orgUnitUid(orgUnit.getUid()).build();

    List<String> events = getEvents(params);

    assertEquals(expected, events);
  }

  @Test
  void shouldOrderEventsByEnrollmentProgramUIDAsc() throws ForbiddenException, BadRequestException {
    Event pTzf9KYMk72 =
        get(Event.class, "pTzf9KYMk72"); // enrolled in program BFcipDERJnf with registration
    Event QRYjLTiJTrA =
        get(Event.class, "QRYjLTiJTrA"); // enrolled in program BFcipDERJne without registration
    List<String> expected =
        Stream.of(pTzf9KYMk72, QRYjLTiJTrA)
            .sorted(Comparator.comparing(event -> event.getEnrollment().getProgram().getUid()))
            .map(Event::getUid)
            .toList();

    EventOperationParams params =
        EventOperationParams.builder()
            .orgUnitMode(ACCESSIBLE)
            .events(Set.of("pTzf9KYMk72", "QRYjLTiJTrA"))
            .orderBy("enrollment.program.uid", SortDirection.ASC)
            .build();

    List<String> events = getEvents(params);

    assertEquals(expected, events);
  }

  @Test
  void shouldOrderEventsByAttributeAsc() throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        eventParamsBuilder
            .orgUnitUid(orgUnit.getUid())
            .orderBy(UID.of("toUpdate000"), SortDirection.ASC)
            .build();

    List<String> events = getEvents(params);

    assertEquals(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), events);
  }

  @Test
  void shouldOrderEventsByAttributeDesc() throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        eventParamsBuilder
            .orgUnitUid(orgUnit.getUid())
            .orderBy(UID.of("toUpdate000"), SortDirection.DESC)
            .build();

    List<String> events = getEvents(params);

    assertEquals(List.of("pTzf9KYMk72", "D9PbzJY8bJM"), events);
  }

  @Test
  void shouldOrderEventsByAttributeAndFilterOutEventsWithATrackedEntityWithoutThatAttribute()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        eventParamsBuilder
            .orgUnitUid(orgUnit.getUid())
            .events(
                Set.of(
                    "pTzf9KYMk72",
                    "D9PbzJY8bJM")) // EV pTzf9KYMk72 => TE QS6w44flWAf without attribute
            // notUpdated0
            .orderBy(UID.of("notUpdated0"), SortDirection.ASC)
            .build();

    List<String> events = getEvents(params);

    assertEquals(List.of("D9PbzJY8bJM"), events);
  }

  @Test
  void shouldOrderEventsByMultipleAttributesDesc() throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        eventParamsBuilder
            .orgUnitUid(orgUnit.getUid())
            .orderBy(UID.of("toDelete000"), SortDirection.DESC)
            .orderBy(UID.of("toUpdate000"), SortDirection.DESC)
            .build();

    List<String> events = getEvents(params);

    assertEquals(List.of("pTzf9KYMk72", "D9PbzJY8bJM"), events);
  }

  @Test
  void shouldOrderEventsByMultipleAttributesAsc() throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        eventParamsBuilder
            .orgUnitUid(orgUnit.getUid())
            .orderBy(UID.of("toDelete000"), SortDirection.DESC)
            .orderBy(UID.of("toUpdate000"), SortDirection.ASC)
            .build();

    List<Event> events = eventService.getEvents(params).getEvents();

    assertEquals(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), uids(events));
    List<String> trackedEntities =
        events.stream()
            .map(event -> event.getEnrollment().getTrackedEntity().getUid())
            .collect(Collectors.toList());
    assertEquals(List.of("dUE514NMOlo", "QS6w44flWAf"), trackedEntities);
  }

  @Test
  void shouldReturnPaginatedEventsOrderedByMultipleAttributesWhenGivenNonDefaultPageSize()
      throws ForbiddenException, BadRequestException {
    EventOperationParamsBuilder paramsBuilder =
        eventParamsBuilder
            .orgUnitUid(orgUnit.getUid())
            .orderBy(UID.of("toDelete000"), SortDirection.DESC)
            .orderBy(UID.of("toUpdate000"), SortDirection.ASC);

    EventOperationParams params = paramsBuilder.page(1).pageSize(1).build();

    Events firstPage = eventService.getEvents(params);

    assertAll(
        "first page",
        () -> assertSlimPager(1, 1, false, firstPage.getPager()),
        () -> assertEquals(List.of("D9PbzJY8bJM"), eventUids(firstPage)));

    params = paramsBuilder.page(2).pageSize(1).build();

    Events secondPage = eventService.getEvents(params);

    assertAll(
        "second (last) page",
        () -> assertSlimPager(2, 1, true, secondPage.getPager()),
        () -> assertEquals(List.of("pTzf9KYMk72"), eventUids(secondPage)));

    params = paramsBuilder.page(3).pageSize(3).build();

    assertIsEmpty(getEvents(params));
  }

  @Test
  void shouldOrderEventsByEnrolledAtDesc() throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        eventParamsBuilder
            .orgUnitUid(orgUnit.getUid())
            .orderBy("enrollment.enrollmentDate", SortDirection.DESC)
            .build();

    List<String> events = getEvents(params);

    assertEquals(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), events);
  }

  @Test
  void shouldOrderEventsByEnrolledAtAsc() throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        eventParamsBuilder
            .orgUnitUid(orgUnit.getUid())
            .orderBy("enrollment.enrollmentDate", SortDirection.ASC)
            .build();

    List<String> events = getEvents(params);

    assertEquals(List.of("pTzf9KYMk72", "D9PbzJY8bJM"), events);
  }

  @Test
  void shouldOrderEventsByTrackedEntityUidDesc() throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        eventParamsBuilder
            .orgUnitUid(orgUnit.getUid())
            .orderBy("enrollment.trackedEntity.uid", SortDirection.DESC)
            .build();

    List<String> events = getEvents(params);

    // TODO(tracker): TECH-1620 the order is reversed
    // EV D9PbzJY8bJM EN TvctPPhpD8z TE dUE514NMOlo
    // EV pTzf9KYMk72 EN nxP7UnKhomJ TE QS6w44flWAf
    // We would therefore expect the TE order to be QS6w44flWAf, dUE514NMOlo
    // and thus the EV order to be pTzf9KYMk72, D9PbzJY8bJM
    assertEquals(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), events);
  }

  @Test
  void shouldOrderEventsByTrackedEntityUidAsc() throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        eventParamsBuilder
            .orgUnitUid(orgUnit.getUid())
            .orderBy("enrollment.trackedEntity.uid", SortDirection.ASC)
            .build();

    List<String> events = getEvents(params);

    // TODO(tracker): TECH-1620 the order is reversed
    // EV D9PbzJY8bJM EN TvctPPhpD8z TE dUE514NMOlo
    // EV pTzf9KYMk72 EN nxP7UnKhomJ TE QS6w44flWAf
    // We would therefore expect the TE order to be dUE514NMOlo, QS6w44flWAf
    // and thus the EV order to be D9PbzJY8bJM, pTzf9KYMk72
    assertEquals(List.of("pTzf9KYMk72", "D9PbzJY8bJM"), events);
  }

  @Test
  void shouldOrderEventsByOccurredAtDesc() throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        eventParamsBuilder
            .orgUnitUid(orgUnit.getUid())
            .orderBy("executionDate", SortDirection.DESC)
            .build();

    List<String> events = getEvents(params);

    assertEquals(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), events);
  }

  @Test
  void shouldOrderEventsByOccurredAtAsc() throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        eventParamsBuilder
            .orgUnitUid(orgUnit.getUid())
            .orderBy("executionDate", SortDirection.ASC)
            .build();

    List<String> events = getEvents(params);

    assertEquals(List.of("pTzf9KYMk72", "D9PbzJY8bJM"), events);
  }

  @Test
  void shouldOrderEventsRespectingOrderWhenAttributeOrderSuppliedBeforeOrderParam()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        eventParamsBuilder
            .orgUnitUid(orgUnit.getUid())
            .orderBy(UID.of("toUpdate000"), SortDirection.ASC)
            .orderBy("enrollment.enrollmentDate", SortDirection.ASC)
            .build();

    List<String> events = getEvents(params);

    assertEquals(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), events);
  }

  @Test
  void shouldOrderEventsRespectingOrderWhenOrderParamSuppliedBeforeAttributeOrder()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        eventParamsBuilder
            .orgUnitUid(orgUnit.getUid())
            .orderBy("enrollment.enrollmentDate", SortDirection.DESC)
            .orderBy(UID.of("toUpdate000"), SortDirection.DESC)
            .build();

    List<String> events = getEvents(params);

    assertEquals(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), events);
  }

  @Test
  void shouldOrderEventsRespectingOrderWhenDataElementSuppliedBeforeOrderParam()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        eventParamsBuilder
            .orgUnitUid(orgUnit.getUid())
            .orderBy("dueDate", SortDirection.DESC)
            .orderBy(UID.of("DATAEL00006"), SortDirection.DESC)
            .orderBy("enrollment.enrollmentDate", SortDirection.DESC)
            .build();

    List<String> events = getEvents(params);

    assertEquals(List.of("pTzf9KYMk72", "D9PbzJY8bJM"), events);
  }

  @Test
  void shouldOrderEventsRespectingOrderWhenOrderParamSuppliedBeforeDataElement()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        eventParamsBuilder
            .orgUnitUid(orgUnit.getUid())
            .orderBy("enrollment.enrollmentDate", SortDirection.DESC)
            .orderBy(UID.of("DATAEL00006"), SortDirection.DESC)
            .build();

    List<String> events = getEvents(params);

    assertEquals(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), events);
  }

  @Test
  void shouldOrderEventsByDataElementAndNotFilterOutEventsWithoutThatDataElement()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        eventParamsBuilder
            .orgUnitUid(orgUnit.getUid())
            .events(
                Set.of(
                    "pTzf9KYMk72", // EV pTzf9KYMk72 without data element DATAEL00002
                    "D9PbzJY8bJM"))
            // notUpdated0
            .orderBy(UID.of("DATAEL00002"), SortDirection.DESC)
            .build();

    List<String> events = getEvents(params);

    // https://www.postgresql.org/docs/current/queries-order.html
    // By default, null values sort as if larger than any non-null value
    // => EV pTzf9KYMk72 without data element DATAEL00002 will come first when DESC
    assertEquals(List.of("pTzf9KYMk72", "D9PbzJY8bJM"), events);
  }

  @Test
  void shouldOrderRelationshipsByPrimaryKeyDescByDefault()
      throws ForbiddenException, NotFoundException {
    Relationship oLT07jKRu9e = get(Relationship.class, "oLT07jKRu9e");
    Relationship yZxjxJli9mO = get(Relationship.class, "yZxjxJli9mO");
    List<String> expected =
        Stream.of(oLT07jKRu9e, yZxjxJli9mO)
            .sorted(Comparator.comparing(Relationship::getId).reversed()) // reversed = desc
            .map(Relationship::getUid)
            .toList();

    RelationshipOperationParams params =
        RelationshipOperationParams.builder()
            .type(TrackerType.EVENT)
            .identifier("pTzf9KYMk72")
            .build();

    List<String> relationships = getRelationships(params);

    assertEquals(expected, relationships);
  }

  @Test
  void shouldReturnPaginatedRelationshipsGivenNonDefaultPageSize()
      throws ForbiddenException, NotFoundException {
    // relationships can only be ordered by created date which is not under our control during
    // testing
    // pagination is tested using default order by primary key desc. We thus need to get the
    // expected order of the pages beforehand.
    Relationship oLT07jKRu9e = get(Relationship.class, "oLT07jKRu9e");
    Relationship yZxjxJli9mO = get(Relationship.class, "yZxjxJli9mO");
    List<String> expected =
        Stream.of(oLT07jKRu9e, yZxjxJli9mO)
            .sorted(Comparator.comparing(Relationship::getId).reversed()) // reversed = desc
            .map(Relationship::getUid)
            .toList();
    String expectedOnPage1 = expected.get(0);
    String expectedOnPage2 = expected.get(1);

    RelationshipOperationParamsBuilder builder =
        RelationshipOperationParams.builder().type(TrackerType.EVENT).identifier("pTzf9KYMk72");

    RelationshipOperationParams params = builder.page(1).pageSize(1).build();

    Relationships firstPage = relationshipService.getRelationships(params);

    assertAll(
        "first page",
        () -> assertSlimPager(1, 1, false, firstPage.getPager()),
        () -> assertEquals(List.of(expectedOnPage1), uids(firstPage.getRelationships())));

    params = builder.page(2).pageSize(1).build();

    Relationships secondPage = relationshipService.getRelationships(params);

    assertAll(
        "second (last) page",
        () -> assertSlimPager(2, 1, true, secondPage.getPager()),
        () -> assertEquals(List.of(expectedOnPage2), uids(secondPage.getRelationships())));

    params = builder.page(3).pageSize(1).build();

    assertIsEmpty(getRelationships(params));
  }

  @Test
  void shouldReturnPaginatedRelationshipsGivenNonDefaultPageSizeAndTotalPages()
      throws ForbiddenException, NotFoundException {
    // relationships can only be ordered by created date which is not under our control during
    // testing
    // pagination is tested using default order by primary key desc. We thus need to get the
    // expected order of the pages beforehand.
    Relationship oLT07jKRu9e = get(Relationship.class, "oLT07jKRu9e");
    Relationship yZxjxJli9mO = get(Relationship.class, "yZxjxJli9mO");
    List<String> expected =
        Stream.of(oLT07jKRu9e, yZxjxJli9mO)
            .sorted(Comparator.comparing(Relationship::getId).reversed()) // reversed = desc
            .map(Relationship::getUid)
            .toList();
    String expectedOnPage1 = expected.get(0);
    String expectedOnPage2 = expected.get(1);

    RelationshipOperationParamsBuilder builder =
        RelationshipOperationParams.builder().type(TrackerType.EVENT).identifier("pTzf9KYMk72");

    RelationshipOperationParams params = builder.page(1).pageSize(1).totalPages(true).build();

    Relationships firstPage = relationshipService.getRelationships(params);

    assertAll(
        "first page",
        () -> assertPager(1, 1, 2, firstPage.getPager()),
        () -> assertEquals(List.of(expectedOnPage1), uids(firstPage.getRelationships())));

    params = builder.page(2).pageSize(1).totalPages(true).build();

    Relationships secondPage = relationshipService.getRelationships(params);

    assertAll(
        "second (last) page",
        () -> assertPager(2, 1, 2, secondPage.getPager()),
        () -> assertEquals(List.of(expectedOnPage2), uids(secondPage.getRelationships())));

    params = builder.page(3).pageSize(1).totalPages(true).build();

    assertIsEmpty(getRelationships(params));
  }

  @Test
  void shouldOrderRelationshipsByCreatedAsc() throws ForbiddenException, NotFoundException {
    Relationship oLT07jKRu9e = get(Relationship.class, "oLT07jKRu9e");
    Relationship yZxjxJli9mO = get(Relationship.class, "yZxjxJli9mO");

    RelationshipOperationParams params =
        RelationshipOperationParams.builder()
            .type(TrackerType.EVENT)
            .identifier("pTzf9KYMk72")
            .orderBy("created", SortDirection.ASC)
            .build();

    List<String> relationships = getRelationships(params);

    boolean isSameCreatedDate = oLT07jKRu9e.getCreated().equals(yZxjxJli9mO.getCreated());
    if (isSameCreatedDate) {
      // the order is non-deterministic if the created date is the same. we can then only assert
      // the correct entities are in the result. otherwise the test is flaky
      assertContainsOnly(List.of("oLT07jKRu9e", "yZxjxJli9mO"), relationships);
    } else {
      List<String> expected =
          Stream.of(oLT07jKRu9e, yZxjxJli9mO)
              .sorted(Comparator.comparing(Relationship::getCreated)) // asc
              .map(Relationship::getUid)
              .toList();
      assertEquals(expected, relationships);
    }
  }

  @Test
  void shouldOrderRelationshipsByCreatedDesc() throws ForbiddenException, NotFoundException {
    Relationship oLT07jKRu9e = get(Relationship.class, "oLT07jKRu9e");
    Relationship yZxjxJli9mO = get(Relationship.class, "yZxjxJli9mO");

    RelationshipOperationParams params =
        RelationshipOperationParams.builder()
            .type(TrackerType.EVENT)
            .identifier("pTzf9KYMk72")
            .orderBy("created", SortDirection.DESC)
            .build();

    List<String> relationships = getRelationships(params);

    boolean isSameCreatedDate = oLT07jKRu9e.getCreated().equals(yZxjxJli9mO.getCreated());
    if (isSameCreatedDate) {
      // the order is non-deterministic if the created date is the same. we can then only assert
      // the correct entities are in the result. otherwise the test is flaky
      assertContainsOnly(List.of("oLT07jKRu9e", "yZxjxJli9mO"), relationships);
    } else {
      List<String> expected =
          Stream.of(oLT07jKRu9e, yZxjxJli9mO)
              .sorted(Comparator.comparing(Relationship::getCreated).reversed()) // reversed = desc
              .map(Relationship::getUid)
              .toList();
      assertEquals(expected, relationships);
    }
  }

  private <T extends IdentifiableObject> T get(Class<T> type, String uid) {
    T t = manager.get(type, uid);
    assertNotNull(
        t,
        () ->
            String.format(
                "'%s' with uid '%s' should have been created", type.getSimpleName(), uid));
    return t;
  }

  private static void assertSlimPager(int pageNumber, int pageSize, boolean isLast, Pager pager) {
    assertInstanceOf(SlimPager.class, pager, "SlimPager should be returned if totalPages=false");
    SlimPager slimPager = (SlimPager) pager;
    assertAll(
        "pagination details",
        () -> assertEquals(pageNumber, slimPager.getPage(), "number of current page"),
        () -> assertEquals(pageSize, slimPager.getPageSize(), "page size"),
        () ->
            assertEquals(
                isLast,
                slimPager.isLastPage(),
                isLast ? "should be the last page" : "should NOT be the last page"));
  }

  private static void assertPager(int pageNumber, int pageSize, int totalCount, Pager pager) {
    assertAll(
        "pagination details",
        () -> assertEquals(pageNumber, pager.getPage(), "number of current page"),
        () -> assertEquals(pageSize, pager.getPageSize(), "page size"),
        () -> assertEquals(totalCount, pager.getTotal(), "total count of items"));
  }

  private List<String> getTrackedEntities(TrackedEntityOperationParams params)
      throws ForbiddenException, BadRequestException, NotFoundException {
    return uids(trackedEntityService.getTrackedEntities(params).getTrackedEntities());
  }

  private List<String> getEnrollments(EnrollmentOperationParams params)
      throws ForbiddenException, BadRequestException {
    return uids(enrollmentService.getEnrollments(params).getEnrollments());
  }

  private List<String> getEvents(EventOperationParams params)
      throws ForbiddenException, BadRequestException {
    return uids(eventService.getEvents(params).getEvents());
  }

  private List<String> getRelationships(RelationshipOperationParams params)
      throws ForbiddenException, NotFoundException {
    return uids(relationshipService.getRelationships(params).getRelationships());
  }

  private static List<String> eventUids(Events events) {
    return uids(events.getEvents());
  }

  private static List<String> uids(List<? extends BaseIdentifiableObject> identifiableObject) {
    return identifiableObject.stream().map(BaseIdentifiableObject::getUid).toList();
  }
}
