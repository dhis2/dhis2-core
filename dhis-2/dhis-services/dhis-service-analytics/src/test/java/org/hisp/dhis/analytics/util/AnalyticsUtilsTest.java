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
package org.hisp.dhis.analytics.util;

import static org.hisp.dhis.analytics.DataQueryParams.VALUE_HEADER_NAME;
import static org.hisp.dhis.analytics.DataQueryParams.VALUE_ID;
import static org.hisp.dhis.common.DimensionConstants.ATTRIBUTEOPTIONCOMBO_DIM_ID;
import static org.hisp.dhis.common.DimensionConstants.CATEGORYOPTIONCOMBO_DIM_ID;
import static org.hisp.dhis.common.DimensionConstants.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionConstants.DIMENSION_SEP;
import static org.hisp.dhis.common.DimensionConstants.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionConstants.PERIOD_DIM_ID;
import static org.hisp.dhis.db.model.DataType.BIGINT;
import static org.hisp.dhis.db.model.DataType.GEOMETRY_POINT;
import static org.hisp.dhis.db.model.DataType.TEXT;
import static org.hisp.dhis.db.model.Database.POSTGRESQL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Lists;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.apache.commons.lang3.EnumUtils;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DataDimensionItemType;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DimensionalObjectUtils;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.MetadataItem;
import org.hisp.dhis.common.QueryRuntimeException;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataelement.DataElementOperand.TotalType;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.db.model.Database;
import org.hisp.dhis.dxf2.datavalueset.DataValueSet;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.DailyPeriodType;
import org.hisp.dhis.period.FinancialAprilPeriodType;
import org.hisp.dhis.period.FinancialJulyPeriodType;
import org.hisp.dhis.period.FinancialNovemberPeriodType;
import org.hisp.dhis.period.FinancialOctoberPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramDataElementDimensionItem;
import org.hisp.dhis.program.ProgramDataElementOptionDimensionItem;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramTrackedEntityAttributeOptionDimensionItem;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.test.TestBase;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.jdbc.UncategorizedSQLException;

/**
 * @author Lars Helge Overland
 */
class AnalyticsUtilsTest extends TestBase {

  private static final String DUMMY_SQL = "SELECT * FROM dummy";

  @Test
  void testGetByDataDimensionType() {
    Program prA = createProgram('A');
    DataElement deA = createDataElement('A', new CategoryCombo());
    DataElement deB = createDataElement('B', new CategoryCombo());
    ProgramDataElementDimensionItem pdeA = new ProgramDataElementDimensionItem(prA, deA);
    ProgramDataElementDimensionItem pdeB = new ProgramDataElementDimensionItem(prA, deB);
    ProgramIndicator piA = createProgramIndicator('A', prA, null, null);
    List<DimensionalItemObject> list = List.of(deA, deB, pdeA, pdeB, piA);
    assertEquals(
        List.of(deA, deB),
        AnalyticsUtils.getByDataDimensionItemType(DataDimensionItemType.DATA_ELEMENT, list));
    assertEquals(
        List.of(pdeA, pdeB),
        AnalyticsUtils.getByDataDimensionItemType(
            DataDimensionItemType.PROGRAM_DATA_ELEMENT, list));
    assertEquals(
        List.of(piA),
        AnalyticsUtils.getByDataDimensionItemType(DataDimensionItemType.PROGRAM_INDICATOR, list));
    assertEquals(
        List.of(),
        AnalyticsUtils.getByDataDimensionItemType(DataDimensionItemType.PROGRAM_ATTRIBUTE, list));
  }

  @Test
  void testConvertDxToOperandCocOnly() {
    Map<String, Double> map = new HashMap<>();
    map.put("GauDLAiXPKT-kC1OT9Q1n1j-R9U8q7X1aJG", 10d);
    map.put("YkRvCLedQa4-h1dJ9W4dWor-Zrd4DAf8M99", 11d);
    map.put("PcfRp1HETO8-zqXKIEycBck-KBJBZopYMPV", 12d);
    Map<String, Double> convertedMap = AnalyticsUtils.convertDxToOperand(map, TotalType.COC_ONLY);
    assertTrue(convertedMap.containsKey("GauDLAiXPKT.kC1OT9Q1n1j-R9U8q7X1aJG"));
    assertTrue(convertedMap.containsKey("YkRvCLedQa4.h1dJ9W4dWor-Zrd4DAf8M99"));
    assertTrue(convertedMap.containsKey("PcfRp1HETO8.zqXKIEycBck-KBJBZopYMPV"));
  }

  @Test
  void testConvertDxToOperandCocOnlyNoDmensions() {
    Map<String, Double> map = new HashMap<>();
    map.put("GauDLAiXPKT-kC1OT9Q1n1j", 10d);
    map.put("YkRvCLedQa4-h1dJ9W4dWor", 11d);
    map.put("PcfRp1HETO8-zqXKIEycBck", 12d);
    Map<String, Double> convertedMap = AnalyticsUtils.convertDxToOperand(map, TotalType.COC_ONLY);
    assertTrue(convertedMap.containsKey("GauDLAiXPKT.kC1OT9Q1n1j"));
    assertTrue(convertedMap.containsKey("YkRvCLedQa4.h1dJ9W4dWor"));
    assertTrue(convertedMap.containsKey("PcfRp1HETO8.zqXKIEycBck"));
  }

  @Test
  void testConvertDxToOperandAocOnly() {
    Map<String, Double> map = new HashMap<>();
    map.put("GauDLAiXPKT-kC1OT9Q1n1j-2016", 10d);
    map.put("YkRvCLedQa4-h1dJ9W4dWor-2017", 11d);
    map.put("w1G4l0cSxOi-gQhAMdimKO4-2017", 12d);
    Map<String, Double> convertedMap = AnalyticsUtils.convertDxToOperand(map, TotalType.AOC_ONLY);
    assertTrue(convertedMap.containsKey("GauDLAiXPKT.*.kC1OT9Q1n1j-2016"));
    assertTrue(convertedMap.containsKey("YkRvCLedQa4.*.h1dJ9W4dWor-2017"));
    assertTrue(convertedMap.containsKey("w1G4l0cSxOi.*.gQhAMdimKO4-2017"));
  }

