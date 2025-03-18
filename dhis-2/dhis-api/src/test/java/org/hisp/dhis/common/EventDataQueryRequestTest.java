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

import static org.hisp.dhis.common.RequestTypeAware.EndpointAction.AGGREGATE;
import static org.hisp.dhis.common.RequestTypeAware.EndpointAction.QUERY;
import static org.hisp.dhis.common.RequestTypeAware.EndpointItem.ENROLLMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class EventDataQueryRequestTest {

  public static boolean[] totalPagesFlags() {
    return new boolean[] {false, true};
  }

  public static Stream<Arguments> deprecatedEventDateFieldTestCases() {
    Object[][] testCases = {
      {
        (Consumer<EventsAnalyticsQueryCriteria>) criteria -> criteria.setEventDate("LAST_YEAR"),
        "pe:LAST_YEAR:EVENT_DATE"
      },
      {
        (Consumer<EventsAnalyticsQueryCriteria>) criteria -> criteria.setOccurredDate("LAST_MONTH"),
        "pe:LAST_MONTH:OCCURRED_DATE"
      },
      {
        (Consumer<EventsAnalyticsQueryCriteria>) criteria -> criteria.setIncidentDate("2021-06-30"),
        "pe:2021-06-30:INCIDENT_DATE"
      }
    };

    return Stream.of(testCases).map(Arguments::of);
  }

  @ParameterizedTest
  @MethodSource(value = "deprecatedEventDateFieldTestCases")
  void testDeprecatedEventDateFieldsAreCorrectlyMapped(
      Consumer<EventsAnalyticsQueryCriteria> setter, String expectedDimension) {
    EventsAnalyticsQueryCriteria criteria = new EventsAnalyticsQueryCriteria();
    setter.accept(criteria);

    EventDataQueryRequest eventDataQueryRequest =
        EventDataQueryRequest.builder()
            .fromCriteria((EventsAnalyticsQueryCriteria) criteria.withEndpointAction(QUERY))
            .build();

    assertEquals(Set.of(Set.of(expectedDimension)), eventDataQueryRequest.getDimension());
  }

  public static Stream<Arguments> deprecatedEnrollmentDateFieldTestCases() {
    Object[][] testCases = {
      {
        (Consumer<EnrollmentAnalyticsQueryCriteria>)
            criteria -> criteria.setOccurredDate("LAST_MONTH"),
        "pe:LAST_MONTH:OCCURRED_DATE"
      },
      {
        (Consumer<EnrollmentAnalyticsQueryCriteria>)
            criteria -> criteria.setIncidentDate("2021-06-30"),
        "pe:2021-06-30:INCIDENT_DATE"
      }
    };

    return Stream.of(testCases).map(Arguments::of);
  }

  @ParameterizedTest
  @MethodSource(value = "deprecatedEnrollmentDateFieldTestCases")
  void testDeprecatedEnrollmentDateFieldsAreCorrectlyMapped(
      Consumer<EnrollmentAnalyticsQueryCriteria> setter, String expectedDimension) {
    EnrollmentAnalyticsQueryCriteria criteria = new EnrollmentAnalyticsQueryCriteria();
    setter.accept(criteria);

    EventDataQueryRequest eventDataQueryRequest =
        EventDataQueryRequest.builder()
            .fromCriteria((EnrollmentAnalyticsQueryCriteria) criteria.withEndpointAction(QUERY))
            .build();

    assertEquals(Set.of(Set.of(expectedDimension)), eventDataQueryRequest.getDimension());
  }

  @Test
  void testDimensionRefactoringOnlyWhenQuery() {
    EventsAnalyticsQueryCriteria criteria = new EventsAnalyticsQueryCriteria();
    criteria.setOccurredDate("YESTERDAY");
    criteria.setDimension(Set.of("pe:TODAY"));

    EventDataQueryRequest eventDataQueryRequest =
        EventDataQueryRequest.builder().fromCriteria(criteria).build();

    assertEquals(eventDataQueryRequest.getDimension(), Set.of(Set.of("pe:TODAY")));

    eventDataQueryRequest =
        EventDataQueryRequest.builder()
            .fromCriteria((EventsAnalyticsQueryCriteria) criteria.withEndpointAction(QUERY))
            .build();

    assertEquals(
        eventDataQueryRequest.getDimension(), Set.of(Set.of("pe:TODAY;YESTERDAY:OCCURRED_DATE")));

    criteria = new EventsAnalyticsQueryCriteria();
    criteria.setOccurredDate("TODAY");
    criteria.setDimension(new HashSet<>());

    eventDataQueryRequest = EventDataQueryRequest.builder().fromCriteria(criteria).build();

    assertEquals(Collections.emptySet(), eventDataQueryRequest.getDimension());

    eventDataQueryRequest =
        EventDataQueryRequest.builder()
            .fromCriteria((EventsAnalyticsQueryCriteria) criteria.withEndpointAction(QUERY))
            .build();

    assertEquals(eventDataQueryRequest.getDimension(), Set.of(Set.of("pe:TODAY:OCCURRED_DATE")));
  }

  @Test
  void testEventsQueryMultiOptionsAreCorrectlyParsed() {
    EventsAnalyticsQueryCriteria criteria = new EventsAnalyticsQueryCriteria();
    criteria.setEnrollmentDate("202111,2021;TODAY");
    criteria.setEventDate("LAST_YEAR");
    criteria.setDimension(new HashSet<>(Set.of("pe:LAST_MONTH")));

    EventDataQueryRequest eventDataQueryRequest =
        EventDataQueryRequest.builder()
            .fromCriteria((EventsAnalyticsQueryCriteria) criteria.withEndpointAction(QUERY))
            .build();

    assertEquals(
        eventDataQueryRequest.getDimension(),
        Set.of(
            Set.of(
                "pe:LAST_MONTH;LAST_YEAR:EVENT_DATE;202111:ENROLLMENT_DATE;2021:ENROLLMENT_DATE;TODAY:ENROLLMENT_DATE")));
  }

  @Test
  void testEnrollmentMultiOptionsAreCorrectlyParsed() {
    EnrollmentAnalyticsQueryCriteria criteria = new EnrollmentAnalyticsQueryCriteria();
    criteria.setOccurredDate("202111,2021;TODAY");
    criteria.setEnrollmentDate("LAST_YEAR");
    criteria.setDimension(new HashSet<>(Set.of("pe:LAST_MONTH")));

    EventDataQueryRequest eventDataQueryRequest =
        EventDataQueryRequest.builder()
            .fromCriteria((EnrollmentAnalyticsQueryCriteria) criteria.withEndpointAction(QUERY))
            .build();

    assertEquals(
        eventDataQueryRequest.getDimension(),
        Set.of(
            Set.of(
                "pe:LAST_MONTH;LAST_YEAR:ENROLLMENT_DATE;202111:OCCURRED_DATE;2021:OCCURRED_DATE;TODAY:OCCURRED_DATE")));
  }

  @Test
  void testAggregateEnrollmentMultiOptionsAreCorrectlyParsed() {
    // Given
    EnrollmentAnalyticsQueryCriteria criteria = new EnrollmentAnalyticsQueryCriteria();
    criteria.setOccurredDate("202111,2021;TODAY");
    criteria.setEnrollmentDate("LAST_YEAR");
    criteria.setDimension(new HashSet<>(Set.of("pe:LAST_MONTH")));

    // When
    EventDataQueryRequest eventDataQueryRequest =
        EventDataQueryRequest.builder()
            .fromCriteria(
                (EnrollmentAnalyticsQueryCriteria)
                    criteria.withEndpointAction(AGGREGATE).withEndpointItem(ENROLLMENT))
            .build();
    // Then
    assertEquals(
        eventDataQueryRequest.getDimension(),
        Set.of(
            Set.of(
                "pe:LAST_MONTH;LAST_YEAR:ENROLLMENT_DATE;202111:OCCURRED_DATE;2021:OCCURRED_DATE;TODAY:OCCURRED_DATE")));
  }

  @ParameterizedTest
  @MethodSource(value = "totalPagesFlags")
  void totalPagesShouldBeSameInCriteriaAndRequestWhenCalled(boolean totalPages) {
    EnrollmentAnalyticsQueryCriteria criteria = new EnrollmentAnalyticsQueryCriteria();

    criteria.setTotalPages(totalPages);

    assertEquals(
        EventDataQueryRequest.builder().fromCriteria(criteria).build().isTotalPages(), totalPages);
  }
}
