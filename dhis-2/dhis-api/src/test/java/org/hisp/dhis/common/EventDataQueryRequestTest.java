package org.hisp.dhis.common;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EventDataQueryRequestTest {

    @Test
    void testDimensionRefactoringOnlyWhenQuery() {
        EventsAnalyticsQueryCriteria criteria = new EventsAnalyticsQueryCriteria();
        criteria.setIncidentDate("YESTERDAY");
        criteria.setDimension(Set.of("pe:TODAY"));

        EventDataQueryRequest eventDataQueryRequest = EventDataQueryRequest.builder()
                .fromCriteria(criteria)
                .build();

        assertEquals(eventDataQueryRequest.getDimension(), Set.of("pe:TODAY"));

        eventDataQueryRequest = EventDataQueryRequest.builder()
                .fromCriteria((EventsAnalyticsQueryCriteria) criteria.withQueryRequestType())
                .build();

        assertEquals(eventDataQueryRequest.getDimension(), Set.of("pe:TODAY;YESTERDAY:INCIDENT_DATE"));

        criteria = new EventsAnalyticsQueryCriteria();
        criteria.setIncidentDate("TODAY");
        criteria.setDimension(new HashSet<>());

        eventDataQueryRequest = EventDataQueryRequest.builder()
                .fromCriteria(criteria)
                .build();

        assertEquals(eventDataQueryRequest.getDimension(), Collections.emptySet());

        eventDataQueryRequest = EventDataQueryRequest.builder()
                .fromCriteria((EventsAnalyticsQueryCriteria) criteria.withQueryRequestType())
                .build();

        assertEquals(eventDataQueryRequest.getDimension(), Set.of("pe:TODAY:INCIDENT_DATE"));

    }
}