  @Test
  void testConvertDxToOperandCocAndAoc() {
    Map<String, Double> map = new HashMap<>();
    map.put("GauDLAiXPKT-kC1OT9Q1n1j-R9U8q7X1aJG-201701", 10d);
    map.put("YkRvCLedQa4-h1dJ9W4dWor-Zrd4DAf8M99-201702", 11d);
    map.put("PcfRp1HETO8-zqXKIEycBck-KBJBZopYMPV-201703", 12d);
    Map<String, Double> convertedMap =
        AnalyticsUtils.convertDxToOperand(map, TotalType.COC_AND_AOC);
    assertTrue(convertedMap.containsKey("GauDLAiXPKT.kC1OT9Q1n1j.R9U8q7X1aJG-201701"));
    assertTrue(convertedMap.containsKey("YkRvCLedQa4.h1dJ9W4dWor.Zrd4DAf8M99-201702"));
    assertTrue(convertedMap.containsKey("PcfRp1HETO8.zqXKIEycBck.KBJBZopYMPV-201703"));
  }

  @Test
  void testConvertDxToOperandNone() {
    Map<String, Double> map = new HashMap<>();
    map.put("GauDLAiXPKT-kC1OT9Q1n1j-R9U8q7X1aJG", 10d);
    map.put("YkRvCLedQa4-h1dJ9W4dWor-Zrd4DAf8M99", 11d);
    map.put("PcfRp1HETO8-zqXKIEycBck-KBJBZopYMPV", 12d);
    Map<String, Double> convertedMap = AnalyticsUtils.convertDxToOperand(map, TotalType.NONE);
    assertTrue(convertedMap.containsKey("GauDLAiXPKT-kC1OT9Q1n1j-R9U8q7X1aJG"));
    assertTrue(convertedMap.containsKey("YkRvCLedQa4-h1dJ9W4dWor-Zrd4DAf8M99"));
    assertTrue(convertedMap.containsKey("PcfRp1HETO8-zqXKIEycBck-KBJBZopYMPV"));
  }

  @Test
  void testGetRoundedValueObject() {
    DataQueryParams paramsA = DataQueryParams.newBuilder().build();
    DataQueryParams paramsB = DataQueryParams.newBuilder().withSkipRounding(true).build();
    assertNull(AnalyticsUtils.getRoundedValueObject(paramsA, null), "Should be null");
    assertEquals(
        "Car", AnalyticsUtils.getRoundedValueObject(paramsA, "Car"), "Should be a String: Car");
    assertEquals(
        3L, AnalyticsUtils.getRoundedValueObject(paramsA, 3d), "Should be a long value: 3");
    assertEquals(
        1000L,
        AnalyticsUtils.getRoundedValueObject(paramsA, 1000.00000000),
        "Should be a long value: 1000");
    assertEquals(
        67L, AnalyticsUtils.getRoundedValueObject(paramsA, 67.0), "Should be a long value: 67");
    assertEquals(
        3.12,
        (Double) AnalyticsUtils.getRoundedValueObject(paramsA, 3.123),
        0.01,
        "Should be a double value: 3.1");
    assertEquals(
        3.123,
        (Double) AnalyticsUtils.getRoundedValueObject(paramsB, 3.123),
        0.01,
        "Should be a double value: 3.123");
  }

  @Test
  void testEndsWithZeroDecimal() {
    assertFalse(
        AnalyticsUtils.endsWithZeroAsDecimal(-20.4), "The value -20.4 has non-zero decimals");
    assertFalse(
        AnalyticsUtils.endsWithZeroAsDecimal(20.000000001),
        "The value 20.000000001 has non-zero decimals");
    assertFalse(
        AnalyticsUtils.endsWithZeroAsDecimal(1000000.000000001),
        "The value 1000000.000000001 has non-zero decimals");

    assertTrue(AnalyticsUtils.endsWithZeroAsDecimal(-20.0), "The value -20.0 has zero decimals");
    assertTrue(
        AnalyticsUtils.endsWithZeroAsDecimal(20.000000000),
        "The value 20.000000000 has zero decimals");
    assertTrue(
        AnalyticsUtils.endsWithZeroAsDecimal(1000000.0000),
        "The value 1000000.0000 has zero decimals");
  }

  @Test
  void testGetRoundedValueDouble() {
    DataQueryParams paramsA = DataQueryParams.newBuilder().build();
    DataQueryParams paramsB = DataQueryParams.newBuilder().withSkipRounding(true).build();
    assertNull(AnalyticsUtils.getRoundedValue(paramsA, null, (Double) null));
    assertEquals(3d, AnalyticsUtils.getRoundedValue(paramsA, null, 3d).doubleValue(), 0.01);
    assertEquals(3.12, AnalyticsUtils.getRoundedValue(paramsA, null, 3.123).doubleValue(), 0.01);
    assertEquals(3.1, AnalyticsUtils.getRoundedValue(paramsA, 1, 3.123).doubleValue(), 0.01);
    assertEquals(3.12, AnalyticsUtils.getRoundedValue(paramsA, 2, 3.123).doubleValue(), 0.01);
    assertEquals(3.123, AnalyticsUtils.getRoundedValue(paramsB, 3, 3.123).doubleValue(), 0.01);
    assertEquals(3L, AnalyticsUtils.getRoundedValue(paramsB, 0, 3.123).longValue());
    assertEquals(12L, AnalyticsUtils.getRoundedValue(paramsB, 0, 12.34).longValue());
    assertEquals(13L, AnalyticsUtils.getRoundedValue(paramsB, 0, 13.999).longValue());
  }

  @Test
  void testGetAggregatedDataValueMapping() {
    Grid grid = new ListGrid();
    grid.addRow();
    grid.addValue("de1");
    grid.addValue("ou2");
    grid.addValue("pe1");
    grid.addValue(3);
    grid.addRow();
    grid.addValue("de2");
    grid.addValue("ou3");
    grid.addValue("pe2");
    grid.addValue(5);
    Map<String, Object> map = AnalyticsUtils.getAggregatedDataValueMapping(grid);
    assertEquals(3, map.get("de1" + DIMENSION_SEP + "ou2" + DIMENSION_SEP + "pe1"));
    assertEquals(5, map.get("de2" + DIMENSION_SEP + "ou3" + DIMENSION_SEP + "pe2"));
  }

