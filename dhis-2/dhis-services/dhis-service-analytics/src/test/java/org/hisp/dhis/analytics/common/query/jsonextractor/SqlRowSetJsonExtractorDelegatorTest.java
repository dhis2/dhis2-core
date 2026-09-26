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
package org.hisp.dhis.analytics.common.query.jsonextractor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParamType;
import org.hisp.dhis.analytics.common.params.dimension.ElementWithOffset;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.ValueStatus;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSetMetaData;

/**
 * The point of these tests is the parse count. {@code TrackedEntityListGrid.addNamedRows} asks this
 * delegator for a value and for a row-context item once per requested column, inside a loop over
 * rows, and each of those used to deserialise the whole {@code enrollments} document again - around
 * twenty parses per row on the export UGANDA-10 traced, 46.8% of that request's CPU.
 */
class SqlRowSetJsonExtractorDelegatorTest {

  private static final String PROGRAM_UID = "PrOgRaM0001";
  private static final String STAGE_UID = "StAgE000001";
  private static final String DE_A_UID = "DaTaEleMnTA";
  private static final String DE_B_UID = "DaTaEleMnTB";

  /** One enrolment, one event, values for A but not for B. */
  private static String enrollments(String valueOfA) {
    return "[{\"programUid\":\""
        + PROGRAM_UID
        + "\",\"enrollmentUid\":\"EnRoLmEnT01\",\"enrollmentDate\":\"2026-07-01T00:00:00\","
        + "\"events\":[{\"programStageUid\":\""
        + STAGE_UID
        + "\",\"eventUid\":\"EvEnT000001\",\"occurredDate\":\"2026-07-02T00:00:00\","
        + "\"eventStatus\":\"ACTIVE\",\"eventDataValues\":{\""
        + DE_A_UID
        + "\":{\"value\":\""
        + valueOfA
        + "\"}}}]}]";
  }

  @Test
  void testEnrollmentsJsonIsParsedOncePerRowNoMatterHowManyColumnsAreRead() {
    List<DimensionIdentifier<DimensionParam>> dimensions =
        List.of(dimension(DE_A_UID), dimension(DE_B_UID));
    SqlRowSetJsonExtractorDelegator delegator = spy(delegator(dimensions, enrollments("11")));

    for (DimensionIdentifier<DimensionParam> dimension : dimensions) {
      delegator.getObject(dimension.getKey());
      delegator.getRowContextItem(dimension.getKey(), 0);
    }

    // Four accesses over two columns; before this was memoised, four parses.
    verify(delegator, times(1)).parseEnrollmentsFromJson(anyString());
  }

  @Test
  void testMemoIsNotReusedWhenTheCursorMovesToARowWithADifferentDocument() {
    List<DimensionIdentifier<DimensionParam>> dimensions = List.of(dimension(DE_A_UID));
    SqlRowSet rowSet = rowSet();
    when(rowSet.getString("enrollments")).thenReturn(enrollments("11"), enrollments("22"));

    SqlRowSetJsonExtractorDelegator delegator =
        spy(new SqlRowSetJsonExtractorDelegator(rowSet, dimensions));

    assertEquals("11", delegator.getObject(dimensions.get(0).getKey()));
    assertEquals("22", delegator.getObject(dimensions.get(0).getKey()));

    verify(delegator, times(2)).parseEnrollmentsFromJson(anyString());
  }

  @Test
  void testValuesAndRowContextStatusesAreUnchangedByTheMemo() {
    DimensionIdentifier<DimensionParam> withValue = dimension(DE_A_UID);
    DimensionIdentifier<DimensionParam> withoutValue = dimension(DE_B_UID);
    SqlRowSetJsonExtractorDelegator delegator =
        delegator(List.of(withValue, withoutValue), enrollments("11"));

    assertEquals("11", delegator.getObject(withValue.getKey()));
    assertNull(delegator.getObject(withoutValue.getKey()));

    // The stage is defined and the event exists, so the column that has a value is SET and
    // therefore reported as nothing at all; the one without a value is NOT_SET.
    assertTrue(delegator.getRowContextItem(withValue.getKey(), 0).isEmpty());
    assertEquals(
        Map.of("0", Map.of("valueStatus", ValueStatus.NOT_SET.getValue())),
        delegator.getRowContextItem(withoutValue.getKey(), 0));
  }

  private static SqlRowSetJsonExtractorDelegator delegator(
      List<DimensionIdentifier<DimensionParam>> dimensions, String json) {
    SqlRowSet rowSet = rowSet();
    when(rowSet.getString("enrollments")).thenReturn(json);
    return new SqlRowSetJsonExtractorDelegator(rowSet, dimensions);
  }

  private static SqlRowSet rowSet() {
    SqlRowSet rowSet = mock(SqlRowSet.class);
    SqlRowSetMetaData metaData = mock(SqlRowSetMetaData.class);
    when(metaData.getColumnNames()).thenReturn(new String[] {"enrollments"});
    when(rowSet.getMetaData()).thenReturn(metaData);
    return rowSet;
  }

  private static DimensionIdentifier<DimensionParam> dimension(String dataElementUid) {
    Program program = new Program();
    program.setUid(PROGRAM_UID);
    ProgramStage programStage = new ProgramStage();
    programStage.setUid(STAGE_UID);

    DataElement dataElement = new DataElement();
    dataElement.setUid(dataElementUid);
    dataElement.setValueType(ValueType.TEXT);

    DimensionParam dimensionParam =
        DimensionParam.ofObject(
            new QueryItem(dataElement), DimensionParamType.DIMENSIONS, IdScheme.UID, List.of());

    return DimensionIdentifier.of(
        ElementWithOffset.of(program), ElementWithOffset.of(programStage), dimensionParam);
  }
}
