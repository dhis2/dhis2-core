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

import java.util.Objects;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.hisp.dhis.util.ObjectUtils;

/**
 * Enum which represents the aggregation type for analytics requests.
 *
 * @author Lars Helge Overland
 */
@ToString
@EqualsAndHashCode
public class AnalyticsAggregationType {
  public static final AnalyticsAggregationType //
      SUM = of(AggregationType.SUM, AggregationType.SUM),
      AVERAGE = of(AggregationType.AVERAGE, AggregationType.AVERAGE),
      COUNT = of(AggregationType.COUNT, AggregationType.COUNT),
      FIRST = of(AggregationType.SUM, AggregationType.FIRST),
      LAST = of(AggregationType.SUM, AggregationType.LAST),
      LAST_IN_PERIOD = of(AggregationType.SUM, AggregationType.LAST_IN_PERIOD);

  /** General aggregation type. */
  @Getter private final AggregationType aggregationType;

  /** Aggregation type for the period dimension. */
  private final AggregationType periodAggregationType;

  /** Analytics data type. */
  @Getter private DataType dataType;

  /** Indicates whether to perform data disaggregation. */
  @Getter private boolean disaggregation;

  public static AnalyticsAggregationType of(
      AggregationType aggregationType, AggregationType periodAggregationType) {
    return new AnalyticsAggregationType(aggregationType, periodAggregationType);
  }

  /**
   * Constructor.
   *
   * @param aggregationType the {@link AggregationType}.
   * @param periodAggregationType the period {@link AggregationType}.
   */
  public AnalyticsAggregationType(
      AggregationType aggregationType, AggregationType periodAggregationType) {
    this.aggregationType = aggregationType;
    this.periodAggregationType = periodAggregationType;
    Objects.requireNonNull(this.aggregationType);
    Objects.requireNonNull(this.periodAggregationType);
  }

  /**
   * Constructor.
   *
   * @param aggregationType the {@link AggregationType}.
   * @param periodAggregationType the period {@link AggregationType}.
   * @param dataType the {@link DataType}.
   * @param disaggregation indicates whether disaggregation is involved.
   */
  public AnalyticsAggregationType(
      AggregationType aggregationType,
      AggregationType periodAggregationType,
      DataType dataType,
      boolean disaggregation) {
    this(aggregationType, periodAggregationType);
    this.dataType = dataType;
    this.disaggregation = disaggregation;
  }

  /** Returns a new instance of this aggregation type. */
  public AnalyticsAggregationType instance() {
    return new AnalyticsAggregationType(
        this.aggregationType, this.periodAggregationType, this.dataType, this.disaggregation);
  }

  public static AnalyticsAggregationType fromAggregationType(AggregationType aggregationType) {
    return switch (aggregationType) {
      case AVERAGE_SUM_ORG_UNIT -> of(AggregationType.SUM, AggregationType.AVERAGE);
      case LAST -> of(AggregationType.SUM, AggregationType.LAST);
      case LAST_AVERAGE_ORG_UNIT -> of(AggregationType.AVERAGE, AggregationType.LAST);
      case LAST_LAST_ORG_UNIT -> of(AggregationType.LAST, AggregationType.LAST);
      case LAST_IN_PERIOD -> of(AggregationType.SUM, AggregationType.LAST_IN_PERIOD);
      case LAST_IN_PERIOD_AVERAGE_ORG_UNIT ->
          of(AggregationType.AVERAGE, AggregationType.LAST_IN_PERIOD);
      case FIRST -> of(AggregationType.SUM, AggregationType.FIRST);
      case FIRST_AVERAGE_ORG_UNIT -> of(AggregationType.AVERAGE, AggregationType.FIRST);
      case FIRST_FIRST_ORG_UNIT -> of(AggregationType.FIRST, AggregationType.FIRST);
      case MAX_SUM_ORG_UNIT -> of(AggregationType.SUM, AggregationType.MAX);
      case MIN_SUM_ORG_UNIT -> of(AggregationType.SUM, AggregationType.MIN);
      default -> of(aggregationType, aggregationType);
    };
  }

  public boolean isAggregationType(AggregationType type) {
    return this.aggregationType == type;
  }

  public boolean isPeriodAggregationType(AggregationType type) {
    return this.periodAggregationType == type;
  }

  public boolean isLastPeriodAggregationType() {
    return periodAggregationType != null && periodAggregationType.isLast();
  }

  public boolean isFirstPeriodAggregationType() {
    return periodAggregationType != null && periodAggregationType.isFirst();
  }

  public boolean isLastInPeriodAggregationType() {
    return AggregationType.LAST_IN_PERIOD == periodAggregationType
        || AggregationType.LAST_IN_PERIOD_AVERAGE_ORG_UNIT == periodAggregationType;
  }

  public boolean isFirstOrLastPeriodAggregationType() {
    return isFirstPeriodAggregationType() || isLastPeriodAggregationType();
  }

  public boolean isFirstOrLastOrLastInPeriodAggregationType() {
    return isFirstPeriodAggregationType()
        || isLastPeriodAggregationType()
        || isLastInPeriodAggregationType();
  }

  public boolean isMinOrMaxInPeriodAggregationType() {
    return periodAggregationType != aggregationType
        && (AggregationType.MIN == periodAggregationType
            || AggregationType.MAX == periodAggregationType);
  }

  public boolean isNumericDataType() {
    return this.dataType == DataType.NUMERIC;
  }

  public boolean isBooleanDataType() {
    return this.dataType == DataType.BOOLEAN;
  }

  /**
   * Returns the period {@link AggregationType}, with fallback to the general {@link
   * AggregationType} if not specified.
   */
  public AggregationType getPeriodAggregationType() {
    return ObjectUtils.firstNonNull(periodAggregationType, aggregationType);
  }
}
