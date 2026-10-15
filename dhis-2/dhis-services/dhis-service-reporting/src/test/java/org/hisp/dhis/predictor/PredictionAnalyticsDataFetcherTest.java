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
package org.hisp.dhis.predictor;

import static org.hisp.dhis.common.DimensionConstants.ATTRIBUTEOPTIONCOMBO_DIM_ID;
import static org.hisp.dhis.common.DimensionConstants.CATEGORYOPTIONCOMBO_DIM_ID;
import static org.hisp.dhis.common.DimensionConstants.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionConstants.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionConstants.PERIOD_DIM_ID;
import static org.hisp.dhis.program.AnalyticsType.ENROLLMENT;
import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.FoundDimensionItemValue;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodDimension;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.test.TestBase;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jim Grace
 */
@ExtendWith(MockitoExtension.class)
class PredictionAnalyticsDataFetcherTest extends TestBase {
  @Mock private AnalyticsService analyticsService;

  @Mock private CategoryService categoryService;

  private OrganisationUnit orgUnitA;

  private OrganisationUnit orgUnitB;

  private List<OrganisationUnit> orgUnits;

  private Period periodA;

  private Period periodB;

  private Set<Period> periods;

  private Program programA;

  private Program programB;

  private TrackedEntityAttribute trackedEntityAttributeA;

  private PredictionAnalyticsDataFetcher fetcher;

  // -------------------------------------------------------------------------
  // Fixture
  // -------------------------------------------------------------------------

  @BeforeEach
  void initTest() {
    periodA = createPeriod("202101");
    periodB = createPeriod("202102");

    periodA.setId(3);
    periodB.setId(4);

    periods = Sets.newHashSet(periodA, periodB);

    orgUnitA = createOrganisationUnit('A');
    orgUnitB = createOrganisationUnit('B');

    orgUnitA.setUid("orgUnitAuid");
    orgUnitB.setUid("orgUnitBuid");

    orgUnits = Lists.newArrayList(orgUnitA, orgUnitB);

    programA = createProgram('A');
    programB = createProgram('B');

    fetcher = new PredictionAnalyticsDataFetcher(analyticsService, categoryService, false);
  }

  // -------------------------------------------------------------------------
  // Tests
  // -------------------------------------------------------------------------

