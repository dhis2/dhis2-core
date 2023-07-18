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
package org.hisp.dhis.analytics;

import com.google.common.base.MoreObjects;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.hisp.dhis.util.ObjectUtils;

/**
 * Enum which represents the aggregation type for analytics requests.
 *
 * @author Lars Helge Overland
 */
public class AnalyticsAggregationType {
  public static final AnalyticsAggregationType SUM =
      new AnalyticsAggregationType(AggregationType.SUM, AggregationType.SUM);

  public static final AnalyticsAggregationType AVERAGE =
      new AnalyticsAggregationType(AggregationType.AVERAGE, AggregationType.AVERAGE);

  public static final AnalyticsAggregationType COUNT =
      new AnalyticsAggregationType(AggregationType.COUNT, AggregationType.COUNT);

  public static final AnalyticsAggregationType FIRST =
      new AnalyticsAggregationType(AggregationType.SUM, AggregationType.FIRST);

  public static final AnalyticsAggregationType LAST =
      new AnalyticsAggregationType(AggregationType.SUM, AggregationType.LAST);

  public static final AnalyticsAggregationType LAST_IN_PERIOD =
      new AnalyticsAggregationType(AggregationType.SUM, AggregationType.LAST_IN_PERIOD);

  /** General aggregation type. */
  private final AggregationType aggregationType;

  /** Aggregation type for the period dimension. */
  private final AggregationType periodAggregationType;

  /** Analytics data type. */
  private DataType dataType;

  /** Indicates whether to perform data disaggregation. */
  private boolean disaggregation;

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

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
    Validate.notNull(this.aggregationType);
    Validate.notNull(this.periodAggregationType);
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

  // -------------------------------------------------------------------------
  // Logic methods
  // -------------------------------------------------------------------------

  public static AnalyticsAggregationType fromAggregationType(AggregationType aggregationType) {
    AnalyticsAggregationType analyticsAggregationType;

    switch (aggregationType) {
      case AVERAGE_SUM_ORG_UNIT:
        analyticsAggregationType =
            new AnalyticsAggregationType(AggregationType.SUM, AggregationType.AVERAGE);
        break;
      case LAST:
        analyticsAggregationType =
            new AnalyticsAggregationType(AggregationType.SUM, AggregationType.LAST);
        break;
      case LAST_AVERAGE_ORG_UNIT:
        analyticsAggregationType =
            new AnalyticsAggregationType(AggregationType.AVERAGE, AggregationType.LAST);
        break;
      case LAST_LAST_ORG_UNIT:
        analyticsAggregationType =
            new AnalyticsAggregationType(AggregationType.LAST, AggregationType.LAST);
        break;
      case LAST_IN_PERIOD:
        analyticsAggregationType =
            new AnalyticsAggregationType(AggregationType.SUM, AggregationType.LAST_IN_PERIOD);
        break;
      case LAST_IN_PERIOD_AVERAGE_ORG_UNIT:
        analyticsAggregationType =
            new AnalyticsAggregationType(AggregationType.AVERAGE, AggregationType.LAST_IN_PERIOD);
        break;
      case FIRST:
        analyticsAggregationType =
            new AnalyticsAggregationType(AggregationType.SUM, AggregationType.FIRST);
        break;
      case FIRST_AVERAGE_ORG_UNIT:
        analyticsAggregationType =
            new AnalyticsAggregationType(AggregationType.AVERAGE, AggregationType.FIRST);
        break;
      case FIRST_FIRST_ORG_UNIT:
        analyticsAggregationType =
            new AnalyticsAggregationType(AggregationType.FIRST, AggregationType.FIRST);
        break;
      case MAX_SUM_ORG_UNIT:
        analyticsAggregationType =
            new AnalyticsAggregationType(AggregationType.SUM, AggregationType.MAX);
        break;
      case MIN_SUM_ORG_UNIT:
        analyticsAggregationType =
            new AnalyticsAggregationType(AggregationType.SUM, AggregationType.MIN);
        break;
      default:
        analyticsAggregationType = new AnalyticsAggregationType(aggregationType, aggregationType);
    }

    return analyticsAggregationType;
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

  // -------------------------------------------------------------------------
  // Get methods
  // -------------------------------------------------------------------------

  /** Returns the general {@link AggregationType}. */
  public AggregationType getAggregationType() {
    return aggregationType;
  }

  /**
   * Returns the period {@link AggregationType}, with fallback to the general {@link
   * AggregationType} if not specified.
   */
  public AggregationType getPeriodAggregationType() {
    return ObjectUtils.firstNonNull(periodAggregationType, aggregationType);
  }

  /** Returns the {@link DataType}. */
  public DataType getDataType() {
    return dataType;
  }

  /** Indicates whether disaggregation is involved. */
  public boolean isDisaggregation() {
    return disaggregation;
  }

  // -------------------------------------------------------------------------
  // toString, equals, hash code
  // -------------------------------------------------------------------------

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("aggregation type", aggregationType)
        .add("period dim aggregation type", periodAggregationType)
        .add("data type", dataType)
        .add("disaggregation", disaggregation)
        .toString();
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }

    if (object == null) {
      return false;
    }

    if (!getClass().isAssignableFrom(object.getClass())) {
      return false;
    }

    AnalyticsAggregationType other = (AnalyticsAggregationType) object;

    return Objects.equals(this.aggregationType, other.aggregationType)
        && Objects.equals(this.periodAggregationType, other.periodAggregationType)
        && Objects.equals(this.dataType, other.dataType)
        && Objects.equals(this.disaggregation, other.disaggregation);
  }

  @Override
  public int hashCode() {
    return 31 * Objects.hash(aggregationType, periodAggregationType, dataType, disaggregation);
  }
}
