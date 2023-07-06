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
package org.hisp.dhis.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.hisp.dhis.analytics.AggregationType;

/**
 * {@see DimensionalItemObject} modifiers for an analytics query, resulting from a parsed indicator
 * expression data item.
 *
 * @author Jim Grace
 */
@Getter
@ToString
@EqualsAndHashCode
@Builder(toBuilder = true)
public class QueryModifiers {
  private static final QueryModifiers DEFAULT = QueryModifiers.builder().build();

  /** Overrides the default aggregation type of this object. */
  @JsonProperty private final AggregationType aggregationType;

  /**
   * Period offset: the offset can be applied within an indicator formula in order to "shift" the
   * query period by the offset value (e.g. Feb 2022 with offset -1 becomes Jan 2022). An offset
   * with value 0 means no offset.
   */
  @JsonProperty private final int periodOffset;

  /** The minimum date (start of any period) for querying this object. */
  @JsonProperty private final Date minDate;

  /** The maximum date (end of any period) for querying this object. */
  @JsonProperty private final Date maxDate;

  /** The sub-expression used to query the analytics value column. */
  @JsonProperty private final String subExpression;

  /** The value type of the sub-expression (can be different from the value type of the object). */
  @JsonProperty private final ValueType valueType;

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  /** Gets the query modifiers that matter for analytics grouping. */
  public QueryModifiers withQueryModsForAnalyticsGrouping() {
    return this.toBuilder().periodOffset(0).build();
  }

  /** Gets an Id for these query modifiers that matter for analytics grouping. */
  public String getQueryModsId() {
    QueryModifiers analyticsQueryMods = withQueryModsForAnalyticsGrouping();

    return analyticsQueryMods.isDefault()
        ? null
        : Integer.toHexString(analyticsQueryMods.hashCode());
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  /** Checks if these query mods have all default values. */
  private boolean isDefault() {
    return this.equals(DEFAULT);
  }
}
