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
package org.hisp.dhis.tracker.event.csv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.GeometryFactory;

class TrackerCsvEventServiceTest
{

    private DefaultCsvEventService service;

    private GeometryFactory geometryFactory;

    @BeforeEach
    void setUp()
    {
        service = new DefaultCsvEventService();
        geometryFactory = new GeometryFactory();
    }

    @Test
    void writeEventsHandlesEventsWithNullEventFields()
        throws IOException
    {
        // this is not to say Events will ever be defined in such a way, just to
        // prevent any further NPEs from slipping through

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        service.writeEvents( out, Collections.singletonList( new ProgramStageInstance() ), false );

        assertEquals( ",ACTIVE,,,,,,,,,,,false,false,,,,,,,,,,,,,,,,,\n", out.toString() );
    }

    @Test
    void writeEventsWithoutDataValues()
        throws IOException
    {
        ProgramStageInstance event = new ProgramStageInstance();
        event.setUid( "BuA2R2Gr4vt" );
        ProgramInstance enrollment = new ProgramInstance();
        enrollment.setFollowup( true );
        event.setProgramInstance( enrollment );
        event.setDeleted( false );
        event.setStatus( EventStatus.ACTIVE );

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        service.writeEvents( out, Collections.singletonList( event ), false );

        assertEquals( "BuA2R2Gr4vt,ACTIVE,,,,,,,,,,,true,false,,,,,,,,,,,,,,,,,\n", out.toString() );
    }

    @Test
    void writeEventsWithDataValuesIntoASingleRow()
        throws IOException
    {

        EventDataValue dataValue1 = new EventDataValue();
        dataValue1.setDataElement( "color" );
        dataValue1.setValue( "purple" );
        dataValue1.setProvidedElsewhere( true );
        dataValue1.setCreated( null );
        dataValue1.setLastUpdated( null );
        EventDataValue dataValue2 = new EventDataValue();
        dataValue2.setDataElement( "color2" );
        dataValue2.setValue( "yellow" );
        dataValue2.setProvidedElsewhere( true );
        dataValue2.setCreated( null );
        dataValue2.setLastUpdated( null );
        ProgramStageInstance event = new ProgramStageInstance();
        event.setUid( "BuA2R2Gr4vt" );
        ProgramInstance enrollment = new ProgramInstance();
        enrollment.setFollowup( true );
        event.setProgramInstance( enrollment );
        event.setDeleted( false );
        event.setStatus( EventStatus.ACTIVE );
        event.setEventDataValues( Set.of( dataValue1, dataValue2 ) );

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        service.writeEvents( out, Collections.singletonList( event ), false );

        assertInCSV( out, "BuA2R2Gr4vt,ACTIVE,,,,,,,,,,,true,false,,,,,,,,,,,color2,yellow,,true,,,\n" );
        assertInCSV( out, "BuA2R2Gr4vt,ACTIVE,,,,,,,,,,,true,false,,,,,,,,,,,color,purple,,true,,,\n" );
    }

    private void assertInCSV( ByteArrayOutputStream out, String expectedLine )
    {
        // not using assertEquals as dataValues are in a Set so its order in the
        // CSV is not guaranteed
        assertTrue( out.toString().contains( expectedLine ),
            () -> "expected line is not in CSV:\nexpected line:\n" + expectedLine + "\ngot CSV:\n" + out );
    }
}
