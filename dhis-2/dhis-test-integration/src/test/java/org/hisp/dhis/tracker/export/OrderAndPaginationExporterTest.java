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
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.DESCENDANTS;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;
import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.SortDirection;
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
import org.hisp.dhis.tracker.export.enrollment.EnrollmentService;
import org.hisp.dhis.tracker.export.event.EventOperationParams;
import org.hisp.dhis.tracker.export.event.EventOperationParams.EventOperationParamsBuilder;
import org.hisp.dhis.tracker.export.event.EventParams;
import org.hisp.dhis.tracker.export.event.EventService;
import org.hisp.dhis.tracker.export.relationship.RelationshipOperationParams;
import org.hisp.dhis.tracker.export.relationship.RelationshipService;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityOperationParams;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityService;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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

  @BeforeAll
  void setUp() throws IOException {
    setUpMetadata("tracker/simple_metadata.json");

    importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);

    TrackerImportParams params = TrackerImportParams.builder().build();
    assertNoErrors(
        trackerImportService.importTracker(params, fromJson("tracker/event_and_enrollment.json")));
    orgUnit = get(OrganisationUnit.class, "h4w96yEMlzO");
    programStage = get(ProgramStage.class, "NpsdDv6kKSO");
    trackedEntityType = get(TrackedEntityType.class, "ja8NY4PW7Xm");

    manager.flush();
    manager.clear();
  }

  @BeforeEach
  void setUpUserAndParams() {
    // needed as some tests are run using another user (injectSecurityContext) while most tests
    // expect to be run by the importUser
    injectSecurityContextUser(importUser);

    eventParamsBuilder = EventOperationParams.builder().eventParams(EventParams.FALSE);
    eventParamsBuilder.orgUnitMode(SELECTED);
  }

  @Test
  void shouldReturnPaginatedTrackedEntitiesGivenNonDefaultPageSize()
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnit.getUid()))
            .orgUnitMode(DESCENDANTS)
            .trackedEntityTypeUid(trackedEntityType.getUid())
            .orderBy(UID.of("numericAttr"), SortDirection.ASC)
            .build();

    Page<TrackedEntity> firstPage =
        trackedEntityService.getTrackedEntities(params, new PageParams(1, 3, false));

    assertAll(
        "first page",
        () -> assertPage(1, 3, firstPage),
        () ->
            assertEquals(
                List.of("dUE514NMOlo", "mHWCacsGYYn", "QS6w44flWAf"), uids(firstPage.getItems())));

    Page<TrackedEntity> secondPage =
        trackedEntityService.getTrackedEntities(params, new PageParams(2, 3, false));

    assertAll(
        "second (last) page",
        () -> assertPage(2, 3, secondPage),
        () ->
            assertEquals(
                List.of("QesgJkTyTCk", "woitxQbWYNq", "guVNoAerxWo"), uids(secondPage.getItems())));

    assertIsEmpty(
        trackedEntityService.getTrackedEntities(params, new PageParams(3, 3, false)).getItems());
  }

  @Test
  void shouldReturnPaginatedTrackedEntitiesGivenNonDefaultPageSizeAndTotalPages()
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnit.getUid()))
            .orgUnitMode(DESCENDANTS)
            .trackedEntityTypeUid(trackedEntityType.getUid())
            .orderBy(UID.of("numericAttr"), SortDirection.ASC)
            .build();

    Page<TrackedEntity> firstPage =
        trackedEntityService.getTrackedEntities(params, new PageParams(1, 3, true));

    assertAll(
        "first page",
        () -> assertPage(1, 3, 6, firstPage),
        () ->
            assertEquals(
                List.of("dUE514NMOlo", "mHWCacsGYYn", "QS6w44flWAf"), uids(firstPage.getItems())));

    Page<TrackedEntity> secondPage =
        trackedEntityService.getTrackedEntities(params, new PageParams(2, 3, true));

    assertAll(
        "second (last) page",
        () -> assertPage(2, 3, 6, secondPage),
        () ->
            assertEquals(
                List.of("QesgJkTyTCk", "woitxQbWYNq", "guVNoAerxWo"), uids(secondPage.getItems())));

    assertIsEmpty(
        trackedEntityService.getTrackedEntities(params, new PageParams(3, 3, true)).getItems());
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
            .orgUnitMode(SELECTED)
            .trackedEntityUids(Set.of("QS6w44flWAf", "dUE514NMOlo"))
            .trackedEntityTypeUid(trackedEntityType.getUid())
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
            .orgUnitMode(SELECTED)
            .trackedEntityUids(Set.of("QS6w44flWAf", "dUE514NMOlo"))
            .trackedEntityTypeUid(trackedEntityType.getUid())
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
            .orgUnitMode(SELECTED)
            .trackedEntityUids(Set.of("QS6w44flWAf", "dUE514NMOlo"))
            .trackedEntityTypeUid(trackedEntityType.getUid())
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
            .orgUnitMode(SELECTED)
            .trackedEntityUids(Set.of("QS6w44flWAf", "dUE514NMOlo"))
            .trackedEntityTypeUid(trackedEntityType.getUid())
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
            .orgUnitMode(SELECTED)
            .trackedEntityUids(Set.of("QS6w44flWAf", "dUE514NMOlo"))
            .trackedEntityTypeUid(trackedEntityType.getUid())
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
            .orgUnitMode(SELECTED)
            .trackedEntityUids(Set.of("QS6w44flWAf", "dUE514NMOlo"))
            .trackedEntityTypeUid(trackedEntityType.getUid())
            .orderBy("enrollment.enrollmentDate", SortDirection.ASC)
            .build();

    List<String> trackedEntities = getTrackedEntities(params);

    assertEquals(List.of("QS6w44flWAf", "dUE514NMOlo"), trackedEntities);
  }

  @Test
  void shouldOrderTrackedEntitiesByEnrolledAtDescWithNoProgramInParams()
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnit.getUid()))
            .orgUnitMode(SELECTED)
            .trackedEntityUids(Set.of("QS6w44flWAf", "dUE514NMOlo"))
            .trackedEntityTypeUid(trackedEntityType.getUid())
            .orderBy("enrollment.enrollmentDate", SortDirection.DESC)
            .build();

    List<String> trackedEntities = getTrackedEntities(params);

    assertEquals(
        List.of("QS6w44flWAf", "dUE514NMOlo"),
        trackedEntities); // QS6w44flWAf has 2 enrollments, one of which has an enrollment with
    // enrolled date greater than the enrollment in dUE514NMOlo
  }

  @Test
  void shouldOrderTrackedEntitiesByEnrolledAtDescWithProgramInParams()
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnit.getUid()))
            .orgUnitMode(SELECTED)
            .trackedEntityUids(Set.of("QS6w44flWAf", "dUE514NMOlo"))
            .trackedEntityTypeUid(trackedEntityType.getUid())
            .programUid("BFcipDERJnf")
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
            .orgUnitMode(SELECTED)
            .trackedEntityUids(Set.of("QS6w44flWAf", "dUE514NMOlo"))
            .trackedEntityTypeUid(trackedEntityType.getUid())
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
            .orgUnitMode(SELECTED)
            .trackedEntityUids(Set.of("QS6w44flWAf", "dUE514NMOlo"))
            .trackedEntityTypeUid(trackedEntityType.getUid())
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
            .orgUnitMode(SELECTED)
            .trackedEntityUids(Set.of("QS6w44flWAf", "dUE514NMOlo"))
            .trackedEntityTypeUid(trackedEntityType.getUid())
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
            .orgUnitMode(SELECTED)
            .trackedEntityTypeUid(trackedEntityType.getUid())
            .orderBy(UID.of("toUpdate000"), SortDirection.ASC)
            .filters(Map.of("numericAttr", List.of(new QueryFilter(QueryOperator.LT, "75"))))
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
            .orgUnitMode(SELECTED)
            .trackedEntityTypeUid(trackedEntityType.getUid())
            .filters(Map.of("numericAttr", List.of(new QueryFilter(QueryOperator.LT, "75"))))
            .orderBy(UID.of("numericAttr"), SortDirection.DESC)
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
            .orgUnitMode(SELECTED)
            .trackedEntityTypeUid(trackedEntityType.getUid())
            .trackedEntityUids(
                Set.of(
                    "dUE514NMOlo", "QS6w44flWAf")) // TE QS6w44flWAf without attribute notUpdated0
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
            .orgUnitMode(SELECTED)
            .trackedEntityUids(Set.of("QS6w44flWAf", "dUE514NMOlo"))
            .trackedEntityTypeUid(trackedEntityType.getUid())
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
            .orgUnitMode(SELECTED)
            .trackedEntityUids(Set.of("QS6w44flWAf", "dUE514NMOlo"))
            .trackedEntityTypeUid(trackedEntityType.getUid())
            .orderBy(UID.of("toDelete000"), SortDirection.DESC)
            .orderBy(UID.of("numericAttr"), SortDirection.DESC)
            .build();

    List<String> trackedEntities = getTrackedEntities(params);

    assertEquals(List.of("QS6w44flWAf", "dUE514NMOlo"), trackedEntities);
  }

  @Test
  void shouldOrderTrackedEntitiesByInactiveDesc()
      throws ForbiddenException, BadRequestException, NotFoundException {

    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnit.getUid()))
            .orgUnitMode(SELECTED)
            .trackedEntityUids(Set.of("QS6w44flWAf", "dUE514NMOlo"))
            .trackedEntityTypeUid(trackedEntityType.getUid())
            .orderBy("inactive", SortDirection.DESC)
            .build();

    List<String> trackedEntities = getTrackedEntities(params);

    assertEquals(List.of("QS6w44flWAf", "dUE514NMOlo"), trackedEntities);
  }

  @Test
  void shouldOrderTrackedEntitiesByInactiveAsc()
      throws ForbiddenException, BadRequestException, NotFoundException {

    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of(orgUnit.getUid()))
            .orgUnitMode(SELECTED)
            .trackedEntityUids(Set.of("QS6w44flWAf", "dUE514NMOlo"))
            .trackedEntityTypeUid(trackedEntityType.getUid())
            .orderBy("inactive", SortDirection.ASC)
            .build();

    List<String> trackedEntities = getTrackedEntities(params);

    assertEquals(List.of("dUE514NMOlo", "QS6w44flWAf"), trackedEntities);
  }

  @Test
  void shouldReturnPaginatedEnrollmentsGivenNonDefaultPageSize()
      throws ForbiddenException, BadRequestException, NotFoundException {
    EnrollmentOperationParams operationParams =
        EnrollmentOperationParams.builder()
            .orgUnits(Set.of(UID.of(orgUnit)))
            .orgUnitMode(SELECTED)
            .orderBy("enrollmentDate", SortDirection.ASC)
            .build();

    Page<Enrollment> firstPage =
        enrollmentService.getEnrollments(operationParams, new PageParams(1, 1, false));

    assertAll(
        "first page",
        () -> assertPage(1, 1, firstPage),
        () -> assertEquals(List.of("nxP7UnKhomJ"), uids(firstPage.getItems())));

    Page<Enrollment> secondPage =
        enrollmentService.getEnrollments(operationParams, new PageParams(2, 1, false));

    assertAll(
        "second page is last page",
        () -> assertPage(2, 1, secondPage),
        () -> assertEquals(List.of("TvctPPhpD8z"), uids(secondPage.getItems())));

    Page<Enrollment> thirdPage =
        enrollmentService.getEnrollments(operationParams, new PageParams(3, 1, false));

    assertIsEmpty(thirdPage.getItems());
  }

  @Test
  void shouldReturnPaginatedEnrollmentsGivenNonDefaultPageSizeAndTotalPages()
      throws ForbiddenException, BadRequestException, NotFoundException {
    EnrollmentOperationParams operationParams =
        EnrollmentOperationParams.builder()
            .orgUnits(Set.of(UID.of(orgUnit)))
            .orgUnitMode(SELECTED)
            .orderBy("enrollmentDate", SortDirection.ASC)
            .build();

    Page<Enrollment> firstPage =
        enrollmentService.getEnrollments(operationParams, new PageParams(1, 1, true));

    assertAll(
        "first page",
        () -> assertPage(1, 1, 2, firstPage),
        () -> assertEquals(List.of("nxP7UnKhomJ"), uids(firstPage.getItems())));

    Page<Enrollment> secondPage =
        enrollmentService.getEnrollments(operationParams, new PageParams(2, 1, true));

    assertAll(
        "second (last) page",
        () -> assertPage(2, 1, 2, secondPage),
        () -> assertEquals(List.of("TvctPPhpD8z"), uids(secondPage.getItems())));

    Page<Enrollment> thirdPage =
        enrollmentService.getEnrollments(operationParams, new PageParams(3, 1, true));

    assertIsEmpty(thirdPage.getItems());
  }

  @Test
  void shouldOrderEnrollmentsByPrimaryKeyDescByDefault()
      throws ForbiddenException, BadRequestException, NotFoundException {
    Enrollment nxP7UnKhomJ = get(Enrollment.class, "nxP7UnKhomJ");
    Enrollment TvctPPhpD8z = get(Enrollment.class, "TvctPPhpD8z");
    List<String> expected =
        Stream.of(nxP7UnKhomJ, TvctPPhpD8z)
            .sorted(Comparator.comparing(Enrollment::getId).reversed()) // reversed = desc
            .map(Enrollment::getUid)
            .toList();

    EnrollmentOperationParams params =
        EnrollmentOperationParams.builder()
            .orgUnits(Set.of(UID.of(orgUnit)))
            .orgUnitMode(SELECTED)
            .build();

    List<String> enrollments = getEnrollments(params);

    assertEquals(expected, enrollments);
  }

  @Test
  void shouldOrderEnrollmentsByEnrolledAtAsc()
      throws ForbiddenException, BadRequestException, NotFoundException {
    EnrollmentOperationParams params =
        EnrollmentOperationParams.builder()
            .orgUnits(Set.of(UID.of(orgUnit)))
            .orgUnitMode(SELECTED)
            .orderBy("enrollmentDate", SortDirection.ASC)
            .build();

    List<String> enrollments = getEnrollments(params);

    assertEquals(List.of("nxP7UnKhomJ", "TvctPPhpD8z"), enrollments);
  }

  @Test
  void shouldOrderEnrollmentsByEnrolledAtDesc()
      throws ForbiddenException, BadRequestException, NotFoundException {
    EnrollmentOperationParams params =
        EnrollmentOperationParams.builder()
            .orgUnits(Set.of(UID.of(orgUnit)))
            .orgUnitMode(SELECTED)
            .orderBy("enrollmentDate", SortDirection.DESC)
            .build();

    List<String> enrollments = getEnrollments(params);

    assertEquals(List.of("TvctPPhpD8z", "nxP7UnKhomJ"), enrollments);
  }

  @Test
  void shouldReturnPaginatedEventsWithNotesGivenNonDefaultPageSize()
      throws ForbiddenException, BadRequestException, NotFoundException {
    EventOperationParams operationParams =
        eventParamsBuilder
            .orgUnitUid(orgUnit.getUid())
            .events(Set.of("pTzf9KYMk72", "D9PbzJY8bJM"))
            .orderBy("occurredDate", SortDirection.DESC)
            .build();

    Page<Event> firstPage = eventService.getEvents(operationParams, new PageParams(1, 1, false));

    assertAll(
        "first page",
        () -> assertPage(1, 1, firstPage),
        () -> assertEquals(List.of("D9PbzJY8bJM"), uids(firstPage)));

    Page<Event> secondPage = eventService.getEvents(operationParams, new PageParams(2, 1, false));

    assertAll(
        "second page is the last page",
        () -> assertPage(2, 1, secondPage),
        () -> assertEquals(List.of("pTzf9KYMk72"), uids(secondPage)));

    assertIsEmpty(getEvents(operationParams, new PageParams(3, 3, false)));
  }

  @Test
  void shouldReturnPaginatedEventsWithTotalPages()
      throws ForbiddenException, BadRequestException, NotFoundException {
    EventOperationParams params =
        eventParamsBuilder
            .orgUnitUid(orgUnit.getUid())
            .programStageUid(programStage.getUid())
            .build();

    Page<Event> page = eventService.getEvents(params, new PageParams(1, 2, true));

    assertAll(
        "page with total counts",
        () -> assertPage(1, 2, 2, page),
        () -> assertEquals(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), uids(page)));
  }

  @Test
  void shouldReturnPaginatedPublicEventsWithMultipleCategoryOptionsGivenNonDefaultPageSize()
      throws ForbiddenException, BadRequestException, NotFoundException {
    OrganisationUnit orgUnit = get(OrganisationUnit.class, "DiszpKrYNg8");
    Program program = get(Program.class, "iS7eutanDry");

    EventOperationParams operationParams =
        eventParamsBuilder
            .orgUnitUid(orgUnit.getUid())
            .programUid(program.getUid())
            .orderBy("occurredDate", SortDirection.DESC)
            .build();

    Page<Event> firstPage = eventService.getEvents(operationParams, new PageParams(1, 3, false));

    assertAll(
        "first page",
        () -> assertPage(1, 3, firstPage),
        () -> assertEquals(List.of("ck7DzdxqLqA", "OTmjvJDn0Fu", "kWjSezkXHVp"), uids(firstPage)));

    Page<Event> secondPage = eventService.getEvents(operationParams, new PageParams(2, 3, false));

    assertAll(
        "second page is the last page",
        () -> assertPage(2, 3, secondPage),
        () -> assertEquals(List.of("lumVtWwwy0O", "QRYjLTiJTrA", "cadc5eGj0j7"), uids(secondPage)));

    assertIsEmpty(getEvents(operationParams, new PageParams(3, 3, false)));
  }

  @Test
  void shouldReturnPaginatedEventsWithMultipleCategoryOptionsGivenNonDefaultPageSizeAndTotalPages()
      throws ForbiddenException, BadRequestException, NotFoundException {
    OrganisationUnit orgUnit = get(OrganisationUnit.class, "DiszpKrYNg8");
    Program program = get(Program.class, "iS7eutanDry");

    EventOperationParams params =
        eventParamsBuilder
            .orgUnitUid(orgUnit.getUid())
            .programUid(program.getUid())
            .orderBy("occurredDate", SortDirection.DESC)
            .build();

    Page<Event> events = eventService.getEvents(params, new PageParams(1, 2, true));

    assertAll(
        "page with total counts",
        () -> assertPage(1, 2, 6, events),
        () -> assertEquals(List.of("ck7DzdxqLqA", "OTmjvJDn0Fu"), uids(events)));
  }

  @Test
  void shouldOrderEventsByPrimaryKeyDescByDefault()
      throws ForbiddenException, BadRequestException, NotFoundException {
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
  void shouldOrderEventsByEnrollmentProgramUIDAsc()
      throws ForbiddenException, BadRequestException, NotFoundException {
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
        eventParamsBuilder
            .orgUnitMode(ACCESSIBLE)
            .events(Set.of("pTzf9KYMk72", "QRYjLTiJTrA"))
            .orderBy("enrollment.program.uid", SortDirection.ASC)
            .build();

    List<String> events = getEvents(params);

    assertEquals(expected, events);
  }

  @Test
  void shouldOrderEventsByEnrollmentProgramUIDDesc()
      throws ForbiddenException, BadRequestException, NotFoundException {
    Event pTzf9KYMk72 =
        get(Event.class, "pTzf9KYMk72"); // enrolled in program BFcipDERJnf with registration
    Event QRYjLTiJTrA =
        get(Event.class, "QRYjLTiJTrA"); // enrolled in program BFcipDERJne without registration
    List<String> expected =
        new java.util.ArrayList<>(
            Stream.of(pTzf9KYMk72, QRYjLTiJTrA)
                .sorted(Comparator.comparing(event -> event.getEnrollment().getProgram().getUid()))
                .map(Event::getUid)
                .toList());
    Collections.reverse(expected);

    EventOperationParams params =
        eventParamsBuilder
            .orgUnitMode(ACCESSIBLE)
            .events(Set.of("pTzf9KYMk72", "QRYjLTiJTrA"))
            .orderBy("enrollment.program.uid", SortDirection.DESC)
            .build();

    List<String> events = getEvents(params);

    assertEquals(expected, events);
  }

  @Test
  void shouldOrderEventsByAttributeAsc()
      throws ForbiddenException, BadRequestException, NotFoundException {
    EventOperationParams params =
        eventParamsBuilder
            .orgUnitUid(orgUnit.getUid())
            .orderBy(UID.of("toUpdate000"), SortDirection.ASC)
            .build();

    List<String> events = getEvents(params);

    assertEquals(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), events);
  }

  @Test
  void shouldOrderEventsByAttributeDesc()
      throws ForbiddenException, BadRequestException, NotFoundException {
    EventOperationParams params =
        eventParamsBuilder
            .orgUnitUid(orgUnit.getUid())
            .orderBy(UID.of("toUpdate000"), SortDirection.DESC)
            .build();

    List<String> events = getEvents(params);

    assertEquals(List.of("pTzf9KYMk72", "D9PbzJY8bJM"), events);
  }

  @Test
  void shouldOrderEventsByAttributeAndNotFilterOutEventsWithATrackedEntityWithoutThatAttribute()
      throws ForbiddenException, BadRequestException, NotFoundException {
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

    assertEquals(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), events);
  }

  @Test
  void shouldOrderEventsByMultipleAttributesDesc()
      throws ForbiddenException, BadRequestException, NotFoundException {
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
  void shouldOrderEventsByMultipleAttributesAsc()
      throws ForbiddenException, BadRequestException, NotFoundException {
    EventOperationParams params =
        eventParamsBuilder
            .orgUnitUid(orgUnit.getUid())
            .orderBy(UID.of("toDelete000"), SortDirection.DESC)
            .orderBy(UID.of("toUpdate000"), SortDirection.ASC)
            .build();

    List<Event> events = eventService.getEvents(params);

    assertEquals(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), uids(events));
    List<String> trackedEntities =
        events.stream()
            .map(event -> event.getEnrollment().getTrackedEntity().getUid())
            .collect(Collectors.toList());
    assertEquals(List.of("dUE514NMOlo", "QS6w44flWAf"), trackedEntities);
  }

  @Test
  void shouldReturnPaginatedEventsOrderedByMultipleAttributesWhenGivenNonDefaultPageSize()
      throws ForbiddenException, BadRequestException, NotFoundException {
    EventOperationParams operationParams =
        eventParamsBuilder
            .orgUnitUid(orgUnit.getUid())
            .orderBy(UID.of("toDelete000"), SortDirection.DESC)
            .orderBy(UID.of("toUpdate000"), SortDirection.ASC)
            .build();

    Page<Event> firstPage = eventService.getEvents(operationParams, new PageParams(1, 1, false));

    assertAll(
        "first page",
        () -> assertPage(1, 1, firstPage),
        () -> assertEquals(List.of("D9PbzJY8bJM"), uids(firstPage)));

    Page<Event> secondPage = eventService.getEvents(operationParams, new PageParams(2, 1, false));

    assertAll(
        "second (last) page",
        () -> assertPage(2, 1, secondPage),
        () -> assertEquals(List.of("pTzf9KYMk72"), uids(secondPage)));

    assertIsEmpty(getEvents(operationParams, new PageParams(3, 3, false)));
  }

  @Test
  void shouldOrderEventsByProgramStageUidDesc()
      throws ForbiddenException, BadRequestException, NotFoundException {
    EventOperationParams params =
        eventParamsBuilder
            .orgUnitUid("uoNW0E3xXUy")
            .orderBy("programStage.uid", SortDirection.DESC)
            .build();

    List<String> events = getEvents(params);

    assertEquals(List.of("JaRDIvcEcEx", "jxgFyJEMUPf"), events);
  }

  @Test
  void shouldOrderEventsByProgramStageUidAsc()
      throws ForbiddenException, BadRequestException, NotFoundException {
    EventOperationParams params =
        eventParamsBuilder
            .orgUnitUid("uoNW0E3xXUy")
            .orderBy("programStage.uid", SortDirection.ASC)
            .build();

    List<String> events = getEvents(params);

    assertEquals(List.of("jxgFyJEMUPf", "JaRDIvcEcEx"), events);
  }

  @Test
  void shouldOrderEventsByTrackedEntityUidDesc()
      throws ForbiddenException, BadRequestException, NotFoundException {
    EventOperationParams params =
        eventParamsBuilder
            .orgUnitUid(orgUnit.getUid())
            .orderBy("enrollment.trackedEntity.uid", SortDirection.DESC)
            .build();

    List<String> events = getEvents(params);

    assertEquals(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), events);
  }

  @Test
  void shouldOrderEventsByTrackedEntityUidAsc()
      throws ForbiddenException, BadRequestException, NotFoundException {
    EventOperationParams params =
        eventParamsBuilder
            .orgUnitUid(orgUnit.getUid())
            .orderBy("enrollment.trackedEntity.uid", SortDirection.ASC)
            .build();

    List<String> events = getEvents(params);

    assertEquals(List.of("pTzf9KYMk72", "D9PbzJY8bJM"), events);
  }

  @Test
  void shouldOrderEventsByAttributeOptionComboUidDesc()
      throws ForbiddenException, BadRequestException, NotFoundException {
    EventOperationParams params =
        eventParamsBuilder
            .orgUnitUid("DiszpKrYNg8")
            .events(Set.of("ck7DzdxqLqA", "lumVtWwwy0O", "cadc5eGj0j7"))
            .orderBy("attributeOptionCombo.uid", SortDirection.DESC)
            .build();

    List<String> events = getEvents(params);

    assertEquals(List.of("ck7DzdxqLqA", "cadc5eGj0j7", "lumVtWwwy0O"), events);
  }

  @Test
  void shouldOrderEventsByAttributeOptionComboUidAsc()
      throws ForbiddenException, BadRequestException, NotFoundException {
    EventOperationParams params =
        eventParamsBuilder
            .orgUnitUid("DiszpKrYNg8")
            .events(Set.of("ck7DzdxqLqA", "lumVtWwwy0O", "cadc5eGj0j7"))
            .orderBy("attributeOptionCombo.uid", SortDirection.ASC)
            .build();

    List<String> events = getEvents(params);

    assertEquals(List.of("lumVtWwwy0O", "cadc5eGj0j7", "ck7DzdxqLqA"), events);
  }

  @Test
  void shouldOrderEventsByOccurredAtDesc()
      throws ForbiddenException, BadRequestException, NotFoundException {
    EventOperationParams params =
        eventParamsBuilder
            .orgUnitUid(orgUnit.getUid())
            .orderBy("occurredDate", SortDirection.DESC)
            .build();

    List<String> events = getEvents(params);

    assertEquals(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), events);
  }

  @Test
  void shouldOrderEventsByOccurredAtAsc()
      throws ForbiddenException, BadRequestException, NotFoundException {
    EventOperationParams params =
        eventParamsBuilder
            .orgUnitUid(orgUnit.getUid())
            .orderBy("occurredDate", SortDirection.ASC)
            .build();

    List<String> events = getEvents(params);

    assertEquals(List.of("pTzf9KYMk72", "D9PbzJY8bJM"), events);
  }

  @Test
  void shouldOrderEventsByCreatedAtClientInAscOrder()
      throws ForbiddenException, BadRequestException, NotFoundException {
    EventOperationParams params =
        eventParamsBuilder
            .orgUnitUid(orgUnit.getUid())
            .programStageUid(programStage.getUid())
            .orderBy("createdAtClient", SortDirection.ASC)
            .build();

    List<String> events = getEvents(params);

    assertEquals(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), events);
  }

  @Test
  void shouldOrderEventsByCreatedAtClientInDescOrder()
      throws ForbiddenException, BadRequestException, NotFoundException {
    EventOperationParams params =
        eventParamsBuilder
            .orgUnitUid(orgUnit.getUid())
            .programStageUid(programStage.getUid())
            .orderBy("createdAtClient", SortDirection.DESC)
            .build();

    List<String> events = getEvents(params);

    assertEquals(List.of("pTzf9KYMk72", "D9PbzJY8bJM"), events);
  }

  @Test
  void shouldOrderEventsByUpdatedAtClientInAscOrder()
      throws ForbiddenException, BadRequestException, NotFoundException {
    EventOperationParams params =
        eventParamsBuilder
            .orgUnitUid(orgUnit.getUid())
            .programStageUid(programStage.getUid())
            .orderBy("lastUpdatedAtClient", SortDirection.ASC)
            .build();

    List<String> events = getEvents(params);

    assertEquals(List.of("pTzf9KYMk72", "D9PbzJY8bJM"), events);
  }

  @Test
  void shouldOrderEventsByUpdatedAtClientInDescOrder()
      throws ForbiddenException, BadRequestException, NotFoundException {
    EventOperationParams params =
        eventParamsBuilder
            .orgUnitUid(orgUnit.getUid())
            .programStageUid(programStage.getUid())
            .orderBy("lastUpdatedAtClient", SortDirection.DESC)
            .build();

    List<String> events = getEvents(params);

    assertEquals(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), events);
  }

  @Test
  void shouldOrderEventsRespectingOrderWhenAttributeOrderSuppliedBeforeOrderParam()
      throws ForbiddenException, BadRequestException, NotFoundException {
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
      throws ForbiddenException, BadRequestException, NotFoundException {
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
      throws ForbiddenException, BadRequestException, NotFoundException {
    EventOperationParams params =
        eventParamsBuilder
            .orgUnitUid(orgUnit.getUid())
            .orderBy("scheduledDate", SortDirection.DESC)
            .orderBy(UID.of("DATAEL00006"), SortDirection.DESC)
            .orderBy("enrollment.enrollmentDate", SortDirection.DESC)
            .build();

    List<String> events = getEvents(params);

    assertEquals(List.of("pTzf9KYMk72", "D9PbzJY8bJM"), events);
  }

  @Test
  void shouldOrderEventsRespectingOrderWhenOrderParamSuppliedBeforeDataElement()
      throws ForbiddenException, BadRequestException, NotFoundException {
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
      throws ForbiddenException, BadRequestException, NotFoundException {
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

  private static Stream<Arguments> orderByFieldInDescendingOrderWhenModeSelected() {
    return Stream.of(
        Arguments.of("enrollment.uid", "pTzf9KYMk72", "D9PbzJY8bJM"),
        Arguments.of("enrollment.status", "D9PbzJY8bJM", "pTzf9KYMk72"),
        Arguments.of("uid", "pTzf9KYMk72", "D9PbzJY8bJM"),
        Arguments.of("enrollment.enrollmentDate", "D9PbzJY8bJM", "pTzf9KYMk72"));
  }

  private static Stream<Arguments> orderByFieldInAscendingOrderWhenModeSelected() {
    return Stream.of(
        Arguments.of("enrollment.uid", "D9PbzJY8bJM", "pTzf9KYMk72"),
        Arguments.of("enrollment.status", "pTzf9KYMk72", "D9PbzJY8bJM"),
        Arguments.of("uid", "D9PbzJY8bJM", "pTzf9KYMk72"),
        Arguments.of("enrollment.enrollmentDate", "pTzf9KYMk72", "D9PbzJY8bJM"));
  }

  @ParameterizedTest
  @MethodSource("orderByFieldInDescendingOrderWhenModeSelected")
  void shouldOrderEventsByFieldInDescendingOrderWhenModeSelected(
      String field, String firstEvent, String secondEvent)
      throws ForbiddenException, BadRequestException, NotFoundException {

    eventParamsBuilder.orgUnitMode(SELECTED);
    eventParamsBuilder.orgUnitUid(orgUnit.getUid());
    eventParamsBuilder.orderBy(field, SortDirection.DESC);

    List<String> events = getEvents(eventParamsBuilder.build());
    assertEquals(List.of(firstEvent, secondEvent), events);
  }

  @ParameterizedTest
  @MethodSource("orderByFieldInAscendingOrderWhenModeSelected")
  void shouldOrderEventsByFieldInAscendingOrderWhenModeSelected(
      String field, String firstEvent, String secondEvent)
      throws ForbiddenException, BadRequestException, NotFoundException {

    eventParamsBuilder.orgUnitMode(SELECTED);
    eventParamsBuilder.orgUnitUid(orgUnit.getUid());
    eventParamsBuilder.orderBy(field, SortDirection.ASC);

    List<String> events = getEvents(eventParamsBuilder.build());
    assertEquals(List.of(firstEvent, secondEvent), events);
  }

  private static Stream<Arguments> orderByFieldInDescendingOrderWhenModeDescendants() {
    return Stream.of(
        Arguments.of("organisationUnit.uid", "gvULMgNiAfM", "SbUJzkxKYAG"),
        Arguments.of("programStage.uid", "SbUJzkxKYAG", "gvULMgNiAfM"),
        Arguments.of("scheduledDate", "gvULMgNiAfM", "SbUJzkxKYAG"),
        Arguments.of("status", "gvULMgNiAfM", "SbUJzkxKYAG"),
        Arguments.of("storedBy", "SbUJzkxKYAG", "gvULMgNiAfM"));
  }

  private static Stream<Arguments> orderByFieldInAscendingOrderWhenModeDescendants() {
    return Stream.of(
        Arguments.of("organisationUnit.uid", "SbUJzkxKYAG", "gvULMgNiAfM"),
        Arguments.of("programStage.uid", "gvULMgNiAfM", "SbUJzkxKYAG"),
        Arguments.of("scheduledDate", "SbUJzkxKYAG", "gvULMgNiAfM"),
        Arguments.of("status", "SbUJzkxKYAG", "gvULMgNiAfM"),
        Arguments.of("storedBy", "gvULMgNiAfM", "SbUJzkxKYAG"));
  }

  @ParameterizedTest
  @MethodSource("orderByFieldInDescendingOrderWhenModeDescendants")
  void shouldOrderByFieldInDescendingOrderWhenModeDescendants(
      String field, String firstEvent, String secondEvent)
      throws ForbiddenException, BadRequestException, NotFoundException {

    eventParamsBuilder.orgUnitMode(DESCENDANTS);
    eventParamsBuilder.orgUnitUid("RojfDTBhoGC");
    eventParamsBuilder.orderBy(field, SortDirection.DESC);

    List<String> events = getEvents(eventParamsBuilder.build());
    assertEquals(List.of(firstEvent, secondEvent), events);
  }

  @ParameterizedTest
  @MethodSource("orderByFieldInAscendingOrderWhenModeDescendants")
  void shouldOrderByFieldInAscendingOrderWhenModeDescendants(
      String field, String firstEvent, String secondEvent)
      throws ForbiddenException, BadRequestException, NotFoundException {

    eventParamsBuilder.orgUnitMode(DESCENDANTS);
    eventParamsBuilder.orgUnitUid("RojfDTBhoGC");
    eventParamsBuilder.orderBy(field, SortDirection.ASC);

    List<String> events = getEvents(eventParamsBuilder.build());
    assertEquals(List.of(firstEvent, secondEvent), events);
  }

  @Test
  void shouldOrderRelationshipsByPrimaryKeyDescByDefault()
      throws ForbiddenException, NotFoundException, BadRequestException {
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
            .identifier(UID.of("pTzf9KYMk72"))
            .build();

    List<String> relationships = getRelationships(params);

    assertEquals(expected, relationships);
  }

  @Test
  void shouldOrderRelationshipsByUpdatedAtClientInDescOrder()
      throws ForbiddenException, NotFoundException, BadRequestException {
    RelationshipOperationParams params =
        RelationshipOperationParams.builder()
            .type(TrackerType.EVENT)
            .identifier(UID.of("pTzf9KYMk72"))
            .orderBy("createdAtClient", SortDirection.DESC)
            .build();

    List<String> relationships = getRelationships(params);

    assertEquals(List.of("yZxjxJli9mO", "oLT07jKRu9e"), relationships);
  }

  @Test
  void shouldOrderRelationshipsByUpdatedAtClientInAscOrder()
      throws ForbiddenException, NotFoundException, BadRequestException {
    RelationshipOperationParams params =
        RelationshipOperationParams.builder()
            .type(TrackerType.EVENT)
            .identifier(UID.of("pTzf9KYMk72"))
            .orderBy("createdAtClient", SortDirection.ASC)
            .build();

    List<String> relationships = getRelationships(params);

    assertEquals(List.of("oLT07jKRu9e", "yZxjxJli9mO"), relationships);
  }

  @Test
  void shouldReturnPaginatedRelationshipsGivenNonDefaultPageSize()
      throws ForbiddenException, NotFoundException, BadRequestException {
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

    RelationshipOperationParams params =
        RelationshipOperationParams.builder()
            .type(TrackerType.EVENT)
            .identifier(UID.of("pTzf9KYMk72"))
            .build();

    Page<Relationship> firstPage =
        relationshipService.getRelationships(params, new PageParams(1, 1, false));

    assertAll(
        "first page",
        () -> assertPage(1, 1, firstPage),
        () -> assertEquals(List.of(expectedOnPage1), uids(firstPage)));

    Page<Relationship> secondPage =
        relationshipService.getRelationships(params, new PageParams(2, 1, false));

    assertAll(
        "second (last) page",
        () -> assertPage(2, 1, secondPage),
        () -> assertEquals(List.of(expectedOnPage2), uids(secondPage)));

    Page<Relationship> thirdPage =
        relationshipService.getRelationships(params, new PageParams(3, 1, false));

    assertIsEmpty(thirdPage.getItems());
  }

  @Test
  void shouldReturnPaginatedRelationshipsGivenNonDefaultPageSizeAndTotalPages()
      throws ForbiddenException, NotFoundException, BadRequestException {
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

    RelationshipOperationParams params =
        RelationshipOperationParams.builder()
            .type(TrackerType.EVENT)
            .identifier(UID.of("pTzf9KYMk72"))
            .build();

    Page<Relationship> firstPage =
        relationshipService.getRelationships(params, new PageParams(1, 1, true));

    assertAll(
        "first page",
        () -> assertPage(1, 1, 2, firstPage),
        () -> assertEquals(List.of(expectedOnPage1), uids(firstPage)));

    Page<Relationship> secondPage =
        relationshipService.getRelationships(params, new PageParams(2, 1, true));

    assertAll(
        "second (last) page",
        () -> assertPage(2, 1, 2, secondPage),
        () -> assertEquals(List.of(expectedOnPage2), uids(secondPage)));

    Page<Relationship> thirdPage =
        relationshipService.getRelationships(params, new PageParams(3, 1, true));

    assertIsEmpty(thirdPage.getItems());
  }

  @Test
  void shouldOrderRelationshipsByCreatedAsc()
      throws ForbiddenException, NotFoundException, BadRequestException {
    Relationship oLT07jKRu9e = get(Relationship.class, "oLT07jKRu9e");
    Relationship yZxjxJli9mO = get(Relationship.class, "yZxjxJli9mO");

    RelationshipOperationParams params =
        RelationshipOperationParams.builder()
            .type(TrackerType.EVENT)
            .identifier(UID.of("pTzf9KYMk72"))
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
  void shouldOrderRelationshipsByCreatedDesc()
      throws ForbiddenException, NotFoundException, BadRequestException {
    Relationship oLT07jKRu9e = get(Relationship.class, "oLT07jKRu9e");
    Relationship yZxjxJli9mO = get(Relationship.class, "yZxjxJli9mO");

    RelationshipOperationParams params =
        RelationshipOperationParams.builder()
            .type(TrackerType.EVENT)
            .identifier(UID.of("pTzf9KYMk72"))
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

  private static <T> void assertPage(int pageNumber, int pageSize, Page<T> page) {
    assertNotNull(page, "paginated results should have a page");
    assertAll(
        "pagination details",
        () -> assertEquals(pageNumber, page.getPage(), "number of current page"),
        () -> assertEquals(pageSize, page.getPageSize(), "page size"));
  }

  private static <T> void assertPage(int pageNumber, int pageSize, int totalCount, Page<T> page) {
    assertNotNull(page, "paginated results should have a page");
    assertAll(
        "pagination details",
        () -> assertEquals(pageNumber, page.getPage(), "number of current page"),
        () -> assertEquals(pageSize, page.getPageSize(), "page size"),
        () -> assertEquals(totalCount, page.getTotal(), "total count of items"));
  }

  private List<String> getTrackedEntities(TrackedEntityOperationParams params)
      throws ForbiddenException, BadRequestException, NotFoundException {
    return uids(trackedEntityService.getTrackedEntities(params));
  }

  private List<String> getEnrollments(EnrollmentOperationParams params)
      throws ForbiddenException, BadRequestException, NotFoundException {
    return uids(enrollmentService.getEnrollments(params));
  }

  private List<String> getEvents(EventOperationParams params)
      throws ForbiddenException, BadRequestException, NotFoundException {
    return uids(eventService.getEvents(params));
  }

  private List<String> getEvents(EventOperationParams params, PageParams pageParams)
      throws ForbiddenException, BadRequestException, NotFoundException {
    return uids(eventService.getEvents(params, pageParams).getItems());
  }

  private List<String> getRelationships(RelationshipOperationParams params)
      throws ForbiddenException, NotFoundException, BadRequestException {
    return uids(relationshipService.getRelationships(params));
  }

  private static <T extends BaseIdentifiableObject> List<String> uids(Page<T> events) {
    return uids(events.getItems());
  }

  private static List<String> uids(List<? extends BaseIdentifiableObject> identifiableObject) {
    return identifiableObject.stream().map(BaseIdentifiableObject::getUid).toList();
  }
}
