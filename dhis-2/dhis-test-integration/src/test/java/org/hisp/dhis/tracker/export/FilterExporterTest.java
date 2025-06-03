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
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;
import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.TestSetup;
import org.hisp.dhis.tracker.export.event.EventOperationParams;
import org.hisp.dhis.tracker.export.event.EventService;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityOperationParams;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityService;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** Tests filtering by attribute and data values in exporter services. */
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FilterExporterTest extends PostgresIntegrationTestBase {
  @Autowired private TestSetup testSetup;

  @Autowired private TrackedEntityService trackedEntityService;
  @Autowired private EventService eventService;

  @Autowired private IdentifiableObjectManager manager;

  private OrganisationUnit orgUnit;
  private ProgramStage programStage;
  private Program program;
  private TrackedEntityType trackedEntityType;

  private User importUser;

  private EventOperationParams.EventOperationParamsBuilder operationParamsBuilder;

  @BeforeAll
  void setUp() throws IOException {
    testSetup.importMetadata();

    importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);

    testSetup.importTrackerData();
    orgUnit = get(OrganisationUnit.class, "h4w96yEMlzO");
    programStage = get(ProgramStage.class, "NpsdDv6kKSO");
    program = programStage.getProgram();
    trackedEntityType = get(TrackedEntityType.class, "ja8NY4PW7Xm");

    // to test that events are only returned if the user has read access to ALL COs of an events COC
    CategoryOption categoryOption = get(CategoryOption.class, "yMj2MnmNI8L");
    categoryOption.getSharing().setOwner("o1HMTIzBGo7");
    manager.update(categoryOption);
    manager.flush();
  }

  @BeforeEach
  void setUpUserAndParams() {
    // needed as some tests are run using another user (injectSecurityContext) while most tests
    // expect to be run by the importUser
    injectSecurityContextUser(importUser);

    operationParamsBuilder = EventOperationParams.builder().orgUnit(orgUnit).orgUnitMode(SELECTED);
  }

  @Test
  void shouldExportEmptyTrackedEntitiesIfFilterDoesNotMatch()
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityAttribute attr = get(TrackedEntityAttribute.class, "V66aa7a2122");
    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnit)
            .orgUnitMode(SELECTED)
            .trackedEntityType(UID.of("ja8NY4PW7Xm"))
            .filterBy(UID.of(attr), List.of(new QueryFilter(QueryOperator.LIKE, "Does not exist")))
            .build();

    List<String> trackedEntities = getTrackedEntities(params);

    assertIsEmpty(trackedEntities);
  }

  @Test
  void shouldExportTrackedEntitiesWhenFilteringByNumberAttributesUsingEq()
      throws ForbiddenException, BadRequestException, NotFoundException {
    // shows number semantics are applied since 170.0 == 170, this would fail if we were to treat
    // value type number as text "170.0" != "170"
    TrackedEntityAttribute attr = get(TrackedEntityAttribute.class, "V66aa7a2122");
    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnit)
            .orgUnitMode(SELECTED)
            .trackedEntityType(UID.of("ja8NY4PW7Xm"))
            .filterBy(UID.of(attr), List.of(new QueryFilter(QueryOperator.EQ, "170")))
            .build();

    List<String> trackedEntities = getTrackedEntities(params);

    assertContainsOnly(List.of("QS6w44flWAf"), trackedEntities);
  }

  @Test
  void shouldExportTrackedEntitiesWhenFilteringByTextAttributesUsingEq()
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnit)
            .orgUnitMode(SELECTED)
            .trackedEntityType(UID.of("ja8NY4PW7Xm"))
            .filterBy(
                UID.of("toUpdate000"), List.of(new QueryFilter(QueryOperator.EQ, "summer day")))
            .build();

    List<String> trackedEntities = getTrackedEntities(params);

    assertContainsOnly(List.of("QS6w44flWAf", "mHWCacsGYYn", "QesgJkTyTCk"), trackedEntities);
  }

  @Test
  void shouldExportTrackedEntitiesWhenFilteringByIntegerAttributesUsingIn()
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityAttribute attr = get(TrackedEntityAttribute.class, "integerAttr");
    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnit)
            .orgUnitMode(SELECTED)
            .trackedEntityType(UID.of("ja8NY4PW7Xm"))
            .filterBy(UID.of(attr), List.of(new QueryFilter(QueryOperator.IN, "70;72")))
            .build();

    List<String> trackedEntities = getTrackedEntities(params);

    assertContainsOnly(List.of("dUE514NMOlo", "mHWCacsGYYn"), trackedEntities);
  }

  @Test
  void shouldExportTrackedEntitiesWhenFilteringByTextAttributesUsingIn()
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityAttribute attr = get(TrackedEntityAttribute.class, "toUpdate000");
    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnit)
            .orgUnitMode(SELECTED)
            .trackedEntityType(UID.of("ja8NY4PW7Xm"))
            .filterBy(
                UID.of(attr), List.of(new QueryFilter(QueryOperator.IN, "Summer day;rainy Day")))
            .build();

    List<String> trackedEntities = getTrackedEntities(params);

    assertContainsOnly(
        List.of("QS6w44flWAf", "mHWCacsGYYn", "QesgJkTyTCk", "dUE514NMOlo"), trackedEntities);
  }

  @Test
  void shouldExportTrackedEntitiesWhenFilteringByIntegerAttributesUsingStartsWith()
      throws ForbiddenException, BadRequestException, NotFoundException {
    // Operators sw is based on SQL like which is supported for numeric value types by not casting
    // values i.e. treating them as strings
    TrackedEntityAttribute attr = get(TrackedEntityAttribute.class, "integerAttr");
    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnit)
            .orgUnitMode(SELECTED)
            .filterBy(UID.of(attr), List.of(new QueryFilter(QueryOperator.SW, "7")))
            .build();

    List<String> trackedEntities = getTrackedEntities(params);

    assertContainsOnly(List.of("dUE514NMOlo", "mHWCacsGYYn"), trackedEntities);
  }

  @Test
  void shouldExportTrackedEntitiesWhenFilteringByTextAttributesUsingStartsWith()
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityAttribute attr = get(TrackedEntityAttribute.class, "toUpdate000");
    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnit)
            .orgUnitMode(SELECTED)
            .filterBy(UID.of(attr), List.of(new QueryFilter(QueryOperator.SW, "RAiny")))
            .build();

    List<String> trackedEntities = getTrackedEntities(params);

    assertContainsOnly(List.of("dUE514NMOlo"), trackedEntities);
  }

  @Test
  void shouldExportTrackedEntitiesWhenFilteringByTextAttributesUsingLike()
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityAttribute attr = get(TrackedEntityAttribute.class, "notUpdated0");
    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnit)
            .orgUnitMode(SELECTED)
            .filterBy(UID.of(attr), List.of(new QueryFilter(QueryOperator.LIKE, "0% \\Winter's")))
            .build();

    List<String> trackedEntities = getTrackedEntities(params);

    assertContainsOnly(List.of("dUE514NMOlo"), trackedEntities);
  }

  @Test
  void shouldExportTrackedEntitiesWhenFilteringByIntegerAttributesUsingLessThan()
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityAttribute attr = get(TrackedEntityAttribute.class, "integerAttr");
    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnit)
            .orgUnitMode(SELECTED)
            .filterBy(UID.of(attr), List.of(new QueryFilter(QueryOperator.LT, "71")))
            .build();

    List<String> trackedEntities = getTrackedEntities(params);

    assertContainsOnly(List.of("dUE514NMOlo"), trackedEntities);
  }

  @Test
  void shouldExportTrackedEntitiesWhenFilteringByIntegerAttributesUsingRange()
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnit)
            .orgUnitMode(SELECTED)
            .filterBy(
                UID.of("integerAttr"),
                List.of(
                    new QueryFilter(QueryOperator.LT, "77"),
                    new QueryFilter(QueryOperator.GT, "8")))
            .build();

    List<String> trackedEntities = getTrackedEntities(params);

    assertContainsOnly(List.of("dUE514NMOlo", "mHWCacsGYYn"), trackedEntities);
  }

  @Test
  void shouldExportTrackedEntitiesWhenFilteringByTextAttributesUsingLessThan()
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityAttribute attr = get(TrackedEntityAttribute.class, "toUpdate000");
    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnit)
            .orgUnitMode(SELECTED)
            .filterBy(UID.of(attr), List.of(new QueryFilter(QueryOperator.LT, "summer day")))
            .build();

    List<String> trackedEntities = getTrackedEntities(params);

    assertContainsOnly(List.of("dUE514NMOlo"), trackedEntities);
  }

  @Test
  void shouldExportTrackedEntitiesWhenFilteringByTextAttributesUsingNotNull()
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityAttribute attr = get(TrackedEntityAttribute.class, "notUpdated0");
    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnit)
            .orgUnitMode(SELECTED)
            .filterBy(UID.of(attr), List.of(new QueryFilter(QueryOperator.NNULL)))
            .build();

    List<String> trackedEntities = getTrackedEntities(params);

    assertContainsOnly(List.of("dUE514NMOlo"), trackedEntities);
  }

  @Test
  void shouldExportTrackedEntitiesWhenFilteringByIntegerAttributesUsingNotNull()
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityAttribute attr = get(TrackedEntityAttribute.class, "integerAttr");
    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnit)
            .orgUnitMode(SELECTED)
            .trackedEntities(Set.of(UID.of("dUE514NMOlo")))
            .filterBy(UID.of(attr), List.of(new QueryFilter(QueryOperator.NNULL)))
            .build();

    List<String> trackedEntities = getTrackedEntities(params);

    assertContainsOnly(List.of("dUE514NMOlo"), trackedEntities);
  }

  @Test
  void shouldOnlyIncludeTrackedEntitiesWithGivenAttributeWhenFilterAttributeHasNoQueryFilter()
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnit)
            .orgUnitMode(SELECTED)
            .filterBy(UID.of("notUpdated0"))
            .build();

    List<String> trackedEntities = getTrackedEntities(params);

    assertContainsOnly(List.of("dUE514NMOlo"), trackedEntities);
  }

  @Test
  void
      shouldExportTrackedEntitiesWhenFilteringByAttributesWithMultipleFiltersOnDifferentAttributes()
          throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnit)
            .orgUnitMode(SELECTED)
            .filterBy(
                UID.of("toUpdate000"), List.of(new QueryFilter(QueryOperator.EQ, "rainy day")))
            .filterBy(UID.of("integerAttr"), List.of(new QueryFilter(QueryOperator.EQ, "70")))
            .build();

    List<String> trackedEntities = getTrackedEntities(params);

    assertContainsOnly(List.of("dUE514NMOlo"), trackedEntities);
  }

  @Test
  void shouldExportTrackedEntitiesWhenFilteringByMultipleFiltersOnTheSameAttribute()
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .organisationUnits(orgUnit)
            .orgUnitMode(SELECTED)
            .filterBy(
                UID.of("toUpdate000"),
                List.of(
                    new QueryFilter(QueryOperator.LIKE, "day"),
                    new QueryFilter(QueryOperator.LIKE, "in")))
            .build();

    List<String> trackedEntities = getTrackedEntities(params);

    assertContainsOnly(List.of("dUE514NMOlo"), trackedEntities);
  }

  @Test
  void shouldExportTrackedEntitiesWhenFilteringByCombiningTwoUnaryOperators()
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .program(UID.of("BFcipDERJnf"))
            .filterBy(
                UID.of("dIVt4l5vIOa"),
                List.of(new QueryFilter(QueryOperator.NNULL), new QueryFilter(QueryOperator.NNULL)))
            .build();

    List<String> trackedEntities = getTrackedEntities(params);

    assertContainsOnly(List.of("dUE514NMOlo"), trackedEntities);
  }

  @Test
  void shouldExportTrackedEntitiesWhenFilteringByCombiningUnaryAndBinaryOperators()
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .program(UID.of("BFcipDERJnf"))
            .filterBy(
                UID.of("toUpdate000"),
                List.of(
                    new QueryFilter(QueryOperator.NNULL),
                    new QueryFilter(QueryOperator.EQ, "rainy day")))
            .build();

    List<String> trackedEntities = getTrackedEntities(params);

    assertContainsOnly(List.of("dUE514NMOlo"), trackedEntities);
  }

  @Test
  void shouldExportTrackedEntitiesWhenFilteringByByNullValues()
      throws ForbiddenException, BadRequestException, NotFoundException {
    TrackedEntityOperationParams params =
        TrackedEntityOperationParams.builder()
            .trackedEntityType(trackedEntityType)
            .filterBy(UID.of("toDelete000"), List.of(new QueryFilter(QueryOperator.NULL)))
            .build();

    List<String> trackedEntities = getTrackedEntities(params);

    assertContainsOnly(List.of("H8732208127"), trackedEntities);
  }

  @Test
  void shouldExportEmptyEventsIfAttributeFilterDoesNotMatch()
      throws ForbiddenException, BadRequestException {
    TrackedEntityAttribute attr = get(TrackedEntityAttribute.class, "V66aa7a2122");
    EventOperationParams params =
        operationParamsBuilder
            .orgUnitMode(ACCESSIBLE)
            .filterByAttribute(UID.of(attr), List.of(new QueryFilter(QueryOperator.EQ, "170000")))
            .build();

    List<String> events = getEvents(params);

    assertIsEmpty(events);
  }

  @Test
  void shouldExportEventsWhenFilteringByNumberAttributesUsingEq()
      throws ForbiddenException, BadRequestException {
    // shows number semantics are applied since 170.0 == 170, this would fail if we were to treat
    // value type number as text "170.0" != "170"
    TrackedEntityAttribute attr = get(TrackedEntityAttribute.class, "V66aa7a2122");
    EventOperationParams params =
        operationParamsBuilder
            .orgUnitMode(ACCESSIBLE)
            .filterByAttribute(UID.of(attr), List.of(new QueryFilter(QueryOperator.EQ, "170")))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("pTzf9KYMk72"), events);
  }

  @Test
  void shouldExportEventsWhenFilteringByTextAttributesUsingEq()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder
            .orgUnit(orgUnit)
            .filterByAttribute(
                UID.of("toUpdate000"), List.of(new QueryFilter(QueryOperator.EQ, "summer day")))
            .build();

    List<String> trackedEntities =
        eventService.findEvents(params).stream()
            .map(event -> event.getEnrollment().getTrackedEntity().getUid())
            .toList();

    assertContainsOnly(List.of("QS6w44flWAf"), trackedEntities);
  }

  @Test
  void shouldExportEventsWhenFilteringByIntegerAttributesUsingIn()
      throws ForbiddenException, BadRequestException {
    TrackedEntityAttribute attr = get(TrackedEntityAttribute.class, "integerAttr");
    EventOperationParams params =
        operationParamsBuilder
            .orgUnitMode(ACCESSIBLE)
            .filterByAttribute(UID.of(attr), List.of(new QueryFilter(QueryOperator.IN, "70;72")))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM", "jxgFyJEMUPf"), events);
  }

  @Test
  void shouldExportEventsWhenFilteringByTextAttributesUsingIn()
      throws ForbiddenException, BadRequestException {
    TrackedEntityAttribute attr = get(TrackedEntityAttribute.class, "toUpdate000");
    EventOperationParams params =
        operationParamsBuilder
            .orgUnitMode(ACCESSIBLE)
            .filterByAttribute(
                UID.of(attr), List.of(new QueryFilter(QueryOperator.IN, "Summer day;rainy Day")))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(
        List.of(
            "YKmfzHdjUDL",
            "LCSfHnurnNB",
            "SbUJzkxKYAG",
            "gvULMgNiAfM",
            "JaRDIvcEcEx",
            "jxgFyJEMUPf",
            "D9PbzJY8bJM",
            "pTzf9KYMk72"),
        events);
  }

  @Test
  void shouldExportEventsWhenFilteringByIntegerAttributesUsingStartsWith()
      throws ForbiddenException, BadRequestException {
    // Operators sw is based on SQL like which is supported for numeric value types by not casting
    // values i.e. treating them as strings
    TrackedEntityAttribute attr = get(TrackedEntityAttribute.class, "integerAttr");
    EventOperationParams params =
        operationParamsBuilder
            .orgUnitMode(ACCESSIBLE)
            .filterByAttribute(UID.of(attr), List.of(new QueryFilter(QueryOperator.SW, "7")))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM", "jxgFyJEMUPf"), events);
  }

  @Test
  void shouldExportEventsWhenFilteringByTextAttributesUsingStartsWith()
      throws ForbiddenException, BadRequestException {
    TrackedEntityAttribute attr = get(TrackedEntityAttribute.class, "toUpdate000");
    EventOperationParams params =
        operationParamsBuilder
            .orgUnitMode(ACCESSIBLE)
            .filterByAttribute(UID.of(attr), List.of(new QueryFilter(QueryOperator.SW, "RAiny")))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM"), events);
  }

  @Test
  void shouldExportEventsWhenFilteringByTextAttributesUsingLike()
      throws ForbiddenException, BadRequestException {
    TrackedEntityAttribute attr = get(TrackedEntityAttribute.class, "notUpdated0");
    EventOperationParams params =
        operationParamsBuilder
            .orgUnitMode(ACCESSIBLE)
            .filterByAttribute(
                UID.of(attr), List.of(new QueryFilter(QueryOperator.LIKE, "0% \\Winter's")))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM"), events);
  }

  @Test
  void shouldExportEventsWhenFilteringByIntegerAttributesUsingLessThan()
      throws ForbiddenException, BadRequestException {
    TrackedEntityAttribute attr = get(TrackedEntityAttribute.class, "integerAttr");
    EventOperationParams params =
        operationParamsBuilder
            .orgUnitMode(ACCESSIBLE)
            .filterByAttribute(UID.of(attr), List.of(new QueryFilter(QueryOperator.LT, "71")))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM"), events);
  }

  @Test
  void shouldExportEventsWhenFilteringByIntegerAttributesUsingRange()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder
            .orgUnit(orgUnit)
            .filterByAttribute(
                UID.of("integerAttr"),
                List.of(
                    new QueryFilter(QueryOperator.LT, "77"),
                    new QueryFilter(QueryOperator.GT, "8")))
            .build();

    List<String> trackedEntities =
        eventService.findEvents(params).stream()
            .map(event -> event.getEnrollment().getTrackedEntity().getUid())
            .toList();

    assertContainsOnly(List.of("dUE514NMOlo"), trackedEntities);
  }

  @Test
  void shouldExportEventsWhenFilteringByTextAttributesUsingLessThan()
      throws ForbiddenException, BadRequestException {
    TrackedEntityAttribute attr = get(TrackedEntityAttribute.class, "toUpdate000");
    EventOperationParams params =
        operationParamsBuilder
            .orgUnitMode(ACCESSIBLE)
            .filterByAttribute(
                UID.of(attr), List.of(new QueryFilter(QueryOperator.LT, "summer day")))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM"), events);
  }

  @Test
  void shouldExportEventsWhenFilteringByTextAttributesUsingNotNull()
      throws ForbiddenException, BadRequestException {
    TrackedEntityAttribute attr = get(TrackedEntityAttribute.class, "notUpdated0");
    EventOperationParams params =
        operationParamsBuilder
            .orgUnitMode(ACCESSIBLE)
            .filterByAttribute(UID.of(attr), List.of(new QueryFilter(QueryOperator.NNULL)))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM"), events);
  }

  @Test
  void shouldExportEventsWhenFilteringByIntegerAttributesUsingNotNull()
      throws ForbiddenException, BadRequestException {
    TrackedEntityAttribute attr = get(TrackedEntityAttribute.class, "integerAttr");
    EventOperationParams params =
        operationParamsBuilder
            .orgUnitMode(ACCESSIBLE)
            .trackedEntity(UID.of("dUE514NMOlo"))
            .filterByAttribute(UID.of(attr), List.of(new QueryFilter(QueryOperator.NNULL)))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM"), events);
  }

  @Test
  void shouldOnlyIncludeEventsWithGivenAttributeWhenFilterAttributeHasNoQueryFilter()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder.orgUnit(orgUnit).filterByAttribute(UID.of("notUpdated0")).build();

    List<String> trackedEntities =
        eventService.findEvents(params).stream()
            .map(event -> event.getEnrollment().getTrackedEntity().getUid())
            .toList();

    assertContainsOnly(List.of("dUE514NMOlo"), trackedEntities);
  }

  @Test
  void shouldExportEventsWhenFilteringByAttributesWithMultipleFiltersOnDifferentAttributes()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder
            .orgUnit(orgUnit)
            .filterByAttribute(
                UID.of("toUpdate000"), List.of(new QueryFilter(QueryOperator.EQ, "rainy day")))
            .filterByAttribute(
                UID.of("integerAttr"), List.of(new QueryFilter(QueryOperator.EQ, "70")))
            .build();

    List<String> trackedEntities =
        eventService.findEvents(params).stream()
            .map(event -> event.getEnrollment().getTrackedEntity().getUid())
            .toList();

    assertContainsOnly(List.of("dUE514NMOlo"), trackedEntities);
  }

  @Test
  void shouldExportEventsWhenFilteringByMultipleFiltersOnTheSameAttribute()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        operationParamsBuilder
            .orgUnit(orgUnit)
            .filterByAttribute(
                UID.of("toUpdate000"),
                List.of(
                    new QueryFilter(QueryOperator.LIKE, "day"),
                    new QueryFilter(QueryOperator.LIKE, "in")))
            .build();

    List<String> trackedEntities =
        eventService.findEvents(params).stream()
            .map(event -> event.getEnrollment().getTrackedEntity().getUid())
            .toList();

    assertContainsOnly(List.of("dUE514NMOlo"), trackedEntities);
  }

  @Test
  void shouldExportEventsWhenFilteringByCombiningTwoUnaryOperators()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        EventOperationParams.builder()
            .enrollments(UID.of("nxP7UnKhomJ", "TvctPPhpD8z"))
            .programStage(programStage)
            .filterByAttribute(
                UID.of("dIVt4l5vIOa"),
                List.of(new QueryFilter(QueryOperator.NNULL), new QueryFilter(QueryOperator.NNULL)))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM"), events);
  }

  @Test
  void shouldExportEventsWhenFilteringByCombiningUnaryAndBinaryOperators()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        EventOperationParams.builder()
            .enrollments(UID.of("nxP7UnKhomJ", "TvctPPhpD8z"))
            .programStage(programStage)
            .filterByAttribute(
                UID.of("toUpdate000"),
                List.of(
                    new QueryFilter(QueryOperator.NNULL),
                    new QueryFilter(QueryOperator.EQ, "rainy day")))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM"), events);
  }

  @Test
  void shouldExportEventsWhenFilteringByByNullValues()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        EventOperationParams.builder()
            .filterByAttribute(UID.of("toDelete000"), List.of(new QueryFilter(QueryOperator.NULL)))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("H0PbzJY8bJG"), events);
  }

  @Test
  void shouldExportEventsWhenFilteringByByNonNullValues()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        EventOperationParams.builder()
            .programStage(programStage)
            .filterByAttribute(UID.of("dIVt4l5vIOa"), List.of(new QueryFilter(QueryOperator.NNULL)))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM"), events);
  }

  @Test
  void shouldExportEmptyEventsIfDataElementFilterDoesNotMatch()
      throws ForbiddenException, BadRequestException {
    DataElement dataElement = dataElement(UID.of("DATAEL00001"));
    EventOperationParams params =
        operationParamsBuilder
            .orgUnitMode(ACCESSIBLE)
            .filterByDataElement(
                UID.of(dataElement), List.of(new QueryFilter(QueryOperator.LIKE, "does not exist")))
            .build();

    List<String> events = getEvents(params);

    assertIsEmpty(events);
  }

  @Test
  void shouldExportEventsWhenFilteringByTextDataElementsUsingLike()
      throws ForbiddenException, BadRequestException {
    DataElement dataElement = dataElement(UID.of("DATAEL00001"));
    EventOperationParams params =
        operationParamsBuilder
            .enrollments(Set.of(UID.of("nxP7UnKhomJ")))
            .programStage(programStage)
            .filterByDataElement(
                UID.of(dataElement), List.of(new QueryFilter(QueryOperator.LIKE, "VaL")))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("pTzf9KYMk72"), events);
  }

  @Test
  void shouldExportEventsWhenFilteringByTextDataElementsUsingSWWithStatusFilter()
      throws ForbiddenException, BadRequestException {
    DataElement dataElement = dataElement(UID.of("DATAEL00001"));

    EventOperationParams params =
        operationParamsBuilder
            .enrollments(Set.of(UID.of("nxP7UnKhomJ")))
            .programStage(programStage)
            .enrollmentStatus(EnrollmentStatus.ACTIVE)
            .filterByDataElement(
                UID.of(dataElement), List.of(new QueryFilter(QueryOperator.SW, "val")))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("pTzf9KYMk72"), events);
  }

  @Test
  void shouldExportEventsWhenFilteringByTextDataElementsUsingEWWithProgramTypeFilter()
      throws ForbiddenException, BadRequestException {
    DataElement dataElement = dataElement(UID.of("DATAEL00001"));

    EventOperationParams params =
        operationParamsBuilder
            .enrollments(Set.of(UID.of("nxP7UnKhomJ")))
            .programStage(programStage)
            .filterByDataElement(
                UID.of(dataElement), List.of(new QueryFilter(QueryOperator.EW, "001")))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("pTzf9KYMk72"), events);
  }

  @Test
  void shouldExportEventsWhenFilteringByTextDataElementsUsingIn()
      throws ForbiddenException, BadRequestException {
    DataElement dataElement = dataElement(UID.of("DATAEL00001"));

    EventOperationParams params =
        operationParamsBuilder
            .enrollments(UID.of("nxP7UnKhomJ", "TvctPPhpD8z"))
            .programStage(programStage)
            .filterByDataElement(
                UID.of(dataElement),
                List.of(new QueryFilter(QueryOperator.IN, "Value00001;Value00002")))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), events);
  }

  @Test
  void shouldExportEventsWhenFilteringByTextDataElementsUsingEqWithCategoryOptionSuperUser()
      throws ForbiddenException, BadRequestException {
    DataElement dataElement = dataElement(UID.of("DATAEL00001"));

    EventOperationParams params =
        operationParamsBuilder
            .enrollments(Set.of(UID.of("nxP7UnKhomJ")))
            .programStage(programStage)
            .program(program)
            .attributeCategoryCombo(UID.of("bjDvmb4bfuf"))
            .attributeCategoryOptions(Set.of(UID.of("xYerKDKCefk")))
            .filterByDataElement(
                UID.of(dataElement), List.of(new QueryFilter(QueryOperator.EQ, "value00001")))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("pTzf9KYMk72"), events);
  }

  @Test
  void shouldExportEventsWhenFilteringByDataElementsUsingEqWithCategoryOptionNotSuperUser()
      throws ForbiddenException, BadRequestException {
    injectSecurityContextUser(
        createAndAddUser(false, "user", Set.of(orgUnit), Set.of(orgUnit), "F_EXPORT_DATA"));
    DataElement dataElement = dataElement(UID.of("DATAEL00002"));

    EventOperationParams params =
        operationParamsBuilder
            .enrollments(Set.of(UID.of("TvctPPhpD8z")))
            .programStage(programStage)
            .program(program)
            .attributeCategoryCombo(UID.of("bjDvmb4bfuf"))
            .attributeCategoryOptions(Set.of(UID.of("xYerKDKCefk")))
            .filterByDataElement(
                UID.of(dataElement), List.of(new QueryFilter(QueryOperator.EQ, "value00002")))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM"), events);
  }

  @Test
  void shouldExportEventsWhenFilteringByDataElementsUsingOptionSetEqual()
      throws ForbiddenException, BadRequestException {
    DataElement dataElement = dataElement(UID.of("DATAEL00005"));
    EventOperationParams params =
        operationParamsBuilder
            .enrollments(Set.of(UID.of("nxP7UnKhomJ")))
            .programStage(programStage)
            .filterByDataElement(
                UID.of(dataElement), List.of(new QueryFilter(QueryOperator.EQ, "option1")))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("pTzf9KYMk72"), events);
  }

  @Test
  void shouldExportEventsWhenFilteringByDataElementsUsingOptionSetIn()
      throws ForbiddenException, BadRequestException {
    DataElement dataElement = dataElement(UID.of("DATAEL00005"));
    EventOperationParams params =
        operationParamsBuilder
            .enrollments(UID.of("nxP7UnKhomJ", "TvctPPhpD8z"))
            .programStage(programStage)
            .filterByDataElement(
                UID.of(dataElement), List.of(new QueryFilter(QueryOperator.IN, "option1;option2")))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM", "pTzf9KYMk72"), events);
  }

  @Test
  void shouldExportEventsWhenFilteringByDataElementsUsingOptionSetLike()
      throws ForbiddenException, BadRequestException {
    DataElement dataElement = dataElement(UID.of("DATAEL00005"));
    EventOperationParams params =
        operationParamsBuilder
            .enrollments(Set.of(UID.of("nxP7UnKhomJ")))
            .programStage(programStage)
            .filterByDataElement(
                UID.of(dataElement), List.of(new QueryFilter(QueryOperator.LIKE, "opt")))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("pTzf9KYMk72"), events);
  }

  @Test
  void shouldExportEventsWhenFilteringByNumericDataElementsUsingComparisonRange()
      throws ForbiddenException, BadRequestException {
    DataElement dataElement = dataElement(UID.of("DATAEL00006"));
    EventOperationParams params =
        operationParamsBuilder
            .enrollments(UID.of("nxP7UnKhomJ", "TvctPPhpD8z"))
            .programStage(programStage)
            .filterByDataElement(
                UID.of(dataElement),
                List.of(
                    new QueryFilter(QueryOperator.LT, "77"),
                    new QueryFilter(QueryOperator.GT, "8")))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM"), events);
  }

  @Test
  void shouldExportEventsWhenFilteringByNumericDataElementsUsingSWAndEW()
      throws ForbiddenException, BadRequestException {
    DataElement dataElement = dataElement(UID.of("DATAEL00006"));
    EventOperationParams params =
        operationParamsBuilder
            .enrollments(UID.of("nxP7UnKhomJ", "TvctPPhpD8z"))
            .programStage(programStage)
            .filterByDataElement(
                UID.of(dataElement), List.of(new QueryFilter(QueryOperator.SW, "7")))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM"), events);
  }

  @Test
  void shouldFilterByEventsNumberDataValueUsingEq() throws ForbiddenException, BadRequestException {
    // shows number semantics are applied since 15.0 == 15, this would fail if we were to treat
    // value type number as text "15.0" != "15"
    EventOperationParams params =
        EventOperationParams.builder()
            .filterByDataElement(
                UID.of("GieVkTxp4HH"), List.of(new QueryFilter(QueryOperator.EQ, "15.0")))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("kWjSezkXHVp", "QRYjLTiJTrA"), events);
  }

  @Test
  void shouldFilterByEventsContainingGivenDataValuesWhenFilteringByNonNullDataValues()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        EventOperationParams.builder()
            .filterByDataElement(
                UID.of("GieVkTxp4HH"), List.of(new QueryFilter(QueryOperator.NNULL)))
            .filterByDataElement(
                UID.of("GieVkTxp4HG"), List.of(new QueryFilter(QueryOperator.NNULL)))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("kWjSezkXHVp", "QRYjLTiJTrA"), events);
  }

  @Test
  void shouldFilterByEventsNotContainingGivenDataValueWhenFilteringByNullDataValues()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        EventOperationParams.builder()
            .enrollments(UID.of("nxP7UnKhomJ", "TvctPPhpD8z"))
            .programStage(programStage)
            .filterByDataElement(
                UID.of("DATAEL00002"), List.of(new QueryFilter(QueryOperator.NULL)))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("pTzf9KYMk72"), events);
  }

  @Test
  void shouldFilterByEventsContainingGivenDataValueWhenCombiningUnaryAndBinaryOperatorsInFilter()
      throws ForbiddenException, BadRequestException {
    DataElement dataElement = dataElement(UID.of("DATAEL00005"));
    EventOperationParams params =
        operationParamsBuilder
            .enrollments(UID.of("nxP7UnKhomJ", "TvctPPhpD8z"))
            .programStage(programStage)
            .filterByDataElement(
                UID.of(dataElement),
                List.of(
                    new QueryFilter(QueryOperator.IN, "option2"),
                    new QueryFilter(QueryOperator.NNULL)))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM"), events);
  }

  @Test
  void shouldFilterByEventsContainingGivenDataValueWhenCombiningTwoUnaryOperatorsInFilter()
      throws ForbiddenException, BadRequestException {
    EventOperationParams params =
        EventOperationParams.builder()
            .enrollments(UID.of("nxP7UnKhomJ", "TvctPPhpD8z"))
            .programStage(programStage)
            .filterByDataElement(
                UID.of("DATAEL00002"),
                List.of(new QueryFilter(QueryOperator.NNULL), new QueryFilter(QueryOperator.NNULL)))
            .build();

    List<String> events = getEvents(params);

    assertContainsOnly(List.of("D9PbzJY8bJM"), events);
  }

  private DataElement dataElement(UID uid) {
    return get(DataElement.class, uid.getValue());
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

  private List<String> getEvents(EventOperationParams params)
      throws ForbiddenException, BadRequestException {
    return uids(eventService.findEvents(params));
  }

  private static List<String> uids(List<? extends BaseIdentifiableObject> identifiableObject) {
    return identifiableObject.stream().map(BaseIdentifiableObject::getUid).toList();
  }
}
