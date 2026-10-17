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
package org.hisp.dhis.analytics.data.handler;

import static org.hisp.dhis.analytics.DataQueryParams.newBuilder;
import static org.hisp.dhis.common.DimensionConstants.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionType.CATEGORY;
import static org.hisp.dhis.common.DimensionType.DATA_X;
import static org.hisp.dhis.dataelement.DataElementOperand.TotalType.AOC_ONLY;
import static org.hisp.dhis.dataelement.DataElementOperand.TotalType.COC_ONLY;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.resolver.ExpressionResolvers;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataelement.DataElementOperand.TotalType;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.system.grid.ListGrid;
import org.junit.jupiter.api.Test;

class DataHandlerTest {

  @Test
  void testOperandDataQueryParamsOnlyOperands() {
    DataElement dataElement = new DataElement("NameA");
    dataElement.setUid("uid1234567A");
    dataElement.setCode("CodeA");

    DataElementOperand dataElementOperand = new DataElementOperand(dataElement, null);

    DataQueryParams stubParams =
        newBuilder()
            .addDimension(
                new BaseDimensionalObject(DATA_X_DIM_ID, DATA_X, List.of(dataElementOperand)))
            .build();

    TotalType anyTotalType = COC_ONLY;

    DataHandler dataHandler = withNullDependencies();
    DataQueryParams params =
        dataHandler.getOperandDataQueryParams(
            stubParams, List.of(dataElementOperand), anyTotalType);

    assertEquals(1, params.getDimensions().size());
  }

  @Test
  void testOperandDataQueryParamsWithCatOptionCombo() {
    DataElement dataElement = new DataElement("NameA");
    dataElement.setUid("uid1234567A");
    dataElement.setCode("CodeA");

    CategoryOptionCombo categoryOptionCombo = new CategoryOptionCombo();
    categoryOptionCombo.setName("NameC");
    categoryOptionCombo.setUid("uid1234567C");
    categoryOptionCombo.setCode("CodeC");

    DataElementOperand dataElementOperand =
        new DataElementOperand(dataElement, categoryOptionCombo);

    DataQueryParams stubParams =
        newBuilder()
            .addDimension(
                new BaseDimensionalObject(DATA_X_DIM_ID, DATA_X, List.of(dataElementOperand)))
            .build();

    DataHandler dataHandler = withNullDependencies();
    DataQueryParams params =
        dataHandler.getOperandDataQueryParams(stubParams, List.of(dataElementOperand), COC_ONLY);

    assertEquals(2, params.getDimensions().size());
  }

  @Test
  void testOperandDataQueryParamsWithCatOptionComboInFilter() {
    DataElement dataElement = new DataElement("NameA");
    dataElement.setUid("uid1234567A");
    dataElement.setCode("CodeA");

    CategoryOptionCombo categoryOptionCombo = new CategoryOptionCombo();
    categoryOptionCombo.setName("NameC");
    categoryOptionCombo.setUid("uid1234567C");
    categoryOptionCombo.setCode("CodeC");

    DataElementOperand dataElementOperand =
        new DataElementOperand(dataElement, categoryOptionCombo);

    DataQueryParams stubParams =
        newBuilder()
            .addFilter(
                new BaseDimensionalObject(DATA_X_DIM_ID, DATA_X, List.of(dataElementOperand)))
            .build();

    DataHandler dataHandler = withNullDependencies();
    DataQueryParams params =
        dataHandler.getOperandDataQueryParams(stubParams, List.of(dataElementOperand), COC_ONLY);

    assertEquals(0, params.getDimensions().size());
    assertEquals(3, params.getFilters().size());
  }

  @Test
  void testOperandDataQueryParamsWithAttrOptionCombo() {
    DataElement dataElement = new DataElement("NameA");
    dataElement.setUid("uid1234567A");
    dataElement.setCode("CodeA");

    CategoryOptionCombo attributeOptionCombo = new CategoryOptionCombo();
    attributeOptionCombo.setName("NameAt");
    attributeOptionCombo.setUid("uid1234567C");
    attributeOptionCombo.setCode("CodeAt");

    DataElementOperand dataElementOperand =
        new DataElementOperand(dataElement, null, attributeOptionCombo);

    DataQueryParams stubParams =
        newBuilder()
            .addDimension(
                new BaseDimensionalObject(DATA_X_DIM_ID, DATA_X, List.of(dataElementOperand)))
            .build();

    DataHandler dataHandler = withNullDependencies();
    DataQueryParams params =
        dataHandler.getOperandDataQueryParams(stubParams, List.of(dataElementOperand), AOC_ONLY);

    assertEquals(2, params.getDimensions().size());
  }

