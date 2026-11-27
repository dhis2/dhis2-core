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
import static org.hisp.dhis.program.EnrollmentStatus.ACTIVE;
import static org.hisp.dhis.program.EnrollmentStatus.COMPLETED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.EventOutputType;
import org.hisp.dhis.feedback.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

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
      },
      {
        (Consumer<EventsAnalyticsQueryCriteria>)
            criteria -> criteria.setEnrollmentOccurredDate("2021-06-30"),
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
  void testEventsAggregateMapsEnrollmentOccurredDate() {
    EventsAnalyticsQueryCriteria criteria = new EventsAnalyticsQueryCriteria();
    criteria.setEnrollmentOccurredDate("2021-01-01_2022-12-31");

    EventDataQueryRequest eventDataQueryRequest =
        EventDataQueryRequest.builder()
            .fromCriteria((EventsAnalyticsQueryCriteria) criteria.withEndpointAction(AGGREGATE))
            .build();

    assertEquals(
        Set.of(Set.of("pe:2021-01-01_2022-12-31:INCIDENT_DATE")),
        eventDataQueryRequest.getDimension());
  }

  @Test
  void testEventsIncidentDateAndEnrollmentOccurredDateAreCombined() {
    EventsAnalyticsQueryCriteria criteria = new EventsAnalyticsQueryCriteria();
    criteria.setIncidentDate("2021");
    criteria.setEnrollmentOccurredDate("2022");

    EventDataQueryRequest eventDataQueryRequest =
        EventDataQueryRequest.builder()
            .fromCriteria((EventsAnalyticsQueryCriteria) criteria.withEndpointAction(QUERY))
            .build();

    assertEquals(
        Set.of(Set.of("pe:2021:INCIDENT_DATE;2022:INCIDENT_DATE")),
        eventDataQueryRequest.getDimension());
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

  /**
   * Enrollment requests must carry the ENROLLMENT output type so query items such as {@code
   * INCIDENT_DATE} resolve to enrollment analytics table columns.
   */
  @ParameterizedTest
  @EnumSource(
      value = RequestTypeAware.EndpointAction.class,
      names = {"QUERY", "AGGREGATE"})
  void testEnrollmentCriteriaSetsEnrollmentOutputType(RequestTypeAware.EndpointAction action) {
    EnrollmentAnalyticsQueryCriteria criteria = new EnrollmentAnalyticsQueryCriteria();

    EventDataQueryRequest eventDataQueryRequest =
        EventDataQueryRequest.builder()
            .fromCriteria(
                (EnrollmentAnalyticsQueryCriteria)
                    criteria.withEndpointAction(action).withEndpointItem(ENROLLMENT))
            .build();

    assertEquals(EventOutputType.ENROLLMENT, eventDataQueryRequest.getOutputType());
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

  @Test
  void aggregateEnrollmentMapsValueAndAggregationType() {
    EnrollmentAnalyticsQueryCriteria criteria = new EnrollmentAnalyticsQueryCriteria();
    criteria.setValue("edqlbukwRfQ.vANAXwtLwcT");
    criteria.setAggregationType(AggregationType.AVERAGE);

    EventDataQueryRequest request =
        EventDataQueryRequest.builder()
            .fromCriteria(
                (EnrollmentAnalyticsQueryCriteria)
                    criteria.withEndpointAction(AGGREGATE).withEndpointItem(ENROLLMENT))
            .build();

    assertEquals("edqlbukwRfQ.vANAXwtLwcT", request.getValue());
    assertEquals(AggregationType.AVERAGE, request.getAggregationType());
  }

  @Test
  void enrollmentQueryDoesNotMapValueOrAggregationType() {
    EnrollmentAnalyticsQueryCriteria criteria = new EnrollmentAnalyticsQueryCriteria();
    criteria.setValue("edqlbukwRfQ.vANAXwtLwcT");
    criteria.setAggregationType(AggregationType.AVERAGE);

    EventDataQueryRequest request =
        EventDataQueryRequest.builder()
            .fromCriteria(
                (EnrollmentAnalyticsQueryCriteria)
                    criteria.withEndpointAction(QUERY).withEndpointItem(ENROLLMENT))
            .build();

    assertNull(request.getValue());
    assertNull(request.getAggregationType());
  }

  @Test
  void eventsWithoutStatusParamsHaveNoEnrollmentStatusFilter() {
    EventDataQueryRequest request =
        EventDataQueryRequest.builder().fromCriteria(new EventsAnalyticsQueryCriteria()).build();

    assertEquals(Set.of(), request.getEnrollmentStatus());
  }

  @Test
  void enrollmentsWithoutStatusParamsHaveNoEnrollmentStatusFilter() {
    EventDataQueryRequest request =
        EventDataQueryRequest.builder()
            .fromCriteria(new EnrollmentAnalyticsQueryCriteria())
            .build();

    assertEquals(Set.of(), request.getEnrollmentStatus());
  }

  @Test
  void eventsEnrollmentStatusIsMapped() {
    EventsAnalyticsQueryCriteria criteria = new EventsAnalyticsQueryCriteria();
    criteria.setEnrollmentStatus(Set.of(COMPLETED));

    EventDataQueryRequest request = EventDataQueryRequest.builder().fromCriteria(criteria).build();

    assertEquals(Set.of(COMPLETED), request.getEnrollmentStatus());
  }

  @Test
  void eventsDeprecatedProgramStatusIsMapped() {
    EventsAnalyticsQueryCriteria criteria = new EventsAnalyticsQueryCriteria();
    criteria.setProgramStatus(Set.of(ACTIVE));

    EventDataQueryRequest request = EventDataQueryRequest.builder().fromCriteria(criteria).build();

    assertEquals(Set.of(ACTIVE), request.getEnrollmentStatus());
  }

  @Test
  void eventsProgramStatusAndEnrollmentStatusTogetherAreRejected() {
    EventsAnalyticsQueryCriteria criteria = new EventsAnalyticsQueryCriteria();
    criteria.setProgramStatus(Set.of(ACTIVE));
    criteria.setEnrollmentStatus(Set.of(COMPLETED));

    IllegalQueryException ex =
        assertThrows(
            IllegalQueryException.class,
            () -> EventDataQueryRequest.builder().fromCriteria(criteria));

    assertEquals(ErrorCode.E7139, ex.getErrorCode());
  }

  @Test
  void enrollmentsEnrollmentStatusIsMapped() {
    EnrollmentAnalyticsQueryCriteria criteria = new EnrollmentAnalyticsQueryCriteria();
    criteria.setEnrollmentStatus(Set.of(COMPLETED));

    EventDataQueryRequest request = EventDataQueryRequest.builder().fromCriteria(criteria).build();

    assertEquals(Set.of(COMPLETED), request.getEnrollmentStatus());
  }

  @Test
  void enrollmentsDeprecatedProgramStatusIsMapped() {
    EnrollmentAnalyticsQueryCriteria criteria = new EnrollmentAnalyticsQueryCriteria();
    criteria.setProgramStatus(Set.of(ACTIVE));

    EventDataQueryRequest request = EventDataQueryRequest.builder().fromCriteria(criteria).build();

    assertEquals(Set.of(ACTIVE), request.getEnrollmentStatus());
  }

  @Test
  void enrollmentsProgramStatusAndEnrollmentStatusTogetherAreRejected() {
    EnrollmentAnalyticsQueryCriteria criteria = new EnrollmentAnalyticsQueryCriteria();
    criteria.setProgramStatus(Set.of(ACTIVE));
    criteria.setEnrollmentStatus(Set.of(COMPLETED));

    IllegalQueryException ex =
        assertThrows(
            IllegalQueryException.class,
            () -> EventDataQueryRequest.builder().fromCriteria(criteria));

    assertEquals(ErrorCode.E7139, ex.getErrorCode());
  }

  public static Stream<String> dateRangeDimensions() {
    return Stream.of(
        "pe:2026-11-01_2026-12-31",
        "pe:20261101_20261231",
        "pe:202611;2026-11-01_2026-12-31",
        "pe:2026-11-01_2026-12-31:ENROLLMENT_DATE",
        "ENROLLMENT_DATE:2026-11-01_2026-12-31",
        "ENROLLMENT_DATE:2026-11-01_2026-11-01",
        "INCIDENT_DATE:2026-11-01_2026-12-31",
        "EVENT_DATE:2026-11-01_2026-12-31",
        "SCHEDULED_DATE:2026-11-01_2026-12-31",
        "LAST_UPDATED:2026-11-01_2026-12-31",
        "CREATED:2026-11-01_2026-12-31",
        "COMPLETED:2026-11-01_2026-12-31");
  }

  @ParameterizedTest
  @MethodSource("dateRangeDimensions")
  void eventsAggregateRejectsDateRangeDimension(String dimension) {
    EventsAnalyticsQueryCriteria criteria = new EventsAnalyticsQueryCriteria();
    criteria.setDimension(new HashSet<>(Set.of("ou:ImspTQPwCqd", dimension)));
    criteria.withEndpointAction(AGGREGATE);

    IllegalQueryException ex =
        assertThrows(
            IllegalQueryException.class,
            () -> EventDataQueryRequest.builder().fromCriteria(criteria));

    assertEquals(ErrorCode.E7270, ex.getErrorCode());
  }

  @ParameterizedTest
  @MethodSource("dateRangeDimensions")
  void enrollmentsAggregateRejectsDateRangeDimension(String dimension) {
    EnrollmentAnalyticsQueryCriteria criteria = new EnrollmentAnalyticsQueryCriteria();
    criteria.setDimension(new HashSet<>(Set.of("ou:ImspTQPwCqd", dimension)));
    criteria.withEndpointAction(AGGREGATE).withEndpointItem(ENROLLMENT);

    IllegalQueryException ex =
        assertThrows(
            IllegalQueryException.class,
            () -> EventDataQueryRequest.builder().fromCriteria(criteria));

    assertEquals(ErrorCode.E7270, ex.getErrorCode());
  }

  @ParameterizedTest
  @MethodSource({"dateRangeDimensions", "stageDateDimensionsWithRangeAndOtherItems"})
  void eventsQueryAcceptsDateRangeDimension(String dimension) {
    EventsAnalyticsQueryCriteria criteria = new EventsAnalyticsQueryCriteria();
    criteria.setDimension(new HashSet<>(Set.of(dimension)));
    criteria.withEndpointAction(QUERY);

    EventDataQueryRequest request = EventDataQueryRequest.builder().fromCriteria(criteria).build();

    assertEquals(Set.of(Set.of(dimension)), request.getDimension());
  }

  @ParameterizedTest
  @MethodSource("dateRangeDimensions")
  void enrollmentsQueryAcceptsDateRangeDimension(String dimension) {
    EnrollmentAnalyticsQueryCriteria criteria = new EnrollmentAnalyticsQueryCriteria();
    criteria.setDimension(new HashSet<>(Set.of(dimension)));
    criteria.withEndpointAction(QUERY).withEndpointItem(ENROLLMENT);

    EventDataQueryRequest request = EventDataQueryRequest.builder().fromCriteria(criteria).build();

    assertEquals(Set.of(Set.of(dimension)), request.getDimension());
  }

  @ParameterizedTest
  @MethodSource({"dateRangeDimensions", "stageDateDimensionsWithRangeAndOtherItems"})
  void eventsAggregateAcceptsDateRangeFilter(String filter) {
    EventsAnalyticsQueryCriteria criteria = new EventsAnalyticsQueryCriteria();
    criteria.setDimension(new HashSet<>(Set.of("ou:ImspTQPwCqd")));
    criteria.setFilter(new HashSet<>(Set.of(filter)));
    criteria.withEndpointAction(AGGREGATE);

    EventDataQueryRequest request = EventDataQueryRequest.builder().fromCriteria(criteria).build();

    assertEquals(Set.of(Set.of(filter)), request.getFilter());
  }

  @ParameterizedTest
  @MethodSource("dateRangeDimensions")
  void enrollmentsAggregateAcceptsDateRangeFilter(String filter) {
    EnrollmentAnalyticsQueryCriteria criteria = new EnrollmentAnalyticsQueryCriteria();
    criteria.setDimension(new HashSet<>(Set.of("ou:ImspTQPwCqd")));
    criteria.setFilter(new HashSet<>(Set.of(filter)));
    criteria.withEndpointAction(AGGREGATE).withEndpointItem(ENROLLMENT);

    EventDataQueryRequest request = EventDataQueryRequest.builder().fromCriteria(criteria).build();

    assertEquals(Set.of(Set.of(filter)), request.getFilter());
  }

  @Test
  void eventsAggregateAcceptsDateRangeRequestParam() {
    EventsAnalyticsQueryCriteria criteria = new EventsAnalyticsQueryCriteria();
    criteria.setIncidentDate("2021-03-01_2023-04-30");
    criteria.setDimension(new HashSet<>(Set.of("ou:ImspTQPwCqd")));
    criteria.withEndpointAction(AGGREGATE);

    EventDataQueryRequest request = EventDataQueryRequest.builder().fromCriteria(criteria).build();

    assertEquals(
        Set.of(Set.of("ou:ImspTQPwCqd"), Set.of("pe:2021-03-01_2023-04-30:INCIDENT_DATE")),
        request.getDimension());
  }

  @Test
  void enrollmentsAggregateAcceptsDateRangeRequestParam() {
    EnrollmentAnalyticsQueryCriteria criteria = new EnrollmentAnalyticsQueryCriteria();
    criteria.setEnrollmentDate("2021-03-01_2023-04-30");
    criteria.setDimension(new HashSet<>(Set.of("ou:ImspTQPwCqd")));
    criteria.withEndpointAction(AGGREGATE).withEndpointItem(ENROLLMENT);

    EventDataQueryRequest request = EventDataQueryRequest.builder().fromCriteria(criteria).build();

    assertEquals(
        Set.of(Set.of("ou:ImspTQPwCqd"), Set.of("pe:2021-03-01_2023-04-30:ENROLLMENT_DATE")),
        request.getDimension());
  }

  /**
   * Relative periods contain the range separator, and stage-scoped dates are query items rather
   * than time dimensions, so neither is a date range dimension.
   */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "pe:LAST_12_MONTHS",
        "ENROLLMENT_DATE:THIS_YEAR",
        "ENROLLMENT_DATE:LAST_5_YEARS",
        "A03MvHHogjR.EVENT_DATE:2021-03-01_2021-05-31",
        "A03MvHHogjR.SCHEDULED_DATE:2021-03-01_2021-05-31",
        "A03MvHHogjR.EVENT_DATE:2021;2022Q1",
        "A03MvHHogjR.EVENT_DATE:LAST_12_MONTHS;THIS_YEAR",
        "A03MvHHogjR.SCHEDULED_DATE:2021;2022Q1"
      })
  void eventsAggregateAcceptsDimensionsThatAreNotDateRanges(String dimension) {
    EventsAnalyticsQueryCriteria criteria = new EventsAnalyticsQueryCriteria();
    criteria.setDimension(new HashSet<>(Set.of(dimension)));
    criteria.withEndpointAction(AGGREGATE);

    EventDataQueryRequest request = EventDataQueryRequest.builder().fromCriteria(criteria).build();

    assertEquals(Set.of(Set.of(dimension)), request.getDimension());
  }

  public static Stream<String> stageDateDimensionsWithRangeAndOtherItems() {
    return Stream.of(
        "A03MvHHogjR.EVENT_DATE:2021;2022Q1;2022-05-01_2022-05-31",
        "A03MvHHogjR.EVENT_DATE:2022-05-01_2022-05-31;2022",
        "A03MvHHogjR.EVENT_DATE:LAST_12_MONTHS;2022-05-01_2022-05-31",
        "A03MvHHogjR.EVENT_DATE:2022-01-01_2022-01-31;2022-03-01_2022-03-31",
        "A03MvHHogjR.SCHEDULED_DATE:2022;2022-05-01_2022-05-31",
        "A03MvHHogjR.event_date:2022;2022-05-01_2022-05-31");
  }

  @ParameterizedTest
  @MethodSource("stageDateDimensionsWithRangeAndOtherItems")
  void eventsAggregateRejectsStageDateDimensionWithRangeAndOtherItems(String dimension) {
    EventsAnalyticsQueryCriteria criteria = new EventsAnalyticsQueryCriteria();
    criteria.setDimension(new HashSet<>(Set.of("ou:ImspTQPwCqd", dimension)));
    criteria.withEndpointAction(AGGREGATE);

    IllegalQueryException ex =
        assertThrows(
            IllegalQueryException.class,
            () -> EventDataQueryRequest.builder().fromCriteria(criteria));

    assertEquals(ErrorCode.E7271, ex.getErrorCode());
  }

  @ParameterizedTest
  @MethodSource("stageDateDimensionsWithRangeAndOtherItems")
  void enrollmentsAggregateRejectsStageDateDimensionWithRangeAndOtherItems(String dimension) {
    EnrollmentAnalyticsQueryCriteria criteria = new EnrollmentAnalyticsQueryCriteria();
    criteria.setDimension(new HashSet<>(Set.of("ou:ImspTQPwCqd", dimension)));
    criteria.withEndpointAction(AGGREGATE).withEndpointItem(ENROLLMENT);

    IllegalQueryException ex =
        assertThrows(
            IllegalQueryException.class,
            () -> EventDataQueryRequest.builder().fromCriteria(criteria));

    assertEquals(ErrorCode.E7271, ex.getErrorCode());
  }
}
