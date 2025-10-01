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
package org.hisp.dhis.analytics.event.data;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.hisp.dhis.analytics.TimeField;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodDimension;
import org.hisp.dhis.period.RelativePeriodEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EventPeriodUtilsTest {

  private static EventQueryParams paramsWithPeriods(PeriodDimension... periods) {
    return new EventQueryParams.Builder()
        // period type label isn't used by EventPeriodUtils; any value is fine
        .withPeriods(List.of(periods), "Monthly")
        .build();
  }

  @Test
  @DisplayName("hasAllDefaultPeriod returns true when no period dimension")
  void hasAllDefaultPeriod_noPeriodDimension_returnsTrue() {
    EventQueryParams params = new EventQueryParams.Builder().build();
    assertTrue(EventPeriodUtils.hasAllDefaultPeriod(params));
  }

  @Test
  @DisplayName("hasAllDefaultPeriod returns true when all items have OCCURRED_DATE field")
  void hasAllDefaultPeriod_allOccurredDate_returnsTrue() {
    PeriodDimension p1 = new PeriodDimension(RelativePeriodEnum.LAST_MONTH);
    p1.setDateField(TimeField.OCCURRED_DATE.name());
    PeriodDimension p2 = new PeriodDimension(RelativePeriodEnum.THIS_MONTH);
    p2.setDateField(TimeField.OCCURRED_DATE.name());

    EventQueryParams params = paramsWithPeriods(p1, p2);

    assertTrue(EventPeriodUtils.hasAllDefaultPeriod(params));
  }

  @Test
  @DisplayName(
      "hasAllDefaultPeriod returns false when any item has non-default, non-OCCURRED_DATE field")
  void hasAllDefaultPeriod_nonDefaultNonOccurredDate_returnsFalse() {
    PeriodDimension p1 = new PeriodDimension(RelativePeriodEnum.LAST_MONTH);
    p1.setDateField(TimeField.ENROLLMENT_DATE.name());
    PeriodDimension p2 = new PeriodDimension(RelativePeriodEnum.THIS_MONTH);
    p2.setDateField(TimeField.COMPLETED_DATE.name());

    EventQueryParams params = paramsWithPeriods(p1, p2);

    assertFalse(EventPeriodUtils.hasAllDefaultPeriod(params));
  }

  @Test
  @DisplayName("hasAllDefaultPeriod returns true when a default period (no dateField) is present")
  void hasAllDefaultPeriod_defaultPeriod_returnsTrue() {
    PeriodDimension defaultPeriod =
        new PeriodDimension(RelativePeriodEnum.LAST_MONTH); // dateField is null by default
    EventQueryParams params = paramsWithPeriods(defaultPeriod);

    assertTrue(EventPeriodUtils.hasAllDefaultPeriod(params));
  }

  @Test
  @DisplayName("hasDefaultPeriod returns true when any item is default (no dateField)")
  void hasDefaultPeriod_anyDefault_returnsTrue() {
    PeriodDimension nonDefault = new PeriodDimension(RelativePeriodEnum.THIS_MONTH);
    nonDefault.setDateField(TimeField.ENROLLMENT_DATE.name());
    PeriodDimension def = new PeriodDimension(RelativePeriodEnum.LAST_MONTH); // default (dateField null)

    EventQueryParams params = paramsWithPeriods(nonDefault, def);

    assertTrue(EventPeriodUtils.hasDefaultPeriod(params));
  }

  @Test
  @DisplayName("hasDefaultPeriod returns false when no periods present")
  void hasDefaultPeriod_noPeriods_returnsFalse() {
    EventQueryParams params = new EventQueryParams.Builder().build();
    assertFalse(EventPeriodUtils.hasDefaultPeriod(params));
  }

  @Test
  @DisplayName("hasDefaultPeriod returns false when all items are non-default")
  void hasDefaultPeriod_allNonDefault_returnsFalse() {
    PeriodDimension p1 = new PeriodDimension(RelativePeriodEnum.LAST_MONTH);
    p1.setDateField(TimeField.LAST_UPDATED.name());
    PeriodDimension p2 = new PeriodDimension(RelativePeriodEnum.THIS_MONTH);
    p2.setDateField(TimeField.ENROLLMENT_DATE.name());

    EventQueryParams params = paramsWithPeriods(p1, p2);

    assertFalse(EventPeriodUtils.hasDefaultPeriod(params));
  }

  @Test
  @DisplayName("hasPeriodDimension returns true when period dimension is present")
  void hasPeriodDimension_present_returnsTrue() {
    PeriodDimension p = new PeriodDimension(RelativePeriodEnum.THIS_MONTH);
    EventQueryParams params = paramsWithPeriods(p);
    assertTrue(EventPeriodUtils.hasPeriodDimension(params));
  }

  @Test
  @DisplayName("hasPeriodDimension returns false when period dimension is absent")
  void hasPeriodDimension_absent_returnsFalse() {
    EventQueryParams params = new EventQueryParams.Builder().build();
    assertFalse(EventPeriodUtils.hasPeriodDimension(params));
  }
}