  @Test
  void testGetDimensionalItemObjectMap() {
    DataElement deA = createDataElement('A');
    Indicator inA = createIndicator('A', null);
    DataSet dsA = createDataSet('A');
    DimensionalObject dx =
        new BaseDimensionalObject(
            DATA_X_DIM_ID, DimensionType.DATA_X, DimensionalObjectUtils.getList(deA, inA, dsA));
    DataQueryParams params =
        DataQueryParams.newBuilder()
            .addDimension(dx)
            .withDisplayProperty(DisplayProperty.NAME)
            .build();
    Map<String, DimensionalItemObject> map = AnalyticsUtils.getDimensionalItemObjectMap(params);
    assertEquals(map.get(deA.getDimensionItem()), deA);
    assertEquals(map.get(inA.getDimensionItem()), inA);
    assertEquals(map.get(dsA.getDimensionItem()), dsA);
  }

  @Test
  void testGetDimensionItemNameMap() {
    DataElement deA = createDataElement('A');
    Indicator inA = createIndicator('A', null);
    DataSet dsA = createDataSet('A');
    OrganisationUnit ouA = createOrganisationUnit('A');
    OrganisationUnit ouB = createOrganisationUnit('B');
    DimensionalObject dx =
        new BaseDimensionalObject(
            DATA_X_DIM_ID, DimensionType.DATA_X, DimensionalObjectUtils.getList(deA, inA, dsA));
    DimensionalObject ou =
        new BaseDimensionalObject(
            ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, List.of(ouA, ouB));
    DataQueryParams params =
        DataQueryParams.newBuilder()
            .addDimension(dx)
            .addDimension(ou)
            .withDisplayProperty(DisplayProperty.NAME)
            .build();
    Map<String, String> map = AnalyticsUtils.getDimensionItemNameMap(params);
    assertEquals(map.get(deA.getDimensionItem()), deA.getDisplayName());
    assertEquals(map.get(inA.getDimensionItem()), inA.getDisplayName());
    assertEquals(map.get(dsA.getDimensionItem()), dsA.getDisplayName());
    assertEquals(map.get(ouA.getDimensionItem()), ouA.getDisplayName());
    assertEquals(map.get(ouB.getDimensionItem()), ouB.getDisplayName());
  }

  @Test
  void testGetCocNameMap() {
    CategoryCombo ccA = createCategoryCombo('A', new Category[0]);
    CategoryCombo ccB = createCategoryCombo('B', new Category[0]);
    CategoryOptionCombo cocA = createCategoryOptionCombo('A');
    CategoryOptionCombo cocB = createCategoryOptionCombo('B');
    ccA.getOptionCombos().add(cocA);
    ccB.getOptionCombos().add(cocB);
    DataElement deA = createDataElement('A');
    DataElement deB = createDataElement('B');
    deA.setCategoryCombo(ccA);
    deB.setCategoryCombo(ccB);
    DimensionalObject dx =
        new BaseDimensionalObject(DATA_X_DIM_ID, DimensionType.DATA_X, List.of(deA, deB));
    DataQueryParams params =
        DataQueryParams.newBuilder()
            .addDimension(dx)
            .withDisplayProperty(DisplayProperty.NAME)
            .build();
    Map<String, String> map = AnalyticsUtils.getCocNameMap(params);
    assertEquals(map.get(cocA.getUid()), cocA.getName());
    assertEquals(map.get(cocB.getUid()), cocB.getName());
  }

  @Test
  void testHandleGridForDataValueSetEmpty() {
    Grid grid = new ListGrid();
    DataQueryParams params =
        DataQueryParams.newBuilder()
            .addDimension(new BaseDimensionalObject(DATA_X_DIM_ID, DimensionType.DATA_X, List.of()))
            .build();
    grid.addHeader(new GridHeader(DATA_X_DIM_ID));
    grid.addHeader(new GridHeader(ORGUNIT_DIM_ID));
    grid.addHeader(new GridHeader(PERIOD_DIM_ID));
    grid.addHeader(new GridHeader(VALUE_ID, VALUE_HEADER_NAME, ValueType.NUMBER, false, false));
    assertEquals(4, grid.getHeaders().size());
    assertEquals(0, grid.getWidth());
    assertEquals(0, grid.getHeight());
    AnalyticsUtils.handleGridForDataValueSet(params, grid);
    assertEquals(6, grid.getHeaders().size());
    assertEquals(0, grid.getWidth());
    assertEquals(0, grid.getHeight());
  }

