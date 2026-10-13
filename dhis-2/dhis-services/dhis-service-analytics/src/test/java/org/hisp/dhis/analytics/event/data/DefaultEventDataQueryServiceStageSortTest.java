/*
 * Copyright (c) 2004-2026, University of Oslo
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
import static org.hisp.dhis.test.TestBase.createProgramStage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.analytics.EventOutputType;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.QueryItemLocator;
import org.hisp.dhis.analytics.event.data.queryitem.QueryItemFilterHandlerRegistry;
import org.hisp.dhis.common.EventDataQueryRequest;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.RequestTypeAware.EndpointAction;
import org.hisp.dhis.common.RequestTypeAware.EndpointItem;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.legend.LegendSetService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.relationship.RelationshipTypeService;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Parser coverage for stage-prefixed {@code asc}/{@code desc} fields. Wires the real {@link
 * DefaultQueryItemLocator} so the output-name versus dimension-name distinction that stage sorting
 * hinges on is exercised rather than mocked away.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DefaultEventDataQueryServiceStageSortTest {
  @Mock private ProgramService programService;
  @Mock private ProgramStageService programStageService;
  @Mock private DataElementService dataElementService;
  @Mock private EventCoordinateService eventCoordinateService;
  @Mock private TrackedEntityAttributeService attributeService;
  @Mock private ProgramIndicatorService programIndicatorService;
  @Mock private LegendSetService legendSetService;
  @Mock private RelationshipTypeService relationshipTypeService;
  @Mock private DataQueryService dataQueryService;
  @Mock private OrganisationUnitService organisationUnitService;

  private DefaultEventDataQueryService subject;
  private Program program;
  private ProgramStage stage;

  @BeforeEach
  void setUp() {
    QueryItemLocator locator =
        new DefaultQueryItemLocator(
            programStageService,
            dataElementService,
            attributeService,
            programIndicatorService,
            legendSetService,
            relationshipTypeService,
            dataQueryService);
    subject =
        new DefaultEventDataQueryService(
            programService,
            programStageService,
            dataElementService,
            eventCoordinateService,
            locator,
            attributeService,
            dataQueryService,
            organisationUnitService,
            new QueryItemFilterHandlerRegistry());

    OrganisationUnit ou = createOrganisationUnit('A');
    program = createProgram('A', null, ou);
    stage = createProgramStage('A', program);

    when(programService.getProgram(program.getUid())).thenReturn(program);
    when(programStageService.getProgramStage(stage.getUid())).thenReturn(stage);
    when(dataQueryService.getUserOrgUnits(any(), any())).thenReturn(Collections.emptyList());
  }

  @ParameterizedTest
  @CsvSource({
    "ouname, ouname",
    "oucode, oucode",
    "eventdate, occurreddate",
    "scheduleddate, scheduleddate",
    "ou, ou",
    "EVENT_DATE, occurreddate",
    "SCHEDULED_DATE, scheduleddate",
    "OuName, ouname",
    "EventDate, occurreddate"
  })
  void eventQueryResolvesStagePrefixedSortToEventOwnColumn(String suffix, String expectedColumn) {
    EventQueryParams params =
        subject.getFromRequest(request(QUERY, EVENT).asc(Set.of(stagePrefixed(suffix))).build());

    QueryItem sortItem = single(params.getAsc());
    assertEquals(expectedColumn, sortItem.getItemId());
    assertFalse(sortItem.hasProgramStage());
  }

  @ParameterizedTest
  @CsvSource({"ouname", "oucode", "eventdate", "scheduleddate"})
  void eventQueryStagePrefixedSortEqualsBareSort(String suffix) {
    QueryItem stagePrefixedSort =
        single(
            subject
                .getFromRequest(request(QUERY, EVENT).desc(Set.of(stagePrefixed(suffix))).build())
                .getDesc());
    QueryItem bareSort =
        single(
            subject.getFromRequest(request(QUERY, EVENT).desc(Set.of(suffix)).build()).getDesc());

    assertEquals(bareSort, stagePrefixedSort);
  }

  @ParameterizedTest
  @CsvSource({
    "ouname, ouname",
    "oucode, oucode",
    "eventdate, occurreddate",
    "scheduleddate, scheduleddate",
    "ou, ou",
    "EVENT_DATE, occurreddate",
    "SCHEDULED_DATE, scheduleddate",
    "OuName, ouname",
    "EventDate, occurreddate"
  })
  void enrollmentQueryResolvesStagePrefixedSortToStageScopedItem(
      String suffix, String expectedItemId) {
    EventQueryParams params =
        subject.getFromRequest(
            request(QUERY, ENROLLMENT).desc(Set.of(stagePrefixed(suffix))).build());

    QueryItem sortItem = single(params.getDesc());
    assertEquals(expectedItemId, sortItem.getItemId());
    assertEquals(stage, sortItem.getProgramStage());
    assertEquals(program, sortItem.getProgram());
    assertEquals(0, sortItem.getProgramStageOffset());
  }

  @Test
  void enrollmentQueryStageOrgUnitSortFieldsAreDistinctOrderingTerms() {
    EventQueryParams params =
        subject.getFromRequest(
            request(QUERY, ENROLLMENT)
                .asc(Set.of(stagePrefixed("ouname")))
                .desc(Set.of(stagePrefixed("oucode"), stagePrefixed("ou")))
                .build());

    QueryItem ouName = single(params.getAsc());
    QueryItem ouCode = byItemId(params.getDesc(), "oucode");
    QueryItem ou = byItemId(params.getDesc(), "ou");
    assertNotEquals(ouName, ouCode);
    assertNotEquals(ouName, ou);
    assertNotEquals(ouCode, ou);
  }

  @ParameterizedTest
  @EnumSource(EndpointItem.class)
  void unknownStageSuffixIsRejected(EndpointItem endpointItem) {
    EventDataQueryRequest request =
        request(QUERY, endpointItem).asc(Set.of(stagePrefixed("bogus"))).build();

    assertRejected(request, ErrorCode.E7224);
  }

  @ParameterizedTest
  @EnumSource(EndpointItem.class)
  void unknownStageIsRejected(EndpointItem endpointItem) {
    EventDataQueryRequest request =
        request(QUERY, endpointItem).asc(Set.of("nOsUcHsTaGe.ouname")).build();

    assertRejected(request, ErrorCode.E7226);
  }

  @ParameterizedTest
  @EnumSource(EndpointItem.class)
  void aggregateEndpointKeepsRejectingStagePrefixedOutputNames(EndpointItem endpointItem) {
    EventDataQueryRequest request =
        request(AGGREGATE, endpointItem).asc(Set.of(stagePrefixed("ouname"))).build();

    assertRejected(request, ErrorCode.E7224);
  }

  @Test
  void enrollmentQueryRejectsRepeatableStageOffsetOnStageSort() {
    EventDataQueryRequest request =
        request(QUERY, ENROLLMENT).asc(Set.of(stage.getUid() + "[-1].ouname")).build();

    assertRejected(request, ErrorCode.E7224);
  }

  private void assertRejected(EventDataQueryRequest request, ErrorCode expected) {
    IllegalQueryException exception =
        assertThrows(IllegalQueryException.class, () -> subject.getFromRequest(request));
    assertEquals(expected, exception.getErrorCode());
  }

  private String stagePrefixed(String suffix) {
    return stage.getUid() + "." + suffix;
  }

  private static QueryItem single(List<QueryItem> items) {
    assertEquals(1, items.size(), () -> "expected exactly one sort item, got " + items);
    return items.get(0);
  }

  private static QueryItem byItemId(List<QueryItem> items, String itemId) {
    return items.stream()
        .filter(item -> itemId.equals(item.getItemId()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("no sort item with id " + itemId + " in " + items));
  }

  private EventDataQueryRequest.EventDataQueryRequestBuilder request(
      EndpointAction action, EndpointItem item) {
    return EventDataQueryRequest.builder()
        .program(program.getUid())
        .endpointAction(action)
        .endpointItem(item)
        .outputType(item == ENROLLMENT ? EventOutputType.ENROLLMENT : EventOutputType.EVENT)
        .dimension(Collections.emptySet())
        .filter(Collections.emptySet());
  }
}
