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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.analytics.TimeField;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.period.PeriodDimension;
import org.hisp.dhis.test.TestBase;
import org.junit.jupiter.api.Test;

// Tests for splitting period dimensions with mixed date fields into per-date-field groups.
class PeriodDimensionSplitterTest extends TestBase {

  @Test
  void singleDateFieldReturnsOriginalDimension() {
    List<PeriodDimension> items = createPeriodDimensions("202301", "202302");
    items.forEach(pd -> pd.setDateField("SCHEDULED_DATE"));

    BaseDimensionalObject dimension =
        new BaseDimensionalObject(PERIOD_DIM_ID, DimensionType.PERIOD, "monthly", "Period", items);

    List<DimensionalObject> result = PeriodDimensionSplitter.splitPeriodDimension(dimension);

    assertEquals(1, result.size());
    assertSame(dimension, result.get(0));
  }

  @Test
  void mixedNonDefaultDateFieldsSplitsIntoGroups() {
    List<PeriodDimension> items = createPeriodDimensions("202301", "202302", "202303");
    items.get(0).setDateField("SCHEDULED_DATE");
    items.get(1).setDateField("SCHEDULED_DATE");
    items.get(2).setDateField("LAST_UPDATED");

    BaseDimensionalObject dimension =
        new BaseDimensionalObject(PERIOD_DIM_ID, DimensionType.PERIOD, "monthly", "Period", items);

    List<DimensionalObject> result = PeriodDimensionSplitter.splitPeriodDimension(dimension);

    assertEquals(2, result.size());

    DimensionalObject scheduledGroup =
        result.stream()
            .filter(d -> "scheduleddate".equals(d.getDimensionName()))
            .findFirst()
            .orElseThrow();
    assertEquals(2, scheduledGroup.getItems().size());
    assertEquals(PERIOD_DIM_ID, scheduledGroup.getDimension());
    assertEquals(DimensionType.PERIOD, scheduledGroup.getDimensionType());

    DimensionalObject lastUpdatedGroup =
        result.stream()
            .filter(d -> "lastupdated".equals(d.getDimensionName()))
            .findFirst()
            .orElseThrow();
    assertEquals(1, lastUpdatedGroup.getItems().size());
    assertEquals(PERIOD_DIM_ID, lastUpdatedGroup.getDimension());
  }

  @Test
  void defaultAndNonDefaultSplitsCorrectly() {
    List<PeriodDimension> items = createPeriodDimensions("202301", "202302");
    // null dateField = default group
    items.get(1).setDateField("LAST_UPDATED");

    BaseDimensionalObject dimension =
        new BaseDimensionalObject(PERIOD_DIM_ID, DimensionType.PERIOD, "monthly", "Period", items);

    List<DimensionalObject> result = PeriodDimensionSplitter.splitPeriodDimension(dimension);

    assertEquals(2, result.size());

    DimensionalObject defaultGroup =
        result.stream()
            .filter(d -> "monthly".equals(d.getDimensionName()))
            .findFirst()
            .orElseThrow();
    assertEquals(1, defaultGroup.getItems().size());

    DimensionalObject lastUpdatedGroup =
        result.stream()
            .filter(d -> "lastupdated".equals(d.getDimensionName()))
            .findFirst()
            .orElseThrow();
    assertEquals(1, lastUpdatedGroup.getItems().size());
  }

  @Test
  void expandPreservesNonPeriodDimensions() {
    BaseDimensionalObject orgUnit =
        new BaseDimensionalObject("ou", DimensionType.ORGANISATION_UNIT, List.of());

    List<PeriodDimension> items = createPeriodDimensions("202301", "202302");
    items.get(0).setDateField("SCHEDULED_DATE");
    items.get(1).setDateField("LAST_UPDATED");

    BaseDimensionalObject period =
        new BaseDimensionalObject(PERIOD_DIM_ID, DimensionType.PERIOD, "monthly", "Period", items);

    BaseDimensionalObject dataElement =
        new BaseDimensionalObject("dx", DimensionType.DATA_X, List.of());

    List<DimensionalObject> result =
        PeriodDimensionSplitter.expandPeriodDimensions(List.of(orgUnit, period, dataElement));

    assertEquals(4, result.size());
    assertSame(orgUnit, result.get(0));
    assertSame(dataElement, result.get(3));
    // Middle two are the split period dimensions
    assertEquals(
        2, result.stream().filter(d -> DimensionType.PERIOD.equals(d.getDimensionType())).count());
  }

