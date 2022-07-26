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
package org.hisp.dhis.webapi.controller.tracker.csv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.tracker.domain.DataValue;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.io.ParseException;

import com.google.common.io.Files;

class TrackerCsvEventServiceTest
{

    private TrackerCsvEventService service;

    @BeforeEach
    void setUp()
    {
        service = new TrackerCsvEventService();
    }

    @Test
    void writeEventsHandlesEventsWithNullEventFields()
        throws IOException
    {
        // this is not to say Events will ever be defined in such a way, just to
        // prevent any further NPEs from slipping through

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        service.writeEvents( out, Collections.singletonList( new Event() ), false );

        assertEquals( "", out.toString() );
    }

    @Test
    void writeEvents()
        throws IOException
    {

        DataValue dataValue = DataValue.builder()
            .dataElement( "color" )
            .value( "red" )
            .providedElsewhere( true )
            .build();
        Event event = Event.builder()
            .event( "BuA2R2Gr4vt" )
            .followup( true )
            .deleted( false )
            .status( EventStatus.ACTIVE )
            .dataValues( Collections.singleton( dataValue ) )
            .build();

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        service.writeEvents( out, Collections.singletonList( event ), false );

        assertEquals( "BuA2R2Gr4vt,ACTIVE,,,,,,,,,,,true,false,,,,,,,,,,,color,red,,true,,,\n", out.toString() );
    }

    @Test
    void testReadEventsFromFileWithHeader()
        throws IOException,
        ParseException
    {
        File event = new File( "src/test/resources/controller/tracker/csv/event.csv" );
        InputStream inputStream = Files.asByteSource( event ).openStream();

        List<Event> events = service.readEvents( inputStream, true );

        assertFalse( events.isEmpty() );
        assertEquals( 1, events.size() );
        assertEquals( "eventId", events.get( 0 ).getEvent() );
        assertEquals( "programId", events.get( 0 ).getProgram() );
        assertEquals( "programStageId", events.get( 0 ).getProgramStage() );
        assertEquals( "orgUnitId", events.get( 0 ).getOrgUnit() );
        assertEquals( "2018-11-01T00:00:00Z", events.get( 0 ).getOccurredAt().toString() );
        assertEquals( EventStatus.ACTIVE, events.get( 0 ).getStatus() );
        assertNull( events.get( 0 ).getEnrollment() );
        assertNull( events.get( 0 ).getCreatedAt() );
        assertNull( events.get( 0 ).getCreatedAtClient() );
        assertNull( events.get( 0 ).getUpdatedAt() );
        assertNull( events.get( 0 ).getUpdatedAtClient() );
        assertNull( events.get( 0 ).getScheduledAt() );
        assertNull( events.get( 0 ).getCompletedAt() );
        assertNull( events.get( 0 ).getCompletedBy() );
        assertNull( events.get( 0 ).getStoredBy() );
        assertNull( events.get( 0 ).getAttributeOptionCombo() );
        assertNull( events.get( 0 ).getAttributeCategoryOptions() );
        assertEquals( new User(), events.get( 0 ).getAssignedUser() );
        assertTrue( events.get( 0 ).getDataValues().isEmpty() );
        assertTrue( events.get( 0 ).getRelationships().isEmpty() );
        assertTrue( events.get( 0 ).getNotes().isEmpty() );
    }

    @Test
    void testReadEventsFromFileWithoutHeader()
        throws IOException,
        ParseException
    {
        File event = new File( "src/test/resources/controller/tracker/csv/completeEvent.csv" );
        InputStream inputStream = Files.asByteSource( event ).openStream();

        List<Event> events = service.readEvents( inputStream, false );

        assertFalse( events.isEmpty() );
        assertEquals( 1, events.size() );
        assertEquals( "eventId", events.get( 0 ).getEvent() );
        assertEquals( "programId", events.get( 0 ).getProgram() );
        assertEquals( "programStageId", events.get( 0 ).getProgramStage() );
        assertEquals( "orgUnitId", events.get( 0 ).getOrgUnit() );
        assertEquals( "2020-02-26T23:01:00Z", events.get( 0 ).getOccurredAt().toString() );
        assertEquals( EventStatus.COMPLETED, events.get( 0 ).getStatus() );
        assertEquals( "enrollmentId", events.get( 0 ).getEnrollment() );
        assertEquals( "2020-02-26T23:02:00Z", events.get( 0 ).getScheduledAt().toString() );
        assertEquals( "2020-02-26T23:03:00Z", events.get( 0 ).getCreatedAt().toString() );
        assertEquals( "2020-02-26T23:04:00Z", events.get( 0 ).getCreatedAtClient().toString() );
        assertEquals( "2020-02-26T23:05:00Z", events.get( 0 ).getUpdatedAt().toString() );
        assertEquals( "2020-02-26T23:06:00Z", events.get( 0 ).getUpdatedAtClient().toString() );
        assertEquals( "2020-02-26T23:07:00Z", events.get( 0 ).getCompletedAt().toString() );
        assertEquals( "admin", events.get( 0 ).getCompletedBy() );
        assertEquals( "admin", events.get( 0 ).getStoredBy() );
        assertEquals( "attributeOptionCombo", events.get( 0 ).getAttributeOptionCombo() );
        assertEquals( "attributeCategoryOptions", events.get( 0 ).getAttributeCategoryOptions() );
        assertEquals( "assignedUser", events.get( 0 ).getAssignedUser().getUsername() );
        assertEquals( 1, events.get( 0 ).getDataValues().size() );
        assertTrue( events.get( 0 ).getRelationships().isEmpty() );
        assertTrue( events.get( 0 ).getNotes().isEmpty() );
        events.get( 0 ).getDataValues().forEach( dv -> {
            assertEquals( "value", dv.getValue() );
            assertEquals( "2020-02-26T23:09:00Z", dv.getCreatedAt().toString() );
            assertEquals( "2020-02-26T23:08:00Z", dv.getUpdatedAt().toString() );
            assertEquals( "dataElement", dv.getDataElement() );
            assertEquals( "admin", dv.getStoredBy() );
            assertFalse( dv.isProvidedElsewhere() );
        } );
    }
}
