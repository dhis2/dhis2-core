/*
 * Copyright (c) 2004-2020, University of Oslo
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

package org.hisp.dhis.tracker.converter;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.converter.EventTrackerConverterService;
import org.hisp.dhis.tracker.converter.TrackerConverterService;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@RunWith( MockitoJUnitRunner.class )
public class EventTrackerConverterServiceTest
    extends DhisConvenienceTest
{
    private final static String PROGRAM_STAGE_UID = "ProgramStageUid";

    private final static String ORGANISATION_UNIT_UID = "OrganisationUnitUid";

    private final static String PROGRAM_UID = "ProgramUid";

    private NotesConverterService notesConverterService = new NotesConverterService();

    private TrackerConverterService<Event, ProgramStageInstance> trackerConverterService = new EventTrackerConverterService(
        notesConverterService );

    @Mock
    public TrackerPreheat preheat;

    @Before
    public void setUpTest()
    {
        ProgramStage programStage = createProgramStage( 'A', 1 );
        programStage.setUid( PROGRAM_STAGE_UID );

        OrganisationUnit organisationUnit = createOrganisationUnit( 'A' );
        organisationUnit.setUid( ORGANISATION_UNIT_UID );

        Program program = createProgram( 'A' );
        program.setUid( PROGRAM_UID );
        program.setProgramType( ProgramType.WITHOUT_REGISTRATION );

        programStage.setProgram( program );

        when( preheat.get( TrackerIdScheme.UID, ProgramStage.class, programStage.getUid() ) )
            .thenReturn( programStage );
        when( preheat.get( TrackerIdScheme.UID, OrganisationUnit.class, organisationUnit.getUid() ) )
            .thenReturn( organisationUnit );
    }

    @Test
    public void testToProgramStageInstance()
    {
        Event event = new Event();
        event.setProgram( PROGRAM_UID );
        event.setProgramStage( PROGRAM_STAGE_UID );
        event.setOrgUnit( ORGANISATION_UNIT_UID );

        ProgramStageInstance programStageInstance = trackerConverterService.from( preheat, event );

        assertNotNull( programStageInstance );
        assertNotNull( programStageInstance.getProgramStage() );
        assertNotNull( programStageInstance.getProgramStage().getProgram() );
        assertNotNull( programStageInstance.getOrganisationUnit() );

        assertEquals( PROGRAM_UID, programStageInstance.getProgramStage().getProgram().getUid() );
        assertEquals( PROGRAM_STAGE_UID, programStageInstance.getProgramStage().getUid() );
        assertEquals( ORGANISATION_UNIT_UID, programStageInstance.getOrganisationUnit().getUid() );
    }
}