  @Test
  void hasDefaultPeriodGroupDetectsNullDateField() {
    List<PeriodDimension> items = createPeriodDimensions("202301");
    // null dateField by default

    BaseDimensionalObject dimension =
        new BaseDimensionalObject(PERIOD_DIM_ID, DimensionType.PERIOD, items);

    assertTrue(PeriodDimensionSplitter.hasDefaultPeriodGroup(dimension));
  }

  @Test
  void hasDefaultPeriodGroupDetectsOccurredDate() {
    List<PeriodDimension> items = createPeriodDimensions("202301");
    items.get(0).setDateField(TimeField.OCCURRED_DATE.name());

    BaseDimensionalObject dimension =
        new BaseDimensionalObject(PERIOD_DIM_ID, DimensionType.PERIOD, items);

    assertTrue(PeriodDimensionSplitter.hasDefaultPeriodGroup(dimension));
  }

  @Test
  void hasDefaultPeriodGroupReturnsFalseForNonDefault() {
    List<PeriodDimension> items = createPeriodDimensions("202301");
    items.get(0).setDateField("SCHEDULED_DATE");

    BaseDimensionalObject dimension =
        new BaseDimensionalObject(PERIOD_DIM_ID, DimensionType.PERIOD, items);

    assertFalse(PeriodDimensionSplitter.hasDefaultPeriodGroup(dimension));
  }

  @Test
  void toDateFieldKeyNormalizesCorrectly() {
    assertEquals("scheduleddate", PeriodDimensionSplitter.toDateFieldKey("SCHEDULED_DATE"));
    assertEquals("lastupdated", PeriodDimensionSplitter.toDateFieldKey("LAST_UPDATED"));
  }

  @Test
  void nonPeriodDimensionReturnedAsIs() {
    BaseDimensionalObject orgUnit =
        new BaseDimensionalObject("ou", DimensionType.ORGANISATION_UNIT, List.of());

    List<DimensionalObject> result = PeriodDimensionSplitter.splitPeriodDimension(orgUnit);

    assertEquals(1, result.size());
    assertSame(orgUnit, result.get(0));
  }

  @Test
  void emptyItemsReturnedAsIs() {
    BaseDimensionalObject dimension =
        new BaseDimensionalObject(
            PERIOD_DIM_ID,
            DimensionType.PERIOD,
            "monthly",
            "Period",
            List.<DimensionalItemObject>of());

    List<DimensionalObject> result = PeriodDimensionSplitter.splitPeriodDimension(dimension);

    assertEquals(1, result.size());
    assertSame(dimension, result.get(0));
  }

  @Test
  void defaultGroupDerivesDimensionNameFromPeriodTypeWhenNull() {
    List<PeriodDimension> items = createPeriodDimensions("202301", "202302");
    // item 0: default (null dateField), item 1: SCHEDULED_DATE
    items.get(1).setDateField("SCHEDULED_DATE");

    // dimensionName is null (simulates pre-planner state)
    BaseDimensionalObject dimension =
        new BaseDimensionalObject(PERIOD_DIM_ID, DimensionType.PERIOD, null, "Period", items);

    List<DimensionalObject> result = PeriodDimensionSplitter.splitPeriodDimension(dimension);

    assertEquals(2, result.size());
    // Default group derives dimensionName from period type
    assertEquals("monthly", result.get(0).getDimensionName());
    // Non-default group uses date field key
    assertEquals("scheduleddate", result.get(1).getDimensionName());
  }
}
