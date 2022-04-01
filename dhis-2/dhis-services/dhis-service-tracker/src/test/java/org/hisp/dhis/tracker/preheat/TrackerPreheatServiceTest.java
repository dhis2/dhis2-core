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
package org.hisp.dhis.tracker.preheat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
class TrackerPreheatServiceTest extends TrackerTest
{

    @Autowired
    private TrackerPreheatService trackerPreheatService;

    @Override
    protected void initTest()
    {
    }

    @Test
    void testPreheatValidation()
        throws IOException
    {
        TrackerImportParams params = fromJson( "tracker/event_events.json" );
        assertTrue( params.getTrackedEntities().isEmpty() );
        assertTrue( params.getEnrollments().isEmpty() );
        assertFalse( params.getEvents().isEmpty() );
    }

    @Test
    void testPreheatEvents()
        throws IOException
    {
        setUpMetadata( "tracker/event_metadata.json" );
        TrackerImportParams params = fromJson( "tracker/event_events.json" );
        assertTrue( params.getTrackedEntities().isEmpty() );
        assertTrue( params.getEnrollments().isEmpty() );
        assertFalse( params.getEvents().isEmpty() );

        TrackerPreheat preheat = trackerPreheatService.preheat( params );

        assertNotNull( preheat );
        assertFalse( preheat.getAll( DataElement.class ).isEmpty() );
        assertFalse( preheat.getAll( OrganisationUnit.class ).isEmpty() );
        assertFalse( preheat.getAll( ProgramStage.class ).isEmpty() );
        assertFalse( preheat.getAll( CategoryOptionCombo.class ).isEmpty() );
        assertNotNull( preheat.get( CategoryOptionCombo.class, "XXXvX50cXC0" ) );
        assertNotNull( preheat.get( CategoryOption.class, "XXXrKDKCefk" ) );
    }
}
