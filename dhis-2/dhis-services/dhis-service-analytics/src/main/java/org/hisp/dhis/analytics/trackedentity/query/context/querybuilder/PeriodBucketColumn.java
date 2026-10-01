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
package org.hisp.dhis.analytics.trackedentity.query.context.querybuilder;

import static lombok.AccessLevel.PRIVATE;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.NoArgsConstructor;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParamItem;
import org.hisp.dhis.analytics.trackedentity.query.PeriodStaticDimensionCondition;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.period.PeriodDimension;
import org.hisp.dhis.period.RelativePeriodEnum;

/**
 * Resolves the period a date dimension's values are bucketed into, named as the column of {@code
 * analytics_rs_dateperiodstructure} that holds it. Grouping a date on this bucket answers {@code
 * EVENT_DATE:THIS_YEAR} with {@code 2026} rather than with a row per distinct timestamp.
 *
 * <p>The tracked entity stack keeps period items as the request wrote them, so a value here is
 * either a relative period name or an ISO period, and both have to be read.
 */
@NoArgsConstructor(access = PRIVATE)
public class PeriodBucketColumn {

  /**
   * Returns the bucket column the dimension's period items share, or empty when they share none:
   * when the dimension carries no items, when its items are of different period types, or when an
   * item is a date range or a comparison rather than a period.
   *
   * @param dimension the dimension identifier.
   * @return the bucket column name, lowercased, for example {@code yearly}.
   */
  static Optional<String> of(DimensionIdentifier<DimensionParam> dimension) {
    return periods(dimension).stream()
        .findFirst()
        .map(period -> period.getPeriodType().getName().toLowerCase());
  }

  /**
   * Returns the periods the dimension's items resolve to, which are the values its bucket column
   * can take. A relative period contributes every period it spans. The list is empty when the items
   * select no single bucket, see {@link #of}.
   *
   * @param dimension the dimension identifier.
   * @return the distinct periods, in request order.
   */
  public static List<PeriodDimension> periods(DimensionIdentifier<DimensionParam> dimension) {
    Map<String, PeriodDimension> periods = new LinkedHashMap<>();

    for (DimensionParamItem item : dimension.getDimension().getItems()) {
      if (item.getOperator().getQueryOperator() != QueryOperator.EQ) {
        return List.of();
      }
      for (String value : item.getValues()) {
        List<PeriodDimension> valuePeriods = periodsOf(value);
        if (valuePeriods.isEmpty()) {
          return List.of();
        }
        valuePeriods.forEach(period -> periods.putIfAbsent(period.getIsoDate(), period));
      }
    }

    long periodTypes =
        periods.values().stream()
            .map(period -> period.getPeriodType().getName())
            .distinct()
            .count();
    if (periodTypes > 1) {
      return List.of();
    }

    return List.copyOf(periods.values());
  }

  /**
   * Returns the periods the given value stands for: the periods a relative period name spans, the
   * ISO period itself, or none for any other value.
   */
  private static List<PeriodDimension> periodsOf(String value) {
    if (RelativePeriodEnum.contains(value)) {
      return PeriodStaticDimensionCondition.relativePeriods(value);
    }

    PeriodDimension period = PeriodDimension.of(value);
    return period == null ? List.of() : List.of(period);
  }
}