  @Test
  void testHandleGridForDataValueSet() {
    IndicatorType itA = new IndicatorType();
    CategoryOptionCombo ocA = createCategoryOptionCombo('A');
    ocA.setUid("ceabcdefghA");
    DataElement dxA = createDataElement('A');
    dxA.setUid("deabcdefghA");
    dxA.setValueType(ValueType.INTEGER);
    DataElement dxB = createDataElement('B');
    dxB.setUid("deabcdefghB");
    dxB.setValueType(ValueType.NUMBER);
    Indicator dxC = createIndicator('C', itA);
    dxC.setUid("deabcdefghC");
    dxC.setDecimals(0);
    dxC.setAggregateExportAttributeOptionCombo("ceabcdefghA");
    Indicator dxD = createIndicator('D', itA);
    dxD.setUid("deabcdefghD");
    dxD.setDecimals(2);
    dxD.setAggregateExportCategoryOptionCombo("ceabcdefghB");
    DataElementOperand dxE = new DataElementOperand(dxA, ocA);
    DataElementOperand dxF = new DataElementOperand(dxB, ocA);
    DataQueryParams params =
        DataQueryParams.newBuilder()
            .addDimension(
                new BaseDimensionalObject(
                    DATA_X_DIM_ID, DimensionType.DATA_X, List.of(dxA, dxB, dxC, dxD, dxE, dxF)))
            .build();
    Grid grid = new ListGrid();
    grid.addHeader(new GridHeader(DATA_X_DIM_ID));
    grid.addHeader(new GridHeader(ORGUNIT_DIM_ID));
    grid.addHeader(new GridHeader(PERIOD_DIM_ID));
    grid.addHeader(new GridHeader(VALUE_ID, VALUE_HEADER_NAME, ValueType.NUMBER, false, false));
    grid.addRow().addValuesAsList(Lists.newArrayList("deabcdefghA", "ouA", "peA", 1d));
    grid.addRow().addValuesAsList(Lists.newArrayList("deabcdefghB", "ouA", "peA", 2d));
    grid.addRow().addValuesAsList(Lists.newArrayList("deabcdefghC", "ouA", "peA", 3d));
    grid.addRow().addValuesAsList(Lists.newArrayList("deabcdefghD", "ouA", "peA", 4d));
    grid.addRow().addValuesAsList(Lists.newArrayList("deabcdefghA.ceabcdefghA", "ouA", "peA", 5d));
    grid.addRow().addValuesAsList(Lists.newArrayList("deabcdefghB.ceabcdefghA", "ouA", "peA", 6d));
    assertEquals(4, grid.getWidth());
    assertEquals(6, grid.getHeight());
    AnalyticsUtils.handleGridForDataValueSet(params, grid);
    assertEquals(6, grid.getWidth());
    assertEquals(6, grid.getHeight());
    assertEquals("deabcdefghA", grid.getRow(0).get(0));
    assertNull(grid.getRow(0).get(3));
    assertNull(grid.getRow(0).get(4));
    assertEquals(1, grid.getRow(0).get(5));
    assertEquals("deabcdefghB", grid.getRow(1).get(0));
    assertNull(grid.getRow(1).get(3));
    assertNull(grid.getRow(1).get(4));
    assertEquals(0.01, (Double) grid.getRow(1).get(5), 2d);
    assertEquals("deabcdefghC", grid.getRow(2).get(0));
    assertNull(grid.getRow(2).get(3));
    assertEquals("ceabcdefghA", grid.getRow(2).get(4));
    assertEquals(3, grid.getRow(2).get(5));
    assertEquals("deabcdefghD", grid.getRow(3).get(0));
    assertEquals("ceabcdefghB", grid.getRow(3).get(3));
    assertNull(grid.getRow(3).get(4));
    assertEquals(0.01, (Double) grid.getRow(3).get(5), 4d);
    assertEquals("deabcdefghA", grid.getRow(4).get(0));
    assertEquals("ceabcdefghA", grid.getRow(4).get(3));
    assertNull(grid.getRow(4).get(4));
    assertEquals(5, grid.getRow(4).get(5));
    assertEquals("deabcdefghB", grid.getRow(5).get(0));
    assertEquals("ceabcdefghA", grid.getRow(5).get(3));
    assertNull(grid.getRow(5).get(4));
    assertEquals(0.01, (Double) grid.getRow(5).get(5), 6d);
  }

  @Test
  void testHandleGridForDataValueSetWithProgramIndicatorDisaggregation() {
    Program program = createProgram('A');
    ProgramIndicator programIndicator = createProgramIndicator('A', program, ".", ".");
    programIndicator.setUid("programIndA");
    programIndicator.setAggregateExportCategoryOptionCombo("AggExporCOC");
    programIndicator.setAggregateExportAttributeOptionCombo("AggExporAOC");
    programIndicator.setAggregateExportDataElement("AggExportDE");

    DataQueryParams params =
        DataQueryParams.newBuilder()
            .addDimension(
                new BaseDimensionalObject(
                    DATA_X_DIM_ID, DimensionType.DATA_X, List.of(programIndicator)))
            .build();

    Grid grid = new ListGrid();
    grid.addHeader(new GridHeader(DATA_X_DIM_ID));
    grid.addHeader(new GridHeader(ORGUNIT_DIM_ID));
    grid.addHeader(new GridHeader(PERIOD_DIM_ID));
    grid.addHeader(new GridHeader(VALUE_ID, VALUE_HEADER_NAME, ValueType.NUMBER, false, false));
    grid.addRow().addValuesAsList(List.of("programIndA", "ouA", "peA", 1d));
    grid.addRow().addValuesAsList(List.of("programIndA.CatOptCombo.*", "ouA", "peA", 2d));
    grid.addRow().addValuesAsList(List.of("programIndA.*.AttOptCombo", "ouA", "peA", 3d));
    grid.addRow().addValuesAsList(List.of("programIndA.CatOptCombo.AttOptCombo", "ouA", "peA", 4d));

    assertEquals(4, grid.getWidth());
    assertEquals(4, grid.getHeight());

    AnalyticsUtils.handleGridForDataValueSet(params, grid);

    assertEquals(6, grid.getWidth());
    assertEquals(4, grid.getHeight());

    assertEquals("AggExportDE", grid.getRow(0).get(0));
    assertEquals("AggExporCOC", grid.getRow(0).get(3));
    assertEquals("AggExporAOC", grid.getRow(0).get(4));
    assertEquals(1.0, grid.getRow(0).get(5));

    assertEquals("AggExportDE", grid.getRow(1).get(0));
    assertEquals("CatOptCombo", grid.getRow(1).get(3));
    assertEquals("AggExporAOC", grid.getRow(1).get(4));
    assertEquals(2.0, grid.getRow(1).get(5));

    assertEquals("AggExportDE", grid.getRow(2).get(0));
    assertEquals("AggExporCOC", grid.getRow(2).get(3));
    assertEquals("AttOptCombo", grid.getRow(2).get(4));
    assertEquals(3.0, grid.getRow(2).get(5));

    assertEquals("AggExportDE", grid.getRow(3).get(0));
    assertEquals("CatOptCombo", grid.getRow(3).get(3));
    assertEquals("AttOptCombo", grid.getRow(3).get(4));
    assertEquals(4.0, grid.getRow(3).get(5));
  }

  @Test
  void testGetColumnType() {
    assertEquals(BIGINT, AnalyticsUtils.getColumnType(ValueType.INTEGER, true));
    assertEquals(GEOMETRY_POINT, AnalyticsUtils.getColumnType(ValueType.COORDINATE, true));
    assertEquals(TEXT, AnalyticsUtils.getColumnType(ValueType.COORDINATE, false));
  }

