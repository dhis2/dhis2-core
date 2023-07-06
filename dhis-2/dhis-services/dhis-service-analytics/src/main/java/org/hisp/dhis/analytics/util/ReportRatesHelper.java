/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.analytics.util;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.util.List;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.PeriodTypeEnum;

/**
 * Just a helper class responsible for providing methods that hides specific Report Rates logic and
 * calculation.
 *
 * @author maikel arabori
 */
public class ReportRatesHelper {
  private ReportRatesHelper() {}

  /**
   * Use number of days for daily data sets as target, as query periods might
   * often span/contain different numbers of days.
   *
   * @param periodIndex the index of the period in the "dataRow".
   * @param timeUnits the time unit size found in the current DataQueryParams.
   *        See {@link #DataHandler.getTimeUnits(DataQueryParams)}.
   * @param dataRow the current dataRow, based on the key map built by
   *        {@link #DataHandler.getAggregatedCompletenessTargetMap(DataQueryParams)).
   * @param target the current value of the respective key ("dataRow"). See
   *        {@link #DataHandler.getAggregatedCompletenessTargetMap(DataQueryParams).
   * @param queryPt the filter period in the current "dataRow". See
   *        {@link PeriodType#getPeriodTypeFromIsoString}.
   * @param dataSetPt the dataset period.
   * @param filterPeriods the filter "pe" in the params.
   *
   * @return the calculate target
   */
  public static Double getCalculatedTarget(
      final int periodIndex,
      final int timeUnits,
      final List<String> dataRow,
      final Double target,
      final PeriodType queryPt,
      final PeriodType dataSetPt,
      final List<DimensionalItemObject> filterPeriods) {
    if (PeriodTypeEnum.DAILY == dataSetPt.getPeriodTypeEnum()) {
      boolean hasPeriodInDimension = periodIndex != -1;

      // If we enter here, it means there is a "pe" in the dimension
      // parameter.
      if (hasPeriodInDimension) {
        return getTargetFromDimension(periodIndex, timeUnits, dataRow, target);
      } else {
        // If we reach here, it means that we should have a "pe"
        // dimension as filter parameter.
        return getTargetFromFilter(timeUnits, target, filterPeriods);
      }
    } else {
      return consolidateTarget(timeUnits, target, queryPt.getPeriodSpan(dataSetPt));
    }
  }

  private static Double getTargetFromDimension(
      final int periodIndex, final int timeUnits, final List<String> dataRow, final Double target) {
    final Period period = PeriodType.getPeriodFromIsoString(dataRow.get(periodIndex));

    return consolidateTarget(timeUnits, target, period.getDaysInPeriod());
  }

  private static Double getTargetFromFilter(
      final int timeUnits, final Double target, final List<DimensionalItemObject> filterPeriods) {
    if (isNotEmpty(filterPeriods)) {
      int totalOfDaysInPeriod = 0;

      for (final DimensionalItemObject itemObject : filterPeriods) {
        final Period period = (Period) itemObject;
        totalOfDaysInPeriod += period.getDaysInPeriod();
      }

      return consolidateTarget(timeUnits, target, totalOfDaysInPeriod);
    }

    return target;
  }

  private static Double consolidateTarget(
      final int timeUnits, final Double target, final int daysInPeriod) {
    return target * daysInPeriod * timeUnits;
  }
}
