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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.NoArgsConstructor;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParamItem;
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
class PeriodBucketColumn {

  /**
   * Returns the bucket column the dimension's period items share, or empty when they share none:
   * when the dimension carries no items, when its items are of different period types, or when an
   * item is a date range or a comparison rather than a period.
   *
   * @param dimension the dimension identifier.
   * @return the bucket column name, lowercased, for example {@code yearly}.
   */
  static Optional<String> of(DimensionIdentifier<DimensionParam> dimension) {
    List<DimensionParamItem> items = dimension.getDimension().getItems();
    if (items.isEmpty()) {
      return Optional.empty();
    }

    Set<String> buckets = new LinkedHashSet<>();

    for (DimensionParamItem item : items) {
      if (item.getOperator().getQueryOperator() != QueryOperator.EQ) {
        return Optional.empty();
      }
      for (String value : item.getValues()) {
        Optional<String> valueBucket = bucketOf(value);
        if (valueBucket.isEmpty()) {
          return Optional.empty();
        }
        buckets.add(valueBucket.get());
        if (buckets.size() > 1) {
          return Optional.empty();
        }
      }
    }

    return buckets.stream().findFirst();
  }

  /**
   * Returns the bucket column of the period type the given value belongs to. A relative period name
   * carries its period type on the enum, an ISO period carries it on the period itself, and any
   * other value has no single period type.
   */
  private static Optional<String> bucketOf(String value) {
    if (RelativePeriodEnum.contains(value)) {
      return Optional.of(RelativePeriodEnum.valueOf(value).value().getName().toLowerCase());
    }

    return Optional.ofNullable(PeriodDimension.of(value))
        .map(PeriodDimension::getPeriodType)
        .map(periodType -> periodType.getName().toLowerCase());
  }
}