  @Test
  void testGetDataValueSetFromGridEmpty() {
    Grid grid = new ListGrid();
    grid.addHeader(new GridHeader(DATA_X_DIM_ID));
    grid.addHeader(new GridHeader(ORGUNIT_DIM_ID));
    grid.addHeader(new GridHeader(PERIOD_DIM_ID));
    grid.addHeader(new GridHeader(CATEGORYOPTIONCOMBO_DIM_ID));
    grid.addHeader(new GridHeader(ATTRIBUTEOPTIONCOMBO_DIM_ID));
    grid.addHeader(new GridHeader(VALUE_ID, VALUE_HEADER_NAME, ValueType.NUMBER, false, false));
    DataValueSet dvs = AnalyticsUtils.getDataValueSet(DataQueryParams.newBuilder().build(), grid);
    assertNotNull(dvs);
    assertNotNull(dvs.getDataValues());
    assertEquals(0, dvs.getDataValues().size());
  }

  @Test
  void testGetDataValueSetFromGrid() {
    Grid grid = new ListGrid();
    grid.addHeader(new GridHeader(DATA_X_DIM_ID));
    grid.addHeader(new GridHeader(ORGUNIT_DIM_ID));
    grid.addHeader(new GridHeader(PERIOD_DIM_ID));
    grid.addHeader(new GridHeader(CATEGORYOPTIONCOMBO_DIM_ID));
    grid.addHeader(new GridHeader(ATTRIBUTEOPTIONCOMBO_DIM_ID));
    grid.addHeader(new GridHeader(VALUE_ID, VALUE_HEADER_NAME, ValueType.NUMBER, false, false));
    grid.addRow().addValuesAsList(Lists.newArrayList("dxA", "ouA", "peA", "coA", "aoA", 1d));
    grid.addRow().addValuesAsList(Lists.newArrayList("dxA", "ouA", "peB", null, null, 2d));
    grid.addRow().addValuesAsList(Lists.newArrayList("dxA", "ouB", "peA", null, null, 3d));
    grid.addRow().addValuesAsList(Lists.newArrayList("dxA", "ouB", "peB", null, null, 4d));
    grid.addRow().addValuesAsList(Lists.newArrayList("dxB", "ouA", "peA", "coA", null, 5d));
    grid.addRow().addValuesAsList(Lists.newArrayList("dxB", "ouA", "peB", "coA", "aoB", 6d));
    grid.addRow().addValuesAsList(Lists.newArrayList("dxC", "ouA", "peA", null, "aoA", 7));
    grid.addRow().addValuesAsList(Lists.newArrayList("dxC", "ouA", "peB", null, null, 8d));
    grid.addRow().addValuesAsList(Lists.newArrayList("dxD", "ouA", "peA", "coB", null, 9d));
    grid.addRow().addValuesAsList(Lists.newArrayList("dxE", "ouA", "peB", null, null, 10));

    DataValueSet dvs = AnalyticsUtils.getDataValueSet(DataQueryParams.newBuilder().build(), grid);
    assertNotNull(dvs);
    assertNotNull(dvs.getDataValues());
    assertEquals(10, dvs.getDataValues().size());

    assertEquals("dxA", dvs.getDataValues().get(1).getDataElement());
    assertEquals("ouA", dvs.getDataValues().get(1).getOrgUnit());
    assertEquals("peB", dvs.getDataValues().get(1).getPeriod());
    assertNull(dvs.getDataValues().get(1).getCategoryOptionCombo());
    assertNull(dvs.getDataValues().get(1).getAttributeOptionCombo());
    assertEquals("2.0", dvs.getDataValues().get(1).getValue());

    assertEquals("dxB", dvs.getDataValues().get(4).getDataElement());
    assertEquals("ouA", dvs.getDataValues().get(4).getOrgUnit());
    assertEquals("peA", dvs.getDataValues().get(4).getPeriod());
    assertEquals("coA", dvs.getDataValues().get(4).getCategoryOptionCombo());
    assertNull(dvs.getDataValues().get(4).getAttributeOptionCombo());
    assertEquals("5.0", dvs.getDataValues().get(4).getValue());

    assertEquals("dxC", dvs.getDataValues().get(6).getDataElement());
    assertEquals("ouA", dvs.getDataValues().get(6).getOrgUnit());
    assertEquals("peA", dvs.getDataValues().get(6).getPeriod());
    assertNull(dvs.getDataValues().get(6).getCategoryOptionCombo());
    assertEquals("aoA", dvs.getDataValues().get(6).getAttributeOptionCombo());
    assertEquals("7", dvs.getDataValues().get(6).getValue());

    assertEquals("dxD", dvs.getDataValues().get(8).getDataElement());
    assertEquals("ouA", dvs.getDataValues().get(8).getOrgUnit());
    assertEquals("peA", dvs.getDataValues().get(8).getPeriod());
    assertEquals("coB", dvs.getDataValues().get(8).getCategoryOptionCombo());
    assertNull(dvs.getDataValues().get(8).getAttributeOptionCombo());
    assertEquals("9.0", dvs.getDataValues().get(8).getValue());

    assertEquals("dxE", dvs.getDataValues().get(9).getDataElement());
    assertEquals("ouA", dvs.getDataValues().get(9).getOrgUnit());
    assertEquals("peB", dvs.getDataValues().get(9).getPeriod());
    assertNull(dvs.getDataValues().get(9).getCategoryOptionCombo());
    assertNull(dvs.getDataValues().get(9).getAttributeOptionCombo());
    assertEquals("10", dvs.getDataValues().get(9).getValue());
  }

