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

import static org.hisp.dhis.common.RequestTypeAware.EndpointAction.QUERY;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class EventDataQueryRequestTest
{

    public static boolean[] totalPagesFlags()
    {
        return new boolean[] { false, true };
    }

    @Test
    void testDimensionRefactoringOnlyWhenQuery()
    {
        EventsAnalyticsQueryCriteria criteria = new EventsAnalyticsQueryCriteria();
        criteria.setIncidentDate( "YESTERDAY" );
        criteria.setDimension( Set.of( "pe:TODAY" ) );

        EventDataQueryRequest eventDataQueryRequest = EventDataQueryRequest.builder()
            .fromCriteria( criteria )
            .build();

        assertEquals( eventDataQueryRequest.getDimension(), Set.of( Set.of( "pe:TODAY" ) ) );

        eventDataQueryRequest = EventDataQueryRequest.builder()
            .fromCriteria( (EventsAnalyticsQueryCriteria) criteria.withEndpointAction( QUERY ) )
            .build();

        assertEquals( eventDataQueryRequest.getDimension(), Set.of( Set.of( "pe:TODAY;YESTERDAY:INCIDENT_DATE" ) ) );

        criteria = new EventsAnalyticsQueryCriteria();
        criteria.setIncidentDate( "TODAY" );
        criteria.setDimension( new HashSet<>() );

        eventDataQueryRequest = EventDataQueryRequest.builder()
            .fromCriteria( criteria )
            .build();

        assertEquals( eventDataQueryRequest.getDimension(), Collections.emptySet() );

        eventDataQueryRequest = EventDataQueryRequest.builder()
            .fromCriteria( (EventsAnalyticsQueryCriteria) criteria.withEndpointAction( QUERY ) )
            .build();

        assertEquals( eventDataQueryRequest.getDimension(), Set.of( Set.of( "pe:TODAY:INCIDENT_DATE" ) ) );

    }

    @Test
    void testEventsQueryMultiOptionsAreCorrectlyParsed()
    {
        EventsAnalyticsQueryCriteria criteria = new EventsAnalyticsQueryCriteria();
        criteria.setEnrollmentDate( "202111,2021;TODAY" );
        criteria.setEventDate( "LAST_YEAR" );
        criteria.setDimension( new HashSet<>( Set.of( "pe:LAST_MONTH" ) ) );

        EventDataQueryRequest eventDataQueryRequest = EventDataQueryRequest.builder()
            .fromCriteria( (EventsAnalyticsQueryCriteria) criteria.withEndpointAction( QUERY ) )
            .build();

        assertEquals( eventDataQueryRequest.getDimension(),
            Set.of(
                Set.of(
                    "pe:LAST_MONTH;LAST_YEAR:EVENT_DATE;202111:ENROLLMENT_DATE;2021:ENROLLMENT_DATE;TODAY:ENROLLMENT_DATE" ) ) );

    }

    @Test
    void testEnrollmentMultiOptionsAreCorrectlyParsed()
    {
        EnrollmentAnalyticsQueryCriteria criteria = new EnrollmentAnalyticsQueryCriteria();
        criteria.setIncidentDate( "202111,2021;TODAY" );
        criteria.setEnrollmentDate( "LAST_YEAR" );
        criteria.setDimension( new HashSet<>( Set.of( "pe:LAST_MONTH" ) ) );

        EventDataQueryRequest eventDataQueryRequest = EventDataQueryRequest.builder()
            .fromCriteria( (EnrollmentAnalyticsQueryCriteria) criteria.withEndpointAction( QUERY ) )
            .build();

        assertEquals( eventDataQueryRequest.getDimension(),
            Set.of(
                Set.of(
                    "pe:LAST_MONTH;LAST_YEAR:ENROLLMENT_DATE;202111:INCIDENT_DATE;2021:INCIDENT_DATE;TODAY:INCIDENT_DATE" ) ) );
    }

    @ParameterizedTest
    @MethodSource( value = "totalPagesFlags" )
    void totalPagesShouldBeSameInCriteriaAndRequestWhenCalled( boolean totalPages )
    {
        EnrollmentAnalyticsQueryCriteria criteria = new EnrollmentAnalyticsQueryCriteria();

        criteria.setTotalPages( totalPages );

        assertEquals( EventDataQueryRequest.builder()
            .fromCriteria( criteria )
            .build().isTotalPages(), totalPages );
    }
}
