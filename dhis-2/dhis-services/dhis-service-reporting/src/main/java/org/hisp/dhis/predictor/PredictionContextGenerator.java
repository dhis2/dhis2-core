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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.FoundDimensionItemValue;
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.common.MapMap;
import org.hisp.dhis.period.Period;

/**
 * Generator of prediction contexts.
 *
 * @author Jim Grace
 */
public class PredictionContextGenerator {
  private PredictionContextGenerator() {
    throw new UnsupportedOperationException("util");
  }

  /**
   * Generates prediction contexts. Each prediction context contains all the values required to
   * evaluate a predictor to (possibly) generate one prediction.
   *
   * <p>All the data used to generate contexts has the same organisation unit.
   *
   * @param outputPeriods output periods (predict within each period)
   * @param values input prediction values (all with the same orgUnit)
   * @param defaultCategoryOptionCombo system default cat option combo
   * @param preDis prediction disaggregator
   * @return contexts for prediction evaluation
   */
  public static List<PredictionContext> getContexts(
      List<Period> outputPeriods,
      List<FoundDimensionItemValue> values,
      CategoryOptionCombo defaultCategoryOptionCombo,
      PredictionDisaggregator preDis) {
    List<PredictionContext> contexts = new ArrayList<>();

    ListMap<CategoryOptionCombo, FoundDimensionItemValue> aocMap =
        getAocMap(values, defaultCategoryOptionCombo);

    for (Map.Entry<CategoryOptionCombo, List<FoundDimensionItemValue>> e : aocMap.entrySet()) {
      CategoryOptionCombo aoc = e.getKey();
      Map<CategoryOptionCombo, MapMap<Period, DimensionalItemObject, Object>> cocMap =
          preDis.getPeriodValueMaps(e.getValue());

      // Periods are in the outer loop, so earlier predictions can be used by later predictions
      for (Period outputPeriod : outputPeriods) {
        for (Map.Entry<CategoryOptionCombo, MapMap<Period, DimensionalItemObject, Object>> e2 :
            cocMap.entrySet()) {
          contexts.add(new PredictionContext(e2.getKey(), aoc, outputPeriod, e2.getValue()));
        }
      }
    }

    return contexts;
  }

  // -------------------------------------------------------------------------
  // Supportive Methods
  // -------------------------------------------------------------------------

  /**
   * Generates a map of values by attribute option combo.
   *
   * <p>If there is no data, then generate predictions for the default attribute option combo. (If a
   * predictor has missing value strategy NEVER_SKIP, it could generate a value even if there is no
   * input data.)
   */
  private static ListMap<CategoryOptionCombo, FoundDimensionItemValue> getAocMap(
      List<FoundDimensionItemValue> values, CategoryOptionCombo defaultCategoryOptionCombo) {
    ListMap<CategoryOptionCombo, FoundDimensionItemValue> map = getAocDataMap(values);

    if (map.isEmpty()) {
      map.put(defaultCategoryOptionCombo, new ArrayList<>());
    }

    addNonAocData(map, values);

    return map;
  }

  /**
   * Generates a map from all the data that is stored by attribute option combination.
   *
   * <p>The values are kept (rather than just a map of items to values) because there may be
   * multiple values for the same period and item with different category option combos. These are
   * filtered and/or summed when generating the prediction contexts.
   */
  private static ListMap<CategoryOptionCombo, FoundDimensionItemValue> getAocDataMap(
      List<FoundDimensionItemValue> values) {
    ListMap<CategoryOptionCombo, FoundDimensionItemValue> map = new ListMap<>();

    for (FoundDimensionItemValue value : values) {
      if (value.getAttributeOptionCombo() != null) {
        map.putValue(value.getAttributeOptionCombo(), value);
      }
    }

    return map;
  }

  /**
   * Adds non-attribute option combo data to data for each AOC.
   *
   * <p>Data that is not stored in the database with attribute option combos is used in predictions
   * for each attribute option combo.
   */
  private static void addNonAocData(
      ListMap<CategoryOptionCombo, FoundDimensionItemValue> map,
      List<FoundDimensionItemValue> values) {
    for (FoundDimensionItemValue value : values) {
      if (value.getAttributeOptionCombo() == null) {
        for (CategoryOptionCombo aoc : map.keySet()) {
          map.putValue(aoc, value);
        }
      }
    }
  }
}