  @Test
  void testGetDataValueSetFromGridWithDuplicates() {
    Grid grid = new ListGrid();
    grid.addHeader(new GridHeader(DATA_X_DIM_ID));
    grid.addHeader(new GridHeader(ORGUNIT_DIM_ID));
    grid.addHeader(new GridHeader(PERIOD_DIM_ID));
    grid.addHeader(new GridHeader(CATEGORYOPTIONCOMBO_DIM_ID));
    grid.addHeader(new GridHeader(ATTRIBUTEOPTIONCOMBO_DIM_ID));
    grid.addHeader(new GridHeader(VALUE_ID, VALUE_HEADER_NAME, ValueType.NUMBER, false, false));
    grid.addRow().addValuesAsList(Lists.newArrayList("dxA", "ouA", "peA", null, null, 1d));
    grid.addRow().addValuesAsList(Lists.newArrayList("dxA", "ouA", "peB", null, null, 2d));
    // Duplicate
    grid.addRow().addValuesAsList(Lists.newArrayList("dxA", "ouA", "peB", null, null, 2d));
    grid.addRow().addValuesAsList(Lists.newArrayList("dxA", "ouB", "peA", null, null, 3d));
    grid.addRow().addValuesAsList(Lists.newArrayList("dxA", "ouB", "peB", null, null, 4d));
    grid.addRow().addValuesAsList(Lists.newArrayList("dxB", "ouA", "peA", null, null, 5d));
    // Duplicate
    grid.addRow().addValuesAsList(Lists.newArrayList("dxB", "ouA", "peA", null, null, 5d));
    grid.addRow().addValuesAsList(Lists.newArrayList("dxB", "ouA", "peB", null, null, 6d));
    grid.addRow().addValuesAsList(Lists.newArrayList("dxC", "ouA", "peA", null, null, 7d));
    // Duplicate
    grid.addRow().addValuesAsList(Lists.newArrayList("dxC", "ouA", "peA", null, null, 7d));
    grid.addRow().addValuesAsList(Lists.newArrayList("dxC", "ouA", "peB", null, null, 8d));
    DataValueSet dvs =
        AnalyticsUtils.getDataValueSet(
            DataQueryParams.newBuilder().withDuplicatesOnly(true).build(), grid);
    assertNotNull(dvs);
    assertNotNull(dvs.getDataValues());
    assertEquals(3, dvs.getDataValues().size());
  }

  @Test
  void testGetDataValueSetAsGridFromGrid() {
    Grid grid = new ListGrid();
    grid.addHeader(new GridHeader(DATA_X_DIM_ID));
    grid.addHeader(new GridHeader(ORGUNIT_DIM_ID));
    grid.addHeader(new GridHeader(PERIOD_DIM_ID));
    grid.addHeader(new GridHeader(CATEGORYOPTIONCOMBO_DIM_ID));
    grid.addHeader(new GridHeader(ATTRIBUTEOPTIONCOMBO_DIM_ID));
    grid.addHeader(new GridHeader(VALUE_ID, VALUE_HEADER_NAME, ValueType.NUMBER, false, false));
    grid.addRow().addValuesAsList(Lists.newArrayList("dxA", "ouA", "peA", "coA", "aoA", 1d));
    grid.addRow().addValuesAsList(Lists.newArrayList("dxA", "ouA", "peB", "coB", "aoB", 2d));
    grid.addRow().addValuesAsList(Lists.newArrayList("dxA", "ouB", "peA", null, null, 3d));
    grid.addRow().addValuesAsList(Lists.newArrayList("dxA", "ouB", "peB", "coA", null, 4d));
    grid.addRow().addValuesAsList(Lists.newArrayList("dxB", "ouA", "peA", "coA", null, 5d));
    grid.addRow().addValuesAsList(Lists.newArrayList("dxB", "ouA", "peB", "coA", "aoB", 6d));
    grid.addRow().addValuesAsList(Lists.newArrayList("dxC", "ouA", "peA", null, "aoA", 7));
    grid.addRow().addValuesAsList(Lists.newArrayList("dxC", "ouA", "peB", null, null, 8d));
    grid.addRow().addValuesAsList(Lists.newArrayList("dxD", "ouA", "peA", "coB", null, 9d));
    grid.addRow().addValuesAsList(Lists.newArrayList("dxE", "ouA", "peB", null, null, 10));

    Grid dvs = AnalyticsUtils.getDataValueSetAsGrid(grid);
    assertNotNull(dvs);
    assertEquals(10, dvs.getHeight());
    assertEquals(11, dvs.getWidth());

    assertEquals("data_element", dvs.getHeaders().get(0).getName());
    assertEquals("organisation_unit", dvs.getHeaders().get(2).getName());
    assertEquals("value", dvs.getHeaders().get(5).getName());
    assertEquals("comment", dvs.getHeaders().get(9).getName());

    assertEquals("dxA", dvs.getRow(1).get(0));
    assertEquals("peB", dvs.getRow(1).get(1));
    assertEquals("ouA", dvs.getRow(1).get(2));
    assertEquals("coB", dvs.getRow(1).get(3));
    assertEquals("aoB", dvs.getRow(1).get(4));
    assertEquals(2d, dvs.getRow(1).get(5));
    assertNull(dvs.getRow(1).get(6));

    assertEquals("dxA", dvs.getRow(3).get(0));
    assertEquals("peB", dvs.getRow(3).get(1));
    assertEquals("ouB", dvs.getRow(3).get(2));
    assertEquals("coA", dvs.getRow(3).get(3));
    assertNull(dvs.getRow(3).get(4));
    assertEquals(4d, dvs.getRow(3).get(5));
    assertNull(dvs.getRow(3).get(6));

    assertEquals("dxC", dvs.getRow(6).get(0));
    assertEquals("peA", dvs.getRow(6).get(1));
    assertEquals("ouA", dvs.getRow(6).get(2));
    assertNull(dvs.getRow(6).get(3));
    assertEquals("aoA", dvs.getRow(6).get(4));
    assertEquals(7, dvs.getRow(6).get(5));
    assertNull(dvs.getRow(6).get(6));
  }

  @Test
  void testGetDataValueSetAsGridFromGridMissingOrgUnitColumn() {
    Grid grid = new ListGrid();
    grid.addHeader(new GridHeader(DATA_X_DIM_ID));
    grid.addHeader(new GridHeader(PERIOD_DIM_ID));
    grid.addHeader(new GridHeader(CATEGORYOPTIONCOMBO_DIM_ID));
    grid.addHeader(new GridHeader(ATTRIBUTEOPTIONCOMBO_DIM_ID));
    grid.addHeader(new GridHeader(VALUE_ID, VALUE_HEADER_NAME, ValueType.NUMBER, false, false));
    grid.addRow().addValuesAsList(Lists.newArrayList("dxA", "peA", "coA", "aoA", 1d));

    assertThrows(IllegalArgumentException.class, () -> AnalyticsUtils.getDataValueSetAsGrid(grid));
  }

