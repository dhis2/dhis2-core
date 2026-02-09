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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.analytics.EventOutputType;
import org.hisp.dhis.analytics.event.QueryItemLocator;
import org.hisp.dhis.analytics.event.data.queryitem.QueryItemFilterHandlerRegistry;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.EventDataQueryRequest;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
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
        .thenReturn(new BaseDimensionalObject("pe"));
    lenient()
        .when(
            dataQueryService.getDimension(
                anyString(), anyList(), any(), anyList(), anyBoolean(), any(), any(IdScheme.class)))
        .thenReturn(new BaseDimensionalObject("pe"));
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
