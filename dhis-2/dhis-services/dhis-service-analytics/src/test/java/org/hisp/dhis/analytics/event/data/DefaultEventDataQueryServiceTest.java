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
package org.hisp.dhis.analytics.event.data;

import static org.hisp.dhis.analytics.event.data.DefaultEventDataQueryService.validateQueryParamsForConfidentialAndSkipAnalytics;
import static org.hisp.dhis.common.DimensionItemType.DATA_ELEMENT;
import static org.hisp.dhis.common.DimensionItemType.PROGRAM_ATTRIBUTE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.junit.jupiter.api.Test;

class DefaultEventDataQueryServiceTest {

  private EventQueryParams mockEventQueryParams(
      boolean includeConfidentialOrSkipAnalyticsItems, List<QueryItem> queryItems) {
    EventQueryParams mock = mock(EventQueryParams.class);
    when(mock.includeConfidentialOrSkipAnalyticsItems())
        .thenReturn(includeConfidentialOrSkipAnalyticsItems);
    when(mock.getItems()).thenReturn(queryItems);
    when(mock.getItemFilters()).thenReturn(List.of());
    return mock;
  }

  private <T extends DimensionalItemObject> QueryItem mockDimensionalItemObject(
      Class<T> clazz, DimensionItemType type, List<Consumer<T>> behavious) {
    T mock = mock(clazz);
    when(mock.isOfType(type)).thenReturn(true);
    QueryItem queryItem = mock(QueryItem.class);
    when(queryItem.getItem()).thenReturn(mock);
    behavious.forEach(c -> c.accept(mock));
    return queryItem;
  }

  @Test
  void testAggregateDontThrowExceptionForConfidential() {
    QueryItem queryItem =
        mockDimensionalItemObject(
            TrackedEntityAttribute.class,
            PROGRAM_ATTRIBUTE,
            List.of(t -> when(t.isConfidentialBool()).thenReturn(true)));
    EventQueryParams eventQueryParams = mockEventQueryParams(true, List.of(queryItem));
    assertDoesNotThrow(() -> validateQueryParamsForConfidentialAndSkipAnalytics(eventQueryParams));
  }

  @Test
  void testAggregateDontThrowExceptionForSkipAnalytics() {
    final String dataElementUid = "dataElementUid";

    ProgramStage programStage = mock(ProgramStage.class);
    ProgramStageDataElement programStageDataElement = mock(ProgramStageDataElement.class);

    when(programStage.getProgramStageDataElements()).thenReturn(Set.of(programStageDataElement));
    when(programStageDataElement.getSkipAnalytics()).thenReturn(true);

    DataElement dataElement = mock(DataElement.class);
    when(dataElement.getUid()).thenReturn(dataElementUid);
    when(programStageDataElement.getDataElement()).thenReturn(dataElement);

    QueryItem queryItem =
        mockDimensionalItemObject(
            DataElement.class,
            DATA_ELEMENT,
            List.of(t -> when(t.getUid()).thenReturn(dataElementUid)));
    when(queryItem.getProgramStage()).thenReturn(programStage);

    EventQueryParams eventQueryParams = mockEventQueryParams(true, List.of(queryItem));

    assertDoesNotThrow(() -> validateQueryParamsForConfidentialAndSkipAnalytics(eventQueryParams));
  }

  @Test
  void testQueryThrowsForConfidential() {
    QueryItem queryItem =
        mockDimensionalItemObject(
            TrackedEntityAttribute.class,
            PROGRAM_ATTRIBUTE,
            List.of(t -> when(t.isConfidentialBool()).thenReturn(true)));
    EventQueryParams eventQueryParams = mockEventQueryParams(false, List.of(queryItem));
    assertThrows(
        IllegalQueryException.class,
        () -> validateQueryParamsForConfidentialAndSkipAnalytics(eventQueryParams));
  }

  @Test
  void testQueryThrowsForSkipAnalytics() {
    final String dataElementUid = "dataElementUid";

    ProgramStage programStage = mock(ProgramStage.class);
    ProgramStageDataElement programStageDataElement = mock(ProgramStageDataElement.class);

    when(programStage.getProgramStageDataElements()).thenReturn(Set.of(programStageDataElement));
    when(programStageDataElement.getSkipAnalytics()).thenReturn(true);

    DataElement dataElement = mock(DataElement.class);
    when(dataElement.getUid()).thenReturn(dataElementUid);
    when(programStageDataElement.getDataElement()).thenReturn(dataElement);

    QueryItem queryItem =
        mockDimensionalItemObject(
            DataElement.class,
            DATA_ELEMENT,
            List.of(t -> when(t.getUid()).thenReturn(dataElementUid)));
    when(queryItem.getProgramStage()).thenReturn(programStage);

    EventQueryParams eventQueryParams = mockEventQueryParams(false, List.of(queryItem));

    assertThrows(
        IllegalQueryException.class,
        () -> validateQueryParamsForConfidentialAndSkipAnalytics(eventQueryParams));
  }
}
