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
package org.hisp.dhis.tracker.export;

import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.DESCENDANTS;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;
import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
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
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.SingleEvent;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.Page;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.TestSetup;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentOperationParams;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentService;
import org.hisp.dhis.tracker.export.relationship.RelationshipOperationParams;
import org.hisp.dhis.tracker.export.relationship.RelationshipService;
import org.hisp.dhis.tracker.export.singleevent.SingleEventOperationParams;
import org.hisp.dhis.tracker.export.singleevent.SingleEventService;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityOperationParams;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityService;
import org.hisp.dhis.tracker.export.trackerevent.TrackerEventOperationParams;
import org.hisp.dhis.tracker.export.trackerevent.TrackerEventService;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** Tests ordering and pagination of tracker exporters via the service layer. */
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrderAndPaginationExporterTest extends PostgresIntegrationTestBase {
  @Autowired private TestSetup testSetup;

  @Autowired private TrackedEntityService trackedEntityService;

  @Autowired private EnrollmentService enrollmentService;

  @Autowired private RelationshipService relationshipService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private TrackerEventService trackerEventService;

  @Autowired private SingleEventService singleEventService;

  private OrganisationUnit orgUnit;

  private OrganisationUnit singleEventOrgUnit;

  private ProgramStage programStage;

  private TrackedEntityType trackedEntityType;

  private TrackerEventOperationParams.TrackerEventOperationParamsBuilder
      trackerEventOperationParamsBuilder;

  private SingleEventOperationParams.SingleEventOperationParamsBuilder
      singleEventOperationParamsBuilder;

  private User importUser;

  @BeforeAll
  void setUp() throws IOException {
    testSetup.importMetadata();

    importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);

    testSetup.importTrackerData();
    orgUnit = get(OrganisationUnit.class, "h4w96yEMlzO");
    singleEventOrgUnit = get(OrganisationUnit.class, "DiszpKrYNg8");
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

    trackerEventOperationParamsBuilder =
        TrackerEventOperationParams.builder().orgUnitMode(SELECTED);
    singleEventOperationParamsBuilder = SingleEventOperationParams.builder().orgUnitMode(SELECTED);
  }

  @Test
  void shouldReturnPaginatedTrackedEntitiesGivenNonDefaultPageSize()
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnit)
            .orgUnitMode(DESCENDANTS)
            .trackedEntityType(trackedEntityType)
            .orderBy(UID.of("integerAttr"), SortDirection.ASC)
            .build();

    Page<String> firstPage =
        trackedEntityService
            .findTrackedEntities(params, PageParams.of(1, 3, false))
            .withMappedItems(IdentifiableObject::getUid);

    assertEquals(
        new Page<>(List.of("dUE514NMOlo", "mHWCacsGYYn", "QS6w44flWAf"), 1, 3, null, null, 2),
        firstPage,
        "first page");

    Page<String> secondPage =
        trackedEntityService
            .findTrackedEntities(params, PageParams.of(2, 3, true))
            .withMappedItems(IdentifiableObject::getUid);

    assertEquals(
        new Page<>(List.of("QesgJkTyTCk", "woitxQbWYNq", "guVNoAerxWo"), 2, 3, 6L, 1, null),
        secondPage,
        "second (last) page");

    Page<String> thirdPage =
        trackedEntityService
            .findTrackedEntities(params, PageParams.of(3, 3, false))
            .withMappedItems(IdentifiableObject::getUid);

    assertEquals(new Page<>(List.of(), 3, 3, null, 2, null), thirdPage, "past the last page");
  }

  @Test
  void shouldReturnPaginatedTrackedEntitiesWithMaxTeCountToReturnOnProgram()
      throws ForbiddenException, BadRequestException, NotFoundException {
    injectSecurityContextUser(userService.getUser("FIgVWzUCkpw"));

    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnit)
            .orgUnitMode(SELECTED)
            .program(UID.of("BFcipDERJnf"))
            .trackedEntities(UID.of("QS6w44flWAf", "dUE514NMOlo"))
            .orderBy(UID.of("integerAttr"), SortDirection.ASC)
            .build();

    Page<String> firstPage =
        trackedEntityService
            .findTrackedEntities(params, PageParams.of(1, 1, false))
            .withMappedItems(IdentifiableObject::getUid);

    assertEquals(new Page<>(List.of("dUE514NMOlo"), 1, 1, null, null, 2), firstPage, "first page");

    Page<String> secondPage =
        trackedEntityService
            .findTrackedEntities(params, PageParams.of(2, 1, true))
            .withMappedItems(IdentifiableObject::getUid);

    assertEquals(
        new Page<>(List.of("QS6w44flWAf"), 2, 1, 2L, 1, null), secondPage, "second (last) page");

    Page<String> thirdPage =
        trackedEntityService
            .findTrackedEntities(params, PageParams.of(3, 1, false))
            .withMappedItems(IdentifiableObject::getUid);

    assertEquals(new Page<>(List.of(), 3, 1, null, 2, null), thirdPage, "past the last page");
  }

  @Test
  void shouldPaginateTrackedEntitiesWhenOrgUnitNotSpecified()
      throws ForbiddenException, BadRequestException, NotFoundException {
    injectSecurityContextUser(userService.getUser("fZidJVYpWWE"));
    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .trackedEntities(
                Set.of(UID.of("woitxQbWYNq"), UID.of("QesgJkTyTCk"), UID.of("guVNoAerxWo")))
            .build();

    Page<String> paginatedEntities =
        trackedEntityService
            .findTrackedEntities(params, PageParams.of(1, 10, true))
            .withMappedItems(IdentifiableObject::getUid);

    assertContainsOnly(List.of("woitxQbWYNq", "guVNoAerxWo"), paginatedEntities.getItems());
    assertEquals(2, paginatedEntities.getTotal());
  }

  @Test
  void shouldOrderTrackedEntitiesByInactiveAndByDefaultOrder()
      throws ForbiddenException, BadRequestException, NotFoundException {
    List<String> expected =
        Stream.of(
                get(TrackedEntity.class, "QesgJkTyTCk"),
                get(TrackedEntity.class, "dUE514NMOlo"),
                get(TrackedEntity.class, "mHWCacsGYYn"))
            .sorted(Comparator.comparing(TrackedEntity::getId).reversed()) // reversed = desc
            .map(TrackedEntity::getUid)
            .toList();

    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnit)
            .orgUnitMode(SELECTED)
            .trackedEntities(UID.of("mHWCacsGYYn", "QesgJkTyTCk", "dUE514NMOlo"))
            .trackedEntityType(trackedEntityType)
            .orderBy("inactive", SortDirection.ASC)
            .build();

    List<String> trackedEntities = getTrackedEntities(params);

    assertEquals(expected, trackedEntities);
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
            .organisationUnits(orgUnit)
            .orgUnitMode(SELECTED)
            .trackedEntities(UID.of("QS6w44flWAf", "dUE514NMOlo"))
            .trackedEntityType(trackedEntityType)
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
            .organisationUnits(orgUnit)
            .orgUnitMode(SELECTED)
            .trackedEntities(UID.of("QS6w44flWAf", "dUE514NMOlo"))
            .trackedEntityType(trackedEntityType)
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
            .organisationUnits(orgUnit)
            .orgUnitMode(SELECTED)
            .trackedEntities(UID.of("QS6w44flWAf", "dUE514NMOlo"))
            .trackedEntityType(trackedEntityType)
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
            .organisationUnits(orgUnit)
            .orgUnitMode(SELECTED)
            .trackedEntities(UID.of("QS6w44flWAf", "dUE514NMOlo"))
            .trackedEntityType(trackedEntityType)
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
            .organisationUnits(orgUnit)
            .orgUnitMode(SELECTED)
            .trackedEntities(UID.of("QS6w44flWAf", "dUE514NMOlo"))
            .trackedEntityType(trackedEntityType)
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
            .organisationUnits(orgUnit)
            .orgUnitMode(SELECTED)
            .trackedEntities(UID.of("QS6w44flWAf", "dUE514NMOlo"))
            .trackedEntityType(trackedEntityType)
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
            .organisationUnits(orgUnit)
            .orgUnitMode(SELECTED)
            .trackedEntities(UID.of("QS6w44flWAf", "dUE514NMOlo"))
            .trackedEntityType(trackedEntityType)
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
            .organisationUnits(orgUnit)
            .orgUnitMode(SELECTED)
            .trackedEntities(UID.of("QS6w44flWAf", "dUE514NMOlo"))
            .trackedEntityType(trackedEntityType)
            .program(UID.of("BFcipDERJnf"))
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
            .organisationUnits(orgUnit)
            .orgUnitMode(SELECTED)
            .trackedEntities(UID.of("QS6w44flWAf", "dUE514NMOlo"))
            .trackedEntityType(trackedEntityType)
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
            .organisationUnits(orgUnit)
            .orgUnitMode(SELECTED)
            .trackedEntities(UID.of("QS6w44flWAf", "dUE514NMOlo"))
            .trackedEntityType(trackedEntityType)
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
            .organisationUnits(orgUnit)
            .orgUnitMode(SELECTED)
            .trackedEntities(UID.of("QS6w44flWAf", "dUE514NMOlo"))
            .trackedEntityType(trackedEntityType)
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
            .organisationUnits(orgUnit)
            .orgUnitMode(SELECTED)
            .trackedEntityType(trackedEntityType)
            .orderBy(UID.of("toUpdate000"), SortDirection.ASC)
            .filterBy(UID.of("integerAttr"), List.of(new QueryFilter(QueryOperator.LT, "75")))
            .build();

    List<String> trackedEntities = getTrackedEntities(params);

    assertEquals(List.of("dUE514NMOlo", "mHWCacsGYYn"), trackedEntities);
  }

  @Test
  void shouldOrderTrackedEntitiesByAttributeWhenFilteredOnSameAttribute()
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnit)
            .orgUnitMode(SELECTED)
            .trackedEntityType(trackedEntityType)
            .filterBy(UID.of("integerAttr"), List.of(new QueryFilter(QueryOperator.LT, "75")))
            .orderBy(UID.of("integerAttr"), SortDirection.DESC)
            .build();

    List<String> trackedEntities = getTrackedEntities(params);

    assertEquals(List.of("mHWCacsGYYn", "dUE514NMOlo"), trackedEntities);
  }

  @Test
  void shouldOrderTrackedEntitiesByAttributeAndNotFilterOutATrackedEntityWithoutThatAttribute()
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnit)
            .orgUnitMode(SELECTED)
            .trackedEntityType(trackedEntityType)
            .trackedEntities(
                UID.of(
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
            .organisationUnits(orgUnit)
            .orgUnitMode(SELECTED)
            .trackedEntities(UID.of("QS6w44flWAf", "dUE514NMOlo"))
            .trackedEntityType(trackedEntityType)
            .orderBy(UID.of("toDelete000"), SortDirection.DESC)
            .orderBy(UID.of("integerAttr"), SortDirection.ASC)
            .build();

    List<String> trackedEntities = getTrackedEntities(params);

    assertEquals(List.of("dUE514NMOlo", "QS6w44flWAf"), trackedEntities);
  }

  @Test
  void shouldOrderTrackedEntitiesByMultipleAttributesDesc()
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnit)
            .orgUnitMode(SELECTED)
            .trackedEntities(UID.of("QS6w44flWAf", "dUE514NMOlo"))
            .trackedEntityType(trackedEntityType)
            .orderBy(UID.of("toDelete000"), SortDirection.DESC)
            .orderBy(UID.of("integerAttr"), SortDirection.DESC)
            .build();

    List<String> trackedEntities = getTrackedEntities(params);

    assertEquals(List.of("QS6w44flWAf", "dUE514NMOlo"), trackedEntities);
  }

  @Test
  void shouldOrderTrackedEntitiesByInactiveDesc()
      throws ForbiddenException, BadRequestException, NotFoundException {

    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnit)
            .orgUnitMode(SELECTED)
            .trackedEntities(UID.of("QS6w44flWAf", "dUE514NMOlo"))
            .trackedEntityType(trackedEntityType)
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
            .organisationUnits(orgUnit)
            .orgUnitMode(SELECTED)
            .trackedEntities(UID.of("QS6w44flWAf", "dUE514NMOlo"))
            .trackedEntityType(trackedEntityType)
            .orderBy("inactive", SortDirection.ASC)
            .build();

    List<String> trackedEntities = getTrackedEntities(params);

    assertEquals(List.of("dUE514NMOlo", "QS6w44flWAf"), trackedEntities);
  }

  @Test
  void shouldOrderEnrollmentsByStatusAndByDefaultOrder()
      throws ForbiddenException, BadRequestException {
    List<String> expected =
        Stream.of(get(Enrollment.class, "HDWTYSYkICe"), get(Enrollment.class, "GYWSSZunTLk"))
            .sorted(Comparator.comparing(Enrollment::getId).reversed()) // reversed = desc
            .map(Enrollment::getUid)
            .toList();

    EnrollmentOperationParams operationParams =
        EnrollmentOperationParams.builder()
            .orgUnits(get(OrganisationUnit.class, "DiszpKrYNg8"))
            .orgUnitMode(SELECTED)
            .enrollments(UID.of("HDWTYSYkICe", "GYWSSZunTLk"))
            .orderBy("status", SortDirection.DESC)
            .build();

    List<String> actual = getEnrollments(operationParams);

    assertEquals(expected, actual);
  }

  @Test
  void shouldReturnPaginatedEnrollmentsGivenNonDefaultPageSize()
      throws ForbiddenException, BadRequestException {
    EnrollmentOperationParams operationParams =
        EnrollmentOperationParams.builder()
            .orgUnits(orgUnit)
            .orgUnitMode(SELECTED)
            .orderBy("enrollmentDate", SortDirection.ASC)
            .build();

    Page<String> firstPage =
        enrollmentService
            .findEnrollments(operationParams, PageParams.single())
            .withMappedItems(IdentifiableObject::getUid);

    assertEquals(new Page<>(List.of("nxP7UnKhomJ"), 1, 1, null, null, 2), firstPage, "first page");

    Page<String> secondPage =
        enrollmentService
            .findEnrollments(operationParams, PageParams.of(2, 1, false))
            .withMappedItems(IdentifiableObject::getUid);

    assertEquals(new Page<>(List.of("TvctPPhpD8z"), 2, 1, null, 1, 3), secondPage, "second page");

    Page<String> thirdPage =
        enrollmentService
            .findEnrollments(operationParams, PageParams.of(3, 1, false))
            .withMappedItems(IdentifiableObject::getUid);

    assertEquals(
        new Page<>(List.of("AbctCDhqH3s"), 3, 1, null, 2, null), thirdPage, "third (last) page");

    Page<Enrollment> fourthPage =
        enrollmentService.findEnrollments(operationParams, PageParams.of(4, 1, false));

    assertEquals(new Page<>(List.of(), 4, 1, null, 3, null), fourthPage, "past the last page");
  }

  @Test
  void shouldReturnPaginatedEnrollmentsGivenNonDefaultPageSizeAndTotalPages()
      throws ForbiddenException, BadRequestException {
    EnrollmentOperationParams operationParams =
        EnrollmentOperationParams.builder()
            .orgUnits(orgUnit)
            .orgUnitMode(SELECTED)
            .orderBy("enrollmentDate", SortDirection.ASC)
            .build();

    Page<String> firstPage =
        enrollmentService
            .findEnrollments(operationParams, PageParams.of(1, 1, true))
            .withMappedItems(IdentifiableObject::getUid);

    assertEquals(new Page<>(List.of("nxP7UnKhomJ"), 1, 1, 3L, null, 2), firstPage, "first page");

    Page<String> secondPage =
        enrollmentService
            .findEnrollments(operationParams, PageParams.of(2, 1, true))
            .withMappedItems(IdentifiableObject::getUid);

    assertEquals(new Page<>(List.of("TvctPPhpD8z"), 2, 1, 3L, 1, 3), secondPage, "second page");

    Page<String> thirdPage =
        enrollmentService
            .findEnrollments(operationParams, PageParams.of(3, 1, true))
            .withMappedItems(IdentifiableObject::getUid);

    assertEquals(
        new Page<>(List.of("AbctCDhqH3s"), 3, 1, 3L, 2, null), thirdPage, "third (last) page");

    Page<Enrollment> fourthPage =
        enrollmentService.findEnrollments(operationParams, PageParams.of(4, 1, true));

    assertEquals(new Page<>(List.of(), 4, 1, 3L, 3, null), fourthPage, "past the last page");
  }

  @Test
  void shouldOrderEnrollmentsByPrimaryKeyDescByDefault()
      throws ForbiddenException, BadRequestException {
    Enrollment nxP7UnKhomJ = get(Enrollment.class, "nxP7UnKhomJ");
    Enrollment TvctPPhpD8z = get(Enrollment.class, "TvctPPhpD8z");
    Enrollment AbctCDhqH3s = get(Enrollment.class, "AbctCDhqH3s");
    List<String> expected =
        Stream.of(nxP7UnKhomJ, TvctPPhpD8z, AbctCDhqH3s)
            .sorted(Comparator.comparing(Enrollment::getId).reversed()) // reversed = desc
            .map(Enrollment::getUid)
            .toList();

    EnrollmentOperationParams params =
        EnrollmentOperationParams.builder().orgUnits(orgUnit).orgUnitMode(SELECTED).build();

    List<String> enrollments = getEnrollments(params);

    assertEquals(expected, enrollments);
  }

  @Test
  void shouldOrderEnrollmentsByEnrolledAtAsc() throws ForbiddenException, BadRequestException {
    EnrollmentOperationParams params =
        EnrollmentOperationParams.builder()
            .orgUnits(orgUnit)
            .orgUnitMode(SELECTED)
            .orderBy("enrollmentDate", SortDirection.ASC)
            .build();

    List<String> enrollments = getEnrollments(params);

    assertEquals(List.of("nxP7UnKhomJ", "TvctPPhpD8z", "AbctCDhqH3s"), enrollments);
  }

  @Test
  void shouldOrderEnrollmentsByEnrolledAtDesc() throws ForbiddenException, BadRequestException {
    EnrollmentOperationParams params =
        EnrollmentOperationParams.builder()
            .orgUnits(orgUnit)
            .orgUnitMode(SELECTED)
            .orderBy("enrollmentDate", SortDirection.DESC)
            .build();

    List<String> enrollments = getEnrollments(params);

    assertEquals(List.of("AbctCDhqH3s", "TvctPPhpD8z", "nxP7UnKhomJ"), enrollments);
  }

  @Test
  void shouldOrderTrackerEventsByStatusAndByDefaultOrder()
      throws ForbiddenException, BadRequestException {
    List<String> expected =
        Stream.of(get(TrackerEvent.class, "D9PbzJY8bJM"), get(TrackerEvent.class, "pTzf9KYMk72"))
            .sorted(Comparator.comparing(TrackerEvent::getId).reversed()) // reversed = desc
            .map(TrackerEvent::getUid)
            .toList();

    TrackerEventOperationParams operationParams =
        trackerEventOperationParamsBuilder
            .orgUnit(orgUnit)
            .orderBy("status", SortDirection.DESC)
            .build();

    List<String> actual = getTrackerEvents(operationParams);

    assertEquals(expected, actual);
  }

  @Test
  void shouldReturnPaginatedTrackerEventsGivenNonDefaultPageSize()
      throws ForbiddenException, BadRequestException {
    TrackerEventOperationParams operationParams =
        trackerEventOperationParamsBuilder
            .orgUnit(orgUnit)
            .events(UID.of("pTzf9KYMk72", "D9PbzJY8bJM"))
            .orderBy("occurredDate", SortDirection.DESC)
            .build();

    Page<String> firstPage =
        trackerEventService
            .findEvents(operationParams, PageParams.single())
            .withMappedItems(IdentifiableObject::getUid);

    assertEquals(new Page<>(List.of("D9PbzJY8bJM"), 1, 1, null, null, 2), firstPage, "first page");

    Page<String> secondPage =
        trackerEventService
            .findEvents(operationParams, PageParams.of(2, 1, true))
            .withMappedItems(IdentifiableObject::getUid);

    assertEquals(
        new Page<>(List.of("pTzf9KYMk72"), 2, 1, 2L, 1, null), secondPage, "second (last) page");

    Page<TrackerEvent> thirdPage =
        trackerEventService.findEvents(operationParams, PageParams.of(3, 1, false));

    assertEquals(new Page<>(List.of(), 3, 1, null, 2, null), thirdPage, "past the last page");
  }

  @Test
  void shouldOrderTrackerEventsByPrimaryKeyDescByDefault()
      throws ForbiddenException, BadRequestException {
    TrackerEvent d9PbzJY8bJM = get(TrackerEvent.class, "D9PbzJY8bJM");
    TrackerEvent pTzf9KYMk72 = get(TrackerEvent.class, "pTzf9KYMk72");
    List<String> expected =
        Stream.of(d9PbzJY8bJM, pTzf9KYMk72)
            .sorted(Comparator.comparing(TrackerEvent::getId).reversed()) // reversed = desc
            .map(TrackerEvent::getUid)
            .toList();

    TrackerEventOperationParams params =
        trackerEventOperationParamsBuilder.orgUnit(orgUnit).build();

    List<String> events = getTrackerEvents(params);

    assertEquals(expected, events);
  }

  @Test
  void shouldOrderTrackerEventsByEnrollmentProgramUIDAsc()
      throws ForbiddenException, BadRequestException {
    TrackerEvent pTzf9KYMk72 =
        get(TrackerEvent.class, "pTzf9KYMk72"); // enrolled in program BFcipDERJnf
    TrackerEvent jxgFyJEMUPf =
        get(TrackerEvent.class, "jxgFyJEMUPf"); // enrolled in program shPjYNifvMK
    List<String> expected =
        Stream.of(pTzf9KYMk72, jxgFyJEMUPf)
            .sorted(Comparator.comparing(event -> event.getEnrollment().getProgram().getUid()))
            .map(TrackerEvent::getUid)
            .toList();

    TrackerEventOperationParams params =
        trackerEventOperationParamsBuilder
            .orgUnitMode(ACCESSIBLE)
            .events(UID.of("jxgFyJEMUPf", "pTzf9KYMk72"))
            .orderBy("enrollment.program.uid", SortDirection.ASC)
            .build();

    List<String> events = getTrackerEvents(params);

    assertEquals(expected, events);
  }

  @Test
  void shouldOrderTrackerEventsByEnrollmentProgramUIDDesc()
      throws ForbiddenException, BadRequestException {
    TrackerEvent pTzf9KYMk72 =
        get(TrackerEvent.class, "pTzf9KYMk72"); // enrolled in program BFcipDERJnf
    TrackerEvent jxgFyJEMUPf =
        get(TrackerEvent.class, "jxgFyJEMUPf"); // enrolled in program shPjYNifvMK
    List<String> expected =
        new java.util.ArrayList<>(
            Stream.of(pTzf9KYMk72, jxgFyJEMUPf)
                .sorted(Comparator.comparing(event -> event.getEnrollment().getProgram().getUid()))
                .map(TrackerEvent::getUid)
                .toList());
    Collections.reverse(expected);

    TrackerEventOperationParams params =
        trackerEventOperationParamsBuilder
            .orgUnitMode(ACCESSIBLE)
            .events(UID.of("pTzf9KYMk72", "jxgFyJEMUPf"))
            .orderBy("enrollment.program.uid", SortDirection.DESC)
            .build();

    List<String> events = getTrackerEvents(params);

    assertEquals(expected, events);
  }

  @Test
  void shouldOrderTrackerEventsByAttributeAsc() throws ForbiddenException, BadRequestException {
    TrackerEventOperationParams params =
        trackerEventOperationParamsBuilder
            .orgUnit(orgUnit)
            .orderBy(UID.of("toUpdate000"), SortDirection.ASC)
            .build();

    List<String> events = getTrackerEvents(params);

    assertEquals(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), events);
  }

  @Test
  void shouldOrderTrackerEventsByAttributeDesc() throws ForbiddenException, BadRequestException {
    TrackerEventOperationParams params =
        trackerEventOperationParamsBuilder
            .orgUnit(orgUnit)
            .orderBy(UID.of("toUpdate000"), SortDirection.DESC)
            .build();

    List<String> events = getTrackerEvents(params);

    assertEquals(List.of("pTzf9KYMk72", "D9PbzJY8bJM"), events);
  }

  @Test
  void
      shouldOrderTrackerEventsByAttributeAndNotFilterOutEventsWithATrackedEntityWithoutThatAttribute()
          throws ForbiddenException, BadRequestException {
    TrackerEventOperationParams params =
        trackerEventOperationParamsBuilder
            .orgUnit(orgUnit)
            .events(
                UID.of(
                    "pTzf9KYMk72",
                    "D9PbzJY8bJM")) // EV pTzf9KYMk72 => TE QS6w44flWAf without attribute
            // notUpdated0
            .orderBy(UID.of("notUpdated0"), SortDirection.ASC)
            .build();

    List<String> events = getTrackerEvents(params);

    assertEquals(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), events);
  }

  @Test
  void shouldOrderTrackerEventsByMultipleAttributesDesc()
      throws ForbiddenException, BadRequestException {
    TrackerEventOperationParams params =
        trackerEventOperationParamsBuilder
            .orgUnit(orgUnit)
            .orderBy(UID.of("toDelete000"), SortDirection.DESC)
            .orderBy(UID.of("toUpdate000"), SortDirection.DESC)
            .build();

    List<String> events = getTrackerEvents(params);

    assertEquals(List.of("pTzf9KYMk72", "D9PbzJY8bJM"), events);
  }

  @Test
  void shouldOrderTrackerEventsByMultipleAttributesAsc()
      throws ForbiddenException, BadRequestException {
    TrackerEventOperationParams params =
        trackerEventOperationParamsBuilder
            .orgUnit(orgUnit)
            .orderBy(UID.of("toDelete000"), SortDirection.DESC)
            .orderBy(UID.of("toUpdate000"), SortDirection.ASC)
            .build();

    List<TrackerEvent> events = trackerEventService.findEvents(params);

    assertEquals(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), uids(events));
    List<String> trackedEntities =
        events.stream().map(event -> event.getEnrollment().getTrackedEntity().getUid()).toList();
    assertEquals(List.of("dUE514NMOlo", "QS6w44flWAf"), trackedEntities);
  }

  @Test
  void shouldReturnPaginatedTrackerEventsOrderedByMultipleAttributesWhenGivenNonDefaultPageSize()
      throws ForbiddenException, BadRequestException {
    TrackerEventOperationParams operationParams =
        trackerEventOperationParamsBuilder
            .orgUnit(orgUnit)
            .orderBy(UID.of("toDelete000"), SortDirection.DESC)
            .orderBy(UID.of("toUpdate000"), SortDirection.ASC)
            .build();

    Page<String> firstPage =
        trackerEventService
            .findEvents(operationParams, PageParams.single())
            .withMappedItems(IdentifiableObject::getUid);

    assertEquals(new Page<>(List.of("D9PbzJY8bJM"), 1, 1, null, null, 2), firstPage, "first page");

    Page<String> secondPage =
        trackerEventService
            .findEvents(operationParams, PageParams.of(2, 1, false))
            .withMappedItems(IdentifiableObject::getUid);

    assertEquals(
        new Page<>(List.of("pTzf9KYMk72"), 2, 1, null, 1, null), secondPage, "second (last) page");

    Page<String> thirdPage =
        trackerEventService
            .findEvents(operationParams, PageParams.of(3, 3, false))
            .withMappedItems(IdentifiableObject::getUid);

    assertEquals(new Page<>(List.of(), 3, 3, null, 2, null), thirdPage, "past the last page");
  }

  @Test
  void shouldOrderTrackerEventsByProgramStageUidDesc()
      throws ForbiddenException, BadRequestException {
    TrackerEventOperationParams params =
        trackerEventOperationParamsBuilder
            .orgUnit(UID.of("uoNW0E3xXUy"))
            .orderBy("programStage.uid", SortDirection.DESC)
            .build();

    List<String> events = getTrackerEvents(params);

    assertEquals(List.of("JaRDIvcEcEx", "jxgFyJEMUPf"), events);
  }

  @Test
  void shouldOrderTrackerEventsByProgramStageUidAsc()
      throws ForbiddenException, BadRequestException {
    TrackerEventOperationParams params =
        trackerEventOperationParamsBuilder
            .orgUnit(UID.of("uoNW0E3xXUy"))
            .orderBy("programStage.uid", SortDirection.ASC)
            .build();

    List<String> events = getTrackerEvents(params);

    assertEquals(List.of("jxgFyJEMUPf", "JaRDIvcEcEx"), events);
  }

  @Test
  void shouldOrderTrackerEventsByTrackedEntityUidDesc()
      throws ForbiddenException, BadRequestException {
    TrackerEventOperationParams params =
        trackerEventOperationParamsBuilder
            .orgUnit(orgUnit)
            .orderBy("enrollment.trackedEntity.uid", SortDirection.DESC)
            .build();

    List<String> events = getTrackerEvents(params);

    assertEquals(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), events);
  }

  @Test
  void shouldOrderTrackerEventsByTrackedEntityUidAsc()
      throws ForbiddenException, BadRequestException {
    TrackerEventOperationParams params =
        trackerEventOperationParamsBuilder
            .orgUnit(orgUnit)
            .orderBy("enrollment.trackedEntity.uid", SortDirection.ASC)
            .build();

    List<String> events = getTrackerEvents(params);

    assertEquals(List.of("pTzf9KYMk72", "D9PbzJY8bJM"), events);
  }

  @Test
  void shouldOrderTrackerEventsByAttributeOptionComboUidDesc()
      throws ForbiddenException, BadRequestException {
    TrackerEventOperationParams params =
        trackerEventOperationParamsBuilder
            .orgUnit(orgUnit)
            .orderBy("attributeOptionCombo.uid", SortDirection.DESC)
            .build();

    List<String> events = getTrackerEvents(params);

    assertEquals(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), events);
  }

  @Test
  void shouldOrderTrackerEventsByAttributeOptionComboUidAsc()
      throws ForbiddenException, BadRequestException {
    TrackerEventOperationParams params =
        trackerEventOperationParamsBuilder
            .orgUnit(orgUnit)
            .orderBy("attributeOptionCombo.uid", SortDirection.ASC)
            .build();

    List<String> events = getTrackerEvents(params);

    assertEquals(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), events);
  }

  @Test
  void shouldOrderTrackerEventsByOccurredAtDesc() throws ForbiddenException, BadRequestException {
    TrackerEventOperationParams params =
        trackerEventOperationParamsBuilder
            .orgUnit(orgUnit)
            .orderBy("occurredDate", SortDirection.DESC)
            .build();

    List<String> events = getTrackerEvents(params);

    assertEquals(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), events);
  }

  @Test
  void shouldOrderTrackerEventsByOccurredAtAsc() throws ForbiddenException, BadRequestException {
    TrackerEventOperationParams params =
        trackerEventOperationParamsBuilder
            .orgUnit(orgUnit)
            .orderBy("occurredDate", SortDirection.ASC)
            .build();

    List<String> events = getTrackerEvents(params);

    assertEquals(List.of("pTzf9KYMk72", "D9PbzJY8bJM"), events);
  }

  @Test
  void shouldOrderTrackerEventsByCreatedAtClientInAscOrder()
      throws ForbiddenException, BadRequestException {
    TrackerEventOperationParams params =
        trackerEventOperationParamsBuilder
            .orgUnit(orgUnit)
            .programStage(programStage)
            .orderBy("createdAtClient", SortDirection.ASC)
            .build();

    List<String> events = getTrackerEvents(params);

    assertEquals(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), events);
  }

  @Test
  void shouldOrderTrackerEventsByCreatedAtClientInDescOrder()
      throws ForbiddenException, BadRequestException {
    TrackerEventOperationParams params =
        trackerEventOperationParamsBuilder
            .orgUnit(orgUnit)
            .programStage(programStage)
            .orderBy("createdAtClient", SortDirection.DESC)
            .build();

    List<String> events = getTrackerEvents(params);

    assertEquals(List.of("pTzf9KYMk72", "D9PbzJY8bJM"), events);
  }

  @Test
  void shouldOrderTrackerEventsByUpdatedAtClientInAscOrder()
      throws ForbiddenException, BadRequestException {
    TrackerEventOperationParams params =
        trackerEventOperationParamsBuilder
            .orgUnit(orgUnit)
            .programStage(programStage)
            .orderBy("lastUpdatedAtClient", SortDirection.ASC)
            .build();

    List<String> events = getTrackerEvents(params);

    assertEquals(List.of("pTzf9KYMk72", "D9PbzJY8bJM"), events);
  }

  @Test
  void shouldOrderTrackerEventsByUpdatedAtClientInDescOrder()
      throws ForbiddenException, BadRequestException {
    TrackerEventOperationParams params =
        trackerEventOperationParamsBuilder
            .orgUnit(orgUnit)
            .programStage(programStage)
            .orderBy("lastUpdatedAtClient", SortDirection.DESC)
            .build();

    List<String> events = getTrackerEvents(params);

    assertEquals(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), events);
  }

  @Test
  void shouldOrderTrackerEventsRespectingOrderWhenAttributeOrderSuppliedBeforeOrderParam()
      throws ForbiddenException, BadRequestException {
    TrackerEventOperationParams params =
        trackerEventOperationParamsBuilder
            .orgUnit(orgUnit)
            .orderBy(UID.of("toUpdate000"), SortDirection.ASC)
            .orderBy("enrollment.enrollmentDate", SortDirection.ASC)
            .build();

    List<String> events = getTrackerEvents(params);

    assertEquals(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), events);
  }

  @Test
  void shouldOrderTrackerEventsRespectingOrderWhenOrderParamSuppliedBeforeAttributeOrder()
      throws ForbiddenException, BadRequestException {
    TrackerEventOperationParams params =
        trackerEventOperationParamsBuilder
            .orgUnit(orgUnit)
            .orderBy("enrollment.enrollmentDate", SortDirection.DESC)
            .orderBy(UID.of("toUpdate000"), SortDirection.DESC)
            .build();

    List<String> events = getTrackerEvents(params);

    assertEquals(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), events);
  }

  @Test
  void shouldOrderTrackerEventsRespectingOrderWhenDataElementSuppliedBeforeOrderParam()
      throws ForbiddenException, BadRequestException {
    TrackerEventOperationParams params =
        trackerEventOperationParamsBuilder
            .orgUnit(orgUnit)
            .orderBy("scheduledDate", SortDirection.DESC)
            .orderBy(UID.of("DATAEL00006"), SortDirection.DESC)
            .orderBy("enrollment.enrollmentDate", SortDirection.DESC)
            .build();

    List<String> events = getTrackerEvents(params);

    assertEquals(List.of("pTzf9KYMk72", "D9PbzJY8bJM"), events);
  }

  @Test
  void shouldOrderTrackerEventsRespectingOrderWhenOrderParamSuppliedBeforeDataElement()
      throws ForbiddenException, BadRequestException {
    TrackerEventOperationParams params =
        trackerEventOperationParamsBuilder
            .orgUnit(orgUnit)
            .orderBy("enrollment.enrollmentDate", SortDirection.DESC)
            .orderBy(UID.of("DATAEL00006"), SortDirection.DESC)
            .build();

    List<String> events = getTrackerEvents(params);

    assertEquals(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), events);
  }

  @Test
  void shouldOrderTrackerEventsByDataElementAndNotFilterOutEventsWithoutThatDataElement()
      throws ForbiddenException, BadRequestException {
    TrackerEventOperationParams params =
        trackerEventOperationParamsBuilder
            .orgUnit(orgUnit)
            .events(
                UID.of(
                    "pTzf9KYMk72", // EV pTzf9KYMk72 without data element DATAEL00002
                    "D9PbzJY8bJM"))
            // notUpdated0
            .orderBy(UID.of("DATAEL00002"), SortDirection.DESC)
            .build();

    List<String> events = getTrackerEvents(params);

    // https://www.postgresql.org/docs/current/queries-order.html
    // By default, null values sort as if larger than any non-null value
    // => EV pTzf9KYMk72 without data element DATAEL00002 will come first when DESC
    assertEquals(List.of("pTzf9KYMk72", "D9PbzJY8bJM"), events);
  }

  @Test
  void shouldOrderSingleEventsByStatusAndByDefaultOrder()
      throws ForbiddenException, BadRequestException {
    List<String> expected =
        Stream.of(
                get(TrackerEvent.class, "ck7DzdxqLqA"),
                get(TrackerEvent.class, "kWjSezkXHVp"),
                get(TrackerEvent.class, "OTmjvJDn0Fu"))
            .sorted(Comparator.comparing(TrackerEvent::getId).reversed()) // reversed = desc
            .map(TrackerEvent::getUid)
            .toList();

    SingleEventOperationParams operationParams =
        singleEventOperationParamsBuilder
            .orgUnit(singleEventOrgUnit)
            .events(UID.of("ck7DzdxqLqA", "kWjSezkXHVp", "OTmjvJDn0Fu"))
            .orderBy("status", SortDirection.DESC)
            .build();

    List<String> actual = getSingleEvents(operationParams);

    assertEquals(expected, actual);
  }

  @Test
  void shouldReturnPaginatedSingleEventsGivenNonDefaultPageSize()
      throws ForbiddenException, BadRequestException {
    SingleEventOperationParams operationParams =
        singleEventOperationParamsBuilder
            .orgUnit(singleEventOrgUnit)
            .events(UID.of("ck7DzdxqLqA", "kWjSezkXHVp"))
            .orderBy("occurredDate", SortDirection.DESC)
            .build();

    Page<String> firstPage =
        singleEventService
            .findEvents(operationParams, PageParams.single())
            .withMappedItems(IdentifiableObject::getUid);

    assertEquals(new Page<>(List.of("ck7DzdxqLqA"), 1, 1, null, null, 2), firstPage, "first page");

    Page<String> secondPage =
        singleEventService
            .findEvents(operationParams, PageParams.of(2, 1, true))
            .withMappedItems(IdentifiableObject::getUid);

    assertEquals(
        new Page<>(List.of("kWjSezkXHVp"), 2, 1, 2L, 1, null), secondPage, "second (last) page");

    Page<SingleEvent> thirdPage =
        singleEventService.findEvents(operationParams, PageParams.of(3, 1, false));

    assertEquals(new Page<>(List.of(), 3, 1, null, 2, null), thirdPage, "past the last page");
  }

  @Test
  void shouldOrderSingleEventsByPrimaryKeyDescByDefault()
      throws ForbiddenException, BadRequestException {
    TrackerEvent qrYjLTiJTrA = get(TrackerEvent.class, "QRYjLTiJTrA");
    TrackerEvent lumVtWwwy0O = get(TrackerEvent.class, "lumVtWwwy0O");
    List<String> expected =
        Stream.of(qrYjLTiJTrA, lumVtWwwy0O)
            .sorted(Comparator.comparing(TrackerEvent::getId).reversed()) // reversed = desc
            .map(TrackerEvent::getUid)
            .toList();

    SingleEventOperationParams params =
        singleEventOperationParamsBuilder
            .events(UID.of("QRYjLTiJTrA", "lumVtWwwy0O"))
            .orgUnit(singleEventOrgUnit)
            .build();

    List<String> events = getSingleEvents(params);

    assertEquals(expected, events);
  }

  @Test
  void shouldOrderSingleEventsByEnrollmentProgramUIDAsc()
      throws ForbiddenException, BadRequestException {
    TrackerEvent ck7DzdxqLqA =
        get(TrackerEvent.class, "ck7DzdxqLqA"); // enrolled in program iS7eutanDry
    TrackerEvent g9PbzJY8bJG =
        get(TrackerEvent.class, "G9PbzJY8bJG"); // enrolled in program BFcipDERJng
    List<String> expected =
        new java.util.ArrayList<>(
            Stream.of(ck7DzdxqLqA, g9PbzJY8bJG)
                .sorted(Comparator.comparing(event -> event.getEnrollment().getProgram().getUid()))
                .map(TrackerEvent::getUid)
                .toList());

    SingleEventOperationParams params =
        singleEventOperationParamsBuilder
            .orgUnitMode(ACCESSIBLE)
            .events(UID.of("G9PbzJY8bJG", "ck7DzdxqLqA"))
            .orderBy("enrollment.program.uid", SortDirection.ASC)
            .build();

    List<String> events = getSingleEvents(params);

    assertEquals(expected, events);
  }

  @Test
  void shouldOrderSingleEventsByEnrollmentProgramUIDDesc()
      throws ForbiddenException, BadRequestException {
    TrackerEvent ck7DzdxqLqA =
        get(TrackerEvent.class, "ck7DzdxqLqA"); // enrolled in program iS7eutanDry
    TrackerEvent g9PbzJY8bJG =
        get(TrackerEvent.class, "G9PbzJY8bJG"); // enrolled in program BFcipDERJng
    List<String> expected =
        new java.util.ArrayList<>(
            Stream.of(ck7DzdxqLqA, g9PbzJY8bJG)
                .sorted(Comparator.comparing(event -> event.getEnrollment().getProgram().getUid()))
                .map(TrackerEvent::getUid)
                .toList());
    Collections.reverse(expected);

    SingleEventOperationParams params =
        singleEventOperationParamsBuilder
            .orgUnitMode(ACCESSIBLE)
            .events(UID.of("ck7DzdxqLqA", "G9PbzJY8bJG"))
            .orderBy("enrollment.program.uid", SortDirection.DESC)
            .build();

    List<String> events = getSingleEvents(params);

    assertEquals(expected, events);
  }

  @Test
  void shouldOrderSingleEventsByAttributeOptionComboUidDesc()
      throws ForbiddenException, BadRequestException {
    SingleEventOperationParams params =
        singleEventOperationParamsBuilder
            .orgUnit(singleEventOrgUnit)
            .events(UID.of("ck7DzdxqLqA", "lumVtWwwy0O", "cadc5eGj0j7"))
            .orderBy("attributeOptionCombo.uid", SortDirection.DESC)
            .build();

    List<String> events = getSingleEvents(params);

    assertEquals(List.of("ck7DzdxqLqA", "cadc5eGj0j7", "lumVtWwwy0O"), events);
  }

  @Test
  void shouldOrderSingleEventsByAttributeOptionComboUidAsc()
      throws ForbiddenException, BadRequestException {
    SingleEventOperationParams params =
        singleEventOperationParamsBuilder
            .orgUnit(UID.of("DiszpKrYNg8"))
            .events(UID.of("ck7DzdxqLqA", "lumVtWwwy0O", "cadc5eGj0j7"))
            .orderBy("attributeOptionCombo.uid", SortDirection.ASC)
            .build();

    List<String> events = getSingleEvents(params);

    assertEquals(List.of("lumVtWwwy0O", "cadc5eGj0j7", "ck7DzdxqLqA"), events);
  }

  @Test
  void shouldOrderSingleEventsByOccurredAtDesc() throws ForbiddenException, BadRequestException {
    SingleEventOperationParams params =
        singleEventOperationParamsBuilder
            .orgUnit(singleEventOrgUnit)
            .events(UID.of("ck7DzdxqLqA", "lumVtWwwy0O", "cadc5eGj0j7"))
            .orderBy("occurredDate", SortDirection.DESC)
            .build();

    List<String> events = getSingleEvents(params);

    assertEquals(List.of("ck7DzdxqLqA", "lumVtWwwy0O", "cadc5eGj0j7"), events);
  }

  @Test
  void shouldOrderSingleEventsByOccurredAtAsc() throws ForbiddenException, BadRequestException {
    SingleEventOperationParams params =
        singleEventOperationParamsBuilder
            .orgUnit(singleEventOrgUnit)
            .events(UID.of("ck7DzdxqLqA", "lumVtWwwy0O", "cadc5eGj0j7"))
            .orderBy("occurredDate", SortDirection.ASC)
            .build();

    List<String> events = getSingleEvents(params);

    assertEquals(List.of("cadc5eGj0j7", "lumVtWwwy0O", "ck7DzdxqLqA"), events);
  }

  @Test
  void shouldOrderSingleEventsByCreatedAtClientInAscOrder()
      throws ForbiddenException, BadRequestException {
    SingleEventOperationParams params =
        singleEventOperationParamsBuilder
            .events(UID.of("ck7DzdxqLqA", "lumVtWwwy0O", "cadc5eGj0j7"))
            .orgUnit(singleEventOrgUnit)
            .orderBy("createdAtClient", SortDirection.ASC)
            .build();

    List<String> events = getSingleEvents(params);

    assertEquals(List.of("cadc5eGj0j7", "lumVtWwwy0O", "ck7DzdxqLqA"), events);
  }

  @Test
  void shouldOrderSingleEventsByCreatedAtClientInDescOrder()
      throws ForbiddenException, BadRequestException {
    SingleEventOperationParams params =
        singleEventOperationParamsBuilder
            .orgUnit(singleEventOrgUnit)
            .events(UID.of("ck7DzdxqLqA", "lumVtWwwy0O", "cadc5eGj0j7"))
            .orderBy("createdAtClient", SortDirection.DESC)
            .build();

    List<String> events = getSingleEvents(params);

    assertEquals(List.of("ck7DzdxqLqA", "lumVtWwwy0O", "cadc5eGj0j7"), events);
  }

  @Test
  void shouldOrderSingleEventsByUpdatedAtClientInAscOrder()
      throws ForbiddenException, BadRequestException {
    SingleEventOperationParams params =
        singleEventOperationParamsBuilder
            .events(UID.of("ck7DzdxqLqA", "lumVtWwwy0O", "cadc5eGj0j7"))
            .orgUnit(singleEventOrgUnit)
            .orderBy("lastUpdatedAtClient", SortDirection.ASC)
            .build();

    List<String> events = getSingleEvents(params);

    assertEquals(List.of("lumVtWwwy0O", "ck7DzdxqLqA", "cadc5eGj0j7"), events);
  }

  @Test
  void shouldOrderSingleEventsByUpdatedAtClientInDescOrder()
      throws ForbiddenException, BadRequestException {
    SingleEventOperationParams params =
        singleEventOperationParamsBuilder
            .orgUnit(singleEventOrgUnit)
            .events(UID.of("cadc5eGj0j7", "lumVtWwwy0O"))
            .orderBy("lastUpdatedAtClient", SortDirection.DESC)
            .build();

    List<String> events = getSingleEvents(params);

    assertEquals(List.of("cadc5eGj0j7", "lumVtWwwy0O"), events);
  }

  @Test
  void shouldOrderSingleEventsRespectingOrderWhenDataElementSuppliedBeforeOrderParam()
      throws ForbiddenException, BadRequestException {
    SingleEventOperationParams params =
        singleEventOperationParamsBuilder
            .orgUnit(singleEventOrgUnit)
            .events(UID.of("ck7DzdxqLqA", "lumVtWwwy0O", "cadc5eGj0j7"))
            .orderBy("occurredDate", SortDirection.DESC)
            .orderBy(UID.of("DATAEL00006"), SortDirection.DESC)
            .orderBy("lastUpdated", SortDirection.DESC)
            .build();

    List<String> events = getSingleEvents(params);

    assertEquals(List.of("ck7DzdxqLqA", "lumVtWwwy0O", "cadc5eGj0j7"), events);
  }

  @Test
  void shouldOrderSingleEventsRespectingOrderWhenOrderParamSuppliedBeforeDataElement()
      throws ForbiddenException, BadRequestException {
    SingleEventOperationParams params =
        singleEventOperationParamsBuilder
            .events(UID.of("ck7DzdxqLqA", "lumVtWwwy0O", "cadc5eGj0j7"))
            .orgUnit(singleEventOrgUnit)
            .orderBy("occurredDate", SortDirection.DESC)
            .orderBy(UID.of("DATAEL00006"), SortDirection.DESC)
            .build();

    List<String> events = getSingleEvents(params);

    assertEquals(List.of("ck7DzdxqLqA", "lumVtWwwy0O", "cadc5eGj0j7"), events);
  }

  @Test
  void shouldOrderSingleEventsByDataElementAndNotFilterOutEventsWithoutThatDataElement()
      throws ForbiddenException, BadRequestException {
    SingleEventOperationParams params =
        singleEventOperationParamsBuilder
            .orgUnit(singleEventOrgUnit)
            .events(
                UID.of(
                    "OTmjvJDn0Fu", // EV OTmjvJDn0Fu without data element DATAEL00007
                    "kWjSezkXHVp"))
            // notUpdated0
            .orderBy(UID.of("DATAEL00007"), SortDirection.DESC)
            .build();

    List<String> events = getSingleEvents(params);

    // https://www.postgresql.org/docs/current/queries-order.html
    // By default, null values sort as if larger than any non-null value
    // => EV OTmjvJDn0Fu without data element DATAEL00007 will come first when DESC
    assertEquals(List.of("OTmjvJDn0Fu", "kWjSezkXHVp"), events);
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
  void shouldOrderTrackerEventsByFieldInDescendingOrderWhenModeSelected(
      String field, String firstEvent, String secondEvent)
      throws ForbiddenException, BadRequestException {

    trackerEventOperationParamsBuilder.orgUnitMode(SELECTED);
    trackerEventOperationParamsBuilder.orgUnit(orgUnit);
    trackerEventOperationParamsBuilder.orderBy(field, SortDirection.DESC);

    List<String> events = getTrackerEvents(trackerEventOperationParamsBuilder.build());
    assertEquals(List.of(firstEvent, secondEvent), events);
  }

  @ParameterizedTest
  @MethodSource("orderByFieldInAscendingOrderWhenModeSelected")
  void shouldOrderTrackerEventsByFieldInAscendingOrderWhenModeSelected(
      String field, String firstEvent, String secondEvent)
      throws ForbiddenException, BadRequestException {

    trackerEventOperationParamsBuilder.orgUnitMode(SELECTED);
    trackerEventOperationParamsBuilder.orgUnit(orgUnit);
    trackerEventOperationParamsBuilder.orderBy(field, SortDirection.ASC);

    List<String> events = getTrackerEvents(trackerEventOperationParamsBuilder.build());
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
      throws ForbiddenException, BadRequestException {

    trackerEventOperationParamsBuilder.orgUnitMode(DESCENDANTS);
    trackerEventOperationParamsBuilder.orgUnit(UID.of("RojfDTBhoGC"));
    trackerEventOperationParamsBuilder.orderBy(field, SortDirection.DESC);

    List<String> events = getTrackerEvents(trackerEventOperationParamsBuilder.build());
    assertEquals(List.of(firstEvent, secondEvent), events);
  }

  @ParameterizedTest
  @MethodSource("orderByFieldInAscendingOrderWhenModeDescendants")
  void shouldOrderByFieldInAscendingOrderWhenModeDescendants(
      String field, String firstEvent, String secondEvent)
      throws ForbiddenException, BadRequestException {

    trackerEventOperationParamsBuilder.orgUnitMode(DESCENDANTS);
    trackerEventOperationParamsBuilder.orgUnit(UID.of("RojfDTBhoGC"));
    trackerEventOperationParamsBuilder.orderBy(field, SortDirection.ASC);

    List<String> events = getTrackerEvents(trackerEventOperationParamsBuilder.build());
    assertEquals(List.of(firstEvent, secondEvent), events);
  }

  @Test
  void shouldOrderRelationshipsByCreatedAtClientAndByDefaultOrder()
      throws ForbiddenException, BadRequestException, NotFoundException {
    Relationship fHn74P5T3r1 = get(Relationship.class, "fHn74P5T3r1");
    Relationship p53a6314631 = get(Relationship.class, "p53a6314631");
    Relationship yZxjxJli9mO = get(Relationship.class, "yZxjxJli9mO");
    List<String> expected =
        Stream.of(fHn74P5T3r1, p53a6314631, yZxjxJli9mO)
            .sorted(Comparator.comparing(Relationship::getId).reversed()) // reversed = desc
            .map(Relationship::getUid)
            .toList();

    RelationshipOperationParams params =
        RelationshipOperationParams.builder(TrackerType.TRACKED_ENTITY, UID.of("dUE514NMOlo"))
            .orderBy("createdAtClient", SortDirection.DESC)
            .build();

    List<String> relationships = getRelationships(params);

    assertEquals(expected, relationships);
  }

  @Test
  void shouldOrderRelationshipsByPrimaryKeyDescByDefault()
      throws ForbiddenException, BadRequestException, NotFoundException {
    Relationship oLT07jKRu9e = get(Relationship.class, "oLT07jKRu9e");
    Relationship yZxjxJli9mO = get(Relationship.class, "yZxjxJli9mO");
    List<String> expected =
        Stream.of(oLT07jKRu9e, yZxjxJli9mO)
            .sorted(Comparator.comparing(Relationship::getId).reversed()) // reversed = desc
            .map(Relationship::getUid)
            .toList();

    RelationshipOperationParams params =
        RelationshipOperationParams.builder(TrackerType.EVENT, UID.of("pTzf9KYMk72")).build();

    List<String> relationships = getRelationships(params);

    assertEquals(expected, relationships);
  }

  @Test
  void shouldOrderRelationshipsByUpdatedAtClientInDescOrder()
      throws ForbiddenException, BadRequestException, NotFoundException {
    RelationshipOperationParams params =
        RelationshipOperationParams.builder(TrackerType.EVENT, UID.of("pTzf9KYMk72"))
            .orderBy("createdAtClient", SortDirection.DESC)
            .build();

    List<String> relationships = getRelationships(params);

    assertEquals(List.of("yZxjxJli9mO", "oLT07jKRu9e"), relationships);
  }

  @Test
  void shouldOrderRelationshipsByUpdatedAtClientInAscOrder()
      throws ForbiddenException, BadRequestException, NotFoundException {
    RelationshipOperationParams params =
        RelationshipOperationParams.builder(TrackerType.EVENT, UID.of("pTzf9KYMk72"))
            .orderBy("createdAtClient", SortDirection.ASC)
            .build();

    List<String> relationships = getRelationships(params);

    assertEquals(List.of("oLT07jKRu9e", "yZxjxJli9mO"), relationships);
  }

  @Test
  void shouldReturnPaginatedRelationshipsGivenNonDefaultPageSize()
      throws ForbiddenException, BadRequestException, NotFoundException {
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
        RelationshipOperationParams.builder(TrackerType.EVENT, UID.of("pTzf9KYMk72")).build();

    Page<String> firstPage =
        relationshipService
            .findRelationships(params, PageParams.single())
            .withMappedItems(IdentifiableObject::getUid);

    assertEquals(
        new Page<>(List.of(expectedOnPage1), 1, 1, null, null, 2), firstPage, "first page");

    Page<String> secondPage =
        relationshipService
            .findRelationships(params, PageParams.of(2, 1, true))
            .withMappedItems(IdentifiableObject::getUid);

    assertEquals(
        new Page<>(List.of(expectedOnPage2), 2, 1, 2L, 1, null), secondPage, "second (last) page");

    Page<Relationship> thirdPage =
        relationshipService.findRelationships(params, PageParams.of(3, 1, true));

    assertEquals(new Page<>(List.of(), 3, 1, 2L, 2, null), thirdPage, "past the last page");
  }

  @Test
  void shouldOrderRelationshipsByCreatedAsc()
      throws ForbiddenException, BadRequestException, NotFoundException {
    Relationship oLT07jKRu9e = get(Relationship.class, "oLT07jKRu9e");
    Relationship yZxjxJli9mO = get(Relationship.class, "yZxjxJli9mO");

    RelationshipOperationParams params =
        RelationshipOperationParams.builder(TrackerType.EVENT, UID.of("pTzf9KYMk72"))
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
      throws ForbiddenException, BadRequestException, NotFoundException {
    Relationship oLT07jKRu9e = get(Relationship.class, "oLT07jKRu9e");
    Relationship yZxjxJli9mO = get(Relationship.class, "yZxjxJli9mO");

    RelationshipOperationParams params =
        RelationshipOperationParams.builder(TrackerType.EVENT, UID.of("pTzf9KYMk72"))
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

  private List<String> getTrackedEntities(TrackedEntityOperationParams params)
      throws ForbiddenException, BadRequestException, NotFoundException {
    return uids(trackedEntityService.findTrackedEntities(params));
  }

  private List<String> getEnrollments(EnrollmentOperationParams params)
      throws ForbiddenException, BadRequestException {
    return uids(enrollmentService.findEnrollments(params));
  }

  private List<String> getTrackerEvents(TrackerEventOperationParams params)
      throws ForbiddenException, BadRequestException {
    return uids(trackerEventService.findEvents(params));
  }

  private List<String> getSingleEvents(SingleEventOperationParams params)
      throws ForbiddenException, BadRequestException {
    return uids(singleEventService.findEvents(params));
  }

  private List<String> getRelationships(RelationshipOperationParams params)
      throws ForbiddenException, BadRequestException, NotFoundException {
    return uids(relationshipService.findRelationships(params));
  }

  private static List<String> uids(List<? extends IdentifiableObject> identifiableObject) {
    return identifiableObject.stream().map(IdentifiableObject::getUid).toList();
  }
}
