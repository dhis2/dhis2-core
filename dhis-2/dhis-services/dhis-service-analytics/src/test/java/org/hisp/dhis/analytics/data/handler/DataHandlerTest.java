/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.analytics.data.handler;

import static org.hisp.dhis.analytics.DataQueryParams.newBuilder;
import static org.hisp.dhis.common.DimensionType.CATEGORY;
import static org.hisp.dhis.common.DimensionType.CATEGORY_OPTION_COMBO;
import static org.hisp.dhis.common.DimensionType.DATA_X;
import static org.hisp.dhis.common.DimensionalObject.CATEGORYOPTIONCOMBO_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.DATA_X_DIM_ID;
import static org.hisp.dhis.setting.SettingKey.ANALYTICS_MAX_INDICATOR_PERMUTATIONS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.resolver.ExpressionResolvers;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.grid.ListGrid;
import org.junit.jupiter.api.Test;

class DataHandlerTest {
  @Test
  void testCheckIndicatorPermutationLimitAllowsUpToMax() {
    assertDoesNotThrow(() -> DataHandler.checkIndicatorPermutationLimit(500, 2, 1000, () -> ""));
  }

  @Test
  void testCheckIndicatorPermutationLimitRejectsAboveMax() {
    IllegalQueryException ex =
        assertThrows(
            IllegalQueryException.class,
            () -> DataHandler.checkIndicatorPermutationLimit(501, 2, 1000, () -> ""));

    assertEquals(ErrorCode.E7153, ex.getErrorCode());
  }

  @Test
  void testCheckIndicatorPermutationLimitDisabledByZero() {
    assertDoesNotThrow(
        () -> DataHandler.checkIndicatorPermutationLimit(Long.MAX_VALUE, 5, 0, () -> ""));
  }

  @Test
  void testCheckIndicatorPermutationLimitDoesNotOverflow() {
    IllegalQueryException ex =
        assertThrows(
            IllegalQueryException.class,
            () ->
                DataHandler.checkIndicatorPermutationLimit(
                    Long.MAX_VALUE / 2 + 1, 2, 1000, () -> ""));

    assertEquals(ErrorCode.E7153, ex.getErrorCode());
  }

  /**
   * Shape of a production incident: one indicator crossed with many category dimensions multiplies
   * into tens of millions of permutations. The query must be rejected before any permutation is
   * built.
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
    SystemSettingManager systemSettingManager = mock(SystemSettingManager.class);
    when(systemSettingManager.getIntSetting(ANALYTICS_MAX_INDICATOR_PERMUTATIONS))
        .thenReturn(1_000_000);

    DataHandler dataHandler =
        new DataHandler(null, null, resolvers, null, null, systemSettingManager, null, null, null);

    DataQueryParams params = builder.build();
    ListGrid grid = new ListGrid();

    IllegalQueryException ex =
        assertThrows(
            IllegalQueryException.class, () -> dataHandler.addIndicatorValues(params, grid));

    assertEquals(ErrorCode.E7153, ex.getErrorCode());
  }

  @Test
  void testDescribeDimensionItemCountsSkipsDataAndCategoryOptionComboDimensions() {
    Indicator indicator = new Indicator();
    indicator.setUid("indicatorUA");
    CategoryOption optionA = new CategoryOption("OptionA");
    optionA.setUid("optionUidAA");
    CategoryOption optionB = new CategoryOption("OptionB");
    optionB.setUid("optionUidBB");

    DataQueryParams params =
        newBuilder()
            .addDimension(new BaseDimensionalObject(DATA_X_DIM_ID, DATA_X, List.of(indicator)))
            .addDimension(
                new BaseDimensionalObject(
                    CATEGORYOPTIONCOMBO_DIM_ID, CATEGORY_OPTION_COMBO, List.of()))
            .addDimension(new BaseDimensionalObject("categoryAA", CATEGORY, List.of(optionA)))
            .addDimension(
                new BaseDimensionalObject("categoryBB", CATEGORY, List.of(optionA, optionB)))
            .build();

    assertEquals("categoryAA=1 categoryBB=2", DataHandler.describeDimensionItemCounts(params));
  }

  @Test
  void testMaxIndicatorPermutationsDefault() {
    assertEquals(1_000_000, ANALYTICS_MAX_INDICATOR_PERMUTATIONS.getDefaultValue());
  }
}