  @Test
  void testIsPeriodOverApprovalThreshold() {
    java.util.Calendar calendar = java.util.Calendar.getInstance();
    int currentYear = calendar.get(Calendar.YEAR);
    Integer thisYear = currentYear;
    Integer oneYearAgo = currentYear - 1;
    Integer twoYearsAgo = currentYear - 2;
    Integer threeYearsAgo = currentYear - 3;
    // maxYears = 0 should always return false
    assertFalse(AnalyticsUtils.periodIsOutsideApprovalMaxYears(thisYear, 0));
    assertFalse(AnalyticsUtils.periodIsOutsideApprovalMaxYears(oneYearAgo, 0));
    assertFalse(AnalyticsUtils.periodIsOutsideApprovalMaxYears(twoYearsAgo, 0));
    assertFalse(AnalyticsUtils.periodIsOutsideApprovalMaxYears(threeYearsAgo, 0));
    // maxYears = 1 should only return true for years other than thisYear
    assertFalse(AnalyticsUtils.periodIsOutsideApprovalMaxYears(thisYear, 1));
    assertTrue(AnalyticsUtils.periodIsOutsideApprovalMaxYears(oneYearAgo, 1));
    assertTrue(AnalyticsUtils.periodIsOutsideApprovalMaxYears(twoYearsAgo, 1));
    assertTrue(AnalyticsUtils.periodIsOutsideApprovalMaxYears(threeYearsAgo, 1));
    // maxYears = 4 should only return false for all three years defined
    assertFalse(AnalyticsUtils.periodIsOutsideApprovalMaxYears(thisYear, 5));
    assertFalse(AnalyticsUtils.periodIsOutsideApprovalMaxYears(oneYearAgo, 5));
    assertFalse(AnalyticsUtils.periodIsOutsideApprovalMaxYears(twoYearsAgo, 5));
    assertFalse(AnalyticsUtils.periodIsOutsideApprovalMaxYears(threeYearsAgo, 5));
  }

  @Test
  void testGetLevelFromOrgUnitDimensionName() {
    assertEquals(3, AnalyticsUtils.getLevelFromOrgUnitDimensionName("oulevel3"));
    assertEquals(5, AnalyticsUtils.getLevelFromOrgUnitDimensionName("oulevel5"));
    assertEquals(-1, AnalyticsUtils.getLevelFromOrgUnitDimensionName("notalevel"));
    assertEquals(-1, AnalyticsUtils.getLevelFromOrgUnitDimensionName("oulevel"));
  }

  @Test
  void testGetIntegerOrValue() {
    ProgramIndicator pi = new ProgramIndicator();
    pi.setUid(CodeGenerator.generateUid());
    pi.setDimensionItemType(DimensionItemType.PROGRAM_INDICATOR);
    pi.setDecimals(0);
    DataElement de = new DataElement();
    de.setUid(CodeGenerator.generateUid());
    de.setDimensionItemType(DimensionItemType.DATA_ELEMENT);
    de.setValueType(ValueType.TEXT);
    assertEquals(5, AnalyticsUtils.getIntegerOrValue(5d, pi));
    assertEquals("Male", AnalyticsUtils.getIntegerOrValue("Male", de));
  }

  @Test
  void testCalculateYearlyWeightedAverage() {
    double avg = AnalyticsUtils.calculateYearlyWeightedAverage(10D, 20D, 9D);
    assertEquals(0, avg, 17.5);
    avg = AnalyticsUtils.calculateYearlyWeightedAverage(10D, -20D, 9D);
    assertEquals(0, avg, 12.5);
  }

  @Test
  void testGetBaseMonth() {
    assertEquals(0, AnalyticsUtils.getBaseMonth(new FinancialAprilPeriodType()), 3);
    assertEquals(0, AnalyticsUtils.getBaseMonth(new FinancialJulyPeriodType()), 6);
    assertEquals(0, AnalyticsUtils.getBaseMonth(new FinancialOctoberPeriodType()), 9);
    assertEquals(0, AnalyticsUtils.getBaseMonth(new FinancialNovemberPeriodType()), 10);
    assertEquals(0, AnalyticsUtils.getBaseMonth(new DailyPeriodType()), 0);
  }

  @Test
  void testIsPeriodInPeriods() {
    Period p1 = PeriodType.getPeriodFromIsoString("202001");
    Period p2 = PeriodType.getPeriodFromIsoString("202002");
    Period p3 = PeriodType.getPeriodFromIsoString("202003");
    List<DimensionalItemObject> periods = List.of(p1, p2, p3);
    assertTrue(AnalyticsUtils.isPeriodInPeriods("202001", periods));
    assertFalse(AnalyticsUtils.isPeriodInPeriods("202005", periods));
  }

  @Test
  void testFindDimensionalItems() {
    ProgramIndicator pi1 = new ProgramIndicator();
    pi1.setUid(CodeGenerator.generateUid());
    ProgramIndicator pi2 = new ProgramIndicator();
    pi2.setUid(CodeGenerator.generateUid());
    ProgramIndicator pi3 = new ProgramIndicator();
    pi3.setUid(CodeGenerator.generateUid());
    ProgramIndicator pi4 = new ProgramIndicator();
    pi4.setUid(pi1.getUid());
    List<DimensionalItemObject> dimensionalItems =
        AnalyticsUtils.findDimensionalItems(pi1.getUid(), List.of(pi1, pi2, pi3, pi4));
    assertEquals(2, dimensionalItems.size());
  }

  @Test
  void testHasPeriod() {
    List<Object> row = new ArrayList<>();
    row.add("someUid");
    row.add("202010");
    row.add(100D);
    // pass - index 1 is a valid iso period
    assertTrue(AnalyticsUtils.hasPeriod(row, 1));
    // fail - index 3 is too high
    assertFalse(AnalyticsUtils.hasPeriod(row, 3));
    // fail - index 4 is too high
    assertFalse(AnalyticsUtils.hasPeriod(row, 4));
    // fail - index 2 is a Double
    assertFalse(AnalyticsUtils.hasPeriod(row, 2));
    row = new ArrayList<>();
    row.add("someUid");
    row.add("2020A");
    row.add(100D);
    // pass - index 1 is not a valid period iso string
    assertFalse(AnalyticsUtils.hasPeriod(row, 1));
  }