  @Test
  void testOperandDataQueryParamsWithAttrOptionComboInFilter() {
    DataElement dataElement = new DataElement("NameA");
    dataElement.setUid("uid1234567A");
    dataElement.setCode("CodeA");

    CategoryOptionCombo attributeOptionCombo = new CategoryOptionCombo();
    attributeOptionCombo.setName("NameAt");
    attributeOptionCombo.setUid("uid1234567C");
    attributeOptionCombo.setCode("CodeAt");

    DataElementOperand dataElementOperand =
        new DataElementOperand(dataElement, null, attributeOptionCombo);

    DataQueryParams stubParams =
        newBuilder()
            .addFilter(
                new BaseDimensionalObject(DATA_X_DIM_ID, DATA_X, List.of(dataElementOperand)))
            .build();

    DataHandler dataHandler = withNullDependencies();
    DataQueryParams params =
        dataHandler.getOperandDataQueryParams(stubParams, List.of(dataElementOperand), AOC_ONLY);

    assertEquals(0, params.getDimensions().size());
    assertEquals(3, params.getFilters().size());
  }

  @Test
  void testCheckIndicatorPermutationLimitAllowsUpToMax() {
    assertDoesNotThrow(() -> DataHandler.checkIndicatorPermutationLimit(500, 2, 1000));
  }

  @Test
  void testCheckIndicatorPermutationLimitRejectsAboveMax() {
    IllegalQueryException ex =
        assertThrows(
            IllegalQueryException.class,
            () -> DataHandler.checkIndicatorPermutationLimit(501, 2, 1000));

    assertEquals(ErrorCode.E7153, ex.getErrorCode());
  }

  @Test
  void testCheckIndicatorPermutationLimitDisabledByZero() {
    assertDoesNotThrow(() -> DataHandler.checkIndicatorPermutationLimit(Long.MAX_VALUE, 5, 0));
  }

  @Test
  void testCheckIndicatorPermutationLimitDoesNotOverflow() {
    IllegalQueryException ex =
        assertThrows(
            IllegalQueryException.class,
            () -> DataHandler.checkIndicatorPermutationLimit(Long.MAX_VALUE / 2 + 1, 2, 1000));

    assertEquals(ErrorCode.E7153, ex.getErrorCode());
  }

  /**
   * Shape of the 2026-10-16 ASC incident: one indicator crossed with many category dimensions
   * multiplies into tens of millions of permutations. The query must be rejected before any
   * permutation is built.
   */
  @Test
  void testAddIndicatorValuesRejectsTooManyPermutations() {
    IndicatorType indicatorType = new IndicatorType("Number", 1, true);
    Indicator indicator = new Indicator();
    indicator.setUid("indicatorUA");
    indicator.setIndicatorType(indicatorType);
    indicator.setNumerator("1");
    indicator.setDenominator("1");

    DataQueryParams.Builder builder =
        newBuilder()
            .addDimension(new BaseDimensionalObject(DATA_X_DIM_ID, DATA_X, List.of(indicator)));

    // 10 dimensions of 6 items: 6^10 = 60,466,176 permutations
    for (int i = 0; i < 10; i++) {
      List<DimensionalItemObject> items = new ArrayList<>();
      for (int j = 0; j < 6; j++) {
        CategoryOption option = new CategoryOption("Option" + i + j);
        option.setUid("option" + i + "x" + j + "xxx");
        items.add(option);
      }
      builder.addDimension(new BaseDimensionalObject("category" + i + "xx", CATEGORY, items));
    }

    ExpressionResolvers resolvers = mock(ExpressionResolvers.class);
    when(resolvers.getExpressionResolvers()).thenReturn(List.of());
    SystemSettingsProvider settingsProvider = mock(SystemSettingsProvider.class);
    when(settingsProvider.getCurrentSettings())
        .thenReturn(SystemSettings.of(Map.of("keyAnalyticsMaxIndicatorPermutations", "1000000")));

    DataHandler dataHandler =
        new DataHandler(null, null, resolvers, null, null, settingsProvider, null, null, null);

    IllegalQueryException ex =
        assertThrows(
            IllegalQueryException.class,
            () -> dataHandler.addIndicatorValues(builder.build(), new ListGrid()));

    assertEquals(ErrorCode.E7153, ex.getErrorCode());
  }

  @Test
  void testMaxIndicatorPermutationsDefault() {
    assertEquals(1_000_000, SystemSettings.of(Map.of()).getAnalyticsMaxIndicatorPermutations());
    // Settable through /api/systemSettings, which only accepts keys with a default
    assertTrue(SystemSettings.keysWithDefaults().contains("keyAnalyticsMaxIndicatorPermutations"));
  }

  private DataHandler withNullDependencies() {
    return new DataHandler(null, null, null, null, null, null, null, null, null);
  }
}
