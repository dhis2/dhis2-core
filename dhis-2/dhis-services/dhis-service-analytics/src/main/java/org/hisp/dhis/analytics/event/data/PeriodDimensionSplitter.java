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
package org.hisp.dhis.analytics.event.data;

import static org.hisp.dhis.common.DimensionConstants.PERIOD_DIM_ID;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.period.PeriodDimension;

public final class PeriodDimensionSplitter {

  private static final String DEFAULT_GROUP_KEY = "__default__";

  private PeriodDimensionSplitter() {}

  /** Splits a period dimension by date field. Returns original if only one group. */
  public static List<DimensionalObject> splitPeriodDimension(DimensionalObject dimension) {
    if (!DimensionType.PERIOD.equals(dimension.getDimensionType())
        || dimension.getItems() == null
        || dimension.getItems().isEmpty()) {
      return List.of(dimension);
    }

    Map<String, List<DimensionalItemObject>> groups = groupByDateField(dimension.getItems());

    if (groups.size() <= 1) {
      return List.of(dimension);
    }

    List<DimensionalObject> result = new ArrayList<>();
    for (Map.Entry<String, List<DimensionalItemObject>> entry : groups.entrySet()) {
      String key = entry.getKey();
      String dimensionName;
      if (DEFAULT_GROUP_KEY.equals(key)) {
        dimensionName = dimension.getDimensionName();
        if (dimensionName == null
            || dimensionName.isBlank()
            || PERIOD_DIM_ID.equals(dimensionName)) {
          PeriodDimension firstPeriod = (PeriodDimension) entry.getValue().get(0);
          dimensionName =
              firstPeriod.getPeriodType().getPeriodTypeEnum().getName().toLowerCase(Locale.ROOT);
        }
      } else {
        dimensionName = key;
      }

      BaseDimensionalObject synthetic =
          new BaseDimensionalObject(
              PERIOD_DIM_ID,
              DimensionType.PERIOD,
              dimensionName,
              dimension.getDimensionDisplayName(),
              entry.getValue(),
              dimension.getDimensionItemKeywords());
      synthetic.setGroupUUID(((BaseDimensionalObject) dimension).getGroupUUID());
      result.add(synthetic);
    }

    return result;
  }

  /** Replaces period dimensions in the list with their splits; non-period pass through. */
  public static List<DimensionalObject> expandPeriodDimensions(List<DimensionalObject> dimensions) {
    List<DimensionalObject> result = new ArrayList<>();
    for (DimensionalObject dim : dimensions) {
      result.addAll(splitPeriodDimension(dim));
    }
    return result;
  }

  /** True if any PeriodDimension item has a null date field (i.e. uses the default date column). */
  public static boolean hasDefaultPeriodGroup(DimensionalObject dimension) {
    if (!DimensionType.PERIOD.equals(dimension.getDimensionType())) {
      return false;
    }
    return dimension.getItems().stream()
        .filter(PeriodDimension.class::isInstance)
        .map(PeriodDimension.class::cast)
        .anyMatch(pd -> isDefaultDateField(pd.getDateField()));
  }

  /**
   * Converts a date field name to its lowercase key form, e.g. "SCHEDULED_DATE" to "scheduleddate".
   */
  public static String toDateFieldKey(String dateField) {
    return dateField.replace("_", "").toLowerCase(Locale.ROOT);
  }

  private static Map<String, List<DimensionalItemObject>> groupByDateField(
      List<DimensionalItemObject> items) {
    Map<String, List<DimensionalItemObject>> groups = new LinkedHashMap<>();
    for (DimensionalItemObject item : items) {
      String key = DEFAULT_GROUP_KEY;
      if (item instanceof PeriodDimension pd && !isDefaultDateField(pd.getDateField())) {
        key = toDateFieldKey(pd.getDateField());
      }
      groups.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
    }
    return groups;
  }

  private static boolean isDefaultDateField(String dateField) {
    return dateField == null;
  }
}
