/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.analytics.tracker;

import static org.hisp.dhis.analytics.tracker.ResponseHelper.getItemUid;
import static org.hisp.dhis.test.TestBase.createDataElement;
import static org.hisp.dhis.test.TestBase.createProgram;
import static org.hisp.dhis.test.TestBase.createProgramStage;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.AnalyticsCustomHeader;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.system.grid.ListGrid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link ResponseHelper}. */
class ResponseHelperTest {

  private Program programA;
  private ProgramStage programStageA;
  private DataElement dataElementA;

  @BeforeEach
  void setUp() {
    programA = createProgram('A');
    programStageA = createProgramStage('A', programA);
    programStageA.setUid("A03MvHHogjR");
    dataElementA = createDataElement('A');
  }

  @Nested
  @DisplayName("getItemUid tests")
  class GetItemUidTests {

    @Test
    @DisplayName("should return item UID when no program stage and no custom header")
    void shouldReturnItemUidWhenNoProgramStageAndNoCustomHeader() {
      // Given
      QueryItem queryItem =
          new QueryItem(
              dataElementA,
              null,
              dataElementA.getValueType(),
              dataElementA.getAggregationType(),
              null);

      // When
      String result = getItemUid(queryItem);

      // Then
      assertEquals(dataElementA.getUid(), result);
    }

    @Test
    @DisplayName("should return prefixed UID when has program stage but no custom header")
    void shouldReturnPrefixedUidWhenHasProgramStageButNoCustomHeader() {
      // Given
      QueryItem queryItem =
          new QueryItem(
              dataElementA,
              null,
              dataElementA.getValueType(),
              dataElementA.getAggregationType(),
              null);
      queryItem.setProgramStage(programStageA);

      // When
      String result = getItemUid(queryItem);

      // Then
      assertEquals(programStageA.getUid() + "." + dataElementA.getUid(), result);
    }

    @Test
    @DisplayName("should return headerKey when item has custom header for EVENT_DATE")
    void shouldReturnHeaderKeyWhenItemHasCustomHeaderForEventDate() {
      // Given
      BaseDimensionalItemObject eventDateItem = new BaseDimensionalItemObject("occurreddate");
      eventDateItem.setUid("occurreddate");

      QueryItem queryItem = new QueryItem(eventDateItem, null, ValueType.DATETIME, null, null);
      queryItem.setProgramStage(programStageA);
      queryItem.setCustomHeader(AnalyticsCustomHeader.forEventDate(programStageA));

      // When
      String result = getItemUid(queryItem);

      // Then
      // Should return "A03MvHHogjR.eventdate" (from headerKey())
      // NOT "A03MvHHogjR.occurreddate" (the underlying UID)
      assertEquals("A03MvHHogjR.eventdate", result);
    }

    @Test
    @DisplayName("should return headerKey when item has custom header for SCHEDULED_DATE")
    void shouldReturnHeaderKeyWhenItemHasCustomHeaderForScheduledDate() {
      // Given
      BaseDimensionalItemObject scheduledDateItem = new BaseDimensionalItemObject("scheduleddate");
      scheduledDateItem.setUid("scheduleddate");

      QueryItem queryItem = new QueryItem(scheduledDateItem, null, ValueType.DATETIME, null, null);
      queryItem.setProgramStage(programStageA);
      queryItem.setCustomHeader(AnalyticsCustomHeader.forScheduledDate(programStageA));

      // When
      String result = getItemUid(queryItem);

      // Then
      assertEquals("A03MvHHogjR.scheduleddate", result);
    }

    @Test
    @DisplayName("should return headerKey when item has custom header for EVENT_STATUS")
    void shouldReturnHeaderKeyWhenItemHasCustomHeaderForEventStatus() {
      // Given
      BaseDimensionalItemObject eventStatusItem = new BaseDimensionalItemObject("eventstatus");
      eventStatusItem.setUid("eventstatus");

      QueryItem queryItem = new QueryItem(eventStatusItem, null, ValueType.TEXT, null, null);
      queryItem.setProgramStage(programStageA);
      queryItem.setCustomHeader(AnalyticsCustomHeader.forEventStatus(programStageA));

      // When
      String result = getItemUid(queryItem);

      // Then
      assertEquals("A03MvHHogjR.eventstatus", result);
    }