  @Test
  void testGetValues() {
    // ---------------------------------------------------------------------
    // Items with Attribute Option Combos
    // ---------------------------------------------------------------------

    CategoryOptionCombo aocC = createCategoryOptionCombo('C');
    CategoryOptionCombo aocD = createCategoryOptionCombo('D');

    when(categoryService.getCategoryOptionCombo(aocC.getUid())).thenReturn(aocC);
    when(categoryService.getCategoryOptionCombo(aocD.getUid())).thenReturn(aocD);

    ProgramIndicator programIndicatorA =
        createProgramIndicator('A', programA, "expressionA", "filterA");
    ProgramIndicator programIndicatorB =
        createProgramIndicator('B', programB, "expressionB", "filterB");

    trackedEntityAttributeA = createTrackedEntityAttribute('A');

    Set<DimensionalItemObject> aocItems =
        Sets.newHashSet(programIndicatorA, programIndicatorB, trackedEntityAttributeA);

    DataQueryParams aocParams =
        DataQueryParams.newBuilder()
            .withPeriods(periods.stream().map(PeriodDimension::of).toList())
            .withDataDimensionItems(Lists.newArrayList(aocItems))
            .withOrganisationUnits(orgUnits)
            .withAttributeOptionCombos(Collections.emptyList())
            .build();

    Grid aocGrid = new ListGrid();

    aocGrid.addHeader(new GridHeader(PERIOD_DIM_ID, "Period", ValueType.TEXT, false, true));
    aocGrid.addHeader(new GridHeader(DATA_X_DIM_ID, "DimensionItem", ValueType.TEXT, false, true));
    aocGrid.addHeader(
        new GridHeader(ORGUNIT_DIM_ID, "OrganisationUnit", ValueType.TEXT, false, true));
    aocGrid.addHeader(
        new GridHeader(ATTRIBUTEOPTIONCOMBO_DIM_ID, "AOC", ValueType.TEXT, false, true));
    aocGrid.addHeader(new GridHeader("value", "Value", ValueType.NUMBER, false, true));

    aocGrid
        .addRow()
        .addValue(periodA.getIsoDate())
        .addValue(programIndicatorA.getUid())
        .addValue(orgUnitA.getUid())
        .addValue(aocC.getUid())
        .addValue(10.0);

    aocGrid
        .addRow()
        .addValue(periodB.getIsoDate())
        .addValue(programIndicatorB.getUid())
        .addValue(orgUnitA.getUid())
        .addValue(aocC.getUid())
        .addValue(20L); // Note: boxed as Long, not Double

    aocGrid
        .addRow()
        .addValue(periodB.getIsoDate())
        .addValue(trackedEntityAttributeA.getUid())
        .addValue(orgUnitA.getUid())
        .addValue(aocD.getUid())
        .addValue(30.0);

    aocGrid
        .addRow()
        .addValue(periodA.getIsoDate())
        .addValue(programIndicatorA.getUid())
        .addValue(orgUnitB.getUid())
        .addValue(aocC.getUid())
        .addValue(40.0);

    when(analyticsService.getAggregatedDataValues(aocParams)).thenReturn(aocGrid);

    FoundDimensionItemValue expected1;
    FoundDimensionItemValue expected2;
    FoundDimensionItemValue expected3;
    FoundDimensionItemValue expected4;

    expected1 = new FoundDimensionItemValue(orgUnitA, periodA, null, aocC, programIndicatorA, 10.0);
    expected2 = new FoundDimensionItemValue(orgUnitA, periodB, null, aocC, programIndicatorB, 20.0);
    expected3 =
        new FoundDimensionItemValue(orgUnitA, periodB, null, aocD, trackedEntityAttributeA, 30.0);
    expected4 = new FoundDimensionItemValue(orgUnitB, periodA, null, aocC, programIndicatorA, 40.0);

    // ---------------------------------------------------------------------
    // Items without Attribute Option Combos
    // ---------------------------------------------------------------------

    ProgramIndicator programIndicatorC =
        createProgramIndicator('C', programA, "expressionC", "filterC");
    ProgramIndicator programIndicatorD =
        createProgramIndicator('D', programB, "expressionD", "filterD");

    programIndicatorC.setAnalyticsType(ENROLLMENT);
    programIndicatorD.setAnalyticsType(ENROLLMENT);

    Set<DimensionalItemObject> nonAocItems = Sets.newHashSet(programIndicatorC, programIndicatorD);

    DataQueryParams nonAocParams =
        DataQueryParams.newBuilder()
            .withPeriods(periods.stream().map(PeriodDimension::of).toList())
            .withDataDimensionItems(Lists.newArrayList(nonAocItems))
            .withOrganisationUnits(orgUnits)
            .build();

    Grid nonAocGrid = new ListGrid();

    nonAocGrid.addHeader(new GridHeader(PERIOD_DIM_ID, "Period", ValueType.TEXT, false, true));
    nonAocGrid.addHeader(
        new GridHeader(DATA_X_DIM_ID, "DimensionItem", ValueType.TEXT, false, true));
    nonAocGrid.addHeader(
        new GridHeader(ORGUNIT_DIM_ID, "OrganisationUnit", ValueType.TEXT, false, true));
    nonAocGrid.addHeader(new GridHeader("value", "Value", ValueType.NUMBER, false, true));

    nonAocGrid
        .addRow()
        .addValue(periodA.getIsoDate())
        .addValue(programIndicatorC.getUid())
        .addValue(orgUnitA.getUid())
        .addValue(100.0);

    nonAocGrid
        .addRow()
        .addValue(periodA.getIsoDate())
        .addValue(programIndicatorD.getUid())
        .addValue(orgUnitB.getUid())
        .addValue(200.0);

    nonAocGrid
        .addRow()
        .addValue(periodB.getIsoDate())
        .addValue(programIndicatorC.getUid())
        .addValue(orgUnitA.getUid())
        .addValue(300.0);

    when(analyticsService.getAggregatedDataValues(nonAocParams)).thenReturn(nonAocGrid);

    FoundDimensionItemValue expected5;
    FoundDimensionItemValue expected6;
    FoundDimensionItemValue expected7;

    CategoryOptionCombo noAoc = null;

    expected5 =
        new FoundDimensionItemValue(orgUnitA, periodA, null, noAoc, programIndicatorC, 100.0);
    expected6 =
        new FoundDimensionItemValue(orgUnitB, periodA, null, noAoc, programIndicatorD, 200.0);
    expected7 =
        new FoundDimensionItemValue(orgUnitA, periodB, null, noAoc, programIndicatorC, 300.0);

    // ---------------------------------------------------------------------
    // Do the test
    // ---------------------------------------------------------------------

    Set<DimensionalItemObject> items = Sets.union(aocItems, nonAocItems);

    fetcher.init(periods, items);

    List<FoundDimensionItemValue> actual = fetcher.getValues(orgUnits);

    assertContainsOnly(
        List.of(expected1, expected2, expected3, expected4, expected5, expected6, expected7),
        actual);
  }

