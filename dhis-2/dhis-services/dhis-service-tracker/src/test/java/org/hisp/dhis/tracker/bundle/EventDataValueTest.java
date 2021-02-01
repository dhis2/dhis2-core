/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.tracker.bundle;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.sql.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerImportService;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.report.TrackerImportReport;
import org.hisp.dhis.tracker.report.TrackerStatus;
import org.hisp.dhis.user.User;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class EventDataValueTest
    extends TrackerTest
{
    @Autowired
    private TrackerImportService trackerImportService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private ProgramStageInstanceService programStageInstanceService;

    private User userA;

    @Override
    protected void initTest()
        throws IOException
    {
        setUpMetadata( "tracker/simple_metadata.json" );

        userA = userService.getUser( "M5zQapPyTZI" );

        TrackerImportParams teiParams = fromJson( "tracker/single_tei.json", userA.getUid() );
        assertNoImportErrors( trackerImportService.importTracker( teiParams ) );

        TrackerImportParams enrollmentParams = fromJson( "tracker/single_enrollment.json", userA.getUid() );
        assertNoImportErrors( trackerImportService.importTracker( enrollmentParams ) );
    }

    @Test
    public void testEventDataValue()
        throws IOException
    {
        TrackerImportParams trackerImportParams = fromJson( "tracker/event_with_data_values.json" );

        TrackerImportReport trackerImportReport = trackerImportService.importTracker( trackerImportParams );
        assertEquals( TrackerStatus.OK, trackerImportReport.getStatus() );

        List<ProgramStageInstance> events = manager.getAll( ProgramStageInstance.class );
        assertEquals( 1, events.size() );

        ProgramStageInstance psi = events.get( 0 );

        Set<EventDataValue> eventDataValues = psi.getEventDataValues();

        assertEquals( 4, eventDataValues.size() );
    }

    @Test
    public void testClientDatesForTeiEnrollmentEvent()
        throws IOException
    {
        TrackerImportParams trackerImportParams = fromJson( "tracker/single_event.json" );

        TrackerImportReport trackerImportReport = trackerImportService.importTracker( trackerImportParams );

        TrackerImportParams teiParams = fromJson( "tracker/single_tei.json", userA.getUid() );
        TrackerImportParams enrollmentParams = fromJson( "tracker/single_enrollment.json", userA.getUid() );

        assertEquals( TrackerStatus.OK, trackerImportReport.getStatus() );

        List<TrackedEntityInstance> teis = manager.getAll( TrackedEntityInstance.class );
        assertEquals( 1, teis.size() );

        TrackedEntityInstance tei = teis.get( 0 );

        assertNotNull( tei.getCreatedAtClient() );
        assertNotNull( tei.getLastUpdatedAtClient() );

        assertEquals(
            Date.from( teiParams.getTrackedEntities().get( 0 ).getCreatedAtClient() ), tei.getCreatedAtClient() );
        assertEquals(
            Date.from( teiParams.getTrackedEntities().get( 0 ).getUpdatedAtClient() ), tei.getLastUpdatedAtClient() );

        Set<ProgramInstance> pis = tei.getProgramInstances();
        assertEquals( 1, pis.size() );
        ProgramInstance pi = pis.iterator().next();

        assertNotNull( pi.getCreatedAtClient() );
        assertNotNull( pi.getLastUpdatedAtClient() );

        assertEquals( Date.from( enrollmentParams.getEnrollments().get( 0 ).getCreatedAtClient() ),
            pi.getCreatedAtClient() );
        assertEquals(
            Date.from( enrollmentParams.getEnrollments().get( 0 ).getUpdatedAtClient() ), pi.getLastUpdatedAtClient() );

        Set<ProgramStageInstance> psis = pi.getProgramStageInstances();
        assertEquals( 1, psis.size() );
        ProgramStageInstance psi = psis.iterator().next();

        assertNotNull( psi.getCreatedAtClient() );
        assertNotNull( psi.getLastUpdatedAtClient() );

        assertEquals(
            Date.from( trackerImportParams.getEvents().get( 0 ).getCreatedAtClient() ), psi.getCreatedAtClient() );
        assertEquals(
            Date.from( trackerImportParams.getEvents().get( 0 ).getUpdatedAtClient() ), psi.getLastUpdatedAtClient() );
    }

    @Test
    public void testTrackedEntityProgramAttributeValueUpdate()
        throws IOException
    {
        TrackerImportParams trackerImportParams = fromJson( "tracker/event_with_data_values.json" );

        TrackerImportReport trackerImportReport = trackerImportService.importTracker( trackerImportParams );
        assertEquals( TrackerStatus.OK, trackerImportReport.getStatus() );

        List<ProgramStageInstance> events = manager.getAll( ProgramStageInstance.class );
        assertEquals( 1, events.size() );

        ProgramStageInstance psi = events.get( 0 );

        Set<EventDataValue> eventDataValues = psi.getEventDataValues();

        assertEquals( 4, eventDataValues.size() );

        // update

        trackerImportParams = fromJson( "tracker/event_with_updated_data_values.json" );
        // make sure that the uid property is populated as well - otherwise
        // update will
        // not work
        trackerImportParams.getEvents().get( 0 ).setUid( trackerImportParams.getEvents().get( 0 ).getEvent() );
        trackerImportParams.setImportStrategy( TrackerImportStrategy.CREATE_AND_UPDATE );

        trackerImportReport = trackerImportService.importTracker( trackerImportParams );
        assertEquals( TrackerStatus.OK, trackerImportReport.getStatus() );

        List<ProgramStageInstance> updatedEvents = manager.getAll( ProgramStageInstance.class );
        assertEquals( 1, updatedEvents.size() );

        ProgramStageInstance updatedPsi = programStageInstanceService
            .getProgramStageInstance( updatedEvents.get( 0 ).getUid() );

        assertEquals( 3, updatedPsi.getEventDataValues().size() );
        List<String> values = updatedPsi.getEventDataValues()
            .stream()
            .map( EventDataValue::getValue )
            .collect( Collectors.toList() );

        assertThat( values, hasItem( "First" ) );
        assertThat( values, hasItem( "Second" ) );
        assertThat( values, hasItem( "Fourth updated" ) );

    }
}