    @Test
    @DisplayName("should return headerKey when item has custom header for OU")
    void shouldReturnHeaderKeyWhenItemHasCustomHeaderForOu() {
      // Given
      BaseDimensionalItemObject ouItem = new BaseDimensionalItemObject("ou");
      ouItem.setUid("ou");

      QueryItem queryItem = new QueryItem(ouItem, null, ValueType.TEXT, null, null);
      queryItem.setProgramStage(programStageA);
      queryItem.setCustomHeader(AnalyticsCustomHeader.forOrgUnit(programStageA));

      // When
      String result = getItemUid(queryItem);

      // Then
      assertEquals("A03MvHHogjR.ou", result);
    }
  }

  @Nested
  @DisplayName("applyHeaders tests")
  class ApplyHeadersTests {

    @Test
    @DisplayName("should normalize enum-style header aliases")
    void shouldNormalizeEnumStyleHeaderAliases() {
      Grid grid = new ListGrid();
      grid.addHeader(new GridHeader("programstatus", "Program status", ValueType.TEXT, false, true))
          .addHeader(
              new GridHeader("ouname", "Organisation unit name", ValueType.TEXT, false, true));

      EventQueryParams params =
          new EventQueryParams.Builder().withHeaders(Set.of("PROGRAM_STATUS")).build();

      ResponseHelper.applyHeaders(grid, params);

      assertEquals(1, grid.getHeaders().size());
      assertEquals("programstatus", grid.getHeaders().get(0).getName());
    }

    @Test
    @DisplayName("should keep requested header order after normalization")
    void shouldKeepRequestedHeaderOrderAfterNormalization() {
      Grid grid = new ListGrid();
      grid.addHeader(new GridHeader("programstatus", "Program status", ValueType.TEXT, false, true))
          .addHeader(
              new GridHeader("ouname", "Organisation unit name", ValueType.TEXT, false, true));

      EventQueryParams params =
          new EventQueryParams.Builder()
              .withHeaders(new LinkedHashSet<>(List.of("OUNAME", "PROGRAM_STATUS")))
              .build();

      ResponseHelper.applyHeaders(grid, params);

      assertEquals(2, grid.getHeaders().size());
      assertEquals("ouname", grid.getHeaders().get(0).getName());
      assertEquals("programstatus", grid.getHeaders().get(1).getName());
    }
  }

  @Nested
  @DisplayName("getItemUid with includeProgramStage parameter tests")
  class GetItemUidWithIncludeProgramStageTests {

    @Test
    @DisplayName("should return headerKey regardless of includeProgramStage flag")
    void shouldReturnHeaderKeyRegardlessOfIncludeProgramStageFlag() {
      // Given
      BaseDimensionalItemObject eventDateItem = new BaseDimensionalItemObject("occurreddate");
      eventDateItem.setUid("occurreddate");

      QueryItem queryItem = new QueryItem(eventDateItem, null, ValueType.DATETIME, null, null);
      queryItem.setProgramStage(programStageA);
      queryItem.setCustomHeader(AnalyticsCustomHeader.forEventDate(programStageA));

      // When - with includeProgramStage = true
      String resultWithFlag = getItemUid(queryItem, true);
      // When - with includeProgramStage = false
      String resultWithoutFlag = getItemUid(queryItem, false);

      // Then - custom header takes precedence
      assertEquals("A03MvHHogjR.eventdate", resultWithFlag);
      assertEquals("A03MvHHogjR.eventdate", resultWithoutFlag);
    }

    @Test
    @DisplayName("should respect includeProgramStage flag when no custom header")
    void shouldRespectIncludeProgramStageFlagWhenNoCustomHeader() {
      // Given
      QueryItem queryItem =
          new QueryItem(
              dataElementA,
              null,
              dataElementA.getValueType(),
              dataElementA.getAggregationType(),
              null);
      queryItem.setProgramStage(programStageA);

      // When
      String resultWithFlag = getItemUid(queryItem, true);
      String resultWithoutFlag = getItemUid(queryItem, false);

      // Then
      assertEquals(programStageA.getUid() + "." + dataElementA.getUid(), resultWithFlag);
      assertEquals(dataElementA.getUid(), resultWithoutFlag);
    }
  }
}