  @Test
  void testGetValuesWithDisaggregatedProgramIndicator() {
    fetcher = new PredictionAnalyticsDataFetcher(analyticsService, categoryService, true);

    CategoryOptionCombo cocE = createCategoryOptionCombo('E');
    CategoryOptionCombo cocF = createCategoryOptionCombo('F');
    CategoryOptionCombo aocC = createCategoryOptionCombo('C');
    CategoryOptionCombo aocDefault = createCategoryOptionCombo('Z');

    when(categoryService.getCategoryOptionCombo(cocE.getUid())).thenReturn(cocE);
    when(categoryService.getCategoryOptionCombo(cocF.getUid())).thenReturn(cocF);
    when(categoryService.getCategoryOptionCombo(aocC.getUid())).thenReturn(aocC);
    when(categoryService.getDefaultCategoryOptionCombo()).thenReturn(aocDefault);

    // ---------------------------------------------------------------------
    // Disaggregated event program indicator, fetched in data value set
    // format. A value without an AOC gets the default AOC.
    // ---------------------------------------------------------------------

    ProgramIndicator programIndicatorE =
        createProgramIndicator('E', programA, "expressionE", "filterE");
    programIndicatorE.setCategoryCombo(createCategoryCombo('E'));
    programIndicatorE.setAggregateExportDataElement("aggExportDE");

    Grid disagGrid = newDisagGrid();

    // Note: analytics returns the aggregate export data element instead of the program indicator
    disagGrid
        .addRow()
        .addValue("aggExportDE")
        .addValue(periodA.getIsoDate())
        .addValue(orgUnitA.getUid())
        .addValue(cocE.getUid())
        .addValue(null)
        .addValue(1.0);

    disagGrid
        .addRow()
        .addValue("aggExportDE")
        .addValue(periodA.getIsoDate())
        .addValue(orgUnitA.getUid())
        .addValue(cocF.getUid())
        .addValue(aocC.getUid())
        .addValue(2); // Note: boxed as Integer, not Double

    when(analyticsService.getAggregatedDataValuesGrid(disagParamsFor(programIndicatorE)))
        .thenReturn(disagGrid);

    FoundDimensionItemValue expected1 =
        new FoundDimensionItemValue(orgUnitA, periodA, cocE, aocDefault, programIndicatorE, 1.0);
    FoundDimensionItemValue expected2 =
        new FoundDimensionItemValue(orgUnitA, periodA, cocF, aocC, programIndicatorE, 2.0);

    // ---------------------------------------------------------------------
    // Disaggregated enrollment program indicator. A value without an AOC
    // keeps no AOC, so it is used for all AOCs.
    // ---------------------------------------------------------------------

    ProgramIndicator programIndicatorH =
        createProgramIndicator('H', programA, "expressionH", "filterH");
    programIndicatorH.setAnalyticsType(ENROLLMENT);
    programIndicatorH.setCategoryCombo(createCategoryCombo('H'));

    Grid enrollmentGrid = newDisagGrid();

    enrollmentGrid
        .addRow()
        .addValue(programIndicatorH.getUid())
        .addValue(periodB.getIsoDate())
        .addValue(orgUnitB.getUid())
        .addValue(cocE.getUid())
        .addValue(null)
        .addValue(4.0);

    when(analyticsService.getAggregatedDataValuesGrid(disagParamsFor(programIndicatorH)))
        .thenReturn(enrollmentGrid);

    FoundDimensionItemValue expected4 =
        new FoundDimensionItemValue(orgUnitB, periodB, cocE, null, programIndicatorH, 4.0);

    // ---------------------------------------------------------------------
    // Program indicator without disaggregation, fetched as usual
    // ---------------------------------------------------------------------

    ProgramIndicator programIndicatorG =
        createProgramIndicator('G', programB, "expressionG", "filterG");

    DataQueryParams aocParams =
        DataQueryParams.newBuilder()
            .withPeriods(periods.stream().map(PeriodDimension::of).toList())
            .withDataDimensionItems(List.of(programIndicatorG))
            .withOrganisationUnits(orgUnits)
            .withAttributeOptionCombos(Collections.emptyList())
            .build();

    Grid aocGrid = new ListGrid();

    aocGrid.addHeader(new GridHeader(PERIOD_DIM_ID, "Period", ValueType.TEXT, false, true));
    aocGrid.addHeader(new GridHeader(DATA_X_DIM_ID, "DimensionItem", ValueType.TEXT, false, true));
    aocGrid.addHeader(
        new GridHeader(ORGUNIT_DIM_ID, "OrganisationUnit", ValueType.TEXT, false, true));
    aocGrid.addHeader(
        new GridHeader(ATTRIBUTEOPTIONCOMBO_DIM_ID, "AOC", ValueType.TEXT, false, true));
    aocGrid.addHeader(new GridHeader("value", "Value", ValueType.NUMBER, false, true));

    aocGrid
        .addRow()
        .addValue(periodB.getIsoDate())
        .addValue(programIndicatorG.getUid())
        .addValue(orgUnitB.getUid())
        .addValue(aocC.getUid())
        .addValue(3.0);

    when(analyticsService.getAggregatedDataValues(aocParams)).thenReturn(aocGrid);

    FoundDimensionItemValue expected3 =
        new FoundDimensionItemValue(orgUnitB, periodB, null, aocC, programIndicatorG, 3.0);

    // ---------------------------------------------------------------------
    // Do the test
    // ---------------------------------------------------------------------

    fetcher.init(periods, Set.of(programIndicatorE, programIndicatorG, programIndicatorH));

    List<FoundDimensionItemValue> actual = fetcher.getValues(orgUnits);

    assertContainsOnly(List.of(expected1, expected2, expected3, expected4), actual);
  }

