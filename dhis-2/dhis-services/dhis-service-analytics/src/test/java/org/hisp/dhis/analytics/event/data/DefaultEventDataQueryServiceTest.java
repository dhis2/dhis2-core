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
package org.hisp.dhis.analytics.event.data;

import static org.hisp.dhis.common.RequestTypeAware.EndpointAction.AGGREGATE;
import static org.hisp.dhis.common.RequestTypeAware.EndpointAction.QUERY;
import static org.hisp.dhis.common.RequestTypeAware.EndpointItem.ENROLLMENT;
import static org.hisp.dhis.common.RequestTypeAware.EndpointItem.EVENT;
import static org.hisp.dhis.test.TestBase.createDataElement;
import static org.hisp.dhis.test.TestBase.createOrganisationUnit;
import static org.hisp.dhis.test.TestBase.createProgram;
import static org.hisp.dhis.test.TestBase.createProgramStage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.analytics.EventOutputType;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.QueryItemLocator;
import org.hisp.dhis.analytics.event.data.queryitem.QueryItemFilterHandlerRegistry;
import org.hisp.dhis.analytics.table.EventAnalyticsColumnName;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.EventDataQueryRequest;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.RepeatableStageParams;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultEventDataQueryServiceTest {
  @Mock private ProgramService programService;
  @Mock private ProgramStageService programStageService;
  @Mock private DataElementService dataElementService;
  @Mock private EventCoordinateService eventCoordinateService;
  @Mock private QueryItemLocator queryItemLocator;
  @Mock private TrackedEntityAttributeService attributeService;
  @Mock private DataQueryService dataQueryService;
  @Mock private OrganisationUnitService organisationUnitService;

  private DefaultEventDataQueryService subject;
  private Program program;

  @BeforeEach
  void setUp() {
    subject =
        new DefaultEventDataQueryService(
            programService,
            programStageService,
            dataElementService,
            eventCoordinateService,
            queryItemLocator,
            attributeService,
            dataQueryService,
            organisationUnitService,
            new QueryItemFilterHandlerRegistry());

    OrganisationUnit ou = createOrganisationUnit('A');
    program = createProgram('A', null, ou);

    when(programService.getProgram(program.getUid())).thenReturn(program);
    when(dataQueryService.getUserOrgUnits(any(), any())).thenReturn(Collections.emptyList());
    lenient()
        .when(
            dataQueryService.getDimension(
                anyString(),
                anyList(),
                any(EventDataQueryRequest.class),
                anyList(),
                anyBoolean(),
                any()))
        .thenReturn(new BaseDimensionalObject("pe", DimensionType.PERIOD, List.of()));
    lenient()
        .when(
            dataQueryService.getDimension(
                anyString(), anyList(), any(), anyList(), anyBoolean(), any(), any(IdScheme.class)))
        .thenReturn(new BaseDimensionalObject("pe", DimensionType.PERIOD, List.of()));
  }

  @Test
  void getFromRequestAcceptsAndNormalizesCreatedDimensionForEnrollmentAggregate() {
    EventDataQueryRequest request =
        baseRequestBuilder(AGGREGATE, ENROLLMENT)
            .dimension(Set.of(Set.of("CREATED:LAST_12_MONTHS")))
            .build();

    subject.getFromRequest(request);

    verify(dataQueryService)
        .getDimension(
            eq("pe"),
            eq(List.of("LAST_12_MONTHS:CREATED")),
            eq(request),
            anyList(),
            eq(true),
            any());
  }

  @Test
  void getFromRequestAcceptsAndNormalizesCreatedFilterForEnrollmentAggregate() {
    EventDataQueryRequest request =
        baseRequestBuilder(AGGREGATE, ENROLLMENT)
            .filter(Set.of(Set.of("CREATED:LAST_12_MONTHS")))
            .build();

    subject.getFromRequest(request);

    verify(dataQueryService)
        .getDimension(
            eq("pe"),
            eq(List.of("LAST_12_MONTHS:CREATED")),
            eq(request.getRelativePeriodDate()),
            anyList(),
            eq(true),
            eq(null),
            any());
  }

  @Test
  void getFromRequestAcceptsAndNormalizesCreatedDimensionForEventQuery() {
    EventDataQueryRequest request =
        baseRequestBuilder(QUERY, EVENT)
            .dimension(Set.of(Set.of("CREATED:LAST_12_MONTHS")))
            .build();

    subject.getFromRequest(request);

    verify(dataQueryService)
        .getDimension(
            eq("pe"),
            eq(List.of("LAST_12_MONTHS:CREATED")),
            eq(request),
            anyList(),
            eq(true),
            any());
  }

  @Test
  void getFromRequestAcceptsAndNormalizesCreatedDimensionForEventAggregate() {
    EventDataQueryRequest request =
        baseRequestBuilder(AGGREGATE, EVENT)
            .dimension(Set.of(Set.of("CREATED:LAST_12_MONTHS")))
            .build();

    subject.getFromRequest(request);

    verify(dataQueryService)
        .getDimension(
            eq("pe"),
            eq(List.of("LAST_12_MONTHS:CREATED")),
            eq(request),
            anyList(),
            eq(true),
            any());
  }

  @Test
  void getFromRequestAcceptsAndNormalizesCompletedDimensionForEnrollmentAggregate() {
    EventDataQueryRequest request =
        baseRequestBuilder(AGGREGATE, ENROLLMENT)
            .dimension(Set.of(Set.of("COMPLETED:LAST_12_MONTHS")))
            .build();

    subject.getFromRequest(request);

    verify(dataQueryService)
        .getDimension(
            eq("pe"),
            eq(List.of("LAST_12_MONTHS:COMPLETED")),
            eq(request),
            anyList(),
            eq(true),
            any());
  }

  @Test
  void getFromRequestAcceptsAndNormalizesCompletedDimensionForEventAggregate() {
    EventDataQueryRequest request =
        baseRequestBuilder(AGGREGATE, EVENT)
            .dimension(Set.of(Set.of("COMPLETED:LAST_12_MONTHS")))
            .build();

    subject.getFromRequest(request);

    verify(dataQueryService)
        .getDimension(
            eq("pe"),
            eq(List.of("LAST_12_MONTHS:COMPLETED")),
            eq(request),
            anyList(),
            eq(true),
            any());
  }

  @Test
  void getFromRequestAcceptsAndNormalizesEventDateDimensionForEnrollmentAggregate() {
    EventDataQueryRequest request =
        baseRequestBuilder(AGGREGATE, ENROLLMENT)
            .dimension(Set.of(Set.of("EVENT_DATE:2022Sep")))
            .build();

    subject.getFromRequest(request);

    verify(dataQueryService)
        .getDimension(
            eq("pe"), eq(List.of("2022Sep:EVENT_DATE")), eq(request), anyList(), eq(true), any());
  }

  @Test
  void getFromRequestRoutesOperatorFilterThroughDateHandler() {
    // Return null for ENROLLMENT_DATE so the flow falls back to getQueryItem
    when(dataQueryService.getDimension(
            eq("ENROLLMENT_DATE"),
            anyList(),
            any(),
            anyList(),
            anyBoolean(),
            any(),
            any(IdScheme.class)))
        .thenReturn(null);

    when(queryItemLocator.getQueryItemFromDimension(
            "ENROLLMENT_DATE", program, EventOutputType.EVENT))
        .thenReturn(createDateQueryItem("enrollmentdate"));

    EventDataQueryRequest request =
        baseRequestBuilder(AGGREGATE, EVENT)
            .filter(Set.of(Set.of("ENROLLMENT_DATE:GT:2023-01-01")))
            .build();

    EventQueryParams params = subject.getFromRequest(request);

    assertEquals(1, params.getItemFilters().size());
    QueryItem filter = params.getItemFilters().get(0);
    assertEquals("enrollmentdate", filter.getItemId());
    assertEquals(1, filter.getFilters().size());
    assertEquals(QueryOperator.GT, filter.getFilters().get(0).getOperator());
    assertEquals("2023-01-01", filter.getFilters().get(0).getFilter());
  }

  @Test
  void getFromRequestRoutesMultiOperatorDateFilterThroughDateHandler() {
    when(dataQueryService.getDimension(
            eq("ENROLLMENT_DATE"),
            anyList(),
            any(),
            anyList(),
            anyBoolean(),
            any(),
            any(IdScheme.class)))
        .thenReturn(null);

    when(queryItemLocator.getQueryItemFromDimension(
            "ENROLLMENT_DATE", program, EventOutputType.EVENT))
        .thenReturn(createDateQueryItem("enrollmentdate"));

    EventDataQueryRequest request =
        baseRequestBuilder(AGGREGATE, EVENT)
            .filter(Set.of(Set.of("ENROLLMENT_DATE:GT:2023-01-01:LT:2023-12-31")))
            .build();

    EventQueryParams params = subject.getFromRequest(request);

    assertEquals(1, params.getItemFilters().size());
    QueryItem filter = params.getItemFilters().get(0);
    assertEquals("enrollmentdate", filter.getItemId());
    assertEquals(2, filter.getFilters().size());
    assertEquals(QueryOperator.GT, filter.getFilters().get(0).getOperator());
    assertEquals("2023-01-01", filter.getFilters().get(0).getFilter());
    assertEquals(QueryOperator.LT, filter.getFilters().get(1).getOperator());
    assertEquals("2023-12-31", filter.getFilters().get(1).getFilter());
  }

  @Test
  void getFromRequestStillNormalizesIsoPeriodForEnrollmentDate() {
    EventDataQueryRequest request =
        baseRequestBuilder(AGGREGATE, EVENT)
            .dimension(Set.of(Set.of("ENROLLMENT_DATE:202301")))
            .build();

    subject.getFromRequest(request);

    verify(dataQueryService)
        .getDimension(
            eq("pe"),
            eq(List.of("202301:ENROLLMENT_DATE")),
            eq(request),
            anyList(),
            eq(true),
            any());
  }

  @Test
  void getFromRequestParsesProgramStatusDimensionForEventQuery() {
    EventDataQueryRequest request =
        baseRequestBuilder(QUERY, EVENT).dimension(Set.of(Set.of("PROGRAM_STATUS:ACTIVE"))).build();

    EventQueryParams params = subject.getFromRequest(request);

    assertEquals(Set.of(EnrollmentStatus.ACTIVE), params.getEnrollmentStatus());
  }

  @Test
  void getFromRequestParsesMultipleProgramStatusesFromDimensionForEventAggregate() {
    EventDataQueryRequest request =
        baseRequestBuilder(AGGREGATE, EVENT)
            .dimension(Set.of(Set.of("PROGRAM_STATUS:ACTIVE;COMPLETED;CANCELLED")))
            .build();

    EventQueryParams params = subject.getFromRequest(request);

    assertEquals(
        Set.of(EnrollmentStatus.ACTIVE, EnrollmentStatus.COMPLETED, EnrollmentStatus.CANCELLED),
        params.getEnrollmentStatus());
  }

  @Test
  void getFromRequestMergesProgramStatusDimensionWithProgramStatusRequestParam() {
    EventDataQueryRequest request =
        baseRequestBuilder(QUERY, EVENT)
            .dimension(Set.of(Set.of("PROGRAM_STATUS:ACTIVE")))
            .enrollmentStatus(Set.of(EnrollmentStatus.COMPLETED))
            .build();

    EventQueryParams params = subject.getFromRequest(request);

    assertEquals(
        Set.of(EnrollmentStatus.ACTIVE, EnrollmentStatus.COMPLETED), params.getEnrollmentStatus());
  }

  @Test
  void getFromRequestRejectsInvalidProgramStatusDimensionValue() {
    EventDataQueryRequest request =
        baseRequestBuilder(QUERY, EVENT)
            .dimension(Set.of(Set.of("PROGRAM_STATUS:INVALID")))
            .build();

    assertThrows(IllegalQueryException.class, () -> subject.getFromRequest(request));
  }

  @Test
  void getFromRequestParsesProgramStatusDimensionForEnrollmentEndpoint() {
    EventDataQueryRequest request =
        baseRequestBuilder(QUERY, ENROLLMENT)
            .dimension(Set.of(Set.of("PROGRAM_STATUS:ACTIVE")))
            .build();

    EventQueryParams params = subject.getFromRequest(request);

    assertEquals(Set.of(EnrollmentStatus.ACTIVE), params.getEnrollmentStatus());
  }

  @Test
  void getFromRequestParsesProgramStatusFilterForEnrollmentEndpoint() {
    EventDataQueryRequest request =
        baseRequestBuilder(QUERY, ENROLLMENT)
            .filter(Set.of(Set.of("PROGRAM_STATUS:ACTIVE;COMPLETED")))
            .build();

    EventQueryParams params = subject.getFromRequest(request);

    assertEquals(
        Set.of(EnrollmentStatus.ACTIVE, EnrollmentStatus.COMPLETED), params.getEnrollmentStatus());
  }

  @Test
  void getFromRequestAcceptsDescendingCreatedSortForEnrollmentEndpoint() {
    EventDataQueryRequest request =
        baseRequestBuilder(QUERY, ENROLLMENT).desc(Set.of("created")).build();

    EventQueryParams params = subject.getFromRequest(request);

    assertEquals(1, params.getDesc().size());
    assertEquals("created", params.getDesc().get(0).getItemId());
  }

  @Test
  void getFromRequestAcceptsAscendingCompletedSortForEnrollmentEndpoint() {
    EventDataQueryRequest request =
        baseRequestBuilder(QUERY, ENROLLMENT).asc(Set.of("completed")).build();

    EventQueryParams params = subject.getFromRequest(request);

    assertEquals(1, params.getAsc().size());
    assertEquals("completeddate", params.getAsc().get(0).getItemId());
  }

  @Test
  void getFromRequestAcceptsDescendingCreatedSortForEventEndpoint() {
    EventDataQueryRequest request =
        baseRequestBuilder(QUERY, EVENT).desc(Set.of("created")).build();

    EventQueryParams params = subject.getFromRequest(request);

    assertEquals(1, params.getDesc().size());
    assertEquals("created", params.getDesc().get(0).getItemId());
  }

  private QueryItem createDateQueryItem(String columnName) {
    return new QueryItem(
        new BaseDimensionalItemObject(columnName),
        program,
        null,
        ValueType.DATE,
        AggregationType.NONE,
        null);
  }

  @Test
  void getFromRequestMergesDateFieldDimensionWithExplicitPeriodDimension() {
    EventDataQueryRequest request =
        baseRequestBuilder(AGGREGATE, EVENT)
            .dimension(Set.of(Set.of("pe:LAST_12_MONTHS"), Set.of("ENROLLMENT_DATE:2021")))
            .build();

    EventQueryParams params = subject.getFromRequest(request);

    assertTrue(params.getDuplicateDimensions().isEmpty());
  }

  @Test
  void getFromRequestPreservesFirstPePositionRelativeToOtherDimensions() {
    OrganisationUnit ou = createOrganisationUnit('B');

    BaseDimensionalObject peDimension =
        new BaseDimensionalObject("pe", DimensionType.PERIOD, List.of());
    BaseDimensionalObject ouDimension =
        new BaseDimensionalObject("ou", DimensionType.ORGANISATION_UNIT, List.of(ou));

    when(dataQueryService.getDimension(
            eq("pe"), eq(List.of("2019", "2020")), any(), anyList(), anyBoolean(), any()))
        .thenReturn(peDimension);
    when(dataQueryService.getDimension(
            eq("ou"), eq(List.of(ou.getUid())), any(), anyList(), anyBoolean(), any()))
        .thenReturn(ouDimension);

    Set<Set<String>> dimensions = new LinkedHashSet<>();
    dimensions.add(Set.of("pe:2019;2020"));
    dimensions.add(Set.of("ou:" + ou.getUid()));

    EventDataQueryRequest request =
        baseRequestBuilder(AGGREGATE, EVENT).dimension(dimensions).build();

    EventQueryParams params = subject.getFromRequest(request);

    List<String> dimensionOrder =
        params.getDimensions().stream().map(DimensionalObject::getDimension).toList();

    assertEquals(List.of("pe", "ou"), dimensionOrder);
  }

  @Test
  void getFromRequestPreservesPePositionWithMixedStaticDateAndExplicitPeriod() {
    OrganisationUnit ou = createOrganisationUnit('B');

    BaseDimensionalObject peDimension =
        new BaseDimensionalObject("pe", DimensionType.PERIOD, List.of());
    BaseDimensionalObject ouDimension =
        new BaseDimensionalObject("ou", DimensionType.ORGANISATION_UNIT, List.of(ou));

    when(dataQueryService.getDimension(
            eq("pe"),
            eq(List.of("2021:ENROLLMENT_DATE", "LAST_12_MONTHS")),
            any(),
            anyList(),
            anyBoolean(),
            any()))
        .thenReturn(peDimension);
    when(dataQueryService.getDimension(
            eq("ou"), eq(List.of(ou.getUid())), any(), anyList(), anyBoolean(), any()))
        .thenReturn(ouDimension);

    Set<Set<String>> dimensions = new LinkedHashSet<>();
    dimensions.add(Set.of("ENROLLMENT_DATE:2021"));
    dimensions.add(Set.of("ou:" + ou.getUid()));
    dimensions.add(Set.of("pe:LAST_12_MONTHS"));

    EventDataQueryRequest request =
        baseRequestBuilder(AGGREGATE, EVENT).dimension(dimensions).build();

    EventQueryParams params = subject.getFromRequest(request);

    List<String> dimensionOrder =
        params.getDimensions().stream().map(DimensionalObject::getDimension).toList();

    assertEquals(List.of("pe", "ou"), dimensionOrder);
    verify(dataQueryService, times(1))
        .getDimension(
            eq("pe"),
            eq(List.of("2021:ENROLLMENT_DATE", "LAST_12_MONTHS")),
            any(),
            anyList(),
            anyBoolean(),
            any());
  }

  @Test
  void getFromRequestMergesPeInputsInFiltersUsingNormalizedFlow() {
    BaseDimensionalObject peFilter =
        new BaseDimensionalObject("pe", DimensionType.PERIOD, List.of());

    when(dataQueryService.getDimension(
            eq("pe"),
            eq(List.of("2021:ENROLLMENT_DATE", "LAST_12_MONTHS")),
            any(),
            anyList(),
            anyBoolean(),
            any(),
            any(IdScheme.class)))
        .thenReturn(peFilter);

    Set<Set<String>> filters = new LinkedHashSet<>();
    filters.add(Set.of("ENROLLMENT_DATE:2021"));
    filters.add(Set.of("pe:LAST_12_MONTHS"));

    EventDataQueryRequest request = baseRequestBuilder(AGGREGATE, EVENT).filter(filters).build();

    EventQueryParams params = subject.getFromRequest(request);

    assertEquals(1, params.getFilters().size());
    assertEquals("pe", params.getFilters().get(0).getDimension());
    verify(dataQueryService, times(1))
        .getDimension(
            eq("pe"),
            eq(List.of("2021:ENROLLMENT_DATE", "LAST_12_MONTHS")),
            any(),
            anyList(),
            anyBoolean(),
            any(),
            any(IdScheme.class));
  }

  @Test
  void getFromRequestResolvesEnrollmentOuAsDimension() {
    OrganisationUnit ouA = createOrganisationUnit('B');
    OrganisationUnit ouB = createOrganisationUnit('C');

    BaseDimensionalObject ouDimension =
        new BaseDimensionalObject("ou", DimensionType.ORGANISATION_UNIT, List.of(ouA, ouB));

    when(dataQueryService.getDimension(
            eq("ou"),
            eq(List.of(ouA.getUid(), ouB.getUid())),
            any(),
            anyList(),
            anyBoolean(),
            any()))
        .thenReturn(ouDimension);

    EventDataQueryRequest request =
        baseRequestBuilder(AGGREGATE, EVENT)
            .dimension(Set.of(Set.of("ENROLLMENT_OU:" + ouA.getUid() + ";" + ouB.getUid())))
            .build();

    EventQueryParams params = subject.getFromRequest(request);

    assertTrue(params.hasEnrollmentOuDimension());
    assertEquals(2, params.getEnrollmentOuDimensionItems().size());
    assertTrue(params.getEnrollmentOuDimensionLevels().isEmpty());
  }

  @Test
  void getFromRequestResolvesEnrollmentOuAsFilter() {
    OrganisationUnit ouA = createOrganisationUnit('B');

    BaseDimensionalObject ouDimension =
        new BaseDimensionalObject("ou", DimensionType.ORGANISATION_UNIT, List.of(ouA));

    when(dataQueryService.getDimension(
            eq("ou"),
            eq(List.of(ouA.getUid())),
            any(),
            anyList(),
            anyBoolean(),
            any(),
            any(IdScheme.class)))
        .thenReturn(ouDimension);

    EventDataQueryRequest request =
        baseRequestBuilder(AGGREGATE, EVENT)
            .filter(Set.of(Set.of("ENROLLMENT_OU:" + ouA.getUid())))
            .build();

    EventQueryParams params = subject.getFromRequest(request);

    assertTrue(params.hasEnrollmentOuFilter());
    assertFalse(params.hasEnrollmentOuDimension());
    assertEquals(1, params.getEnrollmentOuFilterItems().size());
    assertTrue(params.getEnrollmentOuFilterLevels().isEmpty());
  }

  @Test
  void getFromRequestResolvesEnrollmentOuLevelAsDimensionLevelConstraint() {
    when(organisationUnitService.getOrganisationUnitLevelByLevelOrUid("m9lBJogzE95")).thenReturn(4);

    EventDataQueryRequest request =
        baseRequestBuilder(AGGREGATE, EVENT)
            .dimension(Set.of(Set.of("ENROLLMENT_OU:LEVEL-m9lBJogzE95")))
            .build();

    EventQueryParams params = subject.getFromRequest(request);

    assertTrue(params.hasEnrollmentOuDimension());
    assertTrue(params.getEnrollmentOuDimensionItems().isEmpty());
    assertEquals(Set.of(4), params.getEnrollmentOuDimensionLevels());
  }

  @Test
  void getFromRequestResolvesMixedEnrollmentOuLevelAndUidAsFilter() {
    OrganisationUnit ouA = createOrganisationUnit('B');
    BaseDimensionalObject ouDimension =
        new BaseDimensionalObject("ou", DimensionType.ORGANISATION_UNIT, List.of(ouA));

    when(organisationUnitService.getOrganisationUnitLevelByLevelOrUid("m9lBJogzE95")).thenReturn(4);
    when(dataQueryService.getDimension(
            eq("ou"),
            eq(List.of(ouA.getUid())),
            any(),
            anyList(),
            anyBoolean(),
            any(),
            any(IdScheme.class)))
        .thenReturn(ouDimension);

    EventDataQueryRequest request =
        baseRequestBuilder(AGGREGATE, EVENT)
            .filter(Set.of(Set.of("ENROLLMENT_OU:" + ouA.getUid() + ";LEVEL-m9lBJogzE95")))
            .build();

    EventQueryParams params = subject.getFromRequest(request);

    assertTrue(params.hasEnrollmentOuFilter());
    assertEquals(1, params.getEnrollmentOuFilterItems().size());
    assertEquals(Set.of(4), params.getEnrollmentOuFilterLevels());
  }

  @Test
  void getFromRequestMergesMultiplePeriodDimensions() {
    BaseDimensionalObject peDimension =
        new BaseDimensionalObject("pe", DimensionType.PERIOD, List.of());

    when(dataQueryService.getDimension(
            eq("pe"),
            eq(List.of("LAST_12_MONTHS", "2021:ENROLLMENT_DATE", "2022")),
            any(),
            anyList(),
            anyBoolean(),
            any()))
        .thenReturn(peDimension);

    Set<Set<String>> dimensions = new LinkedHashSet<>();
    dimensions.add(Set.of("pe:LAST_12_MONTHS"));
    dimensions.add(Set.of("ENROLLMENT_DATE:2021"));
    dimensions.add(Set.of("pe:2022"));

    EventDataQueryRequest request =
        baseRequestBuilder(AGGREGATE, EVENT).dimension(dimensions).build();

    EventQueryParams params = subject.getFromRequest(request);

    List<String> dimensionOrder =
        params.getDimensions().stream().map(DimensionalObject::getDimension).toList();

    assertEquals(List.of("pe"), dimensionOrder);
    verify(dataQueryService, times(1))
        .getDimension(
            eq("pe"),
            eq(List.of("LAST_12_MONTHS", "2021:ENROLLMENT_DATE", "2022")),
            any(),
            anyList(),
            anyBoolean(),
            any());
  }

  @Test
  void getFromRequestDoesNotDuplicateStageDateDimensionAsFilter() {
    ProgramStage programStage = createProgramStage('A', program);
    QueryItem stageDateItem =
        new QueryItem(
            new BaseDimensionalItemObject(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME),
            program,
            null,
            ValueType.DATE,
            AggregationType.NONE,
            null);
    stageDateItem.setProgramStage(programStage);

    when(queryItemLocator.getQueryItemFromDimension(
            eq(programStage.getUid() + ".EVENT_DATE"), eq(program), eq(EventOutputType.EVENT)))
        .thenReturn(stageDateItem);
    when(dataQueryService.getDimension(
            eq(programStage.getUid() + ".EVENT_DATE"),
            anyList(),
            any(EventDataQueryRequest.class),
            anyList(),
            anyBoolean(),
            any()))
        .thenReturn(null);

    EventDataQueryRequest request =
        baseRequestBuilder(QUERY, EVENT)
            .dimension(Set.of(Set.of(programStage.getUid() + ".EVENT_DATE:THIS_YEAR")))
            .build();

    EventQueryParams params = subject.getFromRequest(request);

    assertEquals(1, params.getItems().size());
    assertTrue(params.getItemFilters().isEmpty());
    assertTrue(params.hasStageDateItem());
    assertEquals(2, params.getItems().get(0).getFilters().size());
  }

  @Test
  void getFromRequestAcceptsBooleanValueWithStagePrefix() {
    ProgramStage programStage = createProgramStage('S', program);
    DataElement booleanElement = createDataElement('B', ValueType.BOOLEAN, AggregationType.SUM);

    lenient()
        .when(programStageService.getProgramStage(programStage.getUid()))
        .thenReturn(programStage);
    lenient()
        .when(dataElementService.getDataElement(booleanElement.getUid()))
        .thenReturn(booleanElement);

    EventDataQueryRequest request =
        baseRequestBuilder(AGGREGATE, EVENT)
            .value(programStage.getUid() + "." + booleanElement.getUid())
            .build();

    EventQueryParams params = subject.getFromRequest(request);

    assertEquals(programStage, params.getProgramStage());
    assertEquals(booleanElement.getUid(), params.getValue().getUid());
    assertTrue(params.hasBooleanValueDimension());
  }

  @Test
  void getFromRequestAcceptsTrueOnlyValueWithoutStagePrefix() {
    DataElement trueOnlyElement = createDataElement('T', ValueType.TRUE_ONLY, AggregationType.SUM);

    lenient()
        .when(dataElementService.getDataElement(trueOnlyElement.getUid()))
        .thenReturn(trueOnlyElement);

    EventDataQueryRequest request =
        baseRequestBuilder(AGGREGATE, EVENT).value(trueOnlyElement.getUid()).build();

    EventQueryParams params = subject.getFromRequest(request);

    assertEquals(trueOnlyElement.getUid(), params.getValue().getUid());
    assertTrue(params.hasBooleanValueDimension());
  }

  @Test
  void getFromRequestPromotesStagePrefixedHeaderIntoItemWhenNotInDimensions() {
    ProgramStage programStage = createProgramStage('S', program);
    DataElement dataElement = createDataElement('D', ValueType.NUMBER, AggregationType.SUM);
    QueryItem dataElementItem =
        new QueryItem(
            new BaseDimensionalItemObject(dataElement.getUid()),
            program,
            null,
            ValueType.NUMBER,
            AggregationType.SUM,
            null);
    dataElementItem.setProgramStage(programStage);

    String headerAlias = programStage.getUid() + "." + dataElement.getUid();
    when(queryItemLocator.getQueryItemFromDimension(headerAlias, program, EventOutputType.EVENT))
        .thenReturn(dataElementItem);

    EventDataQueryRequest request =
        baseRequestBuilder(QUERY, EVENT)
            .headers(new LinkedHashSet<>(List.of("enrollmentouname", headerAlias)))
            .build();

    EventQueryParams params = subject.getFromRequest(request);

    assertEquals(1, params.getItems().size());
    assertEquals(dataElement.getUid(), params.getItems().get(0).getItemId());
    assertEquals(programStage.getUid(), params.getItems().get(0).getProgramStage().getUid());
  }

  @Test
  void getFromRequestDoesNotPromoteStagePrefixedHeaderWhenSuffixIsStaticColumn() {
    ProgramStage programStage = createProgramStage('S', program);

    String headerAlias = programStage.getUid() + ".eventstatus";

    EventDataQueryRequest request =
        baseRequestBuilder(QUERY, EVENT).headers(Set.of(headerAlias)).build();

    EventQueryParams params = subject.getFromRequest(request);

    assertTrue(params.getItems().isEmpty());
    verify(queryItemLocator, times(0)).getQueryItemFromDimension(eq(headerAlias), any(), any());
  }

  @Test
  void getFromRequestDoesNotPromoteStagePrefixedHeaderAlreadyPresentAsItem() {
    ProgramStage programStage = createProgramStage('S', program);
    DataElement dataElement = createDataElement('D', ValueType.NUMBER, AggregationType.SUM);
    QueryItem dataElementItem =
        new QueryItem(
            new BaseDimensionalItemObject(dataElement.getUid()),
            program,
            null,
            ValueType.NUMBER,
            AggregationType.SUM,
            null);
    dataElementItem.setProgramStage(programStage);

    String alias = programStage.getUid() + "." + dataElement.getUid();
    when(queryItemLocator.getQueryItemFromDimension(alias, program, EventOutputType.EVENT))
        .thenReturn(dataElementItem);

    EventDataQueryRequest request =
        baseRequestBuilder(QUERY, EVENT)
            .dimension(Set.of(Set.of(alias)))
            .headers(Set.of(alias))
            .build();

    EventQueryParams params = subject.getFromRequest(request);

    assertEquals(1, params.getItems().size());
  }

  @Test
  void getFromRequestSwallowsLocatorFailureForUnresolvableHeaderSuffix() {
    ProgramStage programStage = createProgramStage('S', program);
    String headerAlias = programStage.getUid() + ".notADataElement";

    when(queryItemLocator.getQueryItemFromDimension(headerAlias, program, EventOutputType.EVENT))
        .thenThrow(new IllegalQueryException(ErrorCode.E7224, headerAlias));

    EventDataQueryRequest request =
        baseRequestBuilder(QUERY, EVENT).headers(Set.of(headerAlias)).build();

    EventQueryParams params = subject.getFromRequest(request);

    assertTrue(params.getItems().isEmpty());
  }

  @Test
  void getFromRequestPromotesStageOuDimensionFromStagePrefixedOuNameHeader() {
    ProgramStage programStage = createProgramStage('S', program);

    QueryItem stageOuQueryItem =
        new QueryItem(
            new BaseDimensionalItemObject(EventAnalyticsColumnName.OU_COLUMN_NAME),
            program,
            null,
            ValueType.ORGANISATION_UNIT,
            AggregationType.NONE,
            null);
    stageOuQueryItem.setProgramStage(programStage);

    String stageOuDim = programStage.getUid() + "." + EventAnalyticsColumnName.OU_COLUMN_NAME;
    String stageOuNameHeader = programStage.getUid() + ".ouname";

    when(queryItemLocator.getQueryItemFromDimension(
            stageOuDim, program, EventOutputType.ENROLLMENT))
        .thenReturn(stageOuQueryItem);

    EventDataQueryRequest request =
        baseRequestBuilder(QUERY, ENROLLMENT)
            .headers(new LinkedHashSet<>(List.of("ouname", stageOuNameHeader)))
            .build();

    EventQueryParams params = subject.getFromRequest(request);

    assertEquals(1, params.getItems().size());
    assertEquals(EventAnalyticsColumnName.OU_COLUMN_NAME, params.getItems().get(0).getItemId());
    assertEquals(programStage.getUid(), params.getItems().get(0).getProgramStage().getUid());
  }

  @Test
  void getFromRequestPromotesStageOuDimensionFromStagePrefixedOuCodeHeader() {
    ProgramStage programStage = createProgramStage('S', program);

    QueryItem stageOuQueryItem =
        new QueryItem(
            new BaseDimensionalItemObject(EventAnalyticsColumnName.OU_COLUMN_NAME),
            program,
            null,
            ValueType.ORGANISATION_UNIT,
            AggregationType.NONE,
            null);
    stageOuQueryItem.setProgramStage(programStage);

    String stageOuDim = programStage.getUid() + "." + EventAnalyticsColumnName.OU_COLUMN_NAME;
    String stageOuCodeHeader = programStage.getUid() + ".oucode";

    when(queryItemLocator.getQueryItemFromDimension(
            stageOuDim, program, EventOutputType.ENROLLMENT))
        .thenReturn(stageOuQueryItem);

    EventDataQueryRequest request =
        baseRequestBuilder(QUERY, ENROLLMENT).headers(Set.of(stageOuCodeHeader)).build();

    EventQueryParams params = subject.getFromRequest(request);

    assertEquals(1, params.getItems().size());
    assertEquals(EventAnalyticsColumnName.OU_COLUMN_NAME, params.getItems().get(0).getItemId());
  }

  @Test
  void getFromRequestDoesNotDuplicateStageOuItemWhenAlreadyPresent() {
    ProgramStage programStage = createProgramStage('S', program);

    QueryItem stageOuQueryItem =
        new QueryItem(
            new BaseDimensionalItemObject(EventAnalyticsColumnName.OU_COLUMN_NAME),
            program,
            null,
            ValueType.ORGANISATION_UNIT,
            AggregationType.NONE,
            null);
    stageOuQueryItem.setProgramStage(programStage);

    String stageOuDim = programStage.getUid() + "." + EventAnalyticsColumnName.OU_COLUMN_NAME;
    String stageOuNameHeader = programStage.getUid() + ".ouname";

    when(queryItemLocator.getQueryItemFromDimension(
            stageOuDim, program, EventOutputType.ENROLLMENT))
        .thenReturn(stageOuQueryItem);

    // Force addDimensionsToParams to fall through to the QueryItem path so the stage.ou
    // dimension ends up in params.getItems() (matches real production behaviour — the central
    // dataQueryService does not recognise stage-prefixed ou dimensions).
    when(dataQueryService.getDimension(
            eq(stageOuDim),
            anyList(),
            any(EventDataQueryRequest.class),
            anyList(),
            anyBoolean(),
            any()))
        .thenReturn(null);

    EventDataQueryRequest request =
        baseRequestBuilder(QUERY, ENROLLMENT)
            .dimension(Set.of(Set.of(stageOuDim)))
            .headers(Set.of(stageOuNameHeader))
            .build();

    EventQueryParams params = subject.getFromRequest(request);

    assertEquals(1, params.getItems().size());
  }

  @Test
  void getFromRequestPromotesFlatTrackedEntityAttributeHeaderIntoItem() {
    QueryItem teaItem =
        new QueryItem(
            new BaseDimensionalItemObject("cejWyOfXge6"),
            program,
            null,
            ValueType.TEXT,
            AggregationType.NONE,
            null);

    when(queryItemLocator.getQueryItemFromDimension("cejWyOfXge6", program, EventOutputType.EVENT))
        .thenReturn(teaItem);

    EventDataQueryRequest request =
        baseRequestBuilder(QUERY, EVENT).headers(Set.of("cejWyOfXge6")).build();

    EventQueryParams params = subject.getFromRequest(request);

    assertEquals(1, params.getItems().size());
    assertEquals("cejWyOfXge6", params.getItems().get(0).getItemId());
  }

  @Test
  void getFromRequestPromotesFlatTrackedEntityAttributeHeaderForEnrollmentEndpoint() {
    QueryItem teaItem =
        new QueryItem(
            new BaseDimensionalItemObject("cejWyOfXge6"),
            program,
            null,
            ValueType.TEXT,
            AggregationType.NONE,
            null);

    when(queryItemLocator.getQueryItemFromDimension(
            "cejWyOfXge6", program, EventOutputType.ENROLLMENT))
        .thenReturn(teaItem);

    EventDataQueryRequest request =
        baseRequestBuilder(QUERY, ENROLLMENT).headers(Set.of("cejWyOfXge6")).build();

    EventQueryParams params = subject.getFromRequest(request);

    assertEquals(1, params.getItems().size());
    assertEquals("cejWyOfXge6", params.getItems().get(0).getItemId());
  }

  @Test
  void getFromRequestDoesNotPromoteFlatHeaderWhenStaticColumn() {
    EventDataQueryRequest request =
        baseRequestBuilder(QUERY, EVENT).headers(Set.of("eventdate")).build();

    EventQueryParams params = subject.getFromRequest(request);

    assertTrue(params.getItems().isEmpty());
    verify(queryItemLocator, times(0)).getQueryItemFromDimension(eq("eventdate"), any(), any());
  }

  @Test
  void getFromRequestDoesNotPromoteFlatHeaderAlreadyPresentAsItem() {
    QueryItem teaItem =
        new QueryItem(
            new BaseDimensionalItemObject("cejWyOfXge6"),
            program,
            null,
            ValueType.TEXT,
            AggregationType.NONE,
            null);

    when(queryItemLocator.getQueryItemFromDimension("cejWyOfXge6", program, EventOutputType.EVENT))
        .thenReturn(teaItem);

    EventDataQueryRequest request =
        baseRequestBuilder(QUERY, EVENT)
            .dimension(Set.of(Set.of("cejWyOfXge6")))
            .headers(Set.of("cejWyOfXge6"))
            .build();

    EventQueryParams params = subject.getFromRequest(request);

    assertEquals(1, params.getItems().size());
  }

  @Test
  void getFromRequestSwallowsLocatorFailureForUnresolvableFlatHeader() {
    when(queryItemLocator.getQueryItemFromDimension("notReal", program, EventOutputType.EVENT))
        .thenThrow(new IllegalQueryException(ErrorCode.E7224, "notReal"));

    EventDataQueryRequest request =
        baseRequestBuilder(QUERY, EVENT).headers(Set.of("notReal")).build();

    EventQueryParams params = subject.getFromRequest(request);

    assertTrue(params.getItems().isEmpty());
  }

  @Test
  void getFromRequestDoesNotPromoteRepeatableStageOffsetHeaderAlreadyPresentAsItem() {
    // Repeatable-stage offset dimensions like uvMKOn1oWvd[0].DX4LVYeP7bw land on
    // params.getItems() with their RepeatableStageParams set. Headers using the same [N].uid
    // notation must dedup against them, otherwise a second QueryItem with the same offset is
    // promoted and the validator raises E7243 (duplicate stage dimension identifier).
    ProgramStage programStage = createProgramStage('S', program);
    DataElement dataElement = createDataElement('D', ValueType.NUMBER, AggregationType.SUM);

    String offsetDim0 = programStage.getUid() + "[0]." + dataElement.getUid();
    String offsetDim1 = programStage.getUid() + "[1]." + dataElement.getUid();

    // Force the dimension path to fall through to getQueryItem so the offset items land on
    // params.getItems() with their RepeatableStageParams — matching production behaviour where
    // dataQueryService.getDimension does not recognise stage-offset notation.
    when(dataQueryService.getDimension(
            eq(offsetDim0),
            anyList(),
            any(EventDataQueryRequest.class),
            anyList(),
            anyBoolean(),
            any()))
        .thenReturn(null);
    when(dataQueryService.getDimension(
            eq(offsetDim1),
            anyList(),
            any(EventDataQueryRequest.class),
            anyList(),
            anyBoolean(),
            any()))
        .thenReturn(null);

    when(queryItemLocator.getQueryItemFromDimension(offsetDim0, program, EventOutputType.EVENT))
        .thenAnswer(inv -> newOffsetItem(programStage, dataElement, 0, offsetDim0));
    when(queryItemLocator.getQueryItemFromDimension(offsetDim1, program, EventOutputType.EVENT))
        .thenAnswer(inv -> newOffsetItem(programStage, dataElement, 1, offsetDim1));

    Set<Set<String>> dimension = new LinkedHashSet<>();
    dimension.add(Set.of(offsetDim0));
    dimension.add(Set.of(offsetDim1));

    EventDataQueryRequest request =
        baseRequestBuilder(QUERY, EVENT)
            .dimension(dimension)
            .headers(new LinkedHashSet<>(List.of(offsetDim0, offsetDim1)))
            .build();

    EventQueryParams params = subject.getFromRequest(request);

    assertEquals(2, params.getItems().size());
    assertTrue(params.getDuplicateStageDimensionIdentifiers().isEmpty());
  }

  private QueryItem newOffsetItem(
      ProgramStage stage, DataElement de, int offset, String dimension) {
    QueryItem qi =
        new QueryItem(
            new BaseDimensionalItemObject(de.getUid()),
            program,
            null,
            ValueType.NUMBER,
            AggregationType.SUM,
            null);
    qi.setProgramStage(stage);
    qi.setRepeatableStageParams(RepeatableStageParams.of(offset, dimension));
    return qi;
  }

  @Test
  void getFromRequestDoesNotPromoteStagePrefixedHeaderAlreadyPresentAsDimension() {
    // Stage-prefixed category / COGS dimensions are stored on params.getDimensions(), not on
    // params.getItems(). The header dedup must still recognise them so we don't synthesise a
    // duplicate (and value-type-less) QueryItem that crashes downstream grid header building.
    String stageCategory = "kO3z4Dhc038.LFsZ8v5v7rq";

    BaseDimensionalObject stageCategoryDim = new BaseDimensionalObject();
    stageCategoryDim.setUid(stageCategory);
    stageCategoryDim.setDimensionType(DimensionType.CATEGORY);
    stageCategoryDim.setDimensionName("LFsZ8v5v7rq");

    when(dataQueryService.getDimension(
            eq(stageCategory),
            anyList(),
            any(EventDataQueryRequest.class),
            anyList(),
            anyBoolean(),
            any()))
        .thenReturn(stageCategoryDim);

    EventDataQueryRequest request =
        baseRequestBuilder(QUERY, EVENT)
            .dimension(Set.of(Set.of(stageCategory + ":CW81uF03hvV;B3nxOazOO2G")))
            .headers(Set.of(stageCategory))
            .build();

    EventQueryParams params = subject.getFromRequest(request);

    assertTrue(params.getItems().isEmpty());
    verify(queryItemLocator, times(0)).getQueryItemFromDimension(eq(stageCategory), any(), any());
  }

  @Test
  void getFromRequestDoesNotPromoteFlatHeaderOnEventAggregateEndpoint() {
    EventDataQueryRequest request =
        baseRequestBuilder(AGGREGATE, EVENT).headers(Set.of("cejWyOfXge6")).build();

    EventQueryParams params = subject.getFromRequest(request);

    assertTrue(params.getItems().isEmpty());
    verify(queryItemLocator, times(0)).getQueryItemFromDimension(eq("cejWyOfXge6"), any(), any());
  }

  @Test
  void getFromRequestDoesNotPromoteFlatHeaderOnEnrollmentAggregateEndpoint() {
    EventDataQueryRequest request =
        baseRequestBuilder(AGGREGATE, ENROLLMENT).headers(Set.of("cejWyOfXge6")).build();

    EventQueryParams params = subject.getFromRequest(request);

    assertTrue(params.getItems().isEmpty());
    verify(queryItemLocator, times(0)).getQueryItemFromDimension(eq("cejWyOfXge6"), any(), any());
  }

  @Test
  void getFromRequestPromotesStageEventDateDimensionFromStagePrefixedEventDateHeader() {
    ProgramStage programStage = createProgramStage('S', program);

    QueryItem stageEventDateItem =
        new QueryItem(
            new BaseDimensionalItemObject(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME),
            program,
            null,
            ValueType.DATE,
            AggregationType.NONE,
            null);
    stageEventDateItem.setProgramStage(programStage);

    String stageEventDateDim = programStage.getUid() + ".EVENT_DATE";
    String stageEventDateHeader = programStage.getUid() + ".eventdate";

    when(queryItemLocator.getQueryItemFromDimension(
            stageEventDateDim, program, EventOutputType.ENROLLMENT))
        .thenReturn(stageEventDateItem);

    EventDataQueryRequest request =
        baseRequestBuilder(QUERY, ENROLLMENT).headers(Set.of(stageEventDateHeader)).build();

    EventQueryParams params = subject.getFromRequest(request);

    assertEquals(1, params.getItems().size());
    assertEquals(
        EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME, params.getItems().get(0).getItemId());
    assertEquals(programStage.getUid(), params.getItems().get(0).getProgramStage().getUid());
  }

  @Test
  void getFromRequestPromotesStageEventStatusDimensionFromStagePrefixedEventStatusHeader() {
    ProgramStage programStage = createProgramStage('S', program);

    QueryItem stageEventStatusItem =
        new QueryItem(
            new BaseDimensionalItemObject(EventAnalyticsColumnName.EVENT_STATUS_COLUMN_NAME),
            program,
            null,
            ValueType.TEXT,
            AggregationType.NONE,
            null);
    stageEventStatusItem.setProgramStage(programStage);

    String stageEventStatusDim = programStage.getUid() + ".EVENT_STATUS";
    String stageEventStatusHeader = programStage.getUid() + ".eventstatus";

    when(queryItemLocator.getQueryItemFromDimension(
            stageEventStatusDim, program, EventOutputType.ENROLLMENT))
        .thenReturn(stageEventStatusItem);

    EventDataQueryRequest request =
        baseRequestBuilder(QUERY, ENROLLMENT).headers(Set.of(stageEventStatusHeader)).build();

    EventQueryParams params = subject.getFromRequest(request);

    assertEquals(1, params.getItems().size());
    assertEquals(
        EventAnalyticsColumnName.EVENT_STATUS_COLUMN_NAME, params.getItems().get(0).getItemId());
    assertEquals(programStage.getUid(), params.getItems().get(0).getProgramStage().getUid());
  }

  @Test
  void getFromRequestPromotesStageScheduledDateDimensionFromStagePrefixedScheduledDateHeader() {
    ProgramStage programStage = createProgramStage('S', program);

    QueryItem stageScheduledDateItem =
        new QueryItem(
            new BaseDimensionalItemObject(EventAnalyticsColumnName.SCHEDULED_DATE_COLUMN_NAME),
            program,
            null,
            ValueType.DATE,
            AggregationType.NONE,
            null);
    stageScheduledDateItem.setProgramStage(programStage);

    String stageScheduledDateDim = programStage.getUid() + ".SCHEDULED_DATE";
    String stageScheduledDateHeader = programStage.getUid() + ".scheduleddate";

    when(queryItemLocator.getQueryItemFromDimension(
            stageScheduledDateDim, program, EventOutputType.ENROLLMENT))
        .thenReturn(stageScheduledDateItem);

    EventDataQueryRequest request =
        baseRequestBuilder(QUERY, ENROLLMENT).headers(Set.of(stageScheduledDateHeader)).build();

    EventQueryParams params = subject.getFromRequest(request);

    assertEquals(1, params.getItems().size());
    assertEquals(
        EventAnalyticsColumnName.SCHEDULED_DATE_COLUMN_NAME, params.getItems().get(0).getItemId());
    assertEquals(programStage.getUid(), params.getItems().get(0).getProgramStage().getUid());
  }

  @Test
  void getFromRequestRejectsNonNumericAndNonBooleanValueTypes() {
    DataElement textElement = createDataElement('X', ValueType.TEXT, AggregationType.NONE);
    DataElement dateElement = createDataElement('D', ValueType.DATE, AggregationType.NONE);

    lenient().when(dataElementService.getDataElement(textElement.getUid())).thenReturn(textElement);
    lenient().when(dataElementService.getDataElement(dateElement.getUid())).thenReturn(dateElement);

    EventDataQueryRequest textRequest =
        baseRequestBuilder(AGGREGATE, EVENT).value(textElement.getUid()).build();
    EventDataQueryRequest dateRequest =
        baseRequestBuilder(AGGREGATE, EVENT).value(dateElement.getUid()).build();

    IllegalQueryException textException =
        assertThrows(IllegalQueryException.class, () -> subject.getFromRequest(textRequest));
    IllegalQueryException dateException =
        assertThrows(IllegalQueryException.class, () -> subject.getFromRequest(dateRequest));

    assertEquals(ErrorCode.E7223, textException.getErrorCode());
    assertEquals(ErrorCode.E7223, dateException.getErrorCode());
  }

  private EventDataQueryRequest.EventDataQueryRequestBuilder baseRequestBuilder(
      org.hisp.dhis.common.RequestTypeAware.EndpointAction action,
      org.hisp.dhis.common.RequestTypeAware.EndpointItem item) {
    return EventDataQueryRequest.builder()
        .program(program.getUid())
        .endpointAction(action)
        .endpointItem(item)
        .outputType(item == ENROLLMENT ? EventOutputType.ENROLLMENT : EventOutputType.EVENT)
        .dimension(Collections.emptySet())
        .filter(Collections.emptySet());
  }
}
