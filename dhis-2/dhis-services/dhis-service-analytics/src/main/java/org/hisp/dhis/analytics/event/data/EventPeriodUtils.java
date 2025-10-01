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

import static org.hisp.dhis.analytics.TimeField.OCCURRED_DATE;
import static org.hisp.dhis.common.DimensionConstants.PERIOD_DIM_ID;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodDimension;

/** Utility class for checking period dimensions in event analytics queries. */
@UtilityClass
public class EventPeriodUtils {

  /**
   * Returns true if all period items have no date field set or have the date field set to
   * OCCURRED_DATE
   *
   * @param params the event query params
   * @return true if all period items have no date field set or have the date field set to
   *     OCCURRED_DATE
   */
  public static boolean hasAllDefaultPeriod(EventQueryParams params) {
    DimensionalObject period = getPeriodDimension(params);
    if (period == null) {
      return true;
    }
    List<DimensionalItemObject> items = period.getItems();
    for (DimensionalItemObject item : items) {
      PeriodDimension p = (PeriodDimension) item;
      // All periods must either have no dateField (default) or OCCURRED_DATE
      if (p.getDateField() != null && !p.getDateField().equals(OCCURRED_DATE.name())) {
        return false;
      }
    }

    return true;
  }

  public static boolean hasDefaultPeriod(EventQueryParams eventQueryParams) {
    return Optional.ofNullable(getPeriodDimension(eventQueryParams))
        .map(DimensionalObject::getItems)
        .orElse(List.of())
        .stream()
        .anyMatch(EventPeriodUtils::isDefaultPeriod);
  }

  public static boolean hasPeriodDimension(EventQueryParams eventQueryParams) {
    return Objects.nonNull(getPeriodDimension(eventQueryParams));
  }

  private static DimensionalObject getPeriodDimension(EventQueryParams eventQueryParams) {
    return eventQueryParams.getDimension(PERIOD_DIM_ID);
  }

  private static boolean isDefaultPeriod(DimensionalItemObject dimensionalItemObject) {
    return ((PeriodDimension) dimensionalItemObject).isDefault();
  }
}