  @Test
  void testGetValuesWithAttributeComboNotDisaggregated() {
    CategoryOptionCombo cocE = createCategoryOptionCombo('E');
    CategoryOptionCombo cocF = createCategoryOptionCombo('F');
    CategoryOptionCombo aocC = createCategoryOptionCombo('C');
    CategoryOptionCombo aocD = createCategoryOptionCombo('D');

    when(categoryService.getCategoryOptionCombo(cocE.getUid())).thenReturn(cocE);
    when(categoryService.getCategoryOptionCombo(cocF.getUid())).thenReturn(cocF);
    when(categoryService.getCategoryOptionCombo(aocC.getUid())).thenReturn(aocC);
    when(categoryService.getCategoryOptionCombo(aocD.getUid())).thenReturn(aocD);

    // ---------------------------------------------------------------------
    // Program indicator with a category combo and attribute combo is
    // fetched with its mapped option combos, even though the predictions
    // are not disaggregated. (Its COC values are summed later.)
    // ---------------------------------------------------------------------

    ProgramIndicator programIndicatorJ =
        createProgramIndicator('J', programA, "expressionJ", "filterJ");
    programIndicatorJ.setCategoryCombo(createCategoryCombo('J'));
    programIndicatorJ.setAttributeCombo(createCategoryCombo('K'));

    Grid disagGrid = newDisagGrid();

    disagGrid
        .addRow()
        .addValue(programIndicatorJ.getUid())
        .addValue(periodA.getIsoDate())
        .addValue(orgUnitA.getUid())
        .addValue(cocE.getUid())
        .addValue(aocC.getUid())
        .addValue(1.0);

    disagGrid
        .addRow()
        .addValue(programIndicatorJ.getUid())
        .addValue(periodA.getIsoDate())
        .addValue(orgUnitA.getUid())
        .addValue(cocF.getUid())
        .addValue(aocC.getUid())
        .addValue(2.0);

    when(analyticsService.getAggregatedDataValuesGrid(disagParamsFor(programIndicatorJ)))
        .thenReturn(disagGrid);

    FoundDimensionItemValue expected1 =
        new FoundDimensionItemValue(orgUnitA, periodA, cocE, aocC, programIndicatorJ, 1.0);
    FoundDimensionItemValue expected2 =
        new FoundDimensionItemValue(orgUnitA, periodA, cocF, aocC, programIndicatorJ, 2.0);

    // ---------------------------------------------------------------------
    // Program indicator with only a category combo is fetched as usual,
    // because the predictions are not disaggregated.
    // ---------------------------------------------------------------------

    ProgramIndicator programIndicatorL =
        createProgramIndicator('L', programB, "expressionL", "filterL");
    programIndicatorL.setCategoryCombo(createCategoryCombo('L'));

    Grid aocGrid = newAocGrid();

    aocGrid
        .addRow()
        .addValue(periodB.getIsoDate())
        .addValue(programIndicatorL.getUid())
        .addValue(orgUnitB.getUid())
        .addValue(aocD.getUid())
        .addValue(3.0);

    when(analyticsService.getAggregatedDataValues(aocParamsFor(programIndicatorL)))
        .thenReturn(aocGrid);

    FoundDimensionItemValue expected3 =
        new FoundDimensionItemValue(orgUnitB, periodB, null, aocD, programIndicatorL, 3.0);

    // ---------------------------------------------------------------------
    // Do the test
    // ---------------------------------------------------------------------

    fetcher.init(periods, Set.of(programIndicatorJ, programIndicatorL));

    List<FoundDimensionItemValue> actual = fetcher.getValues(orgUnits);

    assertContainsOnly(List.of(expected1, expected2, expected3), actual);
  }

