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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.analytics.common.CommonRequestParams;
import org.hisp.dhis.analytics.common.ContextParams;
import org.hisp.dhis.analytics.common.params.CommonParsedParams;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
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
}
