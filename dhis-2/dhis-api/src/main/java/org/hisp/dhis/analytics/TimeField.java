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

import static com.google.common.collect.Sets.newHashSet;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.table.EnrollmentAnalyticsColumnName;
import org.hisp.dhis.analytics.table.EventAnalyticsColumnName;

/** Enum that maps database column names to their respective "business" names. */
@RequiredArgsConstructor
public enum TimeField {
  EVENT_DATE(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME),
  ENROLLMENT_DATE(
      EnrollmentAnalyticsColumnName.ENROLLMENT_DATE_COLUMN_NAME,
      EventAnalyticsColumnName.ENROLLMENT_DATE_COLUMN_NAME,
      "enrollmentdate"),
  INCIDENT_DATE(
      EnrollmentAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME,
      EventAnalyticsColumnName.ENROLLMENT_OCCURRED_DATE_COLUMN_NAME,
      "occurreddate"),
  OCCURRED_DATE("occurreddate"),
  SCHEDULED_DATE(EventAnalyticsColumnName.SCHEDULED_DATE_COLUMN_NAME),
  COMPLETED_DATE(EventAnalyticsColumnName.COMPLETED_DATE_COLUMN_NAME),
  CREATED(EventAnalyticsColumnName.CREATED_COLUMN_NAME),
  CREATED_DATE(EventAnalyticsColumnName.CREATED_DATE_COLUMN_NAME),
  LAST_UPDATED(
      EnrollmentAnalyticsColumnName.LAST_UPDATED_COLUMN_NAME,
      EventAnalyticsColumnName.LAST_UPDATED_COLUMN_NAME,
      "lastupdated");

  @Getter private final String enrollmentColumnName;
  @Getter private final String eventColumnName;
  @Getter private final String trackedEntityColumnName;

  public static final Collection<String> DEFAULT_TIME_FIELDS =
      List.of(EVENT_DATE.name(), LAST_UPDATED.name(), ENROLLMENT_DATE.name());

  /**
   * These constants represent the columns that can be compared using the raw period column (in the
   * analytics tables), instead of dates. This is preferable for performance reasons.
   */
  private static final Collection<TimeField> SUPPORTING_RAW_FIELD_TIME_FIELDS =
      List.of(EVENT_DATE, SCHEDULED_DATE, ENROLLMENT_DATE);

  private static final Set<String> FIELD_NAMES =
      newHashSet(TimeField.values()).stream().map(TimeField::name).collect(toSet());

  TimeField(final String field) {
    this.trackedEntityColumnName = field;
    this.eventColumnName = field;
    this.enrollmentColumnName = field;
  }

  public static Optional<TimeField> of(final String timeField) {
    return Arrays.stream(values()).filter(tf -> tf.name().equals(timeField)).findFirst();
  }

  private static Optional<TimeField> from(final String field) {
    return Arrays.stream(values())
        .filter(timeField -> timeField.getEventColumnName().equals(field))
        .findFirst();
  }

  public static boolean fieldIsValid(final String field) {
    return isNotBlank(field) && FIELD_NAMES.contains(field);
  }

  public boolean supportsRawPeriod() {
    return Optional.of(eventColumnName)
        .filter(StringUtils::isNotBlank)
        .flatMap(TimeField::from)
        .map(SUPPORTING_RAW_FIELD_TIME_FIELDS::contains)
        .orElse(false);
  }
}