  @Test
  void testGetValuesWithAttributeComboOnlyDisaggregated() {
    fetcher = new PredictionAnalyticsDataFetcher(analyticsService, categoryService, true);

    CategoryOptionCombo aocC = createCategoryOptionCombo('C');

    when(categoryService.getCategoryOptionCombo(aocC.getUid())).thenReturn(aocC);

    // Program indicator with only an attribute combo is fetched with its
    // mapped option combos. It has no COC, so it is used for all COCs.
    ProgramIndicator programIndicatorM =
        createProgramIndicator('M', programA, "expressionM", "filterM");
    programIndicatorM.setAttributeCombo(createCategoryCombo('M'));

    Grid disagGrid = newDisagGrid();

    disagGrid
        .addRow()
        .addValue(programIndicatorM.getUid())
        .addValue(periodA.getIsoDate())
        .addValue(orgUnitA.getUid())
        .addValue(null)
        .addValue(aocC.getUid())
        .addValue(5.0);

    when(analyticsService.getAggregatedDataValuesGrid(disagParamsFor(programIndicatorM)))
        .thenReturn(disagGrid);

    FoundDimensionItemValue expected =
        new FoundDimensionItemValue(orgUnitA, periodA, null, aocC, programIndicatorM, 5.0);

    fetcher.init(periods, Set.of(programIndicatorM));

    List<FoundDimensionItemValue> actual = fetcher.getValues(orgUnits);

    assertContainsOnly(List.of(expected), actual);
  }

