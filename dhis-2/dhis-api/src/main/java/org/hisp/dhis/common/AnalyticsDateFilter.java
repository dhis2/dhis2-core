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
package org.hisp.dhis.common;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.TimeField;
import org.hisp.dhis.analytics.common.CommonRequestParams;

/**
 * Enum to map time fields into functions that can extract respective date from controller
 * parameters
 */
@Getter
@RequiredArgsConstructor
public enum AnalyticsDateFilter {
  /**
   * @deprecated use {@link #OCCURRED_DATE} instead. Kept for backward compatibility.
   */
  @Deprecated(since = "2.42")
  EVENT_DATE(
      TimeField.EVENT_DATE,
      EventsAnalyticsQueryCriteria::getEventDate,
      EnrollmentAnalyticsQueryCriteria::getEventDate,
      CommonRequestParams::getEventDate),
  ENROLLMENT_DATE(
      TimeField.ENROLLMENT_DATE,
      EventsAnalyticsQueryCriteria::getEnrollmentDate,
      EnrollmentAnalyticsQueryCriteria::getEnrollmentDate,
      CommonRequestParams::getEnrollmentDate),
  SCHEDULED_DATE(
      TimeField.SCHEDULED_DATE,
      EventsAnalyticsQueryCriteria::getScheduledDate,
      null,
      CommonRequestParams::getScheduledDate),
  /**
   * @deprecated use {@link #OCCURRED_DATE} instead. Kept for backward compatibility.
   */
  @Deprecated(since = "2.42")
  INCIDENT_DATE(
      TimeField.INCIDENT_DATE,
      // Events
      EventsAnalyticsQueryCriteria::getIncidentDate,
      // Enrollments
      EnrollmentAnalyticsQueryCriteria::getIncidentDate,
      // TEs
      CommonRequestParams::getIncidentDate),
  OCCURRED_DATE(
      TimeField.OCCURRED_DATE,
      EventsAnalyticsQueryCriteria::getOccurredDate,
      EnrollmentAnalyticsQueryCriteria::getOccurredDate,
      CommonRequestParams::getOccurredDate),
  LAST_UPDATED(
      TimeField.LAST_UPDATED,
      EventsAnalyticsQueryCriteria::getLastUpdated,
      EnrollmentAnalyticsQueryCriteria::getLastUpdated,
      CommonRequestParams::getLastUpdated),
  CREATED(TimeField.CREATED, null, null, CommonRequestParams::getCreated),
  COMPLETED_DATE(
      TimeField.COMPLETED_DATE,
      EventsAnalyticsQueryCriteria::getCompletedDate,
      EnrollmentAnalyticsQueryCriteria::getCompletedDate,
      // No matching column for TEA analytics queries
      null),
  CREATED_DATE(
      TimeField.CREATED_DATE,
      EventsAnalyticsQueryCriteria::getCreatedDate,
      EnrollmentAnalyticsQueryCriteria::getCreatedDate,
      // No matching column for TEA analytics queries
      null);

  private final TimeField timeField;

  private final Function<EventsAnalyticsQueryCriteria, String> eventExtractor;

  private final Function<EnrollmentAnalyticsQueryCriteria, String> enrollmentExtractor;

  private final Function<CommonRequestParams, Set<String>> trackedEntityExtractor;

  public static Optional<AnalyticsDateFilter> of(String dateField) {
    return Arrays.stream(values())
        .filter(analyticsDateFilter -> analyticsDateFilter.name().equalsIgnoreCase(dateField))
        .findFirst();
  }

  public boolean appliesToEnrollments() {
    return enrollmentExtractor != null;
  }

  public boolean appliesToEvents() {
    return eventExtractor != null;
  }
}
