/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.analytics.common.processing;

import static org.hisp.dhis.analytics.common.params.dimension.ElementWithOffset.emptyElementWithOffset;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Year;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.analytics.common.CommonRequestParams;
import org.hisp.dhis.analytics.common.ContextParams;
import org.hisp.dhis.analytics.common.params.CommonParsedParams;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam.StaticDimension;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParamType;
import org.hisp.dhis.analytics.common.params.dimension.ElementWithOffset;
import org.hisp.dhis.analytics.trackedentity.TrackedEntityQueryParams;
import org.hisp.dhis.analytics.trackedentity.TrackedEntityRequestParams;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.MetadataItem;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.system.grid.ListGrid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MetadataParamsHandlerTest {

  private MetadataParamsHandler metadataParamsHandler;

  @BeforeEach
  void setUp() {
    metadataParamsHandler = new MetadataParamsHandler();
  }

  @Nested
  @DisplayName("Event-level data element metadata items")
  class EventLevelDataElementMetadataTests {

    @Test
    @DisplayName("should contain both short and full format keys for stage-scoped data element")
    void shouldContainBothShortAndFullFormatKeysForStageScopedDataElement() {
      // Given
      String programUid = "IpHINAT79UW";
      String stageUid = "A03MvHHogjR";
      String dataElementUid = "a3kGcGDCuk6";
      String fullFormatKey = programUid + "." + stageUid + "." + dataElementUid;
      String shortFormatKey = stageUid + "." + dataElementUid;

      Program program = new Program("Child Programme");
      program.setUid(programUid);

      ProgramStage stage = new ProgramStage("Birth", program);
      stage.setUid(stageUid);

      DataElement dataElement = new DataElement("MCH Apgar Score");
      dataElement.setUid(dataElementUid);
      dataElement.setValueType(ValueType.NUMBER);

      QueryItem queryItem = new QueryItem(dataElement, null, ValueType.NUMBER, null, null);

      DimensionIdentifier<DimensionParam> dataElementDimension =
          DimensionIdentifier.of(
              ElementWithOffset.of(program),
              ElementWithOffset.of(stage),
              DimensionParam.ofObject(
                  queryItem, DimensionParamType.DIMENSIONS, IdScheme.UID, List.of()));

      Grid grid = new ListGrid();

      // Add the dimension to the request so isInOriginalRequest() returns true
      Set<String> dimensions = new LinkedHashSet<>();
      dimensions.add(dataElementDimension.toString());
      CommonRequestParams requestParams = new CommonRequestParams();
      requestParams.setDimension(dimensions);

      ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams =
          ContextParams.<TrackedEntityRequestParams, TrackedEntityQueryParams>builder()
              .commonRaw(requestParams)
              .commonParsed(
                  CommonParsedParams.builder()
                      .dimensionIdentifiers(List.of(dataElementDimension))
                      .build())
              .build();

      // When
      metadataParamsHandler.handle(grid, contextParams, null, 0);

      // Then
      @SuppressWarnings("unchecked")
      Map<String, Object> items = (Map<String, Object>) grid.getMetaData().get("items");
      assertNotNull(items, "Metadata items should not be null");

      // Verify full format key exists
      assertTrue(
          items.containsKey(fullFormatKey),
          "Items should contain full format key: " + fullFormatKey);

      // Verify short format key exists (THIS IS THE BUG FIX)
      assertTrue(
          items.containsKey(shortFormatKey),
          "Items should contain short format key: " + shortFormatKey);

      // Verify both keys point to the same metadata item
      MetadataItem fullFormatItem = (MetadataItem) items.get(fullFormatKey);
      MetadataItem shortFormatItem = (MetadataItem) items.get(shortFormatKey);
      assertNotNull(fullFormatItem, "Full format item should not be null");
      assertNotNull(shortFormatItem, "Short format item should not be null");
    }
  }

  @Nested
  @DisplayName("No-value keyword in metaData.dimensions")
  class NoValueDimensionTests {

    @Test
    @DisplayName(
        "appends D2__NOVALUE to an option-set dimension filtered by the keyword, without an items"
            + " entry")
    void shouldAppendNoValueToFilteredOptionSetDimension() {
      // Given
      String optionSetUid = "OS123456789";
      OptionSet optionSet = new OptionSet("Mode of discharge", ValueType.TEXT);
      optionSet.setUid(optionSetUid);
      Option option = new Option("Discharged", "MODDISCH");
      option.setUid("opt1234567a");
      optionSet.addOption(option);

      String dataElementUid = "de123456789";
      DataElement dataElement = new DataElement("Mode of discharge");
      dataElement.setUid(dataElementUid);
      dataElement.setValueType(ValueType.TEXT);
      dataElement.setOptionSet(optionSet);

      QueryItem queryItem = new QueryItem(dataElement, null, ValueType.TEXT, null, optionSet);

      DimensionIdentifier<DimensionParam> dimension =
          DimensionIdentifier.of(
              emptyElementWithOffset(),
              emptyElementWithOffset(),
              DimensionParam.ofObject(
                  queryItem,
                  DimensionParamType.DIMENSIONS,
                  IdScheme.UID,
                  List.of("IN:D2__NOVALUE")));

      Grid grid = new ListGrid();
      CommonRequestParams requestParams = new CommonRequestParams();

      ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams =
          ContextParams.<TrackedEntityRequestParams, TrackedEntityQueryParams>builder()
              .commonRaw(requestParams)
              .commonParsed(
                  CommonParsedParams.builder().dimensionIdentifiers(List.of(dimension)).build())
              .build();

      // When
      metadataParamsHandler.handle(grid, contextParams, null, 0);

      // Then
      @SuppressWarnings("unchecked")
      Map<String, List<String>> dimensions =
          (Map<String, List<String>>) grid.getMetaData().get("dimensions");
      assertTrue(
          dimensions.get(dataElementUid).contains("D2__NOVALUE"),
          "Dimensions should contain D2__NOVALUE for the filtered option-set dimension");

      @SuppressWarnings("unchecked")
      Map<String, Object> items = (Map<String, Object>) grid.getMetaData().get("items");
      assertFalse(items.containsKey("D2__NOVALUE"), "Items should not contain a D2__NOVALUE entry");
    }
  }

  @Nested
  @DisplayName("Periods requested by date dimensions")
  class RequestedPeriodTests {
    private static final String PROGRAM_UID = "IpHINAT79UW";

    private static final String STAGE_UID = "A03MvHHogjR";

    @Test
    void listsIsoPeriodOfTrackedEntityDate() {
      Grid grid = addRequestedPeriods(teDate(StaticDimension.CREATED, "2017"));

      assertEquals(List.of("2017"), dimensions(grid).get("created"));
      assertEquals("2017", ((MetadataItem) items(grid).get("2017")).getName());
    }

    @Test
    void listsPeriodsOfMixedTypesInRequestOrder() {
      Grid grid = addRequestedPeriods(teDate(StaticDimension.LASTUPDATED, "202105", "2017"));

      assertEquals(List.of("202105", "2017"), dimensions(grid).get("lastupdated"));
      assertTrue(items(grid).keySet().containsAll(List.of("202105", "2017")));
    }

    @Test
    void listsPeriodsARelativePeriodSpans() {
      String thisYear = String.valueOf(Year.now().getValue());

      Grid grid = addRequestedPeriods(teDate(StaticDimension.CREATED, "THIS_YEAR"));

      assertEquals(List.of(thisYear), dimensions(grid).get("created"));
      assertTrue(items(grid).containsKey(thisYear));
    }

    @Test
    void keysProgramScopedDateByProgram() {
      DimensionIdentifier<DimensionParam> enrollmentDate =
          DimensionIdentifier.of(
              ElementWithOffset.of(program()),
              emptyElementWithOffset(),
              dateParam(StaticDimension.ENROLLMENTDATE, DimensionParamType.DIMENSIONS, "2021"));

      Grid grid = addRequestedPeriods(enrollmentDate);

      assertEquals(List.of("2021"), dimensions(grid).get(PROGRAM_UID + ".enrollmentdate"));
    }

    @Test
    void keysStageScopedDateByStage() {
      ProgramStage stage = new ProgramStage();
      stage.setUid(STAGE_UID);
      DimensionIdentifier<DimensionParam> eventDate =
          DimensionIdentifier.of(
              ElementWithOffset.of(program()),
              ElementWithOffset.of(stage),
              dateParam(StaticDimension.EVENT_DATE, DimensionParamType.DIMENSIONS, "2021"));

      Grid grid = addRequestedPeriods(eventDate);

      assertEquals(List.of("2021"), dimensions(grid).get(STAGE_UID + ".eventdate"));
    }

    @Test
    void listsNoPeriodForADateComparison() {
      Grid grid = addRequestedPeriods(teDate(StaticDimension.CREATED, "GT:2017-01-01"));

      assertEquals(List.of(), dimensions(grid).get("created"));
      assertFalse(items(grid).containsKey("2017"));
    }

    @Test
    void ignoresDateFilters() {
      DimensionIdentifier<DimensionParam> createdFilter =
          DimensionIdentifier.of(
              emptyElementWithOffset(),
              emptyElementWithOffset(),
              dateParam(StaticDimension.CREATED, DimensionParamType.FILTERS, "2017"));

      Grid grid = addRequestedPeriods(createdFilter);

      assertFalse(dimensions(grid).containsKey("created"));
      assertFalse(items(grid).containsKey("2017"));
    }

    @Test
    void includesPeriodDetailsWhenRequested() {
      CommonRequestParams request = new CommonRequestParams();
      request.setIncludeMetadataDetails(true);

      Grid grid = addRequestedPeriods(request, teDate(StaticDimension.CREATED, "2017"));

      MetadataItem period = (MetadataItem) items(grid).get("2017");
      assertEquals("2017", period.getUid());
      assertNotNull(period.getStartDate());
    }

    @Test
    void addsNothingWhenMetadataIsSkipped() {
      CommonRequestParams request = new CommonRequestParams();
      request.setSkipMeta(true);

      Grid grid = addRequestedPeriods(request, teDate(StaticDimension.CREATED, "2017"));

      assertTrue(grid.getMetaData().isEmpty());
    }

    private Grid addRequestedPeriods(DimensionIdentifier<DimensionParam> dimension) {
      return addRequestedPeriods(new CommonRequestParams(), dimension);
    }

    private Grid addRequestedPeriods(
        CommonRequestParams request, DimensionIdentifier<DimensionParam> dimension) {
      ContextParams<TrackedEntityRequestParams, TrackedEntityQueryParams> contextParams =
          ContextParams.<TrackedEntityRequestParams, TrackedEntityQueryParams>builder()
              .commonRaw(request)
              .commonParsed(
                  CommonParsedParams.builder().dimensionIdentifiers(List.of(dimension)).build())
              .build();
      Grid grid = new ListGrid();

      metadataParamsHandler.handle(grid, contextParams, null, 0);
      metadataParamsHandler.addRequestedPeriods(grid, contextParams);

      return grid;
    }

    private DimensionIdentifier<DimensionParam> teDate(
        StaticDimension staticDimension, String... items) {
      return DimensionIdentifier.of(
          emptyElementWithOffset(),
          emptyElementWithOffset(),
          dateParam(staticDimension, DimensionParamType.DIMENSIONS, items));
    }

    private DimensionParam dateParam(
        StaticDimension staticDimension, DimensionParamType type, String... items) {
      return DimensionParam.ofObject(staticDimension.name(), type, IdScheme.UID, List.of(items));
    }

    private Program program() {
      Program program = new Program();
      program.setUid(PROGRAM_UID);
      return program;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> dimensions(Grid grid) {
      return (Map<String, Object>) grid.getMetaData().get("dimensions");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> items(Grid grid) {
      return (Map<String, Object>) grid.getMetaData().get("items");
    }
  }
}
