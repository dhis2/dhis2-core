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

import static org.hisp.dhis.common.ValueType.COORDINATE;
import static org.hisp.dhis.common.ValueType.INTEGER;
import static org.hisp.dhis.common.ValueType.ORGANISATION_UNIT;
import static org.hisp.dhis.common.ValueType.TEXT;
import static org.hisp.dhis.test.TestBase.createDataElement;
import static org.hisp.dhis.test.TestBase.createLegend;
import static org.hisp.dhis.test.TestBase.createLegendSet;
import static org.hisp.dhis.test.TestBase.createOption;
import static org.hisp.dhis.test.TestBase.createOptionSet;
import static org.hisp.dhis.test.TestBase.createProgram;
import static org.hisp.dhis.test.TestBase.createProgramStage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.AnalyticsCustomHeader;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.RepeatableStageParams;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.legend.Legend;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.system.grid.ListGrid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HeaderHelperTest {

  private Program programA;
  private ProgramStage stageA;
  private ProgramStage stageB;
  private OptionSet optionSet;
  private LegendSet legendSet;

  @BeforeEach
  void setUp() {
    programA = createProgram('A');
    stageA = createProgramStage('A', programA);
    stageB = createProgramStage('B', programA);
    stageA.setUid("StageAUid01");
    stageB.setUid("StageBUid01");

    Option option = createOption('A');
    optionSet = createOptionSet('A', option);

    Legend legend = createLegend('A', 0d, 10d);
    legendSet = createLegendSet('A', legend);
  }

  @Test
  @DisplayName("should add dimension and period headers before item headers")
  void shouldAddDimensionAndPeriodHeadersBeforeItemHeaders() {
    Grid grid = new ListGrid();

    BaseDimensionalObject orgUnitDimension =
        new BaseDimensionalObject(
            "ou", DimensionType.ORGANISATION_UNIT, "ou", "Organisation unit", List.of());
    BaseDimensionalObject periodDimension =
        new BaseDimensionalObject("pe", DimensionType.PERIOD, "pe", "Period", List.of());

    QueryItem item = queryItem("deUidA001", "Item A", TEXT);

    EventQueryParams params =
        new EventQueryParams.Builder()
            .addDimension(orgUnitDimension)
            .addItem(item)
            .withDisplayProperty(DisplayProperty.NAME)
            .build();

    HeaderHelper.addCommonHeaders(grid, params, List.of(periodDimension));

    List<GridHeader> headers = grid.getHeaders();

    assertEquals(3, headers.size());
    assertEquals("ou", headers.get(0).getName());
    assertEquals("Organisation unit", headers.get(0).getColumn());
    assertEquals("pe", headers.get(1).getName());
    assertEquals("Period", headers.get(1).getColumn());
    assertEquals("deUidA001", headers.get(2).getName());
    assertEquals("Item A", headers.get(2).getColumn());
  }

  @Test
  @DisplayName("should build coordinate header when item is org unit and part of coordinate fields")
  void shouldBuildCoordinateHeaderForCoordinateFieldItem() {
    Grid grid = new ListGrid();

    QueryItem coordinateItem = queryItem("ouUidA001", "Reporter Org Unit", ORGANISATION_UNIT);
    coordinateItem.setOptionSet(optionSet);
    coordinateItem.setLegendSet(legendSet);

    EventQueryParams params =
        new EventQueryParams.Builder()
            .addItem(coordinateItem)
            .withDisplayProperty(DisplayProperty.NAME)
            .withCoordinateFields(List.of("ouUidA001"))
            .build();

    HeaderHelper.addCommonHeaders(grid, params, List.of());

    GridHeader header = grid.getHeaders().get(0);

    assertEquals("ouUidA001", header.getName());
    assertEquals("Reporter Org Unit", header.getColumn());
    assertEquals("Reporter Org Unit", header.getDisplayColumn());
    assertEquals(COORDINATE, header.getValueType());
    assertEquals(optionSet.getUid(), header.getOptionSet());
    assertEquals(legendSet.getUid(), header.getLegendSet());
  }

  @Test
  @DisplayName(
      "should build repeatable stage header with stage metadata and repeated display column")
  void shouldBuildRepeatableStageHeaderWithStageMetadataAndRepeatedDisplayColumn() {
    Grid grid = new ListGrid();

    QueryItem repeatedItem = queryItem("deUidRep1", "Repeated Item", INTEGER);
    repeatedItem.setProgramStage(stageA);
    repeatedItem.setOptionSet(optionSet);
    repeatedItem.setLegendSet(legendSet);
    repeatedItem.setRepeatableStageParams(RepeatableStageParams.of(2, "StageAUid01.deUidRep1"));

    QueryItem duplicateNameItem = queryItem("deUidRep2", "Repeated Item", INTEGER);

    EventQueryParams params =
        new EventQueryParams.Builder()
            .addItem(repeatedItem)
            .addItem(duplicateNameItem)
            .withDisplayProperty(DisplayProperty.NAME)
            .build();

    HeaderHelper.addCommonHeaders(grid, params, List.of());

    GridHeader header = grid.getHeaders().get(0);

    assertEquals("StageAUid01.deUidRep1", header.getName());
    assertEquals("Repeated Item", header.getColumn());
    assertEquals("Repeated Item - " + stageA.getDisplayName(), header.getDisplayColumn());
    assertEquals(stageA.getUid(), header.getProgramStage());
    assertTrue(header.getRepeatableStageParams().contains("index:2"));
    assertEquals(optionSet.getUid(), header.getOptionSet());
    assertEquals(legendSet.getUid(), header.getLegendSet());
  }

  @Test
  @DisplayName("should build default header using custom header key and label")
  void shouldBuildDefaultHeaderUsingCustomHeaderKeyAndLabel() {
    Grid grid = new ListGrid();

    DataElement dataElement = createDataElement('A');
    QueryItem item =
        new QueryItem(
            dataElement, null, dataElement.getValueType(), dataElement.getAggregationType(), null);
    item.setProgramStage(stageA);
    item.setCustomHeader(AnalyticsCustomHeader.forEventStatus(stageA));

    EventQueryParams params =
        new EventQueryParams.Builder()
            .addItem(item)
            .withDisplayProperty(DisplayProperty.NAME)
            .build();

    HeaderHelper.addCommonHeaders(grid, params, List.of());

    GridHeader header = grid.getHeaders().get(0);

    assertEquals(item.getCustomHeader().headerKey(item.getCustomHeader().key()), header.getName());
    assertEquals(item.getCustomHeader().label(), header.getColumn());
    assertEquals(dataElement.getDisplayName(), header.getDisplayColumn());
    assertNull(header.getProgramStage());
  }

  @Test
  @DisplayName("should append program stage to display column for duplicated default item names")
  void shouldAppendProgramStageToDisplayColumnForDuplicatedDefaultItemNames() {
    Grid grid = new ListGrid();

    QueryItem stageItem = queryItem("dupUidA01", "Duplicated Name", TEXT);
    stageItem.setProgramStage(stageB);

    QueryItem duplicateItem = queryItem("dupUidA02", "Duplicated Name", TEXT);

    EventQueryParams params =
        new EventQueryParams.Builder()
            .addItem(stageItem)
            .addItem(duplicateItem)
            .withDisplayProperty(DisplayProperty.NAME)
            .build();

    HeaderHelper.addCommonHeaders(grid, params, List.of());

    GridHeader header = grid.getHeaders().get(0);

    assertEquals("Duplicated Name - " + stageB.getDisplayName(), header.getDisplayColumn());
  }

  @Test
  @DisplayName("stage.ou with headers containing ouname should add ouname header")
  void stageOuWithOunameHeaderRequested() {
    Grid grid = new ListGrid();
    QueryItem ouItem = stageOuItem();

    EventQueryParams params =
        new EventQueryParams.Builder()
            .addItem(ouItem)
            .withDisplayProperty(DisplayProperty.NAME)
            .withHeaders(Set.of("StageAUid01.ouname"))
            .build();

    HeaderHelper.addCommonHeaders(grid, params, List.of());

    List<GridHeader> headers = grid.getHeaders();
    assertEquals(2, headers.size());
    assertEquals("StageAUid01.ou", headers.get(0).getName());
    assertEquals("StageAUid01.ouname", headers.get(1).getName());
  }

  @Test
  @DisplayName("stage.ou with headers containing oucode should add oucode header")
  void stageOuWithOucodeHeaderRequested() {
    Grid grid = new ListGrid();
    QueryItem ouItem = stageOuItem();

    EventQueryParams params =
        new EventQueryParams.Builder()
            .addItem(ouItem)
            .withDisplayProperty(DisplayProperty.NAME)
            .withHeaders(Set.of("StageAUid01.oucode"))
            .build();

    HeaderHelper.addCommonHeaders(grid, params, List.of());

    List<GridHeader> headers = grid.getHeaders();
    assertEquals(2, headers.size());
    assertEquals("StageAUid01.ou", headers.get(0).getName());
    assertEquals("StageAUid01.oucode", headers.get(1).getName());
  }

  @Test
  @DisplayName("stage.ou with headers containing both ouname and oucode should add both headers")
  void stageOuWithBothHeadersRequested() {
    Grid grid = new ListGrid();
    QueryItem ouItem = stageOuItem();

    EventQueryParams params =
        new EventQueryParams.Builder()
            .addItem(ouItem)
            .withDisplayProperty(DisplayProperty.NAME)
            .withHeaders(Set.of("StageAUid01.ouname", "StageAUid01.oucode"))
            .build();

    HeaderHelper.addCommonHeaders(grid, params, List.of());

    List<GridHeader> headers = grid.getHeaders();
    assertEquals(3, headers.size());
    assertEquals("StageAUid01.ou", headers.get(0).getName());
    assertTrue(headers.stream().anyMatch(h -> "StageAUid01.ouname".equals(h.getName())));
    assertTrue(headers.stream().anyMatch(h -> "StageAUid01.oucode".equals(h.getName())));
  }

  @Test
  @DisplayName("stage.ou with headers NOT containing ouname/oucode should only add ou header")
  void stageOuWithoutOunameOucodeInHeaders() {
    Grid grid = new ListGrid();
    QueryItem ouItem = stageOuItem();

    EventQueryParams params =
        new EventQueryParams.Builder()
            .addItem(ouItem)
            .withDisplayProperty(DisplayProperty.NAME)
            .withHeaders(Set.of("someOtherHeader"))
            .build();

    HeaderHelper.addCommonHeaders(grid, params, List.of());

    List<GridHeader> headers = grid.getHeaders();
    assertEquals(1, headers.size());
    assertEquals("StageAUid01.ou", headers.get(0).getName());
  }

  @Test
  @DisplayName("stage.ou without headers param should only add ou header")
  void stageOuWithNoHeadersParam() {
    Grid grid = new ListGrid();
    QueryItem ouItem = stageOuItem();

    EventQueryParams params =
        new EventQueryParams.Builder()
            .addItem(ouItem)
            .withDisplayProperty(DisplayProperty.NAME)
            .build();

    HeaderHelper.addCommonHeaders(grid, params, List.of());

    List<GridHeader> headers = grid.getHeaders();
    assertEquals(1, headers.size());
    assertEquals("StageAUid01.ou", headers.get(0).getName());
  }

  private QueryItem stageOuItem() {
    BaseDimensionalItemObject ouDimItem = new BaseDimensionalItemObject("ou", "ou", "ou");
    QueryItem item = new QueryItem(ouDimItem, null, ORGANISATION_UNIT, null, null);
    item.setProgramStage(stageA);
    return item;
  }

  private QueryItem queryItem(String uid, String name, ValueType valueType) {
    BaseDimensionalItemObject item = new BaseDimensionalItemObject(uid, uid, name);
    return new QueryItem(item, null, valueType, null, null);
  }
}
