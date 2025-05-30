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
package org.hisp.dhis.analytics;

import java.util.EnumSet;

/**
 * Enum which represents the aggregation type.
 *
 * @author Lars Helge Overland
 */
public enum AggregationType {
  SUM("sum", true),
  AVERAGE("avg", true),
  AVERAGE_SUM_ORG_UNIT("avg_sum_org_unit", true),
  LAST("last", true), // Sum org unit
  LAST_AVERAGE_ORG_UNIT("last_avg_org_unit", true),
  LAST_LAST_ORG_UNIT("last_last_org_unit", true),
  LAST_IN_PERIOD("last_analytics_period", true), // Sum org unit
  LAST_IN_PERIOD_AVERAGE_ORG_UNIT("last_analytics_period_avg_org_unit", true),
  FIRST("first", true),
  FIRST_AVERAGE_ORG_UNIT("first_avg_org_unit", true),
  FIRST_FIRST_ORG_UNIT("first_first_org_unit", true),
  COUNT("count", true),
  STDDEV("stddev", true),
  VARIANCE("variance", true),
  MIN("min", true),
  MAX("max", true),
  MIN_SUM_ORG_UNIT("min_sum_org_unit", true),
  MAX_SUM_ORG_UNIT("max_sum_org_unit", true),
  NONE("none", true), // Aggregatable for text only
  CUSTOM("custom", false),
  DEFAULT("default", false);

  private static final EnumSet<AggregationType> LAST_TYPES =
      EnumSet.of(LAST, LAST_AVERAGE_ORG_UNIT, LAST_LAST_ORG_UNIT);

  private static final EnumSet<AggregationType> FIRST_TYPES =
      EnumSet.of(FIRST, FIRST_AVERAGE_ORG_UNIT, FIRST_FIRST_ORG_UNIT);

  private final String value;

  private boolean aggregatable;

  AggregationType(String value) {
    this.value = value;
  }

  AggregationType(String value, boolean aggregateable) {
    this.value = value;
    this.aggregatable = aggregateable;
  }

  public String getValue() {
    return value;
  }

  public boolean isAggregatable() {
    return aggregatable;
  }

  public boolean isLast() {
    return LAST_TYPES.contains(this);
  }

  public boolean isFirst() {
    return FIRST_TYPES.contains(this);
  }

  public static AggregationType fromValue(String value) {
    for (AggregationType type : AggregationType.values()) {
      if (type.value.equalsIgnoreCase(value)) {
        return type;
      }
    }

    return null;
  }
}
