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
package org.hisp.dhis.tracker.importer;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.List;

import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.tracker.TrackerNtiApiTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author Luca Cambi
 */
public class TrackerEventsExportTests
    extends TrackerNtiApiTest
{
    @BeforeAll
    public void beforeAll()
        throws Exception
    {
        loginActions.loginAsSuperUser();

        trackerActions.postAndGetJobReport(
            new File( "src/test/resources/tracker/importer/teis/teisWithEnrollmentsAndEvents.json" ) )
            .validateSuccessfulImport();
    }

    @Test
    public void shouldReturnFilteredEvent()
    {
        trackerActions.get( "events?enrollmentOccurredAfter=2019-08-16&enrollmentOccurredBefore=2019-08-20" )
            .validate()
            .statusCode( 200 )
            .body( "instances", hasSize( equalTo( 1 ) ) )
            .rootPath( "instances[0]" )
            .body( "event", equalTo( "ZwwuwNp6gVd" ) );
    }

    @Test
    public void shouldReturnDescOrderedEventByTEIAttribute()
    {
        ApiResponse response = trackerActions.get( "events?order=dIVt4l5vIOa:desc" );
        response
            .validate()
            .statusCode( 200 )
            .body( "instances", hasSize( equalTo( 2 ) ) );
        List<String> events = response.extractList( "instances.event.flatten()" );
        assertEquals( List.of( "olfXZzSGacW", "ZwwuwNp6gVd" ), events, "Events are not in the correct order" );
    }

    @Test
    public void shouldReturnAscOrderedEventByTEIAttribute()
    {
        ApiResponse response = trackerActions.get( "events?order=dIVt4l5vIOa:asc" );
        response
            .validate()
            .statusCode( 200 )
            .body( "instances", hasSize( equalTo( 2 ) ) );
        List<String> events = response.extractList( "instances.event.flatten()" );
        assertEquals( List.of( "ZwwuwNp6gVd", "olfXZzSGacW" ), events, "Events are not in the correct order" );
    }
}
