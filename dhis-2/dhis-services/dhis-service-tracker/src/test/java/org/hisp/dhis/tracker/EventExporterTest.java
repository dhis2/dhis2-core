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
package org.hisp.dhis.tracker;

import static org.hisp.dhis.util.DateUtils.parseDate;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.List;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.EventSearchParams;
import org.hisp.dhis.dxf2.events.event.EventService;
import org.hisp.dhis.dxf2.events.event.Events;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

/**
 * @author Enrico Colasante
 */
class EventExporterTest extends TrackerTest
{

    @Autowired
    private EventService eventService;

    @Autowired
    private TrackerImportService trackerImportService;

    @Autowired
    private IdentifiableObjectManager manager;

    private User userA;

    private OrganisationUnit orgUnit;

    @Override
    protected void initTest()
        throws IOException
    {
        setUpMetadata( "tracker/simple_metadata.json" );
        userA = userService.getUser( "M5zQapPyTZI" );
        TrackerImportParams enrollmentParams = fromJson( "tracker/event_and_enrollment.json", userA.getUid() );
        assertNoImportErrors( trackerImportService.importTracker( enrollmentParams ) );
        orgUnit = manager.get( OrganisationUnit.class, "h4w96yEMlzO" );
        manager.flush();
    }

    @Test
    void testExportEvents()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        Events events = eventService.getEvents( params );
        assertNotNull( events );
        assertEquals( 2, events.getEvents().size() );
    }

    @Test
    void testExportEventsWhenFilteringByEnrollment()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setProgramInstances( Sets.newHashSet( "TvctPPhpD8z" ) );
        Events events = eventService.getEvents( params );
        assertNotNull( events );
        assertEquals( 1, events.getEvents().size() );
    }

    @Test
    void shouldReturnNoEventsWhenParamStartDueDateLaterThanEventDueDate()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setDueDateStart( parseDate( "2021-02-28T13:05:00.000" ) );

        Events events = eventService.getEvents( params );

        assertNotNull( events );
        assertIsEmpty( events.getEvents() );
    }

    @Test
    void shouldReturnEventsWhenParamStartDueDateEarlierThanEventsDueDate()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setDueDateStart( parseDate( "2018-02-28T13:05:00.000" ) );

        Events events = eventService.getEvents( params );

        assertNotNull( events );
        assertContainsOnly( List.of( "D9PbzJY8bJM", "D9PbzJY8bJO" ),
            events.getEvents().stream().map( Event::getEvent ).toArray( String[]::new ) );
    }

    @Test
    void shouldReturnNoEventsWhenParamEndDueDateEarlierThanEventDueDate()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setDueDateEnd( parseDate( "2018-02-28T13:05:00.000" ) );

        Events events = eventService.getEvents( params );

        assertNotNull( events );
        assertIsEmpty( events.getEvents() );
    }

    @Test
    void shouldReturnEventsWhenParamEndDueDateLaterThanEventsDueDate()
    {
        EventSearchParams params = new EventSearchParams();
        params.setOrgUnit( orgUnit );
        params.setDueDateEnd( parseDate( "2021-02-28T13:05:00.000" ) );

        Events events = eventService.getEvents( params );

        assertNotNull( events );
        assertContainsOnly( List.of( "D9PbzJY8bJM", "D9PbzJY8bJO" ),
            events.getEvents().stream().map( Event::getEvent ).toArray( String[]::new ) );
    }
}
