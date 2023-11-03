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
package org.hisp.dhis.analytics.data;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.hisp.dhis.analytics.util.AnalyticsSqlUtils.quote;
import static org.hisp.dhis.analytics.util.PeriodOffsetUtils.shiftPeriod;
import static org.hisp.dhis.common.DimensionalObject.DATA_X_DIM_ID;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.period.Period;

/**
 * Utility methods for generating a subExpression query containing periodOffsets.
 *
 * <p>These methods help in the translation between report periods (the periods reported to the
 * user) and data periods (the database periods in which data is fetched). For example a
 * periodOffset of -1 for a reporting period of '202310' (Oct 2023) means that the data comes from
 * data period '202309' (Sep 2023).
 *
 * @author Jim Grace
 */
public class SubexpressionPeriodOffsetUtils {
  private SubexpressionPeriodOffsetUtils() {
    throw new UnsupportedOperationException("util");
  }

  static final String SHIFT = "shift";

  static final String DELTA = quote("delta");

  static final String REPORTPERIOD = quote("reportperiod");

  private static final String DATAPERIOD = quote("dataperiod");

  /**
   * For {@link DataQueryParams} containing a subexpression with periodOffsets, joins an inline
   * value table that maps the reporting periods to data periods, for each distinct periodOffset
   * value.
   *
   * <p>For example, for periods '202309' and '202310' and periods offsets -1 and 0, this would
   * generate:
   *
   * <pre>
   *       join (values(-1,'202309','202308'),(-1,'202310','202309'),
   *                   (0,'202309','202309'),(0,'202310','202310'))
   *       as shift (delta, reportperiod, dataperiod) on dataperiod = "monthly"
   * </pre>
   *
   * @param params parameters with reporting periods
   * @return a join clause to an inline table to map reporting periods to data periods
   */
  protected static String joinPeriodOffsetValues(DataQueryParams params) {
    List<Period> reportPeriods = getReportPeriods(params);
    List<Integer> periodOffsets = getPeriodOffsets(params);

    StringBuilder sb = new StringBuilder(" join (values");

    for (Integer delta : periodOffsets) {
      for (Period reportPeriod : reportPeriods) {
        Period dataPeriod = shiftPeriod(reportPeriod, delta);
        sb.append(
            format("(%s,'%s','%s'),", delta, reportPeriod.getIsoDate(), dataPeriod.getIsoDate()));
      }
    }
    sb.setLength(sb.length() - 1); // Remove final ","

    sb.append(
        format(
            ") as %s (%s, %s, %s) on %s = %s",
            SHIFT,
            DELTA,
            REPORTPERIOD,
            DATAPERIOD,
            DATAPERIOD,
            quote(params.getPeriodType().toLowerCase())));

    return sb.toString();
  }

  /**
   * For {@link DataQueryParams} containing a subexpression with periodOffsets, returns the
   * parameters with data periods and without data dimension items.
   *
   * @param params parameters with reporting periods
   * @return parameters with data periods and without data dimension items
   */
  protected static DataQueryParams getParamsWithOffsetPeriodsWithoutData(DataQueryParams params) {
    return DataQueryParams.newBuilder(getParamsWithOffsetPeriods(params))
        .removeDimension(DATA_X_DIM_ID)
        .build();
  }

  /**
   * For {@link DataQueryParams} containing a subexpression with periodOffsets, replaces the
   * reporting periods in the parameters (the periods to return to the user) with data periods (the
   * periods to fetch data from the database, according to the periodOffsets).
   *
   * @param params parameters with reporting periods
   * @return parameters with data periods
   */
  protected static DataQueryParams getParamsWithOffsetPeriods(DataQueryParams params) {
    List<Period> reportPeriods = getReportPeriods(params);
    List<Integer> periodOffsets = getPeriodOffsets(params);

    Set<Period> shiftedPeriods = new HashSet<>();
    for (Period reportPeriod : reportPeriods) {
      for (Integer periodOffset : periodOffsets) {
        shiftedPeriods.add(shiftPeriod(reportPeriod, periodOffset));
      }
    }

    List<DimensionalItemObject> dataPeriods =
        shiftedPeriods.stream()
            .sorted() // Useful for testing, troubleshooting, etc.
            .map(DimensionalItemObject.class::cast)
            .collect(toList());

    DimensionalObject periodDimension =
        new BaseDimensionalObject(
            DimensionalObject.PERIOD_DIM_ID, DimensionType.PERIOD, dataPeriods);

    return DataQueryParams.newBuilder(params).replaceDimension(periodDimension).build();
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  /** Gets the report periods from params as a list of Periods. */
  private static List<Period> getReportPeriods(DataQueryParams params) {
    return params.getPeriods().stream().map(Period.class::cast).collect(toList());
  }

  /** Gets the sorted, distinct period offsets from items within the subexpression. */
  private static List<Integer> getPeriodOffsets(DataQueryParams params) {
    if (!params.hasSubexpressions()) {
      params.getSubexpression();
    }
    if (params.getSubexpression().getItems().isEmpty()) {
      params.getSubexpression();
    }
    return params.getSubexpression().getItems().stream()
        .map(DimensionalItemObject::getPeriodOffset)
        .distinct()
        .sorted()
        .collect(toList());
  }
}
