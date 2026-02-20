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
import static org.hisp.dhis.test.TestBase.createOrganisationUnit;
import static org.hisp.dhis.test.TestBase.createProgram;
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
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.EventDataQueryRequest;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
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
  void getFromRequestRejectsCreatedDateDimensionForEnrollmentAggregate() {
    EventDataQueryRequest request =
        baseRequestBuilder(AGGREGATE, ENROLLMENT)
            .dimension(Set.of(Set.of("CREATED_DATE:LAST_12_MONTHS")))
            .build();

    IllegalQueryException ex =
        assertThrows(IllegalQueryException.class, () -> subject.getFromRequest(request));
    assertEquals(ErrorCode.E7222, ex.getErrorCode());
  }

  @Test
  void getFromRequestRejectsCreatedDateFilterForEnrollmentAggregate() {
    EventDataQueryRequest request =
        baseRequestBuilder(AGGREGATE, ENROLLMENT)
            .filter(Set.of(Set.of("CREATED_DATE:LAST_12_MONTHS")))
            .build();

    IllegalQueryException ex =
        assertThrows(IllegalQueryException.class, () -> subject.getFromRequest(request));
    assertEquals(ErrorCode.E7222, ex.getErrorCode());
  }

  @Test
  void getFromRequestRejectsCreatedDateDimensionForEventQueryEndpoint() {
    EventDataQueryRequest request =
        baseRequestBuilder(QUERY, EVENT)
            .dimension(Set.of(Set.of("CREATED_DATE:LAST_12_MONTHS")))
            .build();

    IllegalQueryException ex =
        assertThrows(IllegalQueryException.class, () -> subject.getFromRequest(request));
    assertEquals(ErrorCode.E7222, ex.getErrorCode());
  }

  @Test
  void getFromRequestAcceptsAndNormalizesCreatedDateDimensionForEventAggregate() {
    EventDataQueryRequest request =
        baseRequestBuilder(AGGREGATE, EVENT)
            .dimension(Set.of(Set.of("CREATED_DATE:LAST_12_MONTHS")))
            .build();

    subject.getFromRequest(request);

    verify(dataQueryService)
        .getDimension(
            eq("pe"),
            eq(List.of("LAST_12_MONTHS:CREATED_DATE")),
            eq(request),
            anyList(),
            eq(true),
            any());
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