  @Test
  void testGetValuesWithUnknownCategoryOptionCombo() {
    fetcher = new PredictionAnalyticsDataFetcher(analyticsService, categoryService, true);

    ProgramIndicator programIndicatorE =
        createProgramIndicator('E', programA, "expressionE", "filterE");
    programIndicatorE.setCategoryCombo(createCategoryCombo('E'));

    Grid disagGrid = newDisagGrid();

    disagGrid
        .addRow()
        .addValue(programIndicatorE.getUid())
        .addValue(periodA.getIsoDate())
        .addValue(orgUnitA.getUid())
        .addValue("unknownCoc1")
        .addValue(null)
        .addValue(1.0);

    when(analyticsService.getAggregatedDataValuesGrid(disagParamsFor(programIndicatorE)))
        .thenReturn(disagGrid);

    fetcher.init(periods, Set.of(programIndicatorE));

    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> fetcher.getValues(orgUnits));
    assertEquals(
        "Can't find category option combo unknownCoc1 returned by analytics", ex.getMessage());
  }

  @Test
  void testGetValuesEmpty() {
    fetcher.init(periods, Collections.emptySet());

    List<FoundDimensionItemValue> actual = fetcher.getValues(orgUnits);

    assertIsEmpty(actual);
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  /**
   * Matches the query parameters for a disaggregated program indicator. (DataQueryParams.equals
   * doesn't compare dimension items, so the program indicator is also checked.)
   */
  private DataQueryParams disagParamsFor(ProgramIndicator pi) {
    DataQueryParams expected =
        DataQueryParams.newBuilder()
            .withPeriods(periods.stream().map(PeriodDimension::of).toList())
            .withDataDimensionItems(List.of(pi))
            .withOrganisationUnits(orgUnits)
            .build();

    return argThat(p -> expected.equals(p) && List.of(pi).equals(p.getProgramIndicators()));
  }

  /** Makes the query parameters for an item fetched by attribute option combo. */
  private DataQueryParams aocParamsFor(DimensionalItemObject item) {
    DataQueryParams expected =
        DataQueryParams.newBuilder()
            .withPeriods(periods.stream().map(PeriodDimension::of).toList())
            .withDataDimensionItems(List.of(item))
            .withOrganisationUnits(orgUnits)
            .withAttributeOptionCombos(Collections.emptyList())
            .build();

    return argThat(p -> expected.equals(p) && List.of(item).equals(p.getAllDataDimensionItems()));
  }

  /** Makes an empty grid as returned for items fetched by attribute option combo. */
  private Grid newAocGrid() {
    Grid grid = new ListGrid();

    grid.addHeader(new GridHeader(PERIOD_DIM_ID, "Period", ValueType.TEXT, false, true));
    grid.addHeader(new GridHeader(DATA_X_DIM_ID, "DimensionItem", ValueType.TEXT, false, true));
    grid.addHeader(new GridHeader(ORGUNIT_DIM_ID, "OrganisationUnit", ValueType.TEXT, false, true));
    grid.addHeader(new GridHeader(ATTRIBUTEOPTIONCOMBO_DIM_ID, "AOC", ValueType.TEXT, false, true));
    grid.addHeader(new GridHeader("value", "Value", ValueType.NUMBER, false, true));

    return grid;
  }

  /** Makes an empty grid in data value set format, as returned for a disaggregated PI. */
  private Grid newDisagGrid() {
    Grid grid = new ListGrid();

    grid.addHeader(new GridHeader(DATA_X_DIM_ID, "Data", ValueType.TEXT, false, true));
    grid.addHeader(new GridHeader(PERIOD_DIM_ID, "Period", ValueType.TEXT, false, true));
    grid.addHeader(new GridHeader(ORGUNIT_DIM_ID, "OrgUnit", ValueType.TEXT, false, true));
    grid.addHeader(new GridHeader(CATEGORYOPTIONCOMBO_DIM_ID, "COC", ValueType.TEXT, false, true));
    grid.addHeader(new GridHeader(ATTRIBUTEOPTIONCOMBO_DIM_ID, "AOC", ValueType.TEXT, false, true));
    grid.addHeader(new GridHeader("value", "Value", ValueType.NUMBER, false, true));

    return grid;
  }
}