  /** {@link EnumUtils} is case sensitive, not specified in the documentation. */
  @Test
  void testGetEnumCaseSensitivity() {
    assertEquals(POSTGRESQL, EnumUtils.getEnum(Database.class, "POSTGRESQL"));
    assertEquals(POSTGRESQL, EnumUtils.getEnum(Database.class, "PostgreSQL".toUpperCase()));
    assertEquals(POSTGRESQL, EnumUtils.getEnum(Database.class, "postgresql".toUpperCase()));
    assertEquals(POSTGRESQL, EnumUtils.getEnum(Database.class, POSTGRESQL.name()));
    assertEquals(POSTGRESQL, EnumUtils.getEnum(Database.class, POSTGRESQL.toString()));
    assertEquals(
        POSTGRESQL,
        EnumUtils.getEnum(Database.class, ConfigurationKey.ANALYTICS_DATABASE.getDefaultValue()));
    assertNull(EnumUtils.getEnum(Database.class, "PostgreSQL"));
    assertNull(EnumUtils.getEnum(Database.class, "postgresql"));
  }

  @Test
  void whenUncategorizedSQLException_withTableNotExisting_thenThrowException() {
    SQLException sqlException = new SQLException("relation does not exist", "42P01");
    UncategorizedSQLException uncategorizedSQLException =
        new UncategorizedSQLException("task", DUMMY_SQL, sqlException);
    Supplier<String> supplier =
        () -> {
          throw uncategorizedSQLException;
        };

    assertThrows(
        UncategorizedSQLException.class,
        () -> AnalyticsUtils.withExceptionHandling(supplier, false));
  }

  @Test
  void whenQueryRuntimeException_thenRethrow() {
    QueryRuntimeException queryException = new QueryRuntimeException("Test error");
    Supplier<String> supplier =
        () -> {
          throw queryException;
        };

    QueryRuntimeException thrown =
        assertThrows(
            QueryRuntimeException.class,
            () -> AnalyticsUtils.withExceptionHandling(supplier, false));
    assertEquals("Test error", thrown.getMessage());
  }

  @Test
  @DisplayName(
      "When an UncategorizedSQLException is thrown with a column not existing and multiple queries, then return empty")
  void whenUncategorizedSQLException_withColumnNotExisting_thenReturnEmpty() {
    SQLException sqlException =
        new SQLException("Unknown column 'cX5k9anHEHd' in 'ax' in FILTER clause", "HY000");
    UncategorizedSQLException uncategorizedSQLException =
        new UncategorizedSQLException("task", DUMMY_SQL, sqlException);
    Supplier<String> supplier =
        () -> {
          throw uncategorizedSQLException;
        };

    Optional<String> result = AnalyticsUtils.withExceptionHandling(supplier, true);
    assertFalse(result.isPresent());
  }

  @ParameterizedTest
  @MethodSource("metadataDetailsProvider")
  void testGetDimensionMetadataMap_with_ProgramDataElement_withOptions(
      boolean includeMetadataDetails) {
    DisplayProperty displayProperty = DisplayProperty.NAME;

    Program program = createProgram('A');
    Option option = createOption('A');
    OptionSet optionSet = createOptionSet('B', option);

    DataElement dataElement = createDataElement('D');
    dataElement.setOptionSet(optionSet);

    ProgramDataElementOptionDimensionItem pdoItem = new ProgramDataElementOptionDimensionItem();
    pdoItem.setDimensionItemType(DimensionItemType.PROGRAM_DATA_ELEMENT_OPTION);
    pdoItem.setProgram(program);
    pdoItem.setDataElement(dataElement);
    pdoItem.setOption(option);

    List<DimensionalItemObject> items = List.of(pdoItem);
    DimensionalObject programDimension =
        new BaseDimensionalObject("program", DimensionType.DATA_X, items);

    DataQueryParams params =
        DataQueryParams.newBuilder()
            .addDimension(programDimension)
            .withDisplayProperty(displayProperty)
            .withIncludeMetadataDetails(includeMetadataDetails)
            .build();

    Map<String, MetadataItem> result = AnalyticsUtils.getDimensionMetadataItemMap(params);
    String key = "%s.%s".formatted(dataElement.getUid(), optionSet.getUid());

    assertNotNull(result);
    assertEquals("OptionSetB", result.get(key).getName());

    if (includeMetadataDetails) {
      assertEquals(1, result.get(key).getOptions().size());
      assertEquals(option.getUid(), result.get(key).getOptions().get(0).get("uid"));
      assertEquals(option.getCode(), result.get(key).getOptions().get(0).get("code"));
    } else {
      assertNull(result.get(key).getOptions());
    }
  }

  private static Stream<Arguments> metadataDetailsProvider() {
    return Stream.of(
        Arguments.of(true), // with metadata details
        Arguments.of(false) // without metadata details
        );
  }

  @Test
  void testGetDimensionMetadataMap_with_ProgramTrackedAttribute_withOptions() {
    DisplayProperty displayProperty = DisplayProperty.NAME;
    boolean includeMetadataDetails = true;

    Program program = createProgram('A');
    Option option = createOption('A');
    OptionSet optionSet = createOptionSet('B', option);

    DataElement dataElement = createDataElement('D');
    dataElement.setOptionSet(optionSet);

    TrackedEntityAttribute tea = createTrackedEntityAttribute('A');

    ProgramTrackedEntityAttributeOptionDimensionItem pteaoItem =
        new ProgramTrackedEntityAttributeOptionDimensionItem();
    pteaoItem.setDimensionItemType(DimensionItemType.PROGRAM_ATTRIBUTE_OPTION);
    pteaoItem.setProgram(program);
    pteaoItem.setAttribute(tea);
    pteaoItem.setOption(option);

    List<DimensionalItemObject> items = List.of(pteaoItem);
    DimensionalObject programDimension =
        new BaseDimensionalObject("program", DimensionType.DATA_X, items);

    DataQueryParams params =
        DataQueryParams.newBuilder()
            .addDimension(programDimension)
            .withDisplayProperty(displayProperty)
            .withIncludeMetadataDetails(includeMetadataDetails)
            .build();

    Map<String, MetadataItem> result = AnalyticsUtils.getDimensionMetadataItemMap(params);
    String key = "%s.%s".formatted(tea.getUid(), optionSet.getUid());
    assertNotNull(result);
    assertEquals("OptionSetB", result.get(key).getName());
    assertEquals(1, result.get(key).getOptions().size());
    assertEquals(option.getUid(), result.get(key).getOptions().get(0).get("uid"));
    assertEquals(option.getCode(), result.get(key).getOptions().get(0).get("code"));
  }
}
